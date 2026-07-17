package backend.inventory.model;

import java.time.OffsetDateTime;

public record InventorySnapshot(
        String inventorySnapshotId,
        String storeId,
        String productId,
        String skuId,
        int availableStock,
        int reservedStock,
        int safetyStock,
        OffsetDateTime snapshotAt,
        OffsetDateTime createdAt
) {
    public InventorySnapshot withSafetyStock(int safetyStock) {
        return new InventorySnapshot(
                inventorySnapshotId,
                storeId,
                productId,
                skuId,
                availableStock,
                reservedStock,
                safetyStock,
                snapshotAt,
                createdAt
        );
    }

    public InventorySnapshot withInbound(int inboundQty, OffsetDateTime snapshotAt) {
        return new InventorySnapshot(
                inventorySnapshotId,
                storeId,
                productId,
                skuId,
                availableStock + inboundQty,
                reservedStock,
                safetyStock,
                snapshotAt,
                createdAt
        );
    }

    public InventorySnapshot withFreeze(int freezeQty, OffsetDateTime snapshotAt) {
        return new InventorySnapshot(
                inventorySnapshotId,
                storeId,
                productId,
                skuId,
                availableStock - freezeQty,
                reservedStock + freezeQty,
                safetyStock,
                snapshotAt,
                createdAt
        );
    }

    public InventorySnapshot withRelease(int releaseQty, OffsetDateTime snapshotAt) {
        return new InventorySnapshot(
                inventorySnapshotId,
                storeId,
                productId,
                skuId,
                availableStock + releaseQty,
                reservedStock - releaseQty,
                safetyStock,
                snapshotAt,
                createdAt
        );
    }

    public InventorySnapshot withReservedTransfer(int transferQty, OffsetDateTime snapshotAt) {
        return new InventorySnapshot(
                inventorySnapshotId,
                storeId,
                productId,
                skuId,
                availableStock,
                reservedStock - transferQty,
                safetyStock,
                snapshotAt,
                createdAt
        );
    }

    public InventorySnapshot withOutbound(int outboundQty, OffsetDateTime snapshotAt) {
        return new InventorySnapshot(
                inventorySnapshotId,
                storeId,
                productId,
                skuId,
                availableStock - outboundQty,
                reservedStock,
                safetyStock,
                snapshotAt,
                createdAt
        );
    }
}

