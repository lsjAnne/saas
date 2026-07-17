package backend.qa.domain.repository;

import backend.qa.model.ConversationMessage;

import java.util.List;

public interface ConversationMessageRepository {

    ConversationMessage save(ConversationMessage conversationMessage);

    List<ConversationMessage> findByConversationId(String conversationId);

    void deleteAll();
}

