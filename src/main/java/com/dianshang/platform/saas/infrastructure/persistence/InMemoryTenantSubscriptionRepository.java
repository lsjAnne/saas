package com.dianshang.platform.saas.infrastructure.persistence;

import com.dianshang.platform.saas.domain.repository.TenantSubscriptionRepository;
import com.dianshang.platform.saas.model.TenantSubscription;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryTenantSubscriptionRepository implements TenantSubscriptionRepository {

    private final Map<String, TenantSubscription> storage = new ConcurrentHashMap<>();

    @Override
    public TenantSubscription save(TenantSubscription subscription) {
        storage.put(subscription.tenantId(), subscription);
        return subscription;
    }

    @Override
    public Optional<TenantSubscription> findByTenantId(String tenantId) {
        return Optional.ofNullable(storage.get(tenantId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}
