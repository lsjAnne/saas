package com.dianshang.platform.saas.infrastructure.persistence;

import com.dianshang.platform.saas.domain.repository.TenantDataExportTaskRepository;
import com.dianshang.platform.saas.model.TenantDataExportTaskRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryTenantDataExportTaskRepository implements TenantDataExportTaskRepository {

    private final ConcurrentMap<String, TenantDataExportTaskRecord> storage = new ConcurrentHashMap<>();

    @Override
    public TenantDataExportTaskRecord save(TenantDataExportTaskRecord task) {
        storage.put(task.exportTaskId(), task);
        return task;
    }

    @Override
    public List<TenantDataExportTaskRecord> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(task -> tenantId.equals(task.tenantId()))
                .sorted(Comparator.comparing(TenantDataExportTaskRecord::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<TenantDataExportTaskRecord> findByTaskId(String exportTaskId) {
        return Optional.ofNullable(storage.get(exportTaskId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}
