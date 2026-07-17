package backend.approval.model;

import java.time.OffsetDateTime;

public record ApprovalInstance(
        String approvalId,
        String tenantId,
        String approvalType,
        String relatedType,
        String relatedId,
        String status,
        String currentHandlerId,
        String remark,
        String resultRemark,
        OffsetDateTime createdAt
) {
    public ApprovalInstance withStatus(String status, String currentHandlerId, String resultRemark) {
        return new ApprovalInstance(
                approvalId,
                tenantId,
                approvalType,
                relatedType,
                relatedId,
                status,
                currentHandlerId,
                remark,
                resultRemark,
                createdAt
        );
    }

    public ApprovalInstance withTransfer(String currentHandlerId, String resultRemark) {
        return new ApprovalInstance(
                approvalId,
                tenantId,
                approvalType,
                relatedType,
                relatedId,
                status,
                currentHandlerId,
                remark,
                resultRemark,
                createdAt
        );
    }
}

