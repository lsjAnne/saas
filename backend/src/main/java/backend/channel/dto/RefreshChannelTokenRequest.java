package backend.channel.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshChannelTokenRequest(
        @NotBlank(message = "channelTokenId is required")
        String channelTokenId
) {
}
