package backend.live.barrage.infrastructure.adapter;

import backend.live.barrage.application.BarrageConnector;
import backend.live.barrage.application.BarrageSink;
import backend.live.barrage.infrastructure.config.BarrageConnectorProperties;

/**
 * 平台弹幕连接器抽象基类。
 *
 * <p>mock-mode=true 时使用 {@link MockBarrageEmitter} 产生模拟弹幕（无真实凭证可跑通闭环）；
 * mock-mode=false 时按平台开放能力连接真实直播间弹幕流（协议骨架，联调点见各实现类）。</p>
 */
public abstract class AbstractPlatformBarrageConnector implements BarrageConnector {

    protected final BarrageConnectorProperties properties;
    protected final MockBarrageEmitter emitter;

    /** real 模式的运行状态（mock 模式的状态由 emitter 承载） */
    private volatile boolean realRunning;

    protected AbstractPlatformBarrageConnector(BarrageConnectorProperties properties) {
        this.properties = properties;
        this.emitter = new MockBarrageEmitter();
    }

    /** 平台类型标识，子类实现（douyin / kuaishou / wechat_channels） */
    @Override
    public abstract String platformType();

    /** 真实模式接入要点（供日志与接入指引使用） */
    protected abstract String realConnectDoc();

    protected BarrageConnectorProperties.ConnectorSettings settings() {
        BarrageConnectorProperties.ConnectorSettings settings =
                properties.getConnectors().get(platformType());
        return settings != null ? settings : new BarrageConnectorProperties.ConnectorSettings();
    }

    @Override
    public boolean mock() {
        return settings().isMockMode();
    }

    @Override
    public void start(String organizationId, BarrageSink sink) {
        BarrageConnectorProperties.ConnectorSettings s = settings();
        if (s.isMockMode()) {
            emitter.start(null, organizationId, null, platformType(), sink);
            realRunning = false;
            return;
        }
        // TODO(联调-真实弹幕流): 使用 s.getAppKey()/s.getAppSecret()/s.getRoomId()/s.getWsEndpoint()
        // 建立平台开放能力连接（见 realConnectDoc()），并保持心跳与断线重连。
        // 连接建立后，把每条平台弹幕包装为 LiveBarrageMessage 交给 sink.onBarrage(...)。
        realRunning = true;
    }

    @Override
    public void stop() {
        if (mock()) {
            emitter.stop();
        } else {
            // TODO(联调): 关闭真实 WebSocket 连接并释放资源
            realRunning = false;
        }
    }

    @Override
    public boolean isRunning() {
        return mock() ? emitter.isRunning() : realRunning;
    }
}
