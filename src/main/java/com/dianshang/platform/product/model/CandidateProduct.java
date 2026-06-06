package com.dianshang.platform.product.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CandidateProduct(
        String candidateProductId,
        String storeId,
        String sourceType,
        String sourceUrl,
        String sourceUrlHash,
        String title,
        String category,
        String status,
        BigDecimal estimatedProfit,
        String riskLevel,
        String recommendationReason,
        String aiSummary,
        OffsetDateTime createdAt
) {
    public CandidateProduct withUpdatedFields(String sourceUrl,
                                              String sourceUrlHash,
                                              String title,
                                              String category,
                                              BigDecimal estimatedProfit,
                                              String riskLevel,
                                              String recommendationReason,
                                              String aiSummary) {
        return new CandidateProduct(
                candidateProductId,
                storeId,
                sourceType,
                sourceUrl,
                sourceUrlHash,
                title,
                category,
                status,
                estimatedProfit,
                riskLevel,
                recommendationReason,
                aiSummary,
                createdAt
        );
    }

    public CandidateProduct withStatus(String status) {
        return new CandidateProduct(
                candidateProductId,
                storeId,
                sourceType,
                sourceUrl,
                sourceUrlHash,
                title,
                category,
                status,
                estimatedProfit,
                riskLevel,
                recommendationReason,
                aiSummary,
                createdAt
        );
    }
}
