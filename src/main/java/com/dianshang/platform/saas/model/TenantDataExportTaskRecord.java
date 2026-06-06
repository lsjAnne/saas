package com.dianshang.platform.saas.model;

import java.time.OffsetDateTime;

public record TenantDataExportTaskRecord(
        String exportTaskId,
        String tenantId,
        String scopeCode,
        String scopeName,
        String requestedBy,
        String status,
        String fileName,
        String contentType,
        String exportContent,
        String maskingStrategy,
        OffsetDateTime timeRangeStart,
        OffsetDateTime timeRangeEnd,
        OffsetDateTime downloadExpiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt
) {
}
