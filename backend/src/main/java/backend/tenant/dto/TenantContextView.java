package backend.tenant.dto;

public record TenantContextView(
        String tenantId,
        String tenantCode,
        String tenantName,
        String tenantStatus,
        String operatorId,
        String operatorType,
        String defaultOrganizationId
) {
}

