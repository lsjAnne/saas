<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { TenantDataExportTaskSummary } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  exportTasks: TenantDataExportTaskSummary[];
}

defineProps<Props>();

function exportTone(status: string) {
  if (status === 'completed') {
    return 'success';
  }

  if (status === 'failed' || status === 'expired') {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Exports"
    title="导出治理"
    description="导出页不只看任务列表，还要看导出范围、脱敏策略和到期时间。"
  >
    <div class="export-governance">
      <article v-for="task in exportTasks.slice(0, 5)" :key="task.exportTaskId" class="export-governance__item">
        <div class="export-governance__head">
          <div>
            <h4 class="export-governance__title">{{ task.scopeName }}</h4>
            <p class="export-governance__meta">
              {{ task.fileName || '待生成文件' }} · {{ task.maskingStrategy }}
            </p>
          </div>
          <StatusPill :label="task.status" :tone="exportTone(task.status)" />
        </div>
        <p class="export-governance__time">
          创建 {{ formatDateTime(task.createdAt) }} · 到期 {{ formatDateTime(task.downloadExpiresAt) }}
        </p>
      </article>

      <p v-if="exportTasks.length === 0" class="export-governance__empty">
        当前没有数据导出任务。
      </p>
    </div>
  </PanelCard>
</template>

<style scoped>
.export-governance {
  display: grid;
  gap: 0.8rem;
}

.export-governance__item {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.export-governance__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.export-governance__title,
.export-governance__meta,
.export-governance__time,
.export-governance__empty {
  margin: 0;
}

.export-governance__title {
  color: var(--color-ink-strong);
}

.export-governance__meta,
.export-governance__time,
.export-governance__empty {
  margin-top: 0.35rem;
  color: var(--color-ink-soft);
}
</style>
