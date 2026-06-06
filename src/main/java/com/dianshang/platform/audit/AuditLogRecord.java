package com.dianshang.platform.audit;

import java.time.OffsetDateTime;

public record AuditLogRecord(
        String tenantId,
        String operatorId,
        String operatorType,
        String actionType,
        String targetType,
        String targetId,
        String traceId,
        OffsetDateTime createdAt
) {
}
