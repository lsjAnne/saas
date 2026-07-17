package backend.openplatform.model;

public record OpenCallbackReceiveResult(
        String subscriptionId,
        String requestId,
        boolean verified,
        boolean replayed,
        String status
) {
}

