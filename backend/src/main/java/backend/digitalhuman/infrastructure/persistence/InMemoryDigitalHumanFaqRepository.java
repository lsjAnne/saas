package backend.digitalhuman.infrastructure.persistence;

import backend.digitalhuman.domain.repository.DigitalHumanFaqRepository;
import backend.digitalhuman.model.DigitalHumanFaq;
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
public class InMemoryDigitalHumanFaqRepository implements DigitalHumanFaqRepository {

    private final Map<String, DigitalHumanFaq> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(13000);

    @Override
    public DigitalHumanFaq save(DigitalHumanFaq faq) {
        String faqId = faq.faqId();
        if (faqId == null || faqId.isBlank()) {
            faqId = "dhfaq-" + sequence.incrementAndGet();
        }
        DigitalHumanFaq saved = new DigitalHumanFaq(
                faqId,
                faq.tenantId(),
                faq.organizationId(),
                faq.productId(),
                faq.productName(),
                faq.questionType(),
                faq.keywords(),
                faq.question(),
                faq.answer(),
                faq.enabled(),
                faq.hitCount(),
                faq.createdAt(),
                faq.updatedAt()
        );
        storage.put(saved.faqId(), saved);
        return saved;
    }

    @Override
    public List<DigitalHumanFaq> listByTenant(String tenantId) {
        return storage.values().stream()
                .filter(faq -> faq.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(DigitalHumanFaq::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<DigitalHumanFaq> findById(String tenantId, String faqId) {
        return Optional.ofNullable(storage.get(faqId))
                .filter(faq -> faq.tenantId().equals(tenantId));
    }

    @Override
    public void deleteById(String tenantId, String faqId) {
        storage.remove(faqId);
    }
}
