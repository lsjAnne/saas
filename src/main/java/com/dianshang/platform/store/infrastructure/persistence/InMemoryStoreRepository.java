package com.dianshang.platform.store.infrastructure.persistence;

import com.dianshang.platform.store.domain.repository.StoreRepository;
import com.dianshang.platform.store.model.Store;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryStoreRepository implements StoreRepository {

    private final Map<String, Store> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(3000);

    @Override
    public Store save(Store store) {
        String storeId = store.storeId();
        if (storeId == null || storeId.isBlank()) {
            storeId = "store-" + sequence.incrementAndGet();
        }
        Store saved = new Store(
                storeId,
                store.tenantId(),
                store.organizationId(),
                store.ownerUserId(),
                store.platformType(),
                store.platformShopId(),
                store.shopName(),
                store.authStatus(),
                store.profitThreshold(),
                store.riskThreshold(),
                store.defaultShipConfig(),
                store.createdAt()
        );
        storage.put(saved.storeId(), saved);
        return saved;
    }

    @Override
    public List<Store> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(store -> tenantId.equals(store.tenantId()))
                .sorted(Comparator.comparing(Store::createdAt))
                .toList();
    }

    @Override
    public Optional<Store> findByStoreId(String storeId) {
        return Optional.ofNullable(storage.get(storeId));
    }

    @Override
    public Optional<Store> findByTenantAndPlatformShop(String tenantId, String platformType, String platformShopId) {
        return storage.values().stream()
                .filter(store -> tenantId.equals(store.tenantId()))
                .filter(store -> platformType.equals(store.platformType()))
                .filter(store -> platformShopId.equals(store.platformShopId()))
                .findFirst();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(3000);
    }
}
