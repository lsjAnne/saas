<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { DashboardTrend } from '@/services/apiTypes';
import { formatCurrency, formatDate, formatPercent } from '@/utils/formatters';

interface Props {
  trends: DashboardTrend[];
  totalGrossProfit: number;
  averageMargin: number;
}

const props = defineProps<Props>();

const peakGrossProfit = computed(() =>
  Math.max(1, ...props.trends.map((trend) => Number(trend.grossProfit ?? 0)))
);

const visibleTrends = computed(() =>
  props.trends.map((trend) => {
    const salesAmount = Number(trend.salesAmount ?? 0);
    const grossProfit = Number(trend.grossProfit ?? 0);
    return {
      ...trend,
      margin: salesAmount > 0 ? grossProfit / salesAmount : 0,
      width: `${Math.max(10, (grossProfit / peakGrossProfit.value) * 100)}%`
    };
  })
);

function toneOf(margin: number) {
  if (margin >= 0.25) {
    return 'success';
  }

  if (margin > 0) {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    title="毛利趋势"
    description="不仅看卖了多少，还看卖出来的钱有没有留下来。"
  >
    <div class="gross-trend">
      <div class="gross-trend__summary">
        <strong>{{ formatCurrency(totalGrossProfit) }}</strong>
        <span>窗口累计毛利</span>
        <span>平均毛利率 {{ formatPercent(averageMargin, 1) }}</span>
      </div>

      <article v-for="trend in visibleTrends" :key="trend.date" class="gross-trend__row">
        <div class="gross-trend__meta">
          <p class="gross-trend__date">{{ formatDate(trend.date) }}</p>
          <StatusPill :label="formatPercent(trend.margin, 1)" :tone="toneOf(trend.margin)" />
        </div>

        <div class="gross-trend__bar-wrap">
          <div class="gross-trend__bar" :style="{ width: trend.width }" />
        </div>

        <strong class="gross-trend__value">{{ formatCurrency(trend.grossProfit) }}</strong>
      </article>
    </div>
  </PanelCard>
</template>

<style scoped>
.gross-trend,
.gross-trend__summary,
.gross-trend__meta {
  display: grid;
  gap: 0.8rem;
}

.gross-trend__summary {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.gross-trend__summary strong {
  color: var(--color-ink-strong);
  font-size: 1.5rem;
}

.gross-trend__summary span,
.gross-trend__date {
  margin: 0;
  color: var(--color-ink-soft);
}

.gross-trend__row {
  display: grid;
  grid-template-columns: 168px minmax(0, 1fr) 132px;
  gap: 0.85rem;
  align-items: center;
}

.gross-trend__bar-wrap {
  height: 0.82rem;
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: var(--color-surface);
}

.gross-trend__bar {
  height: 100%;
  border-radius: inherit;
  background: var(--color-accent);
}

.gross-trend__value {
  color: var(--color-ink-strong);
  text-align: right;
}

@media (max-width: 960px) {
  .gross-trend__row {
    grid-template-columns: 1fr;
  }

  .gross-trend__value {
    text-align: left;
  }
}
</style>
