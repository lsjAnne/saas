package com.dianshang.platform.tenant;

import com.dianshang.platform.audit.AuditLogService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TenantContextControllerTest {

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
    void shouldReturnCurrentTenantContext() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "星辰电商",
                                  "ownerName": "张三",
                                  "mobile": "13800000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tenantData = objectMapper.readTree(registerResult.getResponse().getContentAsString()).path("data");
        String tenantId = tenantData.path("tenantId").asText();

        mockMvc.perform(get("/api/tenant/context")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin-1")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.tenantId").value(tenantId))
                .andExpect(jsonPath("$.data.tenantName").value("星辰电商"))
                .andExpect(jsonPath("$.data.operatorId").value("tenant-admin-1"))
                .andExpect(jsonPath("$.data.operatorType").value("tenant-admin"));
    }
}
