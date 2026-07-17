package backend.qa.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateFaqKnowledgeRequest(
        @NotBlank(message = "storeId is required")
        String storeId,
        String productId,
        @NotBlank(message = "question is required")
        String question,
        @NotBlank(message = "answer is required")
        String answer,
        @NotBlank(message = "sourceType is required")
        String sourceType,
        Boolean enabled
) {
}

