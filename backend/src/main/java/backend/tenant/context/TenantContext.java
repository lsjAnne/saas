package backend.tenant.context;

import java.util.Set;

public record TenantContext(
        String tenantId,
        String operatorId,
        String operatorType,
        String organizationId,
        String roleCode,
        String operatorName,
        Set<String> permissionCodes
) {

    public TenantContext(String tenantId, String operatorId, String operatorType) {
        this(tenantId, operatorId, operatorType, null, operatorType, operatorId, Set.of());
    }

    public boolean hasPermission(String permissionCode) {
        return permissionCodes != null && permissionCodes.contains(permissionCode);
    }
}

