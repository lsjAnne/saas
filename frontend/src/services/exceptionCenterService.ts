import { apiClient } from '@/services/http/apiClient';
import type { ExceptionTask, UpdateExceptionTaskPayload } from '@/services/apiTypes';

export function getExceptions(token: string) {
  return apiClient.get<ExceptionTask[]>('/api/exceptions', token);
}

export function getException(exceptionTaskId: string, token: string) {
  return apiClient.get<ExceptionTask>(`/api/exceptions/${exceptionTaskId}`, token);
}

export function processException(
  exceptionTaskId: string,
  token: string,
  payload: UpdateExceptionTaskPayload
) {
  return apiClient.post<ExceptionTask>(`/api/exceptions/${exceptionTaskId}/process`, payload, token);
}

export function ignoreException(
  exceptionTaskId: string,
  token: string,
  payload: UpdateExceptionTaskPayload
) {
  return apiClient.post<ExceptionTask>(`/api/exceptions/${exceptionTaskId}/ignore`, payload, token);
}

export function escalateException(
  exceptionTaskId: string,
  token: string,
  payload: UpdateExceptionTaskPayload
) {
  return apiClient.post<ExceptionTask>(`/api/exceptions/${exceptionTaskId}/escalate`, payload, token);
}
