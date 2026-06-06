package com.dianshang.platform.finance.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SettlementRecord(
        String settlementRecordId,
        String financeBillId,
        String settlementType,
        BigDecimal settlementAmount,
        String settlementStatus,
        OffsetDateTime settledAt,
        OffsetDateTime createdAt
) {
}
