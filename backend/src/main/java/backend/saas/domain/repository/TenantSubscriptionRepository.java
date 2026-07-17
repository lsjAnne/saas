package backend.saas.domain.repository;

import backend.saas.model.TenantSubscription;

import java.util.Optional;

public interface TenantSubscriptionRepository {

    TenantSubscription save(TenantSubscription subscription);

    Optional<TenantSubscription> findByTenantId(String tenantId);

    void deleteAll();
}

