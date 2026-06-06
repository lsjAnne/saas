package com.dianshang.platform.product.dto;

import jakarta.validation.constraints.NotBlank;

public record SwitchProductMappingSupplierRequest(
        @NotBlank(message = "supplierId is required")
        String supplierId
) {
}
