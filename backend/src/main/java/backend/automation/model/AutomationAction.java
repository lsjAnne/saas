package backend.automation.model;

import java.time.OffsetDateTime;

/**
 * 规则动作绑定：一条规则可绑定多个动作，按 priority 升序依次执行。
 * actionType：AUTO_SHIP / PAUSE_REVIEW / GENERATE_BILL / AUTO_RECONCILE / NOTIFY
 */
public record AutomationAction(
        String actionId,
        String tenantId,
        String storeId,
        String ruleId,
        String actionType,
        String actionConfig,
        int priority,
        boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
