package com.dianshang.platform.inventory.domain.repository;

import com.dianshang.platform.inventory.model.InventorySnapshot;

import java.util.List;
import java.util.Optional;

public interface InventorySnapshotRepository {

    InventorySnapshot save(InventorySnapshot inventorySnapshot);

    List<InventorySnapshot> findByStoreIds(List<String> storeIds);

    Optional<InventorySnapshot> findByInventorySnapshotId(String inventorySnapshotId);

    void deleteAll();
}
