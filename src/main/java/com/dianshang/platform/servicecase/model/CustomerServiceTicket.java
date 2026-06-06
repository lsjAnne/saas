package com.dianshang.platform.servicecase.model;

import java.time.OffsetDateTime;

public record CustomerServiceTicket(
        String ticketId,
        String storeId,
        String orderId,
        String customerId,
        String ticketStatus,
        boolean riskFlag,
        String aiReplySuggestion,
        OffsetDateTime createdAt
) {
    public CustomerServiceTicket withSuggestion(String ticketStatus, String aiReplySuggestion) {
        return new CustomerServiceTicket(
                ticketId,
                storeId,
                orderId,
                customerId,
                ticketStatus,
                riskFlag,
                aiReplySuggestion,
                createdAt
        );
    }
}
