package com.dianshang.platform.qa.domain.repository;

import com.dianshang.platform.qa.model.CustomerConversation;

import java.util.List;
import java.util.Optional;

public interface CustomerConversationRepository {

    CustomerConversation save(CustomerConversation customerConversation);

    List<CustomerConversation> findByStoreIds(List<String> storeIds);

    Optional<CustomerConversation> findByConversationId(String conversationId);

    void deleteAll();
}
