package com.dianshang.platform.qa.domain.repository;

import com.dianshang.platform.qa.model.FaqKnowledge;

import java.util.List;

public interface FaqKnowledgeRepository {

    FaqKnowledge save(FaqKnowledge faqKnowledge);

    List<FaqKnowledge> findByStoreIds(List<String> storeIds);

    void deleteAll();
}
