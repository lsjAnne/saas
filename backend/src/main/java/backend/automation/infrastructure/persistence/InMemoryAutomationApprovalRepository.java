package backend.automation.infrastructure.persistence;

import backend.automation.domain.repository.AutomationApprovalRepository;
import backend.automation.model.AutomationApproval;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryAutomationApprovalRepository implements AutomationApprovalRepository {

    private final Map<String, AutomationApproval> storage = new ConcurrentHashMap<>();

    @Override
    public AutomationApproval save(AutomationApproval approval) {
        storage.put(approval.taskId(), approval);
        return approval;
    }

    @Override
    public List<AutomationApproval> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(approval -> approval.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(AutomationApproval::createdAt).reversed()
                        .thenComparing(AutomationApproval::taskId))
                .toList();
    }

    @Override
    public Optional<AutomationApproval> findByTaskId(String taskId) {
        return Optional.ofNullable(storage.get(taskId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}
