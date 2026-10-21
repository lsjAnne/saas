package backend.automation.application;

import backend.automation.application.RuleMatcher.RuleMatchContext;
import backend.automation.domain.repository.AutomationActionRepository;
import backend.automation.domain.repository.AutomationApprovalRepository;
import backend.automation.domain.repository.AutomationExecutionRepository;
import backend.automation.model.AutomationAction;
import backend.automation.model.AutomationApproval;
import backend.automation.model.AutomationExecution;
import backend.audit.application.AuditLogService;
import backend.channel.application.ChannelSyncService;
import backend.channel.model.ChannelShipRequest;
import backend.channel.model.ChannelShipResult;
import backend.common.exception.BusinessException;
import backend.finance.application.FinanceService;
import backend.finance.domain.repository.FinanceBillRepository;
import backend.finance.model.FinanceBill;
import backend.fulfillment.domain.repository.FulfillmentTaskRepository;
import backend.order.application.OrderApplicationService;
import backend.order.domain.repository.OrderRepository;
import backend.order.model.OrderMain;
import backend.rule.domain.repository.AutomationRuleRepository;
import backend.rule.model.AutomationRule;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import backend.tenant.context.TenantAccessSupport;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 自动化执行引擎：触发 → 加载租户启用规则 → RuleMatcher 白名单匹配 →
 * 命中动作按 priority 依次执行 → 写 execution 记录；幂等去重、失败重试上限 3 次、
 * PAUSE_REVIEW 挂起进人工队列，人工 APPROVED 放行后续动作 / REJECTED 终止。
 */
@Service
public class AutomationExecutionService {

    static final int MAX_RETRY = 3;
    private static final String TARGET_ORDER = "ORDER";

    private final AutomationActionRepository actionRepository;
    private final AutomationExecutionRepository executionRepository;
    private final AutomationApprovalRepository approvalRepository;
    private final AutomationRuleRepository ruleRepository;
    private final OrderRepository orderRepository;
    private final OrderApplicationService orderApplicationService;
    private final ChannelSyncService channelSyncService;
    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final FinanceService financeService;
    private final FinanceBillRepository financeBillRepository;
    private final StoreRepository storeRepository;
    private final AuditLogService auditLogService;
    private final RuleMatcher ruleMatcher;

    public AutomationExecutionService(AutomationActionRepository actionRepository,
                                      AutomationExecutionRepository executionRepository,
                                      AutomationApprovalRepository approvalRepository,
                                      AutomationRuleRepository ruleRepository,
                                      OrderRepository orderRepository,
                                      OrderApplicationService orderApplicationService,
                                      ChannelSyncService channelSyncService,
                                      FulfillmentTaskRepository fulfillmentTaskRepository,
                                      FinanceService financeService,
                                      FinanceBillRepository financeBillRepository,
                                      StoreRepository storeRepository,
                                      AuditLogService auditLogService,
                                      RuleMatcher ruleMatcher) {
        this.actionRepository = actionRepository;
        this.executionRepository = executionRepository;
        this.approvalRepository = approvalRepository;
        this.ruleRepository = ruleRepository;
        this.orderRepository = orderRepository;
        this.orderApplicationService = orderApplicationService;
        this.channelSyncService = channelSyncService;
        this.fulfillmentTaskRepository = fulfillmentTaskRepository;
        this.financeService = financeService;
        this.financeBillRepository = financeBillRepository;
        this.storeRepository = storeRepository;
        this.auditLogService = auditLogService;
        this.ruleMatcher = ruleMatcher;
    }

    /**
     * 对店铺下全部订单执行规则引擎：先自动风控审核，再按规则匹配并依次执行动作。
     */
    public EvaluationSummary evaluateOrders(String tenantId, Store store, String triggerSource, String channelAccountId) {
        List<AutomationRule> enabledRules = ruleRepository.findByStoreIds(List.of(store.storeId())).stream()
                .filter(AutomationRule::enabled)
                .toList();
        List<AutomationAction> enabledActions = actionRepository.findByStoreId(store.storeId()).stream()
                .filter(AutomationAction::enabled)
                .toList();
        List<OrderMain> orders = orderRepository.findByStoreIds(List.of(store.storeId()));
        int matchedRules = 0;
        List<String> createdExecutionIds = new ArrayList<>();
        for (OrderMain order : orders) {
            orderApplicationService.auditOrder(tenantId, order.orderId());
            RuleMatchContext context = new RuleMatchContext(order.totalAmount(), order.orderStatus(), store.platformType());
            for (AutomationRule rule : enabledRules) {
                if (!ruleMatcher.matches(rule.ruleExpression(), context)) {
                    continue;
                }
                matchedRules++;
                List<AutomationAction> ruleActions = enabledActions.stream()
                        .filter(action -> action.ruleId().equals(rule.ruleId()))
                        .toList();
                createdExecutionIds.addAll(runSequence(tenantId, store, order, triggerSource, rule, ruleActions, channelAccountId));
            }
        }
        return new EvaluationSummary(matchedRules, createdExecutionIds.size(), createdExecutionIds);
    }

