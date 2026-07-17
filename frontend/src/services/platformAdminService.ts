import { apiClient } from '@/services/http/apiClient';
import type {
  AdminComplianceAcceptanceView,
  AdminComplianceDocumentView,
  AdminTenantOverview,
  PlanChangePayload,
  ComplianceDocument,
  ComplianceDocumentPublishPayload,
  DeliveryReadinessView,
  FeatureToggleUpdatePayload,
  ObservabilityReadinessView,
  ReleaseReadinessView,
  SeatPurchasePayload,
  SubscriptionActionPayload,
  SubscriptionAutomationSummary,
  TenantSubscription,
  TenantProfile
} from '@/services/apiTypes';

export function getAdminTenants(token: string) {
  return apiClient.get<AdminTenantOverview[]>('/api/admin/tenants', token);
}

export function updateAdminTenantFeatureToggles(
  tenantId: string,
  payload: FeatureToggleUpdatePayload,
  token: string
) {
  return apiClient.put<TenantProfile>(
    `/api/admin/tenants/${tenantId}/feature-toggles`,
    payload,
    token
  );
}

export function suspendAdminTenant(tenantId: string, token: string) {
  return apiClient.post<TenantProfile>(`/api/admin/tenants/${tenantId}/suspend`, null, token);
}

export function resumeAdminTenant(tenantId: string, token: string) {
  return apiClient.post<TenantProfile>(`/api/admin/tenants/${tenantId}/resume`, null, token);
}

export function startAdminTenantTrial(tenantId: string, token: string) {
  return apiClient.post<TenantProfile>(`/api/tenants/${tenantId}/start-trial`, null, token);
}

export function subscribeAdminTenant(
  tenantId: string,
  payload: SubscriptionActionPayload,
  token: string
) {
  return apiClient.post<TenantSubscription>(
    `/api/admin/tenants/${tenantId}/subscription/subscribe`,
    payload,
    token
  );
}

export function upgradeAdminTenantPlan(
  tenantId: string,
  payload: PlanChangePayload,
  token: string
) {
  return apiClient.post<TenantSubscription>(
    `/api/admin/tenants/${tenantId}/subscription/upgrade`,
    payload,
    token
  );
}

export function downgradeAdminTenantPlan(
  tenantId: string,
  payload: PlanChangePayload,
  token: string
) {
  return apiClient.post<TenantSubscription>(
    `/api/admin/tenants/${tenantId}/subscription/downgrade`,
    payload,
    token
  );
}

export function purchaseAdminTenantSeats(
  tenantId: string,
  payload: SeatPurchasePayload,
  token: string
) {
  return apiClient.post<TenantSubscription>(
    `/api/admin/tenants/${tenantId}/seats/purchase`,
    payload,
    token
  );
}

export function reconcileSubscriptionAutomation(token: string) {
  return apiClient.post<SubscriptionAutomationSummary>(
    '/api/admin/tenants/subscription-automation/reconcile',
    null,
    token
  );
}

export function getReleaseReadiness(tenantId: string, token: string) {
  return apiClient.get<ReleaseReadinessView>(
    `/api/admin/tenants/${tenantId}/release-readiness`,
    token
  );
}

export function getDeliveryReadiness(tenantId: string, token: string) {
  return apiClient.get<DeliveryReadinessView>(
    `/api/admin/tenants/${tenantId}/delivery-readiness`,
    token
  );
}

export function getObservabilityReadiness(tenantId: string, token: string) {
  return apiClient.get<ObservabilityReadinessView>(
    `/api/admin/tenants/${tenantId}/observability-readiness`,
    token
  );
}

export function getAdminComplianceDocuments(token: string) {
  return apiClient.get<AdminComplianceDocumentView[]>(
    '/api/admin/tenants/compliance/documents',
    token
  );
}

export function publishAdminComplianceDocument(
  documentCode: string,
  payload: ComplianceDocumentPublishPayload,
  token: string
) {
  return apiClient.post<ComplianceDocument>(
    `/api/admin/tenants/compliance/documents/${documentCode}/publish`,
    payload,
    token
  );
}

export function getAdminComplianceAcceptances(
  filters: {
    tenantId?: string;
    documentCode?: string;
    acceptanceStatus?: string;
  },
  token: string
) {
  const params = new URLSearchParams();

  if (filters.tenantId) {
    params.set('tenantId', filters.tenantId);
  }

  if (filters.documentCode) {
    params.set('documentCode', filters.documentCode);
  }

  if (filters.acceptanceStatus) {
    params.set('acceptanceStatus', filters.acceptanceStatus);
  }

  const suffix = params.toString();
  return apiClient.get<AdminComplianceAcceptanceView[]>(
    `/api/admin/tenants/compliance/acceptances${suffix ? `?${suffix}` : ''}`,
    token
  );
}
