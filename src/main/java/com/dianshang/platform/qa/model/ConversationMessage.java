package com.dianshang.platform.qa.model;

import java.time.OffsetDateTime;

public record ConversationMessage(
        String messageId,
        String conversationId,
        String senderType,
        String messageType,
        String contentText,
        boolean aiGeneratedFlag,
        boolean riskFlag,
        OffsetDateTime createdAt
) {
}
