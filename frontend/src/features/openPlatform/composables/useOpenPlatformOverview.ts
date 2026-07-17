import { shallowRef } from 'vue';

import {
  createOpenPlatformApp,
  createOpenPlatformWebhook,
  disableOpenPlatformApp,
  disableOpenPlatformWebhook,
  enableOpenPlatformApp,
  enableOpenPlatformWebhook,
  getOpenPlatformApps,
  getOpenPlatformAudit,
  getOpenPlatformCredentials,
  getOpenPlatformGovernance,
  getOpenPlatformLogs,
  getOpenPlatformOverview,
  getOpenPlatformWebhooks,
  getWebhookOrchestrations,
  refreshOpenPlatformCredential,
  revokeOpenPlatformCredential,
  rotateOpenPlatformWebhookSecret,
  type CreateOpenPlatformAppPayload,
  type CreateOpenPlatformWebhookPayload
} from '@/features/openPlatform/services/openPlatformConsoleService';
import type {
  IntegrationAuditOverviewView,
  IntegrationCredentialView,
  IssuedIntegrationCredential,
  OpenPlatformCallLog,
  OpenPlatformOverviewView,
  PluginApp,
  PluginGovernanceOverviewView,
  WebhookOrchestrationView,
  WebhookSubscription
} from '@/services/apiTypes';

function toCredentialMap(
  appIds: string[],
  credentialEntries: Array<readonly [string, IntegrationCredentialView[]]>
) {
  const mappedEntries = [...credentialEntries];
  for (const appId of appIds) {
    if (!mappedEntries.some(([currentAppId]) => currentAppId === appId)) {
      mappedEntries.push([appId, []] as const);
    }
  }
  return Object.fromEntries(mappedEntries) as Record<string, IntegrationCredentialView[]>;
}

