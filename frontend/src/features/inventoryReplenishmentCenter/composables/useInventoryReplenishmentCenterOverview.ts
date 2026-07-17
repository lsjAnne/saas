import { computed, shallowRef } from 'vue';

import {
  createPurchaseOrder,
  createReplenishmentTask,
  dispatchPurchaseOrder,
  getInventorySnapshots,
  getPayableLedgers,
  getPurchaseCostCollections,
  getPurchaseOrders,
  getPurchaseReceiptDiscrepancies,
  getPurchaseReceipts,
  getPurchaseRequests,
  getReplenishmentTasks,
  getSupplierReconciliations,
  getWmsLinkage,
  submitPurchaseOrderApproval,
  submitReplenishmentApproval,
  updateInventorySafetyStock
} from '@/services/inventoryReplenishmentCenterService';
import { getStores } from '@/services/storeChannelService';
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
  Store,
  SupplierReconciliationView,
  UpdateInventorySafetyStockPayload,
  WmsLinkageView
} from '@/services/apiTypes';

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function snapshotPriority(snapshot: InventorySnapshot) {
  if (snapshot.availableStock <= snapshot.safetyStock) {
    return 5;
  }

  if (snapshot.reservedStock > 0) {
    return 3;
  }

  return 1;
}

function taskPriority(task: ReplenishmentTask) {
  const status = normalizeText(task.taskStatus);

  if (status === 'pending_approval') {
    return 5;
  }

  if (status === 'draft') {
    return 4;
  }

  return 1;
}

function orderPriority(order: PurchaseOrder) {
  const orderStatus = normalizeText(order.orderStatus);
  const dispatchStatus = normalizeText(order.dispatchStatus);

  if (orderStatus === 'pending_approval') {
    return 5;
  }

  if (orderStatus === 'approved' && dispatchStatus !== 'dispatched') {
    return 4;
  }

  if (orderStatus === 'draft') {
    return 3;
  }

  return 1;
}

