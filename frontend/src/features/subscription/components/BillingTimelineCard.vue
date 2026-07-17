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
    invoiceForm.invoiceTitle = preferredOrder ? `${preferredOrder.planName} subscription bill` : '';
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
    eyebrow="Billing Timeline"
    title="Bills, payment, and invoice actions"
    description="The right column keeps bill download, settlement, and invoice submission in the same operational flow."
    dark
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
          <StatusPill :label="order.paymentStatus" :tone="paymentTone(order.paymentStatus)" />
        </div>
        <p class="billing-timeline__amount">{{ formatCurrency(order.payableAmount) }}</p>
        <p class="billing-timeline__meta">External order {{ order.externalOrderNo || '--' }}</p>
        <div class="billing-timeline__actions">
          <button
            class="billing-timeline__ghost"
            type="button"
            @click="emit('downloadOrder', order.billingOrderId)"
          >
            Download bill
          </button>
          <button
            class="billing-timeline__solid"
            type="button"
            :disabled="isSubmitting || !canSettle(order)"
            @click="emit('settleOrder', order.billingOrderId)"
          >
            {{ !canSettle(order) ? 'Already paid' : isSubmitting ? 'Processing...' : 'Settle payment' }}
          </button>
        </div>
      </article>

      <p v-if="visibleOrders.length === 0" class="billing-timeline__empty">
        No billing orders yet. This panel will be reused as soon as upgrades, renewals, or seat purchases generate new orders.
      </p>

      <div class="billing-timeline__invoice">
        <div class="billing-timeline__invoice-head">
          <div>
            <p class="billing-timeline__invoice-eyebrow">Invoice Request</p>
            <h4 class="billing-timeline__invoice-title">Submit an invoice request</h4>
          </div>
          <StatusPill
            :label="lastInvoiceRequest?.invoiceStatus || 'draft'"
            :tone="lastInvoiceRequest ? 'success' : 'neutral'"
          />
        </div>

        <label class="billing-timeline__field">
          <span class="billing-timeline__field-label">Billing order</span>
          <select v-model="invoiceForm.billingOrderId" class="billing-timeline__control">
            <option v-for="order in visibleOrders" :key="order.billingOrderId" :value="order.billingOrderId">
              {{ order.billingOrderId }} / {{ order.planName }}
            </option>
          </select>
        </label>

        <label class="billing-timeline__field">
          <span class="billing-timeline__field-label">Invoice title</span>
          <input v-model="invoiceForm.invoiceTitle" class="billing-timeline__control" type="text" />
        </label>

        <label class="billing-timeline__field">
          <span class="billing-timeline__field-label">Tax number</span>
          <input v-model="invoiceForm.invoiceTaxNo" class="billing-timeline__control" type="text" />
        </label>

        <div class="billing-timeline__invoice-summary">
          <p class="billing-timeline__meta">
            Selected amount {{ formatCurrency(selectedOrder?.payableAmount) }}
          </p>
          <p class="billing-timeline__meta">
            Latest request
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
          {{ isSubmitting ? 'Submitting...' : 'Submit invoice request' }}
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
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.06);
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
  color: #fff8f0;
}

.billing-timeline__meta,
.billing-timeline__empty {
  margin: 0.35rem 0 0;
  color: rgba(255, 244, 236, 0.72);
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
  background: rgba(255, 248, 240, 0.92);
  color: #2d2017;
}

.billing-timeline__ghost {
  border: 1px solid rgba(255, 244, 236, 0.18);
  background: transparent;
  color: #fff8f0;
}

.billing-timeline__invoice {
  display: grid;
  gap: 0.85rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.05);
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
  color: rgba(255, 244, 236, 0.72);
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.72rem;
}

.billing-timeline__invoice-title {
  margin: 0.45rem 0 0;
  color: #fff8f0;
}

.billing-timeline__field {
  display: grid;
  gap: 0.55rem;
}

.billing-timeline__control {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid rgba(255, 244, 236, 0.18);
  border-radius: calc(var(--radius-lg) - 0.25rem);
  padding: 0.72rem 0.8rem;
  background: rgba(255, 248, 240, 0.12);
  color: #fff8f0;
  font: inherit;
}

.billing-timeline__invoice-summary {
  display: grid;
  gap: 0.2rem;
}
</style>
