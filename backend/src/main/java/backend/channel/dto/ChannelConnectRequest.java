package backend.channel.dto;

import jakarta.validation.constraints.NotBlank;

public record ChannelConnectRequest(
        @NotBlank(message = "channelAccountId is required")
        String channelAccountId
) {
}
