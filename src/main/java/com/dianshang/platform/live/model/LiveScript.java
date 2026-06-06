package com.dianshang.platform.live.model;

import java.time.OffsetDateTime;

public record LiveScript(
        String liveScriptId,
        String livePlanId,
        String productId,
        String scriptVersion,
        String scriptContent,
        boolean active,
        OffsetDateTime createdAt
) {
    public LiveScript withActive(boolean active) {
        return new LiveScript(
                liveScriptId,
                livePlanId,
                productId,
                scriptVersion,
                scriptContent,
                active,
                createdAt
        );
    }
}
