package backend.product.model;

import java.time.OffsetDateTime;

public record Product(
        String productId,
        String storeId,
        String platformProductId,
        String productDraftId,
        String title,
        String status,
        Integer healthScore,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt
) {
}

