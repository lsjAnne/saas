import { apiClient } from '@/services/http/apiClient';
import type { TenantContextView } from '@/services/apiTypes';

export function getTenantContext(token: string) {
  return apiClient.get<TenantContextView>('/api/tenant/context', token);
}
