package backend.servicecase.model;

import java.time.OffsetDateTime;

public record AfterSaleRecord(
        String afterSaleId,
        String orderId,
        String afterSaleType,
        String reasonText,
        String status,
        String evidenceBlob,
        OffsetDateTime createdAt
) {
    public AfterSaleRecord withStatus(String status) {
        return new AfterSaleRecord(
                afterSaleId,
                orderId,
                afterSaleType,
                reasonText,
                status,
                evidenceBlob,
                createdAt
        );
    }
}

