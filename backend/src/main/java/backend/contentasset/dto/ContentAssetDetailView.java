package backend.contentasset.dto;

import backend.contentasset.model.ContentAsset;
import backend.contentasset.model.ContentAssetReference;
import backend.contentasset.model.ContentAssetVersion;

import java.util.List;

public record ContentAssetDetailView(
        ContentAsset asset,
        List<ContentAssetVersion> versions,
        List<ContentAssetReference> references
) {
}

