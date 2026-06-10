package com.dianshang.platform.system.config;

import com.dianshang.platform.notification.application.NotificationGatewayProperties;
import com.dianshang.platform.fulfillment.application.ExternalRoutingAdapter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
public class ExternalDependencyObservabilityConfiguration {

    @Bean
    public InfoContributor externalDependencyInfoContributor(NotificationGatewayProperties notificationGatewayProperties,
                                                             ExternalRoutingAdapter externalRoutingAdapter,
                                                             Environment environment) {
        return builder -> builder.withDetail("externalDependencies", Map.ofEntries(
                Map.entry("notificationGatewayCount", notificationGatewayProperties.listProviders().size()),
                Map.entry("enabledNotificationGatewayCount", notificationGatewayProperties.enabledProviderCount()),
                Map.entry("mockNotificationGatewayCount", notificationGatewayProperties.mockProviderCount()),
                Map.entry("realNotificationGatewayCount", notificationGatewayProperties.realProviderCount()),
                Map.entry("supportedDeploymentModes", resolveSupportedModes(environment)),
                Map.entry("managementExposure", environment.getProperty("management.endpoints.web.exposure.include", "")),
                Map.entry("requiredExternalIntegrationCount", resolveRequiredExternalSystems(environment).size()),
                Map.entry("readyExternalIntegrationCount", countReadyExternalSystems(environment)),
                Map.entry("observabilityStackReady", isObservabilityStackReady(environment)),
                Map.entry("deliveryPipelineReady", isDeliveryPipelineReady(environment)),
                Map.entry("dualDeliveryAcceptanceReady", isDualDeliveryAcceptanceReady(environment)),
                Map.entry("deliveryRepository", resolveDeliveryRepository(environment)),
                Map.entry("observabilityStack", buildObservabilityStack(environment)),
                Map.entry("externalBiPlatform", buildExternalBiPlatformSummary(environment)),
                Map.entry("externalRouting", buildRoutingSummary(externalRoutingAdapter))
        ));
    }

    @Bean
    public HealthIndicator externalDependenciesHealthIndicator(NotificationGatewayProperties notificationGatewayProperties,
                                                               ExternalRoutingAdapter externalRoutingAdapter,
                                                               Environment environment) {
        return () -> Health.up()
                .withDetail("notificationGatewayCount", notificationGatewayProperties.listProviders().size())
                .withDetail("enabledNotificationGatewayCount", notificationGatewayProperties.enabledProviderCount())
                .withDetail("mockNotificationGatewayCount", notificationGatewayProperties.mockProviderCount())
                .withDetail("realNotificationGatewayCount", notificationGatewayProperties.realProviderCount())
                .withDetail("supportedDeploymentModes", resolveSupportedModes(environment))
                .withDetail("requiredExternalIntegrationCount", resolveRequiredExternalSystems(environment).size())
                .withDetail("readyExternalIntegrationCount", countReadyExternalSystems(environment))
                .withDetail("observabilityStackReady", isObservabilityStackReady(environment))
                .withDetail("deliveryPipelineReady", isDeliveryPipelineReady(environment))
                .withDetail("dualDeliveryAcceptanceReady", isDualDeliveryAcceptanceReady(environment))
                .withDetail("deliveryRepository", resolveDeliveryRepository(environment))
                .withDetail("observabilityStack", buildObservabilityStack(environment))
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
        return Arrays.stream(environment.getProperty("app.integrations.external.required-systems", "erp,wms,tax,messaging,bi").split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private int countReadyExternalSystems(Environment environment) {
        return (int) resolveRequiredExternalSystems(environment).stream()
                .filter(systemCode -> isExternalSystemReady(environment, systemCode))
                .count();
    }

    private boolean isExternalSystemReady(Environment environment, String systemCode) {
        String prefix = "app.integrations.external.systems." + systemCode + ".";
        boolean credentialConfigured = environment.getProperty(prefix + "credential-configured", Boolean.class, false);
        boolean callbackRequired = environment.getProperty(prefix + "callback-required", Boolean.class, false);
        String endpoint = environment.getProperty(prefix + "endpoint", "");
        String callbackUrl = environment.getProperty(prefix + "callback-url", "");
        return !endpoint.isBlank()
                && credentialConfigured
                && (!callbackRequired || !callbackUrl.isBlank());
    }

    private boolean isObservabilityStackReady(Environment environment) {
        return isConfigured(environment, "app.observability.log-aggregation-endpoint")
                && isConfigured(environment, "app.observability.trace-endpoint")
                && isConfigured(environment, "app.observability.alert-router-endpoint")
                && isConfigured(environment, "app.observability.dashboard-url");
    }

    private boolean isDeliveryPipelineReady(Environment environment) {
        return isConfigured(environment, "app.delivery.github-owner")
                && isConfigured(environment, "app.delivery.github-repository")
                && isConfigured(environment, "app.delivery.registry")
                && isConfigured(environment, "app.delivery.image-repository")
                && environment.getProperty("app.delivery.release-key-configured", Boolean.class, false)
                && environment.getProperty("app.delivery.canary-enabled", Boolean.class, false);
    }

    private boolean isDualDeliveryAcceptanceReady(Environment environment) {
        return isConfigured(environment, "app.delivery.standard-saas-base-url")
                && isConfigured(environment, "app.delivery.standard-saas-verified-at")
                && isConfigured(environment, "app.delivery.private-base-url")
                && isConfigured(environment, "app.delivery.private-verified-at");
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

    private Map<String, Object> buildObservabilityStack(Environment environment) {
        return Map.of(
                "logAggregation", buildEndpointSummary(environment, "app.observability.log-aggregation-endpoint"),
                "trace", buildEndpointSummary(environment, "app.observability.trace-endpoint"),
                "alertRouter", buildEndpointSummary(environment, "app.observability.alert-router-endpoint"),
                "dashboard", buildEndpointSummary(environment, "app.observability.dashboard-url")
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

    private Map<String, Object> buildEndpointSummary(Environment environment, String key) {
        String rawEndpoint = environment.getProperty(key, "");
        if (rawEndpoint == null || rawEndpoint.isBlank()) {
            return Map.of(
                    "configured", false,
                    "host", "",
                    "maskedEndpoint", ""
            );
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
            return Map.of(
                    "configured", true,
                    "host", host,
                    "maskedEndpoint", maskedEndpoint
            );
        } catch (IllegalArgumentException exception) {
            return Map.of(
                    "configured", true,
                    "host", "",
                    "maskedEndpoint", "***"
            );
        }
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
}
