package backend.contentasset.dto;

import java.time.OffsetDateTime;

public record ContentAssetReferenceCopyView(
        String assetId,
        String assetName,
        String referenceText,
        int referenceCount,
        OffsetDateTime generatedAt
) {
}

