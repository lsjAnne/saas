package com.dianshang.platform.saas.infrastructure.persistence;

import com.dianshang.platform.saas.domain.repository.ComplianceAcceptanceRepository;
import com.dianshang.platform.saas.model.ComplianceAcceptanceRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryComplianceAcceptanceRepository implements ComplianceAcceptanceRepository {

    private final CopyOnWriteArrayList<ComplianceAcceptanceRecord> storage = new CopyOnWriteArrayList<>();

    @Override
    public ComplianceAcceptanceRecord save(ComplianceAcceptanceRecord acceptance) {
        storage.add(acceptance);
        return acceptance;
    }

    @Override
    public List<ComplianceAcceptanceRecord> findByTenantId(String tenantId) {
        return storage.stream()
                .filter(record -> tenantId.equals(record.tenantId()))
                .sorted(Comparator.comparing(ComplianceAcceptanceRecord::acceptedAt).reversed())
                .toList();
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}
