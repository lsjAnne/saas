package com.dianshang.platform.audit;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/tenant/audit-logs")
@RequireTenantPermission(AuthPermissionCodes.TENANT_AUDIT_READ)
public class TenantAuditLogController {

    private final AuditLogService auditLogService;

    public TenantAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<AuditLogRecord>> list() {
        return ApiResponse.success(
                auditLogService.findByTenantId(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_AUDIT_EXPORT)
    public ResponseEntity<String> export() {
        String tenantId = TenantAccessSupport.requiredTenantId();
        String csv = buildCsv(auditLogService.findByTenantId(tenantId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tenant-audit-" + tenantId + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    private String buildCsv(List<AuditLogRecord> logs) {
        StringBuilder builder = new StringBuilder("tenantId,operatorId,operatorType,actionType,targetType,targetId,traceId,createdAt\n");
        for (AuditLogRecord log : logs) {
            builder.append(csv(log.tenantId())).append(',')
                    .append(csv(log.operatorId())).append(',')
                    .append(csv(log.operatorType())).append(',')
                    .append(csv(log.actionType())).append(',')
                    .append(csv(log.targetType())).append(',')
                    .append(csv(log.targetId())).append(',')
                    .append(csv(log.traceId())).append(',')
                    .append(csv(log.createdAt() == null ? "" : log.createdAt().toString()))
                    .append('\n');
        }
        return builder.toString();
    }

    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
