package backend.channel.model;

import java.time.OffsetDateTime;

/**
 * OAuth 授权中间态：发起授权时落库，回调时凭 state 校验。
 */
public record ChannelAuthState(
        String authStateId,
        String tenantId,
        String organizationId,
        String channelType,
        String channelAccountId,
        String state,
        String redirectUri,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
) {
}
