<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import type { DashboardTrend } from '@/services/apiTypes';
import { formatCount, formatCurrency, formatDate } from '@/utils/formatters';

interface Props {
  trends: DashboardTrend[];
  totalSales: number;
  averageOrders: number;
}

const props = defineProps<Props>();

const peakSalesAmount = computed(() =>
  Math.max(1, ...props.trends.map((trend) => Number(trend.salesAmount ?? 0)))
);

const visibleTrends = computed(() =>
  props.trends.map((trend) => ({
    ...trend,
    width: `${Math.max(10, (Number(trend.salesAmount ?? 0) / peakSalesAmount.value) * 100)}%`
  }))
);
</script>

<template>
  <PanelCard
    eyebrow="Sales Trend"
    title="销售趋势"
    description="直接看最近经营节奏，不先跳进复杂 BI 图表。"
  >
    <div class="sales-trend">
      <div class="sales-trend__summary">
        <strong>{{ formatCurrency(totalSales) }}</strong>
        <span>窗口累计销售</span>
        <span>平均 {{ formatCount(averageOrders.toFixed(0), ' 单/天') }}</span>
      </div>

      <article v-for="trend in visibleTrends" :key="trend.date" class="sales-trend__row">
        <div class="sales-trend__meta">
          <p class="sales-trend__date">{{ formatDate(trend.date) }}</p>
          <p class="sales-trend__orders">{{ formatCount(trend.orderCount, ' 单') }}</p>
        </div>

        <div class="sales-trend__bar-wrap">
          <div class="sales-trend__bar" :style="{ width: trend.width }" />
        </div>

        <strong class="sales-trend__value">{{ formatCurrency(trend.salesAmount) }}</strong>
      </article>
    </div>
  </PanelCard>
</template>

<style scoped>
.sales-trend,
.sales-trend__summary,
.sales-trend__meta {
  display: grid;
  gap: 0.8rem;
}

.sales-trend__summary {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.sales-trend__summary strong {
  color: var(--color-ink-strong);
  font-size: 1.85rem;
}

.sales-trend__summary span,
.sales-trend__date,
.sales-trend__orders {
  margin: 0;
  color: var(--color-ink-soft);
}

.sales-trend__row {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr) 132px;
  gap: 0.85rem;
  align-items: center;
}

.sales-trend__bar-wrap {
  height: 0.82rem;
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: rgba(133, 98, 67, 0.08);
}

.sales-trend__bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #dcc2a4, #835f42);
  box-shadow: 0 8px 18px rgba(133, 98, 67, 0.2);
}

.sales-trend__value {
  color: var(--color-ink-strong);
  text-align: right;
}

@media (max-width: 960px) {
  .sales-trend__row {
    grid-template-columns: 1fr;
  }

  .sales-trend__value {
    text-align: left;
  }
}
</style>
