package com.dianshang.platform.live.model;

import java.time.OffsetDateTime;

public record LivePlan(
        String livePlanId,
        String storeId,
        String liveAccountId,
        String planName,
        String planStatus,
        OffsetDateTime scheduledStartAt,
        OffsetDateTime scheduledEndAt,
        String anchorProfileName,
        OffsetDateTime createdAt
) {
    public LivePlan withStatus(String planStatus) {
        return new LivePlan(
                livePlanId,
                storeId,
                liveAccountId,
                planName,
                planStatus,
                scheduledStartAt,
                scheduledEndAt,
                anchorProfileName,
                createdAt
        );
    }

    public LivePlan withEditableFields(String liveAccountId,
                                       String planName,
                                       OffsetDateTime scheduledStartAt,
                                       OffsetDateTime scheduledEndAt,
                                       String anchorProfileName) {
        return new LivePlan(
                livePlanId,
                storeId,
                liveAccountId,
                planName,
                planStatus,
                scheduledStartAt,
                scheduledEndAt,
                anchorProfileName,
                createdAt
        );
    }
}
