package backend.live.barrage.model;

import java.time.OffsetDateTime;

/**
 * 直播平台弹幕消息：平台连接器拉取到的一条观众弹幕。
 * 真实模式下 rawPayload 保留平台原始报文，便于排障与审计。
 */
public record LiveBarrageMessage(
        String tenantId,
        String organizationId,
        String sessionId,
        String platformType,
        String userNick,
        String content,
        String rawPayload,
        OffsetDateTime receivedAt
) {
}
