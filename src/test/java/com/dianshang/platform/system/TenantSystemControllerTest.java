package com.dianshang.platform.system;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.notification.application.NotificationApplicationService;
import com.dianshang.platform.notification.application.NotificationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        "app.integrations.external.messaging.provider=rabbitmq",
        "app.integrations.external.messaging.endpoint=amqps://rabbitmq.example.com:5671",
        "app.integrations.external.messaging.virtual-host=tenant-hub",
        "app.integrations.external.messaging.exchange=tenant.events",
        "app.integrations.external.messaging.queue-count=4",
        "app.integrations.external.messaging.callback-bridge-enabled=true",
        "app.integrations.external.messaging.dead-letter-enabled=true",
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
        "app.delivery.canary-enabled=true",
        "app.delivery.standard-saas-base-url=https://saas.example.com",
        "app.delivery.standard-saas-verified-at=2026-06-10T15:10:00+08:00",
        "app.delivery.private-base-url=https://private.example.com",
        "app.delivery.private-verified-at=2026-06-10T15:25:00+08:00"
})
@AutoConfigureMockMvc
class TenantSystemControllerTest {

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
        registry.add("app.integrations.external.probe-timeout-millis", () -> "1000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationApplicationService notificationApplicationService;

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
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.configuredCount").value(5))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.reachableCount").value(5))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.erp.detail").value("http 200"))
                .andExpect(jsonPath("$.data.externalIntegrationConnectivity.messaging.detail").value("tcp connected"))
                .andExpect(jsonPath("$.data.observabilityStackReady").value(true))
                .andExpect(jsonPath("$.data.deliveryPipelineReady").value(true))
                .andExpect(jsonPath("$.data.dualDeliveryAcceptanceReady").value(true))
                .andExpect(jsonPath("$.data.deliveryRepository").value("lsjAnne/saas"))
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
                .andExpect(jsonPath("$.data.observabilityStack.logAggregation.configured").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.logAggregation.host").value("logs.example.com"))
                .andExpect(jsonPath("$.data.observabilityStack.logAggregation.maskedEndpoint").value("https://logs.example.com/***"))
                .andExpect(jsonPath("$.data.observabilityStack.trace.configured").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.trace.host").value("trace.example.com"))
                .andExpect(jsonPath("$.data.observabilityStack.trace.maskedEndpoint").value("https://trace.example.com/***"))
                .andExpect(jsonPath("$.data.observabilityStack.alertRouter.configured").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.alertRouter.host").value("alerts.example.com"))
                .andExpect(jsonPath("$.data.observabilityStack.alertRouter.maskedEndpoint").value("https://alerts.example.com/***"))
                .andExpect(jsonPath("$.data.observabilityStack.dashboard.configured").value(true))
                .andExpect(jsonPath("$.data.observabilityStack.dashboard.host").value("grafana.example.com"))
                .andExpect(jsonPath("$.data.observabilityStack.dashboard.maskedEndpoint").value("https://grafana.example.com/***"));
    }

    @Test
    void shouldExposeActuatorObservabilityEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components.externalDependencies.status").value("UP"))
                .andExpect(jsonPath("$.components.externalDependencies.details.externalErpPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.components.externalDependencies.details.externalWmsPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.components.externalDependencies.details.externalMessagingPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.components.externalDependencies.details.externalBiPlatform.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.components.externalDependencies.details.externalRouting.host").value("127.0.0.1"))
                .andExpect(jsonPath("$.components.externalDependencies.details.externalIntegrationConnectivity.ready").value(true))
                .andExpect(jsonPath("$.components.externalDependencies.details.externalIntegrationConnectivity.reachableCount").value(5))
                .andExpect(jsonPath("$.components.externalDependencies.details.observabilityStack.logAggregation.configured").value(true))
                .andExpect(jsonPath("$.components.externalDependencies.details.observabilityStack.logAggregation.host").value("logs.example.com"))
                .andExpect(jsonPath("$.components.externalDependencies.details.observabilityStack.trace.host").value("trace.example.com"))
                .andExpect(jsonPath("$.components.externalDependencies.details.observabilityStack.alertRouter.host").value("alerts.example.com"))
                .andExpect(jsonPath("$.components.externalDependencies.details.observabilityStack.dashboard.host").value("grafana.example.com"));

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
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.logAggregation.host").value("logs.example.com"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.logAggregation.maskedEndpoint").value("https://logs.example.com/***"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.trace.host").value("trace.example.com"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.trace.maskedEndpoint").value("https://trace.example.com/***"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.alertRouter.host").value("alerts.example.com"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.alertRouter.maskedEndpoint").value("https://alerts.example.com/***"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.dashboard.host").value("grafana.example.com"))
                .andExpect(jsonPath("$.externalDependencies.observabilityStack.dashboard.maskedEndpoint").value("https://grafana.example.com/***"));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("# HELP")));
    }

    private static HttpServer createExternalHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/ofbiz/webtools/control", TenantSystemControllerTest::writeOk);
            server.createContext("/openboxes/api", TenantSystemControllerTest::writeOk);
            server.createContext("/superset/api/v1", TenantSystemControllerTest::writeOk);
            server.createContext("/osrm", TenantSystemControllerTest::writeOk);
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
