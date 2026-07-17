package backend.organization.domain.repository;

import backend.organization.model.Organization;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository {

    Organization save(Organization organization);

    List<Organization> findByTenantId(String tenantId);

    Optional<Organization> findById(String organizationId);

    void deleteAll();
}

