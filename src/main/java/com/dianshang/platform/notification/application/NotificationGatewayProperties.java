package com.dianshang.platform.notification.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Component
@ConfigurationProperties(prefix = "app.integrations.notification")
public class NotificationGatewayProperties {

    private boolean requireConfiguredGateway;
    private Map<String, String> channelBindings = new LinkedHashMap<>();
    private Map<String, NotificationGatewayProvider> providers = new LinkedHashMap<>();

    public boolean isRequireConfiguredGateway() {
        return requireConfiguredGateway;
    }

    public void setRequireConfiguredGateway(boolean requireConfiguredGateway) {
        this.requireConfiguredGateway = requireConfiguredGateway;
    }

    public Map<String, String> getChannelBindings() {
        return channelBindings;
    }

    public void setChannelBindings(Map<String, String> channelBindings) {
        this.channelBindings = channelBindings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(channelBindings);
    }

    public Map<String, NotificationGatewayProvider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, NotificationGatewayProvider> providers) {
        this.providers = providers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(providers);
    }

    public ResolvedGateway resolveProvider(String notifyType) {
        String configuredCode = channelBindings.get(notifyType);
        NotificationGatewayProvider provider = configuredCode == null ? null : providers.get(configuredCode);
        if (provider == null) {
            provider = providers.entrySet().stream()
                    .filter(entry -> notifyType.equals(entry.getValue().getNotifyType()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseGet(() -> defaultProvider(notifyType));
            configuredCode = provider.getGatewayCode();
        }
        NotificationGatewayProvider safeProvider = sanitize(configuredCode, notifyType, provider);
        return new ResolvedGateway(
                safeProvider.getGatewayCode(),
                safeProvider.getNotifyType(),
                safeProvider.isEnabled(),
                safeProvider.isMockMode(),
                safeProvider.isReceiptSupported(),
                safeProvider.getEndpoint(),
                safeProvider.getReceiptCallbackPath(),
                safeProvider.getDescription()
        );
    }

    public List<ResolvedGateway> listProviders() {
        Set<String> codes = new TreeSet<>(providers.keySet());
        List<ResolvedGateway> resolved = new ArrayList<>();
        for (String code : codes) {
            NotificationGatewayProvider provider = providers.get(code);
            if (provider == null) {
                continue;
            }
            NotificationGatewayProvider safeProvider = sanitize(
                    code,
                    defaultIfBlank(provider.getNotifyType(), "site_message"),
                    provider
            );
            resolved.add(new ResolvedGateway(
                    safeProvider.getGatewayCode(),
                    safeProvider.getNotifyType(),
                    safeProvider.isEnabled(),
                    safeProvider.isMockMode(),
                    safeProvider.isReceiptSupported(),
                    safeProvider.getEndpoint(),
                    safeProvider.getReceiptCallbackPath(),
                    safeProvider.getDescription()
            ));
        }
        return resolved;
    }

    public boolean supportsReceipt(String gatewayCode) {
        return listProviders().stream()
                .filter(provider -> gatewayCode.equals(provider.gatewayCode()))
                .findFirst()
                .map(ResolvedGateway::receiptSupported)
                .orElse(false);
    }

    public int enabledProviderCount() {
        return (int) listProviders().stream().filter(ResolvedGateway::enabled).count();
    }

    public int mockProviderCount() {
        return (int) listProviders().stream().filter(ResolvedGateway::mockMode).count();
    }

    private NotificationGatewayProvider sanitize(String gatewayCode,
                                                 String notifyType,
                                                 NotificationGatewayProvider provider) {
        NotificationGatewayProvider sanitized = new NotificationGatewayProvider();
        sanitized.setGatewayCode(defaultIfBlank(provider.getGatewayCode(), gatewayCode));
        sanitized.setNotifyType(defaultIfBlank(provider.getNotifyType(), notifyType));
        sanitized.setEnabled(provider.isEnabled());
        sanitized.setMockMode(provider.isMockMode());
        sanitized.setReceiptSupported(provider.isReceiptSupported());
        sanitized.setEndpoint(defaultIfBlank(provider.getEndpoint(), "internal://notification-center"));
        sanitized.setReceiptCallbackPath(defaultIfBlank(provider.getReceiptCallbackPath(), "/api/notifications/{id}/delivery-receipts"));
        sanitized.setDescription(defaultIfBlank(provider.getDescription(), sanitized.getNotifyType() + " gateway"));
        return sanitized;
    }

    private NotificationGatewayProvider defaultProvider(String notifyType) {
        NotificationGatewayProvider provider = new NotificationGatewayProvider();
        provider.setGatewayCode(notifyType + "_mock");
        provider.setNotifyType(notifyType);
        provider.setEnabled(!requireConfiguredGateway);
        provider.setMockMode(true);
        provider.setReceiptSupported(!Objects.equals("site_message", notifyType));
        provider.setEndpoint("internal://mock/" + notifyType);
        provider.setReceiptCallbackPath("/api/notifications/{id}/delivery-receipts");
        provider.setDescription("auto-generated mock gateway");
        return provider;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static class NotificationGatewayProvider {

        private String gatewayCode;
        private String notifyType;
        private boolean enabled = true;
        private boolean mockMode = true;
        private boolean receiptSupported = true;
        private String endpoint;
        private String receiptCallbackPath;
        private String description;

        public String getGatewayCode() {
            return gatewayCode;
        }

        public void setGatewayCode(String gatewayCode) {
            this.gatewayCode = gatewayCode;
        }

        public String getNotifyType() {
            return notifyType;
        }

        public void setNotifyType(String notifyType) {
            this.notifyType = notifyType;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isMockMode() {
            return mockMode;
        }

        public void setMockMode(boolean mockMode) {
            this.mockMode = mockMode;
        }

        public boolean isReceiptSupported() {
            return receiptSupported;
        }

        public void setReceiptSupported(boolean receiptSupported) {
            this.receiptSupported = receiptSupported;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getReceiptCallbackPath() {
            return receiptCallbackPath;
        }

        public void setReceiptCallbackPath(String receiptCallbackPath) {
            this.receiptCallbackPath = receiptCallbackPath;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public record ResolvedGateway(
            String gatewayCode,
            String notifyType,
            boolean enabled,
            boolean mockMode,
            boolean receiptSupported,
            String endpoint,
            String receiptCallbackPath,
            String description
    ) {
    }
}
