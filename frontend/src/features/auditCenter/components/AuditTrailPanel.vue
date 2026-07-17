<script setup lang="ts">
import { computed } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import type { AuditLogRecord } from '@/services/apiTypes';
import { formatCount, formatDateTime } from '@/utils/formatters';

interface Props {
  auditLogs: AuditLogRecord[];
}

const props = defineProps<Props>();

const uniqueTraceCount = computed(() => new Set(props.auditLogs.map((log) => log.traceId)).size);
</script>

<template>
  <PanelCard
    eyebrow="Audit Trail"
    title="审计轨迹"
    description="审计中心要先回答谁在什么时候做了什么，再把导出动作和合规审计串起来。"
  >
    <div class="audit-trail">
      <div class="audit-trail__metrics">
        <MetricCard label="日志总数" :value="formatCount(auditLogs.length, ' 条')" caption="当前租户审计记录总量" />
        <MetricCard label="Trace 数" :value="formatCount(uniqueTraceCount, ' 个')" caption="便于追溯跨模块操作链路" />
      </div>

      <div class="audit-trail__list">
        <article v-for="log in auditLogs.slice(0, 5)" :key="`${log.traceId}-${log.createdAt}`" class="audit-trail__item">
          <h4 class="audit-trail__title">{{ log.actionType }} · {{ log.targetType }}</h4>
          <p class="audit-trail__meta">
            {{ log.operatorId }} / {{ log.operatorType }} · {{ log.targetId }} · {{ formatDateTime(log.createdAt) }}
          </p>
          <p class="audit-trail__trace">Trace {{ log.traceId || '--' }}</p>
        </article>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.audit-trail {
  display: grid;
  gap: 1rem;
}

.audit-trail__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.audit-trail__list {
  display: grid;
  gap: 0.8rem;
}

.audit-trail__item {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.audit-trail__title,
.audit-trail__meta,
.audit-trail__trace {
  margin: 0;
}

.audit-trail__title {
  color: var(--color-ink-strong);
}

.audit-trail__meta,
.audit-trail__trace {
  margin-top: 0.35rem;
  color: var(--color-ink-soft);
}

@media (max-width: 960px) {
  .audit-trail__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
