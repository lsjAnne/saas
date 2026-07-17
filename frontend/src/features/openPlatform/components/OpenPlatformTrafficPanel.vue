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

function toneFromStatus(status: string) {
  return status.includes('accepted') || status.includes('success') ? 'success' : 'warn';
}
</script>

<template>
  <PanelCard
    eyebrow="Traffic"
    title="调用与回调编排"
    description="开放平台真正的质量信号来自调用结果和回调编排表现，而不只是资产数。"
  >
    <div class="open-platform-traffic">
      <section class="open-platform-traffic__block">
        <p class="open-platform-traffic__label">Recent Logs</p>
        <article v-for="log in logs.slice(0, 4)" :key="log.logId" class="open-platform-traffic__item">
          <div class="open-platform-traffic__head">
            <div>
              <h4 class="open-platform-traffic__title">{{ log.sourceModule }} · {{ log.direction }}</h4>
              <p class="open-platform-traffic__meta">{{ log.endpoint }}</p>
            </div>
            <StatusPill :label="log.resultStatus" :tone="toneFromStatus(log.resultStatus)" />
          </div>
          <p class="open-platform-traffic__time">{{ formatDateTime(log.createdAt) }} · Trace {{ log.traceId }}</p>
        </article>
      </section>

      <section class="open-platform-traffic__block">
        <p class="open-platform-traffic__label">Orchestrations</p>
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
            <StatusPill :label="orchestration.status" :tone="toneFromStatus(orchestration.status)" />
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
  color: var(--color-ink-faint);
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.open-platform-traffic__item {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.74);
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
