package com.dianshang.platform.campaign.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CouponTemplate(
        String couponTemplateId,
        String storeId,
        String templateName,
        String discountType,
        BigDecimal discountValue,
        BigDecimal thresholdAmount,
        String status,
        OffsetDateTime createdAt
) {
}
