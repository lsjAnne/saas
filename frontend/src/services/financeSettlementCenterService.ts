import { apiClient } from '@/services/http/apiClient';
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
  SettlementRecord,
  SettleFinanceBillPayload,
  StoreProfitReportView,
  UpdateFinanceInvoicePayload
} from '@/services/apiTypes';

function buildPeriodQuery(periodStart: string, periodEnd: string) {
  const params = new URLSearchParams({
    periodStart,
    periodEnd
  });

  return params.toString();
}

export function getFinanceBills(token: string) {
  return apiClient.get<FinanceBill[]>('/api/finance-bills', token);
}

export function getFinanceBillDetail(financeBillId: string, token: string) {
  return apiClient.get<FinanceBillDetailView>(`/api/finance-bills/${financeBillId}`, token);
}

export function generateFinanceBill(payload: GenerateFinanceBillPayload, token: string) {
  return apiClient.post<FinanceBill>('/api/finance-bills/generate', payload, token);
}

export function reconcileFinanceBill(financeBillId: string, token: string) {
  return apiClient.post<FinanceBillReconcileView>(
    `/api/finance-bills/${financeBillId}/reconcile`,
    null,
    token
  );
}

export function settleFinanceBill(
  financeBillId: string,
  payload: SettleFinanceBillPayload,
  token: string
) {
  return apiClient.post<SettlementRecord>(
    `/api/finance-bills/${financeBillId}/settle`,
    payload,
    token
  );
}

export function getReceivableLedgers(token: string) {
  return apiClient.get<ReceivableLedgerEntry[]>('/api/receivable-ledgers', token);
}

export function getCustomerPayments(token: string) {
  return apiClient.get<CustomerPaymentRecord[]>('/api/customer-payments', token);
}

export function recordCustomerPayment(
  payload: RecordCustomerPaymentPayload,
  token: string
) {
  return apiClient.post<CustomerPaymentRecord>('/api/customer-payments', payload, token);
}

export function getProfitStatement(
  storeId: string,
  periodStart: string,
  periodEnd: string,
  token: string
) {
  const params = new URLSearchParams({
    storeId,
    periodStart,
    periodEnd
  });

  return apiClient.get<ProfitStatementView>(`/api/profit-statements?${params.toString()}`, token);
}

export function getStoreProfitReports(
  periodStart: string,
  periodEnd: string,
  token: string
) {
  return apiClient.get<StoreProfitReportView[]>(
    `/api/store-profit-reports?${buildPeriodQuery(periodStart, periodEnd)}`,
    token
  );
}

export function getFinanceGeneralLedgers(
  periodStart: string,
  periodEnd: string,
  token: string
) {
  return apiClient.get<FinanceGeneralLedgerView[]>(
    `/api/finance-general-ledgers?${buildPeriodQuery(periodStart, periodEnd)}`,
    token
  );
}

export function getFinanceClosingCheck(
  storeId: string,
  periodStart: string,
  periodEnd: string,
  token: string
) {
  const params = new URLSearchParams({
    storeId,
    periodStart,
    periodEnd
  });

  return apiClient.get<FinanceClosingCheckView>(
    `/api/finance-closing-checks?${params.toString()}`,
    token
  );
}

export function getFinanceVouchers(token: string) {
  return apiClient.get<FinanceVoucher[]>('/api/finance-vouchers', token);
}

export function archiveFinanceVoucher(
  payload: ArchiveFinanceVoucherPayload,
  token: string
) {
  return apiClient.post<FinanceVoucher>('/api/finance-vouchers/archive', payload, token);
}

export function getFinanceInvoices(token: string) {
  return apiClient.get<FinanceInvoice[]>('/api/finance-invoices', token);
}

export function issueFinanceInvoice(
  payload: IssueFinanceInvoicePayload,
  token: string
) {
  return apiClient.post<FinanceInvoice>('/api/finance-invoices/issue', payload, token);
}

export function archiveFinanceInvoice(
  invoiceId: string,
  payload: UpdateFinanceInvoicePayload,
  token: string
) {
  return apiClient.post<FinanceInvoice>(
    `/api/finance-invoices/${invoiceId}/archive`,
    payload,
    token
  );
}

export function voidFinanceInvoice(
  invoiceId: string,
  payload: UpdateFinanceInvoicePayload,
  token: string
) {
  return apiClient.post<FinanceInvoice>(
    `/api/finance-invoices/${invoiceId}/void`,
    payload,
    token
  );
}

export function redFlushFinanceInvoice(
  invoiceId: string,
  payload: UpdateFinanceInvoicePayload,
  token: string
) {
  return apiClient.post<FinanceInvoice>(
    `/api/finance-invoices/${invoiceId}/red-flush`,
    payload,
    token
  );
}

export function getFinancePeriodClosings(token: string) {
  return apiClient.get<FinancePeriodClosingRecord[]>('/api/finance-period-closings', token);
}

export function closeFinancePeriod(
  payload: CloseFinancePeriodPayload,
  token: string
) {
  return apiClient.post<FinancePeriodClosingRecord>(
    '/api/finance-period-closings/close',
    payload,
    token
  );
}
