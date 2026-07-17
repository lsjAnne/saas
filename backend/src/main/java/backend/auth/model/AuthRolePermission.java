package backend.auth.model;

public record AuthRolePermission(
        String roleCode,
        String permissionCode
) {
}

