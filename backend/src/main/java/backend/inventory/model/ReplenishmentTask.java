package backend.inventory.model;

import java.time.OffsetDateTime;

public record ReplenishmentTask(
        String replenishmentTaskId,
        String storeId,
        String productId,
        String skuId,
        int suggestedQty,
        String taskStatus,
        String approvalStatus,
        String reasonText,
        OffsetDateTime createdAt
) {
    public ReplenishmentTask withWorkflow(String taskStatus, String approvalStatus) {
        return new ReplenishmentTask(
                replenishmentTaskId,
                storeId,
                productId,
                skuId,
                suggestedQty,
                taskStatus,
                approvalStatus,
                reasonText,
                createdAt
        );
    }
}

