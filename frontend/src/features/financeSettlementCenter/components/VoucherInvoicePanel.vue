<script setup lang="ts">
import { computed, reactive, shallowRef, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  ArchiveFinanceVoucherPayload,
  CloseFinancePeriodPayload,
  FinanceBill,
  FinanceInvoice,
  FinancePeriodClosingRecord,
  FinanceVoucher,
  IssueFinanceInvoicePayload,
  Store
} from '@/services/apiTypes';
import { formatCurrency, formatDate, formatDateTime } from '@/utils/formatters';

interface Props {
  stores: Store[];
  activeStoreId: string;
  selectedStoreName: string;
  selectedBill: FinanceBill | null;
  invoices: FinanceInvoice[];
  selectedInvoiceId: string | null;
  selectedInvoice: FinanceInvoice | null;
  vouchers: FinanceVoucher[];
  selectedVoucherId: string | null;
  selectedVoucher: FinanceVoucher | null;
  closings: FinancePeriodClosingRecord[];
  analysisPeriodStart: string;
  analysisPeriodEnd: string;
  isSubmitting: boolean;
  actionError?: string | null;
}

interface Emits {
  selectInvoice: [invoiceId: string];
  selectVoucher: [voucherId: string];
  issueInvoice: [payload: IssueFinanceInvoicePayload];
  archiveInvoice: [remark?: string];
  voidInvoice: [remark?: string];
  redFlushInvoice: [remark?: string];
  archiveVoucher: [payload: ArchiveFinanceVoucherPayload];
  closePeriod: [payload: CloseFinancePeriodPayload];
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<Emits>();

const invoiceForm = reactive({
  storeId: '',
  referenceType: 'FINANCE_BILL',
  referenceId: '',
  invoiceTitle: '',
  invoiceTaxNo: '',
  invoiceAmount: '',
  remark: ''
});

const voucherForm = reactive({
  storeId: '',
  referenceType: 'FINANCE_BILL',
  referenceId: '',
  voucherType: 'SETTLEMENT',
  voucherAmount: '',
  remark: ''
});

const closeForm = reactive({
  storeId: '',
  periodStart: '',
  periodEnd: '',
  remark: ''
});

const invoiceRemark = shallowRef('');

const defaultStoreId = computed(() => {
  if (props.activeStoreId !== 'all') {
    return props.activeStoreId;
  }

  return props.selectedBill?.storeId ?? props.stores[0]?.storeId ?? '';
});

watch(
  () => [
    defaultStoreId.value,
    props.selectedBill?.financeBillId,
    props.analysisPeriodStart,
    props.analysisPeriodEnd
  ],
  () => {
    invoiceForm.storeId = defaultStoreId.value;
    invoiceForm.referenceId = props.selectedBill?.financeBillId ?? '';
    invoiceForm.invoiceAmount = props.selectedBill ? String(props.selectedBill.incomeAmount) : '';

    voucherForm.storeId = defaultStoreId.value;
    voucherForm.referenceId = props.selectedBill?.financeBillId ?? '';
    voucherForm.voucherAmount = props.selectedBill ? String(props.selectedBill.grossProfit) : '';

    closeForm.storeId = defaultStoreId.value;
    closeForm.periodStart = props.analysisPeriodStart;
    closeForm.periodEnd = props.analysisPeriodEnd;
  },
  { immediate: true }
);

function statusTone(status: string | null | undefined) {
  const normalized = (status ?? '').toLowerCase();
  if (normalized.includes('archived') || normalized.includes('issued') || normalized.includes('closed')) {
    return 'success';
  }

  if (normalized) {
    return 'warn';
  }

  return 'neutral';
}

function submitIssueInvoice() {
  const invoiceAmount = Number(invoiceForm.invoiceAmount);
  if (
    !invoiceForm.storeId ||
    !invoiceForm.referenceId ||
    !invoiceForm.invoiceTitle ||
    !invoiceForm.invoiceTaxNo ||
    !Number.isFinite(invoiceAmount) ||
    invoiceAmount <= 0
  ) {
    return;
  }

  emit('issueInvoice', {
    storeId: invoiceForm.storeId,
    referenceType: invoiceForm.referenceType,
    referenceId: invoiceForm.referenceId,
    invoiceTitle: invoiceForm.invoiceTitle,
    invoiceTaxNo: invoiceForm.invoiceTaxNo,
    invoiceAmount,
    remark: invoiceForm.remark || undefined
  });
}

function submitArchiveVoucher() {
  const voucherAmount = Number(voucherForm.voucherAmount);
  if (
    !voucherForm.storeId ||
    !voucherForm.referenceId ||
    !voucherForm.voucherType ||
    !Number.isFinite(voucherAmount) ||
    voucherAmount <= 0
  ) {
    return;
  }

  emit('archiveVoucher', {
    storeId: voucherForm.storeId,
    referenceType: voucherForm.referenceType,
    referenceId: voucherForm.referenceId,
    voucherType: voucherForm.voucherType,
    voucherAmount,
    remark: voucherForm.remark || undefined
  });
}

function submitClosePeriod() {
  if (!closeForm.storeId || !closeForm.periodStart || !closeForm.periodEnd) {
    return;
  }

  emit('closePeriod', {
    storeId: closeForm.storeId,
    periodStart: closeForm.periodStart,
    periodEnd: closeForm.periodEnd,
    remark: closeForm.remark || undefined
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Invoice / Voucher / Closing"
    title="发票、凭证与关账治理"
    description="先把票据和凭证入口做成可操作面板，再把期间关账作为同页的最后一道动作门禁。"
  >
    <div class="voucher-panel">
      <div class="voucher-panel__forms">
        <div class="voucher-panel__card">
          <div class="voucher-panel__section-head">
            <div>
              <p class="voucher-panel__eyebrow">开具发票</p>
              <h4>{{ selectedBill?.financeBillId || selectedStoreName }}</h4>
            </div>
          </div>
          <div class="voucher-panel__form-grid">
            <label class="voucher-panel__field">
              <span>店铺</span>
              <select v-model="invoiceForm.storeId" class="voucher-panel__control">
                <option v-for="store in stores" :key="store.storeId" :value="store.storeId">
                  {{ store.shopName }}
                </option>
              </select>
            </label>
            <label class="voucher-panel__field">
              <span>引用类型</span>
              <select v-model="invoiceForm.referenceType" class="voucher-panel__control">
                <option value="FINANCE_BILL">FINANCE_BILL</option>
                <option value="ORDER">ORDER</option>
              </select>
            </label>
            <label class="voucher-panel__field">
              <span>引用 ID</span>
              <input v-model="invoiceForm.referenceId" class="voucher-panel__control" type="text" />
            </label>
            <label class="voucher-panel__field">
              <span>发票抬头</span>
              <input v-model="invoiceForm.invoiceTitle" class="voucher-panel__control" type="text" />
            </label>
            <label class="voucher-panel__field">
              <span>税号</span>
              <input v-model="invoiceForm.invoiceTaxNo" class="voucher-panel__control" type="text" />
            </label>
            <label class="voucher-panel__field">
              <span>发票金额</span>
              <input v-model="invoiceForm.invoiceAmount" class="voucher-panel__control" type="number" min="0" step="0.01" />
            </label>
            <label class="voucher-panel__field voucher-panel__field--wide">
              <span>备注</span>
              <input v-model="invoiceForm.remark" class="voucher-panel__control" type="text" />
            </label>
          </div>
          <button class="voucher-panel__primary" type="button" :disabled="isSubmitting" @click="submitIssueInvoice">
            开具发票
          </button>
        </div>

        <div class="voucher-panel__card">
          <div class="voucher-panel__section-head">
            <div>
              <p class="voucher-panel__eyebrow">归档凭证</p>
              <h4>{{ selectedBill?.financeBillId || selectedStoreName }}</h4>
            </div>
          </div>
          <div class="voucher-panel__form-grid">
            <label class="voucher-panel__field">
              <span>店铺</span>
              <select v-model="voucherForm.storeId" class="voucher-panel__control">
                <option v-for="store in stores" :key="store.storeId" :value="store.storeId">
                  {{ store.shopName }}
                </option>
              </select>
            </label>
            <label class="voucher-panel__field">
              <span>引用类型</span>
              <select v-model="voucherForm.referenceType" class="voucher-panel__control">
                <option value="FINANCE_BILL">FINANCE_BILL</option>
                <option value="ORDER">ORDER</option>
                <option value="PAYMENT">PAYMENT</option>
              </select>
            </label>
            <label class="voucher-panel__field">
              <span>引用 ID</span>
              <input v-model="voucherForm.referenceId" class="voucher-panel__control" type="text" />
            </label>
            <label class="voucher-panel__field">
              <span>凭证类型</span>
              <select v-model="voucherForm.voucherType" class="voucher-panel__control">
                <option value="SETTLEMENT">SETTLEMENT</option>
                <option value="RECEIVABLE">RECEIVABLE</option>
                <option value="PROFIT">PROFIT</option>
              </select>
            </label>
            <label class="voucher-panel__field">
              <span>凭证金额</span>
              <input v-model="voucherForm.voucherAmount" class="voucher-panel__control" type="number" min="0" step="0.01" />
            </label>
            <label class="voucher-panel__field voucher-panel__field--wide">
              <span>备注</span>
              <input v-model="voucherForm.remark" class="voucher-panel__control" type="text" />
            </label>
          </div>
          <button class="voucher-panel__secondary" type="button" :disabled="isSubmitting" @click="submitArchiveVoucher">
            归档凭证
          </button>
        </div>

        <div class="voucher-panel__card">
          <div class="voucher-panel__section-head">
            <div>
              <p class="voucher-panel__eyebrow">期间关账</p>
              <h4>{{ selectedStoreName }}</h4>
            </div>
          </div>
          <div class="voucher-panel__form-grid">
            <label class="voucher-panel__field">
              <span>店铺</span>
              <select v-model="closeForm.storeId" class="voucher-panel__control">
                <option v-for="store in stores" :key="store.storeId" :value="store.storeId">
                  {{ store.shopName }}
                </option>
              </select>
            </label>
            <label class="voucher-panel__field">
              <span>开始日期</span>
              <input v-model="closeForm.periodStart" class="voucher-panel__control" type="date" />
            </label>
            <label class="voucher-panel__field">
              <span>结束日期</span>
              <input v-model="closeForm.periodEnd" class="voucher-panel__control" type="date" />
            </label>
            <label class="voucher-panel__field voucher-panel__field--wide">
              <span>备注</span>
              <input v-model="closeForm.remark" class="voucher-panel__control" type="text" />
            </label>
          </div>
          <button class="voucher-panel__primary" type="button" :disabled="isSubmitting" @click="submitClosePeriod">
            提交关账
          </button>
        </div>
      </div>

      <div class="voucher-panel__lists">
        <div class="voucher-panel__card">
          <div class="voucher-panel__section-head">
            <div>
              <p class="voucher-panel__eyebrow">发票列表</p>
              <h4>{{ invoices.length }} 张</h4>
            </div>
          </div>
          <div class="voucher-panel__list">
            <button
              v-for="invoice in invoices.slice(0, 6)"
              :key="invoice.invoiceId"
              type="button"
              :class="[
                'voucher-panel__list-card',
                { 'voucher-panel__list-card--active': invoice.invoiceId === selectedInvoiceId }
              ]"
              @click="emit('selectInvoice', invoice.invoiceId)"
            >
              <div class="voucher-panel__list-row">
                <strong>{{ invoice.invoiceNo }}</strong>
                <StatusPill :label="invoice.invoiceStatus" :tone="statusTone(invoice.invoiceStatus)" />
              </div>
              <p>{{ invoice.invoiceTitle }}</p>
              <p>{{ formatCurrency(invoice.invoiceAmount) }}</p>
              <p>{{ formatDateTime(invoice.updatedAt) }}</p>
            </button>
          </div>

          <label class="voucher-panel__field">
            <span>票据动作备注</span>
            <input v-model="invoiceRemark" class="voucher-panel__control" type="text" />
          </label>

          <div class="voucher-panel__action-row">
            <button class="voucher-panel__secondary" type="button" :disabled="!selectedInvoice || isSubmitting" @click="emit('archiveInvoice', invoiceRemark || undefined)">
              归档发票
            </button>
            <button class="voucher-panel__secondary" type="button" :disabled="!selectedInvoice || isSubmitting" @click="emit('voidInvoice', invoiceRemark || undefined)">
              作废发票
            </button>
            <button class="voucher-panel__secondary" type="button" :disabled="!selectedInvoice || isSubmitting" @click="emit('redFlushInvoice', invoiceRemark || undefined)">
              红冲发票
            </button>
          </div>
        </div>

        <div class="voucher-panel__card">
          <div class="voucher-panel__section-head">
            <div>
              <p class="voucher-panel__eyebrow">凭证与关账历史</p>
              <h4>{{ vouchers.length }} 张凭证 / {{ closings.length }} 次关账</h4>
            </div>
          </div>
          <div class="voucher-panel__list">
            <button
              v-for="voucher in vouchers.slice(0, 5)"
              :key="voucher.voucherId"
              type="button"
              :class="[
                'voucher-panel__list-card',
                { 'voucher-panel__list-card--active': voucher.voucherId === selectedVoucherId }
              ]"
              @click="emit('selectVoucher', voucher.voucherId)"
            >
              <div class="voucher-panel__list-row">
                <strong>{{ voucher.voucherNo }}</strong>
                <StatusPill :label="voucher.archiveStatus" :tone="statusTone(voucher.archiveStatus)" />
              </div>
              <p>{{ voucher.referenceType }} / {{ voucher.referenceId }}</p>
              <p>{{ formatCurrency(voucher.voucherAmount) }}</p>
              <p>{{ formatDateTime(voucher.archivedAt) }}</p>
            </button>
          </div>

          <div class="voucher-panel__closing-list">
            <article
              v-for="closing in closings.slice(0, 4)"
              :key="closing.closingRecordId"
              class="voucher-panel__closing-card"
            >
              <div class="voucher-panel__list-row">
                <strong>{{ closing.closingNo }}</strong>
                <StatusPill :label="closing.closingStatus" :tone="statusTone(closing.closingStatus)" />
              </div>
              <p>{{ formatDate(closing.periodStart) }} - {{ formatDate(closing.periodEnd) }}</p>
              <p>账单 {{ closing.linkedFinanceBillCount }} / 凭证 {{ closing.archivedVoucherCount }}</p>
              <p>{{ formatDateTime(closing.closedAt) }}</p>
            </article>
          </div>
        </div>

        <div v-if="actionError" class="voucher-panel__error">{{ actionError }}</div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.voucher-panel {
  display: grid;
  grid-template-columns: 1.02fr 0.98fr;
  gap: 1rem;
}

.voucher-panel__forms,
.voucher-panel__lists,
.voucher-panel__card {
  display: grid;
  gap: 0.9rem;
}

.voucher-panel__card {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 251, 247, 0.78);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.voucher-panel__section-head,
.voucher-panel__list-row,
.voucher-panel__action-row {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.voucher-panel__form-grid,
.voucher-panel__list,
.voucher-panel__closing-list {
  display: grid;
  gap: 0.75rem;
}

.voucher-panel__form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.voucher-panel__field {
  display: grid;
  gap: 0.45rem;
}

.voucher-panel__field--wide {
  grid-column: span 2;
}

.voucher-panel__eyebrow,
.voucher-panel__field span,
.voucher-panel__card h4,
.voucher-panel__list-card p,
.voucher-panel__closing-card p,
.voucher-panel__error {
  margin: 0;
}

.voucher-panel__eyebrow,
.voucher-panel__field span {
  color: var(--color-ink-faint);
  font-size: 0.75rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.voucher-panel__control,
.voucher-panel__primary,
.voucher-panel__secondary {
  min-height: 2.8rem;
  border-radius: var(--radius-md);
}

.voucher-panel__control {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.16);
  background: rgba(255, 255, 255, 0.92);
  padding: 0 0.85rem;
}

.voucher-panel__primary,
.voucher-panel__secondary {
  border: 0;
  padding: 0 1rem;
  font-weight: 600;
  cursor: pointer;
}

.voucher-panel__primary {
  background: linear-gradient(135deg, #3f5f55, #1f2c28);
  color: #fffaf4;
}

.voucher-panel__secondary {
  background: rgba(61, 46, 36, 0.08);
  color: var(--color-ink-strong);
}

.voucher-panel__list-card,
.voucher-panel__closing-card {
  display: grid;
  gap: 0.35rem;
  padding: 0.85rem;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(91, 72, 54, 0.08);
  text-align: left;
}

.voucher-panel__list-card {
  cursor: pointer;
}

.voucher-panel__list-card--active {
  border-color: rgba(113, 87, 62, 0.44);
}

.voucher-panel__error {
  color: #b55d2f;
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .voucher-panel,
  .voucher-panel__form-grid {
    grid-template-columns: 1fr;
  }

  .voucher-panel__field--wide {
    grid-column: span 1;
  }

  .voucher-panel__action-row {
    flex-direction: column;
  }
}
</style>
