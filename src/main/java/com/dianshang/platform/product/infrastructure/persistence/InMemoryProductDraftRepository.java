package com.dianshang.platform.product.infrastructure.persistence;

import com.dianshang.platform.product.domain.repository.ProductDraftRepository;
import com.dianshang.platform.product.model.ProductDraft;
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
public class InMemoryProductDraftRepository implements ProductDraftRepository {

    private final Map<String, ProductDraft> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(6000);

    @Override
    public ProductDraft save(ProductDraft productDraft) {
        String productDraftId = productDraft.productDraftId();
        if (productDraftId == null || productDraftId.isBlank()) {
            productDraftId = "draft-" + sequence.incrementAndGet();
        }
        ProductDraft saved = new ProductDraft(
                productDraftId,
                productDraft.storeId(),
                productDraft.candidateProductId(),
                productDraft.title(),
                productDraft.sellingPoints(),
                productDraft.detailContent(),
                productDraft.faqContent(),
                productDraft.suggestedPrice(),
                productDraft.status(),
                productDraft.aiVersion(),
                productDraft.createdAt()
        );
        storage.put(saved.productDraftId(), saved);
        return saved;
    }

    @Override
    public List<ProductDraft> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        Set<String> storeIdSet = Set.copyOf(storeIds);
        return storage.values().stream()
                .filter(productDraft -> storeIdSet.contains(productDraft.storeId()))
                .sorted(Comparator.comparing(ProductDraft::createdAt))
                .toList();
    }

    @Override
    public Optional<ProductDraft> findByProductDraftId(String productDraftId) {
        return Optional.ofNullable(storage.get(productDraftId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(6000);
    }
}
