package com.dianshang.platform.qa.domain.repository;

import com.dianshang.platform.qa.model.ConversationMessage;

import java.util.List;

public interface ConversationMessageRepository {

    ConversationMessage save(ConversationMessage conversationMessage);

    List<ConversationMessage> findByConversationId(String conversationId);

    void deleteAll();
}
