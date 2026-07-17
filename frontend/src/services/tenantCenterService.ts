import { apiClient } from '@/services/http/apiClient';
import type {
  ComplianceAcceptance,
  ComplianceDocument,
  TenantCleanupTaskView,
  TenantDataExportTaskSummary
} from '@/services/apiTypes';

export function getComplianceAcceptances(token: string) {
  return apiClient.get<ComplianceAcceptance[]>('/api/compliance/acceptances', token);
}

export function getPrivacyPolicy(token: string) {
  return apiClient.get<ComplianceDocument>('/api/compliance/privacy-policy', token);
}

export function getUserAgreement(token: string) {
  return apiClient.get<ComplianceDocument>('/api/compliance/user-agreement', token);
}

export function getTenantDataExports(token: string) {
  return apiClient.get<TenantDataExportTaskSummary[]>('/api/tenant/data-exports', token);
}

export function getTenantCleanupTasks(tenantId: string, token: string) {
  return apiClient.get<TenantCleanupTaskView[]>(`/api/tenants/${tenantId}/cleanup-tasks`, token);
}
