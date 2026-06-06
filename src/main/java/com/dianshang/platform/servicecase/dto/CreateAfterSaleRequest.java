package com.dianshang.platform.servicecase.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAfterSaleRequest(
        @NotBlank(message = "orderId is required")
        String orderId,
        @NotBlank(message = "afterSaleType is required")
        String afterSaleType,
        String reasonText,
        String evidenceBlob
) {
}
