package backend.digitalhuman.model;

import java.time.OffsetDateTime;

public record DigitalHumanSchedule(
        String scheduleId,
        String tenantId,
        String organizationId,
        String scheduleName,
        String avatarId,
        String scriptId,
        String platformType,
        String repeatType,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        boolean enabled,
        String status,
        OffsetDateTime createdAt
) {
    public DigitalHumanSchedule withStatus(String status) {
        return new DigitalHumanSchedule(
                scheduleId,
                tenantId,
                organizationId,
                scheduleName,
                avatarId,
                scriptId,
                platformType,
                repeatType,
                startAt,
                endAt,
                enabled,
                status,
                createdAt
        );
    }

    public DigitalHumanSchedule withEditableFields(String scheduleName,
                                                  String avatarId,
                                                  String scriptId,
                                                  String platformType,
                                                  String repeatType,
                                                  OffsetDateTime startAt,
                                                  OffsetDateTime endAt,
                                                  boolean enabled,
                                                  String status) {
        return new DigitalHumanSchedule(
                scheduleId,
                tenantId,
                organizationId,
                scheduleName,
                avatarId,
                scriptId,
                platformType,
                repeatType,
                startAt,
                endAt,
                enabled,
                status,
                createdAt
        );
    }
}
