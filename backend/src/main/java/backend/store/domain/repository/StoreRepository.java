package backend.store.domain.repository;

import backend.store.model.Store;

import java.util.List;
import java.util.Optional;

public interface StoreRepository {

    Store save(Store store);

    List<Store> findByTenantId(String tenantId);

    Optional<Store> findByStoreId(String storeId);

    Optional<Store> findByTenantAndPlatformShop(String tenantId, String platformType, String platformShopId);

    void deleteAll();
}

