<script setup lang="ts">
import { onMounted, reactive } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import FinanceBillBoardPanel from '@/features/financeSettlementCenter/components/FinanceBillBoardPanel.vue';
import FinanceSettlementHeroPanel from '@/features/financeSettlementCenter/components/FinanceSettlementHeroPanel.vue';
import ProfitInsightPanel from '@/features/financeSettlementCenter/components/ProfitInsightPanel.vue';
import ReceivablePaymentPanel from '@/features/financeSettlementCenter/components/ReceivablePaymentPanel.vue';
import VoucherInvoicePanel from '@/features/financeSettlementCenter/components/VoucherInvoicePanel.vue';
import { useFinanceSettlementCenterOverview } from '@/features/financeSettlementCenter/composables/useFinanceSettlementCenterOverview';
import type {
  ArchiveFinanceVoucherPayload,
  CloseFinancePeriodPayload,
  GenerateFinanceBillPayload,
  IssueFinanceInvoicePayload,
  RecordCustomerPaymentPayload,
  SettleFinanceBillPayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const financeCenter = reactive(useFinanceSettlementCenterOverview());

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await financeCenter.load(authStore.token);
}

async function withToken<T>(callback: (token: string) => Promise<T>) {
  if (!authStore.token) {
    return;
  }

  await callback(authStore.token);
}

async function handleSelectStore(storeId: string) {
  await withToken((token) => financeCenter.selectStore(storeId, token));
}

async function handleSelectBill(financeBillId: string) {
  await withToken((token) => financeCenter.selectBill(financeBillId, token));
}

async function handleUpdatePeriod(payload: { periodStart: string; periodEnd: string }) {
  await withToken((token) =>
    financeCenter.updateAnalysisPeriod(payload.periodStart, payload.periodEnd, token)
  );
}

async function handleGenerateBill(payload: GenerateFinanceBillPayload) {
  await withToken((token) => financeCenter.submitGenerateFinanceBill(payload, token));
}

async function handleReconcileBill() {
  await withToken((token) => financeCenter.submitReconcileFinanceBill(token));
}

async function handleSettleBill(payload: SettleFinanceBillPayload) {
  await withToken((token) => financeCenter.submitSettleFinanceBill(payload, token));
}

async function handleRecordPayment(payload: RecordCustomerPaymentPayload) {
  await withToken((token) => financeCenter.submitRecordCustomerPayment(payload, token));
}

async function handleIssueInvoice(payload: IssueFinanceInvoicePayload) {
  await withToken((token) => financeCenter.submitIssueFinanceInvoice(payload, token));
}

async function handleArchiveInvoice(remark?: string) {
  await withToken((token) => financeCenter.submitArchiveFinanceInvoice(remark, token));
}

async function handleVoidInvoice(remark?: string) {
  await withToken((token) => financeCenter.submitVoidFinanceInvoice(remark, token));
}

async function handleRedFlushInvoice(remark?: string) {
  await withToken((token) => financeCenter.submitRedFlushFinanceInvoice(remark, token));
}

async function handleArchiveVoucher(payload: ArchiveFinanceVoucherPayload) {
  await withToken((token) => financeCenter.submitArchiveFinanceVoucher(payload, token));
}

