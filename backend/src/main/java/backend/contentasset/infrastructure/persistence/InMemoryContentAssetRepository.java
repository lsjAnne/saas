package backend.contentasset.infrastructure.persistence;

import backend.contentasset.domain.repository.ContentAssetRepository;
import backend.contentasset.model.ContentAsset;
import backend.contentasset.model.ContentAssetReference;
import backend.contentasset.model.ContentAssetVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryContentAssetRepository implements ContentAssetRepository {

    private final Map<String, ContentAsset> assetStorage = new ConcurrentHashMap<>();
    private final Map<String, ContentAssetVersion> versionStorage = new ConcurrentHashMap<>();
    private final Map<String, ContentAssetReference> referenceStorage = new ConcurrentHashMap<>();

    @Override
    public ContentAsset saveAsset(ContentAsset asset) {
        assetStorage.put(asset.assetId(), asset);
        return asset;
    }

    @Override
    public List<ContentAsset> findAssetsByStoreIds(List<String> storeIds) {
        return assetStorage.values().stream()
                .filter(asset -> storeIds.contains(asset.storeId()))
                .toList();
    }

    @Override
    public Optional<ContentAsset> findAssetById(String assetId) {
        return Optional.ofNullable(assetStorage.get(assetId));
    }

    @Override
    public ContentAssetVersion saveVersion(ContentAssetVersion version) {
        versionStorage.put(version.versionId(), version);
        return version;
    }

    @Override
    public List<ContentAssetVersion> findVersionsByAssetId(String assetId) {
        return versionStorage.values().stream()
                .filter(version -> assetId.equals(version.assetId()))
                .toList();
    }

    @Override
    public ContentAssetReference saveReference(ContentAssetReference reference) {
        referenceStorage.put(reference.referenceId(), reference);
        return reference;
    }

    @Override
    public List<ContentAssetReference> findReferencesByAssetId(String assetId) {
        return referenceStorage.values().stream()
                .filter(reference -> assetId.equals(reference.assetId()))
                .toList();
    }

    @Override
    public void deleteAll() {
        referenceStorage.clear();
        versionStorage.clear();
        assetStorage.clear();
    }
}

