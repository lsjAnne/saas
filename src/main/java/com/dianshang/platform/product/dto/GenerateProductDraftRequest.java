package com.dianshang.platform.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record GenerateProductDraftRequest(
        @NotBlank(message = "candidateProductId is required")
        String candidateProductId,
        String aiVersion,
        @DecimalMin(value = "0.00", message = "suggestedPrice must be greater than or equal to 0")
        BigDecimal suggestedPrice
) {
}
