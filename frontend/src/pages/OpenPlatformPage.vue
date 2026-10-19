<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import OpenPlatformAssetsPanel from '@/features/openPlatform/components/OpenPlatformAssetsPanel.vue';
import OpenPlatformOverviewPanel from '@/features/openPlatform/components/OpenPlatformOverviewPanel.vue';
import OpenPlatformTrafficPanel from '@/features/openPlatform/components/OpenPlatformTrafficPanel.vue';
import { useOpenPlatformOverview } from '@/features/openPlatform/composables/useOpenPlatformOverview';
import type {
  CreateOpenPlatformAppPayload,
  CreateOpenPlatformWebhookPayload
} from '@/features/openPlatform/services/openPlatformConsoleService';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const openPlatform = useOpenPlatformOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  try {
    await openPlatform.load(authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

async function handleCreateApp(payload: CreateOpenPlatformAppPayload) {
  if (!authStore.token) {
    return;
  }

  try {
    await openPlatform.submitCreateApp(payload, authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

async function handleToggleApp(payload: { appId: string; enable: boolean }) {
  if (!authStore.token) {
    return;
  }

  try {
    await openPlatform.toggleApp(payload.appId, payload.enable, authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

async function handleCreateWebhook(payload: CreateOpenPlatformWebhookPayload) {
  if (!authStore.token) {
    return;
  }

  try {
    await openPlatform.submitCreateWebhook(payload, authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

async function handleToggleWebhook(payload: { subscriptionId: string; enable: boolean }) {
  if (!authStore.token) {
    return;
  }

  try {
    await openPlatform.toggleWebhook(payload.subscriptionId, payload.enable, authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

async function handleRefreshCredential(appId: string) {
  if (!authStore.token) {
    return;
  }

  try {
    await openPlatform.refreshCredential(appId, authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

async function handleRevokeCredential(appId: string) {
  if (!authStore.token) {
    return;
  }

  try {
    await openPlatform.revokeCredential(appId, authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

async function handleRotateWebhookSecret(subscriptionId: string) {
  if (!authStore.token) {
    return;
  }

  try {
    await openPlatform.rotateWebhookSecret(subscriptionId, authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="open-platform-page">
    <header class="open-platform-page__header">
      <p class="open-platform-page__eyebrow">开放平台</p>
      <h2 class="open-platform-page__title">开放接口</h2>
      <p class="open-platform-page__subtitle">
        开放能力、流量与应用资产管理。
      </p>
    </header>

    <InlineErrorCard
      v-if="openPlatform.errorMessage"
      :message="openPlatform.errorMessage"
      @retry="bootstrapPage"
    />

    <OpenPlatformOverviewPanel
      :overview="openPlatform.overview"
      :governance="openPlatform.governance"
      :audit="openPlatform.audit"
    />

    <div class="open-platform-page__grid">
      <OpenPlatformAssetsPanel
        :organization-id="authStore.organizationId"
        :apps="openPlatform.apps"
        :credentials-by-app="openPlatform.credentialsByApp"
        :issued-credential-by-app="openPlatform.issuedCredentialByApp"
        :webhooks="openPlatform.webhooks"
        :is-running-action="openPlatform.isRunningAction"
        :active-app-id="openPlatform.activeAppId"
        :active-webhook-id="openPlatform.activeWebhookId"
        :action-feedback="openPlatform.actionFeedback"
        @create-app="handleCreateApp"
        @toggle-app="handleToggleApp"
        @refresh-credential="handleRefreshCredential"
        @revoke-credential="handleRevokeCredential"
        @create-webhook="handleCreateWebhook"
        @toggle-webhook="handleToggleWebhook"
        @rotate-webhook-secret="handleRotateWebhookSecret"
      />
      <OpenPlatformTrafficPanel
        :logs="openPlatform.logs"
        :orchestrations="openPlatform.orchestrations"
      />
    </div>
  </section>
</template>

<style scoped>
.open-platform-page {
  display: grid;
  gap: 1.2rem;
}

.open-platform-page__header {
  display: grid;
  gap: 0.75rem;
}

.open-platform-page__eyebrow,
.open-platform-page__title,
.open-platform-page__subtitle {
  margin: 0;
}

.open-platform-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.open-platform-page__title {
  max-width: 18ch;
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
  line-height: 1.2;
}

.open-platform-page__subtitle {
  max-width: 56rem;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.open-platform-page__grid {
  display: grid;
  grid-template-columns: 1.02fr 0.98fr;
  gap: 1rem;
}

@media (max-width: 1080px) {
  .open-platform-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
