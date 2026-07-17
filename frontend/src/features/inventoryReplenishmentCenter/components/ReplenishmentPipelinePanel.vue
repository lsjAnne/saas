<script setup lang="ts">
import { computed, reactive } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  CreatePurchaseOrderPayload,
  PurchaseOrder,
  PurchaseRequest,
  ReplenishmentTask
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  tasks: ReplenishmentTask[];
  purchaseRequests: PurchaseRequest[];
  purchaseOrders: PurchaseOrder[];
  selectedTaskId: string | null;
  selectedPurchaseRequestId: string | null;
  selectedPurchaseOrderId: string | null;
  selectedTask: ReplenishmentTask | null;
  selectedPurchaseRequest: PurchaseRequest | null;
  selectedPurchaseOrder: PurchaseOrder | null;
  isSubmitting: boolean;
  actionError?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<{
  selectTask: [taskId: string];
  selectPurchaseRequest: [purchaseRequestId: string];
  selectPurchaseOrder: [purchaseOrderId: string];
  submitTaskApproval: [];
  createPurchaseOrder: [payload: CreatePurchaseOrderPayload];
  submitPurchaseOrderApproval: [];
  dispatchPurchaseOrder: [];
}>();

const createOrderForm = reactive({
  remark: ''
});

const canSubmitTaskApproval = computed(() => {
  const status = (props.selectedTask?.taskStatus ?? '').toLowerCase();
  return status === 'draft' || status === 'pending_approval';
});

const canCreatePurchaseOrder = computed(
  () => (props.selectedPurchaseRequest?.requestStatus ?? '').toLowerCase() === 'submitted'
);

const canSubmitPurchaseOrderApproval = computed(() => {
  const status = (props.selectedPurchaseOrder?.orderStatus ?? '').toLowerCase();
  return status === 'draft' || status === 'pending_approval';
});

const canDispatchPurchaseOrder = computed(() => {
  const orderStatus = (props.selectedPurchaseOrder?.orderStatus ?? '').toLowerCase();
  const dispatchStatus = (props.selectedPurchaseOrder?.dispatchStatus ?? '').toLowerCase();
  return orderStatus === 'approved' && dispatchStatus !== 'dispatched';
});

function submitCreateOrder() {
  if (!props.selectedPurchaseRequest) {
    return;
  }

  emit('createPurchaseOrder', {
    purchaseRequestId: props.selectedPurchaseRequest.purchaseRequestId,
    remark: createOrderForm.remark.trim() || undefined
  });
}

function resolveTaskTone(task: ReplenishmentTask): 'neutral' | 'warn' | 'success' {
  const status = task.taskStatus.toLowerCase();
  if (status === 'pending_approval') {
    return 'warn';
  }

  if (status === 'draft') {
    return 'success';
  }

  return 'neutral';
}

