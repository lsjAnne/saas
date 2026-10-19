<script setup lang="ts">
import { reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  CustomerPaymentRecord,
  ReceivableLedgerEntry,
  RecordCustomerPaymentPayload
} from '@/services/apiTypes';
import { formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  receivableLedgers: ReceivableLedgerEntry[];
  selectedLedgerId: string | null;
  selectedLedger: ReceivableLedgerEntry | null;
  customerPayments: CustomerPaymentRecord[];
  isSubmitting: boolean;
  actionError?: string | null;
}

interface Emits {
  selectLedger: [receivableLedgerId: string];
  recordPayment: [payload: RecordCustomerPaymentPayload];
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<Emits>();

const paymentForm = reactive({
  orderId: '',
  paymentChannel: 'BANK_TRANSFER',
  paymentAmount: '',
  remark: ''
});

watch(
  () => props.selectedLedger?.receivableLedgerId,
  () => {
    paymentForm.orderId = props.selectedLedger?.orderId ?? '';
    paymentForm.paymentAmount = props.selectedLedger
      ? String(props.selectedLedger.outstandingAmount ?? '')
      : '';
  },
  { immediate: true }
);

function ledgerTone(status: string) {
  const normalized = status.toLowerCase();
  if (normalized === 'collected' || normalized === 'settled') {
    return 'success';
  }

  if (normalized) {
    return 'warn';
  }

  return 'neutral';
}

function submitPayment() {
  const paymentAmount = Number(paymentForm.paymentAmount);
  if (!paymentForm.orderId || !Number.isFinite(paymentAmount) || paymentAmount <= 0) {
    return;
  }

  emit('recordPayment', {
    orderId: paymentForm.orderId,
    paymentChannel: paymentForm.paymentChannel || undefined,
    paymentAmount,
    remark: paymentForm.remark || undefined
  });
}

const labelMap: Record<string, string> = {
  idle: '待处理',
  pending: '待收款',
  partial: '部分到账',
  collected: '已收清',
  settled: '已结算',
  BANK_TRANSFER: '银行转账',
  ALIPAY: '支付宝',
  WECHAT: '微信支付',
  CASH: '现金'
};

function labelOf(value: string) {
  return labelMap[value] ?? value;
}
</script>

<template>
  <PanelCard
    title="应收台账与回款执行"
    description="把应收余额、客户回款和最近到账流水合在一块，让财务可以从未收款项目直接落动作。"
  >
    <div class="receivable-panel">
      <div class="receivable-panel__queue">
        <div class="receivable-panel__section-header">
          <div>
            <p class="receivable-panel__eyebrow">应收队列</p>
            <h4>优先处理未收清账单</h4>
          </div>
        </div>
        <div class="receivable-panel__ledger-list">
          <button
            v-for="ledger in receivableLedgers"
            :key="ledger.receivableLedgerId"
            type="button"
            :class="[
              'receivable-panel__ledger-card',
              { 'receivable-panel__ledger-card--active': ledger.receivableLedgerId === selectedLedgerId }
            ]"
            @click="emit('selectLedger', ledger.receivableLedgerId)"
          >
            <div class="receivable-panel__row-head">
              <strong>{{ ledger.receivableNo }}</strong>
              <StatusPill :label="labelOf(ledger.receivableStatus)" :tone="ledgerTone(ledger.receivableStatus)" />
            </div>
            <p>{{ ledger.customerName }} / {{ ledger.platformOrderId }}</p>
            <p>应收 {{ formatCurrency(ledger.receivableAmount) }}</p>
            <p>未收 {{ formatCurrency(ledger.outstandingAmount) }}</p>
          </button>
          <p v-if="!receivableLedgers.length" class="receivable-panel__empty">当前没有应收台账。</p>
        </div>
      </div>

      <div class="receivable-panel__workspace">
        <div class="receivable-panel__detail-card">
          <div class="receivable-panel__section-header">
            <div>
              <p class="receivable-panel__eyebrow">回款录入</p>
              <h4>{{ selectedLedger?.receivableNo || '等待选择应收记录' }}</h4>
            </div>
            <StatusPill
              :label="labelOf(selectedLedger?.receivableStatus || 'idle')"
              :tone="ledgerTone(selectedLedger?.receivableStatus || '')"
            />
          </div>

          <dl v-if="selectedLedger" class="receivable-panel__detail-grid">
            <div>
              <dt>客户</dt>
              <dd>{{ selectedLedger.customerName }}</dd>
            </div>
            <div>
              <dt>平台单号</dt>
              <dd>{{ selectedLedger.platformOrderId }}</dd>
            </div>
            <div>
              <dt>发生时间</dt>
              <dd>{{ formatDateTime(selectedLedger.occurredAt) }}</dd>
            </div>
            <div>
              <dt>已收</dt>
              <dd>{{ formatCurrency(selectedLedger.collectedAmount) }}</dd>
            </div>
            <div>
              <dt>余额</dt>
              <dd>{{ formatCurrency(selectedLedger.outstandingAmount) }}</dd>
            </div>
            <div>
              <dt>订单</dt>
              <dd>{{ selectedLedger.orderId }}</dd>
            </div>
          </dl>

          <div class="receivable-panel__form-grid">
            <label class="receivable-panel__field">
              <span>订单 ID</span>
              <input v-model="paymentForm.orderId" class="receivable-panel__control" type="text" />
            </label>
            <label class="receivable-panel__field">
              <span>回款渠道</span>
              <select v-model="paymentForm.paymentChannel" class="receivable-panel__control">
                <option value="BANK_TRANSFER">银行转账</option>
                <option value="ALIPAY">支付宝</option>
                <option value="WECHAT">微信支付</option>
                <option value="CASH">现金</option>
              </select>
            </label>
            <label class="receivable-panel__field">
              <span>回款金额</span>
              <input v-model="paymentForm.paymentAmount" class="receivable-panel__control" type="number" min="0" step="0.01" />
            </label>
            <label class="receivable-panel__field">
              <span>备注</span>
              <input v-model="paymentForm.remark" class="receivable-panel__control" type="text" />
            </label>
          </div>

          <button class="receivable-panel__primary" type="button" :disabled="isSubmitting" @click="submitPayment">
            记录回款
          </button>
          <div v-if="actionError" class="receivable-panel__error">{{ actionError }}</div>
        </div>

        <div class="receivable-panel__payments-card">
          <div class="receivable-panel__section-header">
            <div>
              <p class="receivable-panel__eyebrow">最近回款</p>
              <h4>{{ customerPayments.length }} 条到账流水</h4>
            </div>
          </div>
          <div v-if="customerPayments.length" class="receivable-panel__payment-list">
            <article
              v-for="payment in customerPayments.slice(0, 6)"
              :key="payment.paymentRecordId"
              class="receivable-panel__payment-card"
            >
              <div class="receivable-panel__row-head">
                <strong>{{ payment.paymentNo }}</strong>
                <StatusPill :label="labelOf(payment.paymentStatus)" :tone="ledgerTone(payment.paymentStatus)" />
              </div>
              <p>{{ payment.customerName }} / {{ labelOf(payment.paymentChannel) }}</p>
              <p>到账 {{ formatCurrency(payment.paymentAmount) }}</p>
              <p>{{ formatDateTime(payment.receivedAt) }}</p>
            </article>
          </div>
          <p v-else class="receivable-panel__empty">暂时还没有回款记录。</p>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.receivable-panel {
  display: grid;
  grid-template-columns: 0.88fr 1.12fr;
  gap: 1rem;
}

