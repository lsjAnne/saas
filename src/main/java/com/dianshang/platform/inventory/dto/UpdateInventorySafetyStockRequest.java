package com.dianshang.platform.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateInventorySafetyStockRequest(
        @NotNull(message = "safetyStock is required")
        @Min(value = 0, message = "safetyStock must be greater than or equal to 0")
        Integer safetyStock
) {
}