    /**
     * 按 priority 依次执行动作序列；遇到 PAUSE_REVIEW 挂起并停止，等待人工放行。
     */
    private List<String> runSequence(String tenantId,
                                     Store store,
                                     OrderMain order,
                                     String triggerSource,
                                     AutomationRule rule,
                                     List<AutomationAction> actions,
                                     String channelAccountId) {
        List<String> createdIds = new ArrayList<>();
        for (AutomationAction action : actions) {
            Optional<AutomationExecution> existing = executionRepository.findExisting(
                    tenantId, order.orderId(), rule.ruleId(), action.actionType());
            if (existing.isPresent()) {
                continue;
            }
            if ("PAUSE_REVIEW".equals(action.actionType())) {
                createdIds.add(pauseForReview(tenantId, store, order, triggerSource, rule, action));
                return createdIds;
            }
            createdIds.add(executeActionWithRetry(tenantId, store, order, triggerSource, rule, action, channelAccountId));
        }
        return createdIds;
    }

    private String pauseForReview(String tenantId,
                                  Store store,
                                  OrderMain order,
                                  String triggerSource,
                                  AutomationRule rule,
                                  AutomationAction action) {
        OffsetDateTime now = OffsetDateTime.now();
        String executionId = newUuid();
        AutomationExecution pauseExecution = new AutomationExecution(
                executionId, tenantId, store.storeId(), rule.ruleId(), action.actionType(), triggerSource,
                TARGET_ORDER, order.orderId(), "paused_for_review", "PENDING_REVIEW", 0, 0L, null, now);
        executionRepository.save(pauseExecution);
        AutomationApproval approval = new AutomationApproval(
                newUuid(), executionId, tenantId, store.storeId(), TARGET_ORDER, order.orderId(),
                "rule=" + rule.ruleName() + " action=" + action.actionType() + " requires manual review",
                "PENDING", null, null, now);
        approvalRepository.save(approval);
        auditLogService.recordForTenant(tenantId, "AUTOMATION_PAUSE_REVIEW", "automation_approval", approval.taskId());
        return executionId;
    }

    private String executeActionWithRetry(String tenantId,
                                          Store store,
                                          OrderMain order,
                                          String triggerSource,
                                          AutomationRule rule,
                                          AutomationAction action,
                                          String channelAccountId) {
        long start = System.currentTimeMillis();
        Exception lastError = null;
        int attempts = 0;
        String outcome = null;
        while (attempts < MAX_RETRY) {
            attempts++;
            try {
                outcome = dispatchAction(tenantId, store, order, action, channelAccountId);
                lastError = null;
                break;
            } catch (Exception exception) {
                lastError = exception;
            }
        }
        long costMs = System.currentTimeMillis() - start;
        String status = lastError == null ? "SUCCESS" : "FAILED";
        String errorMessage = lastError == null ? null : lastError.getMessage();
        AutomationExecution execution = new AutomationExecution(
                newUuid(), tenantId, store.storeId(), rule.ruleId(), action.actionType(), triggerSource,
                TARGET_ORDER, order.orderId(), outcome, status, attempts - 1, costMs, errorMessage, OffsetDateTime.now());
        executionRepository.save(execution);
        auditLogService.recordForTenant(tenantId, "AUTOMATION_EXECUTE_ACTION", "automation_execution", execution.executionId());
        return execution.executionId();
    }

    private String dispatchAction(String tenantId,
                                  Store store,
                                  OrderMain order,
                                  AutomationAction action,
                                  String channelAccountId) {
        return switch (action.actionType()) {
            case "AUTO_SHIP" -> doShip(tenantId, store, order, channelAccountId);
            case "GENERATE_BILL" -> doGenerateBill(tenantId, store, order);
            case "AUTO_RECONCILE" -> doReconcile(tenantId, store);
            case "NOTIFY" -> "notified:" + order.orderId();
            default -> throw new BusinessException("8601", "unsupported actionType: " + action.actionType(), HttpStatus.BAD_REQUEST);
        };
    }

    private String doShip(String tenantId, Store store, OrderMain order, String channelAccountId) {
        if (channelAccountId == null || channelAccountId.isBlank()) {
            throw new IllegalStateException("channel account not resolved for auto ship");
        }
        ChannelShipRequest shipRequest = new ChannelShipRequest(
                tenantId, store.organizationId(), channelAccountId, store.storeId(),
                order.platformOrderId(), "mock", "MOCK-SF-" + UUID.randomUUID().toString().substring(0, 8), Map.of());
        ChannelShipResult result = channelSyncService.shipOrder(tenantId, shipRequest);
        if (!result.success()) {
            throw new IllegalStateException("channel ship failed: " + result.message());
        }
        fulfillmentTaskRepository.findByOrderId(order.orderId()).forEach(task ->
                fulfillmentTaskRepository.save(task.withStatus("shipped", null)));
        return "shipped:" + result.platformShipmentId();
    }

