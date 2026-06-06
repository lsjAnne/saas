package com.dianshang.platform.qa.infrastructure.persistence;

import com.dianshang.platform.qa.domain.repository.FaqKnowledgeRepository;
import com.dianshang.platform.qa.model.FaqKnowledge;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryFaqKnowledgeRepository implements FaqKnowledgeRepository {

    private final Map<String, FaqKnowledge> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(9900);

    @Override
    public FaqKnowledge save(FaqKnowledge faqKnowledge) {
        String faqId = faqKnowledge.faqId();
        if (faqId == null || faqId.isBlank()) {
            faqId = "faq-" + sequence.incrementAndGet();
        }
        FaqKnowledge saved = new FaqKnowledge(
                faqId,
                faqKnowledge.storeId(),
                faqKnowledge.productId(),
                faqKnowledge.question(),
                faqKnowledge.answer(),
                faqKnowledge.sourceType(),
                faqKnowledge.enabled(),
                faqKnowledge.createdAt()
        );
        storage.put(saved.faqId(), saved);
        return saved;
    }

    @Override
    public List<FaqKnowledge> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(faq -> storeIds.contains(faq.storeId()))
                .sorted(Comparator.comparing(FaqKnowledge::createdAt).reversed())
                .toList();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(9900);
    }
}
