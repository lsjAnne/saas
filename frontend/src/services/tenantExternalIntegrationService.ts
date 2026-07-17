import { apiClient } from '@/services/http/apiClient';
import type { ExternalIntegrationPreferences } from '@/services/apiTypes';

export function getTenantExternalIntegrationPreferences(token: string) {
  return apiClient.get<ExternalIntegrationPreferences>(
    '/api/tenant/external-integrations/preferences',
    token
  );
}

export function updateTenantExternalIntegrationPreferences(
  selectedSystemCodes: string[],
  token: string
) {
  return apiClient.put<ExternalIntegrationPreferences>(
    '/api/tenant/external-integrations/preferences',
    {
      selectedSystemCodes
    },
    token
  );
}
