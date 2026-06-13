package com.dianshang.platform.system.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.live.application.LiveApplicationService;
import com.dianshang.platform.notification.application.NotificationApplicationService;
import com.dianshang.platform.notification.application.NotificationGatewayProperties;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.fulfillment.application.ExternalRoutingAdapter;
import com.dianshang.platform.system.dto.HealthInfo;
import com.dianshang.platform.tenant.TenantContext;
import com.dianshang.platform.tenant.TenantContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/tenant/system")
public class TenantSystemController {

    private final AuditLogService auditLogService;
    private final NotificationApplicationService notificationApplicationService;
    private final NotificationGatewayProperties notificationGatewayProperties;
    private final OpenPlatformApplicationService openPlatformApplicationService;
    private final LiveApplicationService liveApplicationService;
    private final ExternalRoutingAdapter externalRoutingAdapter;
    private final SaasTenantService saasTenantService;
    private final Environment environment;

    public TenantSystemController(AuditLogService auditLogService,
                                  NotificationApplicationService notificationApplicationService,
                                  NotificationGatewayProperties notificationGatewayProperties,
                                  OpenPlatformApplicationService openPlatformApplicationService,
                                  LiveApplicationService liveApplicationService,
                                  ExternalRoutingAdapter externalRoutingAdapter,
                                  SaasTenantService saasTenantService,
                                  Environment environment) {
        this.auditLogService = auditLogService;
        this.notificationApplicationService = notificationApplicationService;
        this.notificationGatewayProperties = notificationGatewayProperties;
        this.openPlatformApplicationService = openPlatformApplicationService;
        this.liveApplicationService = liveApplicationService;
        this.externalRoutingAdapter = externalRoutingAdapter;
        this.saasTenantService = saasTenantService;
        this.environment = environment;
    }

    @GetMapping("/health")
    public ApiResponse<HealthInfo> health() {
        TenantContext context = TenantContextHolder.get();
        HealthInfo healthInfo = new HealthInfo(
                "dianShangPingTai",
                "UP",
                context == null ? "unknown" : context.tenantId()
        );
        return ApiResponse.success(healthInfo, TraceIdHolder.get());
    }

    @GetMapping("/observability-overview")
    public ApiResponse<ObservabilityOverview> observabilityOverview() {
        TenantContext context = TenantContextHolder.get();
        String tenantId = context == null ? "unknown" : context.tenantId();
        List<String> latestTraceIds = auditLogService.findByTenantId(tenantId).stream()
                .map(record -> record.traceId())
                .filter(traceId -> traceId != null && !traceId.isBlank())
                .distinct()
                .limit(5)
                .toList();
        int failedNotificationCount = (int) notificationApplicationService.listNotifications(tenantId).stream()
                .filter(task -> "failed".equals(task.sendStatus()) || "dead_letter".equals(task.sendStatus()))
                .count();
        int rejectedOpenCallCount = (int) openPlatformApplicationService.listCallLogs(tenantId).stream()
                .filter(log -> log.resultStatus() != null && log.resultStatus().startsWith("rejected"))
                .count();
        int liveRiskEventCount = liveApplicationService.listLiveRiskEvents(tenantId).size();
        SaasTenantService.ExternalIntegrationConnectivitySnapshot externalIntegrationConnectivity =
                saasTenantService.getExternalIntegrationConnectivitySnapshot();
        SaasTenantService.DeliveryPipelineSnapshot deliveryPipeline =
                saasTenantService.getDeliveryPipelineSnapshot();
        return ApiResponse.success(
                new ObservabilityOverview(
                        tenantId,
                        auditLogService.findByTenantId(tenantId).size(),
                        failedNotificationCount,
                        openPlatformApplicationService.listCallLogs(tenantId).size(),
                        rejectedOpenCallCount,
                        liveRiskEventCount,
                        failedNotificationCount + rejectedOpenCallCount + liveRiskEventCount,
                        latestTraceIds,
                        notificationGatewayProperties.listProviders().size(),
                        notificationGatewayProperties.enabledProviderCount(),
                        notificationGatewayProperties.mockProviderCount(),
                        notificationGatewayProperties.realProviderCount(),
                        isPrometheusEndpointEnabled(),
                        resolveSupportedDeploymentModes(),
                        resolveRequiredExternalSystems().size(),
                        countReadyExternalSystems(),
                        isObservabilityStackReady(),
                        deliveryPipeline.ready(),
                        isDualDeliveryAcceptanceReady(),
                        resolveDeliveryRepository(),
                        buildDeliveryPipelineView(deliveryPipeline),
                        buildObservabilityStack(),
                        externalIntegrationConnectivity,
                        buildExternalErpPlatformView(),
                        buildExternalWmsPlatformView(),
                        buildExternalMessagingPlatformView(),
                        buildExternalBiPlatformView(),
                        buildExternalRoutingView()
                ),
                TraceIdHolder.get()
        );
    }

