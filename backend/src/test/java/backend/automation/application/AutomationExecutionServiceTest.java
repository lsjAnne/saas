package backend.automation.application;

import backend.automation.domain.repository.AutomationActionRepository;
import backend.automation.domain.repository.AutomationApprovalRepository;
import backend.automation.domain.repository.AutomationExecutionRepository;
import backend.automation.model.AutomationAction;
import backend.automation.model.AutomationApproval;
import backend.automation.model.AutomationExecution;
import backend.audit.application.AuditLogService;
import backend.channel.application.ChannelSyncService;
import backend.channel.model.ChannelShipResult;
import backend.finance.application.FinanceService;
import backend.finance.domain.repository.FinanceBillRepository;
import backend.fulfillment.domain.repository.FulfillmentTaskRepository;
import backend.order.application.OrderApplicationService;
import backend.order.domain.repository.OrderRepository;
import backend.order.model.OrderMain;
import backend.rule.domain.repository.AutomationRuleRepository;
import backend.rule.model.AutomationRule;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import backend.tenant.context.TenantContext;
import backend.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutomationExecutionServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    private AutomationActionRepository actionRepository;
    private AutomationExecutionRepository executionRepository;
    private AutomationApprovalRepository approvalRepository;
    private AutomationRuleRepository ruleRepository;
    private OrderRepository orderRepository;
    private OrderApplicationService orderApplicationService;
    private ChannelSyncService channelSyncService;
    private FulfillmentTaskRepository fulfillmentTaskRepository;
    private FinanceService financeService;
    private FinanceBillRepository financeBillRepository;
    private StoreRepository storeRepository;
    private AuditLogService auditLogService;
    private AutomationExecutionService service;

    private Store store;
    private OrderMain order;

    @BeforeEach
    void setUp() {
        actionRepository = mock(AutomationActionRepository.class);
        executionRepository = mock(AutomationExecutionRepository.class);
        approvalRepository = mock(AutomationApprovalRepository.class);
        ruleRepository = mock(AutomationRuleRepository.class);
        orderRepository = mock(OrderRepository.class);
        orderApplicationService = mock(OrderApplicationService.class);
        channelSyncService = mock(ChannelSyncService.class);
        fulfillmentTaskRepository = mock(FulfillmentTaskRepository.class);
        financeService = mock(FinanceService.class);
        financeBillRepository = mock(FinanceBillRepository.class);
        storeRepository = mock(StoreRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new AutomationExecutionService(
                actionRepository, executionRepository, approvalRepository, ruleRepository,
                orderRepository, orderApplicationService, channelSyncService, fulfillmentTaskRepository,
                financeService, financeBillRepository, storeRepository, auditLogService, new RuleMatcher());

        store = new Store("store-1", "tenant-1", "org-1", "owner", "pdd", "shop-pdd-1",
                "pdd-store", "authorized", BigDecimal.ZERO, BigDecimal.ZERO, null, NOW);
        order = new OrderMain("order-1", "store-1", "po-1", "pending_fulfillment", "pending",
                new BigDecimal("398.00"), new BigDecimal("20.00"), "buyer", "138****0000",
                "浙江省杭州市测试路1号", NOW.plusHours(24), NOW);
        TenantContextHolder.set(new TenantContext("tenant-1", "tester", "user"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private AutomationRule rule(String ruleId, String expression) {
        return new AutomationRule(ruleId, "store-1", "order_risk", "general", "general",
                "rule-" + ruleId, expression, true, NOW);
    }

    private AutomationAction action(String actionId, String ruleId, String type, int priority) {
        return new AutomationAction(actionId, "tenant-1", "store-1", ruleId, type,
                null, priority, true, NOW, NOW);
    }

    private void stubOrderContext(AutomationRule rule, List<AutomationAction> actions) {
        when(ruleRepository.findByStoreIds(List.of("store-1"))).thenReturn(List.of(rule));
        when(actionRepository.findByStoreId("store-1")).thenReturn(actions);
        when(orderRepository.findByStoreIds(List.of("store-1"))).thenReturn(List.of(order));
        when(fulfillmentTaskRepository.findByOrderId("order-1")).thenReturn(List.of());
    }

    @Test
    void shouldSkipWhenExecutionAlreadyExists() {
        stubOrderContext(rule("rule-1", "amount >= 100"), List.of(action("action-1", "rule-1", "AUTO_SHIP", 10)));
        when(executionRepository.findExisting("tenant-1", "order-1", "rule-1", "AUTO_SHIP"))
                .thenReturn(Optional.of(new AutomationExecution(
                        "exec-1", "tenant-1", "store-1", "rule-1", "AUTO_SHIP", "MANUAL",
                        "ORDER", "order-1", "done", "SUCCESS", 0, 10L, null, NOW)));

        AutomationExecutionService.EvaluationSummary summary =
                service.evaluateOrders("tenant-1", store, "MANUAL", "channel-1");

        assertThat(summary.matchedRuleCount()).isEqualTo(1);
        assertThat(summary.executionCount()).isZero();
        verify(executionRepository, never()).save(any());
    }

    @Test
    void shouldRetryThenSucceedWithinLimit() {
        stubOrderContext(rule("rule-1", "amount >= 100"), List.of(action("action-1", "rule-1", "AUTO_SHIP", 10)));
        when(executionRepository.findExisting(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(channelSyncService.shipOrder(anyString(), any())).thenThrow(new IllegalStateException("boom"))
                .thenThrow(new IllegalStateException("boom"))
                .thenReturn(new ChannelShipResult("po-1", true, "ok", "ship-1"));

        AutomationExecutionService.EvaluationSummary summary =
                service.evaluateOrders("tenant-1", store, "MANUAL", "channel-1");

        assertThat(summary.executionCount()).isEqualTo(1);
        verify(executionRepository).save(org.mockito.ArgumentMatchers.argThat(execution ->
                "SUCCESS".equals(execution.status()) && execution.retryCount() == 2));
    }

    @Test
    void shouldMarkFailedAfterMaxRetries() {
        stubOrderContext(rule("rule-1", "amount >= 100"), List.of(action("action-1", "rule-1", "AUTO_SHIP", 10)));
        when(executionRepository.findExisting(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(channelSyncService.shipOrder(anyString(), any()))
                .thenThrow(new IllegalStateException("boom"));

        AutomationExecutionService.EvaluationSummary summary =
                service.evaluateOrders("tenant-1", store, "MANUAL", "channel-1");

        assertThat(summary.executionCount()).isEqualTo(1);
        verify(executionRepository).save(org.mockito.ArgumentMatchers.argThat(execution ->
                "FAILED".equals(execution.status())
                        && execution.retryCount() == AutomationExecutionService.MAX_RETRY - 1
                        && execution.errorMessage() != null));
    }

    @Test
    void shouldPauseReviewThenApproveResumesFlow() {
        AutomationRule rule = rule("rule-1", "amount >= 100");
        stubOrderContext(rule, List.of(
                action("action-1", "rule-1", "AUTO_SHIP", 10),
                action("action-2", "rule-1", "PAUSE_REVIEW", 20)));
        when(executionRepository.findExisting(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(channelSyncService.shipOrder(anyString(), any()))
                .thenReturn(new ChannelShipResult("po-1", true, "ok", "ship-1"));

        AutomationExecutionService.EvaluationSummary summary =
                service.evaluateOrders("tenant-1", store, "MANUAL", "channel-1");
        assertThat(summary.executionCount()).isEqualTo(2);

        // 捕获挂起审批单与挂起执行记录
        java.util.concurrent.atomic.AtomicReference<AutomationApproval> pausedApproval =
                new java.util.concurrent.atomic.AtomicReference<>();
        verify(approvalRepository).save(org.mockito.ArgumentMatchers.argThat(approval -> {
            pausedApproval.set(approval);
            return "PENDING".equals(approval.status());
        }));
        assertThat(pausedApproval.get()).isNotNull();
        String pauseExecutionId = pausedApproval.get().executionId();

        // 人工放行
        AutomationExecution pauseExecution = new AutomationExecution(
                pauseExecutionId, "tenant-1", "store-1", "rule-1", "PAUSE_REVIEW", "MANUAL",
                "ORDER", "order-1", "paused_for_review", "PENDING_REVIEW", 0, 0L, null, NOW);
        when(approvalRepository.findByTaskId(pausedApproval.get().taskId()))
                .thenReturn(Optional.of(pausedApproval.get()));
        when(storeRepository.findByStoreId("store-1")).thenReturn(Optional.of(store));
        when(orderRepository.findByOrderId("order-1")).thenReturn(Optional.of(order));
        when(ruleRepository.findByRuleId("rule-1")).thenReturn(Optional.of(rule));
        when(executionRepository.findByExecutionId(pauseExecutionId))
                .thenReturn(Optional.of(pauseExecution));
        when(executionRepository.findExisting("tenant-1", "order-1", "rule-1", "AUTO_SHIP"))
                .thenReturn(Optional.of(pauseExecution));
        when(executionRepository.findExisting("tenant-1", "order-1", "rule-1", "PAUSE_REVIEW"))
                .thenReturn(Optional.of(pauseExecution));

        AutomationApproval approved = service.approve("tenant-1", pausedApproval.get().taskId(), "ok");

        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(approved.reviewedBy()).isEqualTo("tester");
        verify(executionRepository).save(org.mockito.ArgumentMatchers.argThat(execution ->
                pauseExecutionId.equals(execution.executionId()) && "SUCCESS".equals(execution.status())));
    }

    @Test
    void shouldRejectAndMarkPauseExecutionFailed() {
        AutomationRule rule = rule("rule-1", "amount >= 100");
        stubOrderContext(rule, List.of(
                action("action-1", "rule-1", "AUTO_SHIP", 10),
                action("action-2", "rule-1", "PAUSE_REVIEW", 20)));
        when(executionRepository.findExisting(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(channelSyncService.shipOrder(anyString(), any()))
                .thenReturn(new ChannelShipResult("po-1", true, "ok", "ship-1"));

        service.evaluateOrders("tenant-1", store, "MANUAL", "channel-1");
        java.util.concurrent.atomic.AtomicReference<AutomationApproval> pausedApproval =
                new java.util.concurrent.atomic.AtomicReference<>();
        verify(approvalRepository).save(org.mockito.ArgumentMatchers.argThat(approval -> {
            pausedApproval.set(approval);
            return true;
        }));
        String pauseExecutionId = pausedApproval.get().executionId();

        AutomationExecution pauseExecution = new AutomationExecution(
                pauseExecutionId, "tenant-1", "store-1", "rule-1", "PAUSE_REVIEW", "MANUAL",
                "ORDER", "order-1", "paused_for_review", "PENDING_REVIEW", 0, 0L, null, NOW);
        when(approvalRepository.findByTaskId(pausedApproval.get().taskId()))
                .thenReturn(Optional.of(pausedApproval.get()));
        when(storeRepository.findByStoreId("store-1")).thenReturn(Optional.of(store));
        when(executionRepository.findByExecutionId(pauseExecutionId))
                .thenReturn(Optional.of(pauseExecution));

        AutomationApproval rejected = service.reject("tenant-1", pausedApproval.get().taskId(), "too risky");

        assertThat(rejected.status()).isEqualTo("REJECTED");
        verify(executionRepository).save(org.mockito.ArgumentMatchers.argThat(execution ->
                pauseExecutionId.equals(execution.executionId())
                        && "FAILED".equals(execution.status())
                        && execution.errorMessage().contains("too risky")));
    }
}
