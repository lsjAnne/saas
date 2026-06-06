package com.dianshang.platform.auth;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.auth.dto.LoginResponse;
import com.dianshang.platform.auth.application.AuthService;
import com.dianshang.platform.saas.dto.RegisterTenantResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.saas.application.SaasTenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String BOOTSTRAP_PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaasTenantService saasTenantService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
        authService.clearSensitiveOperationConfirmations();
    }

    @Test
    void shouldRejectRemovedDemoAccount() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("1001"));
    }

    @Test
    void shouldLoginWithRegisteredTenantOwner() throws Exception {
        registerTenant("正式租户", "老板", "13800000000");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "13800000000",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.name").value("老板"))
                .andExpect(jsonPath("$.data.user.role").value("owner"))
                .andExpect(jsonPath("$.data.user.tenantId").value("tenant-1001"))
                .andExpect(jsonPath("$.data.user.permissionCodes").isArray())
                .andExpect(jsonPath("$.data.user.permissionCodes").value(org.hamcrest.Matchers.hasItems(
                        AuthPermissionCodes.STORE_MANAGE,
                        AuthPermissionCodes.SUBSCRIPTION_MANAGE,
                        AuthPermissionCodes.DASHBOARD_READ
                )));
    }

    @Test
    void shouldReturnCurrentUserAndLogoutWithBearerToken() throws Exception {
        registerTenant("正式租户", "老板", "13800000000");
        String token = login("13800000000", BOOTSTRAP_PASSWORD);

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("13800000000"))
                .andExpect(jsonPath("$.data.operatorType").value("tenant-user"))
                .andExpect(jsonPath("$.data.tenantId").value("tenant-1001"))
                .andExpect(jsonPath("$.data.permissionCodes").isArray())
                .andExpect(jsonPath("$.data.permissionCodes").value(org.hamcrest.Matchers.hasItems(
                        AuthPermissionCodes.STORE_MANAGE,
                        AuthPermissionCodes.SUBSCRIPTION_MANAGE,
                        AuthPermissionCodes.DASHBOARD_READ
                )));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loggedOut").value(true));
    }

    @Test
    void shouldChangePasswordInvalidateOldPasswordAndAllowNewPasswordLogin() throws Exception {
        registerTenant("tenant-a", "owner-a", "13800000000");
        String token = login("13800000000", BOOTSTRAP_PASSWORD);

        mockMvc.perform(post("/api/auth/password/change")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "123456",
                                  "newPassword": "StrongPassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.changed").value(true))
                .andExpect(jsonPath("$.data.changedAt").exists());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "13800000000",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("1001"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "13800000000",
                                  "password": "StrongPassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists());
    }

    @Test
    void shouldExposeSecurityStatusRoleMatrixAndAccessChecks() throws Exception {
        registerTenant("tenant-a", "owner-a", "13800000000");
        String token = login("13800000000", BOOTSTRAP_PASSWORD);

        mockMvc.perform(get("/api/auth/security-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenSecretStrong").value(false))
                .andExpect(jsonPath("$.data.bootstrapPasswordStrong").value(false))
                .andExpect(jsonPath("$.data.requireExplicitSecrets").value(false))
                .andExpect(jsonPath("$.data.legacyHeaderContextEnabled").value(true));

        mockMvc.perform(get("/api/auth/rbac/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].roleCode").value("owner"))
                .andExpect(jsonPath("$.data[1].roleCode").value("admin"))
                .andExpect(jsonPath("$.data[2].roleCode").value("operator"))
                .andExpect(jsonPath("$.data[3].roleCode").value("service"))
                .andExpect(jsonPath("$.data[0].permissionCodes").value(org.hamcrest.Matchers.hasItems(
                        AuthPermissionCodes.SUBSCRIPTION_MANAGE,
                        AuthPermissionCodes.OPENPLATFORM_MANAGE
                )))
                .andExpect(jsonPath("$.data[3].permissionCodes").value(org.hamcrest.Matchers.hasItems(
                        AuthPermissionCodes.SERVICECASE_MANAGE,
                        AuthPermissionCodes.MEMBER_MANAGE
                )));

        mockMvc.perform(get("/api/auth/access-checks")
                        .header("Authorization", "Bearer " + token)
                        .param("permissionCode", AuthPermissionCodes.SUBSCRIPTION_MANAGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCode").value(AuthPermissionCodes.SUBSCRIPTION_MANAGE))
                .andExpect(jsonPath("$.data.granted").value(true))
                .andExpect(jsonPath("$.data.roleCode").value("owner"))
                .andExpect(jsonPath("$.data.requiresSecondaryConfirmation").value(false))
                .andExpect(jsonPath("$.data.secondaryConfirmationActive").value(false));

        mockMvc.perform(get("/api/auth/access-checks")
                        .header("Authorization", "Bearer " + token)
                        .param("permissionCode", AuthPermissionCodes.PLATFORM_TENANT_MANAGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCode").value(AuthPermissionCodes.PLATFORM_TENANT_MANAGE))
                .andExpect(jsonPath("$.data.granted").value(false))
                .andExpect(jsonPath("$.data.roleCode").value("owner"))
                .andExpect(jsonPath("$.data.requiresSecondaryConfirmation").value(false))
                .andExpect(jsonPath("$.data.secondaryConfirmationActive").value(false));

        mockMvc.perform(get("/api/auth/access-checks")
                        .header("Authorization", "Bearer " + token)
                        .param("permissionCode", AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCode").value(AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE))
                .andExpect(jsonPath("$.data.granted").value(true))
                .andExpect(jsonPath("$.data.roleCode").value("owner"))
                .andExpect(jsonPath("$.data.requiresSecondaryConfirmation").value(true))
                .andExpect(jsonPath("$.data.secondaryConfirmationActive").value(false));

        mockMvc.perform(post("/api/auth/sensitive-operation-confirmations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "123456",
                                  "permissionCode": "tenant.lifecycle.manage"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCode").value("tenant.lifecycle.manage"))
                .andExpect(jsonPath("$.data.confirmed").value(true))
                .andExpect(jsonPath("$.data.expiresAt").isString());

        mockMvc.perform(get("/api/auth/access-checks")
                        .header("Authorization", "Bearer " + token)
                        .param("permissionCode", AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCode").value(AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE))
                .andExpect(jsonPath("$.data.granted").value(true))
                .andExpect(jsonPath("$.data.roleCode").value("owner"))
                .andExpect(jsonPath("$.data.requiresSecondaryConfirmation").value(true))
                .andExpect(jsonPath("$.data.secondaryConfirmationActive").value(true));
    }

    @Test
    void shouldRejectBlankPermissionCodeForAccessCheck() throws Exception {
        registerTenant("tenant-a", "owner-a", "13800000000");
        String token = login("13800000000", BOOTSTRAP_PASSWORD);

        mockMvc.perform(get("/api/auth/access-checks")
                        .header("Authorization", "Bearer " + token)
                        .param("permissionCode", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("must not be blank"));
    }

    private RegisterTenantResponse registerTenant(String tenantName, String ownerName, String mobile) throws Exception {
        String content = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "%s",
                                  "mobile": "%s"
                                }
                                """.formatted(tenantName, ownerName, mobile)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ApiResponse<RegisterTenantResponse> response = objectMapper.readValue(
                content,
                new TypeReference<>() {
                }
        );
        return response.data();
    }

    private String login(String username, String password) throws Exception {
        String content = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ApiResponse<LoginResponse> response = objectMapper.readValue(
                content,
                new TypeReference<>() {
                }
        );
        return response.data().token();
    }
}
