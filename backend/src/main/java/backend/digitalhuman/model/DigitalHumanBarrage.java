package backend.digitalhuman.model;

import java.time.OffsetDateTime;

/**
 * 直播间的观众弹幕消息。真实模式下由平台开放平台评论/弹幕流接入，
 * mock 模式下由前端"模拟观众"或 simulate 接口产生。
 */
public record DigitalHumanBarrage(
        String barrageId,
        String tenantId,
        String organizationId,
        String sessionId,
        String platformType,
        String userNick,
        String content,
        String replyStatus,
        OffsetDateTime createdAt
) {
    public DigitalHumanBarrage withReplyStatus(String status) {
        return new DigitalHumanBarrage(
                barrageId, tenantId, organizationId, sessionId, platformType,
                userNick, content, status, createdAt
        );
    }
}
