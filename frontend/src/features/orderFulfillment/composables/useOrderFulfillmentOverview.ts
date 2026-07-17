import { computed, shallowRef } from 'vue';

import {
  auditOrder,
  confirmFulfillmentTask,
  createLogisticsRecord,
  getFulfillmentTask,
  getFulfillmentTasks,
  getLogisticsRecords,
  getOmsWorkbench,
  getOrderDetail,
  getOrderOrchestration,
  getOrders,
  getStandardizedOrders,
  mergeOrders,
  planOrderRoute,
  replayFulfillmentTask,
  retryFulfillmentTask,
  splitOrder,
  syncOrder,
  updateOrderReverseStatus
} from '@/services/orderFulfillmentService';
import { getStores } from '@/services/storeChannelService';
import type {
  CreateLogisticsRecordPayload,
  CreateOrderMergePayload,
  CreateOrderReverseStatusPayload,
  CreateOrderRoutePlanPayload,
  CreateOrderSplitPayload,
  CreateOrderSyncPayload,
  FulfillmentTask,
  LogisticsRecord,
  OmsWorkbenchView,
  OrderAuditReviewView,
  OrderDetailView,
  OrderMain,
  OrderOrchestrationView,
  StandardizedOrderView,
  Store
} from '@/services/apiTypes';

export interface OrderFulfillmentSummary {
  totalOrderCount: number;
  pendingFulfillmentCount: number;
  taskCount: number;
  riskOrderCount: number;
  manualReplayCount: number;
  reverseOrderCount: number;
}

interface LoadOptions {
  preferredOrderId?: string | null;
  preferredFulfillmentTaskId?: string | null;
  canLoadFulfillment?: boolean;
}

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function orderPriority(order: OrderMain, task: FulfillmentTask | null) {
  const orderStatus = normalizeText(order.orderStatus);
  const logisticsStatus = normalizeText(order.logisticsStatus);
  const taskStatus = normalizeText(task?.status);

  if (
    taskStatus.includes('manual') ||
    taskStatus.includes('failed') ||
    taskStatus.includes('replay')
  ) {
    return 5;
  }

  if (
    orderStatus.includes('refund') ||
    logisticsStatus.includes('exception') ||
    logisticsStatus.includes('return')
  ) {
    return 4;
  }

  if (orderStatus.includes('pending') || taskStatus.includes('pending')) {
    return 3;
  }

  if (logisticsStatus.includes('transit') || logisticsStatus.includes('execute')) {
    return 2;
  }

  return 1;
}

function fulfillmentTaskPriority(task: FulfillmentTask) {
  const taskStatus = normalizeText(task.status);

  if (taskStatus.includes('manual') || taskStatus.includes('failed')) {
    return 4;
  }

  if (taskStatus.includes('pending')) {
    return 3;
  }

  if (taskStatus.includes('execute') || taskStatus.includes('retry')) {
    return 2;
  }

  return 1;
}

