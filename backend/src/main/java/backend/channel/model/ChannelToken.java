package backend.channel.model;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 渠道授权令牌。channelType 存平台 code（如 pdd/taobao）。
 */
public record ChannelToken(
        String channelTokenId,
        String tenantId,
        String organizationId,
        String channelType,
        String accountName,
        String accessToken,
        String refreshToken,
        String tokenType,
        String scope,
        OffsetDateTime expiresAt,
        OffsetDateTime refreshExpiresAt,
        Map<String, Object> extraConfig,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
