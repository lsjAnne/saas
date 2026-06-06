package com.dianshang.platform.saas.model;

import java.time.OffsetDateTime;

public record TenantSubscription(
        String subscriptionId,
        String tenantId,
        String planCode,
        String planName,
        String subscriptionStatus,
        OffsetDateTime startedAt,
        OffsetDateTime expiredAt,
        int seatCount,
        boolean autoRenew
) {
}
