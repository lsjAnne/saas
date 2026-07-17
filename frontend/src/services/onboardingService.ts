import { apiClient } from '@/services/http/apiClient';
import type {
  PlanChangePayload,
  RegisterTenantRequest,
  RegisterTenantResponse,
  SeatPurchasePayload,
  SubscriptionActionPayload,
  SubscriptionPlan,
  SubscriptionRenewPayload,
  TenantSubscription
} from '@/services/apiTypes';

export function getSubscriptionPlans() {
  return apiClient.get<SubscriptionPlan[]>('/api/subscription-plans');
}

export function registerTenant(payload: RegisterTenantRequest) {
  return apiClient.post<RegisterTenantResponse>('/api/tenants/register', payload);
}

export function subscribeTenant(
  tenantId: string,
  payload: SubscriptionActionPayload,
  token: string
) {
  return apiClient.post<TenantSubscription>(
    `/api/tenants/${tenantId}/subscription/subscribe`,
    payload,
    token
  );
}

export function upgradeTenantPlan(
  tenantId: string,
  payload: PlanChangePayload,
  token: string
) {
  return apiClient.post<TenantSubscription>(
    `/api/tenants/${tenantId}/subscription/upgrade`,
    payload,
    token
  );
}

export function downgradeTenantPlan(
  tenantId: string,
  payload: PlanChangePayload,
  token: string
) {
  return apiClient.post<TenantSubscription>(
    `/api/tenants/${tenantId}/subscription/downgrade`,
    payload,
    token
  );
}

export function renewTenantSubscription(
  tenantId: string,
  payload: SubscriptionRenewPayload,
  token: string
) {
  return apiClient.post<TenantSubscription>(
    `/api/tenants/${tenantId}/subscription/renew`,
    payload,
    token
  );
}

export function purchaseTenantSeats(
  tenantId: string,
  payload: SeatPurchasePayload,
  token: string
) {
  return apiClient.post<TenantSubscription>(
    `/api/tenants/${tenantId}/seats/purchase`,
    payload,
    token
  );
}
