package com.dianshang.platform.supplier.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateSupplierRequest(
        @NotBlank(message = "supplierName is required")
        String supplierName,
        String sourceUrl,
        @Min(value = 0, message = "priceScore must be greater than or equal to 0")
        @Max(value = 100, message = "priceScore must be less than or equal to 100")
        Integer priceScore,
        @Min(value = 0, message = "deliveryScore must be greater than or equal to 0")
        @Max(value = 100, message = "deliveryScore must be less than or equal to 100")
        Integer deliveryScore,
        @Min(value = 0, message = "stabilityScore must be greater than or equal to 0")
        @Max(value = 100, message = "stabilityScore must be less than or equal to 100")
        Integer stabilityScore,
        String riskLevel,
        Boolean dropshipSupportFlag,
        Boolean blacklistFlag
) {
}
