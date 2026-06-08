package com.dianshang.platform.system;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.notification.application.NotificationApplicationService;
import com.dianshang.platform.notification.application.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TenantSystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationApplicationService notificationApplicationService;

    @Test
    void shouldReturnTenantHealth() throws Exception {
        mockMvc.perform(get("/api/tenant/system/health")
                        .header("X-Tenant-Id", "tenant-a")
                        .header("X-Operator-Id", "owner-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void shouldReturnTenantObservabilityOverview() throws Exception {
        auditLogService.clear();
        notificationApplicationService.clear();
        auditLogService.recordForTenant("tenant-9001", "OBS_AUDIT", "system", "obs-1");
        notificationApplicationService.sendNotification(
                "tenant-9001",
                new NotificationService.SendNotificationRequest(
                        "email",
                        "manual_notice_email",
                        "ops-fail@example.com",
                        "{\"forceFail\":true}",
                        "high",
                        null
                )
        );

        mockMvc.perform(get("/api/tenant/system/observability-overview")
                        .header("X-Tenant-Id", "tenant-9001")
                        .header("X-Operator-Id", "owner-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value("tenant-9001"))
                .andExpect(jsonPath("$.data.auditLogCount").value(2))
                .andExpect(jsonPath("$.data.failedNotificationCount").value(1))
                .andExpect(jsonPath("$.data.openPlatformCallLogCount").value(0))
                .andExpect(jsonPath("$.data.rejectedOpenPlatformCallCount").value(0))
                .andExpect(jsonPath("$.data.liveRiskEventCount").value(0))
                .andExpect(jsonPath("$.data.activeAlertCount").value(1))
                .andExpect(jsonPath("$.data.latestTraceIds.length()").value(0))
                .andExpect(jsonPath("$.data.configuredGatewayCount").value(4))
                .andExpect(jsonPath("$.data.enabledGatewayCount").value(4))
                .andExpect(jsonPath("$.data.mockGatewayCount").value(4))
                .andExpect(jsonPath("$.data.prometheusEndpointEnabled").value(true))
                .andExpect(jsonPath("$.data.supportedDeploymentModes[0]").value("standard-saas"));
    }

    @Test
    void shouldExposeActuatorObservabilityEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components.externalDependencies.status").value("UP"));

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalDependencies.notificationGatewayCount").value(4))
                .andExpect(jsonPath("$.externalDependencies.enabledNotificationGatewayCount").value(4))
                .andExpect(jsonPath("$.externalDependencies.supportedDeploymentModes[0]").value("standard-saas"));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("# HELP")));
    }
}
