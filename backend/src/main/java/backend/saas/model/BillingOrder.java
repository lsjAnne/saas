package backend.saas.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BillingOrder(
        String billingOrderId,
        String tenantId,
        String planCode,
        String planName,
        String orderType,
        BigDecimal payableAmount,
        String paymentStatus,
        String externalOrderNo,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt
) {
}

