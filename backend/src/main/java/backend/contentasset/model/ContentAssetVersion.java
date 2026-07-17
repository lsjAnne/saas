package backend.contentasset.model;

import java.time.OffsetDateTime;

public record ContentAssetVersion(
        String versionId,
        String assetId,
        int versionNo,
        String versionLabel,
        String versionStatus,
        String changeSummary,
        String contentSnapshot,
        String previewUrl,
        OffsetDateTime createdAt
) {
}

