package backend.product.infrastructure.persistence;

import backend.product.domain.repository.CandidateProductRepository;
import backend.product.model.CandidateProduct;
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
public class InMemoryCandidateProductRepository implements CandidateProductRepository {

    private final Map<String, CandidateProduct> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(5000);

    @Override
    public CandidateProduct save(CandidateProduct candidateProduct) {
        String candidateProductId = candidateProduct.candidateProductId();
        if (candidateProductId == null || candidateProductId.isBlank()) {
            candidateProductId = "candidate-" + sequence.incrementAndGet();
        }
        CandidateProduct saved = new CandidateProduct(
                candidateProductId,
                candidateProduct.storeId(),
                candidateProduct.sourceType(),
                candidateProduct.sourceUrl(),
                candidateProduct.sourceUrlHash(),
                candidateProduct.title(),
                candidateProduct.category(),
                candidateProduct.status(),
                candidateProduct.estimatedProfit(),
                candidateProduct.riskLevel(),
                candidateProduct.recommendationReason(),
                candidateProduct.aiSummary(),
                candidateProduct.createdAt()
        );
        storage.put(saved.candidateProductId(), saved);
        return saved;
    }

    @Override
    public List<CandidateProduct> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        Set<String> storeIdSet = Set.copyOf(storeIds);
        return storage.values().stream()
                .filter(candidateProduct -> storeIdSet.contains(candidateProduct.storeId()))
                .sorted(Comparator.comparing(CandidateProduct::createdAt))
                .toList();
    }

    @Override
    public Optional<CandidateProduct> findByCandidateProductId(String candidateProductId) {
        return Optional.ofNullable(storage.get(candidateProductId));
    }

    @Override
    public Optional<CandidateProduct> findByStoreAndSourceUrlHash(String storeId, String sourceUrlHash) {
        return storage.values().stream()
                .filter(candidateProduct -> storeId.equals(candidateProduct.storeId()))
                .filter(candidateProduct -> sourceUrlHash.equals(candidateProduct.sourceUrlHash()))
                .findFirst();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(5000);
    }
}