export function useOpenPlatformOverview() {
  const overview = shallowRef<OpenPlatformOverviewView | null>(null);
  const governance = shallowRef<PluginGovernanceOverviewView | null>(null);
  const audit = shallowRef<IntegrationAuditOverviewView | null>(null);
  const apps = shallowRef<PluginApp[]>([]);
  const credentialsByApp = shallowRef<Record<string, IntegrationCredentialView[]>>({});
  const issuedCredentialByApp = shallowRef<Record<string, IssuedIntegrationCredential | undefined>>({});
  const webhooks = shallowRef<WebhookSubscription[]>([]);
  const logs = shallowRef<OpenPlatformCallLog[]>([]);
  const orchestrations = shallowRef<WebhookOrchestrationView[]>([]);
  const actionFeedback = shallowRef<string | null>(null);
  const activeAppId = shallowRef<string | null>(null);
  const activeWebhookId = shallowRef<string | null>(null);
  const isLoading = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [
        overviewPayload,
        governancePayload,
        auditPayload,
        appsPayload,
        webhooksPayload,
        logsPayload,
        orchestrationPayload
      ] = await Promise.all([
        getOpenPlatformOverview(token),
        getOpenPlatformGovernance(token),
        getOpenPlatformAudit(token),
        getOpenPlatformApps(token),
        getOpenPlatformWebhooks(token),
        getOpenPlatformLogs(token),
        getWebhookOrchestrations(token)
      ]);

      const credentialEntries = await Promise.all(
        appsPayload.map(async (app) => [app.appId, await getOpenPlatformCredentials(app.appId, token)] as const)
      );

      overview.value = overviewPayload;
      governance.value = governancePayload;
      audit.value = auditPayload;
      apps.value = appsPayload;
      credentialsByApp.value = toCredentialMap(
        appsPayload.map((app) => app.appId),
        credentialEntries
      );
      webhooks.value = webhooksPayload;
      logs.value = logsPayload;
      orchestrations.value = orchestrationPayload;
    } catch (error) {
      errorMessage.value =
        error instanceof Error
          ? error.message
          : 'Failed to load the open platform workspace. Please retry.';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  async function submitCreateApp(payload: CreateOpenPlatformAppPayload, token: string) {
    isRunningAction.value = true;
    activeAppId.value = null;
    errorMessage.value = null;
    actionFeedback.value = null;

    try {
      const created = await createOpenPlatformApp(payload, token);
      actionFeedback.value = `Created app ${created.appName}. Continue with credential and webhook governance.`;
      await load(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : 'Failed to create the open platform app.';
      throw error;
    } finally {
      isRunningAction.value = false;
    }
  }

  async function toggleApp(appId: string, enable: boolean, token: string) {
    isRunningAction.value = true;
    activeAppId.value = appId;
    errorMessage.value = null;
    actionFeedback.value = null;

    try {
      const updated = enable
        ? await enableOpenPlatformApp(appId, token)
        : await disableOpenPlatformApp(appId, token);
      actionFeedback.value = enable
        ? `Enabled app ${updated.appName}.`
        : `Disabled app ${updated.appName}.`;
      await load(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : 'Failed to update the app status.';
      throw error;
    } finally {
      activeAppId.value = null;
      isRunningAction.value = false;
    }
  }

  async function refreshCredential(appId: string, token: string) {
    isRunningAction.value = true;
    activeAppId.value = appId;
    errorMessage.value = null;
    actionFeedback.value = null;

    try {
      const issued = await refreshOpenPlatformCredential(appId, token);
      issuedCredentialByApp.value = {
        ...issuedCredentialByApp.value,
        [appId]: issued
      };
      actionFeedback.value = `Refreshed credential for ${appId}. New access key: ${issued.accessKey}.`;
      await load(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : 'Failed to refresh the credential.';
      throw error;
    } finally {
      activeAppId.value = null;
      isRunningAction.value = false;
    }
  }

  async function revokeCredential(appId: string, token: string) {
    isRunningAction.value = true;
    activeAppId.value = appId;
    errorMessage.value = null;
    actionFeedback.value = null;

    try {
      const revoked = await revokeOpenPlatformCredential(appId, token);
      credentialsByApp.value = {
        ...credentialsByApp.value,
        [appId]: [revoked]
      };
      actionFeedback.value = `Revoked the current credential for ${appId}.`;
      await load(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : 'Failed to revoke the credential.';
      throw error;
    } finally {
      activeAppId.value = null;
      isRunningAction.value = false;
    }
  }

  async function submitCreateWebhook(payload: CreateOpenPlatformWebhookPayload, token: string) {
    isRunningAction.value = true;
    activeWebhookId.value = null;
    errorMessage.value = null;
    actionFeedback.value = null;

    try {
      const created = await createOpenPlatformWebhook(payload, token);
      actionFeedback.value = `Created webhook ${created.eventCode}.`;
      await load(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : 'Failed to create the webhook subscription.';
      throw error;
    } finally {
      isRunningAction.value = false;
    }
  }

  async function toggleWebhook(subscriptionId: string, enable: boolean, token: string) {
    isRunningAction.value = true;
    activeWebhookId.value = subscriptionId;
    errorMessage.value = null;
    actionFeedback.value = null;

    try {
      const updated = enable
        ? await enableOpenPlatformWebhook(subscriptionId, token)
        : await disableOpenPlatformWebhook(subscriptionId, token);
      actionFeedback.value = enable
        ? `Enabled webhook ${updated.eventCode}.`
        : `Disabled webhook ${updated.eventCode}.`;
      await load(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : 'Failed to update the webhook status.';
      throw error;
    } finally {
      activeWebhookId.value = null;
      isRunningAction.value = false;
    }
  }

  async function rotateWebhookSecret(subscriptionId: string, token: string) {
    isRunningAction.value = true;
    activeWebhookId.value = subscriptionId;
    errorMessage.value = null;
    actionFeedback.value = null;

    try {
      const updated = await rotateOpenPlatformWebhookSecret(subscriptionId, token);
      actionFeedback.value = `Rotated secret token for ${updated.eventCode}.`;
      await load(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : 'Failed to rotate the webhook secret.';
      throw error;
    } finally {
      activeWebhookId.value = null;
      isRunningAction.value = false;
    }
  }

  return {
    overview,
    governance,
    audit,
    apps,
    credentialsByApp,
    issuedCredentialByApp,
    webhooks,
    logs,
    orchestrations,
    actionFeedback,
    activeAppId,
    activeWebhookId,
    isLoading,
    isRunningAction,
    errorMessage,
    load,
    submitCreateApp,
    toggleApp,
    refreshCredential,
    revokeCredential,
    submitCreateWebhook,
    toggleWebhook,
    rotateWebhookSecret
  };
}
