package backend.automation.model;

import java.time.OffsetDateTime;

/**
 * 自动化执行记录：每次动作执行写一行。
 * status：SUCCESS / FAILED / PENDING_REVIEW / SKIPPED
 * triggerSource：SCHEDULE / MANUAL / EVENT
 */
public record AutomationExecution(
        String executionId,
        String tenantId,
        String storeId,
        String ruleId,
        String actionType,
        String triggerSource,
        String targetType,
        String targetId,
        String result,
        String status,
        int retryCount,
        long costMs,
        String errorMessage,
        OffsetDateTime executedAt
) {
    public AutomationExecution withStatus(String status,
                                          String result,
                                          int retryCount,
                                          long costMs,
                                          String errorMessage) {
        return new AutomationExecution(
                executionId, tenantId, storeId, ruleId, actionType, triggerSource,
                targetType, targetId, result, status, retryCount, costMs, errorMessage, executedAt
        );
    }
}
