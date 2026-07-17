import { apiClient } from '@/services/http/apiClient';
import type {
  CreatePurchaseOrderPayload,
  CreateReplenishmentTaskPayload,
  InventorySnapshot,
  PayableLedgerEntry,
  PurchaseCostCollectionView,
  PurchaseOrder,
  PurchaseReceipt,
  PurchaseReceiptDiscrepancy,
  PurchaseRequest,
  ReplenishmentTask,
  SupplierReconciliationView,
  UpdateInventorySafetyStockPayload,
  WmsLinkageView
} from '@/services/apiTypes';

export function getInventorySnapshots(token: string) {
  return apiClient.get<InventorySnapshot[]>('/api/inventory-snapshots', token);
}

export function updateInventorySafetyStock(
  inventorySnapshotId: string,
  payload: UpdateInventorySafetyStockPayload,
  token: string
) {
  return apiClient.put<InventorySnapshot>(
    `/api/inventory-snapshots/${inventorySnapshotId}/safety-stock`,
    payload,
    token
  );
}

export function getReplenishmentTasks(token: string) {
  return apiClient.get<ReplenishmentTask[]>('/api/replenishment-tasks', token);
}

export function createReplenishmentTask(
  payload: CreateReplenishmentTaskPayload,
  token: string
) {
  return apiClient.post<ReplenishmentTask>('/api/replenishment-tasks', payload, token);
}

export function submitReplenishmentApproval(
  replenishmentTaskId: string,
  token: string
) {
  return apiClient.post<ReplenishmentTask>(
    `/api/replenishment-tasks/${replenishmentTaskId}/submit-approval`,
    null,
    token
  );
}

export function getPurchaseRequests(token: string) {
  return apiClient.get<PurchaseRequest[]>('/api/purchase-requests', token);
}

export function getPurchaseOrders(token: string) {
  return apiClient.get<PurchaseOrder[]>('/api/purchase-orders', token);
}

export function createPurchaseOrder(payload: CreatePurchaseOrderPayload, token: string) {
  return apiClient.post<PurchaseOrder>('/api/purchase-orders', payload, token);
}

export function submitPurchaseOrderApproval(purchaseOrderId: string, token: string) {
  return apiClient.post<PurchaseOrder>(
    `/api/purchase-orders/${purchaseOrderId}/submit-approval`,
    null,
    token
  );
}

export function dispatchPurchaseOrder(purchaseOrderId: string, token: string) {
  return apiClient.post<PurchaseOrder>(
    `/api/purchase-orders/${purchaseOrderId}/dispatch`,
    null,
    token
  );
}

export function getPurchaseReceipts(token: string) {
  return apiClient.get<PurchaseReceipt[]>('/api/purchase-receipts', token);
}

export function getPurchaseReceiptDiscrepancies(token: string) {
  return apiClient.get<PurchaseReceiptDiscrepancy[]>(
    '/api/purchase-receipt-discrepancies',
    token
  );
}

export function getPayableLedgers(token: string) {
  return apiClient.get<PayableLedgerEntry[]>('/api/payable-ledgers', token);
}

export function getSupplierReconciliations(token: string) {
  return apiClient.get<SupplierReconciliationView[]>('/api/supplier-reconciliations', token);
}

export function getPurchaseCostCollections(token: string) {
  return apiClient.get<PurchaseCostCollectionView[]>('/api/purchase-cost-collections', token);
}

export function getWmsLinkage(storeId: string, token: string) {
  return apiClient.get<WmsLinkageView>(
    `/api/wms/linkage?storeId=${encodeURIComponent(storeId)}`,
    token
  );
}
