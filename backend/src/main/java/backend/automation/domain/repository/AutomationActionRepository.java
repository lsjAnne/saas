package backend.automation.domain.repository;

import backend.automation.model.AutomationAction;

import java.util.List;
import java.util.Optional;

public interface AutomationActionRepository {

    AutomationAction save(AutomationAction action);

    List<AutomationAction> findByStoreId(String storeId);

    Optional<AutomationAction> findByActionId(String actionId);

    void deleteByActionId(String actionId);

    void deleteAll();
}
