package com.dianshang.platform.saas.domain.repository;

import com.dianshang.platform.saas.model.ComplianceAcceptanceRecord;

import java.util.List;

public interface ComplianceAcceptanceRepository {

    ComplianceAcceptanceRecord save(ComplianceAcceptanceRecord acceptance);

    List<ComplianceAcceptanceRecord> findByTenantId(String tenantId);

    void deleteAll();
}
