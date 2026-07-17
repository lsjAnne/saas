package backend.qa.domain.repository;

import backend.qa.model.FaqKnowledge;

import java.util.List;

public interface FaqKnowledgeRepository {

    FaqKnowledge save(FaqKnowledge faqKnowledge);

    List<FaqKnowledge> findByStoreIds(List<String> storeIds);

    void deleteAll();
}

