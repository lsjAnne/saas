package com.dianshang.platform.admin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.auth.application.AuthService;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.saas.domain.repository.BillingOrderRepository;
import com.dianshang.platform.saas.domain.repository.TenantProfileRepository;
import com.dianshang.platform.saas.domain.repository.TenantSubscriptionRepository;
import com.dianshang.platform.saas.model.TenantProfile;
import com.dianshang.platform.saas.model.TenantSubscription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.release.automation-evidence.suite-count=3",
        "app.release.automation-evidence.environment=server-local",
        "app.release.automation-evidence.executed-at=2026-06-08T09:59:30+08:00",
        "app.release.automation-evidence.summary=auth/openplatform/compliance regression suites passed",
        "app.integrations.external.systems.erp.endpoint=https://erp.example.com/api",
        "app.integrations.external.systems.erp.credential-configured=true",
        "app.integrations.external.systems.wms.endpoint=https://wms.example.com/api",
        "app.integrations.external.systems.wms.credential-configured=true",
        "app.integrations.external.systems.tax.endpoint=https://tax.example.com/api",
        "app.integrations.external.systems.tax.credential-configured=true",
        "app.integrations.external.systems.messaging.endpoint=https://message.example.com/api",
        "app.integrations.external.systems.messaging.credential-configured=true",
        "app.integrations.external.systems.messaging.callback-url=https://callback.example.com/messages",
        "app.integrations.external.systems.bi.endpoint=https://bi.example.com/api",
        "app.integrations.external.systems.bi.credential-configured=true",
        "app.integrations.external.erp.provider=ofbiz",
        "app.integrations.external.erp.endpoint=https://ofbiz.example.com/webtools/control",
        "app.integrations.external.erp.party-sync-enabled=true",
        "app.integrations.external.erp.order-sync-mode=near_real_time",
        "app.integrations.external.erp.ledger-mapping-count=8",
        "app.integrations.external.erp.catalog-export-enabled=true",
        "app.integrations.external.wms.provider=openboxes",
        "app.integrations.external.wms.endpoint=https://openboxes.example.com/openboxes/api",
        "app.integrations.external.wms.facility-count=5",
        "app.integrations.external.wms.stock-sync-mode=two_way",
        "app.integrations.external.wms.outbound-flow=wave_and_pick",
        "app.integrations.external.wms.batch-tracking-enabled=true",
        "app.integrations.external.messaging.provider=rabbitmq",
        "app.integrations.external.messaging.endpoint=amqps://rabbitmq.example.com:5671",
        "app.integrations.external.messaging.virtual-host=tenant-hub",
        "app.integrations.external.messaging.exchange=tenant.events",
        "app.integrations.external.messaging.queue-count=4",
        "app.integrations.external.messaging.callback-bridge-enabled=true",
        "app.integrations.external.messaging.dead-letter-enabled=true",
        "app.integrations.external.messaging.callback-worker-enabled=true",
        "app.integrations.external.messaging.callback-worker-provider=spring-event",
        "app.integrations.external.messaging.callback-worker-endpoint=http://callback-worker.example.internal/consume",
        "app.integrations.external.messaging.callback-worker-consumer-group=open-platform-callbacks",
        "app.integrations.external.systems.routing.endpoint=https://router.example.com",
        "app.integrations.external.systems.routing.credential-configured=true",
        "app.integrations.external.routing.endpoint=https://router.example.com",
        "app.observability.log-aggregation-endpoint=https://logs.example.com",
        "app.observability.trace-endpoint=https://trace.example.com",
        "app.observability.alert-router-endpoint=https://alerts.example.com",
        "app.observability.dashboard-url=https://grafana.example.com",
        "app.delivery.github-owner=lsjAnne",
        "app.delivery.github-repository=saas",
        "app.delivery.registry=ghcr.io",
        "app.delivery.image-repository=lsjAnne/dian-shang-ping-tai",
        "app.delivery.release-key-configured=true",
        "app.delivery.registry-auth-configured=true",
        "app.delivery.canary-enabled=true",
        "app.delivery.canary-strategy=header-weighted",
        "app.delivery.standard-saas-base-url=https://saas.example.com",
        "app.delivery.standard-saas-verified-at=2026-06-10T15:10:00+08:00",
        "app.delivery.private-base-url=https://private.example.com",
        "app.delivery.private-verified-at=2026-06-10T15:25:00+08:00"
})
@AutoConfigureMockMvc
class AdminTenantControllerTest {
    private static final String DEFAULT_DELIVERY_GITHUB_PUBLISH_MODE_SOURCE = "default-delivery-github-publish-mode-test";

    private static final HttpServer EXTERNAL_HTTP_SERVER = createExternalHttpServer();
    private static final int HTTP_PORT = EXTERNAL_HTTP_SERVER.getAddress().getPort();
    private static final TcpProbeServer RABBITMQ_TCP_SERVER = createRabbitMqProbeServer();

    @DynamicPropertySource
    static void registerExternalProbeProperties(DynamicPropertyRegistry registry) {
        String httpBase = "http://127.0.0.1:" + HTTP_PORT;
        String messagingEndpoint = "amqp://127.0.0.1:" + RABBITMQ_TCP_SERVER.port() + "/tenant-hub";
        registry.add("app.integrations.external.systems.erp.endpoint", () -> httpBase + "/ofbiz/webtools/control");
        registry.add("app.integrations.external.systems.wms.endpoint", () -> httpBase + "/openboxes/api");
        registry.add("app.integrations.external.systems.messaging.endpoint", () -> messagingEndpoint);
        registry.add("app.integrations.external.systems.bi.endpoint", () -> httpBase + "/superset/api/v1");
        registry.add("app.integrations.external.systems.routing.endpoint", () -> httpBase + "/osrm");
        registry.add("app.integrations.external.erp.endpoint", () -> httpBase + "/ofbiz/webtools/control");
        registry.add("app.integrations.external.wms.endpoint", () -> httpBase + "/openboxes/api");
        registry.add("app.integrations.external.messaging.endpoint", () -> messagingEndpoint);
        registry.add("app.integrations.external.bi.endpoint", () -> httpBase + "/superset/api/v1");
        registry.add("app.integrations.external.routing.endpoint", () -> httpBase + "/osrm");
        registry.add("app.integrations.external.messaging.callback-worker-endpoint", () -> httpBase + "/callback-worker/consume");
        registry.add("app.observability.log-aggregation-endpoint", () -> httpBase + "/observability/logs");
        registry.add("app.observability.trace-endpoint", () -> httpBase + "/observability/traces");
        registry.add("app.observability.alert-router-endpoint", () -> httpBase + "/observability/alerts");
        registry.add("app.observability.dashboard-url", () -> httpBase + "/observability/dashboard");
        registry.add("app.delivery.github-probe-endpoint", () -> httpBase + "/delivery/github/lsjAnne/saas");
        registry.add("app.delivery.registry-probe-endpoint", () -> httpBase + "/delivery/registry/lsjAnne/dian-shang-ping-tai");
        registry.add("app.delivery.standard-saas-base-url", () -> httpBase + "/delivery/standard-saas");
        registry.add("app.delivery.private-base-url", () -> httpBase + "/delivery/private");
        registry.add("app.integrations.external.probe-timeout-millis", () -> "1000");
    }

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
    private TenantSubscriptionRepository tenantSubscriptionRepository;

