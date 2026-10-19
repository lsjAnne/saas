package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanLearningItemRepository;
import backend.digitalhuman.model.DigitalHumanLearningItem;
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
public class InMemoryDigitalHumanLearningItemRepository implements DigitalHumanLearningItemRepository {

    private final Map<String, DigitalHumanLearningItem> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(16000);

    @Override
    public DigitalHumanLearningItem save(DigitalHumanLearningItem item) {
        String itemId = item.itemId();
        if (itemId == null || itemId.isBlank()) {
            itemId = "dhlearning-" + sequence.incrementAndGet();
        }
        DigitalHumanLearningItem saved = new DigitalHumanLearningItem(
                itemId,
                item.tenantId(),
                item.organizationId(),
                item.sessionId(),
                item.question(),
                item.questionType(),
                item.productName(),
                item.occurrenceCount(),
                item.status(),
                item.faqId(),
                item.lastSeenAt(),
                item.createdAt()
        );
        storage.put(saved.itemId(), saved);
        return saved;
    }

    @Override
    public List<DigitalHumanLearningItem> listByTenant(String tenantId) {
        return storage.values().stream()
                .filter(item -> item.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(DigitalHumanLearningItem::lastSeenAt).reversed())
                .toList();
    }

    @Override
    public Optional<DigitalHumanLearningItem> findById(String tenantId, String itemId) {
        return Optional.ofNullable(storage.get(itemId))
                .filter(item -> item.tenantId().equals(tenantId));
    }

    @Override
    public void deleteById(String tenantId, String itemId) {
        storage.remove(itemId);
    }
}
