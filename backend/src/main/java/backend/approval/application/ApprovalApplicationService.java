package backend.approval.application;

import backend.approval.domain.repository.ApprovalInstanceRepository;
import backend.approval.application.ApprovalService.ApprovalStageDefinition;
import backend.approval.application.ApprovalService.ApprovalTemplateView;
import backend.approval.application.ApprovalService.CreateApprovalRequest;
import backend.approval.application.ApprovalService.CreateApprovalTemplateRequest;
import backend.approval.application.ApprovalService.CreateGovernanceApprovalRequest;
import backend.approval.application.ApprovalService.GovernanceApprovalRequestView;
import backend.approval.application.ApprovalService.TransferApprovalRequest;
import backend.approval.model.ApprovalInstance;
import backend.audit.application.AuditLogService;
import backend.campaign.domain.repository.CampaignActivityRepository;
import backend.campaign.model.CampaignActivity;
import backend.common.exception.BusinessException;
import backend.inventory.application.InventoryService;
import backend.inventory.domain.repository.ReplenishmentTaskRepository;
import backend.inventory.model.ReplenishmentTask;
import backend.notification.application.NotificationService;
import backend.order.domain.repository.OrderRepository;
import backend.order.model.OrderMain;
import backend.servicecase.domain.repository.AfterSaleRecordRepository;
import backend.servicecase.model.AfterSaleRecord;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ApprovalApplicationService {

    private static final Map<String, ApprovalTemplateState> APPROVAL_TEMPLATES = new LinkedHashMap<>();
    private static final Map<String, GovernanceApprovalRequestState> GOVERNANCE_APPROVAL_REQUESTS = new LinkedHashMap<>();
    private static final AtomicLong APPROVAL_TEMPLATE_SEQUENCE = new AtomicLong(1L);
    private static final AtomicLong GOVERNANCE_REQUEST_SEQUENCE = new AtomicLong(1L);

    private final AuditLogService auditLogService;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final ReplenishmentTaskRepository replenishmentTaskRepository;
    private final AfterSaleRecordRepository afterSaleRecordRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final CampaignActivityRepository campaignActivityRepository;
    private final NotificationService notificationService;

    public ApprovalApplicationService(AuditLogService auditLogService,
                                      ApprovalInstanceRepository approvalInstanceRepository,
                                      ReplenishmentTaskRepository replenishmentTaskRepository,
                                      AfterSaleRecordRepository afterSaleRecordRepository,
                                      OrderRepository orderRepository,
                                      StoreRepository storeRepository,
                                      CampaignActivityRepository campaignActivityRepository,
                                      NotificationService notificationService) {
        this.auditLogService = auditLogService;
        this.approvalInstanceRepository = approvalInstanceRepository;
        this.replenishmentTaskRepository = replenishmentTaskRepository;
        this.afterSaleRecordRepository = afterSaleRecordRepository;
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.campaignActivityRepository = campaignActivityRepository;
        this.notificationService = notificationService;
    }

    public List<ApprovalInstance> listApprovals(String tenantId) {
        return approvalInstanceRepository.findByTenantId(tenantId);
    }

    public ApprovalInstance getApproval(String tenantId, String approvalId) {
        ApprovalInstance approvalInstance = approvalInstanceRepository.findByApprovalId(approvalId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(approvalInstance.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        return approvalInstance;
    }

    public ApprovalInstance createApproval(String tenantId, String operatorId, CreateApprovalRequest request) {
        requireOwnedRelatedObject(tenantId, request.relatedType(), request.relatedId());
        return createApprovalIfAbsent(
                tenantId,
                operatorId,
                request.approvalType(),
                request.relatedType(),
                request.relatedId(),
                request.currentHandlerId(),
                request.remark()
        );
    }

    public ApprovalInstance createApprovalIfAbsent(String tenantId,
                                                   String operatorId,
                                                   String approvalType,
                                                   String relatedType,
                                                   String relatedId,
                                                   String currentHandlerId,
                                                   String remark) {
        requireOwnedRelatedObject(tenantId, relatedType, relatedId);
        ApprovalInstance existing = approvalInstanceRepository.findPendingByRelated(tenantId, relatedType, relatedId).orElse(null);
        if (existing != null) {
            return existing;
        }
        ApprovalInstance saved = approvalInstanceRepository.save(new ApprovalInstance(
                null,
                tenantId,
                approvalType,
                relatedType,
                relatedId,
                "pending",
                fallbackHandler(currentHandlerId, operatorId),
                remark,
                null,
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_APPROVAL", "approval_instance", saved.approvalId());
        notificationService.createSystemNotification(
                tenantId,
                "site_message",
                "approval_pending",
                saved.currentHandlerId(),
                "{\"approvalId\":\"%s\",\"relatedType\":\"%s\",\"relatedId\":\"%s\"}".formatted(saved.approvalId(), saved.relatedType(), saved.relatedId())
        );
        return saved;
    }

    public ApprovalInstance approve(String tenantId, String operatorId, String approvalId, String remark) {
        ApprovalInstance current = requirePendingApproval(tenantId, approvalId);
        if ("governance_request".equals(current.relatedType())) {
            return approveGovernanceRequest(tenantId, operatorId, current, remark);
        }
        ApprovalInstance updated = approvalInstanceRepository.save(current.withStatus("approved", operatorId, remark));
        applyDecision(updated, true);
        auditLogService.recordForTenant(tenantId, "APPROVE_APPROVAL", "approval_instance", approvalId);
        notificationService.createSystemNotification(
                tenantId,
                "site_message",
                "approval_approved",
                operatorId,
                "{\"approvalId\":\"%s\"}".formatted(approvalId)
        );
        return updated;
    }

    public ApprovalInstance reject(String tenantId, String operatorId, String approvalId, String remark) {
        ApprovalInstance current = requirePendingApproval(tenantId, approvalId);
        if ("governance_request".equals(current.relatedType())) {
            return rejectGovernanceRequest(tenantId, operatorId, current, remark);
        }
        ApprovalInstance updated = approvalInstanceRepository.save(current.withStatus("rejected", operatorId, remark));
        applyDecision(updated, false);
        auditLogService.recordForTenant(tenantId, "REJECT_APPROVAL", "approval_instance", approvalId);
        notificationService.createSystemNotification(
                tenantId,
                "site_message",
                "approval_rejected",
                operatorId,
                "{\"approvalId\":\"%s\"}".formatted(approvalId)
        );
        return updated;
    }

    public ApprovalInstance transfer(String tenantId, String operatorId, String approvalId, TransferApprovalRequest request) {
        ApprovalInstance current = requirePendingApproval(tenantId, approvalId);
        ApprovalInstance updated = approvalInstanceRepository.save(current.withTransfer(request.currentHandlerId(), request.remark()));
        if ("governance_request".equals(current.relatedType())) {
            GovernanceApprovalRequestState governanceRequest = requireGovernanceApprovalRequestState(tenantId, current.relatedId());
            GOVERNANCE_APPROVAL_REQUESTS.put(
                    governanceRequest.requestId(),
                    governanceRequest.withHandler(request.currentHandlerId())
            );
        }
        auditLogService.recordForTenant(tenantId, "TRANSFER_APPROVAL", "approval_instance", approvalId);
        notificationService.createSystemNotification(
                tenantId,
                "site_message",
                "approval_transferred",
                request.currentHandlerId(),
                "{\"approvalId\":\"%s\"}".formatted(approvalId)
        );
        return updated;
    }

    public List<ApprovalTemplateView> listApprovalTemplates(String tenantId) {
        return APPROVAL_TEMPLATES.values().stream()
                .filter(template -> tenantId.equals(template.tenantId()))
                .sorted(Comparator.comparing(ApprovalTemplateState::createdAt).thenComparing(ApprovalTemplateState::templateId))
                .map(this::toApprovalTemplateView)
                .toList();
    }

    public ApprovalTemplateView createApprovalTemplate(String tenantId, CreateApprovalTemplateRequest request) {
        validateApprovalTemplateRequest(request);
        ApprovalTemplateState existing = findTemplateByCode(tenantId, request.templateCode()).orElse(null);
        ApprovalTemplateState saved = new ApprovalTemplateState(
                existing == null ? "approval-template-" + APPROVAL_TEMPLATE_SEQUENCE.getAndIncrement() : existing.templateId(),
                tenantId,
                request.templateCode(),
                request.templateName(),
                request.approvalType(),
                request.enabled() == null || request.enabled(),
                normalizeStages(request.stages()),
                existing == null ? OffsetDateTime.now() : existing.createdAt()
        );
        APPROVAL_TEMPLATES.put(saved.templateId(), saved);
        auditLogService.recordForTenant(tenantId, "CREATE_APPROVAL_TEMPLATE", "approval_template", saved.templateId());
        return toApprovalTemplateView(saved);
    }

    public List<GovernanceApprovalRequestView> listGovernanceApprovalRequests(String tenantId) {
        return GOVERNANCE_APPROVAL_REQUESTS.values().stream()
                .filter(request -> tenantId.equals(request.tenantId()))
                .sorted(Comparator.comparing(GovernanceApprovalRequestState::createdAt).reversed()
                        .thenComparing(GovernanceApprovalRequestState::requestId, Comparator.reverseOrder()))
                .map(this::toGovernanceApprovalRequestView)
                .toList();
    }

    public GovernanceApprovalRequestView getGovernanceApprovalRequest(String tenantId, String requestId) {
        return toGovernanceApprovalRequestView(requireGovernanceApprovalRequestState(tenantId, requestId));
    }

    public GovernanceApprovalRequestView createGovernanceApprovalRequest(String tenantId,
                                                                         String operatorId,
                                                                         CreateGovernanceApprovalRequest request) {
        validateGovernanceApprovalRequest(request);
        ApprovalTemplateState template = requireTemplateByCode(tenantId, request.templateCode());
        if (!template.enabled()) {
            throw new BusinessException("1008", "approval template disabled", HttpStatus.BAD_REQUEST);
        }
        if (!template.approvalType().equals(request.approvalType())) {
            throw new BusinessException("1004", "approvalType does not match template", HttpStatus.BAD_REQUEST);
        }
        StageState firstStage = template.stages().get(0);
        GovernanceApprovalRequestState governanceRequest = new GovernanceApprovalRequestState(
                "governance-request-" + GOVERNANCE_REQUEST_SEQUENCE.getAndIncrement(),
                tenantId,
                request.approvalType(),
                template.templateId(),
                template.templateCode(),
                request.documentNo(),
                request.subject(),
                request.amount(),
                request.counterparty(),
                operatorId,
                "pending",
                firstStage.stageCode(),
                firstStage.stageOrder(),
                firstStage.handlerId(),
                request.remark(),
                OffsetDateTime.now()
        );
        GOVERNANCE_APPROVAL_REQUESTS.put(governanceRequest.requestId(), governanceRequest);
        createApprovalIfAbsent(
                tenantId,
                operatorId,
                request.approvalType() + "_approval",
                "governance_request",
                governanceRequest.requestId(),
                firstStage.handlerId(),
                request.remark()
        );
        auditLogService.recordForTenant(tenantId, "CREATE_GOVERNANCE_APPROVAL_REQUEST", "governance_request", governanceRequest.requestId());
        return toGovernanceApprovalRequestView(governanceRequest);
    }

    public void clear() {
        approvalInstanceRepository.deleteAll();
        APPROVAL_TEMPLATES.clear();
        GOVERNANCE_APPROVAL_REQUESTS.clear();
        APPROVAL_TEMPLATE_SEQUENCE.set(1L);
        GOVERNANCE_REQUEST_SEQUENCE.set(1L);
    }

    private ApprovalInstance requirePendingApproval(String tenantId, String approvalId) {
        ApprovalInstance approvalInstance = getApproval(tenantId, approvalId);
        if (!"pending".equals(approvalInstance.status())) {
            throw new BusinessException("1008", "approval status does not allow operation", HttpStatus.BAD_REQUEST);
        }
        return approvalInstance;
    }

    private void requireOwnedRelatedObject(String tenantId, String relatedType, String relatedId) {
        switch (relatedType) {
            case "replenishment_task" -> requireOwnedReplenishmentTask(tenantId, relatedId);
            case "purchase_order" -> requireOwnedPurchaseOrder(tenantId, relatedId);
            case "after_sale_record" -> requireOwnedAfterSaleRecord(tenantId, relatedId);
            case "campaign_activity" -> requireOwnedCampaignActivity(tenantId, relatedId);
            case "governance_request" -> requireGovernanceApprovalRequestState(tenantId, relatedId);
            default -> throw new BusinessException("1002", "unsupported approval related type", HttpStatus.BAD_REQUEST);
        }
    }

    private ReplenishmentTask requireOwnedReplenishmentTask(String tenantId, String relatedId) {
        ReplenishmentTask task = replenishmentTaskRepository.findByReplenishmentTaskId(relatedId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, task.storeId());
        return task;
    }

    private AfterSaleRecord requireOwnedAfterSaleRecord(String tenantId, String relatedId) {
        AfterSaleRecord record = afterSaleRecordRepository.findByAfterSaleId(relatedId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        OrderMain order = orderRepository.findByOrderId(record.orderId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, order.storeId());
        return record;
    }

    private InventoryService.PurchaseOrder requireOwnedPurchaseOrder(String tenantId, String relatedId) {
        InventoryService.PurchaseOrder purchaseOrder = InventoryService.findPurchaseOrderForApproval(relatedId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, purchaseOrder.storeId());
        return purchaseOrder;
    }

    private CampaignActivity requireOwnedCampaignActivity(String tenantId, String relatedId) {
        CampaignActivity campaignActivity = campaignActivityRepository.findByCampaignId(relatedId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, campaignActivity.storeId());
        return campaignActivity;
    }

    private void applyDecision(ApprovalInstance approvalInstance, boolean approved) {
        switch (approvalInstance.relatedType()) {
            case "replenishment_task" -> applyReplenishmentDecision(approvalInstance.relatedId(), approved);
            case "purchase_order" -> applyPurchaseOrderDecision(approvalInstance.relatedId(), approved);
            case "after_sale_record" -> applyAfterSaleDecision(approvalInstance.relatedId(), approved);
            case "campaign_activity" -> applyCampaignDecision(approvalInstance.relatedId(), approved);
            case "governance_request" -> applyGovernanceDecision(approvalInstance.relatedId(), approved);
            default -> throw new BusinessException("1002", "unsupported approval related type", HttpStatus.BAD_REQUEST);
        }
    }

    private void applyReplenishmentDecision(String replenishmentTaskId, boolean approved) {
        ReplenishmentTask task = replenishmentTaskRepository.findByReplenishmentTaskId(replenishmentTaskId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        replenishmentTaskRepository.save(task.withWorkflow(approved ? "approved" : "rejected", approved ? "approved" : "rejected"));
    }

    private void applyPurchaseOrderDecision(String purchaseOrderId, boolean approved) {
        InventoryService.PurchaseOrder purchaseOrder = InventoryService.findPurchaseOrderForApproval(purchaseOrderId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        InventoryService.savePurchaseOrderForApproval(
                purchaseOrder.withApproval(approved ? "approved" : "rejected", approved ? "approved" : "rejected")
        );
    }

    private void applyAfterSaleDecision(String afterSaleId, boolean approved) {
        AfterSaleRecord afterSaleRecord = afterSaleRecordRepository.findByAfterSaleId(afterSaleId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        afterSaleRecordRepository.save(afterSaleRecord.withStatus(approved ? "approved" : "rejected"));

        if (!approved) {
            OrderMain order = orderRepository.findByOrderId(afterSaleRecord.orderId())
                    .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
            orderRepository.save(order.withStatus("pending_fulfillment", order.logisticsStatus()));
        }
    }

    private void applyCampaignDecision(String campaignId, boolean approved) {
        CampaignActivity campaignActivity = campaignActivityRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        campaignActivityRepository.save(campaignActivity.withStatus(approved ? "approved" : "rejected"));
    }

    private void applyGovernanceDecision(String requestId, boolean approved) {
        GovernanceApprovalRequestState governanceRequest = requireGovernanceApprovalRequestState(null, requestId);
        GOVERNANCE_APPROVAL_REQUESTS.put(
                requestId,
                governanceRequest.withStatus(approved ? "approved" : "rejected")
        );
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        return store;
    }

    private String fallbackHandler(String currentHandlerId, String operatorId) {
        if (currentHandlerId != null && !currentHandlerId.isBlank()) {
            return currentHandlerId;
        }
        return operatorId == null || operatorId.isBlank() ? "tenant-admin" : operatorId;
    }

    private ApprovalInstance approveGovernanceRequest(String tenantId,
                                                      String operatorId,
                                                      ApprovalInstance current,
                                                      String remark) {
        GovernanceApprovalRequestState governanceRequest = requireGovernanceApprovalRequestState(tenantId, current.relatedId());
        ApprovalTemplateState template = requireTemplateById(tenantId, governanceRequest.templateId());
        StageState nextStage = nextStage(template, governanceRequest.currentStageOrder()).orElse(null);
        if (nextStage != null) {
            GOVERNANCE_APPROVAL_REQUESTS.put(
                    governanceRequest.requestId(),
                    governanceRequest.advanceTo(nextStage.stageCode(), nextStage.stageOrder(), nextStage.handlerId())
            );
            ApprovalInstance updated = approvalInstanceRepository.save(current.withTransfer(nextStage.handlerId(), remark));
            auditLogService.recordForTenant(tenantId, "ADVANCE_GOVERNANCE_APPROVAL_STAGE", "approval_instance", current.approvalId());
            notificationService.createSystemNotification(
                    tenantId,
                    "site_message",
                    "approval_pending",
                    nextStage.handlerId(),
                    "{\"approvalId\":\"%s\",\"relatedType\":\"%s\",\"relatedId\":\"%s\"}".formatted(updated.approvalId(), updated.relatedType(), updated.relatedId())
            );
            return updated;
        }
        ApprovalInstance updated = approvalInstanceRepository.save(current.withStatus("approved", operatorId, remark));
        applyGovernanceDecision(governanceRequest.requestId(), true);
        auditLogService.recordForTenant(tenantId, "APPROVE_APPROVAL", "approval_instance", current.approvalId());
        notificationService.createSystemNotification(
                tenantId,
                "site_message",
                "approval_approved",
                operatorId,
                "{\"approvalId\":\"%s\"}".formatted(current.approvalId())
        );
        return updated;
    }

    private ApprovalInstance rejectGovernanceRequest(String tenantId,
                                                     String operatorId,
                                                     ApprovalInstance current,
                                                     String remark) {
        ApprovalInstance updated = approvalInstanceRepository.save(current.withStatus("rejected", operatorId, remark));
        applyGovernanceDecision(current.relatedId(), false);
        auditLogService.recordForTenant(tenantId, "REJECT_APPROVAL", "approval_instance", current.approvalId());
        notificationService.createSystemNotification(
                tenantId,
                "site_message",
                "approval_rejected",
                operatorId,
                "{\"approvalId\":\"%s\"}".formatted(current.approvalId())
        );
        return updated;
    }

    private GovernanceApprovalRequestState requireGovernanceApprovalRequestState(String tenantId, String requestId) {
        GovernanceApprovalRequestState request = GOVERNANCE_APPROVAL_REQUESTS.get(requestId);
        if (request == null) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        if (tenantId != null && !tenantId.equals(request.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        return request;
    }

    private Optional<ApprovalTemplateState> findTemplateByCode(String tenantId, String templateCode) {
        return APPROVAL_TEMPLATES.values().stream()
                .filter(item -> tenantId.equals(item.tenantId()))
                .filter(item -> templateCode.equals(item.templateCode()))
                .findFirst();
    }

    private ApprovalTemplateState requireTemplateByCode(String tenantId, String templateCode) {
        return findTemplateByCode(tenantId, templateCode)
                .orElseThrow(() -> new BusinessException("1003", "approval template not found", HttpStatus.NOT_FOUND));
    }

    private ApprovalTemplateState requireTemplateById(String tenantId, String templateId) {
        ApprovalTemplateState template = APPROVAL_TEMPLATES.get(templateId);
        if (template == null || !tenantId.equals(template.tenantId())) {
            throw new BusinessException("1003", "approval template not found", HttpStatus.NOT_FOUND);
        }
        return template;
    }

    private List<StageState> normalizeStages(List<ApprovalStageDefinition> stages) {
        List<ApprovalStageDefinition> safeStages = stages == null ? List.of() : stages;
        List<StageState> normalized = new ArrayList<>();
        for (ApprovalStageDefinition stage : safeStages) {
            if (stage == null
                    || stage.stageCode() == null || stage.stageCode().isBlank()
                    || stage.handlerId() == null || stage.handlerId().isBlank()
                    || stage.stageOrder() == null || stage.stageOrder() <= 0) {
                throw new BusinessException("1002", "invalid approval stage definition", HttpStatus.BAD_REQUEST);
            }
            normalized.add(new StageState(stage.stageCode(), stage.handlerId(), stage.stageOrder()));
        }
        if (normalized.isEmpty()) {
            throw new BusinessException("1002", "approval template stages are required", HttpStatus.BAD_REQUEST);
        }
        normalized.sort(Comparator.comparing(StageState::stageOrder));
        return normalized;
    }

    private Optional<StageState> nextStage(ApprovalTemplateState template, int currentStageOrder) {
        return template.stages().stream()
                .filter(stage -> stage.stageOrder() > currentStageOrder)
                .min(Comparator.comparing(StageState::stageOrder));
    }

    private ApprovalTemplateView toApprovalTemplateView(ApprovalTemplateState template) {
        return new ApprovalTemplateView(
                template.templateId(),
                template.tenantId(),
                template.templateCode(),
                template.templateName(),
                template.approvalType(),
                template.enabled(),
                template.stages().stream()
                        .map(stage -> new ApprovalStageDefinition(stage.stageCode(), stage.handlerId(), stage.stageOrder()))
                        .toList(),
                template.createdAt()
        );
    }

    private GovernanceApprovalRequestView toGovernanceApprovalRequestView(GovernanceApprovalRequestState request) {
        return new GovernanceApprovalRequestView(
                request.requestId(),
                request.tenantId(),
                request.approvalType(),
                request.templateId(),
                request.templateCode(),
                request.documentNo(),
                request.subject(),
                request.amount(),
                request.counterparty(),
                request.requesterId(),
                request.requestStatus(),
                request.currentStageCode(),
                request.currentStageOrder(),
                request.currentHandlerId(),
                request.remark(),
                request.createdAt()
        );
    }

    private void validateApprovalTemplateRequest(CreateApprovalTemplateRequest request) {
        if (request.templateCode() == null || request.templateCode().isBlank()
                || request.templateName() == null || request.templateName().isBlank()
                || request.approvalType() == null || request.approvalType().isBlank()) {
            throw new BusinessException("1002", "approval template fields are required", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateGovernanceApprovalRequest(CreateGovernanceApprovalRequest request) {
        if (request.approvalType() == null || request.approvalType().isBlank()
                || request.templateCode() == null || request.templateCode().isBlank()
                || request.documentNo() == null || request.documentNo().isBlank()
                || request.subject() == null || request.subject().isBlank()
                || request.amount() == null
                || request.counterparty() == null || request.counterparty().isBlank()) {
            throw new BusinessException("1002", "governance approval request fields are required", HttpStatus.BAD_REQUEST);
        }
    }

    private record StageState(
            String stageCode,
            String handlerId,
            int stageOrder
    ) {
    }

    private record ApprovalTemplateState(
            String templateId,
            String tenantId,
            String templateCode,
            String templateName,
            String approvalType,
            boolean enabled,
            List<StageState> stages,
            OffsetDateTime createdAt
    ) {
    }

    private record GovernanceApprovalRequestState(
            String requestId,
            String tenantId,
            String approvalType,
            String templateId,
            String templateCode,
            String documentNo,
            String subject,
            BigDecimal amount,
            String counterparty,
            String requesterId,
            String requestStatus,
            String currentStageCode,
            int currentStageOrder,
            String currentHandlerId,
            String remark,
            OffsetDateTime createdAt
    ) {
        GovernanceApprovalRequestState advanceTo(String stageCode, int stageOrder, String handlerId) {
            return new GovernanceApprovalRequestState(
                    requestId, tenantId, approvalType, templateId, templateCode, documentNo, subject, amount, counterparty,
                    requesterId, "pending", stageCode, stageOrder, handlerId, remark, createdAt
            );
        }

        GovernanceApprovalRequestState withStatus(String requestStatus) {
            return new GovernanceApprovalRequestState(
                    requestId, tenantId, approvalType, templateId, templateCode, documentNo, subject, amount, counterparty,
                    requesterId, requestStatus, currentStageCode, currentStageOrder, currentHandlerId, remark, createdAt
            );
        }

        GovernanceApprovalRequestState withHandler(String currentHandlerId) {
            return new GovernanceApprovalRequestState(
                    requestId, tenantId, approvalType, templateId, templateCode, documentNo, subject, amount, counterparty,
                    requesterId, requestStatus, currentStageCode, currentStageOrder, currentHandlerId, remark, createdAt
            );
        }
    }
}

