package com.dianshang.platform.qa.model;

import java.time.OffsetDateTime;

public record CustomerConversation(
        String conversationId,
        String storeId,
        String platformType,
        String platformConversationId,
        String customerId,
        String conversationStatus,
        boolean riskFlag,
        OffsetDateTime lastMessageAt,
        OffsetDateTime createdAt
) {
    public CustomerConversation withStatus(String conversationStatus, OffsetDateTime lastMessageAt) {
        return withStatusAndRisk(conversationStatus, riskFlag, lastMessageAt);
    }

    public CustomerConversation withStatusAndRisk(String conversationStatus, boolean riskFlag, OffsetDateTime lastMessageAt) {
        return new CustomerConversation(
                conversationId,
                storeId,
                platformType,
                platformConversationId,
                customerId,
                conversationStatus,
                riskFlag,
                lastMessageAt,
                createdAt
        );
    }
}
