<script setup lang="ts">
import { computed, onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import OrderActionComposer from '@/features/orderFulfillment/components/OrderActionComposer.vue';
import OrderDetailPanel from '@/features/orderFulfillment/components/OrderDetailPanel.vue';
import OrderHeroPanel from '@/features/orderFulfillment/components/OrderHeroPanel.vue';
import OrderQueuePanel from '@/features/orderFulfillment/components/OrderQueuePanel.vue';
import { useOrderFulfillmentOverview } from '@/features/orderFulfillment/composables/useOrderFulfillmentOverview';
import type {
  CreateLogisticsRecordPayload,
  CreateOrderMergePayload,
  CreateOrderReverseStatusPayload,
  CreateOrderRoutePlanPayload,
  CreateOrderSplitPayload,
  CreateOrderSyncPayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const orderFulfillment = useOrderFulfillmentOverview();

authStore.hydrate();

const canManageFulfillment = computed(() =>
  authStore.hasPermission('fulfillment.manage')
);

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.load(authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

async function handleSelectOrder(orderId: string) {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.selectOrder(orderId, authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

async function handleSelectFulfillmentTask(fulfillmentTaskId: string) {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.selectFulfillmentTask(fulfillmentTaskId, authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

async function handleSyncOrder(payload: CreateOrderSyncPayload) {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.syncSelectedOrder(payload, authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

async function handleAuditOrder() {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.auditSelectedOrder(authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

async function handleSplitOrder(payload: CreateOrderSplitPayload) {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.splitSelectedOrder(payload, authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

async function handleMergeOrders(payload: CreateOrderMergePayload) {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.mergeSelectedOrders(payload, authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

async function handlePlanRoute(payload: CreateOrderRoutePlanPayload) {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.planSelectedOrderRoute(payload, authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

async function handleUpdateReverseStatus(payload: CreateOrderReverseStatusPayload) {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.updateSelectedOrderReverseStatus(payload, authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

async function handleConfirmTask() {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.confirmSelectedTask(authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

async function handleRetryTask() {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.retrySelectedTask(authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

async function handleReplayTask() {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.replaySelectedTask(authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

async function handleCreateLogisticsRecord(payload: CreateLogisticsRecordPayload) {
  if (!authStore.token) {
    return;
  }

  await orderFulfillment.createSelectedTaskLogisticsRecord(payload, authStore.token, {
    canLoadFulfillment: canManageFulfillment.value
  });
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="order-fulfillment-page">
    <header class="order-fulfillment-page__header">
      <p class="order-fulfillment-page__eyebrow">订单履约</p>
      <h2 class="order-fulfillment-page__title">订单与履约</h2>
      <p class="order-fulfillment-page__subtitle">
        订单同步、审单、履约推进、物流登记与逆向处理在同一工作台完成。
      </p>
    </header>

    <InlineErrorCard
      v-if="orderFulfillment.errorMessage"
      :message="orderFulfillment.errorMessage"
      @retry="bootstrapPage"
    />

    <OrderHeroPanel
      :summary="orderFulfillment.summary"
      :selected-order="orderFulfillment.selectedOrder"
      :selected-standardized-order="orderFulfillment.selectedStandardizedOrder"
      :selected-fulfillment-task="orderFulfillment.selectedFulfillmentTask"
      :action-feedback="orderFulfillment.actionFeedback"
    />

    <div class="order-fulfillment-page__grid">
      <OrderQueuePanel
        :orders="orderFulfillment.orders"
        :standardized-orders="orderFulfillment.standardizedOrders"
        :fulfillment-tasks="orderFulfillment.fulfillmentTasks"
        :selected-order-id="orderFulfillment.selectedOrderId"
        :selected-fulfillment-task-id="orderFulfillment.selectedFulfillmentTaskId"
        @select-order="handleSelectOrder"
        @select-fulfillment-task="handleSelectFulfillmentTask"
      />

      <div class="order-fulfillment-page__stack">
        <OrderDetailPanel
          :order-detail="orderFulfillment.selectedOrderDetail"
          :orchestration="orderFulfillment.selectedOrderOrchestration"
          :audit-review="orderFulfillment.selectedAuditReview"
          :fulfillment-task="orderFulfillment.selectedFulfillmentTask"
          :logistics-records="orderFulfillment.logisticsRecords"
          :can-manage-fulfillment="canManageFulfillment"
        />

        <OrderActionComposer
          :stores="orderFulfillment.stores"
          :selected-order="orderFulfillment.selectedOrder"
          :selected-order-detail="orderFulfillment.selectedOrderDetail"
          :selected-fulfillment-task="orderFulfillment.selectedFulfillmentTask"
          :is-submitting="orderFulfillment.isRunningAction"
          :error-message="orderFulfillment.actionError"
          :can-manage-fulfillment="canManageFulfillment"
          @sync-order="handleSyncOrder"
          @audit-order="handleAuditOrder"
          @split-order="handleSplitOrder"
          @merge-orders="handleMergeOrders"
          @plan-route="handlePlanRoute"
          @update-reverse-status="handleUpdateReverseStatus"
          @confirm-task="handleConfirmTask"
          @retry-task="handleRetryTask"
          @replay-task="handleReplayTask"
          @create-logistics-record="handleCreateLogisticsRecord"
        />
      </div>
    </div>

    <p v-if="orderFulfillment.isLoading" class="order-fulfillment-page__footer-note">
      正在回读订单主队列、OMS 摘要、履约任务和物流记录...
    </p>
  </section>
</template>

<style scoped>
.order-fulfillment-page {
  display: grid;
  gap: 1.2rem;
}

.order-fulfillment-page__header {
  display: grid;
  gap: 0.8rem;
}

.order-fulfillment-page__eyebrow,
.order-fulfillment-page__title,
.order-fulfillment-page__subtitle,
.order-fulfillment-page__footer-note {
  margin: 0;
}

.order-fulfillment-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.order-fulfillment-page__title {
  color: var(--color-ink-strong);
  font-size: 1.55rem;
  font-weight: 600;
}

.order-fulfillment-page__subtitle,
.order-fulfillment-page__footer-note {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.order-fulfillment-page__grid {
  display: grid;
  grid-template-columns: 0.96fr 1.04fr;
  gap: 1rem;
  align-items: start;
}

.order-fulfillment-page__stack {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1180px) {
  .order-fulfillment-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
