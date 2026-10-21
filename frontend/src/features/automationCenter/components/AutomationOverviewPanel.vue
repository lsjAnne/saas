<script setup lang="ts">
import { computed } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import type { AutomationStats } from '@/services/automationTypes';
import { AUTOMATION_ACTION_TYPE_LABELS } from '@/services/automationTypes';

interface Props {
  stats: AutomationStats | null;
}

const props = defineProps<Props>();

const metricCards = computed(() => {
  const stats = props.stats;

  return [
    { label: '总执行', value: stats ? String(stats.total) : '--', caption: '全部自动化动作执行次数' },
    { label: '成功', value: stats ? String(stats.success) : '--', caption: '执行成功的动作条数' },
    { label: '失败', value: stats ? String(stats.failed) : '--', caption: '执行失败需重试的动作条数' },
    { label: '挂起人工', value: stats ? String(stats.pendingReview) : '--', caption: '命中 PAUSE_REVIEW 挂起的条数' },
    { label: '待人工确认', value: stats ? String(stats.pendingApprovalCount) : '--', caption: '确认队列中待处理任务数' },
    { label: '今日成功', value: stats ? String(stats.todaySuccess) : '--', caption: '今日已成功执行的动作条数' },
    { label: '平均耗时', value: stats ? `${stats.avgCostMs} ms` : '--', caption: '全部执行的平均耗时' }
  ];
});

const maxActionCount = computed(() => {
  const list = props.stats?.byActionType ?? [];
  return list.reduce((max, item) => Math.max(max, item.count), 0);
});
</script>

<template>
  <PanelCard
    eyebrow="Overview"
    title="自动化总览"
    description="汇总执行成功率、挂起与待确认积压、今日产出与平均耗时，并按动作类型查看分布。"
  >
    <div class="auto-overview__metrics">
      <MetricCard
        v-for="card in metricCards"
        :key="card.label"
        :label="card.label"
        :value="card.value"
        :caption="card.caption"
      />
    </div>

    <div class="auto-overview__dist">
      <p class="auto-overview__dist-title">按动作类型分布</p>
      <p v-if="!stats || stats.byActionType.length === 0" class="auto-overview__empty">
        当前店铺暂无自动化执行数据。
      </p>
      <div v-else class="auto-overview__rows">
        <div v-for="row in stats.byActionType" :key="row.actionType" class="auto-overview__row">
          <div class="auto-overview__row-head">
            <span class="auto-overview__row-label">
              {{ AUTOMATION_ACTION_TYPE_LABELS[row.actionType] ?? row.actionType }}
              <code class="auto-overview__row-code">{{ row.actionType }}</code>
            </span>
            <span class="auto-overview__row-count">
              共 {{ row.count }} · 成功 {{ row.success }} · 失败 {{ row.failed }}
            </span>
          </div>
          <div class="auto-overview__bar-track">
            <div
              class="auto-overview__bar-fill"
              :style="{ width: `${maxActionCount ? (row.count / maxActionCount) * 100 : 0}%` }"
            />
          </div>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.auto-overview__metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.8rem;
}

.auto-overview__dist {
  margin-top: 1.4rem;
  display: grid;
  gap: 0.8rem;
}

.auto-overview__dist-title,
.auto-overview__row-label,
.auto-overview__row-count,
.auto-overview__empty {
  margin: 0;
}

.auto-overview__dist-title {
  color: var(--color-ink-strong);
  font-weight: 700;
}

.auto-overview__rows {
  display: grid;
  gap: 0.9rem;
}

.auto-overview__row {
  display: grid;
  gap: 0.45rem;
}

.auto-overview__row-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.auto-overview__row-label {
  color: var(--color-ink-strong);
  font-weight: 600;
}

.auto-overview__row-code {
  margin-left: 0.4rem;
  padding: 0.1rem 0.45rem;
  border-radius: var(--radius-pill);
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 0.72rem;
  font-weight: 600;
}

.auto-overview__row-count {
  color: var(--color-ink-soft);
  font-size: 0.85rem;
}

.auto-overview__bar-track {
  height: 0.6rem;
  border-radius: var(--radius-pill);
  background: var(--color-surface-2);
  overflow: hidden;
}

.auto-overview__bar-fill {
  height: 100%;
  border-radius: var(--radius-pill);
  background: var(--color-accent);
  transition: width var(--transition-medium);
}

.auto-overview__empty {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px dashed var(--color-line);
  background: var(--color-surface);
  color: var(--color-ink-soft);
  line-height: 1.7;
}
</style>
