package backend.saas.domain.repository;

import backend.saas.model.TenantProfile;

import java.util.List;
import java.util.Optional;

public interface TenantProfileRepository {

    TenantProfile save(TenantProfile profile);

    Optional<TenantProfile> findByTenantId(String tenantId);

    List<TenantProfile> findAll();

    void deleteAll();
}

