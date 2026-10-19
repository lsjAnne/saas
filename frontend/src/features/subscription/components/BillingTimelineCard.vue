<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { BillingOrder, InvoiceRequest, InvoiceRequestPayload } from '@/services/apiTypes';
import { formatCurrency, formatDate, formatDateTime } from '@/utils/formatters';

interface Props {
  billingOrders: BillingOrder[];
  lastInvoiceRequest?: InvoiceRequest | null;
  isSubmitting?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  lastInvoiceRequest: null,
  isSubmitting: false
});
const emit = defineEmits<{
  settleOrder: [billingOrderId: string];
  requestInvoice: [payload: InvoiceRequestPayload];
  downloadOrder: [billingOrderId: string];
}>();

const invoiceForm = reactive({
  billingOrderId: '',
  invoiceTitle: '',
  invoiceTaxNo: ''
});

const visibleOrders = computed(() => props.billingOrders.slice(0, 6));
const selectedOrder = computed(
  () =>
    props.billingOrders.find((order) => order.billingOrderId === invoiceForm.billingOrderId) ??
    null
);

const paymentStatusMap: Record<string, string> = {
  paid: '已支付',
  unpaid: '待支付',
  pending: '处理中'
};

const invoiceStatusMap: Record<string, string> = {
  draft: '草稿',
  submitted: '已提交',
  issued: '已开具'
};

function paymentTone(status: string) {
  return status === 'paid' ? 'success' : 'warn';
}

function canSettle(order: BillingOrder) {
  return order.paymentStatus !== 'paid';
}

watch(
  () => props.billingOrders,
  (orders) => {
    const preferredOrder = orders.find((order) => order.paymentStatus !== 'paid') ?? orders[0] ?? null;

    invoiceForm.billingOrderId = preferredOrder?.billingOrderId ?? '';
    invoiceForm.invoiceTitle = preferredOrder ? `${preferredOrder.planName} 订阅账单` : '';
    invoiceForm.invoiceTaxNo = '';
  },
  { immediate: true }
);

function submitInvoice() {
  if (!invoiceForm.billingOrderId || !invoiceForm.invoiceTitle || !invoiceForm.invoiceTaxNo) {
    return;
  }

  emit('requestInvoice', {
    billingOrderId: invoiceForm.billingOrderId,
    invoiceTitle: invoiceForm.invoiceTitle,
    invoiceTaxNo: invoiceForm.invoiceTaxNo
  });
}
</script>

