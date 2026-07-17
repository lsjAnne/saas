package backend.system.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import backend.audit.application.AuditLogService;
import backend.notification.application.NotificationApplicationService;
import backend.notification.application.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
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
        "app.integrations.external.tax.provider=tax",
        "app.integrations.external.tax.endpoint=https://tax.example.com/api",
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
        "app.integrations.external.bi.provider=superset",
        "app.integrations.external.bi.endpoint=https://bi.example.com/api",
        "app.integrations.external.bi.dashboard-count=12",
        "app.integrations.external.bi.dataset-count=36",
        "app.integrations.external.bi.embed-enabled=true",
        "app.integrations.external.systems.routing.endpoint=https://router.example.com",
        "app.integrations.external.systems.routing.credential-configured=true",
        "app.integrations.external.routing.endpoint=https://router.example.com",
        "app.integrations.external.routing.profile=driving",
        "app.observability.log-aggregation-endpoint=https://logs.example.com/loki/api/v1/push?token=secret-log-token",
        "app.observability.trace-endpoint=https://trace.example.com/otlp/v1/traces",
        "app.observability.alert-router-endpoint=https://alerts.example.com/router/webhook?key=alert-secret",
        "app.observability.dashboard-url=https://grafana.example.com/d/tenant-overview?orgId=1",
        "app.delivery.github-owner=lsjAnne",
        "app.delivery.github-repository=saas",
        "app.delivery.registry=ghcr.io",
        "app.delivery.image-repository=lsjAnne/dian-shang-ping-tai",
        "app.delivery.release-key-configured=true",
        "app.delivery.registry-auth-configured=true",
        "app.delivery.github-publish-mode=github-actions",
        "app.delivery.canary-enabled=true",
        "app.delivery.canary-strategy=header-weighted",
        "app.delivery.standard-saas-base-url=https://saas.example.com",
        "app.delivery.standard-saas-verified-at=2026-06-10T15:10:00+08:00",
        "app.delivery.private-base-url=https://private.example.com",
        "app.delivery.private-verified-at=2026-06-10T15:25:00+08:00"
})
@AutoConfigureMockMvc
class TenantSystemControllerTest {
    private static final String FRESH_STANDARD_SAAS_VERIFIED_AT = OffsetDateTime.now().minusDays(2).withNano(0).toString();
    private static final String FRESH_PRIVATE_VERIFIED_AT = OffsetDateTime.now().minusDays(2).plusMinutes(15).withNano(0).toString();

    private static final HttpServer EXTERNAL_HTTP_SERVER = createExternalHttpServer();
    private static final int HTTP_PORT = EXTERNAL_HTTP_SERVER.getAddress().getPort();
    private static final TcpProbeServer RABBITMQ_TCP_SERVER = createRabbitMqProbeServer();

