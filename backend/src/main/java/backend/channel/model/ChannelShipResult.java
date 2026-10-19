package backend.channel.model;

/**
 * 发货结果。
 */
public record ChannelShipResult(
        String platformOrderId,
        boolean success,
        String message,
        String platformShipmentId
) {
}
