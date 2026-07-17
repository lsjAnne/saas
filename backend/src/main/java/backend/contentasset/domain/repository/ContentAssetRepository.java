package backend.contentasset.domain.repository;

import backend.contentasset.model.ContentAsset;
import backend.contentasset.model.ContentAssetReference;
import backend.contentasset.model.ContentAssetVersion;

import java.util.List;
import java.util.Optional;

public interface ContentAssetRepository {

    ContentAsset saveAsset(ContentAsset asset);

    List<ContentAsset> findAssetsByStoreIds(List<String> storeIds);

    Optional<ContentAsset> findAssetById(String assetId);

    ContentAssetVersion saveVersion(ContentAssetVersion version);

    List<ContentAssetVersion> findVersionsByAssetId(String assetId);

    ContentAssetReference saveReference(ContentAssetReference reference);

    List<ContentAssetReference> findReferencesByAssetId(String assetId);

    void deleteAll();
}