    @DynamicPropertySource
    static void registerExternalProbeProperties(DynamicPropertyRegistry registry) {
        String httpBase = "http://127.0.0.1:" + HTTP_PORT;
        String messagingEndpoint = "amqp://127.0.0.1:" + RABBITMQ_TCP_SERVER.port() + "/tenant-hub";
        registry.add("app.integrations.external.systems.erp.endpoint", () -> httpBase + "/ofbiz/webtools/control");
        registry.add("app.integrations.external.systems.wms.endpoint", () -> httpBase + "/openboxes/api");
        registry.add("app.integrations.external.systems.tax.endpoint", () -> httpBase + "/tax/api");
        registry.add("app.integrations.external.systems.messaging.endpoint", () -> messagingEndpoint);
        registry.add("app.integrations.external.systems.bi.endpoint", () -> httpBase + "/superset/api/v1");
        registry.add("app.integrations.external.systems.routing.endpoint", () -> httpBase + "/osrm");
        registry.add("app.integrations.external.erp.endpoint", () -> httpBase + "/ofbiz/webtools/control");
        registry.add("app.integrations.external.wms.endpoint", () -> httpBase + "/openboxes/api");
        registry.add("app.integrations.external.tax.endpoint", () -> httpBase + "/tax/api");
        registry.add("app.integrations.external.messaging.endpoint", () -> messagingEndpoint);
        registry.add("app.integrations.external.messaging.callback-worker-endpoint", () -> httpBase + "/callback-worker/consume");
        registry.add("app.integrations.external.bi.endpoint", () -> httpBase + "/superset/api/v1");
        registry.add("app.integrations.external.routing.endpoint", () -> httpBase + "/osrm");
        registry.add("app.observability.log-aggregation-endpoint", () -> httpBase + "/observability/logs");
        registry.add("app.observability.trace-endpoint", () -> httpBase + "/observability/traces");
        registry.add("app.observability.alert-router-endpoint", () -> httpBase + "/observability/alerts");
        registry.add("app.observability.dashboard-url", () -> httpBase + "/observability/dashboard");
        registry.add("app.delivery.github-probe-endpoint", () -> httpBase + "/delivery/github/lsjAnne/saas");
        registry.add("app.delivery.registry-probe-endpoint", () -> httpBase + "/delivery/registry/lsjAnne/dian-shang-ping-tai");
        registry.add("app.delivery.standard-saas-base-url", () -> httpBase + "/delivery/standard-saas");
        registry.add("app.delivery.private-base-url", () -> httpBase + "/delivery/private");
        registry.add("app.delivery.standard-saas-verified-at", () -> FRESH_STANDARD_SAAS_VERIFIED_AT);
        registry.add("app.delivery.private-verified-at", () -> FRESH_PRIVATE_VERIFIED_AT);
        registry.add("app.integrations.external.probe-timeout-millis", () -> "1000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationApplicationService notificationApplicationService;

    @Autowired
    private ConfigurableEnvironment environment;

    @AfterAll
    static void shutdownProbeServers() {
        EXTERNAL_HTTP_SERVER.stop(0);
        RABBITMQ_TCP_SERVER.close();
    }

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
                .andExpect(jsonPath("$.data.realGatewayCount").value(0))
                .andExpect(jsonPath("$.data.prometheusEndpointEnabled").value(true))
                .andExpect(jsonPath("$.data.supportedDeploymentModes[0]").value("standard-saas"))
                .andExpect(jsonPath("$.data.requiredExternalSystemCount").value(6))
                .andExpect(jsonPath("$.data.readyExternalSystemCount").value(6))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.ready").value(true))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.configuredCount").value(6))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.reachableCount").value(6))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.erp.detail").value("http 200"))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.tax.detail").value("http 200"))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.messaging.detail").value("tcp connected"))
                .andExpect(jsonPath("$.data.observabilityStackReady").value(true))
                .andExpect(jsonPath("$.data.deliveryPipelineReady").value(true))
                .andExpect(jsonPath("$.data.dualDeliveryAcceptanceReady").value(true))
                .andExpect(jsonPath("$.data.dualDeliveryAcceptance.ready").value(true))
                .andExpect(jsonPath("$.data.dualDeliveryAcceptance.standardSaas.verificationFresh").value(true))
                .andExpect(jsonPath("$.data.dualDeliveryAcceptance.privateDeployment.verificationFresh").value(true))
                .andExpect(jsonPath("$.data.deliveryRepository").value("lsjAnne/saas"))
                .andExpect(jsonPath("$.data.deliveryPipeline.githubOwner").value("lsjAnne"))
                .andExpect(jsonPath("$.data.deliveryPipeline.githubRepository").value("saas"))
                .andExpect(jsonPath("$.data.deliveryPipeline.repository").value("lsjAnne/saas"))
                .andExpect(jsonPath("$.data.deliveryPipeline.releaseKeyConfigured").value(true))
                .andExpect(jsonPath("$.data.deliveryPipeline.canaryEnabled").value(true))
                .andExpect(jsonPath("$.data.deliveryPipeline.imageRepository").value("lsjAnne/dian-shang-ping-tai"))
                .andExpect(jsonPath("$.data.deliveryPipeline.releaseKeyControl.sourceType").value("override"))
                .andExpect(jsonPath("$.data.deliveryPipeline.releaseKeyControl.status").value("configured"))
                .andExpect(jsonPath("$.data.deliveryPipeline.registryAuthControl.sourceType").value("override"))
                .andExpect(jsonPath("$.data.deliveryPipeline.registryAuthControl.status").value("configured"))
                .andExpect(jsonPath("$.data.deliveryPipeline.githubPublishingControl.sourceType").value("override"))
                .andExpect(jsonPath("$.data.deliveryPipeline.githubPublishingControl.status").value("configured"))
                .andExpect(jsonPath("$.data.deliveryPipeline.canaryControl.sourceType").value("override"))
                .andExpect(jsonPath("$.data.deliveryPipeline.canaryControl.value").value("header-weighted"))
                .andExpect(jsonPath("$.data.deliveryPipeline.workflowAsset.ready").value(true))
                .andExpect(jsonPath("$.data.deliveryPipeline.workflowAsset.path").value(".github/workflows/backend-delivery.yml"))
                .andExpect(jsonPath("$.data.deliveryPipeline.standardSaasComposeAsset.ready").value(true))
                .andExpect(jsonPath("$.data.deliveryPipeline.standardSaasComposeAsset.path").value("docker-compose.saas.yml"))
                .andExpect(jsonPath("$.data.deliveryPipeline.privateComposeAsset.ready").value(true))
                .andExpect(jsonPath("$.data.deliveryPipeline.privateComposeAsset.path").value("docker-compose.private.yml"))
                .andExpect(jsonPath("$.data.deliveryPipeline.githubProbe.protocol").value("http"))
                .andExpect(jsonPath("$.data.deliveryPipeline.githubProbe.reachable").value(true))
                .andExpect(jsonPath("$.data.deliveryPipeline.githubProbe.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.deliveryPipeline.githubProbe.detail").value("http 200"))
                .andExpect(jsonPath("$.data.deliveryPipeline.githubProbe.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.data.deliveryPipeline.registryProbe.protocol").value("http"))
                .andExpect(jsonPath("$.data.deliveryPipeline.registryProbe.reachable").value(true))
                .andExpect(jsonPath("$.data.deliveryPipeline.registryProbe.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.deliveryPipeline.registryProbe.detail").value("http 200"))
                .andExpect(jsonPath("$.data.deliveryPipeline.registryProbe.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.data.externalErpPlatform.provider").value("ofbiz"))
                .andExpect(jsonPath("$.data.externalErpPlatform.configured").value(true))
                .andExpect(jsonPath("$.data.externalErpPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.externalErpPlatform.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.externalErpPlatform.partySyncEnabled").value(true))
                .andExpect(jsonPath("$.data.externalErpPlatform.orderSyncMode").value("near_real_time"))
                .andExpect(jsonPath("$.data.externalErpPlatform.ledgerMappingCount").value(8))
                .andExpect(jsonPath("$.data.externalErpPlatform.catalogExportEnabled").value(true))
                .andExpect(jsonPath("$.data.externalWmsPlatform.provider").value("openboxes"))
                .andExpect(jsonPath("$.data.externalWmsPlatform.configured").value(true))
                .andExpect(jsonPath("$.data.externalWmsPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.externalWmsPlatform.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.externalWmsPlatform.facilityCount").value(5))
                .andExpect(jsonPath("$.data.externalWmsPlatform.stockSyncMode").value("two_way"))
                .andExpect(jsonPath("$.data.externalWmsPlatform.outboundFlow").value("wave_and_pick"))
                .andExpect(jsonPath("$.data.externalWmsPlatform.batchTrackingEnabled").value(true))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.provider").value("rabbitmq"))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.configured").value(true))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.virtualHost").value("tenant-hub"))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.exchange").value("tenant.events"))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.queueCount").value(4))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.callbackBridgeEnabled").value(true))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.deadLetterEnabled").value(true))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.callbackWorkerEnabled").value(true))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.callbackWorkerProvider").value("spring-event"))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.callbackWorkerMaskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.callbackWorkerConsumerGroup").value("open-platform-callbacks"))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.callbackWorkerReady").value(true))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.callbackWorkerMissingParts.length()").value(0))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.callbackWorkerProbeReachable").value(true))
                .andExpect(jsonPath("$.data.externalMessagingPlatform.callbackWorkerProbeDetail").value("http 200"))
                .andExpect(jsonPath("$.data.externalBiPlatform.provider").value("superset"))
                .andExpect(jsonPath("$.data.externalBiPlatform.configured").value(true))
                .andExpect(jsonPath("$.data.externalBiPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.externalBiPlatform.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.externalBiPlatform.dashboardCount").value(12))
                .andExpect(jsonPath("$.data.externalBiPlatform.datasetCount").value(36))
                .andExpect(jsonPath("$.data.externalBiPlatform.embedEnabled").value(true))
                .andExpect(jsonPath("$.data.externalRouting.provider").value("osrm"))
                .andExpect(jsonPath("$.data.externalRouting.configured").value(true))
                .andExpect(jsonPath("$.data.externalRouting.fallbackEnabled").value(true))
                .andExpect(jsonPath("$.data.externalRouting.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.externalRouting.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.observabilityStack.logAggregation.protocol").value("http"))
                .andExpect(jsonPath("$.data.observabilityStack.logAggregation.configured").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.logAggregation.reachable").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.logAggregation.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.observabilityStack.logAggregation.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.observabilityStack.logAggregation.detail").value("http 200"))
                .andExpect(jsonPath("$.data.observabilityStack.logAggregation.probeReachable").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.logAggregation.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.data.observabilityStack.trace.protocol").value("http"))
                .andExpect(jsonPath("$.data.observabilityStack.trace.configured").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.trace.reachable").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.trace.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.observabilityStack.trace.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.observabilityStack.trace.detail").value("http 200"))
                .andExpect(jsonPath("$.data.observabilityStack.trace.probeReachable").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.trace.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.data.observabilityStack.alertRouter.protocol").value("http"))
                .andExpect(jsonPath("$.data.observabilityStack.alertRouter.configured").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.alertRouter.reachable").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.alertRouter.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.observabilityStack.alertRouter.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.observabilityStack.alertRouter.detail").value("http 200"))
                .andExpect(jsonPath("$.data.observabilityStack.alertRouter.probeReachable").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.alertRouter.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.data.observabilityStack.dashboard.protocol").value("http"))
                .andExpect(jsonPath("$.data.observabilityStack.dashboard.configured").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.dashboard.reachable").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.dashboard.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.observabilityStack.dashboard.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.data.observabilityStack.dashboard.detail").value("http 200"))
                .andExpect(jsonPath("$.data.observabilityStack.dashboard.probeReachable").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.dashboard.probeDetail").value("http 200"));
    }

    @Test
    void shouldMarkExternalIntegrationsUnreadyWhenCredentialIsNotFromTrustedSource() throws Exception {
        String sourceName = "tenant-external-untrusted-credential-test";
        applyPropertyOverrides(sourceName, Map.of(
                "app.integrations.external.systems.erp.credential-configured", "${test.external.erp.credential:true}",
                "test.external.erp.credential", "true"
        ));
        try {
            mockMvc.perform(get("/api/tenant/system/observability-overview")
                            .header("X-Tenant-Id", "tenant-9001")
                            .header("X-Operator-Id", "owner-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.requiredExternalSystemCount").value(6))
                    .andExpect(jsonPath("$.data.readyExternalSystemCount").value(5))
                    .andExpect(jsonPath("$.data.externalIntegrationConnectivity.ready").value(false))
                    .andExpect(jsonPath("$.data.externalIntegrationConnectivity.configuredCount").value(6))
                    .andExpect(jsonPath("$.data.externalIntegrationConnectivity.reachableCount").value(6));

            mockMvc.perform(get("/actuator/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.externalDependencies.readyExternalIntegrationCount").value(5))
                    .andExpect(jsonPath("$.externalDependencies.externalIntegrationConnectivity.ready").value(false));
        } finally {
            removePropertyOverrides(sourceName);
        }
    }

    @Test
    void shouldExcludeOptionalExternalIntegrationsFromTenantObservabilityOverview() throws Exception {
        String tenantId = registerTenant("tenant-optional-external", "13800000024");

        mockMvc.perform(put("/api/admin/tenants/{id}/feature-toggles", tenantId)
                        .header("X-Operator-Id", "platform-ops-1")
                        .header("X-Operator-Type", "platform-ops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "featureFlags": {
                                    "external_integration_erp_required": false,
                                    "external_integration_wms_required": false,
                                    "external_integration_tax_required": false,
                                    "external_integration_messaging_required": false,
                                    "external_integration_bi_required": false,
                                    "external_integration_routing_required": false
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tenant/system/observability-overview")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "owner-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiredExternalSystemCount").value(0))
                .andExpect(jsonPath("$.data.readyExternalSystemCount").value(0))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.ready").value(true))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.configuredCount").value(0))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.reachableCount").value(0));
    }

    @Test
    void shouldExposeStaleDualDeliveryAcceptanceAsNotReadyInObservabilitySurfaces() throws Exception {
        String tenantId = registerTenant("tenant-stale-delivery", "13800000025");
        String sourceName = "stale-dual-delivery-acceptance-test";
        applyPropertyOverrides(sourceName, Map.of(
                "app.delivery.acceptance-evidence-max-age-days", "7",
                "app.delivery.standard-saas-verified-at", OffsetDateTime.now().minusDays(30).withNano(0).toString(),
                "app.delivery.private-verified-at", OffsetDateTime.now().minusDays(30).plusMinutes(15).withNano(0).toString()
        ));
        try {
            mockMvc.perform(get("/api/tenant/system/observability-overview")
                            .header("X-Tenant-Id", tenantId)
                            .header("X-Operator-Id", "owner-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.deliveryPipelineReady").value(true))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptanceReady").value(false))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.ready").value(false))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.standardSaas.verificationFresh").value(false))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.privateDeployment.verificationFresh").value(false));

            mockMvc.perform(get("/actuator/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.externalDependencies.deliveryPipelineReady").value(true))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptanceReady").value(false))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.ready").value(false))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.standardSaas.verificationFresh").value(false))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.privateDeployment.verificationFresh").value(false));

            mockMvc.perform(get("/actuator/health/externalDependencies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.details.deliveryPipelineReady").value(true))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptanceReady").value(false))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.ready").value(false))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.standardSaas.verificationFresh").value(false))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.privateDeployment.verificationFresh").value(false));
        } finally {
            removePropertyOverrides(sourceName);
        }
    }

    @Test
    void shouldExposeMissingDualDeliveryAcceptanceEvidenceAsNotReadyInObservabilitySurfaces() throws Exception {
        String tenantId = registerTenant("tenant-missing-delivery", "13800000030");
        InlinedTestPropertiesState originalDynamicProperties = replaceNamedPropertySource("Dynamic Test Properties", Map.of(
                "app.delivery.standard-saas-verified-at", "",
                "app.delivery.private-verified-at", FRESH_PRIVATE_VERIFIED_AT
        ));
        try {
            mockMvc.perform(get("/api/tenant/system/observability-overview")
                            .header("X-Tenant-Id", tenantId)
                            .header("X-Operator-Id", "owner-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptanceReady").value(false))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.ready").value(false))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.standardSaas.configured").value(false))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.standardSaas.verificationFresh").value(false))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.standardSaas.verificationStatus").value("missing"))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.standardSaas.verificationAgeDays").value(-1))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.privateDeployment.verificationFresh").value(true));

            mockMvc.perform(get("/actuator/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptanceReady").value(false))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.ready").value(false))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.standardSaas.configured").value(false))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.standardSaas.verificationFresh").value(false))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.standardSaas.verificationStatus").value("missing"))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.standardSaas.verificationAgeDays").value(-1))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.privateDeployment.verificationFresh").value(true));

            mockMvc.perform(get("/actuator/health/externalDependencies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptanceReady").value(false))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.ready").value(false))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.standardSaas.configured").value(false))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.standardSaas.verificationFresh").value(false))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.standardSaas.verificationStatus").value("missing"))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.standardSaas.verificationAgeDays").value(-1))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.privateDeployment.verificationFresh").value(true));
        } finally {
            restoreNamedPropertySource("Dynamic Test Properties", originalDynamicProperties);
        }
    }

    @Test
    void shouldExposeInvalidDualDeliveryAcceptanceEvidenceAsNotReadyInObservabilitySurfaces() throws Exception {
        String tenantId = registerTenant("tenant-invalid-delivery", "13800000028");
        String sourceName = "invalid-dual-delivery-acceptance-test";
        applyPropertyOverrides(sourceName, Map.of(
                "app.delivery.standard-saas-verified-at", "invalid-standard-evidence",
                "app.delivery.private-verified-at", "invalid-private-evidence"
        ));
        try {
            mockMvc.perform(get("/api/tenant/system/observability-overview")
                            .header("X-Tenant-Id", tenantId)
                            .header("X-Operator-Id", "owner-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptanceReady").value(false))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.ready").value(false))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.standardSaas.verificationFresh").value(false))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.standardSaas.verificationStatus").value("invalid"))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.standardSaas.verificationAgeDays").value(-1))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.privateDeployment.verificationFresh").value(false))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.privateDeployment.verificationStatus").value("invalid"))
                    .andExpect(jsonPath("$.data.dualDeliveryAcceptance.privateDeployment.verificationAgeDays").value(-1));

            mockMvc.perform(get("/actuator/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptanceReady").value(false))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.ready").value(false))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.standardSaas.verificationFresh").value(false))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.standardSaas.verificationStatus").value("invalid"))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.standardSaas.verificationAgeDays").value(-1))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.privateDeployment.verificationFresh").value(false))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.privateDeployment.verificationStatus").value("invalid"))
                    .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.privateDeployment.verificationAgeDays").value(-1));

            mockMvc.perform(get("/actuator/health/externalDependencies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptanceReady").value(false))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.ready").value(false))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.standardSaas.verificationFresh").value(false))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.standardSaas.verificationStatus").value("invalid"))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.standardSaas.verificationAgeDays").value(-1))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.privateDeployment.verificationFresh").value(false))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.privateDeployment.verificationStatus").value("invalid"))
                    .andExpect(jsonPath("$.details.dualDeliveryAcceptance.privateDeployment.verificationAgeDays").value(-1));
        } finally {
            removePropertyOverrides(sourceName);
        }
    }

    @Test
    void shouldExposeMissingDeliveryAssetsInObservabilitySurfaces() throws Exception {
        String tenantId = registerTenant("tenant-missing-delivery-assets", "13800000026");
        String sourceName = "tenant-missing-delivery-assets-test";
        applyPropertyOverrides(sourceName, Map.of(
                "app.delivery.workflow-path", ".tools/missing/backend-delivery.yml",
                "app.delivery.standard-saas-compose-path", ".tools/missing/docker-compose.saas.yml",
                "app.delivery.private-compose-path", ".tools/missing/docker-compose.private.yml"
        ));
        try {
            mockMvc.perform(get("/api/tenant/system/observability-overview")
                            .header("X-Tenant-Id", tenantId)
                            .header("X-Operator-Id", "owner-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.deliveryPipelineReady").value(false))
                    .andExpect(jsonPath("$.data.deliveryPipeline.ready").value(false))
                    .andExpect(jsonPath("$.data.deliveryPipeline.workflowAsset.ready").value(false))
                    .andExpect(jsonPath("$.data.deliveryPipeline.workflowAsset.detail").value("file not found"))
                    .andExpect(jsonPath("$.data.deliveryPipeline.standardSaasComposeAsset.ready").value(false))
                    .andExpect(jsonPath("$.data.deliveryPipeline.standardSaasComposeAsset.detail").value("file not found"))
                    .andExpect(jsonPath("$.data.deliveryPipeline.privateComposeAsset.ready").value(false))
                    .andExpect(jsonPath("$.data.deliveryPipeline.privateComposeAsset.detail").value("file not found"));

            mockMvc.perform(get("/actuator/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.externalDependencies.deliveryPipelineReady").value(false))
                    .andExpect(jsonPath("$.externalDependencies.deliveryPipeline.ready").value(false))
                    .andExpect(jsonPath("$.externalDependencies.deliveryPipeline.workflowAsset.ready").value(false))
                    .andExpect(jsonPath("$.externalDependencies.deliveryPipeline.standardSaasComposeAsset.ready").value(false))
                    .andExpect(jsonPath("$.externalDependencies.deliveryPipeline.privateComposeAsset.ready").value(false));

            mockMvc.perform(get("/actuator/health/externalDependencies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.details.deliveryPipelineReady").value(false))
                    .andExpect(jsonPath("$.details.deliveryPipeline.ready").value(false))
                    .andExpect(jsonPath("$.details.deliveryPipeline.workflowAsset.ready").value(false))
                    .andExpect(jsonPath("$.details.deliveryPipeline.standardSaasComposeAsset.ready").value(false))
                    .andExpect(jsonPath("$.details.deliveryPipeline.privateComposeAsset.ready").value(false));
        } finally {
            removePropertyOverrides(sourceName);
        }
    }

    @Test
    void shouldExposeActuatorObservabilityEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components.redis.status").value("DOWN"))
                .andExpect(jsonPath("$.components.externalDependencies.status").value("UP"));

        mockMvc.perform(get("/actuator/health/externalDependencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details.externalErpPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.details.externalWmsPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.details.externalMessagingPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.details.externalMessagingPlatform.callbackWorkerEnabled").value(true))
                .andExpect(jsonPath("$.details.externalMessagingPlatform.callbackWorkerProvider").value("spring-event"))
                .andExpect(jsonPath("$.details.externalMessagingPlatform.callbackWorkerMaskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.details.externalMessagingPlatform.callbackWorkerConsumerGroup").value("open-platform-callbacks"))
                .andExpect(jsonPath("$.details.externalMessagingPlatform.callbackWorkerReady").value(true))
                .andExpect(jsonPath("$.details.externalMessagingPlatform.callbackWorkerMissingParts.length()").value(0))
                .andExpect(jsonPath("$.details.externalMessagingPlatform.callbackWorkerProbeReachable").value(true))
                .andExpect(jsonPath("$.details.externalMessagingPlatform.callbackWorkerProbeDetail").value("http 200"))
                .andExpect(jsonPath("$.details.externalBiPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.details.externalRouting.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.details.externalIntegrationConnectivity.ready").value(true))
                .andExpect(jsonPath("$.details.externalIntegrationConnectivity.reachableCount").value(6))
                .andExpect(jsonPath("$.details.observabilityStack.logAggregation.configured").value(true))
                .andExpect(jsonPath("$.details.observabilityStack.logAggregation.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.details.observabilityStack.logAggregation.probeReachable").value(true))
                .andExpect(jsonPath("$.details.observabilityStack.logAggregation.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.details.observabilityStack.trace.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.details.observabilityStack.trace.probeReachable").value(true))
                .andExpect(jsonPath("$.details.observabilityStack.trace.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.details.observabilityStack.alertRouter.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.details.observabilityStack.alertRouter.probeReachable").value(true))
                .andExpect(jsonPath("$.details.observabilityStack.alertRouter.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.details.observabilityStack.dashboard.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.details.observabilityStack.dashboard.probeReachable").value(true))
                .andExpect(jsonPath("$.details.observabilityStack.dashboard.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.details.deliveryPipeline.releaseKeyControl.sourceType").value("override"))
                .andExpect(jsonPath("$.details.deliveryPipeline.registryAuthControl.sourceType").value("override"))
                .andExpect(jsonPath("$.details.deliveryPipeline.githubPublishingControl.sourceType").value("override"))
                .andExpect(jsonPath("$.details.deliveryPipeline.canaryControl.value").value("header-weighted"))
                .andExpect(jsonPath("$.details.deliveryPipeline.githubProbe.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.details.deliveryPipeline.githubProbe.detail").value("http 200"))
                .andExpect(jsonPath("$.details.deliveryPipeline.registryProbe.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.details.deliveryPipeline.registryProbe.detail").value("http 200"));

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalDependencies.notificationGatewayCount").value(4))
                .andExpect(jsonPath("$.externalDependencies.enabledNotificationGatewayCount").value(4))
                .andExpect(jsonPath("$.externalDependencies.realNotificationGatewayCount").value(0))
                .andExpect(jsonPath("$.externalDependencies.supportedDeploymentModes[0]").value("standard-saas"))
                .andExpect(jsonPath("$.externalDependencies.requiredExternalIntegrationCount").value(6))
                .andExpect(jsonPath("$.externalDependencies.readyExternalIntegrationCount").value(6))
                .andExpect(jsonPath("$.externalDependencies.externalIntegrationConnectivity.ready").value(true))
                .andExpect(jsonPath("$.externalDependencies.externalIntegrationConnectivity.erp.detail").value("http 200"))
                .andExpect(jsonPath("$.externalDependencies.externalIntegrationConnectivity.messaging.detail").value("tcp connected"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStackReady").value(true))
                .andExpect(jsonPath("$.externalDependencies.deliveryPipelineReady").value(true))
                .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptanceReady").value(true))
                .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.ready").value(true))
                .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.standardSaas.verificationFresh").value(true))
                .andExpect(jsonPath("$.externalDependencies.dualDeliveryAcceptance.privateDeployment.verificationFresh").value(true))
                .andExpect(jsonPath("$.externalDependencies.deliveryRepository").value("lsjAnne/saas"))
                .andExpect(jsonPath("$.externalDependencies.externalErpPlatform.provider").value("ofbiz"))
                .andExpect(jsonPath("$.externalDependencies.externalErpPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.externalDependencies.externalErpPlatform.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.externalDependencies.externalErpPlatform.ledgerMappingCount").value(8))
                .andExpect(jsonPath("$.externalDependencies.externalWmsPlatform.provider").value("openboxes"))
                .andExpect(jsonPath("$.externalDependencies.externalWmsPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.externalDependencies.externalWmsPlatform.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.externalDependencies.externalWmsPlatform.facilityCount").value(5))
                .andExpect(jsonPath("$.externalDependencies.externalMessagingPlatform.provider").value("rabbitmq"))
                .andExpect(jsonPath("$.externalDependencies.externalMessagingPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.externalDependencies.externalMessagingPlatform.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.externalDependencies.externalMessagingPlatform.queueCount").value(4))
                .andExpect(jsonPath("$.externalDependencies.externalMessagingPlatform.callbackWorkerEnabled").value(true))
                .andExpect(jsonPath("$.externalDependencies.externalMessagingPlatform.callbackWorkerProvider").value("spring-event"))
                .andExpect(jsonPath("$.externalDependencies.externalMessagingPlatform.callbackWorkerMaskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.externalDependencies.externalMessagingPlatform.callbackWorkerConsumerGroup").value("open-platform-callbacks"))
                .andExpect(jsonPath("$.externalDependencies.externalMessagingPlatform.callbackWorkerReady").value(true))
                .andExpect(jsonPath("$.externalDependencies.externalMessagingPlatform.callbackWorkerMissingParts.length()").value(0))
                .andExpect(jsonPath("$.externalDependencies.externalMessagingPlatform.callbackWorkerProbeReachable").value(true))
                .andExpect(jsonPath("$.externalDependencies.externalMessagingPlatform.callbackWorkerProbeDetail").value("http 200"))
                .andExpect(jsonPath("$.externalDependencies.externalBiPlatform.provider").value("superset"))
                .andExpect(jsonPath("$.externalDependencies.externalBiPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.externalDependencies.externalBiPlatform.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.externalDependencies.externalBiPlatform.dashboardCount").value(12))
                .andExpect(jsonPath("$.externalDependencies.externalBiPlatform.datasetCount").value(36))
                .andExpect(jsonPath("$.externalDependencies.externalBiPlatform.embedEnabled").value(true))
                .andExpect(jsonPath("$.externalDependencies.externalRouting.provider").value("osrm"))
                .andExpect(jsonPath("$.externalDependencies.externalRouting.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.externalDependencies.externalRouting.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.logAggregation.configured").value(true))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.logAggregation.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.logAggregation.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.logAggregation.probeReachable").value(true))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.logAggregation.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.trace.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.trace.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.trace.probeReachable").value(true))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.trace.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.alertRouter.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.alertRouter.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.alertRouter.probeReachable").value(true))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.alertRouter.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.dashboard.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.dashboard.maskedEndpoint").value(org.hamcrest.Matchers.containsString("127.0.0.1")))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.dashboard.probeReachable").value(true))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.dashboard.probeDetail").value("http 200"))
                .andExpect(jsonPath("$.externalDependencies.deliveryPipeline.releaseKeyControl.sourceType").value("override"))
                .andExpect(jsonPath("$.externalDependencies.deliveryPipeline.registryAuthControl.sourceType").value("override"))
                .andExpect(jsonPath("$.externalDependencies.deliveryPipeline.githubPublishingControl.sourceType").value("override"))
                .andExpect(jsonPath("$.externalDependencies.deliveryPipeline.canaryControl.value").value("header-weighted"))
                .andExpect(jsonPath("$.externalDependencies.deliveryPipeline.githubProbe.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.externalDependencies.deliveryPipeline.githubProbe.detail").value("http 200"))
                .andExpect(jsonPath("$.externalDependencies.deliveryPipeline.registryProbe.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.externalDependencies.deliveryPipeline.registryProbe.detail").value("http 200"));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("# HELP")));
    }

    private static HttpServer createExternalHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/ofbiz/webtools/control", TenantSystemControllerTest::writeOk);
            server.createContext("/openboxes/api", TenantSystemControllerTest::writeOk);
            server.createContext("/tax/api", TenantSystemControllerTest::writeOk);
            server.createContext("/superset/api/v1", TenantSystemControllerTest::writeOk);
            server.createContext("/osrm", TenantSystemControllerTest::writeOk);
            server.createContext("/callback-worker/consume", TenantSystemControllerTest::writeOk);
            server.createContext("/observability/logs", TenantSystemControllerTest::writeOk);
            server.createContext("/observability/traces", TenantSystemControllerTest::writeOk);
            server.createContext("/observability/alerts", TenantSystemControllerTest::writeOk);
            server.createContext("/observability/dashboard", TenantSystemControllerTest::writeOk);
            server.createContext("/delivery/github/lsjAnne/saas", TenantSystemControllerTest::writeOk);
            server.createContext("/delivery/registry/lsjAnne/dian-shang-ping-tai", TenantSystemControllerTest::writeOk);
            server.createContext("/delivery/standard-saas", TenantSystemControllerTest::writeOk);
            server.createContext("/delivery/private", TenantSystemControllerTest::writeOk);
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

    private void applyPropertyOverrides(String sourceName, Map<String, Object> properties) {
        environment.getPropertySources().addFirst(new MapPropertySource(sourceName, new LinkedHashMap<>(properties)));
    }

    private String registerTenant(String tenantName, String mobile) throws Exception {
        JsonNode root = objectMapper.readTree(mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "寮犱笁",
                                  "mobile": "%s"
                                }
                                """.formatted(tenantName, mobile)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        return root.path("data").path("tenantId").asText();
    }

    private void removePropertyOverrides(String sourceName) {
        environment.getPropertySources().remove(sourceName);
    }

    private InlinedTestPropertiesState replaceNamedPropertySource(String sourceName, Map<String, Object> overrides) {
        InlinedTestPropertiesState original = detachPropertySource(sourceName);
        environment.getPropertySources().addFirst(new MapPropertySource(sourceName, new LinkedHashMap<>(overrides)));
        return original;
    }

    private void restoreNamedPropertySource(String sourceName, InlinedTestPropertiesState original) {
        environment.getPropertySources().remove(sourceName);
        original.restoreTo(environment.getPropertySources(), sourceName);
    }

    private InlinedTestPropertiesState replaceInlinedTestProperties(Map<String, Object> overrides) {
        return replaceNamedPropertySource("Inlined Test Properties", overrides);
    }

    private void restoreInlinedTestProperties(InlinedTestPropertiesState original) {
        restoreNamedPropertySource("Inlined Test Properties", original);
    }

    private InlinedTestPropertiesState detachPropertySource(String sourceName) {
        MutablePropertySources propertySources = environment.getPropertySources();
        PropertySource<?> existing = propertySources.remove(sourceName);
        if (existing == null) {
            return InlinedTestPropertiesState.absent();
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (existing instanceof EnumerablePropertySource<?> enumerablePropertySource) {
            for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                snapshot.put(propertyName, enumerablePropertySource.getProperty(propertyName));
            }
        }
        return InlinedTestPropertiesState.present(snapshot);
    }

    private record InlinedTestPropertiesState(boolean present, Map<String, Object> properties) {
        private static InlinedTestPropertiesState absent() {
            return new InlinedTestPropertiesState(false, Map.of());
        }

        private static InlinedTestPropertiesState present(Map<String, Object> properties) {
            return new InlinedTestPropertiesState(true, Map.copyOf(properties));
        }

        private void restoreTo(MutablePropertySources propertySources, String sourceName) {
            if (!present) {
                return;
            }
            propertySources.addFirst(new MapPropertySource(sourceName, new LinkedHashMap<>(properties)));
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

