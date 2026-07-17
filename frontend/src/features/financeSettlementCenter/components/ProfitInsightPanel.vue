<script setup lang="ts">
import { reactive, watch } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  FinanceClosingCheckView,
  FinanceGeneralLedgerView,
  ProfitStatementView,
  StoreProfitReportView
} from '@/services/apiTypes';
import { formatCurrency, formatDate, formatPercent } from '@/utils/formatters';

interface Props {
  analysisPeriodStart: string;
  analysisPeriodEnd: string;
  selectedStoreName: string;
  profitStatement: ProfitStatementView | null;
  storeProfitReports: StoreProfitReportView[];
  generalLedgers: FinanceGeneralLedgerView[];
  financeClosingCheck: FinanceClosingCheckView | null;
  isRefreshing: boolean;
}

interface Emits {
  updatePeriod: [payload: { periodStart: string; periodEnd: string }];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const periodForm = reactive({
  periodStart: '',
  periodEnd: ''
});

watch(
  () => [props.analysisPeriodStart, props.analysisPeriodEnd],
  () => {
    periodForm.periodStart = props.analysisPeriodStart;
    periodForm.periodEnd = props.analysisPeriodEnd;
  },
  { immediate: true }
);

function statusTone(status: string | null | undefined) {
  const normalized = (status ?? '').toLowerCase();
  if (normalized.includes('ready') || normalized.includes('healthy') || normalized.includes('pass')) {
    return 'success';
  }

  if (normalized) {
    return 'warn';
  }

  return 'neutral';
}

function submitPeriod() {
  emit('updatePeriod', {
    periodStart: periodForm.periodStart,
    periodEnd: periodForm.periodEnd
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Profit & Ledger"
    title="利润洞察、总账摘要与关账检查"
    description="这一块负责让财务先看到利润面和总账面，再决定是否具备真正的关账条件。"
  >
    <div class="profit-panel">
      <div class="profit-panel__hero">
        <div class="profit-panel__hero-copy">
          <p class="profit-panel__eyebrow">分析窗口</p>
          <h4>{{ selectedStoreName }}</h4>
          <p class="profit-panel__hero-text">
            当前窗口 {{ formatDate(analysisPeriodStart) }} - {{ formatDate(analysisPeriodEnd) }}，
            可直接切换期间，重新回读利润表、门店利润报表、总账和关账检查。
          </p>
        </div>
        <div class="profit-panel__period-form">
          <label class="profit-panel__field">
            <span>开始日期</span>
            <input v-model="periodForm.periodStart" class="profit-panel__control" type="date" />
          </label>
          <label class="profit-panel__field">
            <span>结束日期</span>
            <input v-model="periodForm.periodEnd" class="profit-panel__control" type="date" />
          </label>
          <button class="profit-panel__primary" type="button" :disabled="isRefreshing" @click="submitPeriod">
            刷新分析
          </button>
        </div>
      </div>

      <div class="profit-panel__metrics">
        <MetricCard
          label="销售收入"
          :value="formatCurrency(profitStatement?.salesIncomeAmount)"
          caption="当前焦点店铺利润表"
        />
        <MetricCard
          label="净利润"
          :value="formatCurrency(profitStatement?.netProfitAmount)"
          caption="扣除分摊费用后的结果"
        />
        <MetricCard
          label="利润率"
          :value="formatPercent(profitStatement?.profitMarginRate, 1)"
          caption="和目标阈值一起判断经营质量"
        />
        <MetricCard
          label="未收余额"
          :value="formatCurrency(profitStatement?.outstandingReceivableAmount)"
          caption="利润与回款必须联动判断"
        />
      </div>

      <div class="profit-panel__grid">
        <div class="profit-panel__card">
          <div class="profit-panel__section-head">
            <div>
              <p class="profit-panel__eyebrow">费用拆解</p>
              <h4>利润表细项</h4>
            </div>
            <StatusPill
              :label="profitStatement?.thresholdStatus || 'idle'"
              :tone="statusTone(profitStatement?.thresholdStatus)"
            />
          </div>
          <div v-if="profitStatement?.expenseBreakdown.length" class="profit-panel__expense-list">
            <article
              v-for="item in profitStatement.expenseBreakdown"
              :key="item.expenseType"
              class="profit-panel__expense-card"
            >
              <strong>{{ item.expenseType }}</strong>
              <span>{{ formatCurrency(item.expenseAmount) }}</span>
              <span>{{ item.allocationCount }} 条分摊记录</span>
            </article>
          </div>
          <p v-else class="profit-panel__empty">当前周期暂时没有费用拆解。</p>
        </div>

        <div class="profit-panel__card">
          <div class="profit-panel__section-head">
            <div>
              <p class="profit-panel__eyebrow">门店利润排行</p>
              <h4>{{ storeProfitReports.length }} 个门店</h4>
            </div>
          </div>
          <div v-if="storeProfitReports.length" class="profit-panel__list">
            <article
              v-for="report in storeProfitReports.slice(0, 6)"
              :key="report.storeId"
              class="profit-panel__list-card"
            >
              <div class="profit-panel__list-row">
                <strong>{{ report.shopName }}</strong>
                <StatusPill :label="report.thresholdStatus" :tone="statusTone(report.thresholdStatus)" />
              </div>
              <p>净利润 {{ formatCurrency(report.netProfitAmount) }}</p>
              <p>毛利 {{ formatCurrency(report.grossProfitAmount) }}</p>
              <p>利润率 {{ formatPercent(report.profitMarginRate, 1) }}</p>
            </article>
          </div>
          <p v-else class="profit-panel__empty">当前还没有门店利润报表。</p>
        </div>
      </div>

      <div class="profit-panel__grid">
        <div class="profit-panel__card">
          <div class="profit-panel__section-head">
            <div>
              <p class="profit-panel__eyebrow">总账摘要</p>
              <h4>{{ generalLedgers.length }} 条门店总账</h4>
            </div>
          </div>
          <div v-if="generalLedgers.length" class="profit-panel__list">
            <article
              v-for="ledger in generalLedgers.slice(0, 6)"
              :key="ledger.storeId"
              class="profit-panel__list-card"
            >
              <div class="profit-panel__list-row">
                <strong>{{ ledger.shopName }}</strong>
                <StatusPill :label="ledger.closingStatus" :tone="statusTone(ledger.closingStatus)" />
              </div>
              <p>结算 {{ ledger.settledFinanceBillCount }} / {{ ledger.financeBillCount }}</p>
              <p>回款 {{ formatCurrency(ledger.receivableCollectedAmount) }}</p>
              <p>待开票 {{ ledger.pendingInvoiceCount }} / 待凭证 {{ ledger.pendingVoucherCount }}</p>
            </article>
          </div>
          <p v-else class="profit-panel__empty">当前还没有总账摘要。</p>
        </div>

        <div class="profit-panel__card">
          <div class="profit-panel__section-head">
            <div>
              <p class="profit-panel__eyebrow">关账检查项</p>
              <h4>{{ financeClosingCheck?.shopName || selectedStoreName }}</h4>
            </div>
            <StatusPill
              :label="financeClosingCheck?.readyToClose ? 'ready' : 'blocked'"
              :tone="financeClosingCheck?.readyToClose ? 'success' : 'warn'"
            />
          </div>
          <div v-if="financeClosingCheck?.checkItems.length" class="profit-panel__list">
            <article
              v-for="item in financeClosingCheck.checkItems"
              :key="item.checkCode"
              class="profit-panel__list-card"
            >
              <div class="profit-panel__list-row">
                <strong>{{ item.checkName }}</strong>
                <StatusPill :label="item.checkStatus" :tone="statusTone(item.checkStatus)" />
              </div>
              <p>待处理 {{ item.pendingCount }}</p>
              <p>{{ item.detail }}</p>
            </article>
          </div>
          <p v-else class="profit-panel__empty">当前没有关账检查项。</p>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.profit-panel,
.profit-panel__hero,
.profit-panel__hero-copy,
.profit-panel__card,
.profit-panel__period-form {
  display: grid;
  gap: 0.9rem;
}

.profit-panel__hero {
  grid-template-columns: 1.1fr 0.9fr;
  align-items: stretch;
}

.profit-panel__hero-copy,
.profit-panel__card {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 251, 247, 0.78);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.profit-panel__hero-copy h4,
.profit-panel__hero-text,
.profit-panel__field span,
.profit-panel__empty,
.profit-panel__eyebrow,
.profit-panel__card h4,
.profit-panel__list-card p,
.profit-panel__expense-card span {
  margin: 0;
}

.profit-panel__eyebrow,
.profit-panel__field span {
  color: var(--color-ink-faint);
  font-size: 0.75rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.profit-panel__hero-text {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.profit-panel__period-form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-content: start;
}

.profit-panel__field {
  display: grid;
  gap: 0.45rem;
}

.profit-panel__control,
.profit-panel__primary {
  min-height: 2.8rem;
  border-radius: var(--radius-md);
}

.profit-panel__control {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.16);
  background: rgba(255, 255, 255, 0.92);
  padding: 0 0.85rem;
}

.profit-panel__primary {
  grid-column: span 2;
  border: 0;
  background: linear-gradient(135deg, #485f3f, #1d2818);
  color: #fffaf4;
  font-weight: 600;
  cursor: pointer;
}

.profit-panel__metrics,
.profit-panel__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.profit-panel__section-head,
.profit-panel__list-row {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 0.8rem;
}

.profit-panel__list,
.profit-panel__expense-list {
  display: grid;
  gap: 0.75rem;
}

.profit-panel__list-card,
.profit-panel__expense-card {
  display: grid;
  gap: 0.35rem;
  padding: 0.85rem;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.76);
}

@media (max-width: 1180px) {
  .profit-panel__hero,
  .profit-panel__metrics,
  .profit-panel__grid,
  .profit-panel__period-form {
    grid-template-columns: 1fr;
  }

  .profit-panel__primary {
    grid-column: span 1;
  }
}
</style>
