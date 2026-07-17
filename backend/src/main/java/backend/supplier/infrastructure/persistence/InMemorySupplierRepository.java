package backend.supplier.infrastructure.persistence;

import backend.supplier.domain.repository.SupplierRepository;
import backend.supplier.model.Supplier;
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
public class InMemorySupplierRepository implements SupplierRepository {

    private final Map<String, Supplier> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(8000);

    @Override
    public Supplier save(Supplier supplier) {
        String supplierId = supplier.supplierId();
        if (supplierId == null || supplierId.isBlank()) {
            supplierId = "supplier-" + sequence.incrementAndGet();
        }
        Supplier saved = new Supplier(
                supplierId,
                supplier.storeId(),
                supplier.supplierPlatformType(),
                supplier.supplierPlatformId(),
                supplier.supplierName(),
                supplier.sourceUrl(),
                supplier.priceScore(),
                supplier.deliveryScore(),
                supplier.stabilityScore(),
                supplier.riskLevel(),
                supplier.dropshipSupportFlag(),
                supplier.primary(),
                supplier.backup(),
                supplier.blacklistFlag(),
                supplier.createdAt()
        );
        storage.put(saved.supplierId(), saved);
        return saved;
    }

    @Override
    public List<Supplier> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        Set<String> storeIdSet = Set.copyOf(storeIds);
        return storage.values().stream()
                .filter(supplier -> storeIdSet.contains(supplier.storeId()))
                .sorted(Comparator.comparing(Supplier::createdAt))
                .toList();
    }

    @Override
    public List<Supplier> findByStoreId(String storeId) {
        return storage.values().stream()
                .filter(supplier -> storeId.equals(supplier.storeId()))
                .sorted(Comparator.comparing(Supplier::createdAt))
                .toList();
    }

    @Override
    public Optional<Supplier> findBySupplierId(String supplierId) {
        return Optional.ofNullable(storage.get(supplierId));
    }

    @Override
    public Optional<Supplier> findByStoreAndPlatformSupplier(String storeId, String supplierPlatformType, String supplierPlatformId) {
        return storage.values().stream()
                .filter(supplier -> storeId.equals(supplier.storeId()))
                .filter(supplier -> supplierPlatformType.equals(supplier.supplierPlatformType()))
                .filter(supplier -> supplierPlatformId.equals(supplier.supplierPlatformId()))
                .findFirst();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(8000);
    }
}