    private String doGenerateBill(String tenantId, Store store, OrderMain order) {
        LocalDate day = order.createdAt().toLocalDate();
        try {
            FinanceBill bill = financeService.generateBill(tenantId,
                    new FinanceService.GenerateFinanceBillRequest(store.storeId(), "sales", day, day));
            return "bill_created:" + bill.financeBillId();
        } catch (BusinessException exception) {
            if ("7402".equals(exception.code())) {
                return "bill_already_exists:" + store.storeId();
            }
            throw exception;
        }
    }

    private String doReconcile(String tenantId, Store store) {
        FinanceBill target = financeBillRepository.findByStoreIds(List.of(store.storeId())).stream()
                .max(Comparator.comparing(FinanceBill::createdAt))
                .orElseThrow(() -> new IllegalStateException("no finance bill to reconcile"));
        if (!"draft".equals(target.billStatus())) {
            return "already_reconciled:" + target.financeBillId() + ":" + target.billStatus();
        }
        FinanceService.FinanceBillReconcileView view = financeService.reconcile(tenantId, target.financeBillId());
        return "reconciled:" + target.financeBillId() + ":" + view.billStatus();
    }

    /** 人工放行：标记审批 APPROVED，PAUSE_REVIEW 执行记录置 SUCCESS，续跑后续动作。 */
    public AutomationApproval approve(String tenantId, String taskId, String comment) {
        AutomationApproval approval = requirePendingApproval(tenantId, taskId);
        String operator = TenantAccessSupport.requiredContext().operatorId();
        AutomationApproval reviewed = approval.withReview("APPROVED", operator, OffsetDateTime.now());
        approvalRepository.save(reviewed);
        auditLogService.recordForTenant(tenantId, "AUTOMATION_APPROVE", "automation_approval", taskId);

        AutomationExecution pauseExecution = executionRepository.findByExecutionId(approval.executionId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        executionRepository.save(pauseExecution.withStatus(
                "SUCCESS", "approved:" + (comment == null ? "ok" : comment),
                pauseExecution.retryCount(), pauseExecution.costMs(), null));

        Store store = storeRepository.findByStoreId(approval.storeId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        OrderMain order = orderRepository.findByOrderId(approval.targetId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        AutomationRule rule = ruleRepository.findByRuleId(pauseExecution.ruleId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        List<AutomationAction> remainingActions = actionRepository.findByStoreId(store.storeId()).stream()
                .filter(AutomationAction::enabled)
                .filter(action -> action.ruleId().equals(rule.ruleId()))
                .toList();
        runSequence(tenantId, store, order, "EVENT", rule, remainingActions, null);
        return reviewed;
    }

    /** 人工驳回：标记 REJECTED，PAUSE_REVIEW 执行记录置 FAILED，后续动作不再执行。 */
    public AutomationApproval reject(String tenantId, String taskId, String comment) {
        AutomationApproval approval = requirePendingApproval(tenantId, taskId);
        String operator = TenantAccessSupport.requiredContext().operatorId();
        AutomationApproval reviewed = approval.withReview("REJECTED", operator, OffsetDateTime.now());
        approvalRepository.save(reviewed);
        auditLogService.recordForTenant(tenantId, "AUTOMATION_REJECT", "automation_approval", taskId);

        AutomationExecution pauseExecution = executionRepository.findByExecutionId(approval.executionId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        executionRepository.save(pauseExecution.withStatus(
                "FAILED", "rejected", pauseExecution.retryCount(), pauseExecution.costMs(),
                "reviewer rejected: " + (comment == null ? "no reason" : comment)));
        return reviewed;
    }

    private AutomationApproval requirePendingApproval(String tenantId, String taskId) {
        AutomationApproval approval = approvalRepository.findByTaskId(taskId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        Store store = storeRepository.findByStoreId(approval.storeId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        if (!"PENDING".equals(approval.status())) {
            throw new BusinessException("8602", "approval task already resolved: " + approval.status(), HttpStatus.BAD_REQUEST);
        }
        return approval;
    }

    public void clear() {
        actionRepository.deleteAll();
        executionRepository.deleteAll();
        approvalRepository.deleteAll();
    }

    private static String newUuid() {
        return UUID.randomUUID().toString();
    }

    public record EvaluationSummary(int matchedRuleCount, int executionCount, List<String> createdExecutionIds) {
    }
}
