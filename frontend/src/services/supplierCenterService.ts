import { apiClient } from '@/services/http/apiClient';
import type {
  CreateSupplierAdmissionReviewPayload,
  CreateSupplierDeliveryAppointmentPayload,
  CreateSupplierPayload,
  CreateSupplierRiskEventPayload,
  CreateSupplierScorecardPayload,
  CreateSupplierSettlementStatementPayload,
  Supplier,
  SupplierAdmissionReview,
  SupplierDeliveryAppointment,
  SupplierRiskEvent,
  SupplierScorecard,
  SupplierSettlementStatement,
  SupplierSrmLinkage,
  UpdateSupplierPayload
} from '@/services/apiTypes';

export function getSuppliers(token: string) {
  return apiClient.get<Supplier[]>('/api/suppliers', token);
}

export function getSupplier(supplierId: string, token: string) {
  return apiClient.get<Supplier>(`/api/suppliers/${supplierId}`, token);
}

export function createSupplier(payload: CreateSupplierPayload, token: string) {
  return apiClient.post<Supplier>('/api/suppliers', payload, token);
}

export function updateSupplier(
  supplierId: string,
  payload: UpdateSupplierPayload,
  token: string
) {
  return apiClient.put<Supplier>(`/api/suppliers/${supplierId}`, payload, token);
}

export function setPrimarySupplier(supplierId: string, token: string) {
  return apiClient.post<Supplier>(`/api/suppliers/${supplierId}/set-primary`, null, token);
}

export function setBackupSupplier(supplierId: string, token: string) {
  return apiClient.post<Supplier>(`/api/suppliers/${supplierId}/set-backup`, null, token);
}

export function reviewSupplierAdmission(
  supplierId: string,
  payload: CreateSupplierAdmissionReviewPayload,
  token: string
) {
  return apiClient.post<SupplierAdmissionReview>(
    `/api/suppliers/${supplierId}/admission-review`,
    payload,
    token
  );
}

export function saveSupplierScorecard(
  supplierId: string,
  payload: CreateSupplierScorecardPayload,
  token: string
) {
  return apiClient.post<SupplierScorecard>(
    `/api/suppliers/${supplierId}/scorecards`,
    payload,
    token
  );
}

export function createSupplierDeliveryAppointment(
  supplierId: string,
  payload: CreateSupplierDeliveryAppointmentPayload,
  token: string
) {
  return apiClient.post<SupplierDeliveryAppointment>(
    `/api/suppliers/${supplierId}/delivery-appointments`,
    payload,
    token
  );
}

export function createSupplierRiskEvent(
  supplierId: string,
  payload: CreateSupplierRiskEventPayload,
  token: string
) {
  return apiClient.post<SupplierRiskEvent>(
    `/api/suppliers/${supplierId}/risk-events`,
    payload,
    token
  );
}

export function getSupplierSrmLinkage(supplierId: string, token: string) {
  return apiClient.get<SupplierSrmLinkage>(`/api/suppliers/${supplierId}/srm-linkage`, token);
}

export function createSupplierSettlementStatement(
  payload: CreateSupplierSettlementStatementPayload,
  token: string
) {
  return apiClient.post<SupplierSettlementStatement>(
    '/api/supplier-settlement-statements',
    payload,
    token
  );
}

export function getSupplierSettlementStatements(storeId: string, token: string) {
  return apiClient.get<SupplierSettlementStatement[]>(
    `/api/supplier-settlement-statements?storeId=${encodeURIComponent(storeId)}`,
    token
  );
}