function resolveOrderTone(order: PurchaseOrder): 'neutral' | 'warn' | 'success' {
  const status = order.orderStatus.toLowerCase();
  if (status === 'pending_approval') {
    return 'warn';
  }

  if (status === 'approved') {
    return 'success';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Replenishment Pipeline"
    title="补货与采购推进走廊"
    description="这一块把补货任务、采购申请、采购订单排成连续动作链，避免库存缺口只停留在提醒层。"
  >
    <div class="pipeline">
      <div class="pipeline__columns">
        <div class="pipeline__column">
          <p class="pipeline__column-title">补货任务</p>
          <div v-if="tasks.length" class="pipeline__list">
            <button
              v-for="task in tasks"
              :key="task.replenishmentTaskId"
              type="button"
              :class="['pipeline__item', { 'pipeline__item--active': selectedTaskId === task.replenishmentTaskId }]"
              @click="emit('selectTask', task.replenishmentTaskId)"
            >
              <div class="pipeline__item-top">
                <strong>{{ task.replenishmentTaskId }}</strong>
                <StatusPill :label="task.taskStatus" :tone="resolveTaskTone(task)" />
              </div>
              <p class="pipeline__item-copy">{{ task.skuId }} / 建议 {{ task.suggestedQty }}</p>
              <p class="pipeline__item-copy">{{ formatDateTime(task.createdAt) }}</p>
            </button>
          </div>
        </div>

        <div class="pipeline__column">
          <p class="pipeline__column-title">采购申请</p>
          <div v-if="purchaseRequests.length" class="pipeline__list">
            <button
              v-for="request in purchaseRequests"
              :key="request.purchaseRequestId"
              type="button"
              :class="['pipeline__item', { 'pipeline__item--active': selectedPurchaseRequestId === request.purchaseRequestId }]"
              @click="emit('selectPurchaseRequest', request.purchaseRequestId)"
            >
              <div class="pipeline__item-top">
                <strong>{{ request.requestNo }}</strong>
                <StatusPill :label="request.requestStatus" tone="neutral" />
              </div>
              <p class="pipeline__item-copy">{{ request.supplierId }} / 数量 {{ request.totalRequestedQty }}</p>
              <p class="pipeline__item-copy">{{ formatDateTime(request.createdAt) }}</p>
            </button>
          </div>
        </div>

        <div class="pipeline__column">
          <p class="pipeline__column-title">采购订单</p>
          <div v-if="purchaseOrders.length" class="pipeline__list">
            <button
              v-for="order in purchaseOrders"
              :key="order.purchaseOrderId"
              type="button"
              :class="['pipeline__item', { 'pipeline__item--active': selectedPurchaseOrderId === order.purchaseOrderId }]"
              @click="emit('selectPurchaseOrder', order.purchaseOrderId)"
            >
              <div class="pipeline__item-top">
                <strong>{{ order.orderNo }}</strong>
                <StatusPill :label="order.orderStatus" :tone="resolveOrderTone(order)" />
              </div>
              <p class="pipeline__item-copy">{{ order.supplierId }} / 数量 {{ order.totalRequestedQty }}</p>
              <p class="pipeline__item-copy">{{ order.dispatchStatus }} / {{ formatDateTime(order.createdAt) }}</p>
            </button>
          </div>
        </div>
      </div>

      <div class="pipeline__actions">
        <div class="pipeline__action-block">
          <p class="pipeline__action-title">补货审批动作</p>
          <p class="pipeline__action-copy">
            当前任务：{{ selectedTask?.replenishmentTaskId || '--' }} / {{ selectedTask?.taskStatus || '--' }}
          </p>
          <button
            class="pipeline__ghost"
            type="button"
            :disabled="isSubmitting || !canSubmitTaskApproval"
            @click="emit('submitTaskApproval')"
          >
            提交补货审批
          </button>
        </div>

        <form class="pipeline__action-block" @submit.prevent="submitCreateOrder">
          <p class="pipeline__action-title">从采购申请生成采购单</p>
          <p class="pipeline__action-copy">
            当前申请：{{ selectedPurchaseRequest?.requestNo || '--' }} / {{ selectedPurchaseRequest?.requestStatus || '--' }}
          </p>
          <label class="pipeline__field">
            <span>采购备注</span>
            <textarea
              v-model="createOrderForm.remark"
              rows="3"
              :disabled="isSubmitting"
              placeholder="可选：补充采购订单说明"
            />
          </label>
          <button class="pipeline__primary" type="submit" :disabled="isSubmitting || !canCreatePurchaseOrder">
            生成采购单
          </button>
        </form>

        <div class="pipeline__action-block">
          <p class="pipeline__action-title">采购单推进</p>
          <p class="pipeline__action-copy">
            当前采购单：{{ selectedPurchaseOrder?.orderNo || '--' }} / {{ selectedPurchaseOrder?.orderStatus || '--' }}
          </p>
          <div class="pipeline__buttons">
            <button
              class="pipeline__ghost"
              type="button"
              :disabled="isSubmitting || !canSubmitPurchaseOrderApproval"
              @click="emit('submitPurchaseOrderApproval')"
            >
              提交采购单审批
            </button>
            <button
              class="pipeline__ghost"
              type="button"
              :disabled="isSubmitting || !canDispatchPurchaseOrder"
              @click="emit('dispatchPurchaseOrder')"
            >
              下发采购单
            </button>
          </div>
        </div>
      </div>

      <p v-if="actionError" class="pipeline__error">{{ actionError }}</p>
    </div>
  </PanelCard>
</template>

<style scoped>
.pipeline,
.pipeline__columns,
.pipeline__column,
.pipeline__list,
.pipeline__actions,
.pipeline__action-block {
  display: grid;
  gap: 0.9rem;
}

.pipeline__columns {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.pipeline__column-title,
.pipeline__action-title,
.pipeline__action-copy,
.pipeline__item-copy,
.pipeline__error {
  margin: 0;
}

.pipeline__column-title,
.pipeline__action-title {
  color: var(--color-ink-strong);
  font-size: 0.95rem;
}

.pipeline__list {
  max-height: 16rem;
  overflow: auto;
}

.pipeline__item {
  padding: 0.9rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.72);
  text-align: left;
  display: grid;
  gap: 0.6rem;
}

.pipeline__item--active {
  border-color: rgba(106, 67, 39, 0.38);
}

.pipeline__item-top {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.pipeline__item-copy,
.pipeline__action-copy {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.pipeline__actions {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.pipeline__action-block {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.68);
}

.pipeline__field {
  display: grid;
  gap: 0.45rem;
}

.pipeline__field span {
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.pipeline__field textarea {
  min-height: 2.8rem;
  padding: 0.75rem 0.9rem;
  border-radius: 1rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-ink-strong);
  font: inherit;
}

.pipeline__primary,
.pipeline__ghost {
  min-height: 2.7rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.pipeline__primary {
  border: 0;
  background: linear-gradient(135deg, #6a4327, #2b1f17);
  color: #fff8f0;
}

.pipeline__ghost {
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.66);
  color: var(--color-ink);
}

.pipeline__buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.pipeline__error {
  color: var(--color-danger);
}

@media (max-width: 1180px) {
  .pipeline__columns,
  .pipeline__actions {
    grid-template-columns: 1fr;
  }
}
</style>
