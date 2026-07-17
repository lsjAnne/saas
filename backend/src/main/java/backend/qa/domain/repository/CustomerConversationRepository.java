package backend.qa.domain.repository;

import backend.qa.model.CustomerConversation;

import java.util.List;
import java.util.Optional;

public interface CustomerConversationRepository {

    CustomerConversation save(CustomerConversation customerConversation);

    List<CustomerConversation> findByStoreIds(List<String> storeIds);

    Optional<CustomerConversation> findByConversationId(String conversationId);

    void deleteAll();
}

