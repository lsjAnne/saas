<script setup lang="ts">
import { computed } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  FulfillmentTask,
  OrderMain,
  StandardizedOrderView
} from '@/services/apiTypes';
import type { OrderFulfillmentSummary } from '@/features/orderFulfillment/composables/useOrderFulfillmentOverview';
import { formatCount, formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  summary: OrderFulfillmentSummary;
  selectedOrder: OrderMain | null;
  selectedStandardizedOrder: StandardizedOrderView | null;
  selectedFulfillmentTask: FulfillmentTask | null;
  actionFeedback: string | null;
}

const props = defineProps<Props>();

const metricItems = computed(() => [
  {
    label: '订单总量',
    value: formatCount(props.summary.totalOrderCount, ' 单'),
    caption: '当前租户已同步进入 OMS 主链路的订单规模'
  },
  {
    label: '待履约',
    value: formatCount(props.summary.pendingFulfillmentCount, ' 单'),
    caption: '仍处于待履约或待确认阶段的订单数量'
  },
  {
    label: '人工回放',
    value: formatCount(props.summary.manualReplayCount, ' 单'),
    caption: '需要运营或履约人工介入回放的任务数'
  },
  {
    label: '逆向压力',
    value: formatCount(props.summary.reverseOrderCount, ' 单'),
    caption: '已进入退款/退货逆向链路的订单规模'
  }
]);

function toneFromStatus(status: string | null | undefined) {
  const normalized = (status ?? '').toLowerCase();

  if (
    normalized.includes('success') ||
    normalized.includes('ready') ||
    normalized.includes('delivered')
  ) {
    return 'success';
  }

  if (
    normalized.includes('risk') ||
    normalized.includes('failed') ||
    normalized.includes('manual') ||
    normalized.includes('refund')
  ) {
    return 'warn';
  }

  return 'neutral';
}

const headline = computed(() => {
  if (props.selectedOrder) {
    return `当前焦点订单 ${props.selectedOrder.platformOrderId}，买家 ${props.selectedOrder.buyerName || '--'}，金额 ${formatCurrency(props.selectedOrder.totalAmount)}。`;
  }

  return '先同步一笔订单，再在同一页内完成审单、拆合单、路由规划与履约回读。';
});
</script>

<template>
  <PanelCard
    title="订单履约概览"
    :description="headline"
  >
    <div class="order-hero">
      <div class="order-hero__lead">
        <p class="order-hero__badge">当前焦点</p>
        <h4 class="order-hero__headline">
          订单队列、履约任务与物流记录在同一视角连续处理
        </h4>
        <p class="order-hero__subline">
          同步平台单生成订单与履约任务，所有处理动作完成后回读后端真实状态。
        </p>

        <div v-if="selectedOrder" class="order-hero__focus-card">
          <div class="order-hero__focus-head">
            <div>
              <p class="order-hero__focus-title">
                {{ selectedOrder.platformOrderId }}
              </p>
              <p class="order-hero__focus-meta">
                订单 {{ selectedOrder.orderId }} · 创建于 {{ formatDateTime(selectedOrder.createdAt) }}
              </p>
            </div>
            <div class="order-hero__focus-pills">
              <StatusPill
                :label="selectedOrder.orderStatus"
                :tone="toneFromStatus(selectedOrder.orderStatus)"
              />
              <StatusPill
                :label="selectedOrder.logisticsStatus"
                :tone="toneFromStatus(selectedOrder.logisticsStatus)"
              />
            </div>
          </div>

          <dl class="order-hero__facts">
            <div class="order-hero__fact">
              <dt>买家</dt>
              <dd>{{ selectedOrder.buyerName || '--' }} / {{ selectedOrder.buyerPhoneMask || '--' }}</dd>
            </div>
            <div class="order-hero__fact">
              <dt>标准态</dt>
              <dd>
                {{ selectedStandardizedOrder?.standardOrderStatus || '--' }} ·
                {{ selectedStandardizedOrder?.sourcePlatform || '--' }}
              </dd>
            </div>
            <div class="order-hero__fact">
              <dt>履约任务</dt>
              <dd>
                {{ selectedFulfillmentTask?.fulfillmentTaskId || '--' }} ·
                {{ selectedFulfillmentTask?.status || '待生成' }}
              </dd>
            </div>
            <div class="order-hero__fact">
              <dt>订单金额</dt>
              <dd>{{ formatCurrency(selectedOrder.totalAmount) }}</dd>
            </div>
          </dl>

          <p v-if="actionFeedback" class="order-hero__feedback">{{ actionFeedback }}</p>
        </div>
      </div>

      <div class="order-hero__metrics">
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
.order-hero {
  display: grid;
  gap: 1rem;
}

.order-hero__lead {
  display: grid;
  gap: 0.85rem;
}

.order-hero__badge,
.order-hero__headline,
.order-hero__subline,
.order-hero__focus-title,
.order-hero__focus-meta,
.order-hero__feedback {
  margin: 0;
}

.order-hero__badge {
  width: fit-content;
  padding: 0.3rem 0.65rem;
  border-radius: var(--radius-md);
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.order-hero__headline {
  color: var(--color-ink-strong);
  font-size: 1.35rem;
  line-height: 1.3;
}

.order-hero__subline {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.order-hero__focus-card {
  display: grid;
  gap: 0.9rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.order-hero__focus-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.order-hero__focus-title {
  color: var(--color-ink-strong);
  font-size: 1.05rem;
  font-weight: 700;
}

.order-hero__focus-meta,
.order-hero__feedback,
.order-hero__fact dt {
  color: var(--color-ink-soft);
}

.order-hero__focus-pills {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.5rem;
}

.order-hero__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
  margin: 0;
}

.order-hero__fact {
  display: grid;
  gap: 0.22rem;
}

.order-hero__fact dt {
  font-size: 0.76rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.order-hero__fact dd {
  margin: 0;
  color: var(--color-ink-strong);
  line-height: 1.65;
}

.order-hero__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

@media (max-width: 980px) {
  .order-hero__metrics,
  .order-hero__facts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .order-hero__metrics,
  .order-hero__facts {
    grid-template-columns: 1fr;
  }

  .order-hero__focus-head {
    flex-direction: column;
  }

  .order-hero__focus-pills {
    justify-content: flex-start;
  }
}
</style>
