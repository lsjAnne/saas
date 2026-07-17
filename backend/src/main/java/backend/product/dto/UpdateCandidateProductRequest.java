package backend.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record UpdateCandidateProductRequest(
        @NotBlank(message = "sourceUrl is required")
        String sourceUrl,
        @NotBlank(message = "title is required")
        String title,
        String category,
        @DecimalMin(value = "0.00", message = "estimatedProfit must be greater than or equal to 0")
        BigDecimal estimatedProfit,
        String riskLevel,
        String recommendationReason,
        String aiSummary
) {
}

