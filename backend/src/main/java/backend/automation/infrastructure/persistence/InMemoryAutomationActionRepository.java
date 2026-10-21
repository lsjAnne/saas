package backend.automation.infrastructure.persistence;

import backend.automation.domain.repository.AutomationActionRepository;
import backend.automation.model.AutomationAction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryAutomationActionRepository implements AutomationActionRepository {

    private final Map<String, AutomationAction> storage = new ConcurrentHashMap<>();

    @Override
    public AutomationAction save(AutomationAction action) {
        storage.put(action.actionId(), action);
        return action;
    }

    @Override
    public List<AutomationAction> findByStoreId(String storeId) {
        return storage.values().stream()
                .filter(action -> action.storeId().equals(storeId))
                .sorted(Comparator.comparingInt(AutomationAction::priority)
                        .thenComparing(AutomationAction::createdAt)
                        .thenComparing(AutomationAction::actionId))
                .toList();
    }

    @Override
    public Optional<AutomationAction> findByActionId(String actionId) {
        return Optional.ofNullable(storage.get(actionId));
    }

    @Override
    public void deleteByActionId(String actionId) {
        storage.remove(actionId);
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}