.receivable-panel__queue,
.receivable-panel__workspace,
.receivable-panel__detail-card,
.receivable-panel__payments-card {
  display: grid;
  gap: 0.9rem;
}

.receivable-panel__ledger-list,
.receivable-panel__payment-list {
  display: grid;
  gap: 0.75rem;
}

.receivable-panel__ledger-card,
.receivable-panel__payment-card {
  display: grid;
  gap: 0.4rem;
  padding: 0.95rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
  text-align: left;
}

.receivable-panel__ledger-card {
  cursor: pointer;
}

.receivable-panel__ledger-card--active {
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
}

.receivable-panel__detail-card,
.receivable-panel__payments-card {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.receivable-panel__section-header,
.receivable-panel__row-head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.receivable-panel__eyebrow,
.receivable-panel__field span,
.receivable-panel__detail-grid dt,
.receivable-panel__ledger-card p,
.receivable-panel__payment-card p,
.receivable-panel__empty,
.receivable-panel__error,
.receivable-panel__detail-card h4,
.receivable-panel__payments-card h4 {
  margin: 0;
}

.receivable-panel__eyebrow,
.receivable-panel__field span,
.receivable-panel__detail-grid dt {
  color: var(--color-ink-soft);
  font-size: 0.78rem;
  font-weight: 600;
}

.receivable-panel__detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
  margin: 0;
}

.receivable-panel__detail-grid div {
  padding: 0.8rem;
  border-radius: var(--radius-md);
  background: var(--color-panel-strong);
  border: 1px solid var(--color-line);
}

.receivable-panel__detail-grid dd {
  margin: 0.35rem 0 0;
  color: var(--color-ink-strong);
}

.receivable-panel__form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.receivable-panel__field {
  display: grid;
  gap: 0.45rem;
}

.receivable-panel__control,
.receivable-panel__primary {
  min-height: 2.8rem;
  border-radius: var(--radius-md);
}

.receivable-panel__control {
  width: 100%;
  border: 1px solid var(--color-line);
  background: #ffffff;
  padding: 0 0.85rem;
}

.receivable-panel__primary {
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #ffffff;
  font-weight: 600;
  cursor: pointer;
}

.receivable-panel__primary:hover {
  background: var(--color-accent-hover);
}

.receivable-panel__error {
  color: var(--color-danger);
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .receivable-panel,
  .receivable-panel__detail-grid,
  .receivable-panel__form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
