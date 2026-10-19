package backend.live.barrage.application;

import backend.common.exception.BusinessException;
import backend.digitalhuman.application.DigitalHumanInteractionService;
import backend.digitalhuman.application.DigitalHumanInteractionService.SubmitBarrageRequest;
import backend.live.barrage.infrastructure.config.BarrageConnectorProperties;
import backend.live.barrage.model.BarrageConnectionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 弹幕连接管理器：按组织启停平台弹幕连接器，并把拉到的弹幕统一流转到数字人问答服务。
 *
 * <p>流转闭环：平台连接器 → 本管理器回调 → {@link DigitalHumanInteractionService#submitBarrage}
 * （同一进程内调用，避免 HTTP 回环）→ 数字人按商品/问题类型作答 → 互动记录落库 → 前端聊天区上屏。</p>
 *
 * <p>说明：当前连接器为单实例 Bean，按组织启停（后启动覆盖前启动）；多租户并行接入时，
 * 需将连接器改为按组织工厂化实例（见接入指引 docs/barrage-connect.md）。</p>
 */
@Service
public class BarrageConnectorManager {

    private final List<BarrageConnector> connectors;
    private final DigitalHumanInteractionService interactionService;
    private final BarrageConnectorProperties properties;

    /** 各平台当前运行组织与启动时间（用于状态展示） */
    private final Map<String, RuntimeState> runtimeStates = new ConcurrentHashMap<>();

    public BarrageConnectorManager(List<BarrageConnector> connectors,
                                   DigitalHumanInteractionService interactionService,
                                   BarrageConnectorProperties properties) {
        this.connectors = connectors;
        this.interactionService = interactionService;
        this.properties = properties;
    }

    /** 启动指定平台的弹幕拉流（幂等：已运行则直接返回） */
    public synchronized void start(String tenantId, String organizationId, String platformType) {
        BarrageConnector connector = requireConnector(platformType);
        RuntimeState existing = runtimeStates.get(platformType);
        if (connector.isRunning() && existing != null && platformType.equals(existing.platformType())) {
            return;
        }
        connector.start(organizationId, message -> {
            try {
                interactionService.submitBarrage(tenantId, new SubmitBarrageRequest(
                        organizationId,
                        message.sessionId(),
                        message.platformType(),
                        message.userNick(),
                        message.content()
                ));
            } catch (Exception ignored) {
                // 单条弹幕处理失败不中断拉流（由互动概览体现未命中与学习沉淀）
            }
        });
        runtimeStates.put(platformType, new RuntimeState(platformType, organizationId, OffsetDateTime.now()));
    }

    /** 停止指定平台的弹幕拉流 */
    public synchronized void stop(String organizationId, String platformType) {
        BarrageConnector connector = requireConnector(platformType);
        connector.stop();
        runtimeStates.remove(platformType);
    }

    /** 全部平台连接器状态（前端"弹幕源接入"面板数据） */
    public List<BarrageConnectionStatus> listStatus() {
        return connectors.stream()
                .filter(c -> !"mock".equals(c.platformType()))
                .map(c -> toStatus(c))
                .toList();
    }

    /** 当前运行中的弹幕源（含 mock），供前端标记主弹幕源 */
    public String activeSource() {
        for (BarrageConnector c : connectors) {
            if (c.isRunning()) {
                return c.platformType();
            }
        }
        return "none";
    }

    public boolean isPlatformEnabled(String platformType) {
        BarrageConnectorProperties.ConnectorSettings settings = properties.getConnectors().get(platformType);
        return properties.isEnabled() && settings != null && settings.isEnabled();
    }

    private BarrageConnectionStatus toStatus(BarrageConnector connector) {
        String platformType = connector.platformType();
        BarrageConnectorProperties.ConnectorSettings settings = properties.getConnectors().get(platformType);
        boolean enabled = settings != null && settings.isEnabled();
        RuntimeState state = runtimeStates.get(platformType);
        boolean running = enabled && connector.isRunning();
        String mode = connector.mock() ? "mock" : "real";
        return new BarrageConnectionStatus(
                platformType,
                running,
                mode,
                state != null ? state.startedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null,
                null,
                0L,
                enabled ? null : "未启用（配置 app.live.barrage.connectors." + platformType + ".enabled=false）"
        );
    }

    private BarrageConnector requireConnector(String platformType) {
        return connectors.stream()
                .filter(c -> c.platformType().equals(platformType))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "2201", "unsupported barrage platform: " + platformType, HttpStatus.BAD_REQUEST));
    }

    private record RuntimeState(String platformType, String organizationId, OffsetDateTime startedAt) {
    }
}
