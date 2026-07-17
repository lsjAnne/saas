package backend.inventory.application;

import backend.audit.application.AuditLogService;
import backend.approval.application.ApprovalService;
import backend.common.exception.BusinessException;
import backend.inventory.domain.repository.InventorySnapshotRepository;
import backend.inventory.domain.repository.ReplenishmentTaskRepository;
import backend.inventory.dto.CreateReplenishmentTaskRequest;
import backend.inventory.model.InventorySnapshot;
import backend.inventory.model.ReplenishmentTask;
import backend.product.domain.repository.ProductRepository;
import backend.product.model.Product;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import backend.supplier.domain.repository.SupplierRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InventoryService {

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final ReplenishmentTaskRepository replenishmentTaskRepository;
    private final ApprovalService approvalService;
    private final Map<String, PurchaseRequest> purchaseRequestStorage = new ConcurrentHashMap<>();
    private final AtomicLong purchaseRequestSequence = new AtomicLong(12000);
    private static final Map<String, PurchaseOrder> PURCHASE_ORDER_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong PURCHASE_ORDER_SEQUENCE = new AtomicLong(13000);
    private static final Map<String, PurchaseReceipt> PURCHASE_RECEIPT_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong PURCHASE_RECEIPT_SEQUENCE = new AtomicLong(14000);
    private static final Map<String, PurchaseReceiptDiscrepancy> PURCHASE_DISCREPANCY_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong PURCHASE_DISCREPANCY_SEQUENCE = new AtomicLong(15000);
    private static final Map<String, PurchaseReturnRecord> PURCHASE_RETURN_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong PURCHASE_RETURN_SEQUENCE = new AtomicLong(16000);
    private static final Map<String, InventoryTransactionRecord> INVENTORY_TRANSACTION_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong INVENTORY_TRANSACTION_SEQUENCE = new AtomicLong(17000);
    private static final Map<String, InventoryTransferRecord> INVENTORY_TRANSFER_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong INVENTORY_TRANSFER_SEQUENCE = new AtomicLong(18000);
    private static final Map<String, InventoryFreezeRecord> INVENTORY_FREEZE_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong INVENTORY_FREEZE_SEQUENCE = new AtomicLong(19000);
    private static final Map<String, InventoryBatch> INVENTORY_BATCH_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong INVENTORY_BATCH_SEQUENCE = new AtomicLong(20000);
    private static final Map<String, InventoryCostLot> INVENTORY_COST_LOT_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong INVENTORY_COST_LOT_SEQUENCE = new AtomicLong(21000);
    private static final Map<String, PayableLedgerEntry> PAYABLE_LEDGER_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong PAYABLE_LEDGER_SEQUENCE = new AtomicLong(22000);
    private static final Map<String, PurchaseExpenseAllocation> PURCHASE_EXPENSE_ALLOCATION_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong PURCHASE_EXPENSE_ALLOCATION_SEQUENCE = new AtomicLong(23000);
    private static final Map<String, WmsWarehouse> WMS_WAREHOUSE_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong WMS_WAREHOUSE_SEQUENCE = new AtomicLong();
    private static final Map<String, WmsInboundTask> WMS_INBOUND_TASK_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong WMS_INBOUND_TASK_SEQUENCE = new AtomicLong();
    private static final Map<String, WmsWave> WMS_WAVE_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong WMS_WAVE_SEQUENCE = new AtomicLong();
    private static final Map<String, WmsLockRecord> WMS_LOCK_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong WMS_LOCK_SEQUENCE = new AtomicLong();
    private static final Map<String, WmsCycleCountTask> WMS_CYCLE_COUNT_TASK_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong WMS_CYCLE_COUNT_TASK_SEQUENCE = new AtomicLong();
    private static final Map<String, WmsReverseInboundRecord> WMS_REVERSE_INBOUND_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong WMS_REVERSE_INBOUND_SEQUENCE = new AtomicLong();

    public InventoryService(AuditLogService auditLogService,
                            StoreRepository storeRepository,
                            ProductRepository productRepository,
                            SupplierRepository supplierRepository,
                            InventorySnapshotRepository inventorySnapshotRepository,
                            ReplenishmentTaskRepository replenishmentTaskRepository,
                            ApprovalService approvalService) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.replenishmentTaskRepository = replenishmentTaskRepository;
        this.approvalService = approvalService;
    }

    public List<InventorySnapshot> listInventorySnapshots(String tenantId) {
        return inventorySnapshotRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public InventorySnapshot updateSafetyStock(String tenantId, String inventorySnapshotId, int safetyStock) {
        InventorySnapshot current = requireOwnedInventorySnapshot(tenantId, inventorySnapshotId);
        InventorySnapshot updated = inventorySnapshotRepository.save(current.withSafetyStock(safetyStock));
        auditLogService.recordForTenant(tenantId, "UPDATE_INVENTORY_SAFETY_STOCK", "inventory_snapshot", inventorySnapshotId);
        return updated;
    }

    public List<ReplenishmentTask> listReplenishmentTasks(String tenantId) {
        return replenishmentTaskRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public ReplenishmentTask createReplenishmentTask(String tenantId, CreateReplenishmentTaskRequest request) {
        Store store = requireOwnedStore(tenantId, request.storeId());
        Product product = requireOwnedProduct(tenantId, request.productId());
        requireSameStore(store.storeId(), product.storeId(), "product does not belong to the target store");
        ReplenishmentTask replenishmentTask = replenishmentTaskRepository.save(new ReplenishmentTask(
                null,
                request.storeId(),
                request.productId(),
                request.skuId(),
                request.suggestedQty(),
                "draft",
                "not_required",
                request.reasonText(),
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_REPLENISHMENT_TASK", "replenishment_task", replenishmentTask.replenishmentTaskId());
        return replenishmentTask;
    }

    public ReplenishmentTask submitApproval(String tenantId, String operatorId, String replenishmentTaskId) {
        ReplenishmentTask current = requireOwnedReplenishmentTask(tenantId, replenishmentTaskId);
        if (!"draft".equals(current.taskStatus()) && !"pending_approval".equals(current.taskStatus())) {
            throw new BusinessException("1008", "current status does not allow submit approval", HttpStatus.BAD_REQUEST);
        }
        approvalService.createApprovalIfAbsent(
                tenantId,
                operatorId,
                "replenishment_submit",
                "replenishment_task",
                replenishmentTaskId,
                operatorId,
                current.reasonText()
        );
        ReplenishmentTask updated = replenishmentTaskRepository.save(current.withWorkflow("pending_approval", "pending"));
        auditLogService.recordForTenant(tenantId, "SUBMIT_REPLENISHMENT_APPROVAL", "replenishment_task", replenishmentTaskId);
        return updated;
    }

    public List<PurchaseRequest> listPurchaseRequests(String tenantId) {
        return purchaseRequestStorage.values().stream()
                .filter(request -> ownedStoreIds(tenantId).contains(request.storeId()))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
    }

    public PurchaseRequest createPurchaseRequest(String tenantId,
                                                 String operatorId,
                                                 CreatePurchaseRequestCommand command) {
        Store store = requireOwnedStore(tenantId, command.storeId());
        requireOwnedSupplier(tenantId, command.supplierId(), store.storeId());
        if (command.items() == null || command.items().isEmpty()) {
            throw new BusinessException("7402", "purchase request items must not be empty", HttpStatus.BAD_REQUEST);
        }

        List<PurchaseRequestItem> items = command.items().stream()
                .map(item -> toPurchaseRequestItem(tenantId, store.storeId(), item))
                .toList();
        int totalRequestedQty = items.stream()
                .mapToInt(PurchaseRequestItem::requestedQty)
                .sum();
        String sequenceValue = String.valueOf(purchaseRequestSequence.incrementAndGet());
        PurchaseRequest purchaseRequest = new PurchaseRequest(
                "purchase-request-" + sequenceValue,
                "PR-" + sequenceValue,
                command.storeId(),
                command.supplierId(),
                "draft",
                "not_submitted",
                operatorId,
                command.expectedDeliveryDate(),
                totalRequestedQty,
                command.reasonText(),
                items,
                OffsetDateTime.now()
        );
        purchaseRequestStorage.put(purchaseRequest.purchaseRequestId(), purchaseRequest);
        auditLogService.recordForTenant(tenantId, "CREATE_PURCHASE_REQUEST", "purchase_request", purchaseRequest.purchaseRequestId());
        return purchaseRequest;
    }

    public PurchaseRequest getPurchaseRequest(String tenantId, String purchaseRequestId) {
        PurchaseRequest request = requireOwnedPurchaseRequest(tenantId, purchaseRequestId);
        requireOwnedStore(tenantId, request.storeId());
        return request;
    }

    public PurchaseRequest submitPurchaseRequest(String tenantId, String operatorId, String purchaseRequestId) {
        PurchaseRequest current = requireOwnedPurchaseRequest(tenantId, purchaseRequestId);
        if (!"draft".equals(current.requestStatus())) {
            throw new BusinessException("1008", "current status does not allow submit", HttpStatus.BAD_REQUEST);
        }
        PurchaseRequest updated = current.withWorkflow("submitted", "pending_review", operatorId);
        purchaseRequestStorage.put(updated.purchaseRequestId(), updated);
        auditLogService.recordForTenant(tenantId, "SUBMIT_PURCHASE_REQUEST", "purchase_request", purchaseRequestId);
        return updated;
    }

    public List<PurchaseOrder> listPurchaseOrders(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return PURCHASE_ORDER_STORAGE.values().stream()
                .filter(order -> storeIds.contains(order.storeId()))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
    }

    public PurchaseOrder createPurchaseOrder(String tenantId,
                                             String operatorId,
                                             CreatePurchaseOrderCommand command) {
        PurchaseRequest purchaseRequest = requireOwnedPurchaseRequest(tenantId, command.purchaseRequestId());
        if (!"submitted".equals(purchaseRequest.requestStatus())) {
            throw new BusinessException("7405", "purchase request status does not allow order creation", HttpStatus.BAD_REQUEST);
        }
        findPurchaseOrderByPurchaseRequestId(purchaseRequest.purchaseRequestId()).ifPresent(existing -> {
            throw new BusinessException("7406", "purchase order already exists for this request", HttpStatus.BAD_REQUEST);
        });
        String sequenceValue = String.valueOf(PURCHASE_ORDER_SEQUENCE.incrementAndGet());
        PurchaseOrder purchaseOrder = new PurchaseOrder(
                "purchase-order-" + sequenceValue,
                "PO-" + sequenceValue,
                purchaseRequest.purchaseRequestId(),
                purchaseRequest.requestNo(),
                purchaseRequest.storeId(),
                purchaseRequest.supplierId(),
                "draft",
                "not_submitted",
                "not_dispatched",
                "not_received",
                "not_inbounded",
                "none",
                "none",
                operatorId,
                purchaseRequest.expectedDeliveryDate(),
                purchaseRequest.totalRequestedQty(),
                purchaseRequest.items(),
                command.remark(),
                OffsetDateTime.now()
        );
        PURCHASE_ORDER_STORAGE.put(purchaseOrder.purchaseOrderId(), purchaseOrder);
        PurchaseRequest convertedRequest = purchaseRequest.withWorkflow("ordered", "converted", purchaseRequest.requestedBy());
        purchaseRequestStorage.put(convertedRequest.purchaseRequestId(), convertedRequest);
        auditLogService.recordForTenant(tenantId, "CREATE_PURCHASE_ORDER", "purchase_order", purchaseOrder.purchaseOrderId());
        return purchaseOrder;
    }

    public PurchaseOrder getPurchaseOrder(String tenantId, String purchaseOrderId) {
        PurchaseOrder purchaseOrder = requireOwnedPurchaseOrder(tenantId, purchaseOrderId);
        requireOwnedStore(tenantId, purchaseOrder.storeId());
        return purchaseOrder;
    }

    public PurchaseOrder submitPurchaseOrderApproval(String tenantId, String operatorId, String purchaseOrderId) {
        PurchaseOrder current = requireOwnedPurchaseOrder(tenantId, purchaseOrderId);
        if (!"draft".equals(current.orderStatus()) && !"pending_approval".equals(current.orderStatus())) {
            throw new BusinessException("1008", "current status does not allow submit approval", HttpStatus.BAD_REQUEST);
        }
        approvalService.createApprovalIfAbsent(
                tenantId,
                operatorId,
                "purchase_order_review",
                "purchase_order",
                purchaseOrderId,
                operatorId,
                current.remark()
        );
        PurchaseOrder updated = current.withApproval("pending_approval", "pending");
        PURCHASE_ORDER_STORAGE.put(updated.purchaseOrderId(), updated);
        auditLogService.recordForTenant(tenantId, "SUBMIT_PURCHASE_ORDER_APPROVAL", "purchase_order", purchaseOrderId);
        return updated;
    }

    public PurchaseOrder dispatchPurchaseOrder(String tenantId, String operatorId, String purchaseOrderId) {
        PurchaseOrder current = requireOwnedPurchaseOrder(tenantId, purchaseOrderId);
        if (!"approved".equals(current.orderStatus())) {
            throw new BusinessException("7407", "purchase order status does not allow dispatch", HttpStatus.BAD_REQUEST);
        }
        if ("dispatched".equals(current.dispatchStatus())) {
            throw new BusinessException("7408", "purchase order has already been dispatched", HttpStatus.BAD_REQUEST);
        }
        PurchaseOrder updated = current.withDispatch("dispatched", operatorId);
        PURCHASE_ORDER_STORAGE.put(updated.purchaseOrderId(), updated);
        auditLogService.recordForTenant(tenantId, "DISPATCH_PURCHASE_ORDER", "purchase_order", purchaseOrderId);
        return updated;
    }

    public List<PurchaseReceipt> listPurchaseReceipts(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return PURCHASE_RECEIPT_STORAGE.values().stream()
                .filter(receipt -> storeIds.contains(receipt.storeId()))
                .sorted((left, right) -> right.receivedAt().compareTo(left.receivedAt()))
                .toList();
    }

    public List<PurchaseReceiptDiscrepancy> listPurchaseReceiptDiscrepancies(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return PURCHASE_DISCREPANCY_STORAGE.values().stream()
                .filter(discrepancy -> storeIds.contains(discrepancy.storeId()))
                .sorted((left, right) -> right.reportedAt().compareTo(left.reportedAt()))
                .toList();
    }

    public List<PurchaseReturnRecord> listPurchaseReturns(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return PURCHASE_RETURN_STORAGE.values().stream()
                .filter(returnRecord -> storeIds.contains(returnRecord.storeId()))
                .sorted((left, right) -> right.returnedAt().compareTo(left.returnedAt()))
                .toList();
    }

    public List<InventoryLedger> listInventoryLedgers(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return inventorySnapshotRepository.findByStoreIds(storeIds).stream()
                .map(snapshot -> new InventoryLedger(
                        snapshot.inventorySnapshotId(),
                        snapshot.storeId(),
                        snapshot.productId(),
                        snapshot.skuId(),
                        snapshot.availableStock(),
                        snapshot.reservedStock(),
                        snapshot.safetyStock(),
                        totalTransactionQty(snapshot.storeId(), snapshot.productId(), snapshot.skuId(), "inbound"),
                        totalTransactionQty(snapshot.storeId(), snapshot.productId(), snapshot.skuId(), "outbound"),
                        lastTransactionAt(snapshot.storeId(), snapshot.productId(), snapshot.skuId()),
                        riskLevel(snapshot)
                ))
                .toList();
    }

    public List<InventoryTransactionRecord> listInventoryTransactions(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return INVENTORY_TRANSACTION_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .sorted((left, right) -> right.occurredAt().compareTo(left.occurredAt()))
                .toList();
    }

    public List<InventoryTransferRecord> listInventoryTransfers(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return INVENTORY_TRANSFER_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.sourceStoreId()) || storeIds.contains(record.targetStoreId()))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
    }

    public List<InventoryFreezeRecord> listInventoryFreezes(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return INVENTORY_FREEZE_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
    }

    public List<InventoryBatch> listInventoryBatches(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return INVENTORY_BATCH_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .sorted((left, right) -> right.receivedAt().compareTo(left.receivedAt()))
                .toList();
    }

    public List<InventoryCostLot> listInventoryCostLots(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return INVENTORY_COST_LOT_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .sorted((left, right) -> right.effectiveAt().compareTo(left.effectiveAt()))
                .toList();
    }

    public List<PayableLedgerEntry> listPayableLedgers(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return PAYABLE_LEDGER_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .sorted((left, right) -> right.occurredAt().compareTo(left.occurredAt()))
                .toList();
    }

    public List<SupplierReconciliationView> listSupplierReconciliations(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return PAYABLE_LEDGER_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .map(PayableLedgerEntry::supplierId)
                .distinct()
                .map(supplierId -> buildSupplierReconciliation(tenantId, supplierId))
                .sorted((left, right) -> right.netPayableAmount().compareTo(left.netPayableAmount()))
                .toList();
    }

    public List<PurchaseExpenseAllocation> listPurchaseExpenseAllocations(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return PURCHASE_EXPENSE_ALLOCATION_STORAGE.values().stream()
                .filter(record -> storeIds.contains(record.storeId()))
                .sorted((left, right) -> right.allocatedAt().compareTo(left.allocatedAt()))
                .toList();
    }

    public PurchaseExpenseAllocation createPurchaseExpenseAllocation(String tenantId,
                                                                    String operatorId,
                                                                    CreatePurchaseExpenseAllocationCommand command) {
        PurchaseOrder purchaseOrder = requireOwnedPurchaseOrder(tenantId, command.purchaseOrderId());
        if (command.expenseAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("7428", "expense amount must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        int receiptQty = receiptQtyForPurchaseOrder(purchaseOrder.purchaseOrderId());
        int returnedQty = returnQtyForPurchaseOrder(purchaseOrder.purchaseOrderId());
        int allocatableQty = receiptQty - returnedQty;
        if (allocatableQty <= 0) {
            throw new BusinessException("7429", "no received quantity is available for allocation", HttpStatus.BAD_REQUEST);
        }
        OffsetDateTime now = OffsetDateTime.now();
        String sequenceValue = String.valueOf(PURCHASE_EXPENSE_ALLOCATION_SEQUENCE.incrementAndGet());
        PurchaseExpenseAllocation allocation = new PurchaseExpenseAllocation(
                "purchase-expense-allocation-" + sequenceValue,
                "PEA-" + sequenceValue,
                purchaseOrder.purchaseOrderId(),
                purchaseOrder.orderNo(),
                purchaseOrder.storeId(),
                purchaseOrder.supplierId(),
                command.expenseType(),
                command.allocationRule(),
                command.expenseAmount(),
                allocatableQty,
                command.expenseAmount().divide(BigDecimal.valueOf(allocatableQty), 2, RoundingMode.HALF_UP),
                operatorId,
                command.remark(),
                now
        );
        PURCHASE_EXPENSE_ALLOCATION_STORAGE.put(allocation.allocationId(), allocation);
        auditLogService.recordForTenant(tenantId, "CREATE_PURCHASE_EXPENSE_ALLOCATION", "purchase_expense_allocation", allocation.allocationId());
        return allocation;
    }

    public List<PurchaseCostCollectionView> listPurchaseCostCollections(String tenantId) {
        List<String> storeIds = ownedStoreIds(tenantId);
        return PURCHASE_ORDER_STORAGE.values().stream()
                .filter(order -> storeIds.contains(order.storeId()))
                .filter(order -> receiptQtyForPurchaseOrder(order.purchaseOrderId()) > 0)
                .map(this::buildPurchaseCostCollection)
                .sorted((left, right) -> right.totalCollectedCost().compareTo(left.totalCollectedCost()))
                .toList();
    }

    public PurchaseReceipt receivePurchaseOrder(String tenantId,
                                                String operatorId,
                                                String purchaseOrderId,
                                                ReceivePurchaseOrderCommand command) {
        PurchaseOrder purchaseOrder = requireOwnedPurchaseOrder(tenantId, purchaseOrderId);
        if (!"dispatched".equals(purchaseOrder.orderStatus())) {
            throw new BusinessException("7409", "purchase order must be dispatched before receiving", HttpStatus.BAD_REQUEST);
        }
        if ("completed".equals(purchaseOrder.receivingStatus())) {
            throw new BusinessException("7410", "purchase order has already been completed", HttpStatus.BAD_REQUEST);
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new BusinessException("7411", "receipt items must not be empty", HttpStatus.BAD_REQUEST);
        }

        OffsetDateTime now = OffsetDateTime.now();
        List<PurchaseReceiptItem> receiptItems = command.items().stream()
                .map(item -> toPurchaseReceiptItem(purchaseOrder, item, now))
                .toList();
        List<PurchaseReceiptDiscrepancy> discrepancies = receiptItems.stream()
                .map(receiptItem -> toReceiptDiscrepancy(purchaseOrder, receiptItem, now))
                .filter(discrepancy -> discrepancy.discrepancyQty() > 0)
                .toList();

        for (PurchaseReceiptItem receiptItem : receiptItems) {
            InventorySnapshot currentSnapshot = findInventorySnapshot(purchaseOrder.storeId(), receiptItem.productId(), receiptItem.skuId());
            if (currentSnapshot == null) {
                InventorySnapshot createdSnapshot = inventorySnapshotRepository.save(new InventorySnapshot(
                        null,
                        purchaseOrder.storeId(),
                        receiptItem.productId(),
                        receiptItem.skuId(),
                        receiptItem.receivedQty(),
                        0,
                        0,
                        now,
                        now
                ));
                recordInventoryTransaction(
                        purchaseOrder.storeId(),
                        receiptItem.productId(),
                        receiptItem.skuId(),
                        "inbound",
                        "purchase_receipt",
                        purchaseOrder.purchaseOrderId(),
                        0,
                        createdSnapshot.availableStock(),
                        receiptItem.receivedQty(),
                        now
                );
            } else {
                InventorySnapshot updatedSnapshot = inventorySnapshotRepository.save(currentSnapshot.withInbound(receiptItem.receivedQty(), now));
                recordInventoryTransaction(
                        purchaseOrder.storeId(),
                        receiptItem.productId(),
                        receiptItem.skuId(),
                        "inbound",
                        "purchase_receipt",
                        purchaseOrder.purchaseOrderId(),
                        currentSnapshot.availableStock(),
                        updatedSnapshot.availableStock(),
                        receiptItem.receivedQty(),
                        now
                );
            }
            createInventoryBatch(purchaseOrder, receiptItem, now);
            createInventoryCostLot(
                    purchaseOrder.storeId(),
                    purchaseOrder.purchaseOrderId(),
                    "purchase_receipt",
                    receiptItem.productId(),
                    receiptItem.skuId(),
                    receiptItem.batchNo(),
                    receiptItem.receivedQty(),
                    receiptItem.unitCost(),
                    now
            );
        }

        for (PurchaseReceiptDiscrepancy discrepancy : discrepancies) {
            PURCHASE_DISCREPANCY_STORAGE.put(discrepancy.discrepancyId(), discrepancy);
            auditLogService.recordForTenant(tenantId, "CREATE_PURCHASE_RECEIPT_DISCREPANCY", "purchase_receipt_discrepancy", discrepancy.discrepancyId());
        }

        String sequenceValue = String.valueOf(PURCHASE_RECEIPT_SEQUENCE.incrementAndGet());
        PurchaseReceipt purchaseReceipt = new PurchaseReceipt(
                "purchase-receipt-" + sequenceValue,
                "PRC-" + sequenceValue,
                purchaseOrder.purchaseOrderId(),
                purchaseOrder.orderNo(),
                purchaseOrder.storeId(),
                purchaseOrder.supplierId(),
                "completed",
                operatorId,
                receiptItems,
                command.remark(),
                now
        );
        PURCHASE_RECEIPT_STORAGE.put(purchaseReceipt.purchaseReceiptId(), purchaseReceipt);
        createPayableLedgerEntry(
                purchaseOrder.storeId(),
                purchaseOrder.supplierId(),
                purchaseOrder.purchaseOrderId(),
                purchaseOrder.orderNo(),
                "purchase_receipt",
                purchaseReceipt.purchaseReceiptId(),
                "payable_increase",
                receiptItems.stream()
                        .map(item -> item.unitCost().multiply(BigDecimal.valueOf(item.receivedQty())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                now
        );
        PurchaseOrder receivedOrder = purchaseOrder.withReceiving(
                "completed",
                "completed",
                discrepancies.isEmpty() ? "none" : "reported",
                operatorId
        );
        PURCHASE_ORDER_STORAGE.put(receivedOrder.purchaseOrderId(), receivedOrder);
        auditLogService.recordForTenant(tenantId, "RECEIVE_PURCHASE_ORDER", "purchase_order", purchaseOrderId);
        auditLogService.recordForTenant(tenantId, "CREATE_PURCHASE_RECEIPT", "purchase_receipt", purchaseReceipt.purchaseReceiptId());
        return purchaseReceipt;
    }

    public PurchaseReturnRecord createPurchaseReturn(String tenantId,
                                                     String operatorId,
                                                     String purchaseOrderId,
                                                     CreatePurchaseReturnCommand command) {
        PurchaseOrder purchaseOrder = requireOwnedPurchaseOrder(tenantId, purchaseOrderId);
        if (!"completed".equals(purchaseOrder.receivingStatus())) {
            throw new BusinessException("7415", "purchase return requires a completed receiving record", HttpStatus.BAD_REQUEST);
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new BusinessException("7416", "return items must not be empty", HttpStatus.BAD_REQUEST);
        }

        OffsetDateTime now = OffsetDateTime.now();
        List<PurchaseReturnItem> returnItems = command.items().stream()
                .map(item -> toPurchaseReturnItem(purchaseOrder, item, now))
                .toList();

        for (PurchaseReturnItem returnItem : returnItems) {
            InventorySnapshot currentSnapshot = findInventorySnapshot(purchaseOrder.storeId(), returnItem.productId(), returnItem.skuId());
            if (currentSnapshot == null || currentSnapshot.availableStock() < returnItem.returnQty()) {
                throw new BusinessException("7417", "insufficient available stock for purchase return", HttpStatus.BAD_REQUEST);
            }
            InventorySnapshot updatedSnapshot = inventorySnapshotRepository.save(currentSnapshot.withOutbound(returnItem.returnQty(), now));
            updateBatchForReturn(purchaseOrder.storeId(), returnItem, now);
            consumeInventoryCost(
                    purchaseOrder.storeId(),
                    returnItem.productId(),
                    returnItem.skuId(),
                    returnItem.batchNo(),
                    returnItem.returnQty()
            );
            recordInventoryTransaction(
                    purchaseOrder.storeId(),
                    returnItem.productId(),
                    returnItem.skuId(),
                    "outbound",
                    "purchase_return",
                    purchaseOrder.purchaseOrderId(),
                    currentSnapshot.availableStock(),
                    updatedSnapshot.availableStock(),
                    returnItem.returnQty(),
                    now
            );
        }

        String sequenceValue = String.valueOf(PURCHASE_RETURN_SEQUENCE.incrementAndGet());
        PurchaseReturnRecord returnRecord = new PurchaseReturnRecord(
                "purchase-return-" + sequenceValue,
                "PRTN-" + sequenceValue,
                purchaseOrder.purchaseOrderId(),
                purchaseOrder.orderNo(),
                purchaseOrder.storeId(),
                purchaseOrder.supplierId(),
                "returned",
                operatorId,
                returnItems,
                command.remark(),
                now
        );
        PURCHASE_RETURN_STORAGE.put(returnRecord.purchaseReturnId(), returnRecord);
        createPayableLedgerEntry(
                purchaseOrder.storeId(),
                purchaseOrder.supplierId(),
                purchaseOrder.purchaseOrderId(),
                purchaseOrder.orderNo(),
                "purchase_return",
                returnRecord.purchaseReturnId(),
                "payable_decrease",
                returnItems.stream()
                        .map(item -> unitCostForReturnItem(purchaseOrder.purchaseOrderId(), item)
                                .multiply(BigDecimal.valueOf(item.returnQty())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                now
        );
        PurchaseOrder returnedOrder = purchaseOrder.withReturnStatus("partial_returned", operatorId);
        PURCHASE_ORDER_STORAGE.put(returnedOrder.purchaseOrderId(), returnedOrder);
        auditLogService.recordForTenant(tenantId, "CREATE_PURCHASE_RETURN", "purchase_return", returnRecord.purchaseReturnId());
        return returnRecord;
    }

    public InventoryTransferRecord createInventoryTransfer(String tenantId,
                                                           String operatorId,
                                                           CreateInventoryTransferCommand command) {
        Store sourceStore = requireOwnedStore(tenantId, command.sourceStoreId());
        Store targetStore = requireOwnedStore(tenantId, command.targetStoreId());
        if (sourceStore.storeId().equals(targetStore.storeId())) {
            throw new BusinessException("7420", "source and target stores must be different", HttpStatus.BAD_REQUEST);
        }
        if (command.transferQty() <= 0) {
            throw new BusinessException("7421", "transfer quantity must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        Product product = requireOwnedProduct(tenantId, command.productId());
        requireSameStore(sourceStore.storeId(), product.storeId(), "product does not belong to the source store");
        InventorySnapshot sourceSnapshot = findInventorySnapshot(sourceStore.storeId(), command.productId(), command.skuId());
        if (sourceSnapshot == null || sourceSnapshot.availableStock() < command.transferQty()) {
            throw new BusinessException("7422", "insufficient available stock for transfer", HttpStatus.BAD_REQUEST);
        }

        OffsetDateTime now = OffsetDateTime.now();
        inventorySnapshotRepository.save(sourceSnapshot.withFreeze(command.transferQty(), now));
        InventoryBatch sourceBatch = freezeInventoryBatch(sourceStore.storeId(), command.productId(), command.skuId(), command.batchNo(), command.transferQty(), now);

        String transferSequence = String.valueOf(INVENTORY_TRANSFER_SEQUENCE.incrementAndGet());
        InventoryTransferRecord transferRecord = new InventoryTransferRecord(
                "inventory-transfer-" + transferSequence,
                "ITF-" + transferSequence,
                sourceStore.storeId(),
                targetStore.storeId(),
                command.productId(),
                command.skuId(),
                sourceBatch.batchNo(),
                command.transferQty(),
                "frozen",
                operatorId,
                command.reasonText(),
                now,
                null
        );
        INVENTORY_TRANSFER_STORAGE.put(transferRecord.transferId(), transferRecord);

        String freezeSequence = String.valueOf(INVENTORY_FREEZE_SEQUENCE.incrementAndGet());
        InventoryFreezeRecord freezeRecord = new InventoryFreezeRecord(
                "inventory-freeze-" + freezeSequence,
                "IFZ-" + freezeSequence,
                sourceStore.storeId(),
                command.productId(),
                command.skuId(),
                sourceBatch.batchNo(),
                "inventory_transfer",
                transferRecord.transferId(),
                command.transferQty(),
                "active",
                operatorId,
                command.reasonText(),
                now,
                null
        );
        INVENTORY_FREEZE_STORAGE.put(freezeRecord.freezeId(), freezeRecord);
        auditLogService.recordForTenant(tenantId, "CREATE_INVENTORY_TRANSFER", "inventory_transfer", transferRecord.transferId());
        auditLogService.recordForTenant(tenantId, "FREEZE_INVENTORY_STOCK", "inventory_freeze", freezeRecord.freezeId());
        return transferRecord;
    }

    public InventoryTransferRecord completeInventoryTransfer(String tenantId,
                                                             String operatorId,
                                                             String transferId) {
        InventoryTransferRecord transferRecord = requireOwnedInventoryTransfer(tenantId, transferId);
        if ("completed".equals(transferRecord.transferStatus())) {
            throw new BusinessException("7423", "inventory transfer has already been completed", HttpStatus.BAD_REQUEST);
        }

        OffsetDateTime now = OffsetDateTime.now();
        InventorySnapshot sourceSnapshot = findInventorySnapshot(
                transferRecord.sourceStoreId(),
                transferRecord.productId(),
                transferRecord.skuId()
        );
        if (sourceSnapshot == null || sourceSnapshot.reservedStock() < transferRecord.transferQty()) {
            throw new BusinessException("7424", "insufficient reserved stock for transfer completion", HttpStatus.BAD_REQUEST);
        }

        InventorySnapshot updatedSourceSnapshot = inventorySnapshotRepository.save(
                sourceSnapshot.withReservedTransfer(transferRecord.transferQty(), now)
        );
        releaseInventoryBatchForTransfer(
                transferRecord.sourceStoreId(),
                transferRecord.productId(),
                transferRecord.skuId(),
                transferRecord.batchNo(),
                transferRecord.transferQty(),
                now
        );

        InventoryCostAllocation allocation = consumeInventoryCost(
                transferRecord.sourceStoreId(),
                transferRecord.productId(),
                transferRecord.skuId(),
                transferRecord.batchNo(),
                transferRecord.transferQty()
        );

        InventorySnapshot targetSnapshot = findInventorySnapshot(
                transferRecord.targetStoreId(),
                transferRecord.productId(),
                transferRecord.skuId()
        );
        if (targetSnapshot == null) {
            inventorySnapshotRepository.save(new InventorySnapshot(
                    null,
                    transferRecord.targetStoreId(),
                    transferRecord.productId(),
                    transferRecord.skuId(),
                    transferRecord.transferQty(),
                    0,
                    0,
                    now,
                    now
            ));
        } else {
            inventorySnapshotRepository.save(targetSnapshot.withInbound(transferRecord.transferQty(), now));
        }

        InventoryBatch sourceBatch = findInventoryBatch(
                transferRecord.sourceStoreId(),
                transferRecord.productId(),
                transferRecord.skuId(),
                transferRecord.batchNo()
        );
        createTransferredInventoryBatch(transferRecord, sourceBatch, allocation.unitCost(), now);
        createInventoryCostLot(
                transferRecord.targetStoreId(),
                transferRecord.transferId(),
                "inventory_transfer",
                transferRecord.productId(),
                transferRecord.skuId(),
                transferRecord.batchNo(),
                transferRecord.transferQty(),
                allocation.unitCost(),
                now
        );

        recordInventoryTransaction(
                transferRecord.sourceStoreId(),
                transferRecord.productId(),
                transferRecord.skuId(),
                "outbound",
                "inventory_transfer_out",
                transferRecord.transferId(),
                sourceSnapshot.availableStock() + sourceSnapshot.reservedStock(),
                updatedSourceSnapshot.availableStock(),
                transferRecord.transferQty(),
                now
        );

        InventorySnapshot refreshedTargetSnapshot = findInventorySnapshot(
                transferRecord.targetStoreId(),
                transferRecord.productId(),
                transferRecord.skuId()
        );
        recordInventoryTransaction(
                transferRecord.targetStoreId(),
                transferRecord.productId(),
                transferRecord.skuId(),
                "inbound",
                "inventory_transfer_in",
                transferRecord.transferId(),
                refreshedTargetSnapshot.availableStock() - transferRecord.transferQty(),
                refreshedTargetSnapshot.availableStock(),
                transferRecord.transferQty(),
                now
        );

        InventoryFreezeRecord freezeRecord = findActiveFreezeRecord(transferRecord.transferId());
        if (freezeRecord != null) {
            INVENTORY_FREEZE_STORAGE.put(
                    freezeRecord.freezeId(),
                    freezeRecord.withStatus("released", now)
            );
        }

        InventoryTransferRecord completedRecord = transferRecord.withCompletion(operatorId, now);
        INVENTORY_TRANSFER_STORAGE.put(completedRecord.transferId(), completedRecord);
        auditLogService.recordForTenant(tenantId, "COMPLETE_INVENTORY_TRANSFER", "inventory_transfer", transferRecord.transferId());
        return completedRecord;
    }

    public WmsWarehouse createWmsWarehouse(String tenantId,
                                           String operatorId,
                                           CreateWmsWarehouseCommand command) {
        requireOwnedStore(tenantId, command.storeId());
        OffsetDateTime now = OffsetDateTime.now();
        String sequenceValue = String.valueOf(WMS_WAREHOUSE_SEQUENCE.incrementAndGet());
        WmsWarehouse warehouse = new WmsWarehouse(
                "wms-warehouse-" + sequenceValue,
                command.storeId(),
                "WH-" + sequenceValue,
                command.warehouseName(),
                command.zoneCode(),
                command.locationCode(),
                command.temperatureZone(),
                "active",
                operatorId,
                now
        );
        WMS_WAREHOUSE_STORAGE.put(warehouse.warehouseId(), warehouse);
        auditLogService.recordForTenant(tenantId, "CREATE_WMS_WAREHOUSE", "wms_warehouse", warehouse.warehouseId());
        return warehouse;
    }

    public WmsInboundTask createWmsInboundTask(String tenantId,
                                               String operatorId,
                                               CreateWmsInboundTaskCommand command) {
        requireOwnedStore(tenantId, command.storeId());
        requireOwnedProduct(tenantId, command.productId());
        WmsWarehouse warehouse = requireOwnedWmsWarehouse(tenantId, command.storeId(), command.warehouseCode());
        InventorySnapshot snapshot = findInventorySnapshot(command.storeId(), command.productId(), command.skuId());
        if (snapshot == null || snapshot.availableStock() < command.quantity()) {
            throw new BusinessException("7430", "insufficient available stock for WMS outbound", HttpStatus.BAD_REQUEST);
        }
        InventoryBatch batch = findInventoryBatch(command.storeId(), command.productId(), command.skuId(), command.batchNo());
        if (batch == null || batch.availableQty() < command.quantity()) {
            throw new BusinessException("7431", "鎵规搴撳瓨涓嶈冻锛屼笉鑳藉垱寤?WMS 鍏ュ簱浠诲姟", HttpStatus.BAD_REQUEST);
        }
        OffsetDateTime now = OffsetDateTime.now();
        String sequenceValue = String.valueOf(WMS_INBOUND_TASK_SEQUENCE.incrementAndGet());
        WmsInboundTask inboundTask = new WmsInboundTask(
                "wms-inbound-task-" + sequenceValue,
                command.storeId(),
                command.productId(),
                command.skuId(),
                command.batchNo(),
                warehouse.warehouseCode(),
                command.zoneCode(),
                command.locationCode(),
                command.taskType(),
                command.quantity(),
                "completed",
                "stored",
                operatorId,
                now
        );
        WMS_INBOUND_TASK_STORAGE.put(inboundTask.inboundTaskId(), inboundTask);
        auditLogService.recordForTenant(tenantId, "CREATE_WMS_INBOUND_TASK", "wms_inbound_task", inboundTask.inboundTaskId());
        return inboundTask;
    }

    public WmsWave createWmsWave(String tenantId,
                                 String operatorId,
                                 CreateWmsWaveCommand command) {
        requireOwnedStore(tenantId, command.storeId());
        requireOwnedWmsWarehouse(tenantId, command.storeId(), command.warehouseCode());
        int allocatedQty = command.items().stream()
                .mapToInt(WmsWaveItemCommand::plannedQty)
                .sum();
        OffsetDateTime now = OffsetDateTime.now();
        String sequenceValue = String.valueOf(WMS_WAVE_SEQUENCE.incrementAndGet());
        WmsWave wave = new WmsWave(
                "wms-wave-" + sequenceValue,
                command.storeId(),
                command.warehouseCode(),
                command.waveType(),
                command.strategyCode(),
                allocatedQty,
                "released",
                operatorId,
                now
        );
        WMS_WAVE_STORAGE.put(wave.waveId(), wave);
        auditLogService.recordForTenant(tenantId, "CREATE_WMS_WAVE", "wms_wave", wave.waveId());
        return wave;
    }

    public WmsLockRecord createWmsLock(String tenantId,
                                       String operatorId,
                                       CreateWmsLockCommand command) {
        requireOwnedStore(tenantId, command.storeId());
        requireOwnedProduct(tenantId, command.productId());
        requireOwnedWmsWarehouse(tenantId, command.storeId(), command.warehouseCode());
        InventorySnapshot snapshot = findInventorySnapshot(command.storeId(), command.productId(), command.skuId());
        if (snapshot == null || snapshot.availableStock() < command.lockQty()) {
            throw new BusinessException("7432", "insufficient available stock for warehouse lock", HttpStatus.BAD_REQUEST);
        }
        OffsetDateTime now = OffsetDateTime.now();
        inventorySnapshotRepository.save(snapshot.withFreeze(command.lockQty(), now));
        freezeInventoryBatch(command.storeId(), command.productId(), command.skuId(), command.batchNo(), command.lockQty(), now);
        String sequenceValue = String.valueOf(WMS_LOCK_SEQUENCE.incrementAndGet());
        WmsLockRecord lockRecord = new WmsLockRecord(
                "wms-lock-" + sequenceValue,
                command.storeId(),
                command.productId(),
                command.skuId(),
                command.batchNo(),
                command.warehouseCode(),
                command.locationCode(),
                command.lockQty(),
                "locked",
                command.reasonText(),
                operatorId,
                now
        );
        WMS_LOCK_STORAGE.put(lockRecord.lockId(), lockRecord);
        auditLogService.recordForTenant(tenantId, "CREATE_WMS_LOCK", "wms_lock", lockRecord.lockId());
        return lockRecord;
    }

    public WmsCycleCountTask createWmsCycleCountTask(String tenantId,
                                                     String operatorId,
                                                     CreateWmsCycleCountTaskCommand command) {
        requireOwnedStore(tenantId, command.storeId());
        requireOwnedProduct(tenantId, command.productId());
        requireOwnedWmsWarehouse(tenantId, command.storeId(), command.warehouseCode());
        InventorySnapshot snapshot = findInventorySnapshot(command.storeId(), command.productId(), command.skuId());
        if (snapshot == null) {
            throw new BusinessException("7433", "搴撳瓨蹇収涓嶅瓨鍦紝涓嶈兘鐩樼偣", HttpStatus.BAD_REQUEST);
        }
        int varianceQty = Math.abs(command.systemQty() - command.countedQty());
        String adjustmentType = command.countedQty() >= command.systemQty() ? "surplus" : "shortage";
        OffsetDateTime now = OffsetDateTime.now();
        if (command.countedQty() > command.systemQty()) {
            inventorySnapshotRepository.save(snapshot.withInbound(command.countedQty() - command.systemQty(), now));
        } else if (command.countedQty() < command.systemQty()) {
            inventorySnapshotRepository.save(snapshot.withOutbound(command.systemQty() - command.countedQty(), now));
        }
        String sequenceValue = String.valueOf(WMS_CYCLE_COUNT_TASK_SEQUENCE.incrementAndGet());
        WmsCycleCountTask cycleCountTask = new WmsCycleCountTask(
                "wms-cycle-count-" + sequenceValue,
                command.storeId(),
                command.productId(),
                command.skuId(),
                command.warehouseCode(),
                command.locationCode(),
                command.systemQty(),
                command.countedQty(),
                varianceQty,
                adjustmentType,
                "completed",
                command.varianceReason(),
                operatorId,
                now
        );
        WMS_CYCLE_COUNT_TASK_STORAGE.put(cycleCountTask.cycleCountTaskId(), cycleCountTask);
        auditLogService.recordForTenant(tenantId, "CREATE_WMS_CYCLE_COUNT_TASK", "wms_cycle_count_task", cycleCountTask.cycleCountTaskId());
        return cycleCountTask;
    }

    public WmsReverseInboundRecord createWmsReverseInbound(String tenantId,
                                                           String operatorId,
                                                           CreateWmsReverseInboundCommand command) {
        requireOwnedStore(tenantId, command.storeId());
        requireOwnedProduct(tenantId, command.productId());
        requireOwnedWmsWarehouse(tenantId, command.storeId(), command.warehouseCode());
        OffsetDateTime now = OffsetDateTime.now();
        InventorySnapshot snapshot = findInventorySnapshot(command.storeId(), command.productId(), command.skuId());
        if (snapshot == null) {
            inventorySnapshotRepository.save(new InventorySnapshot(
                    null,
                    command.storeId(),
                    command.productId(),
                    command.skuId(),
                    command.quantity(),
                    0,
                    0,
                    now,
                    now
            ));
        } else {
            inventorySnapshotRepository.save(snapshot.withInbound(command.quantity(), now));
        }
        String sequenceValue = String.valueOf(WMS_REVERSE_INBOUND_SEQUENCE.incrementAndGet());
        WmsReverseInboundRecord reverseInboundRecord = new WmsReverseInboundRecord(
                "wms-reverse-inbound-" + sequenceValue,
                command.storeId(),
                command.productId(),
                command.skuId(),
                command.warehouseCode(),
                command.locationCode(),
                command.reverseType(),
                command.quantity(),
                "completed",
                "returned",
                command.remark(),
                operatorId,
                now
        );
        WMS_REVERSE_INBOUND_STORAGE.put(reverseInboundRecord.reverseInboundId(), reverseInboundRecord);
        auditLogService.recordForTenant(tenantId, "CREATE_WMS_REVERSE_INBOUND", "wms_reverse_inbound", reverseInboundRecord.reverseInboundId());
        return reverseInboundRecord;
    }

    public WmsLinkageView getWmsLinkage(String tenantId, String storeId) {
        requireOwnedStore(tenantId, storeId);
        int warehouseCount = (int) WMS_WAREHOUSE_STORAGE.values().stream()
                .filter(record -> storeId.equals(record.storeId()))
                .count();
        int activeWaveCount = (int) WMS_WAVE_STORAGE.values().stream()
                .filter(record -> storeId.equals(record.storeId()))
                .filter(record -> "released".equals(record.waveStatus()))
                .count();
        int lockedBatchCount = (int) WMS_LOCK_STORAGE.values().stream()
                .filter(record -> storeId.equals(record.storeId()))
                .filter(record -> "locked".equals(record.lockStatus()))
                .count();
        int cycleCountTaskCount = (int) WMS_CYCLE_COUNT_TASK_STORAGE.values().stream()
                .filter(record -> storeId.equals(record.storeId()))
                .count();
        int reverseInboundCount = (int) WMS_REVERSE_INBOUND_STORAGE.values().stream()
                .filter(record -> storeId.equals(record.storeId()))
                .count();
        return new WmsLinkageView(
                storeId,
                warehouseCount,
                activeWaveCount,
                lockedBatchCount,
                cycleCountTaskCount,
                reverseInboundCount,
                "ready",
                "ready",
                "pending"
        );
    }

    public void clear() {
        purchaseRequestStorage.clear();
        purchaseRequestSequence.set(12000);
        PURCHASE_ORDER_STORAGE.clear();
        PURCHASE_ORDER_SEQUENCE.set(13000);
        PURCHASE_RECEIPT_STORAGE.clear();
        PURCHASE_RECEIPT_SEQUENCE.set(14000);
        PURCHASE_DISCREPANCY_STORAGE.clear();
        PURCHASE_DISCREPANCY_SEQUENCE.set(15000);
        PURCHASE_RETURN_STORAGE.clear();
        PURCHASE_RETURN_SEQUENCE.set(16000);
        INVENTORY_TRANSACTION_STORAGE.clear();
        INVENTORY_TRANSACTION_SEQUENCE.set(17000);
        INVENTORY_TRANSFER_STORAGE.clear();
        INVENTORY_TRANSFER_SEQUENCE.set(18000);
        INVENTORY_FREEZE_STORAGE.clear();
        INVENTORY_FREEZE_SEQUENCE.set(19000);
        INVENTORY_BATCH_STORAGE.clear();
        INVENTORY_BATCH_SEQUENCE.set(20000);
        INVENTORY_COST_LOT_STORAGE.clear();
        INVENTORY_COST_LOT_SEQUENCE.set(21000);
        PAYABLE_LEDGER_STORAGE.clear();
        PAYABLE_LEDGER_SEQUENCE.set(22000);
        PURCHASE_EXPENSE_ALLOCATION_STORAGE.clear();
        PURCHASE_EXPENSE_ALLOCATION_SEQUENCE.set(23000);
        WMS_WAREHOUSE_STORAGE.clear();
        WMS_WAREHOUSE_SEQUENCE.set(0);
        WMS_INBOUND_TASK_STORAGE.clear();
        WMS_INBOUND_TASK_SEQUENCE.set(0);
        WMS_WAVE_STORAGE.clear();
        WMS_WAVE_SEQUENCE.set(0);
        WMS_LOCK_STORAGE.clear();
        WMS_LOCK_SEQUENCE.set(0);
        WMS_CYCLE_COUNT_TASK_STORAGE.clear();
        WMS_CYCLE_COUNT_TASK_SEQUENCE.set(0);
        WMS_REVERSE_INBOUND_STORAGE.clear();
        WMS_REVERSE_INBOUND_SEQUENCE.set(0);
        replenishmentTaskRepository.deleteAll();
        inventorySnapshotRepository.deleteAll();
    }

    private InventorySnapshot requireOwnedInventorySnapshot(String tenantId, String inventorySnapshotId) {
        InventorySnapshot inventorySnapshot = inventorySnapshotRepository.findByInventorySnapshotId(inventorySnapshotId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, inventorySnapshot.storeId());
        return inventorySnapshot;
    }

    private ReplenishmentTask requireOwnedReplenishmentTask(String tenantId, String replenishmentTaskId) {
        ReplenishmentTask replenishmentTask = replenishmentTaskRepository.findByReplenishmentTaskId(replenishmentTaskId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, replenishmentTask.storeId());
        return replenishmentTask;
    }

    private Product requireOwnedProduct(String tenantId, String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, product.storeId());
        return product;
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    private WmsWarehouse requireOwnedWmsWarehouse(String tenantId, String storeId, String warehouseCode) {
        WmsWarehouse warehouse = WMS_WAREHOUSE_STORAGE.values().stream()
                .filter(current -> storeId.equals(current.storeId()))
                .filter(current -> warehouseCode.equals(current.warehouseCode()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, warehouse.storeId());
        return warehouse;
    }

    private PurchaseRequest requireOwnedPurchaseRequest(String tenantId, String purchaseRequestId) {
        PurchaseRequest purchaseRequest = purchaseRequestStorage.get(purchaseRequestId);
        if (purchaseRequest == null) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        requireOwnedStore(tenantId, purchaseRequest.storeId());
        return purchaseRequest;
    }

    private PurchaseOrder requireOwnedPurchaseOrder(String tenantId, String purchaseOrderId) {
        PurchaseOrder purchaseOrder = PURCHASE_ORDER_STORAGE.get(purchaseOrderId);
        if (purchaseOrder == null) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        requireOwnedStore(tenantId, purchaseOrder.storeId());
        return purchaseOrder;
    }

    private InventoryTransferRecord requireOwnedInventoryTransfer(String tenantId, String transferId) {
        InventoryTransferRecord transferRecord = INVENTORY_TRANSFER_STORAGE.get(transferId);
        if (transferRecord == null) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        requireOwnedStore(tenantId, transferRecord.sourceStoreId());
        requireOwnedStore(tenantId, transferRecord.targetStoreId());
        return transferRecord;
    }

    private java.util.Optional<PurchaseOrder> findPurchaseOrderByPurchaseRequestId(String purchaseRequestId) {
        return PURCHASE_ORDER_STORAGE.values().stream()
                .filter(order -> purchaseRequestId.equals(order.purchaseRequestId()))
                .findFirst();
    }

    private InventorySnapshot findInventorySnapshot(String storeId, String productId, String skuId) {
        return inventorySnapshotRepository.findByStoreIds(List.of(storeId)).stream()
                .filter(snapshot -> storeId.equals(snapshot.storeId()))
                .filter(snapshot -> productId.equals(snapshot.productId()))
                .filter(snapshot -> skuId.equals(snapshot.skuId()))
                .findFirst()
                .orElse(null);
    }

    private PurchaseReceiptItem toPurchaseReceiptItem(PurchaseOrder purchaseOrder,
                                                      ReceivePurchaseOrderItemCommand item,
                                                      OffsetDateTime receivedAt) {
        if (item.receivedQty() <= 0) {
            throw new BusinessException("7412", "received quantity must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        PurchaseRequestItem matchedItem = purchaseOrder.items().stream()
                .filter(current -> current.productId().equals(item.productId()))
                .filter(current -> current.skuId().equals(item.skuId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("7413", "received item does not match the purchase order", HttpStatus.BAD_REQUEST));
        if (item.receivedQty() > matchedItem.requestedQty()) {
            throw new BusinessException("7414", "received quantity exceeds requested quantity", HttpStatus.BAD_REQUEST);
        }
        return new PurchaseReceiptItem(
                item.productId(),
                item.skuId(),
                item.receivedQty(),
                item.batchNo() == null || item.batchNo().isBlank() ? generateBatchNo(purchaseOrder.orderNo(), item.productId(), item.skuId()) : item.batchNo(),
                item.productionDate(),
                item.expiryDate() == null ? purchaseOrder.expectedDeliveryDate().plusDays(365) : item.expiryDate(),
                matchedItem.targetUnitPrice(),
                receivedAt
        );
    }

    private PurchaseReceiptDiscrepancy toReceiptDiscrepancy(PurchaseOrder purchaseOrder,
                                                            PurchaseReceiptItem receiptItem,
                                                            OffsetDateTime reportedAt) {
        PurchaseRequestItem matchedItem = findPurchaseOrderItem(purchaseOrder, receiptItem.productId(), receiptItem.skuId());
        int discrepancyQty = matchedItem.requestedQty() - receiptItem.receivedQty();
        String sequenceValue = String.valueOf(PURCHASE_DISCREPANCY_SEQUENCE.incrementAndGet());
        return new PurchaseReceiptDiscrepancy(
                "purchase-discrepancy-" + sequenceValue,
                "PDI-" + sequenceValue,
                purchaseOrder.purchaseOrderId(),
                purchaseOrder.orderNo(),
                purchaseOrder.storeId(),
                receiptItem.productId(),
                receiptItem.skuId(),
                matchedItem.requestedQty(),
                receiptItem.receivedQty(),
                discrepancyQty,
                discrepancyQty > 0 ? "short_receive" : "none",
                discrepancyQty > 0 ? "reported" : "none",
                reportedAt
        );
    }

    private PurchaseReturnItem toPurchaseReturnItem(PurchaseOrder purchaseOrder,
                                                    CreatePurchaseReturnItemCommand item,
                                                    OffsetDateTime returnedAt) {
        if (item.returnQty() <= 0) {
            throw new BusinessException("7418", "閫€璐ф暟閲忓繀椤诲ぇ浜?0", HttpStatus.BAD_REQUEST);
        }
        PurchaseRequestItem matchedItem = findPurchaseOrderItem(purchaseOrder, item.productId(), item.skuId());
        int existingReturnedQty = PURCHASE_RETURN_STORAGE.values().stream()
                .filter(record -> purchaseOrder.purchaseOrderId().equals(record.purchaseOrderId()))
                .flatMap(record -> record.items().stream())
                .filter(current -> item.productId().equals(current.productId()))
                .filter(current -> item.skuId().equals(current.skuId()))
                .mapToInt(PurchaseReturnItem::returnQty)
                .sum();
        if (existingReturnedQty + item.returnQty() > matchedItem.requestedQty()) {
            throw new BusinessException("7419", "return quantity exceeds requested quantity", HttpStatus.BAD_REQUEST);
        }
        return new PurchaseReturnItem(
                item.productId(),
                item.skuId(),
                item.batchNo(),
                item.returnQty(),
                item.reasonText(),
                returnedAt
        );
    }

    private PurchaseRequestItem findPurchaseOrderItem(PurchaseOrder purchaseOrder, String productId, String skuId) {
        return purchaseOrder.items().stream()
                .filter(current -> current.productId().equals(productId))
                .filter(current -> current.skuId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("7413", "purchase order item not found", HttpStatus.BAD_REQUEST));
    }

    private void recordInventoryTransaction(String storeId,
                                            String productId,
                                            String skuId,
                                            String direction,
                                            String transactionType,
                                            String relatedId,
                                            int beforeAvailableStock,
                                            int afterAvailableStock,
                                            int quantity,
                                            OffsetDateTime occurredAt) {
        String sequenceValue = String.valueOf(INVENTORY_TRANSACTION_SEQUENCE.incrementAndGet());
        InventoryTransactionRecord record = new InventoryTransactionRecord(
                "inventory-txn-" + sequenceValue,
                "ITX-" + sequenceValue,
                storeId,
                productId,
                skuId,
                direction,
                transactionType,
                relatedId,
                beforeAvailableStock,
                afterAvailableStock,
                quantity,
                occurredAt
        );
        INVENTORY_TRANSACTION_STORAGE.put(record.transactionId(), record);
    }

    private void createInventoryBatch(PurchaseOrder purchaseOrder,
                                      PurchaseReceiptItem receiptItem,
                                      OffsetDateTime now) {
        String batchSequence = String.valueOf(INVENTORY_BATCH_SEQUENCE.incrementAndGet());
        InventoryBatch batch = new InventoryBatch(
                "inventory-batch-" + batchSequence,
                receiptItem.batchNo(),
                purchaseOrder.storeId(),
                receiptItem.productId(),
                receiptItem.skuId(),
                "purchase_receipt",
                purchaseOrder.purchaseOrderId(),
                receiptItem.receivedQty(),
                0,
                receiptItem.receivedQty(),
                receiptItem.productionDate(),
                receiptItem.expiryDate(),
                receiptItem.unitCost(),
                "active",
                now
        );
        INVENTORY_BATCH_STORAGE.put(batch.batchId(), batch);
    }

    private void createTransferredInventoryBatch(InventoryTransferRecord transferRecord,
                                                 InventoryBatch sourceBatch,
                                                 BigDecimal unitCost,
                                                 OffsetDateTime now) {
        String batchSequence = String.valueOf(INVENTORY_BATCH_SEQUENCE.incrementAndGet());
        InventoryBatch batch = new InventoryBatch(
                "inventory-batch-" + batchSequence,
                transferRecord.batchNo(),
                transferRecord.targetStoreId(),
                transferRecord.productId(),
                transferRecord.skuId(),
                "inventory_transfer",
                transferRecord.transferId(),
                transferRecord.transferQty(),
                0,
                transferRecord.transferQty(),
                sourceBatch == null ? null : sourceBatch.productionDate(),
                sourceBatch == null ? null : sourceBatch.expiryDate(),
                unitCost,
                "active",
                now
        );
        INVENTORY_BATCH_STORAGE.put(batch.batchId(), batch);
    }

    private InventoryBatch freezeInventoryBatch(String storeId,
                                                String productId,
                                                String skuId,
                                                String preferredBatchNo,
                                                int quantity,
                                                OffsetDateTime now) {
        InventoryBatch batch = selectInventoryBatch(storeId, productId, skuId, preferredBatchNo, quantity);
        INVENTORY_BATCH_STORAGE.put(batch.batchId(), batch.withFreeze(quantity, now));
        return batch;
    }

    private void releaseInventoryBatchForTransfer(String storeId,
                                                  String productId,
                                                  String skuId,
                                                  String batchNo,
                                                  int quantity,
                                                  OffsetDateTime now) {
        InventoryBatch batch = findInventoryBatch(storeId, productId, skuId, batchNo);
        if (batch == null || batch.lockedQty() < quantity) {
            throw new BusinessException("7425", "insufficient locked stock for transfer", HttpStatus.BAD_REQUEST);
        }
        INVENTORY_BATCH_STORAGE.put(batch.batchId(), batch.withTransferred(quantity, now));
    }

    private void updateBatchForReturn(String storeId, PurchaseReturnItem returnItem, OffsetDateTime now) {
        InventoryBatch batch = selectInventoryBatch(storeId, returnItem.productId(), returnItem.skuId(), returnItem.batchNo(), returnItem.returnQty());
        INVENTORY_BATCH_STORAGE.put(batch.batchId(), batch.withOutbound(returnItem.returnQty(), now));
    }

    private InventoryBatch selectInventoryBatch(String storeId,
                                                String productId,
                                                String skuId,
                                                String preferredBatchNo,
                                                int quantity) {
        InventoryBatch batch = preferredBatchNo != null && !preferredBatchNo.isBlank()
                ? findInventoryBatch(storeId, productId, skuId, preferredBatchNo)
                : INVENTORY_BATCH_STORAGE.values().stream()
                .filter(current -> storeId.equals(current.storeId()))
                .filter(current -> productId.equals(current.productId()))
                .filter(current -> skuId.equals(current.skuId()))
                .filter(current -> current.availableQty() >= quantity)
                .sorted((left, right) -> left.receivedAt().compareTo(right.receivedAt()))
                .findFirst()
                .orElse(null);
        if (batch == null || batch.availableQty() < quantity) {
            throw new BusinessException("7426", "鎵规鍙敤搴撳瓨涓嶈冻", HttpStatus.BAD_REQUEST);
        }
        return batch;
    }

    private InventoryBatch findInventoryBatch(String storeId, String productId, String skuId, String batchNo) {
        return INVENTORY_BATCH_STORAGE.values().stream()
                .filter(current -> storeId.equals(current.storeId()))
                .filter(current -> productId.equals(current.productId()))
                .filter(current -> skuId.equals(current.skuId()))
                .filter(current -> batchNo.equals(current.batchNo()))
                .findFirst()
                .orElse(null);
    }

    private InventoryFreezeRecord findActiveFreezeRecord(String relatedId) {
        return INVENTORY_FREEZE_STORAGE.values().stream()
                .filter(current -> relatedId.equals(current.relatedId()))
                .filter(current -> "active".equals(current.freezeStatus()))
                .findFirst()
                .orElse(null);
    }

    private void createInventoryCostLot(String storeId,
                                        String sourceId,
                                        String sourceType,
                                        String productId,
                                        String skuId,
                                        String batchNo,
                                        int quantity,
                                        BigDecimal unitCost,
                                        OffsetDateTime now) {
        String lotSequence = String.valueOf(INVENTORY_COST_LOT_SEQUENCE.incrementAndGet());
        InventoryCostLot costLot = new InventoryCostLot(
                "inventory-cost-lot-" + lotSequence,
                "ICL-" + lotSequence,
                storeId,
                productId,
                skuId,
                batchNo,
                sourceType,
                sourceId,
                quantity,
                unitCost.multiply(BigDecimal.valueOf(quantity)),
                unitCost,
                now,
                "active"
        );
        INVENTORY_COST_LOT_STORAGE.put(costLot.costLotId(), costLot);
    }

    private InventoryCostAllocation consumeInventoryCost(String storeId,
                                                         String productId,
                                                         String skuId,
                                                         String preferredBatchNo,
                                                         int quantity) {
        List<InventoryCostLot> matchedLots = INVENTORY_COST_LOT_STORAGE.values().stream()
                .filter(current -> storeId.equals(current.storeId()))
                .filter(current -> productId.equals(current.productId()))
                .filter(current -> skuId.equals(current.skuId()))
                .filter(current -> preferredBatchNo == null || preferredBatchNo.isBlank() || preferredBatchNo.equals(current.batchNo()))
                .filter(current -> current.remainingQty() > 0)
                .sorted((left, right) -> left.effectiveAt().compareTo(right.effectiveAt()))
                .toList();
        int remainingQty = quantity;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<InventoryCostLot> updatedLots = new ArrayList<>();
        for (InventoryCostLot costLot : matchedLots) {
            if (remainingQty <= 0) {
                break;
            }
            int consumedQty = Math.min(remainingQty, costLot.remainingQty());
            BigDecimal consumedAmount = costLot.unitCost().multiply(BigDecimal.valueOf(consumedQty));
            totalAmount = totalAmount.add(consumedAmount);
            updatedLots.add(costLot.withOutbound(consumedQty, consumedAmount));
            remainingQty -= consumedQty;
        }
        if (remainingQty > 0) {
            throw new BusinessException("7427", "insufficient inventory cost lots for outbound", HttpStatus.BAD_REQUEST);
        }
        for (InventoryCostLot updatedLot : updatedLots) {
            INVENTORY_COST_LOT_STORAGE.put(updatedLot.costLotId(), updatedLot);
        }
        return new InventoryCostAllocation(
                quantity,
                totalAmount,
                quantity == 0 ? BigDecimal.ZERO : totalAmount.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP)
        );
    }

    private String generateBatchNo(String orderNo, String productId, String skuId) {
        return "BATCH-" + orderNo + "-" + productId.substring(Math.max(0, productId.length() - 4)) + "-" + skuId;
    }

    private void createPayableLedgerEntry(String storeId,
                                          String supplierId,
                                          String purchaseOrderId,
                                          String purchaseOrderNo,
                                          String bizType,
                                          String relatedId,
                                          String direction,
                                          BigDecimal amount,
                                          OffsetDateTime occurredAt) {
        String sequenceValue = String.valueOf(PAYABLE_LEDGER_SEQUENCE.incrementAndGet());
        PayableLedgerEntry entry = new PayableLedgerEntry(
                "payable-ledger-" + sequenceValue,
                "PLG-" + sequenceValue,
                storeId,
                supplierId,
                purchaseOrderId,
                purchaseOrderNo,
                bizType,
                relatedId,
                direction,
                amount,
                occurredAt
        );
        PAYABLE_LEDGER_STORAGE.put(entry.ledgerEntryId(), entry);
    }

    private SupplierReconciliationView buildSupplierReconciliation(String tenantId, String supplierId) {
        backend.supplier.model.Supplier supplier = supplierRepository.findBySupplierId(supplierId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, supplier.storeId());
        List<PayableLedgerEntry> entries = PAYABLE_LEDGER_STORAGE.values().stream()
                .filter(entry -> supplierId.equals(entry.supplierId()))
                .sorted((left, right) -> right.occurredAt().compareTo(left.occurredAt()))
                .toList();
        BigDecimal receiptAmount = entries.stream()
                .filter(entry -> "payable_increase".equals(entry.direction()))
                .map(PayableLedgerEntry::entryAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal returnAmount = entries.stream()
                .filter(entry -> "payable_decrease".equals(entry.direction()))
                .map(PayableLedgerEntry::entryAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int pendingDiscrepancyCount = PURCHASE_DISCREPANCY_STORAGE.values().stream()
                .filter(discrepancy -> supplier.storeId().equals(discrepancy.storeId()))
                .filter(discrepancy -> "reported".equals(discrepancy.status()))
                .map(PurchaseReceiptDiscrepancy::purchaseOrderId)
                .map(PURCHASE_ORDER_STORAGE::get)
                .filter(order -> order != null && supplierId.equals(order.supplierId()))
                .toList()
                .size();
        return new SupplierReconciliationView(
                supplier.supplierId(),
                supplier.supplierName(),
                supplier.storeId(),
                receiptAmount,
                returnAmount,
                receiptAmount.subtract(returnAmount),
                entries.size(),
                pendingDiscrepancyCount,
                pendingDiscrepancyCount > 0 ? "exception" : "matched"
        );
    }

    private PurchaseCostCollectionView buildPurchaseCostCollection(PurchaseOrder purchaseOrder) {
        BigDecimal receiptAmount = PURCHASE_RECEIPT_STORAGE.values().stream()
                .filter(receipt -> purchaseOrder.purchaseOrderId().equals(receipt.purchaseOrderId()))
                .flatMap(receipt -> receipt.items().stream())
                .map(item -> item.unitCost().multiply(BigDecimal.valueOf(item.receivedQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal returnAmount = PURCHASE_RETURN_STORAGE.values().stream()
                .filter(record -> purchaseOrder.purchaseOrderId().equals(record.purchaseOrderId()))
                .flatMap(record -> record.items().stream())
                .map(item -> unitCostForReturnItem(purchaseOrder.purchaseOrderId(), item)
                        .multiply(BigDecimal.valueOf(item.returnQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal basePurchaseCost = receiptAmount.subtract(returnAmount);
        BigDecimal allocatedExpense = PURCHASE_EXPENSE_ALLOCATION_STORAGE.values().stream()
                .filter(allocation -> purchaseOrder.purchaseOrderId().equals(allocation.purchaseOrderId()))
                .map(PurchaseExpenseAllocation::expenseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int receiptQty = receiptQtyForPurchaseOrder(purchaseOrder.purchaseOrderId());
        int returnedQty = returnQtyForPurchaseOrder(purchaseOrder.purchaseOrderId());
        int netReceiptQty = Math.max(receiptQty - returnedQty, 0);
        BigDecimal totalCollectedCost = basePurchaseCost.add(allocatedExpense);
        BigDecimal unitCollectedCost = netReceiptQty == 0
                ? BigDecimal.ZERO
                : totalCollectedCost.divide(BigDecimal.valueOf(netReceiptQty), 2, RoundingMode.HALF_UP);
        long costLotCount = INVENTORY_COST_LOT_STORAGE.values().stream()
                .filter(lot -> "purchase_receipt".equals(lot.sourceType()))
                .filter(lot -> purchaseOrder.purchaseOrderId().equals(lot.sourceId()))
                .count();
        return new PurchaseCostCollectionView(
                purchaseOrder.purchaseOrderId(),
                purchaseOrder.orderNo(),
                purchaseOrder.storeId(),
                purchaseOrder.supplierId(),
                basePurchaseCost,
                allocatedExpense,
                totalCollectedCost,
                unitCollectedCost,
                netReceiptQty,
                (int) costLotCount
        );
    }

    private int receiptQtyForPurchaseOrder(String purchaseOrderId) {
        return PURCHASE_RECEIPT_STORAGE.values().stream()
                .filter(receipt -> purchaseOrderId.equals(receipt.purchaseOrderId()))
                .flatMap(receipt -> receipt.items().stream())
                .mapToInt(PurchaseReceiptItem::receivedQty)
                .sum();
    }

    private int returnQtyForPurchaseOrder(String purchaseOrderId) {
        return PURCHASE_RETURN_STORAGE.values().stream()
                .filter(record -> purchaseOrderId.equals(record.purchaseOrderId()))
                .flatMap(record -> record.items().stream())
                .mapToInt(PurchaseReturnItem::returnQty)
                .sum();
    }

    private BigDecimal unitCostForReturnItem(String purchaseOrderId, PurchaseReturnItem item) {
        return PURCHASE_RECEIPT_STORAGE.values().stream()
                .filter(receipt -> purchaseOrderId.equals(receipt.purchaseOrderId()))
                .flatMap(receipt -> receipt.items().stream())
                .filter(current -> item.productId().equals(current.productId()))
                .filter(current -> item.skuId().equals(current.skuId()))
                .filter(current -> item.batchNo() == null || item.batchNo().isBlank() || item.batchNo().equals(current.batchNo()))
                .map(PurchaseReceiptItem::unitCost)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private int totalTransactionQty(String storeId, String productId, String skuId, String direction) {
        return INVENTORY_TRANSACTION_STORAGE.values().stream()
                .filter(record -> storeId.equals(record.storeId()))
                .filter(record -> productId.equals(record.productId()))
                .filter(record -> skuId.equals(record.skuId()))
                .filter(record -> direction.equals(record.direction()))
                .mapToInt(InventoryTransactionRecord::quantity)
                .sum();
    }

    private OffsetDateTime lastTransactionAt(String storeId, String productId, String skuId) {
        return INVENTORY_TRANSACTION_STORAGE.values().stream()
                .filter(record -> storeId.equals(record.storeId()))
                .filter(record -> productId.equals(record.productId()))
                .filter(record -> skuId.equals(record.skuId()))
                .map(InventoryTransactionRecord::occurredAt)
                .max(OffsetDateTime::compareTo)
                .orElse(null);
    }

    private String riskLevel(InventorySnapshot snapshot) {
        if (snapshot.availableStock() <= 0) {
            return "out_of_stock";
        }
        if (snapshot.availableStock() <= snapshot.safetyStock()) {
            return "low_stock";
        }
        return "healthy";
    }

    private void requireOwnedSupplier(String tenantId, String supplierId, String expectedStoreId) {
        backend.supplier.model.Supplier supplier = supplierRepository.findBySupplierId(supplierId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, supplier.storeId());
        requireSameStore(expectedStoreId, supplier.storeId(), "supplier does not belong to the target store");
    }

    private PurchaseRequestItem toPurchaseRequestItem(String tenantId,
                                                      String storeId,
                                                      PurchaseRequestItemCommand item) {
        if (item.requestedQty() <= 0) {
            throw new BusinessException("7403", "requested quantity must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        if (item.targetUnitPrice() == null || item.targetUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("7404", "target unit price must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        Product product = requireOwnedProduct(tenantId, item.productId());
        requireSameStore(storeId, product.storeId(), "product does not belong to the target store");
        return new PurchaseRequestItem(
                item.productId(),
                item.skuId(),
                item.requestedQty(),
                item.targetUnitPrice()
        );
    }

    private void requireSameStore(String expectedStoreId, String actualStoreId, String message) {
        if (!expectedStoreId.equals(actualStoreId)) {
            throw new BusinessException("7401", message, HttpStatus.BAD_REQUEST);
        }
    }

    public record CreatePurchaseRequestCommand(
            String storeId,
            String supplierId,
            LocalDate expectedDeliveryDate,
            String reasonText,
            List<PurchaseRequestItemCommand> items
    ) {
    }

    public record PurchaseRequestItemCommand(
            String productId,
            String skuId,
            int requestedQty,
            BigDecimal targetUnitPrice
    ) {
    }

    public record PurchaseRequest(
            String purchaseRequestId,
            String requestNo,
            String storeId,
            String supplierId,
            String requestStatus,
            String approvalStatus,
            String requestedBy,
            LocalDate expectedDeliveryDate,
            int totalRequestedQty,
            String reasonText,
            List<PurchaseRequestItem> items,
            OffsetDateTime createdAt
    ) {
        public PurchaseRequest withWorkflow(String requestStatus, String approvalStatus, String requestedBy) {
            return new PurchaseRequest(
                    purchaseRequestId,
                    requestNo,
                    storeId,
                    supplierId,
                    requestStatus,
                    approvalStatus,
                    requestedBy,
                    expectedDeliveryDate,
                    totalRequestedQty,
                    reasonText,
                    items,
                    createdAt
            );
        }
    }

    public record PurchaseRequestItem(
            String productId,
            String skuId,
            int requestedQty,
            BigDecimal targetUnitPrice
    ) {
    }

    public record CreatePurchaseOrderCommand(
            String purchaseRequestId,
            String remark
    ) {
    }

    public record PurchaseOrder(
            String purchaseOrderId,
            String orderNo,
            String purchaseRequestId,
            String purchaseRequestNo,
            String storeId,
            String supplierId,
            String orderStatus,
            String approvalStatus,
            String dispatchStatus,
            String receivingStatus,
            String inboundStatus,
            String discrepancyStatus,
            String returnStatus,
            String purchaserId,
            LocalDate expectedDeliveryDate,
            int totalRequestedQty,
            List<PurchaseRequestItem> items,
            String remark,
            OffsetDateTime createdAt
    ) {
        public PurchaseOrder withApproval(String orderStatus, String approvalStatus) {
            return new PurchaseOrder(
                    purchaseOrderId,
                    orderNo,
                    purchaseRequestId,
                    purchaseRequestNo,
                    storeId,
                    supplierId,
                    orderStatus,
                    approvalStatus,
                    dispatchStatus,
                    receivingStatus,
                    inboundStatus,
                    discrepancyStatus,
                    returnStatus,
                    purchaserId,
                    expectedDeliveryDate,
                    totalRequestedQty,
                    items,
                    remark,
                    createdAt
            );
        }

        public PurchaseOrder withDispatch(String dispatchStatus, String operatorId) {
            return new PurchaseOrder(
                    purchaseOrderId,
                    orderNo,
                    purchaseRequestId,
                    purchaseRequestNo,
                    storeId,
                    supplierId,
                    "dispatched",
                    approvalStatus,
                    dispatchStatus,
                    receivingStatus,
                    inboundStatus,
                    discrepancyStatus,
                    returnStatus,
                    operatorId,
                    expectedDeliveryDate,
                    totalRequestedQty,
                    items,
                    remark,
                    createdAt
            );
        }

        public PurchaseOrder withReceiving(String receivingStatus,
                                          String inboundStatus,
                                          String discrepancyStatus,
                                          String operatorId) {
            return new PurchaseOrder(
                    purchaseOrderId,
                    orderNo,
                    purchaseRequestId,
                    purchaseRequestNo,
                    storeId,
                    supplierId,
                    "received",
                    approvalStatus,
                    dispatchStatus,
                    receivingStatus,
                    inboundStatus,
                    discrepancyStatus,
                    returnStatus,
                    operatorId,
                    expectedDeliveryDate,
                    totalRequestedQty,
                    items,
                    remark,
                    createdAt
            );
        }

        public PurchaseOrder withReturnStatus(String returnStatus, String operatorId) {
            return new PurchaseOrder(
                    purchaseOrderId,
                    orderNo,
                    purchaseRequestId,
                    purchaseRequestNo,
                    storeId,
                    supplierId,
                    orderStatus,
                    approvalStatus,
                    dispatchStatus,
                    receivingStatus,
                    inboundStatus,
                    discrepancyStatus,
                    returnStatus,
                    operatorId,
                    expectedDeliveryDate,
                    totalRequestedQty,
                    items,
                    remark,
                    createdAt
            );
        }
    }

    public record ReceivePurchaseOrderCommand(
            List<ReceivePurchaseOrderItemCommand> items,
            String remark
    ) {
    }

    public record ReceivePurchaseOrderItemCommand(
            String productId,
            String skuId,
            int receivedQty,
            String batchNo,
            LocalDate productionDate,
            LocalDate expiryDate
    ) {
    }

    public record PurchaseReceipt(
            String purchaseReceiptId,
            String receiptNo,
            String purchaseOrderId,
            String purchaseOrderNo,
            String storeId,
            String supplierId,
            String inboundStatus,
            String receiverId,
            List<PurchaseReceiptItem> items,
            String remark,
            OffsetDateTime receivedAt
    ) {
    }

    public record PurchaseReceiptItem(
            String productId,
            String skuId,
            int receivedQty,
            String batchNo,
            LocalDate productionDate,
            LocalDate expiryDate,
            BigDecimal unitCost,
            OffsetDateTime receivedAt
    ) {
    }

    public record PurchaseReceiptDiscrepancy(
            String discrepancyId,
            String discrepancyNo,
            String purchaseOrderId,
            String purchaseOrderNo,
            String storeId,
            String productId,
            String skuId,
            int orderedQty,
            int receivedQty,
            int discrepancyQty,
            String discrepancyType,
            String status,
            OffsetDateTime reportedAt
    ) {
    }

    public record CreatePurchaseReturnCommand(
            List<CreatePurchaseReturnItemCommand> items,
            String remark
    ) {
    }

    public record CreatePurchaseReturnItemCommand(
            String productId,
            String skuId,
            String batchNo,
            int returnQty,
            String reasonText
    ) {
    }

    public record PurchaseReturnRecord(
            String purchaseReturnId,
            String returnNo,
            String purchaseOrderId,
            String purchaseOrderNo,
            String storeId,
            String supplierId,
            String returnStatus,
            String operatorId,
            List<PurchaseReturnItem> items,
            String remark,
            OffsetDateTime returnedAt
    ) {
    }

    public record PurchaseReturnItem(
            String productId,
            String skuId,
            String batchNo,
            int returnQty,
            String reasonText,
            OffsetDateTime returnedAt
    ) {
    }

    public record CreateInventoryTransferCommand(
            String sourceStoreId,
            String targetStoreId,
            String productId,
            String skuId,
            String batchNo,
            int transferQty,
            String reasonText
    ) {
    }

    public record InventoryTransferRecord(
            String transferId,
            String transferNo,
            String sourceStoreId,
            String targetStoreId,
            String productId,
            String skuId,
            String batchNo,
            int transferQty,
            String transferStatus,
            String operatorId,
            String reasonText,
            OffsetDateTime createdAt,
            OffsetDateTime completedAt
    ) {
        public InventoryTransferRecord withCompletion(String operatorId, OffsetDateTime completedAt) {
            return new InventoryTransferRecord(
                    transferId,
                    transferNo,
                    sourceStoreId,
                    targetStoreId,
                    productId,
                    skuId,
                    batchNo,
                    transferQty,
                    "completed",
                    operatorId,
                    reasonText,
                    createdAt,
                    completedAt
            );
        }
    }

    public record InventoryFreezeRecord(
            String freezeId,
            String freezeNo,
            String storeId,
            String productId,
            String skuId,
            String batchNo,
            String relatedType,
            String relatedId,
            int freezeQty,
            String freezeStatus,
            String operatorId,
            String reasonText,
            OffsetDateTime createdAt,
            OffsetDateTime releasedAt
    ) {
        public InventoryFreezeRecord withStatus(String freezeStatus, OffsetDateTime releasedAt) {
            return new InventoryFreezeRecord(
                    freezeId,
                    freezeNo,
                    storeId,
                    productId,
                    skuId,
                    batchNo,
                    relatedType,
                    relatedId,
                    freezeQty,
                    freezeStatus,
                    operatorId,
                    reasonText,
                    createdAt,
                    releasedAt
            );
        }
    }

    public record InventoryBatch(
            String batchId,
            String batchNo,
            String storeId,
            String productId,
            String skuId,
            String sourceType,
            String sourceId,
            int availableQty,
            int lockedQty,
            int receivedQty,
            LocalDate productionDate,
            LocalDate expiryDate,
            BigDecimal unitCost,
            String batchStatus,
            OffsetDateTime receivedAt
    ) {
        public InventoryBatch withFreeze(int quantity, OffsetDateTime receivedAt) {
            return new InventoryBatch(
                    batchId,
                    batchNo,
                    storeId,
                    productId,
                    skuId,
                    sourceType,
                    sourceId,
                    availableQty - quantity,
                    lockedQty + quantity,
                    receivedQty,
                    productionDate,
                    expiryDate,
                    unitCost,
                    batchStatus,
                    receivedAt
            );
        }

        public InventoryBatch withTransferred(int quantity, OffsetDateTime receivedAt) {
            return new InventoryBatch(
                    batchId,
                    batchNo,
                    storeId,
                    productId,
                    skuId,
                    sourceType,
                    sourceId,
                    availableQty,
                    lockedQty - quantity,
                    receivedQty,
                    productionDate,
                    expiryDate,
                    unitCost,
                    batchStatus,
                    receivedAt
            );
        }

        public InventoryBatch withOutbound(int quantity, OffsetDateTime receivedAt) {
            return new InventoryBatch(
                    batchId,
                    batchNo,
                    storeId,
                    productId,
                    skuId,
                    sourceType,
                    sourceId,
                    availableQty - quantity,
                    lockedQty,
                    receivedQty,
                    productionDate,
                    expiryDate,
                    unitCost,
                    batchStatus,
                    receivedAt
            );
        }
    }

    public record InventoryCostLot(
            String costLotId,
            String costLotNo,
            String storeId,
            String productId,
            String skuId,
            String batchNo,
            String sourceType,
            String sourceId,
            int remainingQty,
            BigDecimal remainingAmount,
            BigDecimal unitCost,
            OffsetDateTime effectiveAt,
            String lotStatus
    ) {
        public InventoryCostLot withOutbound(int quantity, BigDecimal amount) {
            int nextRemainingQty = remainingQty - quantity;
            BigDecimal nextRemainingAmount = remainingAmount.subtract(amount);
            return new InventoryCostLot(
                    costLotId,
                    costLotNo,
                    storeId,
                    productId,
                    skuId,
                    batchNo,
                    sourceType,
                    sourceId,
                    nextRemainingQty,
                    nextRemainingAmount,
                    unitCost,
                    effectiveAt,
                    nextRemainingQty == 0 ? "consumed" : lotStatus
            );
        }
    }

    public record InventoryCostAllocation(
            int quantity,
            BigDecimal amount,
            BigDecimal unitCost
    ) {
    }

    public record PayableLedgerEntry(
            String ledgerEntryId,
            String ledgerNo,
            String storeId,
            String supplierId,
            String purchaseOrderId,
            String purchaseOrderNo,
            String bizType,
            String relatedId,
            String direction,
            BigDecimal entryAmount,
            OffsetDateTime occurredAt
    ) {
    }

    public record SupplierReconciliationView(
            String supplierId,
            String supplierName,
            String storeId,
            BigDecimal receiptAmount,
            BigDecimal returnAmount,
            BigDecimal netPayableAmount,
            int entryCount,
            int pendingDiscrepancyCount,
            String reconciliationStatus
    ) {
    }

    public record CreatePurchaseExpenseAllocationCommand(
            String purchaseOrderId,
            String expenseType,
            String allocationRule,
            BigDecimal expenseAmount,
            String remark
    ) {
    }

    public record PurchaseExpenseAllocation(
            String allocationId,
            String allocationNo,
            String purchaseOrderId,
            String purchaseOrderNo,
            String storeId,
            String supplierId,
            String expenseType,
            String allocationRule,
            BigDecimal expenseAmount,
            int allocatedQty,
            BigDecimal unitAllocatedExpense,
            String operatorId,
            String remark,
            OffsetDateTime allocatedAt
    ) {
    }

    public record PurchaseCostCollectionView(
            String purchaseOrderId,
            String purchaseOrderNo,
            String storeId,
            String supplierId,
            BigDecimal basePurchaseCost,
            BigDecimal allocatedExpense,
            BigDecimal totalCollectedCost,
            BigDecimal unitCollectedCost,
            int netReceiptQty,
            int costLotCount
    ) {
    }

    public record CreateWmsWarehouseCommand(
            String storeId,
            String warehouseName,
            String zoneCode,
            String locationCode,
            String temperatureZone
    ) {
    }

    public record WmsWarehouse(
            String warehouseId,
            String storeId,
            String warehouseCode,
            String warehouseName,
            String zoneCode,
            String locationCode,
            String temperatureZone,
            String warehouseStatus,
            String operatorId,
            OffsetDateTime createdAt
    ) {
    }

    public record CreateWmsInboundTaskCommand(
            String storeId,
            String productId,
            String skuId,
            String batchNo,
            String warehouseCode,
            String zoneCode,
            String locationCode,
            String taskType,
            int quantity
    ) {
    }

    public record WmsInboundTask(
            String inboundTaskId,
            String storeId,
            String productId,
            String skuId,
            String batchNo,
            String warehouseCode,
            String zoneCode,
            String locationCode,
            String taskType,
            int quantity,
            String executionStatus,
            String putawayStatus,
            String operatorId,
            OffsetDateTime createdAt
    ) {
    }

    public record CreateWmsWaveCommand(
            String storeId,
            String warehouseCode,
            String waveType,
            String strategyCode,
            List<WmsWaveItemCommand> items
    ) {
    }

    public record WmsWaveItemCommand(
            String productId,
            String skuId,
            String locationCode,
            int plannedQty
    ) {
    }

    public record WmsWave(
            String waveId,
            String storeId,
            String warehouseCode,
            String waveType,
            String pickStrategy,
            int allocatedQty,
            String waveStatus,
            String operatorId,
            OffsetDateTime createdAt
    ) {
    }

    public record CreateWmsLockCommand(
            String storeId,
            String productId,
            String skuId,
            String batchNo,
            String warehouseCode,
            String locationCode,
            int lockQty,
            String reasonText
    ) {
    }

    public record WmsLockRecord(
            String lockId,
            String storeId,
            String productId,
            String skuId,
            String batchNo,
            String warehouseCode,
            String locationCode,
            int lockQty,
            String lockStatus,
            String reasonText,
            String operatorId,
            OffsetDateTime createdAt
    ) {
    }

    public record CreateWmsCycleCountTaskCommand(
            String storeId,
            String productId,
            String skuId,
            String warehouseCode,
            String locationCode,
            int systemQty,
            int countedQty,
            String varianceReason
    ) {
    }

    public record WmsCycleCountTask(
            String cycleCountTaskId,
            String storeId,
            String productId,
            String skuId,
            String warehouseCode,
            String locationCode,
            int systemQty,
            int countedQty,
            int varianceQty,
            String adjustmentType,
            String countStatus,
            String varianceReason,
            String operatorId,
            OffsetDateTime createdAt
    ) {
    }

    public record CreateWmsReverseInboundCommand(
            String storeId,
            String productId,
            String skuId,
            String warehouseCode,
            String locationCode,
            String reverseType,
            int quantity,
            String remark
    ) {
    }

    public record WmsReverseInboundRecord(
            String reverseInboundId,
            String storeId,
            String productId,
            String skuId,
            String warehouseCode,
            String locationCode,
            String reverseType,
            int quantity,
            String reverseStatus,
            String inventoryStatus,
            String remark,
            String operatorId,
            OffsetDateTime createdAt
    ) {
    }

    public record WmsLinkageView(
            String storeId,
            int warehouseCount,
            int activeWaveCount,
            int lockedBatchCount,
            int cycleCountTaskCount,
            int reverseInboundCount,
            String omsSyncStatus,
            String erpSyncStatus,
            String tmsHandoverStatus
    ) {
    }

    public record InventoryLedger(
            String inventorySnapshotId,
            String storeId,
            String productId,
            String skuId,
            int availableStock,
            int reservedStock,
            int safetyStock,
            int inboundTotal,
            int outboundTotal,
            OffsetDateTime lastTransactionAt,
            String riskLevel
    ) {
    }

    public record InventoryTransactionRecord(
            String transactionId,
            String transactionNo,
            String storeId,
            String productId,
            String skuId,
            String direction,
            String transactionType,
            String relatedId,
            int beforeAvailableStock,
            int afterAvailableStock,
            int quantity,
            OffsetDateTime occurredAt
    ) {
    }

    public static java.util.Optional<PurchaseOrder> findPurchaseOrderForApproval(String purchaseOrderId) {
        return java.util.Optional.ofNullable(PURCHASE_ORDER_STORAGE.get(purchaseOrderId));
    }

    public static void savePurchaseOrderForApproval(PurchaseOrder purchaseOrder) {
        PURCHASE_ORDER_STORAGE.put(purchaseOrder.purchaseOrderId(), purchaseOrder);
    }
}

