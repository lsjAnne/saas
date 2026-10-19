package backend.channel.dto;

public record ChannelSyncItem(
        String platformOrderId,
        String message
) {
}
