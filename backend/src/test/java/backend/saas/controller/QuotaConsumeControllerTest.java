package backend.saas.controller;

import backend.audit.application.AuditLogService;
import backend.organization.application.OrganizationService;
import backend.saas.application.SaasTenantService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuotaConsumeControllerTest {

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
    void shouldBlockWhenQuotaExceeded() throws Exception {
        String tenantId = registerTenant();

        mockMvc.perform(post("/api/tenants/{id}/quotas/consume", tenantId)
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin-1")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quotaCode": "live_concurrency",
                                  "amount": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.usedAmount").value(1));

        mockMvc.perform(post("/api/tenants/{id}/quotas/consume", tenantId)
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin-1")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quotaCode": "live_concurrency",
                                  "amount": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("6002"))
                .andExpect(jsonPath("$.message").value("quota exceeded"))
                .andExpect(jsonPath("$.data.quotaCode").value("live_concurrency"))
                .andExpect(jsonPath("$.data.quotaLimit").value(1))
                .andExpect(jsonPath("$.data.usedAmount").value(1));
    }

    private String registerTenant() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "鏄熻景鐢靛晢",
                                  "ownerName": "寮犱笁",
                                  "mobile": "13800000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("tenantId").asText();
    }
}