export function useOrderFulfillmentOverview() {
  const stores = shallowRef<Store[]>([]);
  const orders = shallowRef<OrderMain[]>([]);
  const standardizedOrders = shallowRef<StandardizedOrderView[]>([]);
  const omsWorkbench = shallowRef<OmsWorkbenchView | null>(null);
  const fulfillmentTasks = shallowRef<FulfillmentTask[]>([]);
  const selectedOrderId = shallowRef<string | null>(null);
  const selectedFulfillmentTaskId = shallowRef<string | null>(null);
  const selectedOrderDetail = shallowRef<OrderDetailView | null>(null);
  const selectedOrderOrchestration = shallowRef<OrderOrchestrationView | null>(null);
  const selectedAuditReview = shallowRef<OrderAuditReviewView | null>(null);
  const selectedFulfillmentTask = shallowRef<FulfillmentTask | null>(null);
  const logisticsRecords = shallowRef<LogisticsRecord[]>([]);
  const isLoading = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const ordersById = computed(() => {
    const mapping = new Map<string, OrderMain>();

    for (const order of orders.value) {
      mapping.set(order.orderId, order);
    }

    return mapping;
  });

  const tasksByOrderId = computed(() => {
    const mapping = new Map<string, FulfillmentTask>();

    for (const task of fulfillmentTasks.value) {
      const current = mapping.get(task.orderId);

      if (!current) {
        mapping.set(task.orderId, task);
        continue;
      }

      const nextOrder = ordersById.value.get(task.orderId);
      const currentOrder = ordersById.value.get(current.orderId);
      const nextPriority = nextOrder
        ? orderPriority(nextOrder, task)
        : fulfillmentTaskPriority(task);
      const currentPriority = currentOrder
        ? orderPriority(currentOrder, current)
        : fulfillmentTaskPriority(current);

      if (
        nextPriority > currentPriority ||
        (nextPriority === currentPriority &&
          toTimestamp(task.createdAt) > toTimestamp(current.createdAt))
      ) {
        mapping.set(task.orderId, task);
      }
    }

    return mapping;
  });

  const sortedStores = computed(() =>
    [...stores.value].sort(
      (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const sortedOrders = computed(() =>
    [...orders.value].sort((left, right) => {
      const priorityGap =
        orderPriority(right, tasksByOrderId.value.get(right.orderId) ?? null) -
        orderPriority(left, tasksByOrderId.value.get(left.orderId) ?? null);

      if (priorityGap !== 0) {
        return priorityGap;
      }

      const timeoutGap = toTimestamp(left.timeoutAt) - toTimestamp(right.timeoutAt);
      if (timeoutGap !== 0) {
        return timeoutGap;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const sortedFulfillmentTasks = computed(() =>
    [...fulfillmentTasks.value].sort((left, right) => {
      const retryGap = (right.retryCount ?? 0) - (left.retryCount ?? 0);
      if (retryGap !== 0) {
        return retryGap;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const selectedOrder = computed(
    () => sortedOrders.value.find((item) => item.orderId === selectedOrderId.value) ?? null
  );

  const selectedStandardizedOrder = computed(
    () =>
      standardizedOrders.value.find((item) => item.orderId === selectedOrderId.value) ?? null
  );

  const selectedStore = computed(() => {
    const storeId =
      selectedOrder.value?.storeId ??
      selectedOrderDetail.value?.order.storeId ??
      selectedFulfillmentTask.value?.storeId ??
      null;

    return sortedStores.value.find((item) => item.storeId === storeId) ?? null;
  });

  const summary = computed<OrderFulfillmentSummary>(() => ({
    totalOrderCount: orders.value.length,
    pendingFulfillmentCount: orders.value.filter((item) =>
      normalizeText(item.orderStatus).includes('pending')
    ).length,
    taskCount: fulfillmentTasks.value.length,
    riskOrderCount: omsWorkbench.value?.riskOrderCount ?? 0,
    manualReplayCount: omsWorkbench.value?.manualReplayCount ?? 0,
    reverseOrderCount: omsWorkbench.value?.reverseOrderCount ?? 0
  }));

  function resolveDefaultTask(orderId: string | null | undefined) {
    if (!orderId) {
      return null;
    }

    return (
      sortedFulfillmentTasks.value.find((task) => task.orderId === orderId) ?? null
    );
  }

  function applySelection(
    preferredOrderId?: string | null,
    preferredFulfillmentTaskId?: string | null
  ) {
    const nextOrderId =
      preferredOrderId && orders.value.some((item) => item.orderId === preferredOrderId)
        ? preferredOrderId
        : sortedOrders.value[0]?.orderId ?? null;

    selectedOrderId.value = nextOrderId;

    const nextTask =
      (preferredFulfillmentTaskId &&
        fulfillmentTasks.value.find(
          (item) => item.fulfillmentTaskId === preferredFulfillmentTaskId
        )) ??
      resolveDefaultTask(nextOrderId);

    selectedFulfillmentTaskId.value = nextTask?.fulfillmentTaskId ?? null;
  }

  async function reloadSelectedContext(token: string, canLoadFulfillment: boolean) {
    if (!selectedOrderId.value) {
      selectedOrderDetail.value = null;
      selectedOrderOrchestration.value = null;
      selectedFulfillmentTask.value = null;
      logisticsRecords.value = [];
      return;
    }

    const [detail, orchestration] = await Promise.all([
      getOrderDetail(selectedOrderId.value, token),
      getOrderOrchestration(selectedOrderId.value, token)
    ]);

    selectedOrderDetail.value = detail;
    selectedOrderOrchestration.value = orchestration;

    if (!canLoadFulfillment) {
      selectedFulfillmentTask.value = null;
      selectedFulfillmentTaskId.value = null;
      logisticsRecords.value = [];
      return;
    }

    const nextTaskId =
      selectedFulfillmentTaskId.value ??
      orchestration.fulfillmentTaskId ??
      resolveDefaultTask(selectedOrderId.value)?.fulfillmentTaskId ??
      null;

    if (!nextTaskId) {
      selectedFulfillmentTask.value = null;
      logisticsRecords.value = [];
      return;
    }

    selectedFulfillmentTaskId.value = nextTaskId;

    const [task, records] = await Promise.all([
      getFulfillmentTask(nextTaskId, token),
      getLogisticsRecords(nextTaskId, token)
    ]);

    selectedFulfillmentTask.value = task;
    logisticsRecords.value = records;
  }

  async function load(token: string, options: LoadOptions = {}) {
    const canLoadFulfillment = options.canLoadFulfillment ?? false;

    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [storesPayload, ordersPayload, standardizedPayload, workbenchPayload, tasksPayload] =
        await Promise.all([
          getStores(token),
          getOrders(token),
          getStandardizedOrders(token),
          getOmsWorkbench(token),
          canLoadFulfillment ? getFulfillmentTasks(token) : Promise.resolve([])
        ]);

      stores.value = storesPayload;
      orders.value = ordersPayload;
      standardizedOrders.value = standardizedPayload;
      omsWorkbench.value = workbenchPayload;
      fulfillmentTasks.value = tasksPayload;

      applySelection(
        options.preferredOrderId ?? selectedOrderId.value,
        options.preferredFulfillmentTaskId ?? selectedFulfillmentTaskId.value
      );

      await reloadSelectedContext(token, canLoadFulfillment);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '订单与履约中心加载失败，请稍后重试';
    } finally {
      isLoading.value = false;
    }
  }

  async function selectOrder(
    orderId: string,
    token: string,
    options: Pick<LoadOptions, 'canLoadFulfillment'> = {}
  ) {
    selectedOrderId.value = orderId;
    selectedAuditReview.value = null;
    actionError.value = null;
    selectedFulfillmentTaskId.value =
      resolveDefaultTask(orderId)?.fulfillmentTaskId ?? null;

    try {
      await reloadSelectedContext(token, options.canLoadFulfillment ?? false);
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '读取订单详情失败，请稍后重试';
    }
  }

  async function selectFulfillmentTask(
    fulfillmentTaskId: string,
    token: string,
    options: Pick<LoadOptions, 'canLoadFulfillment'> = {}
  ) {
    const matchedTask =
      fulfillmentTasks.value.find((item) => item.fulfillmentTaskId === fulfillmentTaskId) ?? null;

    selectedFulfillmentTaskId.value = fulfillmentTaskId;
    actionError.value = null;

    if (matchedTask?.orderId) {
      selectedOrderId.value = matchedTask.orderId;
      selectedAuditReview.value = null;
    }

    try {
      await reloadSelectedContext(token, options.canLoadFulfillment ?? false);
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '读取履约任务失败，请稍后重试';
    }
  }

  async function syncSelectedOrder(
    payload: CreateOrderSyncPayload,
    token: string,
    options: Pick<LoadOptions, 'canLoadFulfillment'> = {}
  ) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      const result = await syncOrder(payload, token);
      actionFeedback.value = result.reused
        ? `平台单 ${payload.platformOrderId} 已存在，已回读既有订单 ${result.orderId}。`
        : `已同步平台单 ${payload.platformOrderId}，生成订单 ${result.orderId} 与履约任务 ${result.fulfillmentTaskId}。`;
      await load(token, {
        preferredOrderId: result.orderId,
        preferredFulfillmentTaskId: result.fulfillmentTaskId,
        canLoadFulfillment: options.canLoadFulfillment
      });
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '同步订单失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function auditSelectedOrder(token: string, options: Pick<LoadOptions, 'canLoadFulfillment'> = {}) {
    if (!selectedOrderId.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      const review = await auditOrder(selectedOrderId.value, token);
      selectedAuditReview.value = review;
      actionFeedback.value = `订单 ${review.orderId} 已完成审单，风险级别 ${review.riskLevel}。`;
      await load(token, {
        preferredOrderId: selectedOrderId.value,
        preferredFulfillmentTaskId: selectedFulfillmentTaskId.value,
        canLoadFulfillment: options.canLoadFulfillment
      });
      selectedAuditReview.value = review;
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '执行审单失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function splitSelectedOrder(
    payload: CreateOrderSplitPayload,
    token: string,
    options: Pick<LoadOptions, 'canLoadFulfillment'> = {}
  ) {
    if (!selectedOrderId.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      const result = await splitOrder(selectedOrderId.value, payload, token);
      actionFeedback.value = `订单 ${result.parentOrderId} 已拆成 ${result.children.length} 个子单。`;
      await load(token, {
        preferredOrderId: result.parentOrderId,
        preferredFulfillmentTaskId: selectedFulfillmentTaskId.value,
        canLoadFulfillment: options.canLoadFulfillment
      });
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '拆单失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function mergeSelectedOrders(
    payload: CreateOrderMergePayload,
    token: string,
    options: Pick<LoadOptions, 'canLoadFulfillment'> = {}
  ) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      const result = await mergeOrders(payload, token);
      actionFeedback.value = `已合单 ${result.orderIds.length} 笔订单，合单组 ${result.mergeGroupId} 已生成。`;
      await load(token, {
        preferredOrderId: result.orderIds[0] ?? selectedOrderId.value,
        preferredFulfillmentTaskId: selectedFulfillmentTaskId.value,
        canLoadFulfillment: options.canLoadFulfillment
      });
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '合单失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function planSelectedOrderRoute(
    payload: CreateOrderRoutePlanPayload,
    token: string,
    options: Pick<LoadOptions, 'canLoadFulfillment'> = {}
  ) {
    if (!selectedOrderId.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      const result = await planOrderRoute(selectedOrderId.value, payload, token);
      actionFeedback.value = `订单 ${result.orderId} 已规划到仓 ${result.routeWarehouseCode}。`;
      await load(token, {
        preferredOrderId: result.orderId,
        preferredFulfillmentTaskId: selectedFulfillmentTaskId.value,
        canLoadFulfillment: options.canLoadFulfillment
      });
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '路由规划失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function updateSelectedOrderReverseStatus(
    payload: CreateOrderReverseStatusPayload,
    token: string,
    options: Pick<LoadOptions, 'canLoadFulfillment'> = {}
  ) {
    if (!selectedOrderId.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      const result = await updateOrderReverseStatus(selectedOrderId.value, payload, token);
      actionFeedback.value = `订单 ${result.orderId} 已切到逆向状态 ${result.reverseStatus}。`;
      await load(token, {
        preferredOrderId: result.orderId,
        preferredFulfillmentTaskId: selectedFulfillmentTaskId.value,
        canLoadFulfillment: options.canLoadFulfillment
      });
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '逆向状态更新失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function confirmSelectedTask(token: string, options: Pick<LoadOptions, 'canLoadFulfillment'> = {}) {
    if (!selectedFulfillmentTaskId.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      const task = await confirmFulfillmentTask(selectedFulfillmentTaskId.value, token);
      actionFeedback.value = `履约任务 ${task.fulfillmentTaskId} 已确认，状态切为 ${task.status}。`;
      await load(token, {
        preferredOrderId: task.orderId,
        preferredFulfillmentTaskId: task.fulfillmentTaskId,
        canLoadFulfillment: options.canLoadFulfillment
      });
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '确认履约任务失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function retrySelectedTask(token: string, options: Pick<LoadOptions, 'canLoadFulfillment'> = {}) {
    if (!selectedFulfillmentTaskId.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      const task = await retryFulfillmentTask(selectedFulfillmentTaskId.value, token);
      actionFeedback.value = `履约任务 ${task.fulfillmentTaskId} 已重试，累计重试 ${task.retryCount ?? 0} 次。`;
      await load(token, {
        preferredOrderId: task.orderId,
        preferredFulfillmentTaskId: task.fulfillmentTaskId,
        canLoadFulfillment: options.canLoadFulfillment
      });
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '重试履约任务失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function replaySelectedTask(token: string, options: Pick<LoadOptions, 'canLoadFulfillment'> = {}) {
    if (!selectedFulfillmentTaskId.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      const replay = await replayFulfillmentTask(selectedFulfillmentTaskId.value, token);
      actionFeedback.value = `履约任务 ${replay.fulfillmentTaskId} 已执行异常回放，累计 ${replay.replayCount} 次。`;
      await load(token, {
        preferredOrderId: selectedOrderId.value,
        preferredFulfillmentTaskId: replay.fulfillmentTaskId,
        canLoadFulfillment: options.canLoadFulfillment
      });
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '异常回放失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function createSelectedTaskLogisticsRecord(
    payload: CreateLogisticsRecordPayload,
    token: string,
    options: Pick<LoadOptions, 'canLoadFulfillment'> = {}
  ) {
    if (!selectedFulfillmentTaskId.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      const record = await createLogisticsRecord(selectedFulfillmentTaskId.value, payload, token);
      actionFeedback.value = `已登记物流单号 ${record.trackingNumber}，当前状态 ${record.logisticsStatus}。`;
      await load(token, {
        preferredOrderId: selectedOrderId.value,
        preferredFulfillmentTaskId: record.fulfillmentTaskId,
        canLoadFulfillment: options.canLoadFulfillment
      });
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '登记物流记录失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  return {
    stores: sortedStores,
    orders: sortedOrders,
    standardizedOrders,
    omsWorkbench,
    fulfillmentTasks: sortedFulfillmentTasks,
    summary,
    selectedOrderId,
    selectedOrder,
    selectedStandardizedOrder,
    selectedStore,
    selectedFulfillmentTaskId,
    selectedOrderDetail,
    selectedOrderOrchestration,
    selectedAuditReview,
    selectedFulfillmentTask,
    logisticsRecords,
    isLoading,
    isRunningAction,
    errorMessage,
    actionError,
    actionFeedback,
    load,
    selectOrder,
    selectFulfillmentTask,
    syncSelectedOrder,
    auditSelectedOrder,
    splitSelectedOrder,
    mergeSelectedOrders,
    planSelectedOrderRoute,
    updateSelectedOrderReverseStatus,
    confirmSelectedTask,
    retrySelectedTask,
    replaySelectedTask,
    createSelectedTaskLogisticsRecord
  };
}
