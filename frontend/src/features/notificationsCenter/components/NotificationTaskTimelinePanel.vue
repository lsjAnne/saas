<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { NotificationTask } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  notifications: NotificationTask[];
}

defineProps<Props>();

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toneFromStatus(status: string) {
  const normalized = normalizeText(status);

  if (normalized === 'delivered' || normalized === 'sent') {
    return 'success';
  }

  if (normalized === 'failed' || normalized === 'dead_letter' || normalized === 'dead-letter') {
    return 'warn';
  }

  return 'neutral';
}

function toneFromPriority(priority: string) {
  return normalizeText(priority) === 'urgent' ? 'warn' : 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Recent Activity"
    title="最近通知任务"
    description="保留最近派发记录，方便从配置动作回跳到真实任务与死信原因。"
  >
    <div v-if="notifications.length" class="task-timeline">
      <article
        v-for="notification in notifications"
        :key="notification.notificationTaskId"
        class="task-timeline__item"
      >
        <div class="task-timeline__head">
          <div class="task-timeline__title-wrap">
            <h4 class="task-timeline__title">{{ notification.templateCode }}</h4>
            <p class="task-timeline__meta">
              {{ notification.notifyType }} · {{ notification.targetReceiver }}
            </p>
          </div>
          <div class="task-timeline__pills">
            <StatusPill :label="notification.sendStatus" :tone="toneFromStatus(notification.sendStatus)" />
            <StatusPill :label="notification.priority || 'normal'" :tone="toneFromPriority(notification.priority)" />
          </div>
        </div>

        <div class="task-timeline__facts">
          <span>创建于 {{ formatDateTime(notification.createdAt) }}</span>
          <span>计划发送 {{ formatDateTime(notification.scheduledAt) }}</span>
          <span>重试 {{ notification.retryCount }} 次</span>
          <span>批次 {{ notification.batchId || '--' }}</span>
        </div>

        <p v-if="notification.deadLetterReason" class="task-timeline__dead-letter">
          死信原因：{{ notification.deadLetterReason }}
        </p>
      </article>
    </div>

    <p v-else class="task-timeline__empty">当前没有通知任务可展示。</p>
  </PanelCard>
</template>

<style scoped>
.task-timeline {
  display: grid;
  gap: 0.85rem;
}

.task-timeline__item {
  display: grid;
  gap: 0.65rem;
  padding: 1rem 1.05rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(250, 244, 236, 0.7));
}

.task-timeline__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.task-timeline__title,
.task-timeline__meta,
.task-timeline__dead-letter,
.task-timeline__empty {
  margin: 0;
}

.task-timeline__title {
  color: var(--color-ink-strong);
  font-size: 1.04rem;
}

.task-timeline__meta,
.task-timeline__facts,
.task-timeline__dead-letter,
.task-timeline__empty {
  color: var(--color-ink-soft);
}

.task-timeline__meta {
  margin-top: 0.3rem;
}

.task-timeline__pills {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  justify-content: flex-end;
}

.task-timeline__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.45rem 0.8rem;
  font-size: 0.92rem;
}

.task-timeline__dead-letter {
  padding: 0.75rem 0.85rem;
  border-radius: calc(var(--radius-lg) - 0.25rem);
  background: rgba(145, 96, 54, 0.08);
  color: #7a4d28;
  line-height: 1.65;
}

@media (max-width: 720px) {
  .task-timeline__head,
  .task-timeline__facts {
    display: grid;
    grid-template-columns: 1fr;
  }

  .task-timeline__pills {
    justify-content: flex-start;
  }
}
</style>
