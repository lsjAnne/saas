package backend.rule.model;

import java.time.OffsetDateTime;

public record AutomationRule(
        String ruleId,
        String storeId,
        String ruleType,
        String ruleCategory,
        String riskCategory,
        String ruleName,
        String ruleExpression,
        boolean enabled,
        OffsetDateTime createdAt
) {
}

