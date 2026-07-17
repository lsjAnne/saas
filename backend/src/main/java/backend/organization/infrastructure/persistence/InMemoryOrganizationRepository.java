package backend.organization.infrastructure.persistence;

import backend.organization.domain.repository.OrganizationRepository;
import backend.organization.model.Organization;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryOrganizationRepository implements OrganizationRepository {

    private final Map<String, Organization> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(100);

    @Override
    public Organization save(Organization organization) {
        String organizationId = organization.id();
        if (organizationId == null || organizationId.isBlank()) {
            organizationId = "org-" + sequence.incrementAndGet();
        }
        Organization saved = new Organization(
                organizationId,
                organization.tenantId(),
                organization.organizationName(),
                organization.status(),
                organization.createdAt()
        );
        storage.put(saved.id(), saved);
        return saved;
    }

    @Override
    public List<Organization> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(organization -> tenantId.equals(organization.tenantId()))
                .sorted(Comparator.comparing(Organization::createdAt))
                .toList();
    }

    @Override
    public Optional<Organization> findById(String organizationId) {
        return Optional.ofNullable(storage.get(organizationId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(100);
    }
}

