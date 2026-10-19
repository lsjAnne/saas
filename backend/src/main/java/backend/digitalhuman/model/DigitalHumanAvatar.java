package backend.digitalhuman.model;

import java.time.OffsetDateTime;

public record DigitalHumanAvatar(
        String avatarId,
        String tenantId,
        String organizationId,
        String avatarName,
        String avatarType,
        String voiceName,
        String previewUrl,
        boolean enabled,
        OffsetDateTime createdAt
) {
    public DigitalHumanAvatar withEditableFields(String avatarName,
                                                 String avatarType,
                                                 String voiceName,
                                                 String previewUrl,
                                                 boolean enabled) {
        return new DigitalHumanAvatar(
                avatarId,
                tenantId,
                organizationId,
                avatarName,
                avatarType,
                voiceName,
                previewUrl,
                enabled,
                createdAt
        );
    }
}
