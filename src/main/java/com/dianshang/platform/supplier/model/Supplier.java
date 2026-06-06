package com.dianshang.platform.supplier.model;

import java.time.OffsetDateTime;

public record Supplier(
        String supplierId,
        String storeId,
        String supplierPlatformType,
        String supplierPlatformId,
        String supplierName,
        String sourceUrl,
        Integer priceScore,
        Integer deliveryScore,
        Integer stabilityScore,
        String riskLevel,
        boolean dropshipSupportFlag,
        boolean primary,
        boolean backup,
        boolean blacklistFlag,
        OffsetDateTime createdAt
) {
    public Supplier withUpdatedFields(String supplierName,
                                      String sourceUrl,
                                      Integer priceScore,
                                      Integer deliveryScore,
                                      Integer stabilityScore,
                                      String riskLevel,
                                      boolean dropshipSupportFlag,
                                      boolean blacklistFlag) {
        return new Supplier(
                supplierId,
                storeId,
                supplierPlatformType,
                supplierPlatformId,
                supplierName,
                sourceUrl,
                priceScore,
                deliveryScore,
                stabilityScore,
                riskLevel,
                dropshipSupportFlag,
                primary,
                backup,
                blacklistFlag,
                createdAt
        );
    }

    public Supplier withPriority(boolean primary, boolean backup) {
        return new Supplier(
                supplierId,
                storeId,
                supplierPlatformType,
                supplierPlatformId,
                supplierName,
                sourceUrl,
                priceScore,
                deliveryScore,
                stabilityScore,
                riskLevel,
                dropshipSupportFlag,
                primary,
                backup,
                blacklistFlag,
                createdAt
        );
    }
}
