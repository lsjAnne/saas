package backend.system.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import backend.notification.application.NotificationGatewayProperties;
import backend.fulfillment.application.ExternalRoutingAdapter;
import backend.saas.application.SaasTenantService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExternalDependencyObservabilityConfiguration {

    @Bean(destroyMethod = "close")
    @Profile("stage13-local")
    public Stage13LocalHttpProbeServer stage13LocalHttpProbeServer(Environment environment) {
        try {
            int port = environment.getProperty("app.stage13-local.http-port", Integer.class, 18081);
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/ofbiz/webtools/control", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/openboxes/api", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/tax/api", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/superset/api/v1", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/osrm", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/callback-worker/consume", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/observability/logs", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/observability/traces", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/observability/alerts", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/observability/dashboard", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/delivery/github/lsjAnne/saas", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/delivery/registry/lsjAnne/dian-shang-ping-tai", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/delivery/standard-saas", ExternalDependencyObservabilityConfiguration::writeOk);
            server.createContext("/delivery/private", ExternalDependencyObservabilityConfiguration::writeOk);
            ExecutorService executor = Executors.newCachedThreadPool();
            server.setExecutor(executor);
            server.start();
            return new Stage13LocalHttpProbeServer(server, executor);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to start stage13-local http probe server", exception);
        }
    }

    @Bean(destroyMethod = "close")
    @Profile("stage13-local")
    public Stage13LocalTcpProbeServer stage13LocalTcpProbeServer(Environment environment) {
        try {
            int port = environment.getProperty("app.stage13-local.tcp-port", Integer.class, 15671);
            ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"));
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                while (!serverSocket.isClosed()) {
                    try (Socket socket = serverSocket.accept()) {
                        socket.getOutputStream().write(0);
                        socket.getOutputStream().flush();
                    } catch (IOException exception) {
                        if (!serverSocket.isClosed()) {
                            throw new IllegalStateException("stage13-local tcp probe accept failed", exception);
                        }
                    }
                }
            });
            return new Stage13LocalTcpProbeServer(serverSocket, executor);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to start stage13-local tcp probe server", exception);
        }
    }

    @Bean
    public InfoContributor externalDependencyInfoContributor(NotificationGatewayProperties notificationGatewayProperties,
                                                             ExternalRoutingAdapter externalRoutingAdapter,
                                                             SaasTenantService saasTenantService,
                                                             Environment environment) {
        return builder -> builder.withDetail("externalDependencies", Map.ofEntries(
                Map.entry("notificationGatewayCount", notificationGatewayProperties.listProviders().size()),
                Map.entry("enabledNotificationGatewayCount", notificationGatewayProperties.enabledProviderCount()),
                Map.entry("mockNotificationGatewayCount", notificationGatewayProperties.mockProviderCount()),
                Map.entry("realNotificationGatewayCount", notificationGatewayProperties.realProviderCount()),
                Map.entry("supportedDeploymentModes", resolveSupportedModes(environment)),
                Map.entry("managementExposure", environment.getProperty("management.endpoints.web.exposure.include", "")),
                Map.entry("requiredExternalIntegrationCount", resolveRequiredExternalSystems(environment).size()),
                Map.entry("readyExternalIntegrationCount", countReadyExternalSystems(saasTenantService)),
                Map.entry("observabilityStackReady", isObservabilityStackReady(saasTenantService)),
                Map.entry("deliveryPipelineReady", saasTenantService.getDeliveryPipelineSnapshot().ready()),
                Map.entry("dualDeliveryAcceptanceReady", saasTenantService.getDualDeliveryAcceptanceSnapshot().ready()),
                Map.entry("dualDeliveryAcceptance", saasTenantService.getDualDeliveryAcceptanceSnapshot()),
                Map.entry("deliveryRepository", resolveDeliveryRepository(environment)),
                Map.entry("deliveryPipeline", saasTenantService.getDeliveryPipelineSnapshot()),
                Map.entry("observabilityStack", buildObservabilityStack(saasTenantService)),
                Map.entry("externalIntegrationConnectivity", saasTenantService.getExternalIntegrationConnectivitySnapshot()),
                Map.entry("externalErpPlatform", buildExternalErpPlatformSummary(environment)),
                Map.entry("externalWmsPlatform", buildExternalWmsPlatformSummary(environment)),
                Map.entry("externalMessagingPlatform", buildExternalMessagingPlatformSummary(environment, saasTenantService)),
                Map.entry("externalBiPlatform", buildExternalBiPlatformSummary(environment)),
                Map.entry("externalRouting", buildRoutingSummary(externalRoutingAdapter))
        ));
    }

    @Bean
    public HealthIndicator externalDependenciesHealthIndicator(NotificationGatewayProperties notificationGatewayProperties,
                                                               ExternalRoutingAdapter externalRoutingAdapter,
                                                               SaasTenantService saasTenantService,
                                                               Environment environment) {
        return () -> Health.up()
                .withDetail("notificationGatewayCount", notificationGatewayProperties.listProviders().size())
                .withDetail("enabledNotificationGatewayCount", notificationGatewayProperties.enabledProviderCount())
                .withDetail("mockNotificationGatewayCount", notificationGatewayProperties.mockProviderCount())
                .withDetail("realNotificationGatewayCount", notificationGatewayProperties.realProviderCount())
                .withDetail("supportedDeploymentModes", resolveSupportedModes(environment))
                .withDetail("requiredExternalIntegrationCount", resolveRequiredExternalSystems(environment).size())
                .withDetail("readyExternalIntegrationCount", countReadyExternalSystems(saasTenantService))
                .withDetail("observabilityStackReady", isObservabilityStackReady(saasTenantService))
                .withDetail("deliveryPipelineReady", saasTenantService.getDeliveryPipelineSnapshot().ready())
                .withDetail("dualDeliveryAcceptanceReady", saasTenantService.getDualDeliveryAcceptanceSnapshot().ready())
                .withDetail("dualDeliveryAcceptance", saasTenantService.getDualDeliveryAcceptanceSnapshot())
                .withDetail("deliveryRepository", resolveDeliveryRepository(environment))
                .withDetail("deliveryPipeline", saasTenantService.getDeliveryPipelineSnapshot())
                .withDetail("observabilityStack", buildObservabilityStack(saasTenantService))
                .withDetail("externalIntegrationConnectivity", saasTenantService.getExternalIntegrationConnectivitySnapshot())
                .withDetail("externalErpPlatform", buildExternalErpPlatformSummary(environment))
                .withDetail("externalWmsPlatform", buildExternalWmsPlatformSummary(environment))
                .withDetail("externalMessagingPlatform", buildExternalMessagingPlatformSummary(environment, saasTenantService))
                .withDetail("externalBiPlatform", buildExternalBiPlatformSummary(environment))
                .withDetail("externalRouting", buildRoutingSummary(externalRoutingAdapter))
                .build();
    }

    private List<String> resolveSupportedModes(Environment environment) {
        return Arrays.stream(environment.getProperty("app.deployment.supported-modes", "standard-saas,private-deployment").split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private List<String> resolveRequiredExternalSystems(Environment environment) {
        return Arrays.stream(environment.getProperty("app.integrations.external.required-systems", "erp,wms,tax,messaging,bi,routing").split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private int countReadyExternalSystems(SaasTenantService saasTenantService) {
        return saasTenantService.countReadyRequiredExternalSystems();
    }

    private boolean isObservabilityStackReady(SaasTenantService saasTenantService) {
        SaasTenantService.ObservabilityStackConnectivitySnapshot snapshot =
                saasTenantService.getObservabilityStackConnectivitySnapshot();
        return isObservabilityEndpointReady(snapshot.logAggregation())
                && isObservabilityEndpointReady(snapshot.trace())
                && isObservabilityEndpointReady(snapshot.alertRouter())
                && isObservabilityEndpointReady(snapshot.dashboard());
    }

    private boolean isObservabilityEndpointReady(SaasTenantService.ExternalSystemConnectivitySnapshot snapshot) {
        return snapshot.configured()
                && snapshot.trusted()
                && snapshot.reachable();
    }

    private String resolveDeliveryRepository(Environment environment) {
        String owner = environment.getProperty("app.delivery.github-owner", "");
        String repository = environment.getProperty("app.delivery.github-repository", "");
        if (owner.isBlank() || repository.isBlank()) {
            return "";
        }
        return owner + "/" + repository;
    }

    private boolean isConfigured(Environment environment, String key) {
        String value = environment.getProperty(key, "");
        return value != null && !value.isBlank();
    }

    private Map<String, Object> buildObservabilityStack(SaasTenantService saasTenantService) {
        SaasTenantService.ObservabilityStackConnectivitySnapshot snapshot =
                saasTenantService.getObservabilityStackConnectivitySnapshot();
        return Map.of(
                "logAggregation", buildEndpointSummary(snapshot.logAggregation()),
                "trace", buildEndpointSummary(snapshot.trace()),
                "alertRouter", buildEndpointSummary(snapshot.alertRouter()),
                "dashboard", buildEndpointSummary(snapshot.dashboard())
        );
    }

    private Map<String, Object> buildExternalErpPlatformSummary(Environment environment) {
        String endpoint = environment.getProperty("app.integrations.external.erp.endpoint", "");
        return Map.of(
                "provider", normalizedProperty(environment, "app.integrations.external.erp.provider", "ofbiz"),
                "configured", endpoint != null && !endpoint.isBlank(),
                "host", extractHost(endpoint),
                "maskedEndpoint", maskEndpoint(endpoint),
                "partySyncEnabled", environment.getProperty("app.integrations.external.erp.party-sync-enabled", Boolean.class, false),
                "orderSyncMode", normalizedProperty(environment, "app.integrations.external.erp.order-sync-mode", "manual"),
                "ledgerMappingCount", environment.getProperty("app.integrations.external.erp.ledger-mapping-count", Integer.class, 0),
                "catalogExportEnabled", environment.getProperty("app.integrations.external.erp.catalog-export-enabled", Boolean.class, false)
        );
    }

    private Map<String, Object> buildExternalWmsPlatformSummary(Environment environment) {
        String endpoint = environment.getProperty("app.integrations.external.wms.endpoint", "");
        return Map.of(
                "provider", normalizedProperty(environment, "app.integrations.external.wms.provider", "openboxes"),
                "configured", endpoint != null && !endpoint.isBlank(),
                "host", extractHost(endpoint),
                "maskedEndpoint", maskEndpoint(endpoint),
                "facilityCount", environment.getProperty("app.integrations.external.wms.facility-count", Integer.class, 0),
                "stockSyncMode", normalizedProperty(environment, "app.integrations.external.wms.stock-sync-mode", "manual"),
                "outboundFlow", normalizedProperty(environment, "app.integrations.external.wms.outbound-flow", "manual"),
                "batchTrackingEnabled", environment.getProperty("app.integrations.external.wms.batch-tracking-enabled", Boolean.class, false)
        );
    }

    private Map<String, Object> buildExternalMessagingPlatformSummary(Environment environment,
                                                                      SaasTenantService saasTenantService) {
        String endpoint = environment.getProperty("app.integrations.external.messaging.endpoint", "");
        SaasTenantService.ExternalMessagingCallbackWorkerSnapshot callbackWorker =
                saasTenantService.getExternalMessagingCallbackWorkerSnapshot();
        return Map.ofEntries(
                Map.entry("provider", normalizedProperty(environment, "app.integrations.external.messaging.provider", "rabbitmq")),
                Map.entry("configured", endpoint != null && !endpoint.isBlank()),
                Map.entry("host", extractHost(endpoint)),
                Map.entry("maskedEndpoint", maskEndpoint(endpoint)),
                Map.entry("virtualHost", normalizedProperty(environment, "app.integrations.external.messaging.virtual-host", "")),
                Map.entry("exchange", normalizedProperty(environment, "app.integrations.external.messaging.exchange", "")),
                Map.entry("queueCount", environment.getProperty("app.integrations.external.messaging.queue-count", Integer.class, 0)),
                Map.entry("callbackBridgeEnabled", environment.getProperty("app.integrations.external.messaging.callback-bridge-enabled", Boolean.class, false)),
                Map.entry("deadLetterEnabled", environment.getProperty("app.integrations.external.messaging.dead-letter-enabled", Boolean.class, false)),
                Map.entry("callbackWorkerEnabled", callbackWorker.enabled()),
                Map.entry("callbackWorkerProvider", callbackWorker.provider()),
                Map.entry("callbackWorkerMaskedEndpoint", callbackWorker.maskedEndpoint()),
                Map.entry("callbackWorkerConsumerGroup", callbackWorker.consumerGroup()),
                Map.entry("callbackWorkerReady", callbackWorker.ready()),
                Map.entry("callbackWorkerMissingParts", callbackWorker.missingParts()),
                Map.entry("callbackWorkerProbeReachable", callbackWorker.probeReachable()),
                Map.entry("callbackWorkerProbeDetail", callbackWorker.probeDetail())
        );
    }

    private Map<String, Object> buildExternalBiPlatformSummary(Environment environment) {
        String endpoint = environment.getProperty("app.integrations.external.bi.endpoint", "");
        return Map.of(
                "provider", normalizedProperty(environment, "app.integrations.external.bi.provider", "superset"),
                "configured", endpoint != null && !endpoint.isBlank(),
                "host", extractHost(endpoint),
                "maskedEndpoint", maskEndpoint(endpoint),
                "dashboardCount", environment.getProperty("app.integrations.external.bi.dashboard-count", Integer.class, 0),
                "datasetCount", environment.getProperty("app.integrations.external.bi.dataset-count", Integer.class, 0),
                "embedEnabled", environment.getProperty("app.integrations.external.bi.embed-enabled", Boolean.class, false)
        );
    }

    private Map<String, Object> buildRoutingSummary(ExternalRoutingAdapter externalRoutingAdapter) {
        ExternalRoutingAdapter.RoutingAdapterSummary summary = externalRoutingAdapter.getSummary();
        return Map.of(
                "provider", summary.provider(),
                "configured", summary.configured(),
                "fallbackEnabled", summary.fallbackEnabled(),
                "profile", summary.profile(),
                "host", summary.host(),
                "maskedEndpoint", summary.maskedEndpoint()
        );
    }

    private Map<String, Object> buildEndpointSummary(SaasTenantService.ExternalSystemConnectivitySnapshot snapshot) {
        return Map.of(
                "configured", snapshot.configured(),
                "host", snapshot.host(),
                "maskedEndpoint", snapshot.maskedTarget(),
                "sourceType", snapshot.sourceType(),
                "sourceName", snapshot.sourceName(),
                "defaultValue", snapshot.defaultValue(),
                "trusted", snapshot.trusted(),
                "status", snapshot.status(),
                "probeReachable", snapshot.reachable(),
                "probeDetail", snapshot.detail()
        );
    }

    private String normalizedProperty(Environment environment, String key, String defaultValue) {
        String value = environment.getProperty(key, defaultValue);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String extractHost(String rawEndpoint) {
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(rawEndpoint);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String maskEndpoint(String rawEndpoint) {
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(rawEndpoint);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme();
            String host = uri.getHost() == null ? "" : uri.getHost();
            String authority = host;
            if (uri.getPort() >= 0) {
                authority = authority + ":" + uri.getPort();
            }
            if (scheme.isBlank() || authority.isBlank()) {
                return "***";
            }
            return scheme + "://" + authority + "/***";
        } catch (IllegalArgumentException exception) {
            return "***";
        }
    }

    private static void writeOk(HttpExchange exchange) throws IOException {
        byte[] body = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    public record Stage13LocalHttpProbeServer(HttpServer server, ExecutorService executor) {
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    public record Stage13LocalTcpProbeServer(ServerSocket serverSocket, ExecutorService executor) {
        public void close() {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            executor.shutdownNow();
        }
    }
}

