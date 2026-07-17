package backend.saas.model;

import java.time.OffsetDateTime;

public record ComplianceAcceptanceRecord(
        String acceptanceId,
        String tenantId,
        String documentCode,
        String documentVersion,
        String acceptedBy,
        String acceptedSource,
        OffsetDateTime acceptedAt
) {
}

