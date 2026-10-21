package backend.automation.model;

import java.time.OffsetDateTime;

/**
 * 人工确认队列：PAUSE_REVIEW 挂起后生成一条待审任务。
 * status：PENDING / APPROVED / REJECTED
 */
public record AutomationApproval(
        String taskId,
        String executionId,
        String tenantId,
        String storeId,
        String targetType,
        String targetId,
        String reason,
        String status,
        String reviewedBy,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt
) {
    public AutomationApproval withReview(String status, String reviewedBy, OffsetDateTime reviewedAt) {
        return new AutomationApproval(
                taskId, executionId, tenantId, storeId, targetType, targetId,
                reason, status, reviewedBy, reviewedAt, createdAt
        );
    }
}