export function useInventoryReplenishmentCenterOverview() {
  const stores = shallowRef<Store[]>([]);
  const inventorySnapshots = shallowRef<InventorySnapshot[]>([]);
  const replenishmentTasks = shallowRef<ReplenishmentTask[]>([]);
  const purchaseRequests = shallowRef<PurchaseRequest[]>([]);
  const purchaseOrders = shallowRef<PurchaseOrder[]>([]);
  const purchaseReceipts = shallowRef<PurchaseReceipt[]>([]);
  const purchaseReceiptDiscrepancies = shallowRef<PurchaseReceiptDiscrepancy[]>([]);
  const payableLedgers = shallowRef<PayableLedgerEntry[]>([]);
  const supplierReconciliations = shallowRef<SupplierReconciliationView[]>([]);
  const purchaseCostCollections = shallowRef<PurchaseCostCollectionView[]>([]);
  const wmsLinkage = shallowRef<WmsLinkageView | null>(null);
  const selectedSnapshotId = shallowRef<string | null>(null);
  const selectedTaskId = shallowRef<string | null>(null);
  const selectedPurchaseRequestId = shallowRef<string | null>(null);
  const selectedPurchaseOrderId = shallowRef<string | null>(null);
  const activeStoreId = shallowRef('all');
  const isLoading = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const sortedStores = computed(() =>
    [...stores.value].sort(
      (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const sortedSnapshots = computed(() =>
    [...inventorySnapshots.value].sort((left, right) => {
      const priorityGap = snapshotPriority(right) - snapshotPriority(left);
      if (priorityGap !== 0) {
        return priorityGap;
      }

      return toTimestamp(right.snapshotAt) - toTimestamp(left.snapshotAt);
    })
  );

  const filteredSnapshots = computed(() =>
    sortedSnapshots.value.filter((snapshot) =>
      activeStoreId.value === 'all' ? true : snapshot.storeId === activeStoreId.value
    )
  );

  const sortedTasks = computed(() =>
    [...replenishmentTasks.value].sort((left, right) => {
      const priorityGap = taskPriority(right) - taskPriority(left);
      if (priorityGap !== 0) {
        return priorityGap;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const filteredTasks = computed(() =>
    sortedTasks.value.filter((task) =>
      activeStoreId.value === 'all' ? true : task.storeId === activeStoreId.value
    )
  );

  const sortedPurchaseRequests = computed(() =>
    [...purchaseRequests.value].sort(
      (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const filteredPurchaseRequests = computed(() =>
    sortedPurchaseRequests.value.filter((request) =>
      activeStoreId.value === 'all' ? true : request.storeId === activeStoreId.value
    )
  );

  const sortedPurchaseOrders = computed(() =>
    [...purchaseOrders.value].sort((left, right) => {
      const priorityGap = orderPriority(right) - orderPriority(left);
      if (priorityGap !== 0) {
        return priorityGap;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const filteredPurchaseOrders = computed(() =>
    sortedPurchaseOrders.value.filter((order) =>
      activeStoreId.value === 'all' ? true : order.storeId === activeStoreId.value
    )
  );

  const filteredPurchaseReceipts = computed(() =>
    purchaseReceipts.value.filter((receipt) =>
      activeStoreId.value === 'all' ? true : receipt.storeId === activeStoreId.value
    )
  );

  const filteredPurchaseDiscrepancies = computed(() =>
    purchaseReceiptDiscrepancies.value.filter((item) =>
      activeStoreId.value === 'all' ? true : item.storeId === activeStoreId.value
    )
  );

  const filteredPayableLedgers = computed(() =>
    payableLedgers.value.filter((entry) =>
      activeStoreId.value === 'all' ? true : entry.storeId === activeStoreId.value
    )
  );

  const filteredSupplierReconciliations = computed(() =>
    supplierReconciliations.value.filter((item) =>
      activeStoreId.value === 'all' ? true : item.storeId === activeStoreId.value
    )
  );

  const filteredPurchaseCostCollections = computed(() =>
    purchaseCostCollections.value.filter((item) =>
      activeStoreId.value === 'all' ? true : item.storeId === activeStoreId.value
    )
  );

  const selectedSnapshot = computed(
    () =>
      filteredSnapshots.value.find(
        (snapshot) => snapshot.inventorySnapshotId === selectedSnapshotId.value
      ) ??
      sortedSnapshots.value.find(
        (snapshot) => snapshot.inventorySnapshotId === selectedSnapshotId.value
      ) ??
      null
  );

  const selectedTask = computed(
    () =>
      filteredTasks.value.find((task) => task.replenishmentTaskId === selectedTaskId.value) ??
      sortedTasks.value.find((task) => task.replenishmentTaskId === selectedTaskId.value) ??
      null
  );

  const selectedPurchaseRequest = computed(
    () =>
      filteredPurchaseRequests.value.find(
        (request) => request.purchaseRequestId === selectedPurchaseRequestId.value
      ) ??
      sortedPurchaseRequests.value.find(
        (request) => request.purchaseRequestId === selectedPurchaseRequestId.value
      ) ??
      null
  );

  const selectedPurchaseOrder = computed(
    () =>
      filteredPurchaseOrders.value.find(
        (order) => order.purchaseOrderId === selectedPurchaseOrderId.value
      ) ??
      sortedPurchaseOrders.value.find(
        (order) => order.purchaseOrderId === selectedPurchaseOrderId.value
      ) ??
      null
  );

  const currentStoreFocusId = computed(() => {
    if (activeStoreId.value !== 'all') {
      return activeStoreId.value;
    }

    return (
      selectedSnapshot.value?.storeId ??
      selectedPurchaseOrder.value?.storeId ??
      filteredSnapshots.value[0]?.storeId ??
      filteredPurchaseOrders.value[0]?.storeId ??
      sortedStores.value[0]?.storeId ??
      null
    );
  });

  const selectedStoreName = computed(() => {
    if (activeStoreId.value === 'all') {
      return currentStoreFocusId.value
        ? sortedStores.value.find((store) => store.storeId === currentStoreFocusId.value)?.shopName ??
            '全部店铺'
        : '全部店铺';
    }

    return (
      sortedStores.value.find((store) => store.storeId === activeStoreId.value)?.shopName ??
      activeStoreId.value
    );
  });

  const summary = computed(() => ({
    totalSnapshots: filteredSnapshots.value.length,
    lowStockCount: filteredSnapshots.value.filter(
      (snapshot) => snapshot.availableStock <= snapshot.safetyStock
    ).length,
    pendingApprovalTaskCount: filteredTasks.value.filter(
      (task) => normalizeText(task.taskStatus) === 'pending_approval'
    ).length,
    pendingApprovalOrderCount: filteredPurchaseOrders.value.filter(
      (order) => normalizeText(order.orderStatus) === 'pending_approval'
    ).length,
    discrepancyCount: filteredPurchaseDiscrepancies.value.filter(
      (item) => normalizeText(item.status) !== 'none'
    ).length,
    payableAmount: filteredPayableLedgers.value.reduce((sum, item) => {
      const signed = normalizeText(item.direction) === 'credit' ? -item.entryAmount : item.entryAmount;
      return sum + signed;
    }, 0)
  }));

  function applySelections() {
    selectedSnapshotId.value =
      filteredSnapshots.value.find(
        (snapshot) => snapshot.inventorySnapshotId === selectedSnapshotId.value
      )?.inventorySnapshotId ??
      filteredSnapshots.value[0]?.inventorySnapshotId ??
      null;

    selectedTaskId.value =
      filteredTasks.value.find((task) => task.replenishmentTaskId === selectedTaskId.value)
        ?.replenishmentTaskId ??
      filteredTasks.value[0]?.replenishmentTaskId ??
      null;

    selectedPurchaseRequestId.value =
      filteredPurchaseRequests.value.find(
        (request) => request.purchaseRequestId === selectedPurchaseRequestId.value
      )?.purchaseRequestId ??
      filteredPurchaseRequests.value[0]?.purchaseRequestId ??
      null;

    selectedPurchaseOrderId.value =
      filteredPurchaseOrders.value.find(
        (order) => order.purchaseOrderId === selectedPurchaseOrderId.value
      )?.purchaseOrderId ??
      filteredPurchaseOrders.value[0]?.purchaseOrderId ??
      null;
  }

  async function refreshCollections(token: string) {
    const [
      snapshotsPayload,
      tasksPayload,
      purchaseRequestsPayload,
      purchaseOrdersPayload,
      purchaseReceiptsPayload,
      discrepanciesPayload,
      payableLedgersPayload,
      reconciliationsPayload,
      purchaseCostCollectionsPayload
    ] = await Promise.all([
      getInventorySnapshots(token),
      getReplenishmentTasks(token),
      getPurchaseRequests(token),
      getPurchaseOrders(token),
      getPurchaseReceipts(token),
      getPurchaseReceiptDiscrepancies(token),
      getPayableLedgers(token),
      getSupplierReconciliations(token),
      getPurchaseCostCollections(token)
    ]);

    inventorySnapshots.value = snapshotsPayload;
    replenishmentTasks.value = tasksPayload;
    purchaseRequests.value = purchaseRequestsPayload;
    purchaseOrders.value = purchaseOrdersPayload;
    purchaseReceipts.value = purchaseReceiptsPayload;
    purchaseReceiptDiscrepancies.value = discrepanciesPayload;
    payableLedgers.value = payableLedgersPayload;
    supplierReconciliations.value = reconciliationsPayload;
    purchaseCostCollections.value = purchaseCostCollectionsPayload;
    applySelections();
  }

  async function refreshWmsContext(token: string) {
    if (!currentStoreFocusId.value) {
      wmsLinkage.value = null;
      return;
    }

    wmsLinkage.value = await getWmsLinkage(currentStoreFocusId.value, token);
  }

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const storesPayload = await getStores(token);
      stores.value = storesPayload;

      if (
        activeStoreId.value !== 'all' &&
        !storesPayload.some((store) => store.storeId === activeStoreId.value)
      ) {
        activeStoreId.value = storesPayload[0]?.storeId ?? 'all';
      }

      if (activeStoreId.value === 'all' && storesPayload.length === 1) {
        activeStoreId.value = storesPayload[0].storeId;
      }

      await refreshCollections(token);
      await refreshWmsContext(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '库存与补货中心加载失败，请稍后重试';
    } finally {
      isLoading.value = false;
    }
  }

  async function selectStore(storeId: string, token: string) {
    activeStoreId.value = storeId;
    applySelections();
    await refreshWmsContext(token);
  }

  async function selectSnapshot(snapshotId: string, token: string) {
    selectedSnapshotId.value = snapshotId;
    await refreshWmsContext(token);
  }

  function selectTask(taskId: string) {
    selectedTaskId.value = taskId;
  }

  async function selectPurchaseRequest(purchaseRequestId: string, token: string) {
    selectedPurchaseRequestId.value = purchaseRequestId;
    await refreshWmsContext(token);
  }

  async function selectPurchaseOrder(purchaseOrderId: string, token: string) {
    selectedPurchaseOrderId.value = purchaseOrderId;
    await refreshWmsContext(token);
  }

  async function runAction(callback: () => Promise<void>) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      await callback();
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '库存补货动作执行失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitSafetyStock(
    payload: UpdateInventorySafetyStockPayload,
    token: string
  ) {
    if (!selectedSnapshotId.value) {
      return;
    }

    await runAction(async () => {
      const updated = await updateInventorySafetyStock(selectedSnapshotId.value!, payload, token);
      actionFeedback.value = `已把 ${updated.skuId} 的安全库存调整到 ${updated.safetyStock}`;
      await refreshCollections(token);
      await refreshWmsContext(token);
      selectedSnapshotId.value = updated.inventorySnapshotId;
    });
  }

  async function submitCreateReplenishmentTask(
    payload: CreateReplenishmentTaskPayload,
    token: string
  ) {
    await runAction(async () => {
      const created = await createReplenishmentTask(payload, token);
      actionFeedback.value = `已创建补货任务 ${created.replenishmentTaskId}`;
      await refreshCollections(token);
      await refreshWmsContext(token);
      selectedTaskId.value = created.replenishmentTaskId;
    });
  }

  async function submitReplenishmentTaskApproval(token: string) {
    if (!selectedTaskId.value) {
      return;
    }

    await runAction(async () => {
      const updated = await submitReplenishmentApproval(selectedTaskId.value!, token);
      actionFeedback.value = `已提交补货任务 ${updated.replenishmentTaskId} 审批`;
      await refreshCollections(token);
      selectedTaskId.value = updated.replenishmentTaskId;
    });
  }

  async function submitCreatePurchaseOrder(
    payload: CreatePurchaseOrderPayload,
    token: string
  ) {
    await runAction(async () => {
      const created = await createPurchaseOrder(payload, token);
      actionFeedback.value = `已生成采购单 ${created.orderNo}`;
      await refreshCollections(token);
      await refreshWmsContext(token);
      selectedPurchaseOrderId.value = created.purchaseOrderId;
    });
  }

  async function submitPurchaseOrderReview(token: string) {
    if (!selectedPurchaseOrderId.value) {
      return;
    }

    await runAction(async () => {
      const updated = await submitPurchaseOrderApproval(selectedPurchaseOrderId.value!, token);
      actionFeedback.value = `已提交采购单 ${updated.orderNo} 审批`;
      await refreshCollections(token);
      selectedPurchaseOrderId.value = updated.purchaseOrderId;
    });
  }

  async function submitPurchaseOrderDispatch(token: string) {
    if (!selectedPurchaseOrderId.value) {
      return;
    }

    await runAction(async () => {
      const updated = await dispatchPurchaseOrder(selectedPurchaseOrderId.value!, token);
      actionFeedback.value = `已下发采购单 ${updated.orderNo}`;
      await refreshCollections(token);
      selectedPurchaseOrderId.value = updated.purchaseOrderId;
    });
  }

  return {
    stores: sortedStores,
    inventorySnapshots: sortedSnapshots,
    filteredSnapshots,
    filteredTasks,
    filteredPurchaseRequests,
    filteredPurchaseOrders,
    filteredPurchaseReceipts,
    filteredPurchaseDiscrepancies,
    filteredPayableLedgers,
    filteredSupplierReconciliations,
    filteredPurchaseCostCollections,
    wmsLinkage,
    selectedSnapshotId,
    selectedTaskId,
    selectedPurchaseRequestId,
    selectedPurchaseOrderId,
    selectedSnapshot,
    selectedTask,
    selectedPurchaseRequest,
    selectedPurchaseOrder,
    selectedStoreName,
    summary,
    activeStoreId,
    isLoading,
    isRunningAction,
    errorMessage,
    actionError,
    actionFeedback,
    load,
    selectStore,
    selectSnapshot,
    selectTask,
    selectPurchaseRequest,
    selectPurchaseOrder,
    submitSafetyStock,
    submitCreateReplenishmentTask,
    submitReplenishmentTaskApproval,
    submitCreatePurchaseOrder,
    submitPurchaseOrderReview,
    submitPurchaseOrderDispatch
  };
}
