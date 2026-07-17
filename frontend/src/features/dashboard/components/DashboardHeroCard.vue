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
    eyebrow="Daily Command Deck"
    title="把今日经营脉搏、待处理压力和下一步动作压缩到一个视图里"
    description="首页先讲结果，再讲风险，再给动作。这里不是统计墙，而是租户登录后的日常指挥台。"
    dark
  >
    <div class="dashboard-hero">
      <div class="dashboard-hero__lead">
        <p class="dashboard-hero__badge">经营建议</p>
        <h4 class="dashboard-hero__headline">
          {{ summary?.topSuggestion || '正在整理今日建议动作' }}
        </h4>
        <p class="dashboard-hero__subline">
          异常、库存与履约压力会直接反映在下方卡片里，避免用户在多个模块之间来回寻找优先级。
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
  padding: 0.45rem 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 244, 236, 0.78);
  font-size: 0.76rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.dashboard-hero__headline {
  margin: 0;
  max-width: 18ch;
  font-size: clamp(1.8rem, 3vw, 2.8rem);
  line-height: 1.08;
  color: #fff8f0;
}

.dashboard-hero__subline {
  margin: 0;
  max-width: 56rem;
  color: rgba(255, 244, 236, 0.72);
  line-height: 1.7;
}

.dashboard-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.dashboard-hero__action {
  min-height: 3rem;
  padding: 0 1.1rem;
  border-radius: var(--radius-pill);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.08);
  color: #fff8f0;
}

.dashboard-hero__action--solid {
  border: 0;
  background: linear-gradient(135deg, rgba(255, 249, 242, 0.98), rgba(225, 207, 186, 0.84));
  color: var(--color-ink-strong);
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
