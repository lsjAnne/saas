package backend.saas.infrastructure.persistence;

import backend.saas.domain.repository.TenantProfileRepository;
import backend.saas.model.TenantProfile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryTenantProfileRepository implements TenantProfileRepository {

    private final Map<String, TenantProfile> storage = new ConcurrentHashMap<>();

    @Override
    public TenantProfile save(TenantProfile profile) {
        storage.put(profile.tenantId(), profile);
        return profile;
    }

    @Override
    public Optional<TenantProfile> findByTenantId(String tenantId) {
        return Optional.ofNullable(storage.get(tenantId));
    }

    @Override
    public List<TenantProfile> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(TenantProfile::createdAt))
                .toList();
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}

