package backend.channel.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 平台商品快照（拉取侧）。
 */
public record ChannelProduct(
        String platformProductId,
        String title,
        String skuId,
        BigDecimal price,
        String status,
        Map<String, Object> raw
) {
}
