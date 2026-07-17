package backend.product.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProductMappingRequest(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "productId is required")
        String productId,
        @NotBlank(message = "supplierId is required")
        String supplierId,
        @NotBlank(message = "mappingType is required")
        String mappingType
) {
}

