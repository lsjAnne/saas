package com.dianshang.platform.qa.infrastructure.persistence;

import com.dianshang.platform.qa.domain.repository.CustomerConversationRepository;
import com.dianshang.platform.qa.model.CustomerConversation;
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
public class InMemoryCustomerConversationRepository implements CustomerConversationRepository {

    private final Map<String, CustomerConversation> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(9700);

    @Override
    public CustomerConversation save(CustomerConversation customerConversation) {
        String conversationId = customerConversation.conversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = "conversation-" + sequence.incrementAndGet();
        }
        CustomerConversation saved = new CustomerConversation(
                conversationId,
                customerConversation.storeId(),
                customerConversation.platformType(),
                customerConversation.platformConversationId(),
                customerConversation.customerId(),
                customerConversation.conversationStatus(),
                customerConversation.riskFlag(),
                customerConversation.lastMessageAt(),
                customerConversation.createdAt()
        );
        storage.put(saved.conversationId(), saved);
        return saved;
    }

    @Override
    public List<CustomerConversation> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(conversation -> storeIds.contains(conversation.storeId()))
                .sorted(Comparator.comparing(CustomerConversation::lastMessageAt).reversed())
                .toList();
    }

    @Override
    public Optional<CustomerConversation> findByConversationId(String conversationId) {
        return Optional.ofNullable(storage.get(conversationId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(9700);
    }
}