    private boolean isPrometheusEndpointEnabled() {
        String exposure = environment.getProperty("management.endpoints.web.exposure.include", "");
        return Arrays.stream(exposure.split(","))
                .map(String::trim)
                .anyMatch("prometheus"::equals);
    }

    private List<String> resolveSupportedDeploymentModes() {
        return Arrays.stream(environment.getProperty("app.deployment.supported-modes", "standard-saas,private-deployment").split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private List<String> resolveRequiredExternalSystems() {
        return Arrays.stream(environment.getProperty("app.integrations.external.required-systems", "erp,wms,tax,messaging,bi").split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private int countReadyExternalSystems() {
        return saasTenantService.countReadyRequiredExternalSystems();
    }

    private boolean isExternalSystemReady(String systemCode) {
        return saasTenantService.isRequiredExternalSystemReady(systemCode);
    }

    private boolean isObservabilityStackReady() {
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

    private boolean isDualDeliveryAcceptanceReady() {
        return isConfigured("app.delivery.standard-saas-base-url")
                && isConfigured("app.delivery.standard-saas-verified-at")
                && isConfigured("app.delivery.private-base-url")
                && isConfigured("app.delivery.private-verified-at");
    }

    private String resolveDeliveryRepository() {
        String owner = environment.getProperty("app.delivery.github-owner", "");
        String repository = environment.getProperty("app.delivery.github-repository", "");
        if (owner.isBlank() || repository.isBlank()) {
            return "";
        }
        return owner + "/" + repository;
    }

    private boolean isConfigured(String key) {
        String value = environment.getProperty(key, "");
        return value != null && !value.isBlank();
    }

    private ObservabilityStackView buildObservabilityStack() {
        SaasTenantService.ObservabilityStackConnectivitySnapshot snapshot =
                saasTenantService.getObservabilityStackConnectivitySnapshot();
        return new ObservabilityStackView(
                buildEndpointView(snapshot.logAggregation()),
                buildEndpointView(snapshot.trace()),
                buildEndpointView(snapshot.alertRouter()),
                buildEndpointView(snapshot.dashboard())
        );
    }

    private DeliveryPipelineObservabilityView buildDeliveryPipelineView(SaasTenantService.DeliveryPipelineSnapshot snapshot) {
        return new DeliveryPipelineObservabilityView(
                snapshot.ready(),
                snapshot.repository(),
                snapshot.registry(),
                snapshot.releaseKeyControl(),
                snapshot.registryAuthControl(),
                snapshot.githubPublishingControl(),
                snapshot.canaryControl(),
                buildEndpointView(snapshot.githubProbe()),
                buildEndpointView(snapshot.registryProbe())
        );
    }

    private ExternalErpPlatformView buildExternalErpPlatformView() {
        String endpoint = environment.getProperty("app.integrations.external.erp.endpoint", "");
        return new ExternalErpPlatformView(
                normalizedProperty("app.integrations.external.erp.provider", "ofbiz"),
                endpoint != null && !endpoint.isBlank(),
                extractHost(endpoint),
                maskEndpoint(endpoint),
                environment.getProperty("app.integrations.external.erp.party-sync-enabled", Boolean.class, false),
                normalizedProperty("app.integrations.external.erp.order-sync-mode", "manual"),
                environment.getProperty("app.integrations.external.erp.ledger-mapping-count", Integer.class, 0),
                environment.getProperty("app.integrations.external.erp.catalog-export-enabled", Boolean.class, false)
        );
    }

    private ExternalWmsPlatformView buildExternalWmsPlatformView() {
        String endpoint = environment.getProperty("app.integrations.external.wms.endpoint", "");
        return new ExternalWmsPlatformView(
                normalizedProperty("app.integrations.external.wms.provider", "openboxes"),
                endpoint != null && !endpoint.isBlank(),
                extractHost(endpoint),
                maskEndpoint(endpoint),
                environment.getProperty("app.integrations.external.wms.facility-count", Integer.class, 0),
                normalizedProperty("app.integrations.external.wms.stock-sync-mode", "manual"),
                normalizedProperty("app.integrations.external.wms.outbound-flow", "manual"),
                environment.getProperty("app.integrations.external.wms.batch-tracking-enabled", Boolean.class, false)
        );
    }

    private ExternalMessagingPlatformView buildExternalMessagingPlatformView() {
        String endpoint = environment.getProperty("app.integrations.external.messaging.endpoint", "");
        SaasTenantService.ExternalMessagingCallbackWorkerSnapshot callbackWorker =
                saasTenantService.getExternalMessagingCallbackWorkerSnapshot();
        return new ExternalMessagingPlatformView(
                normalizedProperty("app.integrations.external.messaging.provider", "rabbitmq"),
                endpoint != null && !endpoint.isBlank(),
                extractHost(endpoint),
                maskEndpoint(endpoint),
                normalizedProperty("app.integrations.external.messaging.virtual-host", ""),
                normalizedProperty("app.integrations.external.messaging.exchange", ""),
                environment.getProperty("app.integrations.external.messaging.queue-count", Integer.class, 0),
                environment.getProperty("app.integrations.external.messaging.callback-bridge-enabled", Boolean.class, false),
                environment.getProperty("app.integrations.external.messaging.dead-letter-enabled", Boolean.class, false),
                callbackWorker.enabled(),
                callbackWorker.provider(),
                callbackWorker.maskedEndpoint(),
                callbackWorker.consumerGroup(),
                callbackWorker.ready(),
                callbackWorker.missingParts(),
                callbackWorker.probeReachable(),
                callbackWorker.probeDetail()
        );
    }

    private ExternalBiPlatformView buildExternalBiPlatformView() {
        String endpoint = environment.getProperty("app.integrations.external.bi.endpoint", "");
        return new ExternalBiPlatformView(
                normalizedProperty("app.integrations.external.bi.provider", "superset"),
                endpoint != null && !endpoint.isBlank(),
                extractHost(endpoint),
                maskEndpoint(endpoint),
                environment.getProperty("app.integrations.external.bi.dashboard-count", Integer.class, 0),
                environment.getProperty("app.integrations.external.bi.dataset-count", Integer.class, 0),
                environment.getProperty("app.integrations.external.bi.embed-enabled", Boolean.class, false)
        );
    }

    private ExternalRoutingView buildExternalRoutingView() {
        ExternalRoutingAdapter.RoutingAdapterSummary summary = externalRoutingAdapter.getSummary();
        return new ExternalRoutingView(
                summary.provider(),
                summary.configured(),
                summary.fallbackEnabled(),
                summary.profile(),
                summary.host(),
                summary.maskedEndpoint()
        );
    }

    private ObservabilityEndpointView buildEndpointView(SaasTenantService.ExternalSystemConnectivitySnapshot snapshot) {
        return new ObservabilityEndpointView(
                snapshot.configured(),
                snapshot.host(),
                snapshot.maskedTarget(),
                snapshot.sourceType(),
                snapshot.sourceName(),
                snapshot.defaultValue(),
                snapshot.trusted(),
                snapshot.status(),
                snapshot.reachable(),
                snapshot.detail()
        );
    }

    private String normalizedProperty(String key, String defaultValue) {
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
}

record ObservabilityOverview(
        String tenantId,
        int auditLogCount,
        int failedNotificationCount,
        int openPlatformCallLogCount,
        int rejectedOpenPlatformCallCount,
        int liveRiskEventCount,
        int activeAlertCount,
        List<String> latestTraceIds,
        int configuredGatewayCount,
        int enabledGatewayCount,
        int mockGatewayCount,
        int realGatewayCount,
        boolean prometheusEndpointEnabled,
        List<String> supportedDeploymentModes,
        int requiredExternalSystemCount,
        int readyExternalSystemCount,
        boolean observabilityStackReady,
        boolean deliveryPipelineReady,
        boolean dualDeliveryAcceptanceReady,
        String deliveryRepository,
        DeliveryPipelineObservabilityView deliveryPipeline,
        ObservabilityStackView observabilityStack,
        SaasTenantService.ExternalIntegrationConnectivitySnapshot externalIntegrationConnectivity,
        ExternalErpPlatformView externalErpPlatform,
        ExternalWmsPlatformView externalWmsPlatform,
        ExternalMessagingPlatformView externalMessagingPlatform,
        ExternalBiPlatformView externalBiPlatform,
        ExternalRoutingView externalRouting
) {
}

record DeliveryPipelineObservabilityView(
        boolean ready,
        String repository,
        String registry,
        SaasTenantService.DeliveryControlDiagnosticSnapshot releaseKeyControl,
        SaasTenantService.DeliveryControlDiagnosticSnapshot registryAuthControl,
        SaasTenantService.DeliveryControlDiagnosticSnapshot githubPublishingControl,
        SaasTenantService.DeliveryControlDiagnosticSnapshot canaryControl,
        ObservabilityEndpointView githubProbe,
        ObservabilityEndpointView registryProbe
) {
}

record ObservabilityStackView(
        ObservabilityEndpointView logAggregation,
        ObservabilityEndpointView trace,
        ObservabilityEndpointView alertRouter,
        ObservabilityEndpointView dashboard
) {
}

record ObservabilityEndpointView(
        boolean configured,
        String host,
        String maskedEndpoint,
        String sourceType,
        String sourceName,
        boolean defaultValue,
        boolean trusted,
        String status,
        boolean probeReachable,
        String probeDetail
) {
}

record ExternalErpPlatformView(
        String provider,
        boolean configured,
        String host,
        String maskedEndpoint,
        boolean partySyncEnabled,
        String orderSyncMode,
        int ledgerMappingCount,
        boolean catalogExportEnabled
) {
}

record ExternalWmsPlatformView(
        String provider,
        boolean configured,
        String host,
        String maskedEndpoint,
        int facilityCount,
        String stockSyncMode,
        String outboundFlow,
        boolean batchTrackingEnabled
) {
}

record ExternalMessagingPlatformView(
        String provider,
        boolean configured,
        String host,
        String maskedEndpoint,
        String virtualHost,
        String exchange,
        int queueCount,
        boolean callbackBridgeEnabled,
        boolean deadLetterEnabled,
        boolean callbackWorkerEnabled,
        String callbackWorkerProvider,
        String callbackWorkerMaskedEndpoint,
        String callbackWorkerConsumerGroup,
        boolean callbackWorkerReady,
        List<String> callbackWorkerMissingParts,
        boolean callbackWorkerProbeReachable,
        String callbackWorkerProbeDetail
) {
}

record ExternalBiPlatformView(
        String provider,
        boolean configured,
        String host,
        String maskedEndpoint,
        int dashboardCount,
        int datasetCount,
        boolean embedEnabled
) {
}

record ExternalRoutingView(
        String provider,
        boolean configured,
        boolean fallbackEnabled,
        String profile,
        String host,
        String maskedEndpoint
) {
}
