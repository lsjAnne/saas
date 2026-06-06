package com.dianshang.platform.audit.domain.repository;

import com.dianshang.platform.audit.AuditLogRecord;

import java.util.List;

public interface AuditLogRepository {

    AuditLogRecord save(AuditLogRecord record);

    List<AuditLogRecord> findAll();

    List<AuditLogRecord> findByTenantId(String tenantId);

    void deleteAll();
}
