package com.dianshang.platform.saas.infrastructure.persistence;

import com.dianshang.platform.saas.domain.repository.UsageQuotaRepository;
import com.dianshang.platform.saas.model.UsageQuota;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryUsageQuotaRepository implements UsageQuotaRepository {

    private final Map<String, List<UsageQuota>> storage = new ConcurrentHashMap<>();

    @Override
    public void saveAll(String tenantId, List<UsageQuota> quotas) {
        storage.put(tenantId, List.copyOf(quotas));
    }

    @Override
    public List<UsageQuota> findByTenantId(String tenantId) {
        return List.copyOf(storage.getOrDefault(tenantId, List.of()));
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}
