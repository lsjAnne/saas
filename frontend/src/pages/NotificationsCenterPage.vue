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
      <p class="notifications-center-page__eyebrow">Notifications Center</p>
      <h2 class="notifications-center-page__title">
        把通知通道路由、模板启停和失败恢复压进同一个配置操作面
      </h2>
      <p class="notifications-center-page__subtitle">
        这个页面不再只看监控数字，而是直接处理通知链路的三类关键动作：调整渠道路由、维护模板可用性、把失败或死信任务拉回投递闭环。
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
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.notifications-center-page__title {
  max-width: 16ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.15rem, 3.8vw, 3.7rem);
  line-height: 1.05;
}

.notifications-center-page__subtitle,
.notifications-center-page__feedback,
.notifications-center-page__footer-note {
  max-width: 70rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.notifications-center-page__feedback {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.notifications-center-page__feedback--success {
  color: #2f5b39;
  background: rgba(228, 242, 232, 0.82);
  border-color: rgba(76, 124, 85, 0.18);
}

.notifications-center-page__feedback--error {
  color: #7a3f2b;
  background: rgba(251, 237, 230, 0.86);
  border-color: rgba(150, 92, 67, 0.18);
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
