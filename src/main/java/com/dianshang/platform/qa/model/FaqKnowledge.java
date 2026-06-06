package com.dianshang.platform.qa.model;

import java.time.OffsetDateTime;

public record FaqKnowledge(
        String faqId,
        String storeId,
        String productId,
        String question,
        String answer,
        String sourceType,
        boolean enabled,
        OffsetDateTime createdAt
) {
}
