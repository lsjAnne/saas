import { apiClient } from '@/services/http/apiClient';
import type {
  AutomationAction,
  AutomationActionQuery,
  AutomationApprovalQuery,
  AutomationApprovalTask,
  AutomationExecution,
  AutomationExecutionQuery,
  AutomationOrderSyncTriggerResult,
  AutomationPage,
  AutomationRule,
  AutomationStats,
  CreateAutomationActionPayload,
  UpdateAutomationActionPayload
} from '@/services/automationTypes';

function buildQuery(
  query: Record<string, string | number | boolean | undefined | null>
): string {
  const searchParams = new URLSearchParams();

  Object.entries(query).forEach(([key, value]) => {
    if (value == null || value === '') {
      return;
    }

    searchParams.set(key, String(value));
  });

  const serialized = searchParams.toString();
  return serialized ? `?${serialized}` : '';
}

export function getAutomationRules(token: string) {
  return apiClient.get<AutomationRule[]>('/api/automation/rules?enabled=true', token);
}

export function getAutomationActions(query: AutomationActionQuery, token: string) {
  return apiClient.get<AutomationPage<AutomationAction>>(
    `/api/automation/actions${buildQuery(query)}`,
    token
  );
}

export function createAutomationAction(payload: CreateAutomationActionPayload, token: string) {
  return apiClient.post<AutomationAction>('/api/automation/actions', payload, token);
}

export function updateAutomationAction(
  actionId: string,
  payload: UpdateAutomationActionPayload,
  token: string
) {
  return apiClient.put<AutomationAction>(
    `/api/automation/actions/${actionId}`,
    payload,
    token
  );
}

export function deleteAutomationAction(actionId: string, token: string) {
  return apiClient.delete<void>(`/api/automation/actions/${actionId}`, token);
}

export function getAutomationStats(storeId: string | undefined, token: string) {
  return apiClient.get<AutomationStats>(
    `/api/automation/stats${buildQuery({ storeId })}`,
    token
  );
}

export function getAutomationExecutions(query: AutomationExecutionQuery, token: string) {
  return apiClient.get<AutomationPage<AutomationExecution>>(
    `/api/automation/executions${buildQuery(query)}`,
    token
  );
}

export function getAutomationExecution(executionId: string, token: string) {
  return apiClient.get<AutomationExecution>(
    `/api/automation/executions/${executionId}`,
    token
  );
}

export function getAutomationApprovals(query: AutomationApprovalQuery, token: string) {
  return apiClient.get<AutomationPage<AutomationApprovalTask>>(
    `/api/automation/approvals${buildQuery({ status: 'PENDING', ...query })}`,
    token
  );
}

export function approveAutomationTask(
  taskId: string,
  comment: string | undefined,
  token: string
) {
  return apiClient.post<void>(
    `/api/automation/approvals/${taskId}/approve`,
    { comment },
    token
  );
}

export function rejectAutomationTask(
  taskId: string,
  comment: string | undefined,
  token: string
) {
  return apiClient.post<void>(
    `/api/automation/approvals/${taskId}/reject`,
    { comment },
    token
  );
}

export function triggerAutomationOrderSync(storeId: string, token: string) {
  return apiClient.post<AutomationOrderSyncTriggerResult>(
    `/api/automation/trigger/order-sync${buildQuery({ storeId })}`,
    null,
    token
  );
}
