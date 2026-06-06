package com.dianshang.platform.qa.infrastructure.persistence;

import com.dianshang.platform.qa.domain.repository.ConversationMessageRepository;
import com.dianshang.platform.qa.model.ConversationMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryConversationMessageRepository implements ConversationMessageRepository {

    private final Map<String, ConversationMessage> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(9800);

    @Override
    public ConversationMessage save(ConversationMessage conversationMessage) {
        String messageId = conversationMessage.messageId();
        if (messageId == null || messageId.isBlank()) {
            messageId = "message-" + sequence.incrementAndGet();
        }
        ConversationMessage saved = new ConversationMessage(
                messageId,
                conversationMessage.conversationId(),
                conversationMessage.senderType(),
                conversationMessage.messageType(),
                conversationMessage.contentText(),
                conversationMessage.aiGeneratedFlag(),
                conversationMessage.riskFlag(),
                conversationMessage.createdAt()
        );
        storage.put(saved.messageId(), saved);
        return saved;
    }

    @Override
    public List<ConversationMessage> findByConversationId(String conversationId) {
        return storage.values().stream()
                .filter(message -> conversationId.equals(message.conversationId()))
                .sorted(Comparator.comparing(ConversationMessage::createdAt))
                .toList();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(9800);
    }
}
