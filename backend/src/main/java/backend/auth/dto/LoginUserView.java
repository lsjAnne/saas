package backend.auth.dto;

import java.util.Set;

public record LoginUserView(
        String id,
        String name,
        String role,
        String tenantId,
        String organizationId,
        String operatorType,
        Set<String> permissionCodes
) {
}

