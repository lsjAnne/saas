package com.dianshang.platform.saas.model;

import java.time.OffsetDateTime;

public record InvoiceRequest(
        String invoiceRequestId,
        String tenantId,
        String billingOrderId,
        String invoiceTitle,
        String invoiceTaxNo,
        String invoiceStatus,
        OffsetDateTime createdAt
) {
}
