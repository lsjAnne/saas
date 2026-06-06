package com.dianshang.platform.auth;

import com.dianshang.platform.auth.domain.repository.AuthRolePermissionRepository;
import com.dianshang.platform.auth.domain.repository.AuthUserRepository;
import com.dianshang.platform.auth.model.AuthRolePermission;
import com.dianshang.platform.auth.model.AuthUser;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AuthBootstrapService {

    private static final String BOOTSTRAP_PLATFORM_ADMIN_USER_ID = "bootstrap-platform-admin";

    private final AuthUserRepository authUserRepository;
    private final AuthRolePermissionRepository authRolePermissionRepository;
    private final PasswordHashService passwordHashService;
    private final String bootstrapAdminUsername;
    private final String bootstrapAdminPassword;
    private final String bootstrapAdminDisplayName;

    public AuthBootstrapService(AuthUserRepository authUserRepository,
                                AuthRolePermissionRepository authRolePermissionRepository,
                                PasswordHashService passwordHashService,
                                @Value("${app.auth.bootstrap-admin-username:${APP_AUTH_BOOTSTRAP_ADMIN_USERNAME:}}") String bootstrapAdminUsername,
                                @Value("${app.auth.bootstrap-admin-password:${APP_AUTH_BOOTSTRAP_ADMIN_PASSWORD:}}") String bootstrapAdminPassword,
                                @Value("${app.auth.bootstrap-admin-display-name:${APP_AUTH_BOOTSTRAP_ADMIN_DISPLAY_NAME:平台系统管理员}}") String bootstrapAdminDisplayName) {
        this.authUserRepository = authUserRepository;
        this.authRolePermissionRepository = authRolePermissionRepository;
        this.passwordHashService = passwordHashService;
        this.bootstrapAdminUsername = bootstrapAdminUsername;
        this.bootstrapAdminPassword = bootstrapAdminPassword;
        this.bootstrapAdminDisplayName = bootstrapAdminDisplayName;
    }

    @PostConstruct
    public void initialize() {
        seedRolePermissions();
        seedBootstrapAdmin();
    }

    public void resetForTests() {
        authUserRepository.deleteAll();
        authRolePermissionRepository.deleteAll();
        seedRolePermissions();
        seedBootstrapAdmin();
    }

    private void seedRolePermissions() {
        authRolePermissionRepository.saveAll(List.of(
                permission("owner", AuthPermissionCodes.TENANT_CONTEXT_READ),
                permission("owner", AuthPermissionCodes.ORGANIZATION_MANAGE),
                permission("owner", AuthPermissionCodes.SUBSCRIPTION_MANAGE),
                permission("owner", AuthPermissionCodes.TENANT_AUDIT_READ),
                permission("owner", AuthPermissionCodes.TENANT_AUDIT_EXPORT),
                permission("owner", AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE),
                permission("owner", AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE),
                permission("owner", AuthPermissionCodes.TENANT_COMPLIANCE_READ),
                permission("owner", AuthPermissionCodes.TENANT_COMPLIANCE_ACCEPT),
                permission("owner", AuthPermissionCodes.STORE_MANAGE),
                permission("owner", AuthPermissionCodes.CHANNEL_MANAGE),
                permission("owner", AuthPermissionCodes.PRODUCT_MANAGE),
                permission("owner", AuthPermissionCodes.SUPPLIER_MANAGE),
                permission("owner", AuthPermissionCodes.INVENTORY_MANAGE),
                permission("owner", AuthPermissionCodes.ORDER_MANAGE),
                permission("owner", AuthPermissionCodes.FULFILLMENT_MANAGE),
                permission("owner", AuthPermissionCodes.EXCEPTION_MANAGE),
                permission("owner", AuthPermissionCodes.SERVICECASE_MANAGE),
                permission("owner", AuthPermissionCodes.QA_MANAGE),
                permission("owner", AuthPermissionCodes.MEMBER_MANAGE),
                permission("owner", AuthPermissionCodes.CAMPAIGN_MANAGE),
                permission("owner", AuthPermissionCodes.LIVE_MANAGE),
                permission("owner", AuthPermissionCodes.RULE_MANAGE),
                permission("owner", AuthPermissionCodes.APPROVAL_MANAGE),
                permission("owner", AuthPermissionCodes.NOTIFICATION_MANAGE),
                permission("owner", AuthPermissionCodes.FINANCE_MANAGE),
                permission("owner", AuthPermissionCodes.DASHBOARD_READ),
                permission("owner", AuthPermissionCodes.OPENPLATFORM_MANAGE),

                permission("tenant_admin", AuthPermissionCodes.TENANT_CONTEXT_READ),
                permission("tenant_admin", AuthPermissionCodes.ORGANIZATION_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.SUBSCRIPTION_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.TENANT_AUDIT_READ),
                permission("tenant_admin", AuthPermissionCodes.TENANT_AUDIT_EXPORT),
                permission("tenant_admin", AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.TENANT_COMPLIANCE_READ),
                permission("tenant_admin", AuthPermissionCodes.TENANT_COMPLIANCE_ACCEPT),
                permission("tenant_admin", AuthPermissionCodes.STORE_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.CHANNEL_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.PRODUCT_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.SUPPLIER_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.INVENTORY_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.ORDER_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.FULFILLMENT_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.EXCEPTION_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.SERVICECASE_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.QA_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.MEMBER_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.CAMPAIGN_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.LIVE_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.RULE_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.APPROVAL_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.NOTIFICATION_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.FINANCE_MANAGE),
                permission("tenant_admin", AuthPermissionCodes.DASHBOARD_READ),
                permission("tenant_admin", AuthPermissionCodes.OPENPLATFORM_MANAGE),

                permission("admin", AuthPermissionCodes.TENANT_CONTEXT_READ),
                permission("admin", AuthPermissionCodes.ORGANIZATION_MANAGE),
                permission("admin", AuthPermissionCodes.SUBSCRIPTION_MANAGE),
                permission("admin", AuthPermissionCodes.TENANT_AUDIT_READ),
                permission("admin", AuthPermissionCodes.TENANT_AUDIT_EXPORT),
                permission("admin", AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE),
                permission("admin", AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE),
                permission("admin", AuthPermissionCodes.TENANT_COMPLIANCE_READ),
                permission("admin", AuthPermissionCodes.TENANT_COMPLIANCE_ACCEPT),
                permission("admin", AuthPermissionCodes.STORE_MANAGE),
                permission("admin", AuthPermissionCodes.CHANNEL_MANAGE),
                permission("admin", AuthPermissionCodes.PRODUCT_MANAGE),
                permission("admin", AuthPermissionCodes.SUPPLIER_MANAGE),
                permission("admin", AuthPermissionCodes.INVENTORY_MANAGE),
                permission("admin", AuthPermissionCodes.ORDER_MANAGE),
                permission("admin", AuthPermissionCodes.FULFILLMENT_MANAGE),
                permission("admin", AuthPermissionCodes.EXCEPTION_MANAGE),
                permission("admin", AuthPermissionCodes.SERVICECASE_MANAGE),
                permission("admin", AuthPermissionCodes.QA_MANAGE),
                permission("admin", AuthPermissionCodes.MEMBER_MANAGE),
                permission("admin", AuthPermissionCodes.CAMPAIGN_MANAGE),
                permission("admin", AuthPermissionCodes.LIVE_MANAGE),
                permission("admin", AuthPermissionCodes.RULE_MANAGE),
                permission("admin", AuthPermissionCodes.APPROVAL_MANAGE),
                permission("admin", AuthPermissionCodes.NOTIFICATION_MANAGE),
                permission("admin", AuthPermissionCodes.FINANCE_MANAGE),
                permission("admin", AuthPermissionCodes.DASHBOARD_READ),
                permission("admin", AuthPermissionCodes.OPENPLATFORM_MANAGE),

                permission("operator", AuthPermissionCodes.TENANT_CONTEXT_READ),
                permission("operator", AuthPermissionCodes.TENANT_COMPLIANCE_READ),
                permission("operator", AuthPermissionCodes.STORE_MANAGE),
                permission("operator", AuthPermissionCodes.CHANNEL_MANAGE),
                permission("operator", AuthPermissionCodes.PRODUCT_MANAGE),
                permission("operator", AuthPermissionCodes.SUPPLIER_MANAGE),
                permission("operator", AuthPermissionCodes.INVENTORY_MANAGE),
                permission("operator", AuthPermissionCodes.ORDER_MANAGE),
                permission("operator", AuthPermissionCodes.FULFILLMENT_MANAGE),
                permission("operator", AuthPermissionCodes.EXCEPTION_MANAGE),
                permission("operator", AuthPermissionCodes.CAMPAIGN_MANAGE),
                permission("operator", AuthPermissionCodes.LIVE_MANAGE),
                permission("operator", AuthPermissionCodes.RULE_MANAGE),
                permission("operator", AuthPermissionCodes.APPROVAL_MANAGE),
                permission("operator", AuthPermissionCodes.NOTIFICATION_MANAGE),
                permission("operator", AuthPermissionCodes.DASHBOARD_READ),

                permission("customer_service", AuthPermissionCodes.TENANT_CONTEXT_READ),
                permission("customer_service", AuthPermissionCodes.TENANT_COMPLIANCE_READ),
                permission("customer_service", AuthPermissionCodes.SERVICECASE_MANAGE),
                permission("customer_service", AuthPermissionCodes.QA_MANAGE),
                permission("customer_service", AuthPermissionCodes.MEMBER_MANAGE),
                permission("customer_service", AuthPermissionCodes.APPROVAL_MANAGE),
                permission("customer_service", AuthPermissionCodes.NOTIFICATION_MANAGE),

                permission("service", AuthPermissionCodes.TENANT_CONTEXT_READ),
                permission("service", AuthPermissionCodes.TENANT_COMPLIANCE_READ),
                permission("service", AuthPermissionCodes.SERVICECASE_MANAGE),
                permission("service", AuthPermissionCodes.QA_MANAGE),
                permission("service", AuthPermissionCodes.MEMBER_MANAGE),
                permission("service", AuthPermissionCodes.APPROVAL_MANAGE),
                permission("service", AuthPermissionCodes.NOTIFICATION_MANAGE),

                permission("platform_ops", AuthPermissionCodes.PLATFORM_TENANT_MANAGE),
                permission("platform_ops", AuthPermissionCodes.PLATFORM_SUPPORT_MANAGE),
                permission("platform_admin", AuthPermissionCodes.PLATFORM_TENANT_MANAGE),
                permission("platform_admin", AuthPermissionCodes.PLATFORM_SUPPORT_MANAGE),
                permission("platform_support", AuthPermissionCodes.PLATFORM_SUPPORT_MANAGE)
        ));
    }

    private void seedBootstrapAdmin() {
        if (bootstrapAdminUsername == null || bootstrapAdminUsername.isBlank()
                || bootstrapAdminPassword == null || bootstrapAdminPassword.isBlank()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        saveIfAbsent(new AuthUser(
                BOOTSTRAP_PLATFORM_ADMIN_USER_ID,
                bootstrapAdminUsername,
                passwordHashService.hash(bootstrapAdminPassword),
                bootstrapAdminDisplayName,
                "platform_admin",
                "platform-admin",
                null,
                null,
                "active",
                now,
                now,
                null
        ));
    }

    private AuthRolePermission permission(String roleCode, String permissionCode) {
        return new AuthRolePermission(roleCode, permissionCode);
    }

    private void saveIfAbsent(AuthUser authUser) {
        AuthUser existing = authUserRepository.findByUsername(authUser.username()).orElse(null);
        if (existing == null) {
            authUserRepository.save(authUser);
            return;
        }
        if (passwordHashService.needsRehash(existing.passwordHash())) {
            authUserRepository.save(existing.withPasswordHash(passwordHashService.hash(bootstrapAdminPassword), OffsetDateTime.now()));
        }
    }
}
