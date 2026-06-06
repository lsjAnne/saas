package com.dianshang.platform.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReplenishmentTaskRequest(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "productId is required")
        String productId,
        @NotBlank(message = "skuId is required")
        String skuId,
        @NotNull(message = "suggestedQty is required")
        @Min(value = 1, message = "suggestedQty must be greater than 0")
        Integer suggestedQty,
        String reasonText
) {
}
