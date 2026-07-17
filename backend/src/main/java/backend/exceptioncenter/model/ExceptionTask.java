package backend.exceptioncenter.model;

import java.time.OffsetDateTime;

public record ExceptionTask(
        String exceptionTaskId,
        String storeId,
        String relatedType,
        String relatedId,
        String exceptionType,
        String severity,
        String status,
        String suggestionText,
        String ownerUserId,
        OffsetDateTime createdAt
) {
    public ExceptionTask withStatus(String status, String ownerUserId, String suggestionText) {
        return new ExceptionTask(
                exceptionTaskId,
                storeId,
                relatedType,
                relatedId,
                exceptionType,
                severity,
                status,
                suggestionText,
                ownerUserId,
                createdAt
        );
    }
}

