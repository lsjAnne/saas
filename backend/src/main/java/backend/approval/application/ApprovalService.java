package backend.approval.application;

import backend.approval.model.ApprovalInstance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Deprecated(forRemoval = false)
public class ApprovalService {

    private final ApprovalApplicationService approvalApplicationService;

    public ApprovalService(ApprovalApplicationService approvalApplicationService) {
        this.approvalApplicationService = approvalApplicationService;
    }

    public List<ApprovalInstance> listApprovals(String tenantId) {
        return approvalApplicationService.listApprovals(tenantId);
    }

    public ApprovalInstance getApproval(String tenantId, String approvalId) {
        return approvalApplicationService.getApproval(tenantId, approvalId);
    }

    public ApprovalInstance createApproval(String tenantId, String operatorId, CreateApprovalRequest request) {
        return approvalApplicationService.createApproval(tenantId, operatorId, request);
    }

    public ApprovalInstance createApprovalIfAbsent(String tenantId,
                                                   String operatorId,
                                                   String approvalType,
                                                   String relatedType,
                                                   String relatedId,
                                                   String currentHandlerId,
                                                   String remark) {
        return approvalApplicationService.createApprovalIfAbsent(
                tenantId,
                operatorId,
                approvalType,
                relatedType,
                relatedId,
                currentHandlerId,
                remark
        );
    }

    public ApprovalInstance approve(String tenantId, String operatorId, String approvalId, String remark) {
        return approvalApplicationService.approve(tenantId, operatorId, approvalId, remark);
    }

    public ApprovalInstance reject(String tenantId, String operatorId, String approvalId, String remark) {
        return approvalApplicationService.reject(tenantId, operatorId, approvalId, remark);
    }

    public ApprovalInstance transfer(String tenantId, String operatorId, String approvalId, TransferApprovalRequest request) {
        return approvalApplicationService.transfer(tenantId, operatorId, approvalId, request);
    }

    public List<ApprovalTemplateView> listApprovalTemplates(String tenantId) {
        return approvalApplicationService.listApprovalTemplates(tenantId);
    }

    public ApprovalTemplateView createApprovalTemplate(String tenantId, CreateApprovalTemplateRequest request) {
        return approvalApplicationService.createApprovalTemplate(tenantId, request);
    }

    public List<GovernanceApprovalRequestView> listGovernanceApprovalRequests(String tenantId) {
        return approvalApplicationService.listGovernanceApprovalRequests(tenantId);
    }

    public GovernanceApprovalRequestView getGovernanceApprovalRequest(String tenantId, String requestId) {
        return approvalApplicationService.getGovernanceApprovalRequest(tenantId, requestId);
    }

    public GovernanceApprovalRequestView createGovernanceApprovalRequest(String tenantId,
                                                                         String operatorId,
                                                                         CreateGovernanceApprovalRequest request) {
        return approvalApplicationService.createGovernanceApprovalRequest(tenantId, operatorId, request);
    }

    public void clear() {
        approvalApplicationService.clear();
    }

    public record CreateApprovalRequest(
            String approvalType,
            String relatedType,
            String relatedId,
            String currentHandlerId,
            String remark
    ) {
    }

    public record TransferApprovalRequest(
            String currentHandlerId,
            String remark
    ) {
    }

    public record CreateApprovalTemplateRequest(
            String templateCode,
            String templateName,
            String approvalType,
            Boolean enabled,
            List<ApprovalStageDefinition> stages
    ) {
    }

    public record ApprovalStageDefinition(
            String stageCode,
            String handlerId,
            Integer stageOrder
    ) {
    }

    public record ApprovalTemplateView(
            String templateId,
            String tenantId,
            String templateCode,
            String templateName,
            String approvalType,
            boolean enabled,
            List<ApprovalStageDefinition> stages,
            OffsetDateTime createdAt
    ) {
    }

    public record CreateGovernanceApprovalRequest(
            String approvalType,
            String templateCode,
            String documentNo,
            String subject,
            BigDecimal amount,
            String counterparty,
            String remark
    ) {
    }

    public record GovernanceApprovalRequestView(
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
            Integer currentStageOrder,
            String currentHandlerId,
            String remark,
            OffsetDateTime createdAt
    ) {
    }
}

