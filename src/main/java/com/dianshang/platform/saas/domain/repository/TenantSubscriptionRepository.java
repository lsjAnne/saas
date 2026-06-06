package com.dianshang.platform.saas.domain.repository;

import com.dianshang.platform.saas.model.TenantSubscription;

import java.util.Optional;

public interface TenantSubscriptionRepository {

    TenantSubscription save(TenantSubscription subscription);

    Optional<TenantSubscription> findByTenantId(String tenantId);

    void deleteAll();
}
