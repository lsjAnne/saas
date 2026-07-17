package backend.qa.dto;

import java.util.List;

public record ReplySuggestionView(
        String conversationId,
        String messageId,
        String suggestedReply,
        String sourceType,
        boolean autoSendEligible,
        String matchedQuestion,
        String confidenceLevel,
        String handoffReason,
        List<String> riskLabels,
        String recommendedAction,
        String knowledgeSourceSummary
) {
}

