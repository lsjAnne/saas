<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { NotificationGatewayOverviewView, NotificationTask } from '@/services/apiTypes';
import { formatCount, formatDateTime } from '@/utils/formatters';

interface Props {
  notifications: NotificationTask[];
  gatewayOverview: NotificationGatewayOverviewView | null;
}

defineProps<Props>();

function toneFromStatus(status: string) {
  if (status === 'delivered' || status === 'sent') {
    return 'success';
  }

  if (status === 'failed' || status === 'dead_letter') {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Notifications"
    title="通知网关与消息重试压力"
    description="支持工单页顺带呈现通知链路压力，因为大量客服场景最后都会落到通知送达问题。"
  >
    <div class="notification-ops">
      <div class="notification-ops__summary">
        <article class="notification-ops__summary-item">
          <h4 class="notification-ops__summary-title">网关启用</h4>
          <p class="notification-ops__summary-value">
            {{ formatCount(gatewayOverview?.enabledGatewayCount) }} / {{ formatCount(gatewayOverview?.configuredGatewayCount) }}
          </p>
        </article>
        <article class="notification-ops__summary-item">
          <h4 class="notification-ops__summary-title">失败消息</h4>
          <p class="notification-ops__summary-value">{{ formatCount(gatewayOverview?.failedTaskCount, ' 条') }}</p>
        </article>
        <article class="notification-ops__summary-item">
          <h4 class="notification-ops__summary-title">死信消息</h4>
          <p class="notification-ops__summary-value">{{ formatCount(gatewayOverview?.deadLetterTaskCount, ' 条') }}</p>
        </article>
      </div>

      <div class="notification-ops__list">
        <article
          v-for="notification in notifications.slice(0, 4)"
          :key="notification.notificationTaskId"
          class="notification-ops__item"
        >
          <div class="notification-ops__head">
            <div>
              <h4 class="notification-ops__title">{{ notification.templateCode }}</h4>
              <p class="notification-ops__meta">
                {{ notification.notifyType }} · {{ notification.targetReceiver }}
              </p>
            </div>
            <StatusPill :label="notification.sendStatus" :tone="toneFromStatus(notification.sendStatus)" />
          </div>
          <p class="notification-ops__time">{{ formatDateTime(notification.createdAt) }}</p>
        </article>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.notification-ops {
  display: grid;
  gap: 1rem;
}

.notification-ops__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

.notification-ops__summary-item {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.74);
}

.notification-ops__summary-title,
.notification-ops__summary-value,
.notification-ops__title,
.notification-ops__meta,
.notification-ops__time {
  margin: 0;
}

.notification-ops__summary-title,
.notification-ops__meta,
.notification-ops__time {
  color: var(--color-ink-soft);
}

.notification-ops__summary-value,
.notification-ops__title {
  margin-top: 0.35rem;
  color: var(--color-ink-strong);
}

.notification-ops__list {
  display: grid;
  gap: 0.8rem;
}

.notification-ops__item {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.74);
}

.notification-ops__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

@media (max-width: 960px) {
  .notification-ops__summary {
    grid-template-columns: 1fr;
  }
}
</style>
