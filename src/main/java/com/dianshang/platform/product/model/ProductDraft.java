package com.dianshang.platform.product.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductDraft(
        String productDraftId,
        String storeId,
        String candidateProductId,
        String title,
        String sellingPoints,
        String detailContent,
        String faqContent,
        BigDecimal suggestedPrice,
        String status,
        String aiVersion,
        OffsetDateTime createdAt
) {
    public ProductDraft withUpdatedFields(String title,
                                          String sellingPoints,
                                          String detailContent,
                                          String faqContent,
                                          BigDecimal suggestedPrice,
                                          String status) {
        return new ProductDraft(
                productDraftId,
                storeId,
                candidateProductId,
                title,
                sellingPoints,
                detailContent,
                faqContent,
                suggestedPrice,
                status,
                aiVersion,
                createdAt
        );
    }

    public ProductDraft withStatus(String status) {
        return new ProductDraft(
                productDraftId,
                storeId,
                candidateProductId,
                title,
                sellingPoints,
                detailContent,
                faqContent,
                suggestedPrice,
                status,
                aiVersion,
                createdAt
        );
    }
}
