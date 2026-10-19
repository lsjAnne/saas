package backend.digitalhuman.domain.repository;

import backend.digitalhuman.model.DigitalHumanFaq;

import java.util.List;
import java.util.Optional;

public interface DigitalHumanFaqRepository {

    DigitalHumanFaq save(DigitalHumanFaq faq);

    List<DigitalHumanFaq> listByTenant(String tenantId);

    Optional<DigitalHumanFaq> findById(String tenantId, String faqId);

    void deleteById(String tenantId, String faqId);
}
