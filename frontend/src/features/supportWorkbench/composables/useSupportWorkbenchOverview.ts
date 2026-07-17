import { shallowRef } from 'vue';

import {
  getNotificationGatewayOverview,
  getNotifications,
  getSupportSessions,
  getTicketSlaOverview,
  getTickets
} from '@/services/supportWorkbenchService';
import type {
  CustomerServiceTicket,
  NotificationGatewayOverviewView,
  NotificationTask,
  SupportSession,
  TicketSlaOverviewView
} from '@/services/apiTypes';

export function useSupportWorkbenchOverview() {
  const tickets = shallowRef<CustomerServiceTicket[]>([]);
  const ticketSlaOverview = shallowRef<TicketSlaOverviewView | null>(null);
  const supportSessions = shallowRef<SupportSession[]>([]);
  const notifications = shallowRef<NotificationTask[]>([]);
  const gatewayOverview = shallowRef<NotificationGatewayOverviewView | null>(null);
  const isLoading = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [ticketsPayload, slaPayload, sessionsPayload, notificationsPayload, gatewayPayload] =
        await Promise.all([
          getTickets(token),
          getTicketSlaOverview(token),
          getSupportSessions(token),
          getNotifications(token),
          getNotificationGatewayOverview(token)
        ]);

      tickets.value = ticketsPayload;
      ticketSlaOverview.value = slaPayload;
      supportSessions.value = sessionsPayload;
      notifications.value = notificationsPayload;
      gatewayOverview.value = gatewayPayload;
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '支持与工单中心数据加载失败，请稍后重试';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  return {
    tickets,
    ticketSlaOverview,
    supportSessions,
    notifications,
    gatewayOverview,
    isLoading,
    errorMessage,
    load
  };
}
