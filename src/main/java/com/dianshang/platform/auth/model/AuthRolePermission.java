package com.dianshang.platform.auth.model;

public record AuthRolePermission(
        String roleCode,
        String permissionCode
) {
}
