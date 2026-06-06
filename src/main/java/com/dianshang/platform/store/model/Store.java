package com.dianshang.platform.store.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

public record Store(
        String storeId,
        String tenantId,
        String organizationId,
        String ownerUserId,
        String platformType,
        String platformShopId,
        String shopName,
        String authStatus,
        BigDecimal profitThreshold,
        BigDecimal riskThreshold,
        Map<String, Object> defaultShipConfig,
        OffsetDateTime createdAt
) {
    public Store withSettings(BigDecimal profitThreshold,
                              BigDecimal riskThreshold,
                              Map<String, Object> defaultShipConfig) {
        return new Store(
                storeId,
                tenantId,
                organizationId,
                ownerUserId,
                platformType,
                platformShopId,
                shopName,
                authStatus,
                profitThreshold,
                riskThreshold,
                defaultShipConfig,
                createdAt
        );
    }
}
