package backend.saas.model;

import java.time.OffsetDateTime;

public record TenantCleanupTaskRecord(
        String cleanupTaskId,
        String tenantId,
        String status,
        String reason,
        String cleanupScopesJson,
        String requestedBy,
        String reviewedBy,
        String executedBy,
        String impactSummaryJson,
        String resultSummaryJson,
        OffsetDateTime createdAt,
        OffsetDateTime executedAt,
        OffsetDateTime completedAt
) {
}

