package com.dianshang.platform.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record FinanceBill(
        String financeBillId,
        String storeId,
        String billType,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal incomeAmount,
        BigDecimal costAmount,
        BigDecimal grossProfit,
        String billStatus,
        OffsetDateTime createdAt
) {
    public FinanceBill withStatus(String billStatus) {
        return new FinanceBill(
                financeBillId,
                storeId,
                billType,
                periodStart,
                periodEnd,
                incomeAmount,
                costAmount,
                grossProfit,
                billStatus,
                createdAt
        );
    }
}
