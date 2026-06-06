package com.dianshang.platform.qa.dto;

public record ReplySuggestionView(
        String conversationId,
        String messageId,
        String suggestedReply,
        String sourceType,
        boolean autoSendEligible,
        String matchedQuestion
) {
}
