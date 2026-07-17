<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  FinanceBill,
  FinanceBillDetailView,
  FinanceBillReconcileView,
  GenerateFinanceBillPayload,
  SettleFinanceBillPayload,
  Store
} from '@/services/apiTypes';
import { formatCurrency, formatDate, formatDateTime } from '@/utils/formatters';

interface Props {
  stores: Store[];
  activeStoreId: string;
  bills: FinanceBill[];
  selectedBillId: string | null;
  selectedBill: FinanceBill | null;
  selectedBillDetail: FinanceBillDetailView | null;
  lastReconcileView: FinanceBillReconcileView | null;
  analysisPeriodStart: string;
  analysisPeriodEnd: string;
  isSubmitting: boolean;
  actionError?: string | null;
}

interface Emits {
  selectStore: [storeId: string];
  selectBill: [financeBillId: string];
  generateBill: [payload: GenerateFinanceBillPayload];
  reconcileBill: [];
  settleBill: [payload: SettleFinanceBillPayload];
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<Emits>();

const generateForm = reactive({
  storeId: '',
  billType: 'ORDER_SETTLEMENT',
  periodStart: '',
  periodEnd: ''
});

const settleForm = reactive({
  settlementType: 'BANK_TRANSFER'
});

const defaultStoreId = computed(() => {
  if (props.activeStoreId !== 'all') {
    return props.activeStoreId;
  }

  return props.selectedBill?.storeId ?? props.bills[0]?.storeId ?? props.stores[0]?.storeId ?? '';
});

watch(
  () => [
    defaultStoreId.value,
    props.analysisPeriodStart,
    props.analysisPeriodEnd,
    props.selectedBill?.periodStart,
    props.selectedBill?.periodEnd
  ],
  () => {
    generateForm.storeId = defaultStoreId.value;
    generateForm.periodStart = props.selectedBill?.periodStart ?? props.analysisPeriodStart;
    generateForm.periodEnd = props.selectedBill?.periodEnd ?? props.analysisPeriodEnd;
  },
  { immediate: true }
);

function billTone(status: string) {
  const normalized = status.toLowerCase();
  if (normalized === 'settled' || normalized === 'closed') {
    return 'success';
  }

  if (normalized) {
    return 'warn';
  }

  return 'neutral';
}

function submitGenerateBill() {
  emit('generateBill', {
    storeId: generateForm.storeId,
    billType: generateForm.billType,
    periodStart: generateForm.periodStart,
    periodEnd: generateForm.periodEnd
  });
}

function submitSettleBill() {
  emit('settleBill', {
    settlementType: settleForm.settlementType || undefined
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Bill Board"
    title="账单总览与结算推进"
    description="先把账单生成、详情、对账和结算放到一个面板里，确保财务主链第一跳可看、可追、可执行。"
  >
    <div class="bill-board">
      <div class="bill-board__sidebar">
        <div class="bill-board__toolbar">
          <label class="bill-board__field">
            <span>筛选店铺</span>
            <select
              class="bill-board__control"
              :value="activeStoreId"
              @change="emit('selectStore', ($event.target as HTMLSelectElement).value)"
            >
              <option value="all">全部店铺</option>
              <option v-for="store in stores" :key="store.storeId" :value="store.storeId">
                {{ store.shopName }}
              </option>
            </select>
          </label>
        </div>

        <div class="bill-board__list">
          <button
            v-for="bill in bills"
            :key="bill.financeBillId"
            type="button"
            :class="[
              'bill-board__bill-card',
              { 'bill-board__bill-card--active': bill.financeBillId === selectedBillId }
            ]"
            @click="emit('selectBill', bill.financeBillId)"
          >
            <div class="bill-board__bill-card-header">
              <strong>{{ bill.financeBillId }}</strong>
              <StatusPill :label="bill.billStatus" :tone="billTone(bill.billStatus)" />
            </div>
            <p>{{ formatDate(bill.periodStart) }} - {{ formatDate(bill.periodEnd) }}</p>
            <p>收入 {{ formatCurrency(bill.incomeAmount) }}</p>
            <p>毛利 {{ formatCurrency(bill.grossProfit) }}</p>
          </button>
          <p v-if="!bills.length" class="bill-board__empty">当前筛选范围还没有账单。</p>
        </div>

        <div class="bill-board__form-block">
          <h4>生成新账单</h4>
          <div class="bill-board__form-grid">
            <label class="bill-board__field">
              <span>店铺</span>
              <select v-model="generateForm.storeId" class="bill-board__control">
                <option v-for="store in stores" :key="store.storeId" :value="store.storeId">
                  {{ store.shopName }}
                </option>
              </select>
            </label>
            <label class="bill-board__field">
              <span>账单类型</span>
              <select v-model="generateForm.billType" class="bill-board__control">
                <option value="ORDER_SETTLEMENT">ORDER_SETTLEMENT</option>
                <option value="RECEIVABLE_SETTLEMENT">RECEIVABLE_SETTLEMENT</option>
                <option value="PROFIT_SUMMARY">PROFIT_SUMMARY</option>
              </select>
            </label>
            <label class="bill-board__field">
              <span>开始日期</span>
              <input v-model="generateForm.periodStart" class="bill-board__control" type="date" />
            </label>
            <label class="bill-board__field">
              <span>结束日期</span>
              <input v-model="generateForm.periodEnd" class="bill-board__control" type="date" />
            </label>
          </div>
          <button class="bill-board__primary" type="button" :disabled="isSubmitting" @click="submitGenerateBill">
            生成账单
          </button>
        </div>
      </div>

      <div class="bill-board__content">
        <div class="bill-board__detail-card">
          <div class="bill-board__detail-header">
            <div>
              <p class="bill-board__detail-eyebrow">账单详情</p>
              <h4>{{ selectedBill?.financeBillId || '等待选择账单' }}</h4>
            </div>
            <StatusPill
              :label="selectedBill?.billStatus || 'idle'"
              :tone="billTone(selectedBill?.billStatus || '')"
            />
          </div>

          <dl v-if="selectedBill" class="bill-board__detail-grid">
            <div>
              <dt>账单类型</dt>
              <dd>{{ selectedBill.billType }}</dd>
            </div>
            <div>
              <dt>生成时间</dt>
              <dd>{{ formatDateTime(selectedBill.createdAt) }}</dd>
            </div>
            <div>
              <dt>收入</dt>
              <dd>{{ formatCurrency(selectedBill.incomeAmount) }}</dd>
            </div>
            <div>
              <dt>成本</dt>
              <dd>{{ formatCurrency(selectedBill.costAmount) }}</dd>
            </div>
            <div>
              <dt>毛利</dt>
              <dd>{{ formatCurrency(selectedBill.grossProfit) }}</dd>
            </div>
            <div>
              <dt>周期</dt>
              <dd>{{ formatDate(selectedBill.periodStart) }} - {{ formatDate(selectedBill.periodEnd) }}</dd>
            </div>
          </dl>

          <div class="bill-board__insight-strip">
            <article>
              <span>拆分状态</span>
              <strong>{{ selectedBillDetail?.splitStatus || '--' }}</strong>
            </article>
            <article>
              <span>发票校验</span>
              <strong>{{ selectedBillDetail?.invoiceCheckStatus || '--' }}</strong>
            </article>
            <article>
              <span>对账差异</span>
              <strong>{{ lastReconcileView?.discrepancyCount ?? '--' }}</strong>
            </article>
          </div>

          <div class="bill-board__actions">
            <button type="button" class="bill-board__secondary" :disabled="!selectedBill || isSubmitting" @click="emit('reconcileBill')">
              发起对账
            </button>
            <label class="bill-board__field bill-board__field--inline">
              <span>结算方式</span>
              <select v-model="settleForm.settlementType" class="bill-board__control">
                <option value="BANK_TRANSFER">BANK_TRANSFER</option>
                <option value="ALIPAY">ALIPAY</option>
                <option value="WECHAT">WECHAT</option>
                <option value="OFFLINE">OFFLINE</option>
              </select>
            </label>
            <button type="button" class="bill-board__primary" :disabled="!selectedBill || isSubmitting" @click="submitSettleBill">
              提交结算
            </button>
          </div>

          <div v-if="actionError" class="bill-board__error">{{ actionError }}</div>
        </div>

        <div class="bill-board__settlements">
          <div class="bill-board__detail-header">
            <div>
              <p class="bill-board__detail-eyebrow">结算流水</p>
              <h4>已关联 {{ selectedBillDetail?.settlements.length ?? 0 }} 条</h4>
            </div>
          </div>
          <div v-if="selectedBillDetail?.settlements.length" class="bill-board__settlement-list">
            <article
              v-for="settlement in selectedBillDetail.settlements"
              :key="settlement.settlementRecordId"
              class="bill-board__settlement-card"
            >
              <div class="bill-board__bill-card-header">
                <strong>{{ settlement.settlementType }}</strong>
                <StatusPill :label="settlement.settlementStatus" :tone="billTone(settlement.settlementStatus)" />
              </div>
              <p>金额 {{ formatCurrency(settlement.settlementAmount) }}</p>
              <p>创建 {{ formatDateTime(settlement.createdAt) }}</p>
              <p>结算 {{ formatDateTime(settlement.settledAt) }}</p>
            </article>
          </div>
          <p v-else class="bill-board__empty">当前账单还没有结算流水。</p>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.bill-board {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 1rem;
}

.bill-board__sidebar,
.bill-board__content,
.bill-board__toolbar,
.bill-board__form-block,
.bill-board__detail-card,
.bill-board__settlements {
  display: grid;
  gap: 0.9rem;
}

.bill-board__list,
.bill-board__settlement-list {
  display: grid;
  gap: 0.75rem;
}

.bill-board__bill-card,
.bill-board__settlement-card {
  display: grid;
  gap: 0.45rem;
  padding: 0.95rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.1);
  background: rgba(255, 255, 255, 0.74);
  text-align: left;
}

.bill-board__bill-card {
  cursor: pointer;
}

.bill-board__bill-card--active {
  border-color: rgba(113, 87, 62, 0.44);
  box-shadow: inset 0 0 0 1px rgba(113, 87, 62, 0.14);
}

.bill-board__bill-card p,
.bill-board__settlement-card p,
.bill-board__empty,
.bill-board__error,
.bill-board__detail-eyebrow,
.bill-board__detail-card h4,
.bill-board__settlements h4 {
  margin: 0;
}

.bill-board__detail-card,
.bill-board__settlements {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 251, 247, 0.78);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.bill-board__detail-header,
.bill-board__bill-card-header,
.bill-board__actions {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 0.8rem;
}

.bill-board__detail-eyebrow,
.bill-board__field span {
  color: var(--color-ink-faint);
  font-size: 0.75rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.bill-board__detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
  margin: 0;
}

.bill-board__detail-grid div,
.bill-board__insight-strip article {
  padding: 0.8rem;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.76);
}

.bill-board__detail-grid dt,
.bill-board__insight-strip span {
  margin: 0 0 0.35rem;
  color: var(--color-ink-faint);
  font-size: 0.78rem;
}

.bill-board__detail-grid dd,
.bill-board__insight-strip strong {
  margin: 0;
  color: var(--color-ink-strong);
}

.bill-board__insight-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.bill-board__field {
  display: grid;
  gap: 0.45rem;
}

.bill-board__field--inline {
  min-width: 12rem;
  flex: 1;
}

.bill-board__form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.bill-board__control,
.bill-board__primary,
.bill-board__secondary {
  min-height: 2.8rem;
  border-radius: var(--radius-md);
}

.bill-board__control {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.16);
  background: rgba(255, 255, 255, 0.92);
  padding: 0 0.85rem;
  color: var(--color-ink-strong);
}

.bill-board__primary,
.bill-board__secondary {
  border: 0;
  padding: 0 1rem;
  font-weight: 600;
  cursor: pointer;
}

.bill-board__primary {
  background: linear-gradient(135deg, #3f5f55, #1f2c28);
  color: #fffaf4;
}

.bill-board__secondary {
  background: rgba(61, 46, 36, 0.08);
  color: var(--color-ink-strong);
}

.bill-board__error {
  color: #b55d2f;
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .bill-board {
    grid-template-columns: 1fr;
  }

  .bill-board__detail-grid,
  .bill-board__insight-strip,
  .bill-board__form-grid {
    grid-template-columns: 1fr;
  }

  .bill-board__actions {
    flex-direction: column;
  }
}
</style>
