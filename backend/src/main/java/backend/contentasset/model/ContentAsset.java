package backend.contentasset.model;

import java.time.OffsetDateTime;

public record ContentAsset(
        String assetId,
        String storeId,
        String assetCategory,
        String assetType,
        String assetName,
        String assetStatus,
        String previewMode,
        String previewUrl,
        String previewText,
        boolean generated,
        String sourceChannel,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

