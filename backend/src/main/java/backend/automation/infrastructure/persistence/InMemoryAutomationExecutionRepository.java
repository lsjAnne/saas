package backend.automation.infrastructure.persistence;

import backend.automation.domain.repository.AutomationExecutionRepository;
import backend.automation.model.AutomationExecution;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryAutomationExecutionRepository implements AutomationExecutionRepository {

    private final Map<String, AutomationExecution> storage = new ConcurrentHashMap<>();

    @Override
    public AutomationExecution save(AutomationExecution execution) {
        storage.put(execution.executionId(), execution);
        return execution;
    }

    @Override
    public List<AutomationExecution> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(execution -> execution.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(AutomationExecution::executedAt).reversed()
                        .thenComparing(AutomationExecution::executionId))
                .toList();
    }

    @Override
    public Optional<AutomationExecution> findByExecutionId(String executionId) {
        return Optional.ofNullable(storage.get(executionId));
    }

    @Override
    public Optional<AutomationExecution> findExisting(String tenantId, String targetId, String ruleId, String actionType) {
        return storage.values().stream()
                .filter(execution -> execution.tenantId().equals(tenantId))
                .filter(execution -> execution.targetId().equals(targetId))
                .filter(execution -> execution.ruleId().equals(ruleId))
                .filter(execution -> execution.actionType().equals(actionType))
                .findFirst();
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}
