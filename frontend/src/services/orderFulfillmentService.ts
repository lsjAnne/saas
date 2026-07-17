import { apiClient } from '@/services/http/apiClient';
import type {
  CreateLogisticsRecordPayload,
  CreateOrderMergePayload,
  CreateOrderReverseStatusPayload,
  CreateOrderRoutePlanPayload,
  CreateOrderSplitPayload,
  CreateOrderSyncPayload,
  FulfillmentReplayView,
  FulfillmentTask,
  LogisticsRecord,
  OmsWorkbenchView,
  OrderAuditReviewView,
  OrderDetailView,
  OrderMain,
  OrderMergeView,
  OrderOrchestrationView,
  OrderReverseStatusView,
  OrderRoutePlanView,
  OrderSplitView,
  OrderSyncResult,
  StandardizedOrderView
} from '@/services/apiTypes';

export function getOrders(token: string) {
  return apiClient.get<OrderMain[]>('/api/orders', token);
}

export function getStandardizedOrders(token: string) {
  return apiClient.get<StandardizedOrderView[]>('/api/orders/oms-standardized', token);
}

export function getOmsWorkbench(token: string) {
  return apiClient.get<OmsWorkbenchView>('/api/orders/oms-workbench', token);
}

export function getOrderDetail(orderId: string, token: string) {
  return apiClient.get<OrderDetailView>(`/api/orders/${orderId}`, token);
}

export function getOrderOrchestration(orderId: string, token: string) {
  return apiClient.get<OrderOrchestrationView>(
    `/api/orders/${orderId}/orchestration-view`,
    token
  );
}

export function auditOrder(orderId: string, token: string) {
  return apiClient.post<OrderAuditReviewView>(`/api/orders/${orderId}/audit-review`, null, token);
}

export function splitOrder(
  orderId: string,
  payload: CreateOrderSplitPayload,
  token: string
) {
  return apiClient.post<OrderSplitView>(`/api/orders/${orderId}/split`, payload, token);
}

export function mergeOrders(payload: CreateOrderMergePayload, token: string) {
  return apiClient.post<OrderMergeView>('/api/orders/merge', payload, token);
}

export function planOrderRoute(
  orderId: string,
  payload: CreateOrderRoutePlanPayload,
  token: string
) {
  return apiClient.post<OrderRoutePlanView>(
    `/api/orders/${orderId}/route-plan`,
    payload,
    token
  );
}

export function updateOrderReverseStatus(
  orderId: string,
  payload: CreateOrderReverseStatusPayload,
  token: string
) {
  return apiClient.post<OrderReverseStatusView>(
    `/api/orders/${orderId}/reverse-status`,
    payload,
    token
  );
}

export function syncOrder(payload: CreateOrderSyncPayload, token: string) {
  return apiClient.post<OrderSyncResult>('/api/orders/sync', payload, token);
}

export function getFulfillmentTasks(token: string) {
  return apiClient.get<FulfillmentTask[]>('/api/fulfillment-tasks', token);
}

export function getFulfillmentTask(fulfillmentTaskId: string, token: string) {
  return apiClient.get<FulfillmentTask>(`/api/fulfillment-tasks/${fulfillmentTaskId}`, token);
}

export function confirmFulfillmentTask(fulfillmentTaskId: string, token: string) {
  return apiClient.post<FulfillmentTask>(
    `/api/fulfillment-tasks/${fulfillmentTaskId}/confirm`,
    null,
    token
  );
}

export function retryFulfillmentTask(fulfillmentTaskId: string, token: string) {
  return apiClient.post<FulfillmentTask>(
    `/api/fulfillment-tasks/${fulfillmentTaskId}/retry`,
    null,
    token
  );
}

export function replayFulfillmentTask(fulfillmentTaskId: string, token: string) {
  return apiClient.post<FulfillmentReplayView>(
    `/api/fulfillment-tasks/${fulfillmentTaskId}/exception-replay`,
    null,
    token
  );
}

export function getLogisticsRecords(fulfillmentTaskId: string, token: string) {
  return apiClient.get<LogisticsRecord[]>(
    `/api/fulfillment-tasks/${fulfillmentTaskId}/logistics-records`,
    token
  );
}

export function createLogisticsRecord(
  fulfillmentTaskId: string,
  payload: CreateLogisticsRecordPayload,
  token: string
) {
  return apiClient.post<LogisticsRecord>(
    `/api/fulfillment-tasks/${fulfillmentTaskId}/logistics-records`,
    payload,
    token
  );
}
