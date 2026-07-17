package backend.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record UpdateProductDraftRequest(
        @NotBlank(message = "title is required")
        String title,
        String sellingPoints,
        String detailContent,
        String faqContent,
        @DecimalMin(value = "0.00", message = "suggestedPrice must be greater than or equal to 0")
        BigDecimal suggestedPrice,
        @NotBlank(message = "status is required")
        String status
) {
}