async function handleClosePeriod(payload: CloseFinancePeriodPayload) {
  await withToken((token) => financeCenter.submitCloseFinancePeriod(payload, token));
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="finance-center-page">
    <header class="finance-center-page__header">
      <p class="finance-center-page__eyebrow">Finance & Settlement</p>
      <h2 class="finance-center-page__title">
        把账单生成、应收回款、利润洞察、发票凭证和期间关账收成一张真正可执行的财务首页
      </h2>
      <p class="finance-center-page__subtitle">
        这页不再停留在财务规划骨架，而是直接围绕账单、回款、利润、票据和关账五条真实主链搭起一套可看、可追、可操作的结算控制台。
      </p>
    </header>

    <InlineErrorCard
      v-if="financeCenter.errorMessage"
      :message="financeCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <FinanceSettlementHeroPanel
      :selected-store-name="financeCenter.selectedStoreName"
      :selected-bill="financeCenter.selectedBill"
      :selected-bill-detail="financeCenter.selectedBillDetail"
      :finance-closing-check="financeCenter.financeClosingCheck"
      :summary="financeCenter.summary"
      :action-feedback="financeCenter.actionFeedback"
    />

    <div class="finance-center-page__grid">
      <FinanceBillBoardPanel
        :stores="financeCenter.stores"
        :active-store-id="financeCenter.activeStoreId"
        :bills="financeCenter.filteredBills"
        :selected-bill-id="financeCenter.selectedBillId"
        :selected-bill="financeCenter.selectedBill"
        :selected-bill-detail="financeCenter.selectedBillDetail"
        :last-reconcile-view="financeCenter.lastBillReconcileView"
        :analysis-period-start="financeCenter.analysisPeriod.periodStart"
        :analysis-period-end="financeCenter.analysisPeriod.periodEnd"
        :is-submitting="financeCenter.isRunningAction"
        :action-error="financeCenter.actionError"
        @select-store="handleSelectStore"
        @select-bill="handleSelectBill"
        @generate-bill="handleGenerateBill"
        @reconcile-bill="handleReconcileBill"
        @settle-bill="handleSettleBill"
      />

      <div class="finance-center-page__stack">
        <ReceivablePaymentPanel
          :receivable-ledgers="financeCenter.filteredLedgers"
          :selected-ledger-id="financeCenter.selectedLedgerId"
          :selected-ledger="financeCenter.selectedLedger"
          :customer-payments="financeCenter.filteredPayments"
          :is-submitting="financeCenter.isRunningAction"
          :action-error="financeCenter.actionError"
          @select-ledger="financeCenter.selectLedger"
          @record-payment="handleRecordPayment"
        />

        <ProfitInsightPanel
          :analysis-period-start="financeCenter.analysisPeriod.periodStart"
          :analysis-period-end="financeCenter.analysisPeriod.periodEnd"
          :selected-store-name="financeCenter.selectedStoreName"
          :profit-statement="financeCenter.profitStatement"
          :store-profit-reports="financeCenter.filteredStoreProfitReports"
          :general-ledgers="financeCenter.filteredFinanceGeneralLedgers"
          :finance-closing-check="financeCenter.financeClosingCheck"
          :is-refreshing="financeCenter.isRefreshingAnalytics"
          @update-period="handleUpdatePeriod"
        />

        <VoucherInvoicePanel
          :stores="financeCenter.stores"
          :active-store-id="financeCenter.activeStoreId"
          :selected-store-name="financeCenter.selectedStoreName"
          :selected-bill="financeCenter.selectedBill"
          :invoices="financeCenter.filteredInvoices"
          :selected-invoice-id="financeCenter.selectedInvoiceId"
          :selected-invoice="financeCenter.selectedInvoice"
          :vouchers="financeCenter.filteredVouchers"
          :selected-voucher-id="financeCenter.selectedVoucherId"
          :selected-voucher="financeCenter.selectedVoucher"
          :closings="financeCenter.filteredClosings"
          :analysis-period-start="financeCenter.analysisPeriod.periodStart"
          :analysis-period-end="financeCenter.analysisPeriod.periodEnd"
          :is-submitting="financeCenter.isRunningAction"
          :action-error="financeCenter.actionError"
          @select-invoice="financeCenter.selectInvoice"
          @select-voucher="financeCenter.selectVoucher"
          @issue-invoice="handleIssueInvoice"
          @archive-invoice="handleArchiveInvoice"
          @void-invoice="handleVoidInvoice"
          @red-flush-invoice="handleRedFlushInvoice"
          @archive-voucher="handleArchiveVoucher"
          @close-period="handleClosePeriod"
        />
      </div>
    </div>

    <p
      v-if="financeCenter.isLoading || financeCenter.isRefreshingAnalytics"
      class="finance-center-page__footer-note"
    >
      正在回读账单、应收回款、利润报表、总账摘要和关账检查...
    </p>
  </section>
</template>

<style scoped>
.finance-center-page {
  display: grid;
  gap: 1.2rem;
}

.finance-center-page__header {
  display: grid;
  gap: 0.75rem;
}

.finance-center-page__eyebrow,
.finance-center-page__title,
.finance-center-page__subtitle,
.finance-center-page__footer-note {
  margin: 0;
}

.finance-center-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.finance-center-page__title {
  max-width: 14ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 3.8rem);
  line-height: 1.04;
}

.finance-center-page__subtitle,
.finance-center-page__footer-note {
  max-width: 68rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.finance-center-page__grid {
  display: grid;
  grid-template-columns: 0.94fr 1.06fr;
  gap: 1rem;
  align-items: start;
}

.finance-center-page__stack {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1180px) {
  .finance-center-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
