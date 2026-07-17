package backend.order.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderMain(
        String orderId,
        String storeId,
        String platformOrderId,
        String orderStatus,
        String logisticsStatus,
        BigDecimal totalAmount,
        BigDecimal estimatedProfit,
        String buyerName,
        String buyerPhoneMask,
        String shippingAddress,
        OffsetDateTime timeoutAt,
        OffsetDateTime createdAt
) {
    public OrderMain withStatus(String orderStatus, String logisticsStatus) {
        return new OrderMain(
                orderId,
                storeId,
                platformOrderId,
                orderStatus,
                logisticsStatus,
                totalAmount,
                estimatedProfit,
                buyerName,
                buyerPhoneMask,
                shippingAddress,
                timeoutAt,
                createdAt
        );
    }
}