    @Autowired
    private TenantProfileRepository tenantProfileRepository;

    @Autowired
    private BillingOrderRepository billingOrderRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private ConfigurableEnvironment environment;

    @AfterAll
    static void shutdownProbeServers() {
        EXTERNAL_HTTP_SERVER.stop(0);
        RABBITMQ_TCP_SERVER.close();
    }

    @BeforeEach
    void setUp() {
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
        authService.clearSensitiveOperationConfirmations();
        removePropertyOverrides(DEFAULT_DELIVERY_GITHUB_PUBLISH_MODE_SOURCE);
        applyPropertyOverrides(DEFAULT_DELIVERY_GITHUB_PUBLISH_MODE_SOURCE, Map.of(
                "app.delivery.github-publish-mode", "github-actions"
        ));
    }

    @Test
    void shouldStartTrialSuspendAndResumeTenant() throws Exception {
        String tenantId = registerTenant();

        mockMvc.perform(post("/api/tenants/{id}/start-trial", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("trial"));

        mockMvc.perform(post("/api/admin/tenants/{id}/suspend", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("suspended"));

        mockMvc.perform(post("/api/admin/tenants/{id}/resume", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("trial"));

        mockMvc.perform(put("/api/admin/tenants/{id}/feature-toggles", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "featureFlags": {
                                    "ai_copilot": true,
                                    "live_module": false
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featureFlags.ai_copilot").value(true))
                .andExpect(jsonPath("$.data.featureFlags.live_module").value(false));
    }

    @Test
    void shouldReconcileSubscriptionAutomationForAutoRenewAndExpiredSuspend() throws Exception {
        String autoRenewTenantId = registerTenant("tenant-auto-renew", "13800000001");
        String expiredSuspendTenantId = registerTenant("tenant-expired-suspend", "13800000002");

        subscribeTenant(autoRenewTenantId, "13800000001", true);
        subscribeTenant(expiredSuspendTenantId, "13800000002", false);

        expireSubscription(autoRenewTenantId, "active");
        expireSubscription(expiredSuspendTenantId, "active");

        mockMvc.perform(post("/api/admin/tenants/subscription-automation/reconcile")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoRenewedCount").value(1))
                .andExpect(jsonPath("$.data.autoSuspendedCount").value(1))
                .andExpect(jsonPath("$.data.autoRenewedTenantIds[0]").value(autoRenewTenantId))
                .andExpect(jsonPath("$.data.autoSuspendedTenantIds[0]").value(expiredSuspendTenantId))
                .andExpect(jsonPath("$.data.executedAt").isString());

        TenantSubscription renewedSubscription = tenantSubscriptionRepository.findByTenantId(autoRenewTenantId).orElseThrow();
        TenantProfile renewedProfile = tenantProfileRepository.findByTenantId(autoRenewTenantId).orElseThrow();
        assertEquals("active", renewedSubscription.subscriptionStatus());
        assertEquals("active", renewedProfile.tenantStatus());
        assertEquals(2, billingOrderRepository.findByTenantId(autoRenewTenantId).size());
        assertEquals("auto_renew", billingOrderRepository.findByTenantId(autoRenewTenantId).get(0).orderType());

        TenantSubscription expiredSubscription = tenantSubscriptionRepository.findByTenantId(expiredSuspendTenantId).orElseThrow();
        TenantProfile expiredProfile = tenantProfileRepository.findByTenantId(expiredSuspendTenantId).orElseThrow();
        assertEquals("expired", expiredSubscription.subscriptionStatus());
        assertEquals("expired_suspended", expiredProfile.tenantStatus());
        assertEquals(1, billingOrderRepository.findByTenantId(expiredSuspendTenantId).size());
    }

    @Test
    void shouldCreatePendingAutoRenewBillAndRecoverAfterSettlement() throws Exception {
        String tenantId = registerTenant("tenant-auto-renew-pending", "13800000005");
        subscribeTenant(tenantId, "13800000005", true);

        mockMvc.perform(put("/api/admin/tenants/{id}/feature-toggles", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "featureFlags": {
                                    "billing_auto_charge_authorized": false
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featureFlags.billing_auto_charge_authorized").value(false));

        expireSubscription(tenantId, "active");

        mockMvc.perform(post("/api/admin/tenants/subscription-automation/reconcile")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoRenewedCount").value(0))
                .andExpect(jsonPath("$.data.autoSuspendedCount").value(0))
                .andExpect(jsonPath("$.data.autoChargeFailedCount").value(1))
                .andExpect(jsonPath("$.data.autoChargeFailedTenantIds[0]").value(tenantId));

        TenantSubscription pendingSubscription = tenantSubscriptionRepository.findByTenantId(tenantId).orElseThrow();
        TenantProfile pendingProfile = tenantProfileRepository.findByTenantId(tenantId).orElseThrow();
        assertEquals("past_due", pendingSubscription.subscriptionStatus());
        assertEquals("payment_overdue", pendingProfile.tenantStatus());

        String pendingBillingOrderId = billingOrderRepository.findByTenantId(tenantId).get(0).billingOrderId();
        assertEquals("auto_renew", billingOrderRepository.findByTenantId(tenantId).get(0).orderType());
        assertEquals("pending", billingOrderRepository.findByTenantId(tenantId).get(0).paymentStatus());

        String ownerToken = login("13800000005");
        mockMvc.perform(post("/api/tenants/{id}/billing-orders/{billingOrderId}/settle", tenantId, pendingBillingOrderId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billingOrderId").value(pendingBillingOrderId))
                .andExpect(jsonPath("$.data.paymentStatus").value("paid"));

        TenantSubscription recoveredSubscription = tenantSubscriptionRepository.findByTenantId(tenantId).orElseThrow();
        TenantProfile recoveredProfile = tenantProfileRepository.findByTenantId(tenantId).orElseThrow();
        assertEquals("active", recoveredSubscription.subscriptionStatus());
        assertEquals("active", recoveredProfile.tenantStatus());

        mockMvc.perform(post("/api/tenants/{id}/invoice-requests", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billingOrderId": "%s",
                                  "invoiceTitle": "自动续费补偿账单",
                                  "invoiceTaxNo": "91310000AUTO0005"
                                }
                                """.formatted(pendingBillingOrderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billingOrderId").value(pendingBillingOrderId))
                .andExpect(jsonPath("$.data.invoiceStatus").value("pending"));
    }

    @Test
    void shouldExposeReleaseReadinessChecklistWithBlockingReasonsAndEvidence() throws Exception {
        String tenantId = registerTenant("tenant-release-readiness", "13800000006");
        String ownerToken = login("13800000006");

        subscribeTenant(tenantId, "13800000006", true);
        confirmSensitivePermission(ownerToken, "tenant.data.export.manage");

        mockMvc.perform(post("/api/tenant/data-exports")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeCode": "tenant_profile",
                                  "maskingStrategy": "tenant_admin_only"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/compliance/acceptances")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                                  "documentCodes": ["privacy-policy"]
                                }
                                """))
                .andExpect(status().isOk());

        confirmSensitivePermission(ownerToken, "tenant.lifecycle.manage");
        mockMvc.perform(post("/api/tenants/{id}/offboarding/request", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tenants/{id}/cleanup-tasks/plan", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "release_readiness_sampling"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/tenants/{id}/release-readiness", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(tenantId))
                .andExpect(jsonPath("$.data.conclusion").value("reject_release"))
                .andExpect(jsonPath("$.data.checklistItems.length()").value(8))
                .andExpect(jsonPath("$.data.checklistItems[0].itemCode").value("config_hardening"))
                .andExpect(jsonPath("$.data.checklistItems[0].status").value("blocked"))
                .andExpect(jsonPath("$.data.checklistItems[0].detail").value(org.hamcrest.Matchers.containsString("explicit auth secret enforcement is disabled")))
                .andExpect(jsonPath("$.data.checklistItems[0].detail").value(org.hamcrest.Matchers.containsString("legacy header context fallback is still enabled")))
                .andExpect(jsonPath("$.data.checklistItems[1].itemCode").value("critical_state_evidence"))
                .andExpect(jsonPath("$.data.checklistItems[1].status").value("passed"))
                .andExpect(jsonPath("$.data.checklistItems[1].evidenceCount").value(4))
                .andExpect(jsonPath("$.data.checklistItems[2].itemCode").value("audit_traceability"))
                .andExpect(jsonPath("$.data.checklistItems[2].status").value("passed"))
                .andExpect(jsonPath("$.data.checklistItems[3].itemCode").value("automation_regression_evidence"))
                .andExpect(jsonPath("$.data.checklistItems[3].status").value("passed"))
                .andExpect(jsonPath("$.data.checklistItems[3].evidenceCount").value(3))
                .andExpect(jsonPath("$.data.checklistItems[3].detail").value(org.hamcrest.Matchers.containsString("server-local")))
                .andExpect(jsonPath("$.data.checklistItems[3].detail").value(org.hamcrest.Matchers.containsString("2026-06-08T09:59:30+08:00")))
                .andExpect(jsonPath("$.data.checklistItems[3].detail").value(org.hamcrest.Matchers.containsString("auth/openplatform/compliance regression suites passed")))
                .andExpect(jsonPath("$.data.checklistItems[4].itemCode").value("external_integration_readiness"))
                .andExpect(jsonPath("$.data.checklistItems[4].status").value("passed"))
                .andExpect(jsonPath("$.data.checklistItems[4].evidenceCount").value(6))
                .andExpect(jsonPath("$.data.checklistItems[4].detail").value(org.hamcrest.Matchers.containsString("erp(ofbiz)")))
                .andExpect(jsonPath("$.data.checklistItems[4].detail").value(org.hamcrest.Matchers.containsString("wms(openboxes)")))
                .andExpect(jsonPath("$.data.checklistItems[4].detail").value(org.hamcrest.Matchers.containsString("messaging(rabbitmq)")))
                .andExpect(jsonPath("$.data.checklistItems[4].detail").value(org.hamcrest.Matchers.containsString("callback worker")))
                .andExpect(jsonPath("$.data.checklistItems[4].detail").value(org.hamcrest.Matchers.containsString("probe reachable 5/5")))
                .andExpect(jsonPath("$.data.checklistItems[5].itemCode").value("observability_stack_readiness"))
                .andExpect(jsonPath("$.data.checklistItems[5].status").value("passed"))
                .andExpect(jsonPath("$.data.checklistItems[5].detail").value(org.hamcrest.Matchers.containsString("probe reachable 4/4")))
                .andExpect(jsonPath("$.data.checklistItems[6].itemCode").value("delivery_pipeline_readiness"))
                .andExpect(jsonPath("$.data.checklistItems[6].status").value("passed"))
                .andExpect(jsonPath("$.data.checklistItems[6].detail").value(org.hamcrest.Matchers.containsString("release-key=configured@override")))
                .andExpect(jsonPath("$.data.checklistItems[6].detail").value(org.hamcrest.Matchers.containsString("registry-auth=configured@override")))
                .andExpect(jsonPath("$.data.checklistItems[6].detail").value(org.hamcrest.Matchers.containsString("github-publish-mode=configured@override")))
                .andExpect(jsonPath("$.data.checklistItems[6].detail").value(org.hamcrest.Matchers.containsString("canary-strategy=configured@override")))
                .andExpect(jsonPath("$.data.checklistItems[6].detail").value(org.hamcrest.Matchers.containsString("probe reachable 2/2")))
                .andExpect(jsonPath("$.data.checklistItems[7].itemCode").value("dual_delivery_acceptance"))
                .andExpect(jsonPath("$.data.checklistItems[7].status").value("passed"))
                .andExpect(jsonPath("$.data.checklistItems[7].detail").value(org.hamcrest.Matchers.containsString("probe reachable 2/2")))
                .andExpect(jsonPath("$.data.blockingReasons[0]").value(org.hamcrest.Matchers.containsString("default auth secret")))
                .andExpect(jsonPath("$.data.blockingReasons[1]").value(org.hamcrest.Matchers.containsString("bootstrap password")))
                .andExpect(jsonPath("$.data.blockingReasons[2]").value(org.hamcrest.Matchers.containsString("explicit auth secret enforcement is disabled")))
                .andExpect(jsonPath("$.data.blockingReasons[3]").value(org.hamcrest.Matchers.containsString("legacy header context fallback is still enabled")))
                .andExpect(jsonPath("$.data.evidenceSummary.exportTaskCount").value(1))
                .andExpect(jsonPath("$.data.evidenceSummary.cleanupTaskCount").value(1))
                .andExpect(jsonPath("$.data.evidenceSummary.complianceAcceptanceCount").value(1))
                .andExpect(jsonPath("$.data.evidenceSummary.billingOrderCount").value(1));
    }

    @Test
    void shouldExposeStructuredDeliveryReadinessDetails() throws Exception {
        String tenantId = registerTenant("tenant-delivery-readiness", "13800000007");

        mockMvc.perform(get("/api/admin/tenants/{id}/delivery-readiness", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(tenantId))
                .andExpect(jsonPath("$.data.pipeline.ready").value(true))
                .andExpect(jsonPath("$.data.pipeline.githubOwner").value("lsjAnne"))
                .andExpect(jsonPath("$.data.pipeline.githubRepository").value("saas"))
                .andExpect(jsonPath("$.data.pipeline.repository").value("lsjAnne/saas"))
                .andExpect(jsonPath("$.data.pipeline.registry").value("ghcr.io"))
                .andExpect(jsonPath("$.data.pipeline.imageRepository").value("lsjAnne/dian-shang-ping-tai"))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyConfigured").value(true))
                .andExpect(jsonPath("$.data.pipeline.canaryEnabled").value(true))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.controlCode").value("release-key"))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.value").value("true"))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.configured").value(true))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.sourceType").value("override"))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.defaultValue").value(false))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.trusted").value(true))
                .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.status").value("configured"))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.controlCode").value("registry-auth"))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.value").value("true"))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.configured").value(true))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.sourceType").value("override"))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.defaultValue").value(false))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.trusted").value(true))
                .andExpect(jsonPath("$.data.pipeline.registryAuthControl.status").value("configured"))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.controlCode").value("github-publish-mode"))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.value").value("github-actions"))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.configured").value(true))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.sourceType").value("override"))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.defaultValue").value(false))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.trusted").value(true))
                .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.status").value("configured"))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.controlCode").value("canary-strategy"))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.value").value("header-weighted"))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.configured").value(true))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.sourceType").value("override"))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.defaultValue").value(false))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.trusted").value(true))
                .andExpect(jsonPath("$.data.pipeline.canaryControl.status").value("configured"))
                .andExpect(jsonPath("$.data.pipeline.workflowAsset.ready").value(true))
                .andExpect(jsonPath("$.data.pipeline.workflowAsset.path").value(".github/workflows/backend-delivery.yml"))
                .andExpect(jsonPath("$.data.pipeline.standardSaasComposeAsset.ready").value(true))
                .andExpect(jsonPath("$.data.pipeline.standardSaasComposeAsset.path").value("docker-compose.saas.yml"))
                .andExpect(jsonPath("$.data.pipeline.privateComposeAsset.ready").value(true))
                .andExpect(jsonPath("$.data.pipeline.privateComposeAsset.path").value("docker-compose.private.yml"))
                .andExpect(jsonPath("$.data.pipeline.githubProbe.systemCode").value("github-repository"))
                .andExpect(jsonPath("$.data.pipeline.githubProbe.protocol").value("http"))
                .andExpect(jsonPath("$.data.pipeline.githubProbe.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.pipeline.githubProbe.reachable").value(true))
                .andExpect(jsonPath("$.data.pipeline.githubProbe.detail").value("http 200"))
                .andExpect(jsonPath("$.data.pipeline.registryProbe.systemCode").value("container-registry"))
                .andExpect(jsonPath("$.data.pipeline.registryProbe.protocol").value("http"))
                .andExpect(jsonPath("$.data.pipeline.registryProbe.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.pipeline.registryProbe.reachable").value(true))
                .andExpect(jsonPath("$.data.pipeline.registryProbe.detail").value("http 200"))
                .andExpect(jsonPath("$.data.externalIntegrations.ready").value(true))
                .andExpect(jsonPath("$.data.externalIntegrations.configuredCount").value(5))
                .andExpect(jsonPath("$.data.externalIntegrations.reachableCount").value(5))
                .andExpect(jsonPath("$.data.externalIntegrations.erp.systemCode").value("erp"))
                .andExpect(jsonPath("$.data.externalIntegrations.erp.provider").value("ofbiz"))
                .andExpect(jsonPath("$.data.externalIntegrations.erp.protocol").value("http"))
                .andExpect(jsonPath("$.data.externalIntegrations.erp.reachable").value(true))
                .andExpect(jsonPath("$.data.externalIntegrations.erp.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.externalIntegrations.erp.detail").value("http 200"))
                .andExpect(jsonPath("$.data.externalIntegrations.wms.systemCode").value("wms"))
                .andExpect(jsonPath("$.data.externalIntegrations.wms.provider").value("openboxes"))
                .andExpect(jsonPath("$.data.externalIntegrations.wms.reachable").value(true))
                .andExpect(jsonPath("$.data.externalIntegrations.messaging.systemCode").value("messaging"))
                .andExpect(jsonPath("$.data.externalIntegrations.messaging.provider").value("rabbitmq"))
                .andExpect(jsonPath("$.data.externalIntegrations.messaging.protocol").value("tcp"))
                .andExpect(jsonPath("$.data.externalIntegrations.messaging.reachable").value(true))
                .andExpect(jsonPath("$.data.externalIntegrations.messaging.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.externalIntegrations.messaging.detail").value("tcp connected"))
                .andExpect(jsonPath("$.data.externalIntegrations.bi.systemCode").value("bi"))
                .andExpect(jsonPath("$.data.externalIntegrations.bi.provider").value("superset"))
                .andExpect(jsonPath("$.data.externalIntegrations.bi.reachable").value(true))
                .andExpect(jsonPath("$.data.externalIntegrations.routing.systemCode").value("routing"))
                .andExpect(jsonPath("$.data.externalIntegrations.routing.provider").value("osrm"))
                .andExpect(jsonPath("$.data.externalIntegrations.routing.reachable").value(true))
                .andExpect(jsonPath("$.data.acceptance.ready").value(true))
                .andExpect(jsonPath("$.data.acceptance.standardSaas.mode").value("standard-saas"))
                .andExpect(jsonPath("$.data.acceptance.standardSaas.configured").value(true))
                .andExpect(jsonPath("$.data.acceptance.standardSaas.protocol").value("http"))
                .andExpect(jsonPath("$.data.acceptance.standardSaas.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.acceptance.standardSaas.maskedBaseUrl").value("http://127.0.0.1:" + HTTP_PORT + "/***"))
                .andExpect(jsonPath("$.data.acceptance.standardSaas.reachable").value(true))
                .andExpect(jsonPath("$.data.acceptance.standardSaas.verificationFresh").value(true))
                .andExpect(jsonPath("$.data.acceptance.standardSaas.detail").value("http 200"))
                .andExpect(jsonPath("$.data.acceptance.standardSaas.verifiedAt").value("2026-06-10T15:10:00+08:00"))
                .andExpect(jsonPath("$.data.acceptance.privateDeployment.mode").value("private-deployment"))
                .andExpect(jsonPath("$.data.acceptance.privateDeployment.configured").value(true))
                .andExpect(jsonPath("$.data.acceptance.privateDeployment.protocol").value("http"))
                .andExpect(jsonPath("$.data.acceptance.privateDeployment.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.acceptance.privateDeployment.maskedBaseUrl").value("http://127.0.0.1:" + HTTP_PORT + "/***"))
                .andExpect(jsonPath("$.data.acceptance.privateDeployment.reachable").value(true))
                .andExpect(jsonPath("$.data.acceptance.privateDeployment.verificationFresh").value(true))
                .andExpect(jsonPath("$.data.acceptance.privateDeployment.detail").value("http 200"))
                .andExpect(jsonPath("$.data.acceptance.privateDeployment.verifiedAt").value("2026-06-10T15:25:00+08:00"));
    }

    @Test
    void shouldBlockDeliveryReadinessWhenAcceptanceEvidenceIsStale() throws Exception {
        String tenantId = registerTenant("tenant-delivery-stale-evidence", "13800000017");
        String sourceName = "delivery-stale-evidence-test";
        applyPropertyOverrides(sourceName, Map.of(
                "app.delivery.acceptance-evidence-max-age-days", "7",
                "app.delivery.standard-saas-verified-at", OffsetDateTime.now().minusDays(30).toString(),
                "app.delivery.private-verified-at", OffsetDateTime.now().minusDays(30).toString()
        ));
        try {
            mockMvc.perform(get("/api/admin/tenants/{id}/delivery-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.acceptance.ready").value(false))
                    .andExpect(jsonPath("$.data.acceptance.standardSaas.verificationFresh").value(false))
                    .andExpect(jsonPath("$.data.acceptance.standardSaas.detail").value(org.hamcrest.Matchers.containsString("verification evidence is stale")))
                    .andExpect(jsonPath("$.data.acceptance.privateDeployment.verificationFresh").value(false))
                    .andExpect(jsonPath("$.data.acceptance.privateDeployment.detail").value(org.hamcrest.Matchers.containsString("verification evidence is stale")));

            mockMvc.perform(get("/api/admin/tenants/{id}/release-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("dual delivery acceptance standard-saas verification evidence is stale")))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("dual delivery acceptance private-deployment verification evidence is stale")))
                    .andExpect(jsonPath("$.data.checklistItems[?(@.itemCode=='dual_delivery_acceptance')].status", org.hamcrest.Matchers.hasItem("blocked")));
        } finally {
            removePropertyOverrides(sourceName);
        }
    }

    @Test
    void shouldBlockDeliveryReadinessWhenDeliveryAssetsAreMissing() throws Exception {
        String tenantId = registerTenant("tenant-delivery-missing-assets", "13800000018");
        String sourceName = "delivery-missing-assets-test";
        applyPropertyOverrides(sourceName, Map.of(
                "app.delivery.workflow-path", ".tools/missing/backend-delivery.yml",
                "app.delivery.standard-saas-compose-path", ".tools/missing/docker-compose.saas.yml",
                "app.delivery.private-compose-path", ".tools/missing/docker-compose.private.yml"
        ));
        try {
            mockMvc.perform(get("/api/admin/tenants/{id}/delivery-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pipeline.ready").value(false))
                    .andExpect(jsonPath("$.data.pipeline.workflowAsset.ready").value(false))
                    .andExpect(jsonPath("$.data.pipeline.standardSaasComposeAsset.ready").value(false))
                    .andExpect(jsonPath("$.data.pipeline.privateComposeAsset.ready").value(false));

            mockMvc.perform(get("/api/admin/tenants/{id}/release-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline workflow asset is missing or invalid")))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline standard saas compose asset is missing or invalid")))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline private compose asset is missing or invalid")))
                    .andExpect(jsonPath("$.data.checklistItems[?(@.itemCode=='delivery_pipeline_readiness')].status", org.hamcrest.Matchers.hasItem("blocked")));
        } finally {
            removePropertyOverrides(sourceName);
        }
    }

    @Test
    void shouldBlockDeliveryReadinessWhenRegistryAuthControlIsMissing() throws Exception {
        String tenantId = registerTenant("tenant-delivery-missing-registry-auth", "13800000019");
        String sourceName = "delivery-missing-registry-auth-test";
        applyPropertyOverrides(sourceName, Map.of(
                "app.delivery.registry-auth-configured", "false"
        ));
        try {
            mockMvc.perform(get("/api/admin/tenants/{id}/delivery-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pipeline.ready").value(false))
                    .andExpect(jsonPath("$.data.pipeline.registryAuthControl.configured").value(false))
                    .andExpect(jsonPath("$.data.pipeline.registryAuthControl.sourceType").value("override"))
                    .andExpect(jsonPath("$.data.pipeline.registryAuthControl.status").value("disabled"));

            mockMvc.perform(get("/api/admin/tenants/{id}/release-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline is missing container registry publish credentials")))
                    .andExpect(jsonPath("$.data.checklistItems[?(@.itemCode=='delivery_pipeline_readiness')].status", org.hamcrest.Matchers.hasItem("blocked")))
                    .andExpect(jsonPath("$.data.checklistItems[?(@.itemCode=='delivery_pipeline_readiness')].detail", org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("registry-auth=disabled@override"))));
        } finally {
            removePropertyOverrides(sourceName);
        }
    }

    @Test
    void shouldBlockDeliveryReadinessWhenGithubPublishModeStillReliesOnDefaultConfiguration() throws Exception {
        String tenantId = registerTenant("tenant-delivery-default-github-publish-mode", "13800000020");
        String sourceName = "delivery-default-github-publish-mode-test";
        removePropertyOverrides(DEFAULT_DELIVERY_GITHUB_PUBLISH_MODE_SOURCE);
        try {
            mockMvc.perform(get("/api/admin/tenants/{id}/delivery-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pipeline.ready").value(false))
                    .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.configured").value(true))
                    .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.sourceType").value("default"))
                    .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.defaultValue").value(true))
                    .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.trusted").value(false))
                    .andExpect(jsonPath("$.data.pipeline.githubPublishingControl.status").value("configured"));

            mockMvc.perform(get("/api/admin/tenants/{id}/release-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline github publish mode must be explicitly configured")))
                    .andExpect(jsonPath("$.data.checklistItems[?(@.itemCode=='delivery_pipeline_readiness')].status", org.hamcrest.Matchers.hasItem("blocked")))
                    .andExpect(jsonPath("$.data.checklistItems[?(@.itemCode=='delivery_pipeline_readiness')].detail", org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("github-publish-mode=configured@default"))));
        } finally {
            applyPropertyOverrides(DEFAULT_DELIVERY_GITHUB_PUBLISH_MODE_SOURCE, Map.of(
                    "app.delivery.github-publish-mode", "github-actions"
            ));
        }
    }

    @Test
    void shouldBlockDeliveryReadinessWhenReleaseKeyControlIsNotFromTrustedSource() throws Exception {
        String tenantId = registerTenant("tenant-delivery-untrusted-release-key", "13800000021");
        String sourceName = "delivery-untrusted-release-key-test";
        InlinedTestPropertiesState originalInlineProperties = replaceInlinedTestProperties(Map.of(
                "app.delivery.release-key-configured", "${test.delivery.release-key:true}"
        ));
        applyPropertyOverrides(sourceName, Map.of(
                "test.delivery.release-key", "true"
        ));
        try {
            mockMvc.perform(get("/api/admin/tenants/{id}/delivery-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pipeline.ready").value(false))
                    .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.configured").value(true))
                    .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.sourceType").value("resolved"))
                    .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.defaultValue").value(false))
                    .andExpect(jsonPath("$.data.pipeline.releaseKeyControl.trusted").value(false));

            mockMvc.perform(get("/api/admin/tenants/{id}/release-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline release key injection must be explicitly configured")))
                    .andExpect(jsonPath("$.data.checklistItems[?(@.itemCode=='delivery_pipeline_readiness')].status", org.hamcrest.Matchers.hasItem("blocked")))
                    .andExpect(jsonPath("$.data.checklistItems[?(@.itemCode=='delivery_pipeline_readiness')].detail", org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("release-key=configured-untrusted@resolved"))));
        } finally {
            removePropertyOverrides(sourceName);
            restoreInlinedTestProperties(originalInlineProperties);
        }
    }

    @Test
    void shouldBlockDeliveryReadinessWhenRegistryAuthControlIsNotFromTrustedSource() throws Exception {
        String tenantId = registerTenant("tenant-delivery-untrusted-registry-auth", "13800000022");
        String sourceName = "delivery-untrusted-registry-auth-test";
        InlinedTestPropertiesState originalInlineProperties = replaceInlinedTestProperties(Map.of(
                "app.delivery.registry-auth-configured", "${test.delivery.registry-auth:true}"
        ));
        applyPropertyOverrides(sourceName, Map.of(
                "test.delivery.registry-auth", "true"
        ));
        try {
            mockMvc.perform(get("/api/admin/tenants/{id}/delivery-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pipeline.ready").value(false))
                    .andExpect(jsonPath("$.data.pipeline.registryAuthControl.configured").value(true))
                    .andExpect(jsonPath("$.data.pipeline.registryAuthControl.sourceType").value("resolved"))
                    .andExpect(jsonPath("$.data.pipeline.registryAuthControl.defaultValue").value(false))
                    .andExpect(jsonPath("$.data.pipeline.registryAuthControl.trusted").value(false));

            mockMvc.perform(get("/api/admin/tenants/{id}/release-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("delivery pipeline container registry publish credentials must be explicitly configured")))
                    .andExpect(jsonPath("$.data.checklistItems[?(@.itemCode=='delivery_pipeline_readiness')].status", org.hamcrest.Matchers.hasItem("blocked")))
                    .andExpect(jsonPath("$.data.checklistItems[?(@.itemCode=='delivery_pipeline_readiness')].detail", org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("registry-auth=configured-untrusted@resolved"))));
        } finally {
            removePropertyOverrides(sourceName);
            restoreInlinedTestProperties(originalInlineProperties);
        }
    }

    @Test
    void shouldBlockReleaseReadinessWhenExternalCredentialIsNotFromTrustedSource() throws Exception {
        String tenantId = registerTenant("tenant-external-untrusted-credential", "13800000023");
        String sourceName = "external-untrusted-credential-test";
        applyPropertyOverrides(sourceName, Map.of(
                "app.integrations.external.systems.erp.credential-configured", "${test.external.erp.credential:true}",
                "test.external.erp.credential", "true"
        ));
        try {
            mockMvc.perform(get("/api/admin/tenants/{id}/release-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("erp(ofbiz) integration credentials must be explicitly configured")))
                    .andExpect(jsonPath("$.data.checklistItems[?(@.itemCode=='external_integration_readiness')].status", org.hamcrest.Matchers.hasItem("blocked")))
                    .andExpect(jsonPath("$.data.checklistItems[?(@.itemCode=='external_integration_readiness')].detail",
                            org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("erp(ofbiz) integration credentials must be explicitly configured"))));
        } finally {
            removePropertyOverrides(sourceName);
        }
    }

    @Test
    void shouldExposeStructuredObservabilityReadinessDetails() throws Exception {
        String tenantId = registerTenant("tenant-observability-readiness", "13800000008");
        String ownerToken = login("13800000008");

        mockMvc.perform(post("/api/tenants/{id}/subscription/subscribe", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "basic",
                                  "seatCount": 2,
                                  "autoRenew": true
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/tenants/{id}/observability-readiness", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(tenantId))
                .andExpect(jsonPath("$.data.stack.ready").value(true))
                .andExpect(jsonPath("$.data.stack.configuredCount").value(4))
                .andExpect(jsonPath("$.data.stack.reachableCount").value(4))
                .andExpect(jsonPath("$.data.stack.releaseGateStatus").value("passed"))
                .andExpect(jsonPath("$.data.stack.releaseGateDetail").value(org.hamcrest.Matchers.containsString("probe reachable 4/4")))
                .andExpect(jsonPath("$.data.stack.logAggregation.systemCode").value("log-aggregation"))
                .andExpect(jsonPath("$.data.stack.logAggregation.provider").value("observability"))
                .andExpect(jsonPath("$.data.stack.logAggregation.protocol").value("http"))
                .andExpect(jsonPath("$.data.stack.logAggregation.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.stack.logAggregation.sourceType").value("override"))
                .andExpect(jsonPath("$.data.stack.logAggregation.defaultValue").value(false))
                .andExpect(jsonPath("$.data.stack.logAggregation.trusted").value(true))
                .andExpect(jsonPath("$.data.stack.logAggregation.status").value("configured"))
                .andExpect(jsonPath("$.data.stack.logAggregation.reachable").value(true))
                .andExpect(jsonPath("$.data.stack.logAggregation.detail").value("http 200"))
                .andExpect(jsonPath("$.data.stack.trace.systemCode").value("trace"))
                .andExpect(jsonPath("$.data.stack.trace.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.stack.trace.reachable").value(true))
                .andExpect(jsonPath("$.data.stack.alertRouter.systemCode").value("alert-router"))
                .andExpect(jsonPath("$.data.stack.alertRouter.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.stack.alertRouter.reachable").value(true))
                .andExpect(jsonPath("$.data.stack.dashboard.systemCode").value("dashboard"))
                .andExpect(jsonPath("$.data.stack.dashboard.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.stack.dashboard.reachable").value(true))
                .andExpect(jsonPath("$.data.auditTraceability.auditLogCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.auditTraceability.traceableAuditLogCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.auditTraceability.latestTraceIds.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.blockingReasons.length()").value(0));
    }

    @Test
    void shouldBlockObservabilityReadinessWhenEndpointIsNotFromTrustedSource() throws Exception {
        String tenantId = registerTenant("tenant-observability-untrusted-endpoint", "13800000023");
        InlinedTestPropertiesState originalDynamicProperties = replaceNamedPropertySource("Dynamic Test Properties", Map.of(
                "app.observability.log-aggregation-endpoint", "${test.observability.log-endpoint}",
                "test.observability.log-endpoint", "http://127.0.0.1:" + HTTP_PORT + "/observability/logs"
        ));
        try {
            mockMvc.perform(get("/api/admin/tenants/{id}/observability-readiness", tenantId)
                            .header("X-Operator-Id", "platform-ops-1")
                            .header("X-Operator-Type", "platform-ops"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.stack.ready").value(false))
                    .andExpect(jsonPath("$.data.stack.releaseGateStatus").value("blocked"))
                    .andExpect(jsonPath("$.data.stack.logAggregation.configured").value(true))
                    .andExpect(jsonPath("$.data.stack.logAggregation.sourceType").value("resolved"))
                    .andExpect(jsonPath("$.data.stack.logAggregation.defaultValue").value(false))
                    .andExpect(jsonPath("$.data.stack.logAggregation.trusted").value(false))
                    .andExpect(jsonPath("$.data.stack.logAggregation.status").value("configured-untrusted"))
                    .andExpect(jsonPath("$.data.blockingReasons", org.hamcrest.Matchers.hasItem("observability stack log aggregation endpoint must be explicitly configured")))
                    .andExpect(jsonPath("$.data.stack.releaseGateDetail").value(org.hamcrest.Matchers.containsString("log-aggregation=configured-untrusted@resolved")));
        } finally {
            restoreNamedPropertySource("Dynamic Test Properties", originalDynamicProperties);
        }
    }

    @Test
    void shouldPublishComplianceDocumentAndTrackPendingReacceptance() throws Exception {
        String tenantId = registerTenant("tenant-compliance-a", "13800000003");
        String ownerToken = login("13800000003");

        mockMvc.perform(post("/api/compliance/acceptances")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentCodes": ["privacy-policy"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data[0].version").value("2026.06"));

        mockMvc.perform(get("/api/admin/tenants/compliance/acceptances")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .param("documentCode", "privacy-policy")
                        .param("acceptanceStatus", "accepted_current_version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tenantId").value(tenantId))
                .andExpect(jsonPath("$.data[0].documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data[0].currentVersion").value("2026.06"))
                .andExpect(jsonPath("$.data[0].latestAcceptedVersion").value("2026.06"))
                .andExpect(jsonPath("$.data[0].acceptanceStatus").value("accepted_current_version"));

        mockMvc.perform(post("/api/admin/tenants/compliance/documents/privacy-policy/publish")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": "2026.07",
                                  "title": "隐私政策",
                                  "content": "已发布新的隐私政策版本，需租户重新确认。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data.version").value("2026.07"));

        mockMvc.perform(get("/api/compliance/privacy-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("2026.07"));

        mockMvc.perform(get("/api/admin/tenants/compliance/documents")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data[0].version").value("2026.07"))
                .andExpect(jsonPath("$.data[0].pendingReacceptanceTenantCount").value(1));

        mockMvc.perform(get("/api/admin/tenants/compliance/acceptances")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .param("documentCode", "privacy-policy")
                        .param("acceptanceStatus", "pending_reacceptance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tenantId").value(tenantId))
                .andExpect(jsonPath("$.data[0].currentVersion").value("2026.07"))
                .andExpect(jsonPath("$.data[0].latestAcceptedVersion").value("2026.06"))
                .andExpect(jsonPath("$.data[0].acceptanceStatus").value("pending_reacceptance"));

        mockMvc.perform(post("/api/compliance/acceptances")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentCodes": ["privacy-policy"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data[0].version").value("2026.07"));

        mockMvc.perform(get("/api/admin/tenants/compliance/acceptances")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .param("documentCode", "privacy-policy")
                        .param("tenantId", tenantId)
                        .param("acceptanceStatus", "accepted_current_version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tenantId").value(tenantId))
                .andExpect(jsonPath("$.data[0].currentVersion").value("2026.07"))
                .andExpect(jsonPath("$.data[0].latestAcceptedVersion").value("2026.07"))
                .andExpect(jsonPath("$.data[0].acceptanceStatus").value("accepted_current_version"));
    }

    @Test
    void shouldBlockSensitiveOperationsUntilLatestComplianceDocumentsAreReaccepted() throws Exception {
        String tenantId = registerTenant("tenant-compliance-block", "13800000004");
        String ownerToken = login("13800000004");

        mockMvc.perform(post("/api/compliance/acceptances")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentCodes": ["privacy-policy", "user-agreement"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].version").value("2026.06"))
                .andExpect(jsonPath("$.data[1].version").value("2026.06"));

        mockMvc.perform(post("/api/admin/tenants/compliance/documents/privacy-policy/publish")
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": "2026.08",
                                  "title": "隐私政策",
                                  "content": "新的隐私政策版本已经发布。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("2026.08"));

        confirmSensitivePermission(ownerToken, "tenant.data.export.manage");
        mockMvc.perform(post("/api/tenant/data-exports")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeCode": "tenant_profile",
                                  "maskingStrategy": "tenant_admin_only"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1014"))
                .andExpect(jsonPath("$.data.permissionCode").value("tenant.data.export.manage"))
                .andExpect(jsonPath("$.data.pendingDocumentCodes[0]").value("privacy-policy"));

        confirmSensitivePermission(ownerToken, "tenant.lifecycle.manage");
        mockMvc.perform(post("/api/tenants/{id}/offboarding/request", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1014"))
                .andExpect(jsonPath("$.data.permissionCode").value("tenant.lifecycle.manage"))
                .andExpect(jsonPath("$.data.pendingDocumentCodes[0]").value("privacy-policy"));

        mockMvc.perform(post("/api/compliance/acceptances")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentCodes": ["privacy-policy"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].documentCode").value("privacy-policy"))
                .andExpect(jsonPath("$.data[0].version").value("2026.08"));

        confirmSensitivePermission(ownerToken, "tenant.data.export.manage");
        mockMvc.perform(post("/api/tenant/data-exports")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeCode": "tenant_profile",
                                  "maskingStrategy": "tenant_admin_only"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeCode").value("tenant_profile"))
                .andExpect(jsonPath("$.data.status").value("completed"));

        confirmSensitivePermission(ownerToken, "tenant.lifecycle.manage");
        mockMvc.perform(post("/api/tenants/{id}/offboarding/request", tenantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantStatus").value("offboarding_requested"));
    }

    private String registerTenant() throws Exception {
        return registerTenant("星迹电商", "13800000000");
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

    private void subscribeTenant(String tenantId, String mobile, boolean autoRenew) throws Exception {
        String ownerToken = login(mobile);
        mockMvc.perform(post("/api/tenants/{id}/subscription/subscribe", tenantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "basic",
                                  "seatCount": 2,
                                  "autoRenew": %s
                                }
                                """.formatted(autoRenew)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode").value("basic"))
                .andExpect(jsonPath("$.data.autoRenew").value(autoRenew));
    }

    private void expireSubscription(String tenantId, String tenantStatus) {
        TenantSubscription current = tenantSubscriptionRepository.findByTenantId(tenantId).orElseThrow();
        tenantSubscriptionRepository.save(new TenantSubscription(
                current.subscriptionId(),
                current.tenantId(),
                current.planCode(),
                current.planName(),
                current.subscriptionStatus(),
                current.startedAt(),
                OffsetDateTime.now().minusDays(2),
                current.seatCount(),
                current.autoRenew()
        ));
        TenantProfile profile = tenantProfileRepository.findByTenantId(tenantId).orElseThrow();
        tenantProfileRepository.save(profile.withStatusAndTrialEndAt(tenantStatus, current.expiredAt()));
    }

    private String login(String mobile) throws Exception {
        JsonNode root = objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "123456"
                                }
                                """.formatted(mobile)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        return root.path("data").path("token").asText();
    }

    private void confirmSensitivePermission(String token, String permissionCode) throws Exception {
        mockMvc.perform(post("/api/auth/sensitive-operation-confirmations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "123456",
                                  "permissionCode": "%s"
                                }
                                """.formatted(permissionCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCode").value(permissionCode))
                .andExpect(jsonPath("$.data.confirmed").value(true))
                .andExpect(jsonPath("$.data.expiresAt").isString());
    }

    private void applyPropertyOverrides(String sourceName, Map<String, Object> properties) {
        environment.getPropertySources().addFirst(new MapPropertySource(sourceName, new LinkedHashMap<>(properties)));
    }

    private void removePropertyOverrides(String sourceName) {
        environment.getPropertySources().remove(sourceName);
    }

    private InlinedTestPropertiesState replaceNamedPropertySource(String sourceName, Map<String, Object> overrides) {
        InlinedTestPropertiesState original = detachPropertySource(sourceName);
        Map<String, Object> properties = copyProperties(original.propertySource());
        properties.putAll(overrides);
        attachPropertySource(new InlinedTestPropertiesState(
                new MapPropertySource(sourceName, properties),
                original.previousName(),
                original.nextName()
        ));
        return original;
    }

    private void restoreNamedPropertySource(String sourceName, InlinedTestPropertiesState original) {
        environment.getPropertySources().remove(sourceName);
        attachPropertySource(original);
    }

    private InlinedTestPropertiesState replaceInlinedTestProperties(Map<String, Object> overrides) {
        return replaceNamedPropertySource("Inlined Test Properties", overrides);
    }

    private void restoreInlinedTestProperties(InlinedTestPropertiesState original) {
        restoreNamedPropertySource("Inlined Test Properties", original);
    }

    private Map<String, Object> copyProperties(PropertySource<?> propertySource) {
        Map<String, Object> copied = new LinkedHashMap<>();
        if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource) {
            for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                copied.put(propertyName, enumerablePropertySource.getProperty(propertyName));
            }
        }
        return copied;
    }

    private InlinedTestPropertiesState detachInlinedTestProperties() {
        return detachPropertySource("Inlined Test Properties");
    }

    private InlinedTestPropertiesState detachPropertySource(String sourceName) {
        MutablePropertySources propertySources = environment.getPropertySources();
        PropertySource<?> original = null;
        String previousName = null;
        String nextName = null;
        boolean found = false;
        for (PropertySource<?> propertySource : propertySources) {
            if (sourceName.equals(propertySource.getName())) {
                original = propertySource;
                found = true;
                continue;
            }
            if (!found) {
                previousName = propertySource.getName();
            } else {
                nextName = propertySource.getName();
                break;
            }
        }
        if (original != null) {
            propertySources.remove(sourceName);
        }
        return new InlinedTestPropertiesState(original, previousName, nextName);
    }

    private void attachPropertySource(InlinedTestPropertiesState state) {
        if (state == null || state.propertySource() == null) {
            return;
        }
        MutablePropertySources propertySources = environment.getPropertySources();
        if (state.nextName() != null && propertySources.contains(state.nextName())) {
            propertySources.addBefore(state.nextName(), state.propertySource());
            return;
        }
        if (state.previousName() != null && propertySources.contains(state.previousName())) {
            propertySources.addAfter(state.previousName(), state.propertySource());
            return;
        }
        propertySources.addLast(state.propertySource());
    }

    private static HttpServer createExternalHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/ofbiz/webtools/control", AdminTenantControllerTest::writeOk);
            server.createContext("/openboxes/api", AdminTenantControllerTest::writeOk);
            server.createContext("/superset/api/v1", AdminTenantControllerTest::writeOk);
            server.createContext("/osrm", AdminTenantControllerTest::writeOk);
            server.createContext("/callback-worker/consume", AdminTenantControllerTest::writeOk);
            server.createContext("/observability/logs", AdminTenantControllerTest::writeOk);
            server.createContext("/observability/traces", AdminTenantControllerTest::writeOk);
            server.createContext("/observability/alerts", AdminTenantControllerTest::writeOk);
            server.createContext("/observability/dashboard", AdminTenantControllerTest::writeOk);
            server.createContext("/delivery/github/lsjAnne/saas", AdminTenantControllerTest::writeOk);
            server.createContext("/delivery/registry/lsjAnne/dian-shang-ping-tai", AdminTenantControllerTest::writeOk);
            server.createContext("/delivery/standard-saas", AdminTenantControllerTest::writeOk);
            server.createContext("/delivery/private", AdminTenantControllerTest::writeOk);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("failed to start external http probe server", exception);
        }
    }

    private static void writeOk(HttpExchange exchange) throws IOException {
        byte[] body = "{\"status\":\"ok\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static TcpProbeServer createRabbitMqProbeServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                while (!serverSocket.isClosed()) {
                    try (Socket socket = serverSocket.accept()) {
                        socket.getOutputStream().write(0);
                        socket.getOutputStream().flush();
                    } catch (IOException exception) {
                        if (!serverSocket.isClosed()) {
                            throw new IllegalStateException("rabbitmq probe accept failed", exception);
                        }
                    }
                }
            });
            return new TcpProbeServer(serverSocket, executor);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to start rabbitmq probe server", exception);
        }
    }

    private record TcpProbeServer(ServerSocket serverSocket, ExecutorService executor) {
        private int port() {
            return serverSocket.getLocalPort();
        }

        private void close() {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            executor.shutdownNow();
        }
    }
}

record InlinedTestPropertiesState(PropertySource<?> propertySource, String previousName, String nextName) {
}
