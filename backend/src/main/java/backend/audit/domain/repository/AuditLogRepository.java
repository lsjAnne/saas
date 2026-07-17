package backend.audit.domain.repository;

import backend.audit.model.AuditLogRecord;

import java.util.List;

public interface AuditLogRepository {

    AuditLogRecord save(AuditLogRecord record);

    List<AuditLogRecord> findAll();

    List<AuditLogRecord> findByTenantId(String tenantId);

    void deleteAll();
}

