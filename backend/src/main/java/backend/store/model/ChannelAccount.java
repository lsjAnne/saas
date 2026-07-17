package backend.store.model;

import java.time.OffsetDateTime;
import java.util.Map;

public record ChannelAccount(
        String channelAccountId,
        String organizationId,
        String channelType,
        String accountName,
        String authStatus,
        OffsetDateTime expiresAt,
        Map<String, Object> extraConfig,
        OffsetDateTime createdAt
) {
    public ChannelAccount withAuthStatus(String authStatus, OffsetDateTime expiresAt) {
        return new ChannelAccount(
                channelAccountId,
                organizationId,
                channelType,
                accountName,
                authStatus,
                expiresAt,
                extraConfig,
                createdAt
        );
    }
}

