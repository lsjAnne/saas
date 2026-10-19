<script setup lang="ts">
import { computed } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import type { DashboardSummary } from '@/services/apiTypes';
import { formatCount, formatCurrency } from '@/utils/formatters';

interface Props {
  summary: DashboardSummary | null;
}

const props = defineProps<Props>();

const metricItems = computed(() => [
  {
    label: '今日销售额',
    value: formatCurrency(props.summary?.todaySalesAmount),
    caption: '来自今天订单的实时汇总'
  },
  {
    label: '今日订单量',
    value: formatCount(props.summary?.todayOrderCount, ' 单'),
    caption: '10 秒内判断经营是否开局正常'
  },
  {
    label: '预计毛利',
    value: formatCurrency(props.summary?.grossProfit),
    caption: '面向经营复盘的即时利润视角'
  }
]);
</script>

<template>
  <PanelCard
    title="今日经营概览"
    description="销售额、订单量与预计毛利的最新汇总，以及今日最值得优先推进的建议动作。"
  >
    <div class="dashboard-hero">
      <div class="dashboard-hero__lead">
        <p class="dashboard-hero__badge">经营建议</p>
        <h4 class="dashboard-hero__headline">
          {{ summary?.topSuggestion || '正在整理今日建议动作' }}
        </h4>
        <p class="dashboard-hero__subline">
          今日建议动作与快捷入口，帮助直接进入高频处理页。
        </p>
        <div class="dashboard-hero__actions">
          <RouterLink
            class="dashboard-hero__action dashboard-hero__action--solid"
            to="/app/order-fulfillment-center"
          >
            进入订单履约中心
          </RouterLink>
          <RouterLink class="dashboard-hero__action" to="/app/exception-center">
            打开异常中心
          </RouterLink>
          <RouterLink class="dashboard-hero__action" to="/app/inventory-replenishment-center">
            查看库存补货中心
          </RouterLink>
        </div>
      </div>

      <div class="dashboard-hero__metrics">
        <MetricCard
          v-for="metric in metricItems"
          :key="metric.label"
          :label="metric.label"
          :value="metric.value"
          :caption="metric.caption"
        />
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.dashboard-hero {
  display: grid;
  gap: 1.2rem;
}

.dashboard-hero__lead {
  display: grid;
  gap: 0.85rem;
}

.dashboard-hero__badge {
  margin: 0;
  width: fit-content;
  padding: 0.35rem 0.7rem;
  border-radius: var(--radius-md);
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.dashboard-hero__headline {
  margin: 0;
  max-width: 34ch;
  font-size: clamp(1.35rem, 2.2vw, 1.8rem);
  line-height: 1.25;
  color: var(--color-ink-strong);
}

.dashboard-hero__subline {
  margin: 0;
  max-width: 56rem;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.dashboard-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.dashboard-hero__action {
  min-height: 2.5rem;
  padding: 0 0.95rem;
  border-radius: var(--radius-md);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
  font-size: 0.88rem;
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast),
    background var(--transition-fast);
}

.dashboard-hero__action:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.dashboard-hero__action--solid {
  border: 0;
  background: var(--color-accent);
  color: #ffffff;
}

.dashboard-hero__action--solid:hover {
  background: var(--color-accent-hover);
  color: #ffffff;
}

.dashboard-hero__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.9rem;
}

@media (max-width: 900px) {
  .dashboard-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
