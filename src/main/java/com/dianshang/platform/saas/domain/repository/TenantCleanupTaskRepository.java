package com.dianshang.platform.saas.domain.repository;

import com.dianshang.platform.saas.model.TenantCleanupTaskRecord;

import java.util.List;
import java.util.Optional;

public interface TenantCleanupTaskRepository {

    TenantCleanupTaskRecord save(TenantCleanupTaskRecord task);

    List<TenantCleanupTaskRecord> findByTenantId(String tenantId);

    Optional<TenantCleanupTaskRecord> findByTaskId(String cleanupTaskId);

    void deleteAll();
}
