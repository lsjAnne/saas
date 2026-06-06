package com.dianshang.platform.saas.domain.repository;

import com.dianshang.platform.saas.model.UsageQuota;

import java.util.List;

public interface UsageQuotaRepository {

    void saveAll(String tenantId, List<UsageQuota> quotas);

    List<UsageQuota> findByTenantId(String tenantId);

    void deleteAll();
}
