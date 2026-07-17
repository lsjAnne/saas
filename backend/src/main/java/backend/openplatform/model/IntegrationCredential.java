package backend.openplatform.model;

import java.time.OffsetDateTime;

public record IntegrationCredential(
        String credentialId,
        String pluginAppId,
        String credentialType,
        String accessKey,
        String secretDigest,
        String secretKeyMasked,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt
) {
}

