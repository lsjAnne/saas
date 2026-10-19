package backend.live.barrage.infrastructure;

import backend.live.barrage.infrastructure.config.BarrageConnectorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 抖音云 WebSocket 网关推送客户端。
 *
 * <p>抖音云方案中，开发者后端与主播玩法客户端之间的长连接由抖音云 WebSocket 网关托管：
 * 后端只需通过 HTTP 调用网关地址即可把指令下发到客户端（消息秒级必达，耗时小于 100ms）。
 * 网关地址在抖音云完成服务部署后获得（部署方案第 5.3 节），本组件按配置惰性生效：
 * 未配置网关地址时仅记录日志并跳过（不影响回调 ack，避免开发阶段空转）。</p>
 *
 * <p>TODO(联调-抖音云网关): 网关鉴权头、回调路径 /websocket_callback 的上行消息处理、
 * 失败数据轮询补推（平台回查接口），在部署方案联调阶段按网关实际文档补齐。</p>
 */
@Component
public class DouyinCloudPushGateway {

    private static final Logger log = LoggerFactory.getLogger(DouyinCloudPushGateway.class);

    private final BarrageConnectorProperties properties;
    private final HttpClient httpClient;

    public DouyinCloudPushGateway(BarrageConnectorProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    /**
     * 推送一条玩法指令到抖音云 WebSocket 网关，由网关下发到主播玩法客户端。
     *
     * @param roomId  直播间 id（网关按 room 路由到对应客户端连接）
     * @param payload 指令内容（JSON，随客户端协议定义）
     * @return true=已推送（或网关未配置时按跳过处理），false=推送失败
     */
    public boolean push(String roomId, String payload) {
        BarrageConnectorProperties.ConnectorSettings douyin = settings();
        String gatewayEndpoint = douyin.getGatewayEndpoint();
        if (gatewayEndpoint == null || gatewayEndpoint.isBlank()) {
            log.info("[douyin-gateway] 网关地址未配置（APP_LIVE_BARRAGE_DOUYIN_GATEWAY_ENDPOINT），跳过下发 roomId={} payload={}", roomId, payload);
            return true;
        }
        try {
            String url = gatewayEndpoint.endsWith("/")
                    ? gatewayEndpoint + "push"
                    : gatewayEndpoint + "/push";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .header("x-roomid", roomId == null ? "" : roomId)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }
            log.warn("[douyin-gateway] 推送失败 status={} roomId={}", response.statusCode(), roomId);
            return false;
        } catch (Exception e) {
            log.warn("[douyin-gateway] 推送异常 roomId={} err={}", roomId, e.getMessage());
            return false;
        }
    }

    private BarrageConnectorProperties.ConnectorSettings settings() {
        BarrageConnectorProperties.ConnectorSettings settings = properties.getConnectors().get("douyin");
        return settings != null ? settings : new BarrageConnectorProperties.ConnectorSettings();
    }
}
