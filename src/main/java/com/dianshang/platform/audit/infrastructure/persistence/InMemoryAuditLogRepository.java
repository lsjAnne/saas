package com.dianshang.platform.audit.infrastructure.persistence;

import com.dianshang.platform.audit.AuditLogRecord;
import com.dianshang.platform.audit.domain.repository.AuditLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryAuditLogRepository implements AuditLogRepository {

    private final List<AuditLogRecord> storage = new CopyOnWriteArrayList<>();

    @Override
    public AuditLogRecord save(AuditLogRecord record) {
        storage.add(record);
        return record;
    }

    @Override
    public List<AuditLogRecord> findAll() {
        return List.copyOf(storage);
    }

    @Override
    public List<AuditLogRecord> findByTenantId(String tenantId) {
        return storage.stream()
                .filter(record -> tenantId.equals(record.tenantId()))
                .toList();
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}
