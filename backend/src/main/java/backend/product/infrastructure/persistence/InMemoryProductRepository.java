package backend.product.infrastructure.persistence;

import backend.product.domain.repository.ProductRepository;
import backend.product.model.Product;
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
public class InMemoryProductRepository implements ProductRepository {

    private final Map<String, Product> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(7000);

    @Override
    public Product save(Product product) {
        String productId = product.productId();
        if (productId == null || productId.isBlank()) {
            productId = "product-" + sequence.incrementAndGet();
        }
        Product saved = new Product(
                productId,
                product.storeId(),
                product.platformProductId(),
                product.productDraftId(),
                product.title(),
                product.status(),
                product.healthScore(),
                product.publishedAt(),
                product.createdAt()
        );
        storage.put(saved.productId(), saved);
        return saved;
    }

    @Override
    public List<Product> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(product -> storeIds.contains(product.storeId()))
                .sorted(Comparator.comparing(Product::createdAt))
                .toList();
    }

    @Override
    public Optional<Product> findByProductId(String productId) {
        return Optional.ofNullable(storage.get(productId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(7000);
    }
}

