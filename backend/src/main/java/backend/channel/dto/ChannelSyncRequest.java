package backend.channel.dto;

import jakarta.validation.constraints.NotBlank;

public record ChannelSyncRequest(
        @NotBlank(message = "channelAccountId is required")
        String channelAccountId,
        @NotBlank(message = "storeId is required")
        String storeId
) {
}
