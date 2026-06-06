package com.dianshang.platform.saas.model;

import java.time.OffsetDateTime;

public record UsageQuota(
        String tenantId,
        String quotaCode,
        int quotaLimit,
        int usedAmount,
        OffsetDateTime resetAt
) {
    public UsageQuota withUsedAmount(int usedAmount) {
        return new UsageQuota(tenantId, quotaCode, quotaLimit, usedAmount, resetAt);
    }
}
