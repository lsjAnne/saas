package backend.openplatform.model;

import java.time.OffsetDateTime;

public record OpenCallbackReplayRecord(
        String subscriptionId,
        String requestId,
        OffsetDateTime createdAt
) {
}

