package com.dianshang.platform.fulfillment.model;

import java.time.OffsetDateTime;

public record LogisticsRecord(
        String logisticsRecordId,
        String fulfillmentTaskId,
        String trackingNumber,
        String logisticsCompany,
        String logisticsStatus,
        OffsetDateTime syncedAt,
        OffsetDateTime createdAt
) {
}
