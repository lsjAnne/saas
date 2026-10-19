package backend.channel.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 平台售后单快照（拉取侧）。
 */
public record ChannelAfterSale(
        String afterSaleId,
        String platformOrderId,
        String status,
        String reason,
        BigDecimal refundAmount,
        OffsetDateTime createdAt,
        Map<String, Object> raw
) {
}
