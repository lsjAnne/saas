package backend.channel.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorizeChannelRequest(
        @NotBlank(message = "organizationId is required")
        String organizationId
) {
}
