<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { OpenPlatformCallLog, WebhookOrchestrationView } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  logs: OpenPlatformCallLog[];
  orchestrations: WebhookOrchestrationView[];
}

defineProps<Props>();

const trafficStatusMap: Record<string, string> = {
  accepted: '已接受',
  success: '成功',
  rejected: '已拒绝',
  failed: '失败',
  pending: '处理中'
};

function toneFromStatus(status: string) {
  return status.includes('accepted') || status.includes('success') ? 'success' : 'warn';
}
</script>

<template>
  <PanelCard
    title="调用与回调编排"
    description="调用结果与回调编排表现一览。"
  >
    <div class="open-platform-traffic">
      <section class="open-platform-traffic__block">
        <p class="open-platform-traffic__label">最近日志</p>
        <article v-for="log in logs.slice(0, 4)" :key="log.logId" class="open-platform-traffic__item">
          <div class="open-platform-traffic__head">
            <div>
              <h4 class="open-platform-traffic__title">{{ log.sourceModule }} · {{ log.direction }}</h4>
              <p class="open-platform-traffic__meta">{{ log.endpoint }}</p>
            </div>
            <StatusPill :label="trafficStatusMap[log.resultStatus] || log.resultStatus" :tone="toneFromStatus(log.resultStatus)" />
          </div>
          <p class="open-platform-traffic__time">{{ formatDateTime(log.createdAt) }} · Trace {{ log.traceId }}</p>
        </article>
      </section>

      <section class="open-platform-traffic__block">
        <p class="open-platform-traffic__label">回调编排</p>
        <article
          v-for="orchestration in orchestrations.slice(0, 4)"
          :key="orchestration.subscriptionId"
          class="open-platform-traffic__item"
        >
          <div class="open-platform-traffic__head">
            <div>
              <h4 class="open-platform-traffic__title">{{ orchestration.eventCode }}</h4>
              <p class="open-platform-traffic__meta">{{ orchestration.callbackUrl }}</p>
            </div>
            <StatusPill :label="trafficStatusMap[orchestration.status] || orchestration.status" :tone="toneFromStatus(orchestration.status)" />
          </div>
          <p class="open-platform-traffic__time">
            尝试 {{ orchestration.callbackAttemptCount }} · 接受 {{ orchestration.acceptedCallbackCount }} · 拒绝 {{ orchestration.rejectedCallbackCount }}
          </p>
        </article>
      </section>
    </div>
  </PanelCard>
</template>

<style scoped>
.open-platform-traffic {
  display: grid;
  gap: 1rem;
}

.open-platform-traffic__block {
  display: grid;
  gap: 0.8rem;
}

.open-platform-traffic__label,
.open-platform-traffic__title,
.open-platform-traffic__meta,
.open-platform-traffic__time {
  margin: 0;
}

.open-platform-traffic__label {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.open-platform-traffic__item {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.open-platform-traffic__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.open-platform-traffic__title {
  color: var(--color-ink-strong);
}

.open-platform-traffic__meta,
.open-platform-traffic__time {
  margin-top: 0.35rem;
  color: var(--color-ink-soft);
}
</style>
