import { apiClient } from '@/services/http/apiClient';
import type {
  BillingOrder,
  InvoiceRequest,
  InvoiceRequestPayload,
  ObservabilityOverview,
  TenantSubscription,
  UsageQuota
} from '@/services/apiTypes';

export function getTenantObservabilityOverview(token: string) {
  return apiClient.get<ObservabilityOverview>('/api/tenant/system/observability-overview', token);
}

export function getTenantSubscription(tenantId: string, token: string) {
  return apiClient.get<TenantSubscription>(`/api/tenants/${tenantId}/subscription`, token);
}

export function getTenantQuotas(tenantId: string, token: string) {
  return apiClient.get<UsageQuota[]>(`/api/tenants/${tenantId}/quotas`, token);
}

export function getTenantBillingOrders(tenantId: string, token: string) {
  return apiClient.get<BillingOrder[]>(`/api/tenants/${tenantId}/billing-orders`, token);
}

export function settleTenantBillingOrder(tenantId: string, billingOrderId: string, token: string) {
  return apiClient.post<BillingOrder>(
    `/api/tenants/${tenantId}/billing-orders/${billingOrderId}/settle`,
    null,
    token
  );
}

export function createTenantInvoiceRequest(
  tenantId: string,
  payload: InvoiceRequestPayload,
  token: string
) {
  return apiClient.post<InvoiceRequest>(
    `/api/tenants/${tenantId}/invoice-requests`,
    payload,
    token
  );
}
