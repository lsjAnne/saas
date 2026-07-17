package backend.contentasset.model;

import java.time.OffsetDateTime;

public record ContentAssetReference(
        String referenceId,
        String assetId,
        String referenceType,
        String referenceName,
        String referenceTargetId,
        String quoteText,
        OffsetDateTime createdAt
) {
}

