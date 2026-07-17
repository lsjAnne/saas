import { apiClient } from '@/services/http/apiClient';
import type { AuditLogRecord } from '@/services/apiTypes';

export function getTenantAuditLogs(token: string) {
  return apiClient.get<AuditLogRecord[]>('/api/tenant/audit-logs', token);
}
