package backend.saas.dto;

import java.time.OffsetDateTime;

public record RegisterTenantResponse(
        String tenantId,
        String tenantCode,
        String tenantName,
        String tenantStatus,
        String defaultOrganizationId,
        OffsetDateTime trialEndAt
) {
}

