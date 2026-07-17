package backend.order.dto;

public record OrderSyncResult(
        String taskId,
        String storeId,
        String orderId,
        String fulfillmentTaskId,
        boolean reused
) {
}

