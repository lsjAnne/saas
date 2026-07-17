import { apiClient } from '@/services/http/apiClient';
import type {
  CustomerServiceTicket,
  NotificationGatewayConfigView,
  NotificationGatewayOverviewView,
  NotificationTask,
  NotificationTemplateView,
  ReplayNotificationTaskPayload,
  SupportSession,
  TicketSlaOverviewView,
  UpdateNotificationGatewayConfigPayload,
  UpdateNotificationTemplatePayload
} from '@/services/apiTypes';

export function getTickets(token: string) {
  return apiClient.get<CustomerServiceTicket[]>('/api/tickets', token);
}

export function getTicketSlaOverview(token: string) {
  return apiClient.get<TicketSlaOverviewView>('/api/tickets/sla-overview', token);
}

export function getSupportSessions(token: string) {
  return apiClient.get<SupportSession[]>('/api/support-sessions', token);
}

export function getNotifications(token: string) {
  return apiClient.get<NotificationTask[]>('/api/notifications', token);
}

export function getNotificationGatewayOverview(token: string) {
  return apiClient.get<NotificationGatewayOverviewView>(
    '/api/notifications/gateway-overview',
    token
  );
}

export function getNotificationGatewayConfig(token: string) {
  return apiClient.get<NotificationGatewayConfigView>('/api/notification-gateway-config', token);
}

export function updateNotificationGatewayConfig(
  payload: UpdateNotificationGatewayConfigPayload,
  token: string
) {
  return apiClient.put<NotificationGatewayConfigView>('/api/notification-gateway-config', payload, token);
}

export function getNotificationTemplates(token: string) {
  return apiClient.get<NotificationTemplateView[]>('/api/notification-templates', token);
}

export function updateNotificationTemplate(
  notificationTemplateId: string,
  payload: UpdateNotificationTemplatePayload,
  token: string
) {
  return apiClient.put<NotificationTemplateView>(
    `/api/notification-templates/${notificationTemplateId}`,
    payload,
    token
  );
}

export function retryNotificationTask(notificationTaskId: string, token: string) {
  return apiClient.post<NotificationTask>(`/api/notifications/${notificationTaskId}/retry`, null, token);
}

export function replayDeadLetterNotificationTask(
  notificationTaskId: string,
  payload: ReplayNotificationTaskPayload,
  token: string
) {
  return apiClient.post<NotificationTask>(
    `/api/notifications/${notificationTaskId}/dead-letter-replay`,
    payload,
    token
  );
}
