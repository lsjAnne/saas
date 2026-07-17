package backend.contentasset.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateContentAssetRequest(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "assetCategory is required")
        String assetCategory,
        @NotBlank(message = "assetType is required")
        String assetType,
        @NotBlank(message = "assetName is required")
        String assetName,
        @NotBlank(message = "brief is required")
        String brief,
        String tone,
        String referenceHint,
        String sourceAssetId,
        String clipTemplate,
        Integer durationSeconds,
        String publishPlatform
) {
}

