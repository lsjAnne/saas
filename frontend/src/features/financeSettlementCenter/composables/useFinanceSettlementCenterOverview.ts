import { computed, reactive, shallowRef } from 'vue';

import {
  archiveFinanceInvoice,
  archiveFinanceVoucher,
  closeFinancePeriod,
  generateFinanceBill,
  getCustomerPayments,
  getFinanceBillDetail,
  getFinanceBills,
  getFinanceClosingCheck,
  getFinanceGeneralLedgers,
  getFinanceInvoices,
  getFinancePeriodClosings,
  getFinanceVouchers,
  getProfitStatement,
  getReceivableLedgers,
  getStoreProfitReports,
  issueFinanceInvoice,
  reconcileFinanceBill,
  recordCustomerPayment,
  redFlushFinanceInvoice,
  settleFinanceBill,
  voidFinanceInvoice
} from '@/services/financeSettlementCenterService';
import { getStores } from '@/services/storeChannelService';
import type {
  ArchiveFinanceVoucherPayload,
  CloseFinancePeriodPayload,
  CustomerPaymentRecord,
  FinanceBill,
  FinanceBillDetailView,
  FinanceBillReconcileView,
  FinanceClosingCheckView,
  FinanceGeneralLedgerView,
  FinanceInvoice,
  FinancePeriodClosingRecord,
  FinanceVoucher,
  GenerateFinanceBillPayload,
  IssueFinanceInvoicePayload,
  ProfitStatementView,
  ReceivableLedgerEntry,
  RecordCustomerPaymentPayload,
  SettleFinanceBillPayload,
  Store,
  StoreProfitReportView
} from '@/services/apiTypes';

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function pad(value: number) {
  return String(value).padStart(2, '0');
}

function formatDateInput(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function createCurrentPeriod() {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1);

  return {
    periodStart: formatDateInput(start),
    periodEnd: formatDateInput(now)
  };
}

function matchesStore(activeStoreId: string, storeId: string) {
  return activeStoreId === 'all' ? true : activeStoreId === storeId;
}

function isPendingBill(bill: FinanceBill) {
  const status = normalizeText(bill.billStatus);
  return status !== 'settled' && status !== 'closed';
}

function isPendingInvoice(invoice: FinanceInvoice) {
  return (
    normalizeText(invoice.archiveStatus) !== 'archived' &&
    !['void', 'red_flush', 'red-flush'].includes(normalizeText(invoice.invoiceStatus))
  );
}

function isPendingVoucher(voucher: FinanceVoucher) {
  return normalizeText(voucher.archiveStatus) !== 'archived';
}

