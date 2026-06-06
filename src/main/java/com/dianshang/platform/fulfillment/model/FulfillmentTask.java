package com.dianshang.platform.fulfillment.model;

import java.time.OffsetDateTime;

public record FulfillmentTask(
        String fulfillmentTaskId,
        String storeId,
        String orderId,
        String idempotencyKey,
        String status,
        Integer retryCount,
        OffsetDateTime dueAt,
        String lastErrorMessage,
        OffsetDateTime createdAt
) {
    public FulfillmentTask withStatus(String status, String lastErrorMessage) {
        return new FulfillmentTask(
                fulfillmentTaskId,
                storeId,
                orderId,
                idempotencyKey,
                status,
                retryCount,
                dueAt,
                lastErrorMessage,
                createdAt
        );
    }

    public FulfillmentTask withRetry(String status) {
        return new FulfillmentTask(
                fulfillmentTaskId,
                storeId,
                orderId,
                idempotencyKey,
                status,
                retryCount == null ? 1 : retryCount + 1,
                dueAt,
                null,
                createdAt
        );
    }
}
