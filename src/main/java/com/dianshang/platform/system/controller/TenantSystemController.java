package com.dianshang.platform.system.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.live.application.LiveApplicationService;
import com.dianshang.platform.notification.application.NotificationApplicationService;
import com.dianshang.platform.notification.application.NotificationGatewayProperties;
import com.dianshang.platform.openplatform.application.OpenPlatformApplicationService;
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
    private final Environment environment;

    public TenantSystemController(AuditLogService auditLogService,
                                  NotificationApplicationService notificationApplicationService,
                                  NotificationGatewayProperties notificationGatewayProperties,
                                  OpenPlatformApplicationService openPlatformApplicationService,
                                  LiveApplicationService liveApplicationService,
                                  ExternalRoutingAdapter externalRoutingAdapter,
                                  Environment environment) {
        this.auditLogService = auditLogService;
        this.notificationApplicationService = notificationApplicationService;
        this.notificationGatewayProperties = notificationGatewayProperties;
        this.openPlatformApplicationService = openPlatformApplicationService;
        this.liveApplicationService = liveApplicationService;
        this.externalRoutingAdapter = externalRoutingAdapter;
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
                        isDeliveryPipelineReady(),
                        isDualDeliveryAcceptanceReady(),
                        resolveDeliveryRepository(),
                        buildObservabilityStack(),
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
        return (int) resolveRequiredExternalSystems().stream()
                .filter(this::isExternalSystemReady)
                .count();
    }

    private boolean isExternalSystemReady(String systemCode) {
        String prefix = "app.integrations.external.systems." + systemCode + ".";
        boolean credentialConfigured = environment.getProperty(prefix + "credential-configured", Boolean.class, false);
        boolean callbackRequired = environment.getProperty(prefix + "callback-required", Boolean.class, false);
        String endpoint = environment.getProperty(prefix + "endpoint", "");
        String callbackUrl = environment.getProperty(prefix + "callback-url", "");
        return !endpoint.isBlank()
                && credentialConfigured
                && (!callbackRequired || !callbackUrl.isBlank());
    }

    private boolean isObservabilityStackReady() {
        return isConfigured("app.observability.log-aggregation-endpoint")
                && isConfigured("app.observability.trace-endpoint")
                && isConfigured("app.observability.alert-router-endpoint")
                && isConfigured("app.observability.dashboard-url");
    }

    private boolean isDeliveryPipelineReady() {
        return isConfigured("app.delivery.github-owner")
                && isConfigured("app.delivery.github-repository")
                && isConfigured("app.delivery.registry")
                && isConfigured("app.delivery.image-repository")
                && environment.getProperty("app.delivery.release-key-configured", Boolean.class, false)
                && environment.getProperty("app.delivery.canary-enabled", Boolean.class, false);
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
        return new ObservabilityStackView(
                buildEndpointView("app.observability.log-aggregation-endpoint"),
                buildEndpointView("app.observability.trace-endpoint"),
                buildEndpointView("app.observability.alert-router-endpoint"),
                buildEndpointView("app.observability.dashboard-url")
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

    private ObservabilityEndpointView buildEndpointView(String key) {
        String rawEndpoint = environment.getProperty(key, "");
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            return new ObservabilityEndpointView(false, "", "");
        }
        try {
            URI uri = URI.create(rawEndpoint);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme();
            String host = uri.getHost() == null ? "" : uri.getHost();
            String authority = host;
            if (uri.getPort() >= 0) {
                authority = authority + ":" + uri.getPort();
            }
            String maskedEndpoint = scheme.isBlank() || authority.isBlank()
                    ? "***"
                    : scheme + "://" + authority + "/***";
            return new ObservabilityEndpointView(true, host, maskedEndpoint);
        } catch (IllegalArgumentException exception) {
            return new ObservabilityEndpointView(true, "", "***");
        }
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
        ObservabilityStackView observabilityStack,
        ExternalBiPlatformView externalBiPlatform,
        ExternalRoutingView externalRouting
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
        String maskedEndpoint
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
