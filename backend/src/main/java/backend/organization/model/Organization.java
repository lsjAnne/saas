package backend.organization.model;

import java.time.OffsetDateTime;

public record Organization(
        String id,
        String tenantId,
        String organizationName,
        String status,
        OffsetDateTime createdAt
) {
}

