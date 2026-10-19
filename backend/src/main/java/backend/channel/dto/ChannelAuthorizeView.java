package backend.channel.dto;

public record ChannelAuthorizeView(
        String channelType,
        String authorizeUrl,
        String state
) {
}
