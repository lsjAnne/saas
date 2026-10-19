package backend.live.barrage.application;

/**
 * 直播平台弹幕连接器端口。
 *
 * <p>实现约定：
 * <ul>
 *   <li>连接器为无状态端口实现，运行上下文（目标组织）由 {@link #start} 传入，内部记录当前上下文；</li>
 *   <li>{@code mock()} 为 true 时不得发起任何真实网络请求，只产生模拟弹幕（驱动数字人问答闭环演示）；</li>
 *   <li>真实模式按各平台开放能力实现握手、心跳、拉流与重连（见各实现类 TODO 联调标注）。</li>
 * </ul>
 */
public interface BarrageConnector {

    /** 平台类型标识：douyin / kuaishou / wechat_channels / mock */
    String platformType();

    /** 是否运行在 mock 模式（无真实凭证也可跑通闭环） */
    boolean mock();

    /** 启动拉流：绑定目标组织与弹幕回调，幂等 */
    void start(String organizationId, BarrageSink sink);

    /** 停止拉流：释放连接与调度资源，幂等 */
    void stop();

    /** 当前是否运行中 */
    boolean isRunning();
}
