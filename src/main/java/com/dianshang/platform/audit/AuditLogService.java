package com.dianshang.platform.audit;

import com.dianshang.platform.audit.domain.repository.AuditLogRepository;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.tenant.TenantContext;
import com.dianshang.platform.tenant.TenantContextHolder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String actionType, String targetType, String targetId) {
        TenantContext tenantContext = TenantContextHolder.get();
        recordInternal(
                tenantContext == null ? "platform" : tenantContext.tenantId(),
                tenantContext == null ? "system" : tenantContext.operatorId(),
                tenantContext == null ? "platform-system" : tenantContext.operatorType(),
                actionType,
                targetType,
                targetId
        );
    }

    public void recordForTenant(String tenantId, String actionType, String targetType, String targetId) {
        TenantContext tenantContext = TenantContextHolder.get();
        recordInternal(
                tenantId,
                tenantContext == null ? "system" : tenantContext.operatorId(),
                tenantContext == null ? "platform-system" : tenantContext.operatorType(),
                actionType,
                targetType,
                targetId
        );
    }

    public List<AuditLogRecord> findAll() {
        return auditLogRepository.findAll();
    }

    public List<AuditLogRecord> findByTenantId(String tenantId) {
        return auditLogRepository.findByTenantId(tenantId);
    }

    public void clear() {
        auditLogRepository.deleteAll();
    }

    private void recordInternal(String tenantId,
                                String operatorId,
                                String operatorType,
                                String actionType,
                                String targetType,
                                String targetId) {
        auditLogRepository.save(new AuditLogRecord(
                tenantId,
                operatorId,
                operatorType,
                actionType,
                targetType,
                targetId,
                TraceIdHolder.get(),
                OffsetDateTime.now()
        ));
    }
}
