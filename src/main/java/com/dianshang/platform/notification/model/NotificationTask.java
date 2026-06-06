package com.dianshang.platform.notification.model;

import java.time.OffsetDateTime;

public record NotificationTask(
        String notificationTaskId,
        String tenantId,
        String notifyType,
        String templateCode,
        String targetReceiver,
        String sendStatus,
        int retryCount,
        String payloadJson,
        String priority,
        OffsetDateTime scheduledAt,
        String batchId,
        String deadLetterReason,
        OffsetDateTime createdAt
) {
    public NotificationTask withDispatchResult(String sendStatus, int retryCount, String deadLetterReason) {
        return new NotificationTask(
                notificationTaskId,
                tenantId,
                notifyType,
                templateCode,
                targetReceiver,
                sendStatus,
                retryCount,
                payloadJson,
                priority,
                scheduledAt,
                batchId,
                deadLetterReason,
                createdAt
        );
    }

    public NotificationTask withDispatchPlan(String sendStatus, OffsetDateTime scheduledAt, String batchId) {
        return new NotificationTask(
                notificationTaskId,
                tenantId,
                notifyType,
                templateCode,
                targetReceiver,
                sendStatus,
                retryCount,
                payloadJson,
                priority,
                scheduledAt,
                batchId,
                deadLetterReason,
                createdAt
        );
    }
}
