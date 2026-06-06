package com.dianshang.platform.product.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCandidateProductStatusRequest(
        @NotBlank(message = "status is required")
        String status
) {
}
