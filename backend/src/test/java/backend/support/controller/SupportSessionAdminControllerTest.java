package backend.support.controller;

import backend.audit.application.AuditLogService;
import backend.saas.application.SaasTenantService;
import backend.support.application.SupportSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SupportSessionAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SupportSessionService supportSessionService;

    @Autowired
    private SaasTenantService saasTenantService;

    @Autowired
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        supportSessionService.clear();
        saasTenantService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldCreateSupportSession() throws Exception {
        String requestBody = """
                {
                  "tenantId": "tenant-a",
                  "reason": "排查订单异常",
                  "approver": "platform-manager",
                  "expiresAt": "2030-01-01T00:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/admin/support-sessions")
                        .header("X-Operator-Id", "platform-support-1")
                        .header("X-Operator-Type", "platform-support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void shouldApplyApproveListAndCloseSupportSession() throws Exception {
        String tenantId = registerTenant("support-center");

        MvcResult applyResult = mockMvc.perform(post("/api/support-sessions/apply")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "申请平台协助排查直播承诺风险",
                                  "requestedExpiresAt": "2030-01-01T00:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.tenantId").value(tenantId))
                .andExpect(jsonPath("$.data.requesterId").value("tenant-admin"))
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andReturn();

        String supportSessionId = objectMapper.readTree(applyResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText();

        mockMvc.perform(get("/api/admin/support-sessions")
                        .header("X-Operator-Id", "platform-support-1")
                        .header("X-Operator-Type", "platform-support"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(supportSessionId))
                .andExpect(jsonPath("$.data[0].status").value("pending"));

        mockMvc.perform(post("/api/admin/support-sessions/{id}/approve", supportSessionId)
                        .header("X-Operator-Id", "platform-support-1")
                        .header("X-Operator-Type", "platform-support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approver": "platform-support-1",
                                  "expiresAt": "2030-01-02T00:00:00Z",
                                  "approvalRemark": "approved by platform support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(supportSessionId))
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.approver").value("platform-support-1"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.approvalRemark").value("approved by platform support"));

        mockMvc.perform(get("/api/support-sessions")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(supportSessionId))
                .andExpect(jsonPath("$.data[0].status").value("approved"));

        mockMvc.perform(post("/api/admin/support-sessions/{id}/close", supportSessionId)
                        .header("X-Operator-Id", "platform-support-1")
                        .header("X-Operator-Type", "platform-support"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(supportSessionId))
                .andExpect(jsonPath("$.data.status").value("closed"))
                .andExpect(jsonPath("$.data.active").value(false));

        var auditActions = auditLogService.findByTenantId(tenantId).stream()
                .map(record -> record.actionType())
                .toList();
        assertTrue(auditActions.contains("REQUEST_SUPPORT_SESSION"));
        assertTrue(auditActions.contains("APPROVE_SUPPORT_SESSION"));
        assertTrue(auditActions.contains("CLOSE_SUPPORT_SESSION"));
    }

    private String registerTenant(String tenantName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "support-owner",
                                  "mobile": "13800000000"
                                }
                                """.formatted(tenantName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("tenantId")
                .asText();
    }
}

