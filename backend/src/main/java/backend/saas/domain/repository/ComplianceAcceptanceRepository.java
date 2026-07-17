package backend.saas.domain.repository;

import backend.saas.model.ComplianceAcceptanceRecord;

import java.util.List;

public interface ComplianceAcceptanceRepository {

    ComplianceAcceptanceRecord save(ComplianceAcceptanceRecord acceptance);

    List<ComplianceAcceptanceRecord> findByTenantId(String tenantId);

    void deleteAll();
}

