import { apiClient } from '@/services/http/apiClient';
import type {
  AfterSaleRecord,
  CreateAfterSalePayload,
  CustomerServiceTicket,
  SaveTicketSatisfactionPayload,
  TicketSatisfactionView,
  TicketSlaOverviewView
} from '@/services/apiTypes';

function buildQuery(
  query: Record<string, string | number | undefined | null>
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

export function getTickets(token: string) {
  return apiClient.get<CustomerServiceTicket[]>('/api/tickets', token);
}

export function getTicket(ticketId: string, token: string) {
  return apiClient.get<CustomerServiceTicket>(`/api/tickets/${ticketId}`, token);
}

export function generateTicketReplySuggestion(ticketId: string, token: string) {
  return apiClient.post<CustomerServiceTicket>(
    `/api/tickets/${ticketId}/reply-suggestion`,
    null,
    token
  );
}

export function saveTicketSatisfaction(
  ticketId: string,
  payload: SaveTicketSatisfactionPayload,
  token: string
) {
  return apiClient.post<TicketSatisfactionView>(
    `/api/tickets/${ticketId}/satisfaction`,
    payload,
    token
  );
}

export function getTicketSlaOverview(storeId: string | undefined, token: string) {
  return apiClient.get<TicketSlaOverviewView>(
    `/api/tickets/sla-overview${buildQuery({ storeId })}`,
    token
  );
}

export function createAfterSale(payload: CreateAfterSalePayload, token: string) {
  return apiClient.post<AfterSaleRecord>('/api/after-sales', payload, token);
}

export function getAfterSale(afterSaleId: string, token: string) {
  return apiClient.get<AfterSaleRecord>(`/api/after-sales/${afterSaleId}`, token);
}

export function submitAfterSaleApproval(afterSaleId: string, token: string) {
  return apiClient.post<AfterSaleRecord>(
    `/api/after-sales/${afterSaleId}/submit-approval`,
    null,
    token
  );
}
