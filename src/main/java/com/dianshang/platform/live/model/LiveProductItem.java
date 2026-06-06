package com.dianshang.platform.live.model;

import java.time.OffsetDateTime;

public record LiveProductItem(
        String liveProductItemId,
        String livePlanId,
        String candidateProductId,
        String productDraftId,
        String productId,
        int sortOrder,
        OffsetDateTime createdAt
) {
}
