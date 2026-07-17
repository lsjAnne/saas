package backend.inventory.infrastructure.persistence;

import backend.inventory.domain.repository.InventorySnapshotRepository;
import backend.inventory.model.InventorySnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryInventorySnapshotRepository implements InventorySnapshotRepository {

    private final Map<String, InventorySnapshot> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(10000);

    @Override
    public InventorySnapshot save(InventorySnapshot inventorySnapshot) {
        String inventorySnapshotId = inventorySnapshot.inventorySnapshotId();
        if (inventorySnapshotId == null || inventorySnapshotId.isBlank()) {
            inventorySnapshotId = "inventory-" + sequence.incrementAndGet();
        }
        InventorySnapshot saved = new InventorySnapshot(
                inventorySnapshotId,
                inventorySnapshot.storeId(),
                inventorySnapshot.productId(),
                inventorySnapshot.skuId(),
                inventorySnapshot.availableStock(),
                inventorySnapshot.reservedStock(),
                inventorySnapshot.safetyStock(),
                inventorySnapshot.snapshotAt(),
                inventorySnapshot.createdAt()
        );
        storage.put(saved.inventorySnapshotId(), saved);
        return saved;
    }

    @Override
    public List<InventorySnapshot> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        Set<String> storeIdSet = Set.copyOf(storeIds);
        return storage.values().stream()
                .filter(snapshot -> storeIdSet.contains(snapshot.storeId()))
                .sorted(Comparator.comparing(InventorySnapshot::snapshotAt).reversed())
                .toList();
    }

    @Override
    public Optional<InventorySnapshot> findByInventorySnapshotId(String inventorySnapshotId) {
        return Optional.ofNullable(storage.get(inventorySnapshotId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(10000);
    }
}

