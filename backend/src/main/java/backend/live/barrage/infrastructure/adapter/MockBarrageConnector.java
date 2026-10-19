package backend.live.barrage.infrastructure.adapter;

import backend.live.barrage.application.BarrageConnector;
import backend.live.barrage.application.BarrageSink;
import org.springframework.stereotype.Component;

/**
 * 默认弹幕连接器（mock）：以共享模拟弹幕源持续产生观众弹幕，驱动数字人问答闭环。
 * 用于无真实平台凭证时的联调与演示；真实平台连接器就绪后，本连接器可停用。
 */
@Component
public class MockBarrageConnector implements BarrageConnector {

    private final MockBarrageEmitter emitter;

    public MockBarrageConnector() {
        this.emitter = new MockBarrageEmitter();
    }

    @Override
    public String platformType() {
        return "mock";
    }

    @Override
    public boolean mock() {
        return true;
    }

    @Override
    public void start(String organizationId, BarrageSink sink) {
        emitter.start(null, organizationId, null, platformType(), sink);
    }

    @Override
    public void stop() {
        emitter.stop();
    }

    @Override
    public boolean isRunning() {
        return emitter.isRunning();
    }
}
