package backend.contentasset.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadContentAssetRequest(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "assetCategory is required")
        String assetCategory,
        @NotBlank(message = "assetType is required")
        String assetType,
        @NotBlank(message = "assetName is required")
        String assetName,
        String sourceUrl,
        String previewText,
        String changeSummary,
        String referenceHint
) {
}

