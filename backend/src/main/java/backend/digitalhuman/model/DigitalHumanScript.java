package backend.digitalhuman.model;

import java.time.OffsetDateTime;

public record DigitalHumanScript(
        String scriptId,
        String tenantId,
        String organizationId,
        String scriptName,
        String sceneType,
        String contentText,
        String version,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public DigitalHumanScript withEditableFields(String scriptName,
                                                 String sceneType,
                                                 String contentText,
                                                 String version,
                                                 String status) {
        return new DigitalHumanScript(
                scriptId,
                tenantId,
                organizationId,
                scriptName,
                sceneType,
                contentText,
                version,
                status,
                createdAt,
                OffsetDateTime.now()
        );
    }
}
