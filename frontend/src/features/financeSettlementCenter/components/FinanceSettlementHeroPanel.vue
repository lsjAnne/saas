<script setup lang="ts">
import { computed } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  FinanceBill,
  FinanceBillDetailView,
  FinanceClosingCheckView
} from '@/services/apiTypes';
import { formatCurrency, formatDate, formatDateTime } from '@/utils/formatters';

interface Summary {
  totalBillCount: number;
  pendingBillCount: number;
  outstandingReceivableAmount: number;
  collectedPaymentAmount: number;
  netProfitAmount: number;
  pendingInvoiceCount: number;
  pendingVoucherCount: number;
  readyToClose: boolean;
  closingBlockerCount: number;
}

interface Props {
  selectedStoreName: string;
  selectedBill: FinanceBill | null;
  selectedBillDetail: FinanceBillDetailView | null;
  financeClosingCheck: FinanceClosingCheckView | null;
  summary: Summary;
  actionFeedback?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionFeedback: null
});

const billTone = computed<'neutral' | 'warn' | 'success'>(() => {
  const status = (props.selectedBill?.billStatus ?? '').toLowerCase();
  if (status === 'settled' || status === 'closed') {
    return 'success';
  }

  if (status) {
    return 'warn';
  }

  return 'neutral';
});

const closingTone = computed<'neutral' | 'warn' | 'success'>(() => {
  if (!props.financeClosingCheck) {
    return 'neutral';
  }

  return props.financeClosingCheck.readyToClose ? 'success' : 'warn';
});

const labelMap: Record<string, string> = {
  idle: '待处理',
  draft: '草稿',
  pending: '待结算',
  reconciled: '已对账',
  settled: '已结算',
  closed: '已关账',
  ready: '可关账',
  blocked: '有阻塞'
};

function labelOf(value: string) {
  return labelMap[value] ?? value;
}
</script>

<template>
  <PanelCard
    title="财务工作台"
    description="账单结算、应收回款、利润洞察、发票凭证归档与期间关账统一在一条财务主链。"
  >
    <div class="finance-hero">
      <div class="finance-hero__copy">
        <p class="finance-hero__eyebrow">当前账务范围</p>
        <h4 class="finance-hero__title">{{ selectedStoreName }}</h4>
        <p class="finance-hero__summary">
          当前筛选范围下共有 {{ summary.totalBillCount }} 张账单，其中
          {{ summary.pendingBillCount }} 张仍待推进；应收余额
          {{ formatCurrency(summary.outstandingReceivableAmount) }}，累计回款
          {{ formatCurrency(summary.collectedPaymentAmount) }}。
        </p>
        <p v-if="actionFeedback" class="finance-hero__feedback">{{ actionFeedback }}</p>
      </div>

      <div class="finance-hero__focus">
        <div class="finance-hero__focus-header">
          <div>
            <p class="finance-hero__focus-eyebrow">当前焦点账单</p>
            <h5 class="finance-hero__focus-title">
              {{ selectedBill?.financeBillId || '等待选择账单' }}
            </h5>
          </div>
          <StatusPill :label="labelOf(selectedBill?.billStatus || 'idle')" :tone="billTone" />
        </div>
        <p class="finance-hero__focus-meta">
          周期 {{ formatDate(selectedBill?.periodStart) }} - {{ formatDate(selectedBill?.periodEnd) }}
        </p>
        <p class="finance-hero__focus-meta">
          收入 {{ formatCurrency(selectedBill?.incomeAmount) }} / 成本
          {{ formatCurrency(selectedBill?.costAmount) }} / 毛利
          {{ formatCurrency(selectedBill?.grossProfit) }}
        </p>
        <p class="finance-hero__focus-note">
          {{
            selectedBillDetail
              ? `拆分状态 ${selectedBillDetail.splitStatus}，发票校验 ${selectedBillDetail.invoiceCheckStatus}，已关联 ${selectedBillDetail.settlements.length} 条结算记录。`
              : '左下账单面板会同步展示账单详情、对账结论与结算流水。'
          }}
        </p>

        <div class="finance-hero__closing">
          <div>
            <p class="finance-hero__focus-eyebrow">关账检查</p>
            <p class="finance-hero__closing-text">
              {{ financeClosingCheck?.shopName || selectedStoreName }}
              {{ financeClosingCheck?.readyToClose ? '已具备关账条件' : '仍有阻塞项待清理' }}
            </p>
          </div>
          <StatusPill
            :label="labelOf(financeClosingCheck?.readyToClose ? 'ready' : 'blocked')"
            :tone="closingTone"
          />
        </div>
        <p class="finance-hero__focus-meta">
          阻塞项 {{ financeClosingCheck?.blockingIssueCount ?? summary.closingBlockerCount }}，
          最近检查窗口 {{ formatDate(financeClosingCheck?.periodStart) }} -
          {{ formatDate(financeClosingCheck?.periodEnd) }}
        </p>
      </div>
    </div>

    <div class="finance-hero__metrics">
      <MetricCard
        label="净利润"
        :value="formatCurrency(summary.netProfitAmount)"
        caption="按当前店铺和分析周期聚合"
      />
      <MetricCard
        label="待开发票"
        :value="`${summary.pendingInvoiceCount}`"
        caption="未归档且仍在主链中的发票"
      />
      <MetricCard
        label="待归档凭证"
        :value="`${summary.pendingVoucherCount}`"
        caption="凭证归档会直接影响总账和关账"
      />
      <MetricCard
        label="最后账单生成"
        :value="formatDateTime(selectedBill?.createdAt)"
        caption="以当前焦点账单为准"
      />
    </div>
  </PanelCard>
</template>

<style scoped>
.finance-hero,
.finance-hero__copy,
.finance-hero__focus,
.finance-hero__closing {
  display: grid;
  gap: 0.9rem;
}

.finance-hero {
  grid-template-columns: 1.08fr 0.92fr;
  align-items: stretch;
}

.finance-hero__copy,
.finance-hero__focus {
  padding: 1.05rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
}

.finance-hero__copy {
  background: var(--color-panel-strong);
}

.finance-hero__focus {
  background: var(--color-surface);
}

.finance-hero__eyebrow,
.finance-hero__title,
.finance-hero__summary,
.finance-hero__feedback,
.finance-hero__focus-eyebrow,
.finance-hero__focus-title,
.finance-hero__focus-meta,
.finance-hero__focus-note,
.finance-hero__closing-text {
  margin: 0;
}

.finance-hero__eyebrow,
.finance-hero__focus-eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.finance-hero__title,
.finance-hero__focus-title {
  color: var(--color-ink-strong);
}

.finance-hero__title {
  font-size: 1.5rem;
  font-weight: 600;
  line-height: 1.2;
}

.finance-hero__summary,
.finance-hero__feedback,
.finance-hero__focus-meta,
.finance-hero__focus-note,
.finance-hero__closing-text {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.finance-hero__feedback {
  color: var(--color-success);
}

.finance-hero__focus-header,
.finance-hero__closing {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.finance-hero__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
  margin-top: 1rem;
}

@media (max-width: 1080px) {
  .finance-hero,
  .finance-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
