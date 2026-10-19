package backend.live.barrage.infrastructure.adapter;

import backend.live.barrage.application.BarrageSink;
import backend.live.barrage.model.LiveBarrageMessage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模拟弹幕源：mock 模式的连接器用它按随机间隔产生贴近真实直播间的观众弹幕，
 * 驱动「弹幕到达 → 数字人问答 → 回答上屏 → 未命中进学习记忆」的完整闭环，无真实凭证也可演示。
 *
 * <p>非 Spring Bean：每个连接器各自持有独立实例，保证平台间启停互不影响。</p>
 */
public class MockBarrageEmitter {

    /** 模拟弹幕池：覆盖多商品、多问题类型与闲聊兜底场景 */
    static final List<String> SIMULATED_BARRAGES = List.of(
            "这个蓝牙耳机多少钱呀",
            "无线耳机续航怎么样",
            "蓝牙耳机延迟高不高",
            "耳机戴着舒服吗",
            "羽绒服有尺码推荐吗",
            "羽绒服保暖吗 会跑毛吗",
            "羽绒服多久发货",
            "羽绒服可以退货吗",
            "有优惠券吗",
            "包邮吗",
            "怎么下单啊",
            "主播在吗 有链接吗",
            "这个质量怎么样",
            "能开发票吗"
    );

    private final AtomicLong sequence = new AtomicLong(0);

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> task;
    private volatile String tenantId;
    private volatile String organizationId;
    private volatile String sessionId;
    private volatile String platformType;
    private volatile BarrageSink sink;
    private final AtomicLong emittedCount = new AtomicLong(0);

    /** 启动模拟弹幕流：每 2.5~5 秒随机弹出一条 */
    public synchronized void start(String tenantId, String organizationId, String sessionId,
                                   String platformType, BarrageSink sink) {
        stop();
        this.tenantId = tenantId;
        this.organizationId = organizationId;
        this.sessionId = sessionId;
        this.platformType = platformType;
        this.sink = sink;
        this.emittedCount.set(0);
        executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "mock-barrage-" + platformType);
            t.setDaemon(true);
            return t;
        });
        task = executor.scheduleWithFixedDelay(this::emitOne, 1, 1, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }

    public long emittedCount() {
        return emittedCount.get();
    }

    public String platformType() {
        return platformType;
    }

    private void emitOne() {
        BarrageSink currentSink = this.sink;
        if (currentSink == null) {
            return;
        }
        List<String> pool = SIMULATED_BARRAGES;
        String content = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        emittedCount.incrementAndGet();
        currentSink.onBarrage(new LiveBarrageMessage(
                tenantId,
                organizationId,
                sessionId,
                platformType,
                "平台观众" + ThreadLocalRandom.current().nextInt(1000, 9999),
                content,
                "{\"mock\":true}",
                OffsetDateTime.now()
        ));
    }
}
