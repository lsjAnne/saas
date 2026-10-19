package backend.channel.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 平台订单快照（拉取侧）。
 */
public record ChannelOrder(
        String platformOrderId,
        String productId,
        String skuId,
        Integer quantity,
        BigDecimal unitPrice,
        String buyerName,
        String buyerPhoneMask,
        String shippingAddress,
        Map<String, Object> raw
) {
}
