package com.dianshang.platform.inventory.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.inventory.application.InventoryService;
import com.dianshang.platform.inventory.dto.CreateReplenishmentTaskRequest;
import com.dianshang.platform.inventory.dto.UpdateInventorySafetyStockRequest;
import com.dianshang.platform.inventory.model.InventorySnapshot;
import com.dianshang.platform.inventory.model.ReplenishmentTask;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.INVENTORY_MANAGE)
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/api/inventory-snapshots")
    public ApiResponse<List<InventorySnapshot>> listInventorySnapshots() {
        return ApiResponse.success(
                inventoryService.listInventorySnapshots(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/inventory-ledgers")
    public ApiResponse<List<InventoryService.InventoryLedger>> listInventoryLedgers() {
        return ApiResponse.success(
                inventoryService.listInventoryLedgers(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/inventory-transactions")
    public ApiResponse<List<InventoryService.InventoryTransactionRecord>> listInventoryTransactions() {
        return ApiResponse.success(
                inventoryService.listInventoryTransactions(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/inventory-transfers")
    public ApiResponse<List<InventoryService.InventoryTransferRecord>> listInventoryTransfers() {
        return ApiResponse.success(
                inventoryService.listInventoryTransfers(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/inventory-freezes")
    public ApiResponse<List<InventoryService.InventoryFreezeRecord>> listInventoryFreezes() {
        return ApiResponse.success(
                inventoryService.listInventoryFreezes(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/inventory-batches")
    public ApiResponse<List<InventoryService.InventoryBatch>> listInventoryBatches() {
        return ApiResponse.success(
                inventoryService.listInventoryBatches(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/inventory-cost-lots")
    public ApiResponse<List<InventoryService.InventoryCostLot>> listInventoryCostLots() {
        return ApiResponse.success(
                inventoryService.listInventoryCostLots(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/payable-ledgers")
    public ApiResponse<List<InventoryService.PayableLedgerEntry>> listPayableLedgers() {
        return ApiResponse.success(
                inventoryService.listPayableLedgers(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/supplier-reconciliations")
    public ApiResponse<List<InventoryService.SupplierReconciliationView>> listSupplierReconciliations() {
        return ApiResponse.success(
                inventoryService.listSupplierReconciliations(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/purchase-expense-allocations")
    public ApiResponse<List<InventoryService.PurchaseExpenseAllocation>> listPurchaseExpenseAllocations() {
        return ApiResponse.success(
                inventoryService.listPurchaseExpenseAllocations(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/purchase-cost-collections")
    public ApiResponse<List<InventoryService.PurchaseCostCollectionView>> listPurchaseCostCollections() {
        return ApiResponse.success(
                inventoryService.listPurchaseCostCollections(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/inventory-snapshots/{id}/safety-stock")
    public ApiResponse<InventorySnapshot> updateSafetyStock(@PathVariable String id,
                                                            @Valid @RequestBody UpdateInventorySafetyStockRequest request) {
        return ApiResponse.success(
                inventoryService.updateSafetyStock(TenantAccessSupport.requiredTenantId(), id, request.safetyStock()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/replenishment-tasks")
    public ApiResponse<List<ReplenishmentTask>> listReplenishmentTasks() {
        return ApiResponse.success(
                inventoryService.listReplenishmentTasks(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/replenishment-tasks")
    public ApiResponse<ReplenishmentTask> createReplenishmentTask(@Valid @RequestBody CreateReplenishmentTaskRequest request) {
        return ApiResponse.success(
                inventoryService.createReplenishmentTask(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/replenishment-tasks/{id}/submit-approval")
    public ApiResponse<ReplenishmentTask> submitApproval(@PathVariable String id) {
        return ApiResponse.success(
                inventoryService.submitApproval(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/purchase-requests")
    public ApiResponse<List<InventoryService.PurchaseRequest>> listPurchaseRequests() {
        return ApiResponse.success(
                inventoryService.listPurchaseRequests(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/purchase-requests")
    public ApiResponse<InventoryService.PurchaseRequest> createPurchaseRequest(
            @Valid @RequestBody CreatePurchaseRequestRequest request
    ) {
        return ApiResponse.success(
                inventoryService.createPurchaseRequest(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/purchase-requests/{id}")
    public ApiResponse<InventoryService.PurchaseRequest> getPurchaseRequest(@PathVariable String id) {
        return ApiResponse.success(
                inventoryService.getPurchaseRequest(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/purchase-requests/{id}/submit")
    public ApiResponse<InventoryService.PurchaseRequest> submitPurchaseRequest(@PathVariable String id) {
        return ApiResponse.success(
                inventoryService.submitPurchaseRequest(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/purchase-orders")
    public ApiResponse<List<InventoryService.PurchaseOrder>> listPurchaseOrders() {
        return ApiResponse.success(
                inventoryService.listPurchaseOrders(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/purchase-orders")
    public ApiResponse<InventoryService.PurchaseOrder> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request
    ) {
        return ApiResponse.success(
                inventoryService.createPurchaseOrder(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/purchase-orders/{id}")
    public ApiResponse<InventoryService.PurchaseOrder> getPurchaseOrder(@PathVariable String id) {
        return ApiResponse.success(
                inventoryService.getPurchaseOrder(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/purchase-orders/{id}/submit-approval")
    public ApiResponse<InventoryService.PurchaseOrder> submitPurchaseOrderApproval(@PathVariable String id) {
        return ApiResponse.success(
                inventoryService.submitPurchaseOrderApproval(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/purchase-orders/{id}/dispatch")
    public ApiResponse<InventoryService.PurchaseOrder> dispatchPurchaseOrder(@PathVariable String id) {
        return ApiResponse.success(
                inventoryService.dispatchPurchaseOrder(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/purchase-receipts")
    public ApiResponse<List<InventoryService.PurchaseReceipt>> listPurchaseReceipts() {
        return ApiResponse.success(
                inventoryService.listPurchaseReceipts(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/purchase-receipt-discrepancies")
    public ApiResponse<List<InventoryService.PurchaseReceiptDiscrepancy>> listPurchaseReceiptDiscrepancies() {
        return ApiResponse.success(
                inventoryService.listPurchaseReceiptDiscrepancies(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/purchase-returns")
    public ApiResponse<List<InventoryService.PurchaseReturnRecord>> listPurchaseReturns() {
        return ApiResponse.success(
                inventoryService.listPurchaseReturns(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/purchase-orders/{id}/receive")
    public ApiResponse<InventoryService.PurchaseReceipt> receivePurchaseOrder(
            @PathVariable String id,
            @Valid @RequestBody ReceivePurchaseOrderRequest request
    ) {
        return ApiResponse.success(
                inventoryService.receivePurchaseOrder(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id,
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/purchase-orders/{id}/returns")
    public ApiResponse<InventoryService.PurchaseReturnRecord> createPurchaseReturn(
            @PathVariable String id,
            @Valid @RequestBody CreatePurchaseReturnRequest request
    ) {
        return ApiResponse.success(
                inventoryService.createPurchaseReturn(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id,
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/inventory-transfers")
    public ApiResponse<InventoryService.InventoryTransferRecord> createInventoryTransfer(
            @Valid @RequestBody CreateInventoryTransferRequest request
    ) {
        return ApiResponse.success(
                inventoryService.createInventoryTransfer(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/inventory-transfers/{id}/complete")
    public ApiResponse<InventoryService.InventoryTransferRecord> completeInventoryTransfer(@PathVariable String id) {
        return ApiResponse.success(
                inventoryService.completeInventoryTransfer(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/purchase-expense-allocations")
    public ApiResponse<InventoryService.PurchaseExpenseAllocation> createPurchaseExpenseAllocation(
            @Valid @RequestBody CreatePurchaseExpenseAllocationRequest request
    ) {
        return ApiResponse.success(
                inventoryService.createPurchaseExpenseAllocation(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/wms/warehouses")
    public ApiResponse<InventoryService.WmsWarehouse> createWmsWarehouse(
            @Valid @RequestBody CreateWmsWarehouseRequest request
    ) {
        return ApiResponse.success(
                inventoryService.createWmsWarehouse(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/wms/inbound-tasks")
    public ApiResponse<InventoryService.WmsInboundTask> createWmsInboundTask(
            @Valid @RequestBody CreateWmsInboundTaskRequest request
    ) {
        return ApiResponse.success(
                inventoryService.createWmsInboundTask(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/wms/waves")
    public ApiResponse<InventoryService.WmsWave> createWmsWave(
            @Valid @RequestBody CreateWmsWaveRequest request
    ) {
        return ApiResponse.success(
                inventoryService.createWmsWave(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/wms/locks")
    public ApiResponse<InventoryService.WmsLockRecord> createWmsLock(
            @Valid @RequestBody CreateWmsLockRequest request
    ) {
        return ApiResponse.success(
                inventoryService.createWmsLock(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/wms/cycle-count-tasks")
    public ApiResponse<InventoryService.WmsCycleCountTask> createWmsCycleCountTask(
            @Valid @RequestBody CreateWmsCycleCountTaskRequest request
    ) {
        return ApiResponse.success(
                inventoryService.createWmsCycleCountTask(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/wms/reverse-inbounds")
    public ApiResponse<InventoryService.WmsReverseInboundRecord> createWmsReverseInbound(
            @Valid @RequestBody CreateWmsReverseInboundRequest request
    ) {
        return ApiResponse.success(
                inventoryService.createWmsReverseInbound(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/wms/linkage")
    public ApiResponse<InventoryService.WmsLinkageView> getWmsLinkage(@RequestParam String storeId) {
        return ApiResponse.success(
                inventoryService.getWmsLinkage(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    public record CreatePurchaseRequestRequest(
            @NotBlank(message = "storeId is required")
            String storeId,
            @NotBlank(message = "supplierId is required")
            String supplierId,
            @NotNull(message = "expectedDeliveryDate is required")
            LocalDate expectedDeliveryDate,
            @NotBlank(message = "reasonText is required")
            String reasonText,
            @NotEmpty(message = "items is required")
            List<@Valid PurchaseRequestItemPayload> items
    ) {
        InventoryService.CreatePurchaseRequestCommand toCommand() {
            return new InventoryService.CreatePurchaseRequestCommand(
                    storeId,
                    supplierId,
                    expectedDeliveryDate,
                    reasonText,
                    items.stream()
                            .map(item -> new InventoryService.PurchaseRequestItemCommand(
                                    item.productId(),
                                    item.skuId(),
                                    item.requestedQty(),
                                    item.targetUnitPrice()
                            ))
                            .toList()
            );
        }
    }

    public record PurchaseRequestItemPayload(
            @NotBlank(message = "productId is required")
            String productId,
            @NotBlank(message = "skuId is required")
            String skuId,
            @NotNull(message = "requestedQty is required")
            @Min(value = 1, message = "requestedQty must be greater than 0")
            Integer requestedQty,
            @NotNull(message = "targetUnitPrice is required")
            @DecimalMin(value = "0.01", message = "targetUnitPrice must be greater than 0")
            BigDecimal targetUnitPrice
    ) {
    }

    public record CreatePurchaseOrderRequest(
            @NotBlank(message = "purchaseRequestId is required")
            String purchaseRequestId,
            String remark
    ) {
        InventoryService.CreatePurchaseOrderCommand toCommand() {
            return new InventoryService.CreatePurchaseOrderCommand(purchaseRequestId, remark);
        }
    }

    public record ReceivePurchaseOrderRequest(
            @NotEmpty(message = "items is required")
            List<@Valid ReceivePurchaseOrderItemPayload> items,
            String remark
    ) {
        InventoryService.ReceivePurchaseOrderCommand toCommand() {
            return new InventoryService.ReceivePurchaseOrderCommand(
                    items.stream()
                            .map(item -> new InventoryService.ReceivePurchaseOrderItemCommand(
                                    item.productId(),
                                    item.skuId(),
                                    item.receivedQty(),
                                    item.batchNo(),
                                    item.productionDate(),
                                    item.expiryDate()
                            ))
                            .toList(),
                    remark
            );
        }
    }

    public record ReceivePurchaseOrderItemPayload(
            @NotBlank(message = "productId is required")
            String productId,
            @NotBlank(message = "skuId is required")
            String skuId,
            @NotNull(message = "receivedQty is required")
            @Min(value = 1, message = "receivedQty must be greater than 0")
            Integer receivedQty,
            String batchNo,
            LocalDate productionDate,
            LocalDate expiryDate
    ) {
    }

    public record CreatePurchaseReturnRequest(
            @NotEmpty(message = "items is required")
            List<@Valid CreatePurchaseReturnItemPayload> items,
            String remark
    ) {
        InventoryService.CreatePurchaseReturnCommand toCommand() {
            return new InventoryService.CreatePurchaseReturnCommand(
                    items.stream()
                            .map(item -> new InventoryService.CreatePurchaseReturnItemCommand(
                                    item.productId(),
                                    item.skuId(),
                                    item.batchNo(),
                                    item.returnQty(),
                                    item.reasonText()
                            ))
                            .toList(),
                    remark
            );
        }
    }

    public record CreatePurchaseReturnItemPayload(
            @NotBlank(message = "productId is required")
            String productId,
            @NotBlank(message = "skuId is required")
            String skuId,
            String batchNo,
            @NotNull(message = "returnQty is required")
            @Min(value = 1, message = "returnQty must be greater than 0")
            Integer returnQty,
            @NotBlank(message = "reasonText is required")
            String reasonText
    ) {
    }

    public record CreateInventoryTransferRequest(
            @NotBlank(message = "sourceStoreId is required")
            String sourceStoreId,
            @NotBlank(message = "targetStoreId is required")
            String targetStoreId,
            @NotBlank(message = "productId is required")
            String productId,
            @NotBlank(message = "skuId is required")
            String skuId,
            String batchNo,
            @NotNull(message = "transferQty is required")
            @Min(value = 1, message = "transferQty must be greater than 0")
            Integer transferQty,
            @NotBlank(message = "reasonText is required")
            String reasonText
    ) {
        InventoryService.CreateInventoryTransferCommand toCommand() {
            return new InventoryService.CreateInventoryTransferCommand(
                    sourceStoreId,
                    targetStoreId,
                    productId,
                    skuId,
                    batchNo,
                    transferQty,
                    reasonText
            );
        }
    }

    public record CreatePurchaseExpenseAllocationRequest(
            @NotBlank(message = "purchaseOrderId is required")
            String purchaseOrderId,
            @NotBlank(message = "expenseType is required")
            String expenseType,
            @NotBlank(message = "allocationRule is required")
            String allocationRule,
            @NotNull(message = "expenseAmount is required")
            @DecimalMin(value = "0.01", message = "expenseAmount must be greater than 0")
            BigDecimal expenseAmount,
            String remark
    ) {
        InventoryService.CreatePurchaseExpenseAllocationCommand toCommand() {
            return new InventoryService.CreatePurchaseExpenseAllocationCommand(
                    purchaseOrderId,
                    expenseType,
                    allocationRule,
                    expenseAmount,
                    remark
            );
        }
    }

    public record CreateWmsWarehouseRequest(
            @NotBlank(message = "storeId is required")
            String storeId,
            @NotBlank(message = "warehouseName is required")
            String warehouseName,
            @NotBlank(message = "zoneCode is required")
            String zoneCode,
            @NotBlank(message = "locationCode is required")
            String locationCode,
            @NotBlank(message = "temperatureZone is required")
            String temperatureZone
    ) {
        InventoryService.CreateWmsWarehouseCommand toCommand() {
            return new InventoryService.CreateWmsWarehouseCommand(
                    storeId,
                    warehouseName,
                    zoneCode,
                    locationCode,
                    temperatureZone
            );
        }
    }

    public record CreateWmsInboundTaskRequest(
            @NotBlank(message = "storeId is required")
            String storeId,
            @NotBlank(message = "productId is required")
            String productId,
            @NotBlank(message = "skuId is required")
            String skuId,
            @NotBlank(message = "batchNo is required")
            String batchNo,
            @NotBlank(message = "warehouseCode is required")
            String warehouseCode,
            @NotBlank(message = "zoneCode is required")
            String zoneCode,
            @NotBlank(message = "locationCode is required")
            String locationCode,
            @NotBlank(message = "taskType is required")
            String taskType,
            @NotNull(message = "quantity is required")
            @Min(value = 1, message = "quantity must be greater than 0")
            Integer quantity
    ) {
        InventoryService.CreateWmsInboundTaskCommand toCommand() {
            return new InventoryService.CreateWmsInboundTaskCommand(
                    storeId,
                    productId,
                    skuId,
                    batchNo,
                    warehouseCode,
                    zoneCode,
                    locationCode,
                    taskType,
                    quantity
            );
        }
    }

    public record CreateWmsWaveRequest(
            @NotBlank(message = "storeId is required")
            String storeId,
            @NotBlank(message = "warehouseCode is required")
            String warehouseCode,
            @NotBlank(message = "waveType is required")
            String waveType,
            @NotBlank(message = "strategyCode is required")
            String strategyCode,
            @NotEmpty(message = "items is required")
            List<@Valid WmsWaveItemPayload> items
    ) {
        InventoryService.CreateWmsWaveCommand toCommand() {
            return new InventoryService.CreateWmsWaveCommand(
                    storeId,
                    warehouseCode,
                    waveType,
                    strategyCode,
                    items.stream()
                            .map(item -> new InventoryService.WmsWaveItemCommand(
                                    item.productId(),
                                    item.skuId(),
                                    item.locationCode(),
                                    item.plannedQty()
                            ))
                            .toList()
            );
        }
    }

    public record WmsWaveItemPayload(
            @NotBlank(message = "productId is required")
            String productId,
            @NotBlank(message = "skuId is required")
            String skuId,
            @NotBlank(message = "locationCode is required")
            String locationCode,
            @NotNull(message = "plannedQty is required")
            @Min(value = 1, message = "plannedQty must be greater than 0")
            Integer plannedQty
    ) {
    }

    public record CreateWmsLockRequest(
            @NotBlank(message = "storeId is required")
            String storeId,
            @NotBlank(message = "productId is required")
            String productId,
            @NotBlank(message = "skuId is required")
            String skuId,
            @NotBlank(message = "batchNo is required")
            String batchNo,
            @NotBlank(message = "warehouseCode is required")
            String warehouseCode,
            @NotBlank(message = "locationCode is required")
            String locationCode,
            @NotNull(message = "lockQty is required")
            @Min(value = 1, message = "lockQty must be greater than 0")
            Integer lockQty,
            @NotBlank(message = "reasonText is required")
            String reasonText
    ) {
        InventoryService.CreateWmsLockCommand toCommand() {
            return new InventoryService.CreateWmsLockCommand(
                    storeId,
                    productId,
                    skuId,
                    batchNo,
                    warehouseCode,
                    locationCode,
                    lockQty,
                    reasonText
            );
        }
    }

    public record CreateWmsCycleCountTaskRequest(
            @NotBlank(message = "storeId is required")
            String storeId,
            @NotBlank(message = "productId is required")
            String productId,
            @NotBlank(message = "skuId is required")
            String skuId,
            @NotBlank(message = "warehouseCode is required")
            String warehouseCode,
            @NotBlank(message = "locationCode is required")
            String locationCode,
            @NotNull(message = "systemQty is required")
            @Min(value = 0, message = "systemQty must be greater than or equal to 0")
            Integer systemQty,
            @NotNull(message = "countedQty is required")
            @Min(value = 0, message = "countedQty must be greater than or equal to 0")
            Integer countedQty,
            @NotBlank(message = "varianceReason is required")
            String varianceReason
    ) {
        InventoryService.CreateWmsCycleCountTaskCommand toCommand() {
            return new InventoryService.CreateWmsCycleCountTaskCommand(
                    storeId,
                    productId,
                    skuId,
                    warehouseCode,
                    locationCode,
                    systemQty,
                    countedQty,
                    varianceReason
            );
        }
    }

    public record CreateWmsReverseInboundRequest(
            @NotBlank(message = "storeId is required")
            String storeId,
            @NotBlank(message = "productId is required")
            String productId,
            @NotBlank(message = "skuId is required")
            String skuId,
            @NotBlank(message = "warehouseCode is required")
            String warehouseCode,
            @NotBlank(message = "locationCode is required")
            String locationCode,
            @NotBlank(message = "reverseType is required")
            String reverseType,
            @NotNull(message = "quantity is required")
            @Min(value = 1, message = "quantity must be greater than 0")
            Integer quantity,
            String remark
    ) {
        InventoryService.CreateWmsReverseInboundCommand toCommand() {
            return new InventoryService.CreateWmsReverseInboundCommand(
                    storeId,
                    productId,
                    skuId,
                    warehouseCode,
                    locationCode,
                    reverseType,
                    quantity,
                    remark
            );
        }
    }
}
