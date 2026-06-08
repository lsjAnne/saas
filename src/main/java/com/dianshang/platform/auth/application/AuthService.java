package com.dianshang.platform.auth.application;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.auth.AuthTokenService;
import com.dianshang.platform.auth.AuthUserAccessService;
import com.dianshang.platform.auth.AuthenticatedUserView;
import com.dianshang.platform.auth.AuthSecurityConfigVerifier;
import com.dianshang.platform.auth.PasswordHashService;
import com.dianshang.platform.auth.AuthUserAccessService.AccessCheckView;
import com.dianshang.platform.auth.AuthUserAccessService.RolePermissionMatrixView;
import com.dianshang.platform.auth.domain.repository.AuthUserRepository;
import com.dianshang.platform.auth.dto.LoginRequest;
import com.dianshang.platform.auth.dto.LoginResponse;
import com.dianshang.platform.auth.dto.LoginUserView;
import com.dianshang.platform.auth.model.AuthUser;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AuthService {
    private static final Set<String> SENSITIVE_PERMISSION_CODES = Set.of(
            AuthPermissionCodes.TENANT_AUDIT_EXPORT,
            AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE,
            AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE,
            AuthPermissionCodes.OPENPLATFORM_MANAGE
    );

    private final AuditLogService auditLogService;
    private final AuthUserRepository authUserRepository;
    private final PasswordHashService passwordHashService;
    private final AuthTokenService authTokenService;
    private final AuthUserAccessService authUserAccessService;
    private final AuthSecurityConfigVerifier authSecurityConfigVerifier;
    private final ConcurrentMap<String, OffsetDateTime> sensitiveOperationConfirmations = new ConcurrentHashMap<>();

    public AuthService(AuditLogService auditLogService,
                       AuthUserRepository authUserRepository,
                       PasswordHashService passwordHashService,
                       AuthTokenService authTokenService,
                       AuthUserAccessService authUserAccessService,
                       AuthSecurityConfigVerifier authSecurityConfigVerifier) {
        this.auditLogService = auditLogService;
        this.authUserRepository = authUserRepository;
        this.passwordHashService = passwordHashService;
        this.authTokenService = authTokenService;
        this.authUserAccessService = authUserAccessService;
        this.authSecurityConfigVerifier = authSecurityConfigVerifier;
    }

    public LoginResponse login(LoginRequest request) {
        AuthUser authUser = authUserRepository.findByUsername(request.username()).orElse(null);
        if (authUser == null || !"active".equals(authUser.status())) {
            throw new BusinessException("1001", "未登录", HttpStatus.UNAUTHORIZED);
        }
        if (!passwordHashService.matches(request.password(), authUser.passwordHash())) {
            throw new BusinessException("1001", "未登录", HttpStatus.UNAUTHORIZED);
        }
        AuthUser updated = authUserRepository.save(authUser.withLastLoginAt(passwordHashService.now()));
        auditLogService.recordForTenant(
                updated.tenantId() == null || updated.tenantId().isBlank() ? "platform" : updated.tenantId(),
                "LOGIN_SUCCESS",
                "auth_user",
                updated.userId()
        );
        return new LoginResponse(
                authTokenService.issue(updated.userId()),
                toLoginUserView(updated)
        );
    }

    public Map<String, Object> logout() {
        TenantContext context = TenantAccessSupport.requiredContext();
        auditLogService.record("LOGOUT_SUCCESS", "auth_user", context.operatorId());
        return Map.of("loggedOut", true);
    }

    public LoginUserView currentUser() {
        TenantContext context = TenantAccessSupport.requiredContext();
        String tenantId = "platform".equals(context.tenantId()) ? null : context.tenantId();
        return new LoginUserView(
                context.operatorId(),
                context.operatorName(),
                context.roleCode(),
                tenantId,
                context.organizationId(),
                context.operatorType(),
                context.permissionCodes()
        );
    }

    public PasswordChangeResult changePassword(ChangePasswordRequest request) {
        TenantContext context = TenantAccessSupport.requiredContext();
        AuthUser authUser = authUserRepository.findByUserId(context.operatorId())
                .orElseThrow(() -> new BusinessException("1001", "未登录", HttpStatus.UNAUTHORIZED));
        if (!passwordHashService.matches(request.currentPassword(), authUser.passwordHash())) {
            throw new BusinessException("1001", "current password invalid", HttpStatus.UNAUTHORIZED);
        }
        if (!passwordHashService.isStrongPassword(request.newPassword())) {
            throw new BusinessException("1002", "new password must be at least 12 chars and contain letters and digits", HttpStatus.BAD_REQUEST);
        }
        if (passwordHashService.matches(request.newPassword(), authUser.passwordHash())) {
            throw new BusinessException("1002", "new password must be different from current password", HttpStatus.BAD_REQUEST);
        }
        OffsetDateTime changedAt = passwordHashService.now();
        authUserRepository.save(authUser.withPasswordHash(passwordHashService.hash(request.newPassword()), changedAt));
        auditLogService.recordForTenant(
                authUser.tenantId() == null || authUser.tenantId().isBlank() ? "platform" : authUser.tenantId(),
                "CHANGE_PASSWORD",
                "auth_user",
                authUser.userId()
        );
        return new PasswordChangeResult(true, changedAt);
    }

    public AuthSecurityStatusView getSecurityStatus() {
        TenantAccessSupport.requiredContext();
        return new AuthSecurityStatusView(
                authSecurityConfigVerifier.isTokenSecretStrong(),
                authSecurityConfigVerifier.isBootstrapPasswordStrong(),
                authSecurityConfigVerifier.requireExplicitSecrets(),
                authSecurityConfigVerifier.isLegacyHeaderContextEnabled()
        );
    }

    public List<RolePermissionMatrixView> listTenantRolePermissionMatrix() {
        TenantAccessSupport.requiredContext();
        return authUserAccessService.listTenantRolePermissionMatrix();
    }

    public AccessCheckView checkAccess(String permissionCode) {
        TenantContext context = TenantAccessSupport.requiredContext();
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new BusinessException("1002", "permissionCode is required", HttpStatus.BAD_REQUEST);
        }
        String normalizedPermissionCode = permissionCode.trim();
        AccessCheckView accessCheckView = authUserAccessService.checkAccess(context.operatorId(), normalizedPermissionCode);
        return new AccessCheckView(
                accessCheckView.permissionCode(),
                accessCheckView.granted(),
                accessCheckView.roleCode(),
                requiresSecondaryConfirmation(normalizedPermissionCode),
                hasActiveSensitiveOperationConfirmation(context.operatorId(), normalizedPermissionCode)
        );
    }

    public SensitiveOperationConfirmationView confirmSensitiveOperation(SensitiveOperationConfirmationRequest request) {
        TenantContext context = TenantAccessSupport.requiredContext();
        String normalizedPermissionCode = request.permissionCode() == null ? "" : request.permissionCode().trim();
        if (normalizedPermissionCode.isBlank()) {
            throw new BusinessException("1002", "permissionCode is required", HttpStatus.BAD_REQUEST);
        }
        if (!context.hasPermission(normalizedPermissionCode)) {
            throw new BusinessException("1009", "权限不足", HttpStatus.FORBIDDEN);
        }
        if (!requiresSecondaryConfirmation(normalizedPermissionCode)) {
            throw new BusinessException("1002", "permissionCode does not require secondary confirmation", HttpStatus.BAD_REQUEST);
        }
        AuthUser authUser = authUserRepository.findByUserId(context.operatorId())
                .orElseThrow(() -> new BusinessException("1001", "未登录", HttpStatus.UNAUTHORIZED));
        if (!passwordHashService.matches(request.currentPassword(), authUser.passwordHash())) {
            throw new BusinessException("1001", "current password invalid", HttpStatus.UNAUTHORIZED);
        }
        OffsetDateTime expiresAt = passwordHashService.now().plusMinutes(10);
        sensitiveOperationConfirmations.put(sensitiveConfirmationKey(context.operatorId(), normalizedPermissionCode), expiresAt);
        auditLogService.recordForTenant(
                authUser.tenantId() == null || authUser.tenantId().isBlank() ? "platform" : authUser.tenantId(),
                "CONFIRM_SENSITIVE_OPERATION",
                "tenant_permission",
                normalizedPermissionCode
        );
        return new SensitiveOperationConfirmationView(normalizedPermissionCode, true, expiresAt);
    }

    public void assertSensitiveOperationConfirmed(String userId, String permissionCode) {
        if (!requiresSecondaryConfirmation(permissionCode)) {
            return;
        }
        if (hasActiveSensitiveOperationConfirmation(userId, permissionCode)) {
            return;
        }
        throw new BusinessException("1015", "sensitive operation confirmation required", HttpStatus.FORBIDDEN);
    }

    public boolean requiresSecondaryConfirmation(String permissionCode) {
        return permissionCode != null && SENSITIVE_PERMISSION_CODES.contains(permissionCode.trim());
    }

    public boolean hasActiveSensitiveOperationConfirmation(String userId, String permissionCode) {
        if (userId == null || userId.isBlank() || permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        String key = sensitiveConfirmationKey(userId, permissionCode.trim());
        OffsetDateTime expiresAt = sensitiveOperationConfirmations.get(key);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isAfter(passwordHashService.now())) {
            return true;
        }
        sensitiveOperationConfirmations.remove(key);
        return false;
    }

    public void clearSensitiveOperationConfirmations() {
        sensitiveOperationConfirmations.clear();
    }

    private String sensitiveConfirmationKey(String userId, String permissionCode) {
        return userId + "::" + permissionCode;
    }

    private LoginUserView toLoginUserView(AuthUser authUser) {
        AuthenticatedUserView userView = authUserAccessService.load(authUser.userId());
        return new LoginUserView(
                authUser.userId(),
                authUser.displayName(),
                authUser.roleCode(),
                authUser.tenantId(),
                authUser.organizationId(),
                authUser.operatorType(),
                userView == null ? java.util.Set.of() : userView.permissionCodes()
        );
    }

    public record ChangePasswordRequest(
            String currentPassword,
            String newPassword
    ) {
    }

    public record PasswordChangeResult(
            boolean changed,
            OffsetDateTime changedAt
    ) {
    }

    public record AuthSecurityStatusView(
            boolean tokenSecretStrong,
            boolean bootstrapPasswordStrong,
            boolean requireExplicitSecrets,
            boolean legacyHeaderContextEnabled
    ) {
    }

    public record SensitiveOperationConfirmationRequest(
            String currentPassword,
            String permissionCode
    ) {
    }

    public record SensitiveOperationConfirmationView(
            String permissionCode,
            boolean confirmed,
            OffsetDateTime expiresAt
    ) {
    }
}
