package backend.organization.controller;

import backend.audit.application.AuditLogService;
import backend.common.api.ApiResponse;
import backend.auth.dto.LoginResponse;
import backend.organization.application.OrganizationService;
import backend.saas.application.SaasTenantService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerTest {

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

    @BeforeEach
    void setUp() {
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldManageOrganizationMembersAndRoles() throws Exception {
        JsonNode tenantData = registerTenant();
        String tenantId = tenantData.path("tenantId").asText();
        String defaultOrganizationId = tenantData.path("defaultOrganizationId").asText();

        mockMvc.perform(post("/api/organizations")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationName": "operations-team"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.tenantId").value(tenantId))
                .andExpect(jsonPath("$.data.organizationName").value("operations-team"));

        mockMvc.perform(get("/api/organizations")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        MvcResult inviteResult = mockMvc.perform(post("/api/organizations/{id}/members/invite", defaultOrganizationId)
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-2001",
                                  "userName": "李四",
                                  "mobile": "13900000000",
                                  "roleCode": "operator"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("operator"))
                .andReturn();

        String memberId = objectMapper.readTree(inviteResult.getResponse().getContentAsString())
                .path("data")
                .path("memberId")
                .asText();

        mockMvc.perform(put("/api/organizations/{id}/members/{memberId}/role", defaultOrganizationId, memberId)
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleCode": "admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("admin"));

        mockMvc.perform(get("/api/organizations/{id}/members", defaultOrganizationId)
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].roleCode").value("admin"));
    }

    @Test
    void shouldProvisionInvitedMemberLoginAndApplyRbacAfterRoleChange() throws Exception {
        JsonNode tenantData = registerTenant();
        String tenantId = tenantData.path("tenantId").asText();
        String defaultOrganizationId = tenantData.path("defaultOrganizationId").asText();
        String ownerToken = login("13800000000", BOOTSTRAP_PASSWORD);

        MvcResult inviteResult = mockMvc.perform(post("/api/organizations/{id}/members/invite", defaultOrganizationId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-3001",
                                  "userName": "运营专员",
                                  "mobile": "13900000000",
                                  "roleCode": "operator"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("operator"))
                .andReturn();

        String memberId = objectMapper.readTree(inviteResult.getResponse().getContentAsString())
                .path("data")
                .path("memberId")
                .asText();

        String operatorToken = login("13900000000", BOOTSTRAP_PASSWORD);

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("user-3001"))
                .andExpect(jsonPath("$.data.role").value("operator"))
                .andExpect(jsonPath("$.data.tenantId").value(tenantId));

        mockMvc.perform(get("/api/tenants/{id}/subscription", tenantId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1009"));

        mockMvc.perform(put("/api/organizations/{id}/members/{memberId}/role", defaultOrganizationId, memberId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleCode": "admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("admin"));

        String adminToken = login("13900000000", BOOTSTRAP_PASSWORD);

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("user-3001"))
                .andExpect(jsonPath("$.data.role").value("admin"))
                .andExpect(jsonPath("$.data.permissionCodes").isArray());

        mockMvc.perform(get("/api/tenants/{id}/subscription", tenantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(tenantId));
    }

    @Test
    void shouldRejectUnsupportedTenantRoleOnInvite() throws Exception {
        JsonNode tenantData = registerTenant();
        String defaultOrganizationId = tenantData.path("defaultOrganizationId").asText();
        String ownerToken = login("13800000000", BOOTSTRAP_PASSWORD);

        mockMvc.perform(post("/api/organizations/{id}/members/invite", defaultOrganizationId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-4001",
                                  "userName": "platform-role-user",
                                  "mobile": "13900000001",
                                  "roleCode": "platform_admin"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1002"))
                .andExpect(jsonPath("$.message").value("unsupported tenant role"));
    }

    @Test
    void shouldRejectOwnerPromotionByNonOwnerAdmin() throws Exception {
        JsonNode tenantData = registerTenant();
        String defaultOrganizationId = tenantData.path("defaultOrganizationId").asText();
        String ownerToken = login("13800000000", BOOTSTRAP_PASSWORD);

        MvcResult adminInviteResult = mockMvc.perform(post("/api/organizations/{id}/members/invite", defaultOrganizationId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-4002",
                                  "userName": "admin-user",
                                  "mobile": "13900000002",
                                  "roleCode": "admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String adminMemberId = objectMapper.readTree(adminInviteResult.getResponse().getContentAsString())
                .path("data")
                .path("memberId")
                .asText();

        MvcResult operatorInviteResult = mockMvc.perform(post("/api/organizations/{id}/members/invite", defaultOrganizationId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "user-4003",
                                  "userName": "operator-user",
                                  "mobile": "13900000003",
                                  "roleCode": "operator"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String operatorMemberId = objectMapper.readTree(operatorInviteResult.getResponse().getContentAsString())
                .path("data")
                .path("memberId")
                .asText();

        String adminToken = login("13900000002", BOOTSTRAP_PASSWORD);

        mockMvc.perform(put("/api/organizations/{id}/members/{memberId}/role", defaultOrganizationId, operatorMemberId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleCode": "owner"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1009"));

        mockMvc.perform(get("/api/organizations/{id}/members", defaultOrganizationId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[1].memberId").value(adminMemberId))
                .andExpect(jsonPath("$.data[1].roleCode").value("admin"))
                .andExpect(jsonPath("$.data[2].memberId").value(operatorMemberId))
                .andExpect(jsonPath("$.data[2].roleCode").value("operator"));
    }

    private JsonNode registerTenant() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "organization-center",
                                  "ownerName": "张三",
                                  "mobile": "13800000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
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

