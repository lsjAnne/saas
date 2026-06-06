package com.dianshang.platform.auth;

import com.dianshang.platform.auth.domain.repository.AuthRolePermissionRepository;
import com.dianshang.platform.auth.domain.repository.AuthUserRepository;
import com.dianshang.platform.auth.model.AuthUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthUserAccessService {

    private static final List<String> TENANT_ROLE_CODES = List.of("owner", "admin", "operator", "service");

    private final AuthUserRepository authUserRepository;
    private final AuthRolePermissionRepository authRolePermissionRepository;

    public AuthUserAccessService(AuthUserRepository authUserRepository,
                                 AuthRolePermissionRepository authRolePermissionRepository) {
        this.authUserRepository = authUserRepository;
        this.authRolePermissionRepository = authRolePermissionRepository;
    }

    public AuthenticatedUserView load(String userId) {
        AuthUser authUser = authUserRepository.findByUserId(userId).orElse(null);
        if (authUser == null || !"active".equals(authUser.status())) {
            return null;
        }
        Set<String> permissions = permissionsForRole(authUser.roleCode());
        return new AuthenticatedUserView(authUser, permissions);
    }

    public AuthenticatedUserView loadLegacy(String tenantId, String operatorId, String operatorType) {
        String roleCode = switch (operatorType) {
            case "platform-ops" -> "platform_ops";
            case "platform-admin" -> "platform_admin";
            case "platform-support" -> "platform_support";
            case "tenant-admin" -> "tenant_admin";
            case "tenant-user" -> "owner";
            default -> operatorType.replace('-', '_');
        };
        AuthUser authUser = new AuthUser(
                operatorId,
                operatorId,
                "legacy-header-context",
                operatorId,
                roleCode,
                operatorType,
                "platform".equals(tenantId) ? null : tenantId,
                null,
                "active",
                null,
                null,
                null
        );
        return new AuthenticatedUserView(authUser, permissionsForRole(roleCode));
    }

    private Set<String> permissionsForRole(String roleCode) {
        return authRolePermissionRepository.findByRoleCode(roleCode).stream()
                .map(permission -> permission.permissionCode())
                .collect(Collectors.toSet());
    }

    public List<RolePermissionMatrixView> listTenantRolePermissionMatrix() {
        return TENANT_ROLE_CODES.stream()
                .map(roleCode -> new RolePermissionMatrixView(roleCode, permissionsForRole(roleCode)))
                .toList();
    }

    public AccessCheckView checkAccess(String userId, String permissionCode) {
        AuthenticatedUserView userView = load(userId);
        boolean granted = userView != null && userView.permissionCodes().contains(permissionCode);
        return new AccessCheckView(
                permissionCode,
                granted,
                userView == null ? null : userView.authUser().roleCode(),
                false,
                false
        );
    }

    public String normalizeTenantRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return "operator";
        }
        return switch (roleCode.trim()) {
            case "tenant_admin", "admin" -> "admin";
            case "customer_service", "service" -> "service";
            case "owner" -> "owner";
            case "operator" -> "operator";
            default -> throw new IllegalArgumentException("unsupported tenant role");
        };
    }

    public boolean canAssignTenantRole(String actorRoleCode, String targetRoleCode) {
        String normalizedActorRole = actorRoleCode == null ? "" : actorRoleCode.trim();
        String normalizedTargetRole = normalizeTenantRole(targetRoleCode);
        return switch (normalizedActorRole) {
            case "owner" -> true;
            case "tenant_admin", "admin" -> !"owner".equals(normalizedTargetRole);
            default -> false;
        };
    }

    public record RolePermissionMatrixView(
            String roleCode,
            Set<String> permissionCodes
    ) {
    }

    public record AccessCheckView(
            String permissionCode,
            boolean granted,
            String roleCode,
            boolean requiresSecondaryConfirmation,
            boolean secondaryConfirmationActive
    ) {
    }
}
