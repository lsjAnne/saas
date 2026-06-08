package com.dianshang.platform.system.config;

import com.dianshang.platform.notification.application.NotificationGatewayProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class ExternalDependencyObservabilityConfiguration {

    @Bean
    public InfoContributor externalDependencyInfoContributor(NotificationGatewayProperties notificationGatewayProperties,
                                                             Environment environment) {
        return builder -> builder.withDetail("externalDependencies", Map.of(
                "notificationGatewayCount", notificationGatewayProperties.listProviders().size(),
                "enabledNotificationGatewayCount", notificationGatewayProperties.enabledProviderCount(),
                "mockNotificationGatewayCount", notificationGatewayProperties.mockProviderCount(),
                "supportedDeploymentModes", resolveSupportedModes(environment),
                "managementExposure", environment.getProperty("management.endpoints.web.exposure.include", "")
        ));
    }

    @Bean
    public HealthIndicator externalDependenciesHealthIndicator(NotificationGatewayProperties notificationGatewayProperties,
                                                               Environment environment) {
        return () -> Health.up()
                .withDetail("notificationGatewayCount", notificationGatewayProperties.listProviders().size())
                .withDetail("enabledNotificationGatewayCount", notificationGatewayProperties.enabledProviderCount())
                .withDetail("mockNotificationGatewayCount", notificationGatewayProperties.mockProviderCount())
                .withDetail("supportedDeploymentModes", resolveSupportedModes(environment))
                .build();
    }

    private List<String> resolveSupportedModes(Environment environment) {
        return Arrays.stream(environment.getProperty("app.deployment.supported-modes", "standard-saas,private-deployment").split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
