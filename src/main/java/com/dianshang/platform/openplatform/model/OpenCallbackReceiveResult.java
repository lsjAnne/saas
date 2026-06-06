package com.dianshang.platform.openplatform.model;

public record OpenCallbackReceiveResult(
        String subscriptionId,
        String requestId,
        boolean verified,
        boolean replayed,
        String status
) {
}
