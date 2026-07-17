package backend.saas.infrastructure.persistence;

import backend.saas.domain.repository.TenantCleanupTaskRepository;
import backend.saas.model.TenantCleanupTaskRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryTenantCleanupTaskRepository implements TenantCleanupTaskRepository {

    private final ConcurrentMap<String, TenantCleanupTaskRecord> storage = new ConcurrentHashMap<>();

    @Override
    public TenantCleanupTaskRecord save(TenantCleanupTaskRecord task) {
        storage.put(task.cleanupTaskId(), task);
        return task;
    }

    @Override
    public List<TenantCleanupTaskRecord> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(task -> tenantId.equals(task.tenantId()))
                .sorted(Comparator.comparing(TenantCleanupTaskRecord::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<TenantCleanupTaskRecord> findByTaskId(String cleanupTaskId) {
        return Optional.ofNullable(storage.get(cleanupTaskId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}

