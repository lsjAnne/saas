package com.dianshang.platform.audit;

import com.dianshang.platform.auth.dto.LoginResponse;
import com.dianshang.platform.common.api.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class TenantAuditLogControllerTest {

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
    void shouldReturnOnlyCurrentTenantAuditLogs() throws Exception {
        String tenantA = registerTenant("星辰电商", "13800000001");
        String tenantB = registerTenant("远航电商", "13800000002");
        String tenantAToken = login("13800000001", "123456");
        String tenantBToken = login("13800000002", "123456");

        createOrganization(tenantAToken, "运营组");
        createOrganization(tenantBToken, "客服组");

        mockMvc.perform(get("/api/tenant/audit-logs")
                        .header("Authorization", "Bearer " + tenantAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].tenantId").value(tenantA))
                .andExpect(jsonPath("$.data[1].tenantId").value(tenantA))
                .andExpect(jsonPath("$.data[*].actionType").value(org.hamcrest.Matchers.hasItems(
                        "LOGIN_SUCCESS",
                        "CREATE_ORGANIZATION"
                )));
    }

    @Test
    void shouldExportCurrentTenantAuditLogsAsCsv() throws Exception {
        String tenantA = registerTenant("星辰电商", "13800000003");
        registerTenant("远航电商", "13800000004");
        String tenantAToken = login("13800000003", "123456");
        String tenantBToken = login("13800000004", "123456");

        createOrganization(tenantAToken, "导出组");
        createOrganization(tenantBToken, "隔离组");

        mockMvc.perform(get("/api/tenant/audit-logs/export")
                        .header("Authorization", "Bearer " + tenantAToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"tenant-audit-" + tenantA + ".csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CREATE_ORGANIZATION")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(tenantA)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("隔离组"))));
    }

    private String registerTenant(String tenantName, String mobile) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "张三",
                                  "mobile": "%s"
                                }
                                """.formatted(tenantName, mobile)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("tenantId").asText();
    }

    private void createOrganization(String token, String organizationName) throws Exception {
        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationName": "%s"
                                }
                                """.formatted(organizationName)))
                .andExpect(status().isOk());
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
