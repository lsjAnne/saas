package backend.product.model;

import java.time.OffsetDateTime;

public record ProductSourceMapping(
        String productMappingId,
        String storeId,
        String productId,
        String supplierId,
        String mappingType,
        boolean active,
        boolean riskFlag,
        OffsetDateTime createdAt
) {
    public ProductSourceMapping withActive(boolean active) {
        return new ProductSourceMapping(
                productMappingId,
                storeId,
                productId,
                supplierId,
                mappingType,
                active,
                riskFlag,
                createdAt
        );
    }

    public ProductSourceMapping withSupplier(String supplierId, boolean active) {
        return new ProductSourceMapping(
                productMappingId,
                storeId,
                productId,
                supplierId,
                mappingType,
                active,
                riskFlag,
                createdAt
        );
    }
}

