package backend.channel.application;

import backend.channel.model.ChannelType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * app.channel.platforms.<code> 配置绑定。缺省回退到 mock 通道，保证未配置凭证时仍可离线联调。
 */
@Component
@ConfigurationProperties(prefix = "app.channel")
public class ChannelPlatformProperties {

    private Map<String, ChannelPlatformSettings> platforms = new LinkedHashMap<>();

    public Map<String, ChannelPlatformSettings> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(Map<String, ChannelPlatformSettings> platforms) {
        this.platforms = platforms == null ? new LinkedHashMap<>() : new LinkedHashMap<>(platforms);
    }

    public ChannelPlatformSettings settings(ChannelType channelType) {
        ChannelPlatformSettings configured = platforms.get(channelType.code());
        ChannelPlatformSettings settings = configured == null ? new ChannelPlatformSettings() : configured;
        ChannelPlatformSettings normalized = new ChannelPlatformSettings();
        normalized.setEnabled(settings.isEnabled());
        normalized.setMockMode(settings.isMockMode());
        normalized.setEndpoint(defaultIfBlank(settings.getEndpoint(), "internal://mock/" + channelType.code()));
        normalized.setAppKey(settings.getAppKey());
        normalized.setAppSecret(settings.getAppSecret());
        normalized.setCallbackPath(defaultIfBlank(
                settings.getCallbackPath(),
                "/api/channels/callback/" + channelType.code()
        ));
        normalized.setDescription(defaultIfBlank(settings.getDescription(), channelType.code() + " channel"));
        return normalized;
    }

    public boolean isMockMode(ChannelType channelType) {
        return settings(channelType).isMockMode();
    }

    public boolean isEnabled(ChannelType channelType) {
        return settings(channelType).isEnabled();
    }

    public String endpoint(ChannelType channelType) {
        return settings(channelType).getEndpoint();
    }

    public String appKey(ChannelType channelType) {
        return settings(channelType).getAppKey();
    }

    public String appSecret(ChannelType channelType) {
        return settings(channelType).getAppSecret();
    }

    public String callbackPath(ChannelType channelType) {
        return settings(channelType).getCallbackPath();
    }

    public List<ChannelPlatformSettings> listSettings() {
        List<ChannelPlatformSettings> resolved = new ArrayList<>();
        for (ChannelType channelType : ChannelType.values()) {
            resolved.add(settings(channelType));
        }
        return resolved;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static class ChannelPlatformSettings {

        private boolean enabled = true;
        private boolean mockMode = true;
        private String endpoint;
        private String appKey;
        private String appSecret;
        private String callbackPath;
        private String description;

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

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getCallbackPath() {
            return callbackPath;
        }

        public void setCallbackPath(String callbackPath) {
            this.callbackPath = callbackPath;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
