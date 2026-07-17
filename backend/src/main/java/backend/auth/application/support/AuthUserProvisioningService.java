package backend.auth.application.support;

import backend.auth.domain.repository.AuthUserRepository;
import backend.auth.model.AuthUser;
import backend.auth.security.PasswordHashService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AuthUserProvisioningService {

    private final AuthUserRepository authUserRepository;
    private final PasswordHashService passwordHashService;
    private final String bootstrapPassword;

    public AuthUserProvisioningService(AuthUserRepository authUserRepository,
                                       PasswordHashService passwordHashService,
                                       @Value("${app.auth.bootstrap-password:${APP_AUTH_BOOTSTRAP_PASSWORD:123456}}") String bootstrapPassword) {
        this.authUserRepository = authUserRepository;
        this.passwordHashService = passwordHashService;
        this.bootstrapPassword = bootstrapPassword;
    }

    public AuthUser provisionTenantOwner(String tenantId,
                                         String organizationId,
                                         String ownerName,
                                         String mobile) {
        OffsetDateTime now = OffsetDateTime.now();
        AuthUser existing = authUserRepository.findByUsername(mobile).orElse(null);
        if (existing != null) {
            return authUserRepository.save(existing.withOrganizationId(organizationId));
        }
        return authUserRepository.save(new AuthUser(
                mobile,
                mobile,
                passwordHashService.hash(bootstrapPassword),
                ownerName,
                "owner",
                "tenant-user",
                tenantId,
                organizationId,
                "active",
                now,
                now,
                null
        ));
    }

    public AuthUser provisionOrganizationMember(String tenantId,
                                                String organizationId,
                                                String userId,
                                                String userName,
                                                String mobile,
                                                String roleCode) {
        OffsetDateTime now = OffsetDateTime.now();
        String normalizedRoleCode = normalizeRoleCode(roleCode);
        String username = mobile == null || mobile.isBlank() ? userId : mobile;
        AuthUser existing = authUserRepository.findByUserId(userId)
                .or(() -> authUserRepository.findByUsername(username))
                .orElse(null);
        if (existing != null) {
            return authUserRepository.save(existing.withAccessProfile(
                    userName,
                    normalizedRoleCode,
                    operatorTypeForRole(normalizedRoleCode),
                    tenantId,
                    organizationId
            ));
        }
        return authUserRepository.save(new AuthUser(
                userId,
                username,
                passwordHashService.hash(bootstrapPassword),
                userName,
                normalizedRoleCode,
                operatorTypeForRole(normalizedRoleCode),
                tenantId,
                organizationId,
                "active",
                now,
                now,
                null
        ));
    }

    private String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return "operator";
        }
        return switch (roleCode.trim()) {
            case "tenant_admin" -> "admin";
            case "customer_service" -> "service";
            default -> roleCode.trim();
        };
    }

    private String operatorTypeForRole(String roleCode) {
        return switch (roleCode) {
            case "owner" -> "tenant-user";
            case "admin" -> "tenant-admin";
            case "service" -> "customer-service";
            case "platform_admin" -> "platform-admin";
            case "platform_ops" -> "platform-ops";
            case "platform_support" -> "platform-support";
            default -> roleCode;
        };
    }
}

