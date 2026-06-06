package com.dianshang.platform.live.model;

import java.time.OffsetDateTime;

public record LiveSession(
        String liveSessionId,
        String livePlanId,
        String tenantId,
        String storeId,
        String liveAccountId,
        String sessionStatus,
        String roomId,
        OffsetDateTime actualStartAt,
        OffsetDateTime actualEndAt,
        String errorMessage,
        String controlMode,
        String currentScene,
        String takeoverStatus,
        String takeoverOperator,
        String promiseAuditStatus,
        String promiseAuditRemark,
        OffsetDateTime createdAt
) {
    public LiveSession withStatus(String sessionStatus, OffsetDateTime actualEndAt, String errorMessage) {
        return new LiveSession(
                liveSessionId,
                livePlanId,
                tenantId,
                storeId,
                liveAccountId,
                sessionStatus,
                roomId,
                actualStartAt,
                actualEndAt,
                errorMessage,
                controlMode,
                currentScene,
                takeoverStatus,
                takeoverOperator,
                promiseAuditStatus,
                promiseAuditRemark,
                createdAt
        );
    }

    public LiveSession withGovernance(String controlMode,
                                      String currentScene,
                                      String takeoverStatus,
                                      String takeoverOperator,
                                      String promiseAuditStatus,
                                      String promiseAuditRemark) {
        return new LiveSession(
                liveSessionId,
                livePlanId,
                tenantId,
                storeId,
                liveAccountId,
                sessionStatus,
                roomId,
                actualStartAt,
                actualEndAt,
                errorMessage,
                controlMode,
                currentScene,
                takeoverStatus,
                takeoverOperator,
                promiseAuditStatus,
                promiseAuditRemark,
                createdAt
        );
    }
}
