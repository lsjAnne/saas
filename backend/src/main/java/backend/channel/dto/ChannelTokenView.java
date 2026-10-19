package backend.channel.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 渠道令牌视图。mock 阶段 accessToken/refreshToken 保留原值；生产环境应对令牌字段做脱敏后再返回。
 */
public record ChannelTokenView(
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