<template>
  <PanelCard
    title="账单与发票"
    description="账单下载、结算与开票申请同屏处理。"
  >
    <div class="billing-timeline">
      <article v-for="order in visibleOrders" :key="order.billingOrderId" class="billing-timeline__item">
        <div class="billing-timeline__head">
          <div>
            <h4 class="billing-timeline__title">{{ order.planName }}</h4>
            <p class="billing-timeline__meta">
              {{ formatDate(order.createdAt) }} / {{ order.orderType }}
            </p>
          </div>
          <StatusPill :label="paymentStatusMap[order.paymentStatus] || order.paymentStatus" :tone="paymentTone(order.paymentStatus)" />
        </div>
        <p class="billing-timeline__amount">{{ formatCurrency(order.payableAmount) }}</p>
        <p class="billing-timeline__meta">外部订单 {{ order.externalOrderNo || '--' }}</p>
        <div class="billing-timeline__actions">
          <button
            class="billing-timeline__ghost"
            type="button"
            @click="emit('downloadOrder', order.billingOrderId)"
          >
            下载账单
          </button>
          <button
            class="billing-timeline__solid"
            type="button"
            :disabled="isSubmitting || !canSettle(order)"
            @click="emit('settleOrder', order.billingOrderId)"
          >
            {{ !canSettle(order) ? '已支付' : isSubmitting ? '处理中...' : '结算付款' }}
          </button>
        </div>
      </article>

      <p v-if="visibleOrders.length === 0" class="billing-timeline__empty">
        暂无账单订单。升级、续费或购买坐席后将在此生成新订单。
      </p>

      <div class="billing-timeline__invoice">
        <div class="billing-timeline__invoice-head">
          <div>
            <p class="billing-timeline__invoice-eyebrow">开票申请</p>
            <h4 class="billing-timeline__invoice-title">提交开票申请</h4>
          </div>
          <StatusPill
            :label="invoiceStatusMap[lastInvoiceRequest?.invoiceStatus ?? ''] || lastInvoiceRequest?.invoiceStatus || '草稿'"
            :tone="lastInvoiceRequest ? 'success' : 'neutral'"
          />
        </div>

        <label class="billing-timeline__field">
          <span class="billing-timeline__field-label">账单订单</span>
          <select v-model="invoiceForm.billingOrderId" class="billing-timeline__control">
            <option v-for="order in visibleOrders" :key="order.billingOrderId" :value="order.billingOrderId">
              {{ order.billingOrderId }} / {{ order.planName }}
            </option>
          </select>
        </label>

        <label class="billing-timeline__field">
          <span class="billing-timeline__field-label">发票抬头</span>
          <input v-model="invoiceForm.invoiceTitle" class="billing-timeline__control" type="text" />
        </label>

        <label class="billing-timeline__field">
          <span class="billing-timeline__field-label">税号</span>
          <input v-model="invoiceForm.invoiceTaxNo" class="billing-timeline__control" type="text" />
        </label>

        <div class="billing-timeline__invoice-summary">
          <p class="billing-timeline__meta">
            已选金额 {{ formatCurrency(selectedOrder?.payableAmount) }}
          </p>
          <p class="billing-timeline__meta">
            最近申请
            {{
              lastInvoiceRequest
                ? `${lastInvoiceRequest.invoiceRequestId} / ${formatDateTime(lastInvoiceRequest.createdAt)}`
                : '--'
            }}
          </p>
        </div>

        <button
          class="billing-timeline__solid"
          type="button"
          :disabled="isSubmitting || !invoiceForm.billingOrderId || !invoiceForm.invoiceTitle || !invoiceForm.invoiceTaxNo"
          @click="submitInvoice"
        >
          {{ isSubmitting ? '提交中...' : '提交开票申请' }}
        </button>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.billing-timeline {
  display: grid;
  gap: 1rem;
}

.billing-timeline__item {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.billing-timeline__head {
  display: flex;
  justify-content: space-between;
  gap: 0.85rem;
  align-items: flex-start;
}

.billing-timeline__title,
.billing-timeline__amount {
  margin: 0;
  color: var(--color-ink-strong);
}

.billing-timeline__meta,
.billing-timeline__empty {
  margin: 0.35rem 0 0;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.billing-timeline__amount {
  margin-top: 0.75rem;
  font-size: 1.35rem;
}

.billing-timeline__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 0.9rem;
}

.billing-timeline__solid,
.billing-timeline__ghost {
  min-height: 2.8rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.billing-timeline__solid {
  border: 0;
  background: var(--color-accent);
  color: #ffffff;
}

.billing-timeline__solid:hover {
  background: var(--color-accent-hover);
  color: #ffffff;
}

.billing-timeline__ghost {
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
}

.billing-timeline__ghost:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.billing-timeline__invoice {
  display: grid;
  gap: 0.85rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.billing-timeline__invoice-head {
  display: flex;
  justify-content: space-between;
  gap: 0.85rem;
  align-items: flex-start;
}

.billing-timeline__invoice-eyebrow,
.billing-timeline__field-label {
  margin: 0;
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.billing-timeline__invoice-title {
  margin: 0.45rem 0 0;
  color: var(--color-ink-strong);
}

.billing-timeline__field {
  display: grid;
  gap: 0.55rem;
}

.billing-timeline__control {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--color-line);
  border-radius: calc(var(--radius-lg) - 0.25rem);
  padding: 0.72rem 0.8rem;
  background: var(--color-surface);
  color: var(--color-ink-strong);
  font: inherit;
}

.billing-timeline__invoice-summary {
  display: grid;
  gap: 0.2rem;
}
</style>
