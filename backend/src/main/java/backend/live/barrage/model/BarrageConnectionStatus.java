package backend.live.barrage.model;

/**
 * 弹幕连接器运行状态：供前端"弹幕源接入"面板展示。
 *
 * @param mode 运行模式：mock（模拟弹幕）/ real（真实平台协议，待联调）
 */
public record BarrageConnectionStatus(
        String platformType,
        boolean running,
        String mode,
        String connectedAt,
        String lastMessageAt,
        long messageCount,
        String errorMessage
) {
}
