import { apiClient } from '@/services/http/apiClient';
import type {
  IntegrationCredentialView,
  IssuedIntegrationCredential,
  IntegrationAuditOverviewView,
  OpenPlatformCallLog,
  OpenPlatformOverviewView,
  PluginGovernanceOverviewView,
  PluginApp,
  WebhookOrchestrationView,
  WebhookSubscription
} from '@/services/apiTypes';

export interface CreateOpenPlatformAppPayload {
  organizationId: string;
  appName: string;
  appType: string;
  permissionScope: string[];
}

export interface CreateOpenPlatformWebhookPayload {
  organizationId: string;
  eventCode: string;
  callbackUrl: string;
}

export function getOpenPlatformOverview(token: string) {
  return apiClient.get<OpenPlatformOverviewView>('/api/open/overview', token);
}

export function getOpenPlatformGovernance(token: string) {
  return apiClient.get<PluginGovernanceOverviewView>('/api/open/plugin-governance', token);
}

export function getOpenPlatformAudit(token: string) {
  return apiClient.get<IntegrationAuditOverviewView>('/api/open/integration-audit', token);
}

export function getOpenPlatformApps(token: string) {
  return apiClient.get<PluginApp[]>('/api/open/apps', token);
}

export function createOpenPlatformApp(payload: CreateOpenPlatformAppPayload, token: string) {
  return apiClient.post<PluginApp>('/api/open/apps', payload, token);
}

export function disableOpenPlatformApp(appId: string, token: string) {
  return apiClient.post<PluginApp>(`/api/open/apps/${appId}/disable`, null, token);
}

export function enableOpenPlatformApp(appId: string, token: string) {
  return apiClient.post<PluginApp>(`/api/open/apps/${appId}/enable`, null, token);
}

export function getOpenPlatformCredentials(appId: string, token: string) {
  return apiClient.get<IntegrationCredentialView[]>(`/api/open/apps/${appId}/credentials`, token);
}

export function refreshOpenPlatformCredential(appId: string, token: string) {
  return apiClient.post<IssuedIntegrationCredential>(
    `/api/open/apps/${appId}/credentials/refresh`,
    null,
    token
  );
}

export function revokeOpenPlatformCredential(appId: string, token: string) {
  return apiClient.post<IntegrationCredentialView>(
    `/api/open/apps/${appId}/credentials/revoke`,
    null,
    token
  );
}

export function getOpenPlatformWebhooks(token: string) {
  return apiClient.get<WebhookSubscription[]>('/api/open/webhooks', token);
}

export function createOpenPlatformWebhook(
  payload: CreateOpenPlatformWebhookPayload,
  token: string
) {
  return apiClient.post<WebhookSubscription>('/api/open/webhooks', payload, token);
}

export function disableOpenPlatformWebhook(subscriptionId: string, token: string) {
  return apiClient.post<WebhookSubscription>(
    `/api/open/webhooks/${subscriptionId}/disable`,
    null,
    token
  );
}

export function enableOpenPlatformWebhook(subscriptionId: string, token: string) {
  return apiClient.post<WebhookSubscription>(
    `/api/open/webhooks/${subscriptionId}/enable`,
    null,
    token
  );
}

export function rotateOpenPlatformWebhookSecret(subscriptionId: string, token: string) {
  return apiClient.post<WebhookSubscription>(
    `/api/open/webhooks/${subscriptionId}/secret/rotate`,
    null,
    token
  );
}

export function getOpenPlatformLogs(token: string) {
  return apiClient.get<OpenPlatformCallLog[]>('/api/open/logs', token);
}

export function getWebhookOrchestrations(token: string) {
  return apiClient.get<WebhookOrchestrationView[]>('/api/open/webhook-orchestrations', token);
}
