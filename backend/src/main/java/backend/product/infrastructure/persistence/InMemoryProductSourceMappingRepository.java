package backend.product.infrastructure.persistence;

import backend.product.domain.repository.ProductSourceMappingRepository;
import backend.product.model.ProductSourceMapping;
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
public class InMemoryProductSourceMappingRepository implements ProductSourceMappingRepository {

    private final Map<String, ProductSourceMapping> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(9000);

    @Override
    public ProductSourceMapping save(ProductSourceMapping productSourceMapping) {
        String productMappingId = productSourceMapping.productMappingId();
        if (productMappingId == null || productMappingId.isBlank()) {
            productMappingId = "mapping-" + sequence.incrementAndGet();
        }
        ProductSourceMapping saved = new ProductSourceMapping(
                productMappingId,
                productSourceMapping.storeId(),
                productSourceMapping.productId(),
                productSourceMapping.supplierId(),
                productSourceMapping.mappingType(),
                productSourceMapping.active(),
                productSourceMapping.riskFlag(),
                productSourceMapping.createdAt()
        );
        storage.put(saved.productMappingId(), saved);
        return saved;
    }

    @Override
    public List<ProductSourceMapping> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        Set<String> storeIdSet = Set.copyOf(storeIds);
        return storage.values().stream()
                .filter(mapping -> storeIdSet.contains(mapping.storeId()))
                .sorted(Comparator.comparing(ProductSourceMapping::createdAt))
                .toList();
    }

    @Override
    public List<ProductSourceMapping> findByProductId(String productId) {
        return storage.values().stream()
                .filter(mapping -> productId.equals(mapping.productId()))
                .sorted(Comparator.comparing(ProductSourceMapping::createdAt))
                .toList();
    }

    @Override
    public Optional<ProductSourceMapping> findByProductMappingId(String productMappingId) {
        return Optional.ofNullable(storage.get(productMappingId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(9000);
    }
}

