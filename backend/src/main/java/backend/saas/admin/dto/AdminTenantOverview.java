package backend.saas.admin.dto;

import java.time.OffsetDateTime;

public record AdminTenantOverview(
        String tenantId,
        String tenantCode,
        String tenantName,
        String tenantStatus,
        String ownerName,
        String mobile,
        String subscriptionId,
        String planCode,
        String planName,
        String subscriptionStatus,
        int seatCount,
        int quotaCount,
        int totalQuotaLimit,
        int totalUsedAmount,
        OffsetDateTime trialEndAt,
        OffsetDateTime createdAt
) {
}

