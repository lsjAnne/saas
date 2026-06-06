package com.dianshang.platform.order.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderItem(
        String orderItemId,
        String orderId,
        String productId,
        String skuId,
        Integer quantity,
        BigDecimal unitPrice,
        OffsetDateTime createdAt
) {
}
