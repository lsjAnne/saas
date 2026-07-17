package backend.saas.domain.repository;

import backend.saas.model.UsageQuota;

import java.util.List;

public interface UsageQuotaRepository {

    void saveAll(String tenantId, List<UsageQuota> quotas);

    List<UsageQuota> findByTenantId(String tenantId);

    void deleteAll();
}

