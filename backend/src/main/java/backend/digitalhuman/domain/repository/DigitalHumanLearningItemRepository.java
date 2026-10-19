package backend.digitalhuman.domain.repository;

import backend.digitalhuman.model.DigitalHumanLearningItem;

import java.util.List;
import java.util.Optional;

public interface DigitalHumanLearningItemRepository {

    DigitalHumanLearningItem save(DigitalHumanLearningItem item);

    List<DigitalHumanLearningItem> listByTenant(String tenantId);

    Optional<DigitalHumanLearningItem> findById(String tenantId, String itemId);

    void deleteById(String tenantId, String itemId);
}
