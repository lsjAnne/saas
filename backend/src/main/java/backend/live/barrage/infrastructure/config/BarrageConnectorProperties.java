package backend.live.barrage.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 弹幕接入配置：app.live.barrage.*
 *
 * <pre>
 * app:
 *   live:
 *     barrage:
 *       enabled: true
 *       connectors:
 *         douyin:
 *           enabled: true
 *           mock-mode: ${APP_LIVE_BARRAGE_DOUYIN_MOCK_MODE:true}
 *           app-key: ${APP_LIVE_BARRAGE_DOUYIN_APP_KEY:}
 *           app-secret: ${APP_LIVE_BARRAGE_DOUYIN_APP_SECRET:}
 *           room-id: ${APP_LIVE_BARRAGE_DOUYIN_ROOM_ID:}
 *           ws-endpoint: ${APP_LIVE_BARRAGE_DOUYIN_WS_ENDPOINT:}
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "app.live.barrage")
public class BarrageConnectorProperties {

    /** 弹幕接入总开关 */
    private boolean enabled = true;

    /** 各平台连接器配置（key = 平台类型） */
    private Map<String, ConnectorSettings> connectors = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, ConnectorSettings> getConnectors() {
        return connectors;
    }

    public void setConnectors(Map<String, ConnectorSettings> connectors) {
        this.connectors = connectors;
    }

    /** 单个平台连接器设置 */
    public static class ConnectorSettings {

        private boolean enabled = true;
        private boolean mockMode = true;
        private String appKey = "";
        private String appSecret = "";
        private String roomId = "";
        private String wsEndpoint = "";
        private String callbackPath = "/live_room/callback";
        private String callbackSecret = "";
        private String gatewayEndpoint = "";
        private String organizationId = "";
        private String tenantId = "";
        private String description = "";

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

        public String getRoomId() {
            return roomId;
        }

        public void setRoomId(String roomId) {
            this.roomId = roomId;
        }

        public String getWsEndpoint() {
            return wsEndpoint;
        }

        public void setWsEndpoint(String wsEndpoint) {
            this.wsEndpoint = wsEndpoint;
        }

        public String getCallbackPath() {
            return callbackPath;
        }

        public void setCallbackPath(String callbackPath) {
            this.callbackPath = callbackPath;
        }

        public String getCallbackSecret() {
            return callbackSecret;
        }

        public void setCallbackSecret(String callbackSecret) {
            this.callbackSecret = callbackSecret;
        }

        public String getGatewayEndpoint() {
            return gatewayEndpoint;
        }

        public void setGatewayEndpoint(String gatewayEndpoint) {
            this.gatewayEndpoint = gatewayEndpoint;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
