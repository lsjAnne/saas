package com.dianshang.platform.openplatform.model;

import java.time.OffsetDateTime;

public record OpenPlatformCallLog(
        String logId,
        String tenantId,
        String organizationId,
        String appId,
        String subscriptionId,
        String requestId,
        String endpoint,
        String direction,
        String sourceModule,
        String resultStatus,
        boolean signatureVerified,
        boolean replayed,
        String traceId,
        String message,
        OffsetDateTime createdAt
) {
}
