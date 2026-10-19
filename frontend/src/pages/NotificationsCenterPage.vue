<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import NotificationChannelStatsPanel from '@/features/notificationsCenter/components/NotificationChannelStatsPanel.vue';
import NotificationFailureRecoveryPanel from '@/features/notificationsCenter/components/NotificationFailureRecoveryPanel.vue';
import NotificationHeroPanel from '@/features/notificationsCenter/components/NotificationHeroPanel.vue';
import NotificationProviderMatrixPanel from '@/features/notificationsCenter/components/NotificationProviderMatrixPanel.vue';
import NotificationTaskTimelinePanel from '@/features/notificationsCenter/components/NotificationTaskTimelinePanel.vue';
import NotificationTemplateManagerPanel from '@/features/notificationsCenter/components/NotificationTemplateManagerPanel.vue';
import { useNotificationsCenterOverview } from '@/features/notificationsCenter/composables/useNotificationsCenterOverview';
import type {
  ReplayNotificationTaskPayload,
  UpdateNotificationGatewayChannelBindingPayload,
  UpdateNotificationGatewayProviderPayload,
  UpdateNotificationTemplatePayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const notificationsCenter = useNotificationsCenterOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await notificationsCenter.load(authStore.token);
}

async function handleSaveBindings(payload: {
  requireConfiguredGateway: boolean;
  channelBindings: UpdateNotificationGatewayChannelBindingPayload[];
}) {
  if (!authStore.token) {
    return;
  }

  await notificationsCenter.saveChannelBindings(authStore.token, payload);
}

async function handleSaveProviders(providers: UpdateNotificationGatewayProviderPayload[]) {
  if (!authStore.token) {
    return;
  }

  await notificationsCenter.saveGatewayProviders(authStore.token, providers);
}

async function handleSaveTemplate(payload: {
  notificationTemplateId: string;
  template: UpdateNotificationTemplatePayload;
}) {
  if (!authStore.token) {
    return;
  }

  await notificationsCenter.saveTemplate(
    authStore.token,
    payload.notificationTemplateId,
    payload.template
  );
}

async function handleRetryTask(notificationTaskId: string) {
  if (!authStore.token) {
    return;
  }

  await notificationsCenter.retryFailedTask(authStore.token, notificationTaskId);
}

async function handleReplayTask(payload: {
  notificationTaskId: string;
  replay: ReplayNotificationTaskPayload;
}) {
  if (!authStore.token) {
    return;
  }

  await notificationsCenter.replayDeadLetterTask(
    authStore.token,
    payload.notificationTaskId,
    payload.replay
  );
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="notifications-center-page">
    <header class="notifications-center-page__header">
      <p class="notifications-center-page__eyebrow">消息中心</p>
      <h2 class="notifications-center-page__title">消息通知</h2>
      <p class="notifications-center-page__subtitle">
        通知渠道、模板与失败恢复。
      </p>
    </header>

    <InlineErrorCard
      v-if="notificationsCenter.errorMessage"
      :message="notificationsCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <NotificationHeroPanel
      :overview="notificationsCenter.gatewayOverview"
      :highlighted-tasks="notificationsCenter.highlightedTasks"
    />

    <p
      v-if="notificationsCenter.actionMessage"
      class="notifications-center-page__feedback notifications-center-page__feedback--success"
    >
      {{ notificationsCenter.actionMessage }}
    </p>
    <p
      v-if="notificationsCenter.actionErrorMessage"
      class="notifications-center-page__feedback notifications-center-page__feedback--error"
    >
      {{ notificationsCenter.actionErrorMessage }}
    </p>

    <div class="notifications-center-page__grid">
      <NotificationChannelStatsPanel
        :require-configured-gateway="notificationsCenter.gatewayConfig?.requireConfiguredGateway ?? false"
        :channel-bindings="notificationsCenter.gatewayConfig?.channelBindings ?? []"
        :channel-stats="notificationsCenter.channelStats"
        :providers="notificationsCenter.gatewayProviders"
        :is-submitting="notificationsCenter.isSavingBindings"
        @save-bindings="handleSaveBindings"
      />

      <NotificationProviderMatrixPanel
        :providers="notificationsCenter.gatewayProviders"
        :provider-stats="notificationsCenter.providerStats"
        :is-submitting="notificationsCenter.isSavingProviders"
        @save-providers="handleSaveProviders"
      />
    </div>

    <div class="notifications-center-page__grid">
      <NotificationTemplateManagerPanel
        :templates="notificationsCenter.templates"
        :saving-template-id="notificationsCenter.savingTemplateId"
        @save-template="handleSaveTemplate"
      />

      <NotificationFailureRecoveryPanel
        :tasks="notificationsCenter.failedTasks"
        :running-task-id="notificationsCenter.runningTaskId"
        @retry-task="handleRetryTask"
        @replay-task="handleReplayTask"
      />
    </div>

    <NotificationTaskTimelinePanel :notifications="notificationsCenter.recentTasks" />

    <p v-if="notificationsCenter.isLoading" class="notifications-center-page__footer-note">
      正在刷新通知路由、模板配置和失败任务列表...
    </p>
  </section>
</template>

<style scoped>
.notifications-center-page {
  display: grid;
  gap: 1.2rem;
}

.notifications-center-page__header {
  display: grid;
  gap: 0.8rem;
}

.notifications-center-page__eyebrow,
.notifications-center-page__title,
.notifications-center-page__subtitle,
.notifications-center-page__feedback,
.notifications-center-page__footer-note {
  margin: 0;
}

.notifications-center-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.notifications-center-page__title {
  max-width: 16ch;
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
  line-height: 1.2;
}

.notifications-center-page__subtitle,
.notifications-center-page__feedback,
.notifications-center-page__footer-note {
  max-width: 70rem;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.notifications-center-page__feedback {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.notifications-center-page__feedback--success {
  color: var(--color-success);
  background: var(--color-accent-soft);
  border-color: var(--color-line);
}

.notifications-center-page__feedback--error {
  color: var(--color-danger);
  background: var(--color-surface);
  border-color: var(--color-line);
}

.notifications-center-page__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  align-items: start;
}

@media (max-width: 1180px) {
  .notifications-center-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
