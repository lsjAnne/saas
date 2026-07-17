package backend.openplatform.model;

import java.time.OffsetDateTime;

public record WebhookSubscription(
        String subscriptionId,
        String organizationId,
        String eventCode,
        String callbackUrl,
        String secretToken,
        String status,
        OffsetDateTime createdAt
) {
}

