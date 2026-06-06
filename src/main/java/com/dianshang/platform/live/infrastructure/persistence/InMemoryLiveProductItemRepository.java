package com.dianshang.platform.live.infrastructure.persistence;

import com.dianshang.platform.live.domain.repository.LiveProductItemRepository;
import com.dianshang.platform.live.model.LiveProductItem;
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
public class InMemoryLiveProductItemRepository implements LiveProductItemRepository {

    private final Map<String, LiveProductItem> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(21000);

    @Override
    public LiveProductItem save(LiveProductItem liveProductItem) {
        String liveProductItemId = liveProductItem.liveProductItemId();
        if (liveProductItemId == null || liveProductItemId.isBlank()) {
            liveProductItemId = "live-product-item-" + sequence.incrementAndGet();
        }
        LiveProductItem saved = new LiveProductItem(
                liveProductItemId,
                liveProductItem.livePlanId(),
                liveProductItem.candidateProductId(),
                liveProductItem.productDraftId(),
                liveProductItem.productId(),
                liveProductItem.sortOrder(),
                liveProductItem.createdAt()
        );
        storage.put(saved.liveProductItemId(), saved);
        return saved;
    }

    @Override
    public List<LiveProductItem> findByLivePlanId(String livePlanId) {
        return storage.values().stream()
                .filter(item -> livePlanId.equals(item.livePlanId()))
                .sorted(Comparator.comparingInt(LiveProductItem::sortOrder)
                        .thenComparing(LiveProductItem::createdAt)
                        .thenComparing(LiveProductItem::liveProductItemId))
                .toList();
    }

    @Override
    public Optional<LiveProductItem> findByLiveProductItemId(String liveProductItemId) {
        return Optional.ofNullable(storage.get(liveProductItemId));
    }

    @Override
    public void deleteByLiveProductItemId(String liveProductItemId) {
        storage.remove(liveProductItemId);
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(21000);
    }
}
