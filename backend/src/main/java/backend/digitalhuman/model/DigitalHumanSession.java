package backend.digitalhuman.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DigitalHumanSession(
        String sessionId,
        String tenantId,
        String organizationId,
        String scheduleId,
        String avatarId,
        String platformRoomId,
        String liveStatus,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        long viewerPeak,
        long orderCount,
        BigDecimal gmvAmount,
        OffsetDateTime heartbeatAt,
        boolean mockFlag,
        OffsetDateTime createdAt
) {
    public DigitalHumanSession withLiveState(String liveStatus,
                                             OffsetDateTime startedAt,
                                             OffsetDateTime endedAt,
                                             OffsetDateTime heartbeatAt) {
        return new DigitalHumanSession(
                sessionId,
                tenantId,
                organizationId,
                scheduleId,
                avatarId,
                platformRoomId,
                liveStatus,
                startedAt,
                endedAt,
                viewerPeak,
                orderCount,
                gmvAmount,
                heartbeatAt,
                mockFlag,
                createdAt
        );
    }

    public DigitalHumanSession withMetrics(long viewerPeak, long orderCount, BigDecimal gmvAmount) {
        return new DigitalHumanSession(
                sessionId,
                tenantId,
                organizationId,
                scheduleId,
                avatarId,
                platformRoomId,
                liveStatus,
                startedAt,
                endedAt,
                viewerPeak,
                orderCount,
                gmvAmount,
                heartbeatAt,
                mockFlag,
                createdAt
        );
    }
}
