<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  FulfillmentTask,
  OrderMain,
  StandardizedOrderView
} from '@/services/apiTypes';
import { formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  orders: OrderMain[];
  standardizedOrders: StandardizedOrderView[];
  fulfillmentTasks: FulfillmentTask[];
  selectedOrderId: string | null;
  selectedFulfillmentTaskId: string | null;
}

interface Emits {
  selectOrder: [orderId: string];
  selectFulfillmentTask: [fulfillmentTaskId: string];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const standardizedMap = computed(() => {
  const mapping = new Map<string, StandardizedOrderView>();

  for (const item of props.standardizedOrders) {
    mapping.set(item.orderId, item);
  }

  return mapping;
});

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
    normalized.includes('manual') ||
    normalized.includes('failed') ||
    normalized.includes('refund') ||
    normalized.includes('risk')
  ) {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Queue Radar"
    title="订单与履约队列"
    description="先在队列里定位订单和履约任务，再进入详情和动作区，不把状态判断分散到多个页面。"
  >
    <div class="order-queue">
      <section class="order-queue__section">
        <div class="order-queue__section-head">
          <p class="order-queue__section-title">订单主队列</p>
          <p class="order-queue__section-meta">{{ orders.length }} 笔</p>
        </div>

        <div class="order-queue__list">
          <button
            v-for="order in orders"
            :key="order.orderId"
            type="button"
            :class="[
              'order-queue__item',
              { 'order-queue__item--active': order.orderId === selectedOrderId }
            ]"
            @click="emit('selectOrder', order.orderId)"
          >
            <div class="order-queue__item-head">
              <div>
                <p class="order-queue__item-title">{{ order.platformOrderId }}</p>
                <p class="order-queue__item-meta">
                  {{ order.buyerName || '--' }} · {{ formatCurrency(order.totalAmount) }}
                </p>
              </div>
              <div class="order-queue__item-pills">
                <StatusPill :label="order.orderStatus" :tone="toneFromStatus(order.orderStatus)" />
                <StatusPill
                  :label="standardizedMap.get(order.orderId)?.standardOrderStatus || order.logisticsStatus"
                  :tone="toneFromStatus(standardizedMap.get(order.orderId)?.standardOrderStatus || order.logisticsStatus)"
                />
              </div>
            </div>
            <p class="order-queue__item-subline">
              {{ order.shippingAddress || '当前无收货地址' }}
            </p>
            <p class="order-queue__item-time">
              超时点 {{ formatDateTime(order.timeoutAt) }} · 创建 {{ formatDateTime(order.createdAt) }}
            </p>
          </button>
        </div>
      </section>

      <section class="order-queue__section">
        <div class="order-queue__section-head">
          <p class="order-queue__section-title">履约任务队列</p>
          <p class="order-queue__section-meta">{{ fulfillmentTasks.length }} 条</p>
        </div>

        <div v-if="fulfillmentTasks.length" class="order-queue__list">
          <button
            v-for="task in fulfillmentTasks"
            :key="task.fulfillmentTaskId"
            type="button"
            :class="[
              'order-queue__item',
              { 'order-queue__item--active': task.fulfillmentTaskId === selectedFulfillmentTaskId }
            ]"
            @click="emit('selectFulfillmentTask', task.fulfillmentTaskId)"
          >
            <div class="order-queue__item-head">
              <div>
                <p class="order-queue__item-title">{{ task.fulfillmentTaskId }}</p>
                <p class="order-queue__item-meta">订单 {{ task.orderId }} · 店铺 {{ task.storeId }}</p>
              </div>
              <StatusPill :label="task.status" :tone="toneFromStatus(task.status)" />
            </div>
            <p class="order-queue__item-subline">
              重试 {{ task.retryCount ?? 0 }} 次 · 最近错误 {{ task.lastErrorMessage || '无' }}
            </p>
            <p class="order-queue__item-time">
              到期 {{ formatDateTime(task.dueAt) }} · 创建 {{ formatDateTime(task.createdAt) }}
            </p>
          </button>
        </div>

        <p v-else class="order-queue__empty">
          当前账号没有履约权限或尚未生成履约任务。
        </p>
      </section>
    </div>
  </PanelCard>
</template>

<style scoped>
.order-queue {
  display: grid;
  gap: 1rem;
}

.order-queue__section {
  display: grid;
  gap: 0.8rem;
}

.order-queue__section-head,
.order-queue__item-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.order-queue__section-title,
.order-queue__section-meta,
.order-queue__item-title,
.order-queue__item-meta,
.order-queue__item-subline,
.order-queue__item-time,
.order-queue__empty {
  margin: 0;
}

.order-queue__section-title,
.order-queue__item-title {
  color: var(--color-ink-strong);
}

.order-queue__section-meta,
.order-queue__item-meta,
.order-queue__item-subline,
.order-queue__item-time,
.order-queue__empty {
  color: var(--color-ink-soft);
}

.order-queue__list {
  display: grid;
  gap: 0.75rem;
}

.order-queue__item {
  display: grid;
  gap: 0.52rem;
  width: 100%;
  padding: 0.95rem 1rem;
  text-align: left;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background:
    radial-gradient(circle at top right, rgba(198, 172, 140, 0.12), transparent 34%),
    rgba(255, 255, 255, 0.82);
}

.order-queue__item--active {
  border-color: rgba(156, 113, 71, 0.3);
  box-shadow: 0 14px 30px rgba(112, 84, 55, 0.12);
}

.order-queue__item-title {
  font-weight: 700;
}

.order-queue__item-pills {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.5rem;
}

.order-queue__item-subline {
  line-height: 1.6;
}

.order-queue__item-time {
  font-size: 0.84rem;
}

.order-queue__empty {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px dashed rgba(91, 72, 54, 0.14);
  background: rgba(255, 252, 248, 0.7);
  line-height: 1.7;
}

@media (max-width: 720px) {
  .order-queue__section-head,
  .order-queue__item-head {
    flex-direction: column;
  }

  .order-queue__item-pills {
    justify-content: flex-start;
  }
}
</style>