export function useFinanceSettlementCenterOverview() {
  const stores = shallowRef<Store[]>([]);
  const financeBills = shallowRef<FinanceBill[]>([]);
  const receivableLedgers = shallowRef<ReceivableLedgerEntry[]>([]);
  const customerPayments = shallowRef<CustomerPaymentRecord[]>([]);
  const financeVouchers = shallowRef<FinanceVoucher[]>([]);
  const financeInvoices = shallowRef<FinanceInvoice[]>([]);
  const financePeriodClosings = shallowRef<FinancePeriodClosingRecord[]>([]);
  const profitStatement = shallowRef<ProfitStatementView | null>(null);
  const storeProfitReports = shallowRef<StoreProfitReportView[]>([]);
  const financeGeneralLedgers = shallowRef<FinanceGeneralLedgerView[]>([]);
  const financeClosingCheck = shallowRef<FinanceClosingCheckView | null>(null);
  const selectedBillDetail = shallowRef<FinanceBillDetailView | null>(null);
  const lastBillReconcileView = shallowRef<FinanceBillReconcileView | null>(null);
  const activeStoreId = shallowRef('all');
  const selectedBillId = shallowRef<string | null>(null);
  const selectedLedgerId = shallowRef<string | null>(null);
  const selectedInvoiceId = shallowRef<string | null>(null);
  const selectedVoucherId = shallowRef<string | null>(null);
  const analysisPeriod = reactive(createCurrentPeriod());
  const isLoading = shallowRef(false);
  const isRefreshingAnalytics = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const sortedStores = computed(() =>
    [...stores.value].sort(
      (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const sortedBills = computed(() =>
    [...financeBills.value].sort(
      (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const sortedLedgers = computed(() =>
    [...receivableLedgers.value].sort((left, right) => {
      const balanceGap = right.outstandingAmount - left.outstandingAmount;
      if (balanceGap !== 0) {
        return balanceGap;
      }

      return toTimestamp(right.occurredAt) - toTimestamp(left.occurredAt);
    })
  );

  const sortedPayments = computed(() =>
    [...customerPayments.value].sort(
      (left, right) => toTimestamp(right.receivedAt) - toTimestamp(left.receivedAt)
    )
  );

  const sortedInvoices = computed(() =>
    [...financeInvoices.value].sort(
      (left, right) => toTimestamp(right.updatedAt) - toTimestamp(left.updatedAt)
    )
  );

  const sortedVouchers = computed(() =>
    [...financeVouchers.value].sort(
      (left, right) => toTimestamp(right.archivedAt) - toTimestamp(left.archivedAt)
    )
  );

  const sortedClosings = computed(() =>
    [...financePeriodClosings.value].sort(
      (left, right) => toTimestamp(right.closedAt) - toTimestamp(left.closedAt)
    )
  );

  const filteredBills = computed(() =>
    sortedBills.value.filter((item) => matchesStore(activeStoreId.value, item.storeId))
  );

  const filteredLedgers = computed(() =>
    sortedLedgers.value.filter((item) => matchesStore(activeStoreId.value, item.storeId))
  );

  const filteredPayments = computed(() =>
    sortedPayments.value.filter((item) => matchesStore(activeStoreId.value, item.storeId))
  );

  const filteredInvoices = computed(() =>
    sortedInvoices.value.filter((item) => matchesStore(activeStoreId.value, item.storeId))
  );

  const filteredVouchers = computed(() =>
    sortedVouchers.value.filter((item) => matchesStore(activeStoreId.value, item.storeId))
  );

  const filteredClosings = computed(() =>
    sortedClosings.value.filter((item) => matchesStore(activeStoreId.value, item.storeId))
  );

  const filteredStoreProfitReports = computed(() =>
    storeProfitReports.value.filter((item) => matchesStore(activeStoreId.value, item.storeId))
  );

  const filteredFinanceGeneralLedgers = computed(() =>
    financeGeneralLedgers.value.filter((item) => matchesStore(activeStoreId.value, item.storeId))
  );

  const selectedBill = computed(
    () =>
      filteredBills.value.find((item) => item.financeBillId === selectedBillId.value) ??
      sortedBills.value.find((item) => item.financeBillId === selectedBillId.value) ??
      null
  );

  const selectedLedger = computed(
    () =>
      filteredLedgers.value.find((item) => item.receivableLedgerId === selectedLedgerId.value) ??
      sortedLedgers.value.find((item) => item.receivableLedgerId === selectedLedgerId.value) ??
      null
  );

  const selectedInvoice = computed(
    () =>
      filteredInvoices.value.find((item) => item.invoiceId === selectedInvoiceId.value) ??
      sortedInvoices.value.find((item) => item.invoiceId === selectedInvoiceId.value) ??
      null
  );

  const selectedVoucher = computed(
    () =>
      filteredVouchers.value.find((item) => item.voucherId === selectedVoucherId.value) ??
      sortedVouchers.value.find((item) => item.voucherId === selectedVoucherId.value) ??
      null
  );

  const currentStoreFocusId = computed(() => {
    if (activeStoreId.value !== 'all') {
      return activeStoreId.value;
    }

    return (
      selectedBill.value?.storeId ??
      selectedLedger.value?.storeId ??
      selectedInvoice.value?.storeId ??
      selectedVoucher.value?.storeId ??
      filteredBills.value[0]?.storeId ??
      filteredLedgers.value[0]?.storeId ??
      filteredInvoices.value[0]?.storeId ??
      filteredStoreProfitReports.value[0]?.storeId ??
      sortedStores.value[0]?.storeId ??
      null
    );
  });

  const selectedStoreName = computed(() => {
    if (activeStoreId.value === 'all') {
      return currentStoreFocusId.value
        ? sortedStores.value.find((store) => store.storeId === currentStoreFocusId.value)?.shopName ??
            '全部店铺'
        : '全部店铺';
    }

    return (
      sortedStores.value.find((store) => store.storeId === activeStoreId.value)?.shopName ??
      activeStoreId.value
    );
  });

  const summary = computed(() => ({
    totalBillCount: filteredBills.value.length,
    pendingBillCount: filteredBills.value.filter(isPendingBill).length,
    outstandingReceivableAmount: filteredLedgers.value.reduce(
      (sum, item) => sum + item.outstandingAmount,
      0
    ),
    collectedPaymentAmount: filteredPayments.value.reduce(
      (sum, item) => sum + item.paymentAmount,
      0
    ),
    netProfitAmount:
      activeStoreId.value === 'all'
        ? filteredStoreProfitReports.value.reduce((sum, item) => sum + item.netProfitAmount, 0)
        : (profitStatement.value?.netProfitAmount ?? filteredStoreProfitReports.value[0]?.netProfitAmount ?? 0),
    pendingInvoiceCount: filteredInvoices.value.filter(isPendingInvoice).length,
    pendingVoucherCount: filteredVouchers.value.filter(isPendingVoucher).length,
    readyToClose: financeClosingCheck.value?.readyToClose ?? false,
    closingBlockerCount: financeClosingCheck.value?.blockingIssueCount ?? 0
  }));

  function applySelections() {
    selectedBillId.value =
      filteredBills.value.find((item) => item.financeBillId === selectedBillId.value)?.financeBillId ??
      filteredBills.value[0]?.financeBillId ??
      null;

    selectedLedgerId.value =
      filteredLedgers.value.find((item) => item.receivableLedgerId === selectedLedgerId.value)
        ?.receivableLedgerId ??
      filteredLedgers.value[0]?.receivableLedgerId ??
      null;

    selectedInvoiceId.value =
      filteredInvoices.value.find((item) => item.invoiceId === selectedInvoiceId.value)?.invoiceId ??
      filteredInvoices.value[0]?.invoiceId ??
      null;

    selectedVoucherId.value =
      filteredVouchers.value.find((item) => item.voucherId === selectedVoucherId.value)?.voucherId ??
      filteredVouchers.value[0]?.voucherId ??
      null;
  }

  async function refreshCollections(token: string) {
    const [
      billsPayload,
      ledgersPayload,
      paymentsPayload,
      vouchersPayload,
      invoicesPayload,
      closingsPayload
    ] = await Promise.all([
      getFinanceBills(token),
      getReceivableLedgers(token),
      getCustomerPayments(token),
      getFinanceVouchers(token),
      getFinanceInvoices(token),
      getFinancePeriodClosings(token)
    ]);

    financeBills.value = billsPayload;
    receivableLedgers.value = ledgersPayload;
    customerPayments.value = paymentsPayload;
    financeVouchers.value = vouchersPayload;
    financeInvoices.value = invoicesPayload;
    financePeriodClosings.value = closingsPayload;
    applySelections();
  }

  async function refreshSelectedBillDetail(token: string) {
    if (!selectedBillId.value) {
      selectedBillDetail.value = null;
      return;
    }

    selectedBillDetail.value = await getFinanceBillDetail(selectedBillId.value, token);
  }

  async function refreshAnalytics(token: string) {
    isRefreshingAnalytics.value = true;

    try {
      const focusStoreId = currentStoreFocusId.value;
      const [profitReportsPayload, generalLedgersPayload, profitPayload, closingPayload] =
        await Promise.all([
          getStoreProfitReports(analysisPeriod.periodStart, analysisPeriod.periodEnd, token),
          getFinanceGeneralLedgers(analysisPeriod.periodStart, analysisPeriod.periodEnd, token),
          focusStoreId
            ? getProfitStatement(
                focusStoreId,
                analysisPeriod.periodStart,
                analysisPeriod.periodEnd,
                token
              )
            : Promise.resolve<ProfitStatementView | null>(null),
          focusStoreId
            ? getFinanceClosingCheck(
                focusStoreId,
                analysisPeriod.periodStart,
                analysisPeriod.periodEnd,
                token
              )
            : Promise.resolve<FinanceClosingCheckView | null>(null)
        ]);

      storeProfitReports.value = profitReportsPayload;
      financeGeneralLedgers.value = generalLedgersPayload;
      profitStatement.value = profitPayload;
      financeClosingCheck.value = closingPayload;
    } finally {
      isRefreshingAnalytics.value = false;
    }
  }

  async function refreshAfterMutation(token: string, reloadBillDetail = true) {
    await refreshCollections(token);
    await refreshAnalytics(token);

    if (reloadBillDetail) {
      await refreshSelectedBillDetail(token);
    }
  }

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const storesPayload = await getStores(token);
      stores.value = storesPayload;

      if (
        activeStoreId.value !== 'all' &&
        !storesPayload.some((store) => store.storeId === activeStoreId.value)
      ) {
        activeStoreId.value = storesPayload[0]?.storeId ?? 'all';
      }

      if (activeStoreId.value === 'all' && storesPayload.length === 1) {
        activeStoreId.value = storesPayload[0].storeId;
      }

      await refreshCollections(token);
      await refreshAnalytics(token);
      await refreshSelectedBillDetail(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '财务结算中心加载失败，请稍后重试。';
    } finally {
      isLoading.value = false;
    }
  }

  async function selectStore(storeId: string, token: string) {
    activeStoreId.value = storeId;
    lastBillReconcileView.value = null;
    applySelections();
    await refreshAnalytics(token);
    await refreshSelectedBillDetail(token);
  }

  async function selectBill(financeBillId: string, token: string) {
    selectedBillId.value = financeBillId;
    lastBillReconcileView.value = null;
    await refreshSelectedBillDetail(token);
    await refreshAnalytics(token);
  }

  function selectLedger(receivableLedgerId: string) {
    selectedLedgerId.value = receivableLedgerId;
  }

  function selectInvoice(invoiceId: string) {
    selectedInvoiceId.value = invoiceId;
  }

  function selectVoucher(voucherId: string) {
    selectedVoucherId.value = voucherId;
  }

  async function updateAnalysisPeriod(periodStart: string, periodEnd: string, token: string) {
    analysisPeriod.periodStart = periodStart;
    analysisPeriod.periodEnd = periodEnd;
    await refreshAnalytics(token);
  }

  async function runAction(callback: () => Promise<void>) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      await callback();
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '财务动作执行失败，请稍后重试。';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitGenerateFinanceBill(
    payload: GenerateFinanceBillPayload,
    token: string
  ) {
    await runAction(async () => {
      const created = await generateFinanceBill(payload, token);
      selectedBillId.value = created.financeBillId;
      actionFeedback.value = `已生成账单 ${created.financeBillId}。`;
      await refreshAfterMutation(token);
    });
  }

  async function submitReconcileFinanceBill(token: string) {
    if (!selectedBillId.value) {
      return;
    }

    await runAction(async () => {
      const reconcileView = await reconcileFinanceBill(selectedBillId.value!, token);
      lastBillReconcileView.value = reconcileView;
      actionFeedback.value = `账单 ${reconcileView.financeBillId} 已完成对账，发现 ${reconcileView.discrepancyCount} 个差异。`;
      await refreshAfterMutation(token);
    });
  }

  async function submitSettleFinanceBill(
    payload: SettleFinanceBillPayload,
    token: string
  ) {
    if (!selectedBillId.value) {
      return;
    }

    await runAction(async () => {
      const settlement = await settleFinanceBill(selectedBillId.value!, payload, token);
      actionFeedback.value = `已登记账单结算 ${settlement.settlementRecordId}。`;
      await refreshAfterMutation(token);
    });
  }

  async function submitRecordCustomerPayment(
    payload: RecordCustomerPaymentPayload,
    token: string
  ) {
    await runAction(async () => {
      const created = await recordCustomerPayment(payload, token);
      actionFeedback.value = `已登记客户回款 ${created.paymentNo}。`;
      await refreshAfterMutation(token, false);
    });
  }

  async function submitIssueFinanceInvoice(
    payload: IssueFinanceInvoicePayload,
    token: string
  ) {
    await runAction(async () => {
      const created = await issueFinanceInvoice(payload, token);
      selectedInvoiceId.value = created.invoiceId;
      actionFeedback.value = `已开具发票 ${created.invoiceNo}。`;
      await refreshAfterMutation(token);
    });
  }

  async function submitArchiveFinanceInvoice(remark: string | undefined, token: string) {
    if (!selectedInvoiceId.value) {
      return;
    }

    await runAction(async () => {
      const updated = await archiveFinanceInvoice(
        selectedInvoiceId.value!,
        { remark },
        token
      );
      selectedInvoiceId.value = updated.invoiceId;
      actionFeedback.value = `已归档发票 ${updated.invoiceNo}。`;
      await refreshAfterMutation(token, false);
    });
  }

  async function submitVoidFinanceInvoice(remark: string | undefined, token: string) {
    if (!selectedInvoiceId.value) {
      return;
    }

    await runAction(async () => {
      const updated = await voidFinanceInvoice(selectedInvoiceId.value!, { remark }, token);
      selectedInvoiceId.value = updated.invoiceId;
      actionFeedback.value = `已作废发票 ${updated.invoiceNo}。`;
      await refreshAfterMutation(token, false);
    });
  }

  async function submitRedFlushFinanceInvoice(remark: string | undefined, token: string) {
    if (!selectedInvoiceId.value) {
      return;
    }

    await runAction(async () => {
      const updated = await redFlushFinanceInvoice(
        selectedInvoiceId.value!,
        { remark },
        token
      );
      selectedInvoiceId.value = updated.invoiceId;
      actionFeedback.value = `已红冲发票 ${updated.invoiceNo}。`;
      await refreshAfterMutation(token, false);
    });
  }

  async function submitArchiveFinanceVoucher(
    payload: ArchiveFinanceVoucherPayload,
    token: string
  ) {
    await runAction(async () => {
      const created = await archiveFinanceVoucher(payload, token);
      selectedVoucherId.value = created.voucherId;
      actionFeedback.value = `已归档凭证 ${created.voucherNo}。`;
      await refreshAfterMutation(token, false);
    });
  }

  async function submitCloseFinancePeriod(
    payload: CloseFinancePeriodPayload,
    token: string
  ) {
    await runAction(async () => {
      const created = await closeFinancePeriod(payload, token);
      actionFeedback.value = `已提交期间关账 ${created.closingNo}。`;
      await refreshAfterMutation(token, false);
    });
  }

  return {
    stores: sortedStores,
    filteredBills,
    filteredLedgers,
    filteredPayments,
    filteredInvoices,
    filteredVouchers,
    filteredClosings,
    filteredStoreProfitReports,
    filteredFinanceGeneralLedgers,
    profitStatement,
    financeClosingCheck,
    selectedBillId,
    selectedLedgerId,
    selectedInvoiceId,
    selectedVoucherId,
    selectedBill,
    selectedBillDetail,
    selectedLedger,
    selectedInvoice,
    selectedVoucher,
    selectedStoreName,
    currentStoreFocusId,
    lastBillReconcileView,
    analysisPeriod,
    summary,
    activeStoreId,
    isLoading,
    isRefreshingAnalytics,
    isRunningAction,
    errorMessage,
    actionError,
    actionFeedback,
    load,
    selectStore,
    selectBill,
    selectLedger,
    selectInvoice,
    selectVoucher,
    updateAnalysisPeriod,
    submitGenerateFinanceBill,
    submitReconcileFinanceBill,
    submitSettleFinanceBill,
    submitRecordCustomerPayment,
    submitIssueFinanceInvoice,
    submitArchiveFinanceInvoice,
    submitVoidFinanceInvoice,
    submitRedFlushFinanceInvoice,
    submitArchiveFinanceVoucher,
    submitCloseFinancePeriod
  };
}
