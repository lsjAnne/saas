package com.dianshang.platform.finance.application;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.finance.domain.repository.FinanceBillRepository;
import com.dianshang.platform.finance.domain.repository.SettlementRecordRepository;
import com.dianshang.platform.finance.model.FinanceBill;
import com.dianshang.platform.inventory.application.InventoryService;
import com.dianshang.platform.finance.model.SettlementRecord;
import com.dianshang.platform.order.domain.repository.OrderRepository;
import com.dianshang.platform.order.model.OrderMain;
import com.dianshang.platform.store.domain.repository.StoreRepository;
import com.dianshang.platform.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FinanceService {
    private static final Map<String, CustomerPaymentRecord> CUSTOMER_PAYMENT_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong CUSTOMER_PAYMENT_SEQUENCE = new AtomicLong(11000);
    private static final Map<String, FinanceVoucher> FINANCE_VOUCHER_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong FINANCE_VOUCHER_SEQUENCE = new AtomicLong(12000);
    private static final Map<String, FinancePeriodClosingRecord> FINANCE_PERIOD_CLOSING_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong FINANCE_PERIOD_CLOSING_SEQUENCE = new AtomicLong(13000);
    private static final Map<String, FinanceInvoice> FINANCE_INVOICE_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong FINANCE_INVOICE_SEQUENCE = new AtomicLong(14000);
    private static final Map<String, ErpMasterDataDictionaryEntry> ERP_MASTER_DATA_DICTIONARY_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong ERP_MASTER_DATA_DICTIONARY_SEQUENCE = new AtomicLong(15000);
    private static final Map<String, ErpAccountMappingEntry> ERP_ACCOUNT_MAPPING_STORAGE = new ConcurrentHashMap<>();
    private static final AtomicLong ERP_ACCOUNT_MAPPING_SEQUENCE = new AtomicLong(16000);

    private final AuditLogService auditLogService;
    private final InventoryService inventoryService;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final FinanceBillRepository financeBillRepository;
    private final SettlementRecordRepository settlementRecordRepository;

    public FinanceService(AuditLogService auditLogService,
                          InventoryService inventoryService,
                          StoreRepository storeRepository,
                          OrderRepository orderRepository,
                          FinanceBillRepository financeBillRepository,
                          SettlementRecordRepository settlementRecordRepository) {
        this.auditLogService = auditLogService;
        this.inventoryService = inventoryService;
        this.storeRepository = storeRepository;
        this.orderRepository = orderRepository;
        this.financeBillRepository = financeBillRepository;
        this.settlementRecordRepository = settlementRecordRepository;
    }

    public List<FinanceBill> listFinanceBills(String tenantId) {
        return financeBillRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public List<ReceivableLedgerEntry> listReceivableLedgers(String tenantId) {
        return orderRepository.findByStoreIds(ownedStoreIds(tenantId)).stream()
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .map(this::toReceivableLedgerEntry)
                .toList();
    }

    public List<CustomerPaymentRecord> listCustomerPayments(String tenantId) {
        return CUSTOMER_PAYMENT_STORAGE.values().stream()
                .filter(record -> ownedStoreIds(tenantId).contains(record.storeId()))
                .sorted((left, right) -> right.receivedAt().compareTo(left.receivedAt()))
                .toList();
    }

    public List<ErpMasterDataDictionaryEntry> listErpMasterDataDictionaries(String tenantId, String dictionaryType) {
        String normalizedType = normalizeFilter(dictionaryType);
        return ERP_MASTER_DATA_DICTIONARY_STORAGE.values().stream()
                .filter(entry -> tenantId.equals(entry.tenantId()))
                .filter(entry -> normalizedType == null || normalizedType.equals(entry.dictionaryType()))
                .sorted(Comparator.comparing(ErpMasterDataDictionaryEntry::dictionaryType)
                        .thenComparing(ErpMasterDataDictionaryEntry::dictionaryCode))
                .toList();
    }

    public ErpMasterDataDictionaryEntry saveErpMasterDataDictionary(String tenantId,
                                                                    String operatorId,
                                                                    SaveErpMasterDataDictionaryRequest request) {
        String dictionaryType = normalizeRequired(request.dictionaryType(), "dictionaryType");
        String dictionaryCode = normalizeRequired(request.dictionaryCode(), "dictionaryCode");
        String key = tenantId + "|" + dictionaryType + "|" + dictionaryCode;
        OffsetDateTime now = OffsetDateTime.now();
        ErpMasterDataDictionaryEntry existing = ERP_MASTER_DATA_DICTIONARY_STORAGE.get(key);
        ErpMasterDataDictionaryEntry saved = new ErpMasterDataDictionaryEntry(
                existing == null ? "erp-dictionary-" + ERP_MASTER_DATA_DICTIONARY_SEQUENCE.incrementAndGet() : existing.dictionaryEntryId(),
                tenantId,
                dictionaryType,
                dictionaryCode,
                normalizeRequired(request.dictionaryName(), "dictionaryName"),
                normalizeRequired(request.erpCode(), "erpCode"),
                normalizeRequired(request.erpName(), "erpName"),
                request.enabled(),
                operatorId,
                existing == null ? now : existing.createdAt(),
                now,
                normalizeOptionalText(request.remark())
        );
        ERP_MASTER_DATA_DICTIONARY_STORAGE.put(key, saved);
        auditLogService.recordForTenant(tenantId, "SAVE_ERP_MASTER_DATA_DICTIONARY", "erp_master_data_dictionary", saved.dictionaryEntryId());
        return saved;
    }

    public List<ErpAccountMappingEntry> listErpAccountMappings(String tenantId,
                                                               String mappingCategory,
                                                               String storeId) {
        String normalizedCategory = normalizeFilter(mappingCategory);
        String normalizedStoreId = normalizeFilter(storeId);
        return ERP_ACCOUNT_MAPPING_STORAGE.values().stream()
                .filter(entry -> tenantId.equals(entry.tenantId()))
                .filter(entry -> normalizedCategory == null || normalizedCategory.equals(entry.mappingCategory()))
                .filter(entry -> normalizedStoreId == null || normalizedStoreId.equals(entry.storeId()))
                .sorted(Comparator.comparing(ErpAccountMappingEntry::mappingCategory)
                        .thenComparing(ErpAccountMappingEntry::storeId)
                        .thenComparing(ErpAccountMappingEntry::businessCode))
                .toList();
    }

    public ErpAccountMappingEntry saveErpAccountMapping(String tenantId,
                                                        String operatorId,
                                                        SaveErpAccountMappingRequest request) {
        String normalizedStoreId = normalizeRequired(request.storeId(), "storeId");
        requireOwnedStore(tenantId, normalizedStoreId);
        String mappingCategory = normalizeRequired(request.mappingCategory(), "mappingCategory");
        String businessType = normalizeRequired(request.businessType(), "businessType");
        String businessCode = normalizeRequired(request.businessCode(), "businessCode");
        String direction = normalizeRequired(request.direction(), "direction");
        if (!List.of("debit", "credit").contains(direction)) {
            throw new BusinessException("7424", "direction must be debit or credit", HttpStatus.BAD_REQUEST);
        }
        String key = tenantId + "|" + normalizedStoreId + "|" + mappingCategory + "|" + businessType + "|" + businessCode;
        OffsetDateTime now = OffsetDateTime.now();
        ErpAccountMappingEntry existing = ERP_ACCOUNT_MAPPING_STORAGE.get(key);
        ErpAccountMappingEntry saved = new ErpAccountMappingEntry(
                existing == null ? "erp-account-mapping-" + ERP_ACCOUNT_MAPPING_SEQUENCE.incrementAndGet() : existing.mappingId(),
                tenantId,
                normalizedStoreId,
                mappingCategory,
                businessType,
                businessCode,
                normalizeRequired(request.subjectCode(), "subjectCode"),
                normalizeRequired(request.subjectName(), "subjectName"),
                direction,
                request.enabled(),
                operatorId,
                existing == null ? now : existing.createdAt(),
                now,
                normalizeOptionalText(request.remark())
        );
        ERP_ACCOUNT_MAPPING_STORAGE.put(key, saved);
        auditLogService.recordForTenant(tenantId, "SAVE_ERP_ACCOUNT_MAPPING", "erp_account_mapping", saved.mappingId());
        return saved;
    }

    public ErpIntegrationBaselineView getErpIntegrationBaseline(String tenantId) {
        int availableStoreCount = storeRepository.findByTenantId(tenantId).size();
        int dictionaryCount = (int) ERP_MASTER_DATA_DICTIONARY_STORAGE.values().stream()
                .filter(entry -> tenantId.equals(entry.tenantId()))
                .count();
        int accountMappingCount = (int) ERP_ACCOUNT_MAPPING_STORAGE.values().stream()
                .filter(entry -> tenantId.equals(entry.tenantId()))
                .count();
        return new ErpIntegrationBaselineView(
                "pull",
                availableStoreCount,
                dictionaryCount,
                accountMappingCount,
                List.of(
                        "master_data_dictionary",
                        "account_mapping",
                        "finance_general_ledger",
                        "closing_check",
                        "period_closing"
                ),
                OffsetDateTime.now()
        );
    }

    public ProfitStatementView getProfitStatement(String tenantId,
                                                  String storeId,
                                                  LocalDate periodStart,
                                                  LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);
        Store store = requireOwnedStore(tenantId, storeId);
        ProfitMetrics metrics = buildProfitMetrics(tenantId, store, periodStart, periodEnd);
        return new ProfitStatementView(
                store.storeId(),
                store.shopName(),
                periodStart,
                periodEnd,
                metrics.salesOrderCount(),
                metrics.salesIncomeAmount(),
                metrics.estimatedCostAmount(),
                metrics.grossProfitAmount(),
                metrics.allocatedExpenseAmount(),
                metrics.netProfitAmount(),
                metrics.profitMarginRate(),
                store.profitThreshold(),
                metrics.thresholdStatus(),
                metrics.outstandingReceivableAmount(),
                metrics.expenseBreakdown()
        );
    }

    public List<StoreProfitReportView> listStoreProfitReports(String tenantId,
                                                              LocalDate periodStart,
                                                              LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);
        return storeRepository.findByTenantId(tenantId).stream()
                .map(store -> new StoreProfitReportEnvelope(
                        store,
                        buildProfitMetrics(tenantId, store, periodStart, periodEnd)
                ))
                .filter(envelope -> hasProfitActivity(envelope.metrics()))
                .map(envelope -> new StoreProfitReportView(
                        envelope.store().storeId(),
                        envelope.store().shopName(),
                        periodStart,
                        periodEnd,
                        envelope.metrics().salesOrderCount(),
                        envelope.metrics().salesIncomeAmount(),
                        envelope.metrics().grossProfitAmount(),
                        envelope.metrics().allocatedExpenseAmount(),
                        envelope.metrics().netProfitAmount(),
                        envelope.metrics().profitMarginRate(),
                        envelope.store().profitThreshold(),
                        envelope.metrics().thresholdStatus(),
                        envelope.metrics().outstandingReceivableAmount()
                ))
                .sorted(Comparator.comparing(StoreProfitReportView::netProfitAmount).reversed()
                        .thenComparing(StoreProfitReportView::salesIncomeAmount, Comparator.reverseOrder()))
                .toList();
    }

    public List<FinanceGeneralLedgerView> listFinanceGeneralLedgers(String tenantId,
                                                                    LocalDate periodStart,
                                                                    LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);
        return storeRepository.findByTenantId(tenantId).stream()
                .map(store -> new FinanceGeneralLedgerEnvelope(
                        store,
                        buildGeneralLedgerMetrics(tenantId, store, periodStart, periodEnd)
                ))
                .filter(envelope -> hasGeneralLedgerActivity(envelope.metrics()))
                .map(envelope -> toGeneralLedgerView(envelope.store(), periodStart, periodEnd, envelope.metrics()))
                .sorted(Comparator.comparing(FinanceGeneralLedgerView::salesIncomeAmount).reversed()
                        .thenComparing(FinanceGeneralLedgerView::shopName))
                .toList();
    }

    public FinanceClosingCheckView getFinanceClosingCheck(String tenantId,
                                                          String storeId,
                                                          LocalDate periodStart,
                                                          LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);
        Store store = requireOwnedStore(tenantId, storeId);
        GeneralLedgerMetrics metrics = buildGeneralLedgerMetrics(tenantId, store, periodStart, periodEnd);
        List<FinanceClosingCheckItemView> checkItems = List.of(
                buildClosingCheckItem(
                        "finance_bill_settlement",
                        "Finance Bill Settlement",
                        metrics.pendingFinanceBillCount(),
                        metrics.pendingFinanceBillCount() == 0
                                ? "all finance bills in the period are settled"
                                : metrics.pendingFinanceBillCount() + " finance bills are not settled"
                ),
                buildClosingCheckItem(
                        "receivable_collection",
                        "Receivable Collection",
                        metrics.outstandingReceivableOrderCount(),
                        metrics.outstandingReceivableOrderCount() == 0
                                ? "all receivables in the period are collected"
                                : metrics.outstandingReceivableOrderCount() + " receivable orders remain outstanding"
                ),
                buildClosingCheckItem(
                        "voucher_archiving",
                        "Voucher Archiving",
                        metrics.pendingVoucherCount(),
                        metrics.pendingVoucherCount() == 0
                                ? "all settlement and payment vouchers are archived"
                                : metrics.pendingVoucherCount() + " settlement or payment vouchers are pending archive"
                ),
                buildClosingCheckItem(
                        "invoice_archiving",
                        "Invoice Archiving",
                        metrics.pendingInvoiceCount(),
                        metrics.pendingInvoiceCount() == 0
                                ? "all finance invoices are archived or voided"
                                : metrics.pendingInvoiceCount() + " finance invoices are pending archive"
                ),
                buildClosingCheckItem(
                        "supplier_reconciliation",
                        "Supplier Reconciliation",
                        metrics.pendingSupplierDiscrepancyCount(),
                        metrics.pendingSupplierDiscrepancyCount() == 0
                                ? "supplier reconciliations have no pending discrepancies"
                                : metrics.pendingSupplierDiscrepancyCount() + " supplier discrepancies remain unresolved"
                )
        );
        int blockingIssueCount = (int) checkItems.stream()
                .filter(item -> "blocking".equals(item.checkStatus()))
                .count();
        return new FinanceClosingCheckView(
                store.storeId(),
                store.shopName(),
                periodStart,
                periodEnd,
                blockingIssueCount == 0,
                blockingIssueCount,
                checkItems
        );
    }

    public CustomerPaymentRecord recordCustomerPayment(String tenantId,
                                                       String operatorId,
                                                       RecordCustomerPaymentRequest request) {
        OrderMain order = requireOwnedOrder(tenantId, request.orderId());
        if (request.paymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("7410", "customer payment amount must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        BigDecimal collectedAmount = collectedAmount(order.orderId());
        BigDecimal outstandingAmount = order.totalAmount().subtract(collectedAmount);
        if (request.paymentAmount().compareTo(outstandingAmount) > 0) {
            throw new BusinessException("7411", "customer payment exceeds outstanding receivable", HttpStatus.BAD_REQUEST);
        }
        String sequenceValue = String.valueOf(CUSTOMER_PAYMENT_SEQUENCE.incrementAndGet());
        CustomerPaymentRecord paymentRecord = new CustomerPaymentRecord(
                "customer-payment-" + sequenceValue,
                "CPR-" + sequenceValue,
                order.storeId(),
                order.orderId(),
                order.platformOrderId(),
                order.buyerName(),
                request.paymentChannel() == null || request.paymentChannel().isBlank() ? "online" : request.paymentChannel(),
                request.paymentAmount(),
                outstandingAmount.compareTo(request.paymentAmount()) == 0 ? "collected" : "partial_collected",
                operatorId,
                OffsetDateTime.now(),
                request.remark()
        );
        CUSTOMER_PAYMENT_STORAGE.put(paymentRecord.paymentRecordId(), paymentRecord);
        auditLogService.recordForTenant(tenantId, "RECORD_CUSTOMER_PAYMENT", "customer_payment_record", paymentRecord.paymentRecordId());
        return paymentRecord;
    }

    public FinanceBill generateBill(String tenantId, GenerateFinanceBillRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new BusinessException("7401", "finance bill periodEnd must be after periodStart", HttpStatus.BAD_REQUEST);
        }
        financeBillRepository.findByStoreAndPeriod(request.storeId(), request.billType(), request.periodStart(), request.periodEnd())
                .ifPresent(existing -> {
                    throw new BusinessException("7402", "finance bill already exists for period", HttpStatus.BAD_REQUEST);
                });
        FinanceBillCalculation calculation = calculateBill(tenantId, request.storeId(), request.periodStart(), request.periodEnd());
        FinanceBill saved = financeBillRepository.save(new FinanceBill(
                null,
                request.storeId(),
                request.billType(),
                request.periodStart(),
                request.periodEnd(),
                calculation.incomeAmount(),
                calculation.costAmount(),
                calculation.grossProfit(),
                "draft",
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "GENERATE_FINANCE_BILL", "finance_bill", saved.financeBillId());
        return saved;
    }

    public FinanceBillDetailView getFinanceBill(String tenantId, String financeBillId) {
        FinanceBill bill = requireOwnedBill(tenantId, financeBillId);
        List<SettlementRecord> settlements = settlementRecordRepository.findByFinanceBillId(financeBillId);
        return new FinanceBillDetailView(
                bill,
                settlements,
                settlements.isEmpty() ? "pending_split" : "completed",
                bill.grossProfit().compareTo(BigDecimal.ZERO) < 0 ? "risk_review" : resolveInvoiceCheckStatus(financeBillId)
        );
    }

    public FinanceBillReconcileView reconcile(String tenantId, String financeBillId) {
        FinanceBill bill = requireOwnedBill(tenantId, financeBillId);
        int discrepancyCount = bill.incomeAmount().compareTo(BigDecimal.ZERO) == 0 ? 1 : 0;
        String nextStatus = discrepancyCount > 0 || bill.grossProfit().compareTo(BigDecimal.ZERO) < 0
                ? "exception"
                : "reconciled";
        FinanceBill updated = financeBillRepository.save(bill.withStatus(nextStatus));
        auditLogService.recordForTenant(tenantId, "RECONCILE_FINANCE_BILL", "finance_bill", financeBillId);
        return new FinanceBillReconcileView(
                updated.financeBillId(),
                updated.billStatus(),
                discrepancyCount,
                discrepancyCount > 0 ? "manual_review" : resolveInvoiceCheckStatus(financeBillId)
        );
    }

    public SettlementRecord settle(String tenantId, String financeBillId, SettleFinanceBillRequest request) {
        FinanceBill bill = requireOwnedBill(tenantId, financeBillId);
        if (!List.of("reconciled", "settled").contains(bill.billStatus())) {
            throw new BusinessException("7403", "finance bill must be reconciled before settle", HttpStatus.BAD_REQUEST);
        }
        if ("settled".equals(bill.billStatus()) && !settlementRecordRepository.findByFinanceBillId(financeBillId).isEmpty()) {
            return settlementRecordRepository.findByFinanceBillId(financeBillId).get(0);
        }
        SettlementRecord saved = settlementRecordRepository.save(new SettlementRecord(
                null,
                financeBillId,
                request.settlementType() == null || request.settlementType().isBlank() ? "bank_transfer" : request.settlementType(),
                bill.grossProfit(),
                "settled",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        ));
        financeBillRepository.save(bill.withStatus("settled"));
        auditLogService.recordForTenant(tenantId, "SETTLE_FINANCE_BILL", "settlement_record", saved.settlementRecordId());
        return saved;
    }

    public List<FinanceVoucher> listFinanceVouchers(String tenantId) {
        return FINANCE_VOUCHER_STORAGE.values().stream()
                .filter(record -> ownedStoreIds(tenantId).contains(record.storeId()))
                .sorted((left, right) -> right.archivedAt().compareTo(left.archivedAt()))
                .toList();
    }

    public List<FinanceInvoice> listFinanceInvoices(String tenantId) {
        return FINANCE_INVOICE_STORAGE.values().stream()
                .filter(record -> ownedStoreIds(tenantId).contains(record.storeId()))
                .sorted((left, right) -> right.updatedAt().compareTo(left.updatedAt()))
                .toList();
    }

    public FinanceInvoice issueFinanceInvoice(String tenantId,
                                              String operatorId,
                                              IssueFinanceInvoiceRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        if (request.invoiceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("7416", "finance invoice amount must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        validateInvoiceReference(tenantId, request.storeId(), request.referenceType(), request.referenceId());
        String sequenceValue = String.valueOf(FINANCE_INVOICE_SEQUENCE.incrementAndGet());
        OffsetDateTime now = OffsetDateTime.now();
        FinanceInvoice invoice = new FinanceInvoice(
                "finance-invoice-" + sequenceValue,
                "FIV-" + sequenceValue,
                request.storeId(),
                request.referenceType(),
                request.referenceId(),
                null,
                request.invoiceTitle(),
                request.invoiceTaxNo(),
                request.invoiceAmount(),
                "issued",
                "pending_archive",
                false,
                operatorId,
                now,
                now,
                request.remark()
        );
        FINANCE_INVOICE_STORAGE.put(invoice.invoiceId(), invoice);
        auditLogService.recordForTenant(tenantId, "ISSUE_FINANCE_INVOICE", "finance_invoice", invoice.invoiceId());
        return invoice;
    }

    public FinanceInvoice archiveFinanceInvoice(String tenantId,
                                                String operatorId,
                                                String invoiceId,
                                                String remark) {
        FinanceInvoice current = requireOwnedInvoice(tenantId, invoiceId);
        if (!List.of("issued", "red_flushed").contains(current.invoiceStatus())) {
            throw new BusinessException("7417", "finance invoice current status does not allow archive", HttpStatus.BAD_REQUEST);
        }
        FinanceInvoice updated = current.withArchiveStatus("archived", operatorId, remark);
        FINANCE_INVOICE_STORAGE.put(updated.invoiceId(), updated);
        auditLogService.recordForTenant(tenantId, "ARCHIVE_FINANCE_INVOICE", "finance_invoice", invoiceId);
        return updated;
    }

    public FinanceInvoice voidFinanceInvoice(String tenantId,
                                             String operatorId,
                                             String invoiceId,
                                             String remark) {
        FinanceInvoice current = requireOwnedInvoice(tenantId, invoiceId);
        if (!List.of("issued", "archived").contains(current.invoiceStatus())) {
            throw new BusinessException("7418", "finance invoice current status does not allow void", HttpStatus.BAD_REQUEST);
        }
        FinanceInvoice updated = current.withStatus("voided", operatorId, remark);
        FINANCE_INVOICE_STORAGE.put(updated.invoiceId(), updated);
        auditLogService.recordForTenant(tenantId, "VOID_FINANCE_INVOICE", "finance_invoice", invoiceId);
        return updated;
    }

    public FinanceInvoice redFlushFinanceInvoice(String tenantId,
                                                 String operatorId,
                                                 String invoiceId,
                                                 String remark) {
        FinanceInvoice sourceInvoice = requireOwnedInvoice(tenantId, invoiceId);
        if (!List.of("issued", "archived").contains(sourceInvoice.invoiceStatus())) {
            throw new BusinessException("7419", "finance invoice current status does not allow red flush", HttpStatus.BAD_REQUEST);
        }
        boolean existsRedFlush = FINANCE_INVOICE_STORAGE.values().stream()
                .anyMatch(invoice -> invoiceId.equals(invoice.sourceInvoiceId()));
        if (existsRedFlush) {
            throw new BusinessException("7420", "finance invoice has already been red flushed", HttpStatus.BAD_REQUEST);
        }
        String sequenceValue = String.valueOf(FINANCE_INVOICE_SEQUENCE.incrementAndGet());
        OffsetDateTime now = OffsetDateTime.now();
        FinanceInvoice redFlushInvoice = new FinanceInvoice(
                "finance-invoice-" + sequenceValue,
                "FIV-" + sequenceValue,
                sourceInvoice.storeId(),
                sourceInvoice.referenceType(),
                sourceInvoice.referenceId(),
                sourceInvoice.invoiceId(),
                sourceInvoice.invoiceTitle(),
                sourceInvoice.invoiceTaxNo(),
                sourceInvoice.invoiceAmount().negate(),
                "red_flushed",
                "pending_archive",
                true,
                operatorId,
                now,
                now,
                remark
        );
        FINANCE_INVOICE_STORAGE.put(redFlushInvoice.invoiceId(), redFlushInvoice);
        auditLogService.recordForTenant(tenantId, "RED_FLUSH_FINANCE_INVOICE", "finance_invoice", redFlushInvoice.invoiceId());
        return redFlushInvoice;
    }

    public FinanceVoucher archiveFinanceVoucher(String tenantId,
                                                String operatorId,
                                                ArchiveFinanceVoucherRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        if (request.voucherAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("7412", "finance voucher amount must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        String sequenceValue = String.valueOf(FINANCE_VOUCHER_SEQUENCE.incrementAndGet());
        FinanceVoucher voucher = new FinanceVoucher(
                "finance-voucher-" + sequenceValue,
                "FAV-" + sequenceValue,
                request.storeId(),
                request.referenceType(),
                request.referenceId(),
                request.voucherType(),
                request.voucherAmount(),
                "archived",
                operatorId,
                OffsetDateTime.now(),
                request.remark()
        );
        FINANCE_VOUCHER_STORAGE.put(voucher.voucherId(), voucher);
        auditLogService.recordForTenant(tenantId, "ARCHIVE_FINANCE_VOUCHER", "finance_voucher", voucher.voucherId());
        return voucher;
    }

    public List<FinancePeriodClosingRecord> listFinancePeriodClosings(String tenantId) {
        return FINANCE_PERIOD_CLOSING_STORAGE.values().stream()
                .filter(record -> ownedStoreIds(tenantId).contains(record.storeId()))
                .sorted((left, right) -> right.closedAt().compareTo(left.closedAt()))
                .toList();
    }

    public FinancePeriodClosingRecord closeFinancePeriod(String tenantId,
                                                         String operatorId,
                                                         CloseFinancePeriodRequest request) {
        Store store = requireOwnedStore(tenantId, request.storeId());
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new BusinessException("7413", "finance period end must be after period start", HttpStatus.BAD_REQUEST);
        }
        List<FinanceBill> bills = financeBillRepository.findByStoreIds(List.of(request.storeId())).stream()
                .filter(bill -> !bill.periodEnd().isBefore(request.periodStart()) && !bill.periodStart().isAfter(request.periodEnd()))
                .toList();
        boolean hasUnsettledBill = bills.stream()
                .anyMatch(bill -> !"settled".equals(bill.billStatus()));
        if (hasUnsettledBill) {
            throw new BusinessException("7414", "finance bills must be settled before period closing", HttpStatus.BAD_REQUEST);
        }
        GeneralLedgerMetrics metrics = buildGeneralLedgerMetrics(tenantId, store, request.periodStart(), request.periodEnd());
        if (!"ready".equals(closingStatus(metrics))) {
            throw new BusinessException("7423", "finance closing checks must pass before period closing", HttpStatus.BAD_REQUEST);
        }
        long archivedVoucherCount = FINANCE_VOUCHER_STORAGE.values().stream()
                .filter(voucher -> request.storeId().equals(voucher.storeId()))
                .filter(voucher -> "archived".equals(voucher.archiveStatus()))
                .count();
        String sequenceValue = String.valueOf(FINANCE_PERIOD_CLOSING_SEQUENCE.incrementAndGet());
        FinancePeriodClosingRecord record = new FinancePeriodClosingRecord(
                "finance-period-closing-" + sequenceValue,
                "FPC-" + sequenceValue,
                request.storeId(),
                request.periodStart(),
                request.periodEnd(),
                "closed",
                operatorId,
                bills.size(),
                (int) archivedVoucherCount,
                OffsetDateTime.now(),
                request.remark()
        );
        FINANCE_PERIOD_CLOSING_STORAGE.put(record.closingRecordId(), record);
        auditLogService.recordForTenant(tenantId, "CLOSE_FINANCE_PERIOD", "finance_period_closing", record.closingRecordId());
        return record;
    }

    public void clear() {
        CUSTOMER_PAYMENT_STORAGE.clear();
        CUSTOMER_PAYMENT_SEQUENCE.set(11000);
        FINANCE_VOUCHER_STORAGE.clear();
        FINANCE_VOUCHER_SEQUENCE.set(12000);
        FINANCE_PERIOD_CLOSING_STORAGE.clear();
        FINANCE_PERIOD_CLOSING_SEQUENCE.set(13000);
        FINANCE_INVOICE_STORAGE.clear();
        FINANCE_INVOICE_SEQUENCE.set(14000);
        ERP_MASTER_DATA_DICTIONARY_STORAGE.clear();
        ERP_MASTER_DATA_DICTIONARY_SEQUENCE.set(15000);
        ERP_ACCOUNT_MAPPING_STORAGE.clear();
        ERP_ACCOUNT_MAPPING_SEQUENCE.set(16000);
        settlementRecordRepository.deleteAll();
        financeBillRepository.deleteAll();
    }

    private FinanceBillCalculation calculateBill(String tenantId, String storeId, LocalDate periodStart, LocalDate periodEnd) {
        List<OrderMain> orders = ordersInPeriod(storeId, periodStart, periodEnd);
        BigDecimal incomeAmount = orders.stream()
                .map(OrderMain::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossProfit = orders.stream()
                .map(OrderMain::estimatedProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal costAmount = incomeAmount.subtract(grossProfit);
        requireOwnedStore(tenantId, storeId);
        return new FinanceBillCalculation(incomeAmount, costAmount.max(BigDecimal.ZERO), grossProfit);
    }

    private ReceivableLedgerEntry toReceivableLedgerEntry(OrderMain order) {
        BigDecimal collectedAmount = collectedAmount(order.orderId());
        BigDecimal outstandingAmount = order.totalAmount().subtract(collectedAmount);
        String receivableStatus = outstandingAmount.compareTo(BigDecimal.ZERO) == 0
                ? "collected"
                : collectedAmount.compareTo(BigDecimal.ZERO) > 0 ? "partial" : "pending";
        return new ReceivableLedgerEntry(
                "receivable-ledger-" + order.orderId(),
                "RLG-" + order.orderId(),
                order.storeId(),
                order.orderId(),
                order.platformOrderId(),
                order.buyerName(),
                order.totalAmount(),
                collectedAmount,
                outstandingAmount,
                receivableStatus,
                order.createdAt()
        );
    }

    private BigDecimal collectedAmount(String orderId) {
        return CUSTOMER_PAYMENT_STORAGE.values().stream()
                .filter(record -> orderId.equals(record.orderId()))
                .map(CustomerPaymentRecord::paymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ProfitMetrics buildProfitMetrics(String tenantId,
                                             Store store,
                                             LocalDate periodStart,
                                             LocalDate periodEnd) {
        List<OrderMain> orders = ordersInPeriod(store.storeId(), periodStart, periodEnd);
        BigDecimal salesIncomeAmount = orders.stream()
                .map(OrderMain::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossProfitAmount = orders.stream()
                .map(OrderMain::estimatedProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal estimatedCostAmount = orders.stream()
                .map(order -> order.totalAmount().subtract(order.estimatedProfit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstandingReceivableAmount = orders.stream()
                .map(order -> order.totalAmount().subtract(collectedAmount(order.orderId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InventoryService.PurchaseExpenseAllocation> expenseAllocations = inventoryService.listPurchaseExpenseAllocations(tenantId).stream()
                .filter(allocation -> store.storeId().equals(allocation.storeId()))
                .filter(allocation -> isInPeriod(allocation.allocatedAt(), periodStart, periodEnd))
                .toList();
        BigDecimal allocatedExpenseAmount = expenseAllocations.stream()
                .map(InventoryService.PurchaseExpenseAllocation::expenseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netProfitAmount = grossProfitAmount.subtract(allocatedExpenseAmount);
        BigDecimal profitMarginRate = percentage(netProfitAmount, salesIncomeAmount);

        Map<String, ExpenseSummaryAccumulator> expenseSummaryMap = new ConcurrentHashMap<>();
        for (InventoryService.PurchaseExpenseAllocation allocation : expenseAllocations) {
            expenseSummaryMap.computeIfAbsent(
                    allocation.expenseType(),
                    ignored -> new ExpenseSummaryAccumulator(allocation.expenseType())
            ).add(allocation.expenseAmount());
        }
        List<ExpenseBreakdownView> expenseBreakdown = expenseSummaryMap.values().stream()
                .map(ExpenseSummaryAccumulator::toView)
                .sorted(Comparator.comparing(ExpenseBreakdownView::expenseAmount).reversed())
                .toList();

        return new ProfitMetrics(
                orders.size(),
                salesIncomeAmount,
                estimatedCostAmount,
                grossProfitAmount,
                allocatedExpenseAmount,
                netProfitAmount,
                profitMarginRate,
                thresholdStatus(store.profitThreshold(), salesIncomeAmount, netProfitAmount, profitMarginRate),
                outstandingReceivableAmount,
                expenseBreakdown
        );
    }

    private GeneralLedgerMetrics buildGeneralLedgerMetrics(String tenantId,
                                                           Store store,
                                                           LocalDate periodStart,
                                                           LocalDate periodEnd) {
        List<FinanceBill> financeBills = financeBillRepository.findByStoreIds(List.of(store.storeId())).stream()
                .filter(bill -> periodsOverlap(bill.periodStart(), bill.periodEnd(), periodStart, periodEnd))
                .toList();
        int financeBillCount = financeBills.size();
        int settledFinanceBillCount = (int) financeBills.stream()
                .filter(bill -> "settled".equals(bill.billStatus()))
                .count();

        List<OrderMain> orders = ordersInPeriod(store.storeId(), periodStart, periodEnd);
        BigDecimal salesIncomeAmount = orders.stream()
                .map(OrderMain::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OrderMain> outstandingOrders = orders.stream()
                .filter(order -> order.totalAmount().subtract(collectedAmount(order.orderId())).compareTo(BigDecimal.ZERO) > 0)
                .toList();
        int outstandingReceivableOrderCount = outstandingOrders.size();
        BigDecimal outstandingReceivableAmount = outstandingOrders.stream()
                .map(order -> order.totalAmount().subtract(collectedAmount(order.orderId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CustomerPaymentRecord> paymentRecords = CUSTOMER_PAYMENT_STORAGE.values().stream()
                .filter(record -> store.storeId().equals(record.storeId()))
                .filter(record -> isInPeriod(record.receivedAt(), periodStart, periodEnd))
                .toList();
        BigDecimal receivableCollectedAmount = paymentRecords.stream()
                .map(CustomerPaymentRecord::paymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InventoryService.PayableLedgerEntry> payableLedgers = inventoryService.listPayableLedgers(tenantId).stream()
                .filter(entry -> store.storeId().equals(entry.storeId()))
                .filter(entry -> isInPeriod(entry.occurredAt(), periodStart, periodEnd))
                .toList();
        BigDecimal payableNetAmount = payableLedgers.stream()
                .map(this::signedPayableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SettlementRecord> settlements = financeBills.stream()
                .flatMap(bill -> settlementRecordRepository.findByFinanceBillId(bill.financeBillId()).stream())
                .filter(record -> isInPeriod(record.settledAt() == null ? record.createdAt() : record.settledAt(), periodStart, periodEnd))
                .toList();
        BigDecimal settlementAmount = settlements.stream()
                .map(SettlementRecord::settlementAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FinanceVoucher> vouchers = FINANCE_VOUCHER_STORAGE.values().stream()
                .filter(voucher -> store.storeId().equals(voucher.storeId()))
                .filter(voucher -> isInPeriod(voucher.archivedAt(), periodStart, periodEnd))
                .toList();
        BigDecimal archivedVoucherAmount = vouchers.stream()
                .map(FinanceVoucher::voucherAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FinanceInvoice> invoices = FINANCE_INVOICE_STORAGE.values().stream()
                .filter(invoice -> store.storeId().equals(invoice.storeId()))
                .filter(invoice -> isInPeriod(invoice.issuedAt(), periodStart, periodEnd))
                .toList();
        BigDecimal issuedInvoiceAmount = invoices.stream()
                .filter(invoice -> !invoice.redFlush())
                .filter(invoice -> !"voided".equals(invoice.invoiceStatus()))
                .map(FinanceInvoice::invoiceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal redFlushInvoiceAmount = invoices.stream()
                .filter(FinanceInvoice::redFlush)
                .map(FinanceInvoice::invoiceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int pendingVoucherCount = countPendingVoucherRecords(settlements, paymentRecords, vouchers);
        int pendingInvoiceCount = (int) invoices.stream()
                .filter(invoice -> !"voided".equals(invoice.invoiceStatus()))
                .filter(invoice -> !"archived".equals(invoice.archiveStatus()))
                .count();

        int pendingSupplierDiscrepancyCount = inventoryService.listSupplierReconciliations(tenantId).stream()
                .filter(reconciliation -> store.storeId().equals(reconciliation.storeId()))
                .filter(reconciliation -> !"matched".equals(reconciliation.reconciliationStatus()))
                .mapToInt(InventoryService.SupplierReconciliationView::pendingDiscrepancyCount)
                .sum();

        return new GeneralLedgerMetrics(
                financeBillCount,
                settledFinanceBillCount,
                salesIncomeAmount,
                receivableCollectedAmount,
                outstandingReceivableAmount,
                outstandingReceivableOrderCount,
                payableNetAmount,
                settlementAmount,
                archivedVoucherAmount,
                issuedInvoiceAmount,
                redFlushInvoiceAmount,
                pendingVoucherCount,
                pendingInvoiceCount,
                pendingSupplierDiscrepancyCount
        );
    }

    private List<OrderMain> ordersInPeriod(String storeId, LocalDate periodStart, LocalDate periodEnd) {
        return orderRepository.findByStoreIds(List.of(storeId)).stream()
                .filter(order -> isInPeriod(order.createdAt(), periodStart, periodEnd))
                .toList();
    }

    private boolean isInPeriod(OffsetDateTime occurredAt, LocalDate periodStart, LocalDate periodEnd) {
        LocalDate occurredDate = occurredAt.toLocalDate();
        return !occurredDate.isBefore(periodStart) && !occurredDate.isAfter(periodEnd);
    }

    private boolean periodsOverlap(LocalDate currentStart,
                                   LocalDate currentEnd,
                                   LocalDate targetStart,
                                   LocalDate targetEnd) {
        return !currentEnd.isBefore(targetStart) && !currentStart.isAfter(targetEnd);
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private String thresholdStatus(BigDecimal targetProfitThreshold,
                                   BigDecimal salesIncomeAmount,
                                   BigDecimal netProfitAmount,
                                   BigDecimal profitMarginRate) {
        if (salesIncomeAmount.compareTo(BigDecimal.ZERO) == 0) {
            return "no_sales";
        }
        if (netProfitAmount.compareTo(BigDecimal.ZERO) < 0) {
            return "loss";
        }
        return profitMarginRate.compareTo(targetProfitThreshold) >= 0 ? "healthy" : "below_threshold";
    }

    private FinanceGeneralLedgerView toGeneralLedgerView(Store store,
                                                         LocalDate periodStart,
                                                         LocalDate periodEnd,
                                                         GeneralLedgerMetrics metrics) {
        return new FinanceGeneralLedgerView(
                store.storeId(),
                store.shopName(),
                periodStart,
                periodEnd,
                metrics.financeBillCount(),
                metrics.settledFinanceBillCount(),
                metrics.salesIncomeAmount(),
                metrics.receivableCollectedAmount(),
                metrics.outstandingReceivableAmount(),
                metrics.payableNetAmount(),
                metrics.settlementAmount(),
                metrics.archivedVoucherAmount(),
                metrics.issuedInvoiceAmount(),
                metrics.redFlushInvoiceAmount(),
                metrics.pendingVoucherCount(),
                metrics.pendingInvoiceCount(),
                closingStatus(metrics)
        );
    }

    private FinanceClosingCheckItemView buildClosingCheckItem(String checkCode,
                                                              String checkName,
                                                              int pendingCount,
                                                              String detail) {
        return new FinanceClosingCheckItemView(
                checkCode,
                checkName,
                pendingCount == 0 ? "passed" : "blocking",
                pendingCount,
                detail
        );
    }

    private String closingStatus(GeneralLedgerMetrics metrics) {
        return metrics.pendingFinanceBillCount() == 0
                && metrics.outstandingReceivableOrderCount() == 0
                && metrics.pendingVoucherCount() == 0
                && metrics.pendingInvoiceCount() == 0
                && metrics.pendingSupplierDiscrepancyCount() == 0
                ? "ready"
                : "pending";
    }

    private boolean hasGeneralLedgerActivity(GeneralLedgerMetrics metrics) {
        return metrics.financeBillCount() > 0
                || metrics.salesIncomeAmount().compareTo(BigDecimal.ZERO) != 0
                || metrics.receivableCollectedAmount().compareTo(BigDecimal.ZERO) != 0
                || metrics.payableNetAmount().compareTo(BigDecimal.ZERO) != 0
                || metrics.issuedInvoiceAmount().compareTo(BigDecimal.ZERO) != 0
                || metrics.redFlushInvoiceAmount().compareTo(BigDecimal.ZERO) != 0;
    }

    private BigDecimal signedPayableAmount(InventoryService.PayableLedgerEntry entry) {
        return "payable_decrease".equals(entry.direction())
                ? entry.entryAmount().negate()
                : entry.entryAmount();
    }

    private int countPendingVoucherRecords(List<SettlementRecord> settlements,
                                           List<CustomerPaymentRecord> paymentRecords,
                                           List<FinanceVoucher> vouchers) {
        long pendingSettlements = settlements.stream()
                .filter(settlement -> vouchers.stream()
                        .noneMatch(voucher -> "finance_settlement".equals(voucher.referenceType())
                                && settlement.settlementRecordId().equals(voucher.referenceId())))
                .count();
        long pendingPayments = paymentRecords.stream()
                .filter(payment -> vouchers.stream()
                        .noneMatch(voucher -> "customer_payment".equals(voucher.referenceType())
                                && payment.paymentRecordId().equals(voucher.referenceId())))
                .count();
        return (int) (pendingSettlements + pendingPayments);
    }

    private void validateInvoiceReference(String tenantId,
                                          String storeId,
                                          String referenceType,
                                          String referenceId) {
        if ("finance_bill".equals(referenceType)) {
            FinanceBill bill = requireOwnedBill(tenantId, referenceId);
            if (!storeId.equals(bill.storeId())) {
                throw new BusinessException("7421", "finance invoice storeId does not match finance bill", HttpStatus.BAD_REQUEST);
            }
            if (!"settled".equals(bill.billStatus())) {
                throw new BusinessException("7422", "finance bill must be settled before invoice issue", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private String resolveInvoiceCheckStatus(String financeBillId) {
        List<FinanceInvoice> invoices = FINANCE_INVOICE_STORAGE.values().stream()
                .filter(invoice -> "finance_bill".equals(invoice.referenceType()))
                .filter(invoice -> financeBillId.equals(invoice.referenceId()))
                .toList();
        if (invoices.isEmpty()) {
            return "matched";
        }
        if (invoices.stream().anyMatch(FinanceInvoice::redFlush)) {
            return "red_flushed";
        }
        if (invoices.stream().anyMatch(invoice -> "archived".equals(invoice.archiveStatus()))) {
            return "archived";
        }
        if (invoices.stream().anyMatch(invoice -> "issued".equals(invoice.invoiceStatus()))) {
            return "issued";
        }
        if (invoices.stream().allMatch(invoice -> "voided".equals(invoice.invoiceStatus()))) {
            return "voided";
        }
        return "matched";
    }

    private boolean hasProfitActivity(ProfitMetrics metrics) {
        return metrics.salesOrderCount() > 0
                || metrics.salesIncomeAmount().compareTo(BigDecimal.ZERO) != 0
                || metrics.allocatedExpenseAmount().compareTo(BigDecimal.ZERO) != 0
                || metrics.outstandingReceivableAmount().compareTo(BigDecimal.ZERO) != 0;
    }

    private void validatePeriod(LocalDate periodStart, LocalDate periodEnd) {
        if (periodEnd.isBefore(periodStart)) {
            throw new BusinessException("7415", "periodEnd must be after periodStart", HttpStatus.BAD_REQUEST);
        }
    }

    private FinanceBill requireOwnedBill(String tenantId, String financeBillId) {
        FinanceBill bill = financeBillRepository.findByFinanceBillId(financeBillId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, bill.storeId());
        return bill;
    }

    private FinanceInvoice requireOwnedInvoice(String tenantId, String invoiceId) {
        FinanceInvoice invoice = FINANCE_INVOICE_STORAGE.get(invoiceId);
        if (invoice == null) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        requireOwnedStore(tenantId, invoice.storeId());
        return invoice;
    }

    private OrderMain requireOwnedOrder(String tenantId, String orderId) {
        OrderMain order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, order.storeId());
        return order;
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        return store;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new BusinessException("7425", fieldName + " is required", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private String normalizeFilter(String value) {
        String normalized = normalizeOptionalText(value);
        return normalized == null ? null : normalized;
    }

    private record FinanceBillCalculation(
            BigDecimal incomeAmount,
            BigDecimal costAmount,
            BigDecimal grossProfit
    ) {
    }

    public record ReceivableLedgerEntry(
            String receivableLedgerId,
            String receivableNo,
            String storeId,
            String orderId,
            String platformOrderId,
            String customerName,
            BigDecimal receivableAmount,
            BigDecimal collectedAmount,
            BigDecimal outstandingAmount,
            String receivableStatus,
            OffsetDateTime occurredAt
    ) {
    }

    public record RecordCustomerPaymentRequest(
            String orderId,
            String paymentChannel,
            BigDecimal paymentAmount,
            String remark
    ) {
    }

    public record CustomerPaymentRecord(
            String paymentRecordId,
            String paymentNo,
            String storeId,
            String orderId,
            String platformOrderId,
            String customerName,
            String paymentChannel,
            BigDecimal paymentAmount,
            String paymentStatus,
            String operatorId,
            OffsetDateTime receivedAt,
            String remark
    ) {
    }

    public record ArchiveFinanceVoucherRequest(
            String storeId,
            String referenceType,
            String referenceId,
            String voucherType,
            BigDecimal voucherAmount,
            String remark
    ) {
    }

    public record FinanceVoucher(
            String voucherId,
            String voucherNo,
            String storeId,
            String referenceType,
            String referenceId,
            String voucherType,
            BigDecimal voucherAmount,
            String archiveStatus,
            String operatorId,
            OffsetDateTime archivedAt,
            String remark
    ) {
    }

    public record IssueFinanceInvoiceRequest(
            String storeId,
            String referenceType,
            String referenceId,
            String invoiceTitle,
            String invoiceTaxNo,
            BigDecimal invoiceAmount,
            String remark
    ) {
    }

    public record FinanceInvoice(
            String invoiceId,
            String invoiceNo,
            String storeId,
            String referenceType,
            String referenceId,
            String sourceInvoiceId,
            String invoiceTitle,
            String invoiceTaxNo,
            BigDecimal invoiceAmount,
            String invoiceStatus,
            String archiveStatus,
            boolean redFlush,
            String operatorId,
            OffsetDateTime issuedAt,
            OffsetDateTime updatedAt,
            String remark
    ) {
        public FinanceInvoice withArchiveStatus(String archiveStatus, String operatorId, String remark) {
            return new FinanceInvoice(
                    invoiceId,
                    invoiceNo,
                    storeId,
                    referenceType,
                    referenceId,
                    sourceInvoiceId,
                    invoiceTitle,
                    invoiceTaxNo,
                    invoiceAmount,
                    invoiceStatus,
                    archiveStatus,
                    redFlush,
                    operatorId,
                    issuedAt,
                    OffsetDateTime.now(),
                    remark
            );
        }

        public FinanceInvoice withStatus(String invoiceStatus, String operatorId, String remark) {
            return new FinanceInvoice(
                    invoiceId,
                    invoiceNo,
                    storeId,
                    referenceType,
                    referenceId,
                    sourceInvoiceId,
                    invoiceTitle,
                    invoiceTaxNo,
                    invoiceAmount,
                    invoiceStatus,
                    archiveStatus,
                    redFlush,
                    operatorId,
                    issuedAt,
                    OffsetDateTime.now(),
                    remark
            );
        }
    }

    public record CloseFinancePeriodRequest(
            String storeId,
            LocalDate periodStart,
            LocalDate periodEnd,
            String remark
    ) {
    }

    public record SaveErpMasterDataDictionaryRequest(
            String dictionaryType,
            String dictionaryCode,
            String dictionaryName,
            String erpCode,
            String erpName,
            boolean enabled,
            String remark
    ) {
    }

    public record ErpMasterDataDictionaryEntry(
            String dictionaryEntryId,
            String tenantId,
            String dictionaryType,
            String dictionaryCode,
            String dictionaryName,
            String erpCode,
            String erpName,
            boolean enabled,
            String operatorId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String remark
    ) {
    }

    public record SaveErpAccountMappingRequest(
            String storeId,
            String mappingCategory,
            String businessType,
            String businessCode,
            String subjectCode,
            String subjectName,
            String direction,
            boolean enabled,
            String remark
    ) {
    }

    public record ErpAccountMappingEntry(
            String mappingId,
            String tenantId,
            String storeId,
            String mappingCategory,
            String businessType,
            String businessCode,
            String subjectCode,
            String subjectName,
            String direction,
            boolean enabled,
            String operatorId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String remark
    ) {
    }

    public record ErpIntegrationBaselineView(
            String syncMode,
            int availableStoreCount,
            int masterDataDictionaryCount,
            int accountMappingCount,
            List<String> supportedResources,
            OffsetDateTime exportedAt
    ) {
    }

    public record FinancePeriodClosingRecord(
            String closingRecordId,
            String closingNo,
            String storeId,
            LocalDate periodStart,
            LocalDate periodEnd,
            String closingStatus,
            String operatorId,
            int linkedFinanceBillCount,
            int archivedVoucherCount,
            OffsetDateTime closedAt,
            String remark
    ) {
    }

    public record ProfitStatementView(
            String storeId,
            String shopName,
            LocalDate periodStart,
            LocalDate periodEnd,
            int salesOrderCount,
            BigDecimal salesIncomeAmount,
            BigDecimal estimatedCostAmount,
            BigDecimal grossProfitAmount,
            BigDecimal allocatedExpenseAmount,
            BigDecimal netProfitAmount,
            BigDecimal profitMarginRate,
            BigDecimal targetProfitThreshold,
            String thresholdStatus,
            BigDecimal outstandingReceivableAmount,
            List<ExpenseBreakdownView> expenseBreakdown
    ) {
    }

    public record StoreProfitReportView(
            String storeId,
            String shopName,
            LocalDate periodStart,
            LocalDate periodEnd,
            int salesOrderCount,
            BigDecimal salesIncomeAmount,
            BigDecimal grossProfitAmount,
            BigDecimal allocatedExpenseAmount,
            BigDecimal netProfitAmount,
            BigDecimal profitMarginRate,
            BigDecimal targetProfitThreshold,
            String thresholdStatus,
            BigDecimal outstandingReceivableAmount
    ) {
    }

    public record FinanceGeneralLedgerView(
            String storeId,
            String shopName,
            LocalDate periodStart,
            LocalDate periodEnd,
            int financeBillCount,
            int settledFinanceBillCount,
            BigDecimal salesIncomeAmount,
            BigDecimal receivableCollectedAmount,
            BigDecimal outstandingReceivableAmount,
            BigDecimal payableNetAmount,
            BigDecimal settlementAmount,
            BigDecimal archivedVoucherAmount,
            BigDecimal issuedInvoiceAmount,
            BigDecimal redFlushInvoiceAmount,
            int pendingVoucherCount,
            int pendingInvoiceCount,
            String closingStatus
    ) {
    }

    public record FinanceClosingCheckView(
            String storeId,
            String shopName,
            LocalDate periodStart,
            LocalDate periodEnd,
            boolean readyToClose,
            int blockingIssueCount,
            List<FinanceClosingCheckItemView> checkItems
    ) {
    }

    public record FinanceClosingCheckItemView(
            String checkCode,
            String checkName,
            String checkStatus,
            int pendingCount,
            String detail
    ) {
    }

    public record ExpenseBreakdownView(
            String expenseType,
            BigDecimal expenseAmount,
            int allocationCount
    ) {
    }

    public record GenerateFinanceBillRequest(
            String storeId,
            String billType,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
    }

    public record FinanceBillDetailView(
            FinanceBill bill,
            List<SettlementRecord> settlements,
            String splitStatus,
            String invoiceCheckStatus
    ) {
    }

    public record FinanceBillReconcileView(
            String financeBillId,
            String billStatus,
            int discrepancyCount,
            String invoiceCheckStatus
    ) {
    }

    public record SettleFinanceBillRequest(
            String settlementType
    ) {
    }

    private record ProfitMetrics(
            int salesOrderCount,
            BigDecimal salesIncomeAmount,
            BigDecimal estimatedCostAmount,
            BigDecimal grossProfitAmount,
            BigDecimal allocatedExpenseAmount,
            BigDecimal netProfitAmount,
            BigDecimal profitMarginRate,
            String thresholdStatus,
            BigDecimal outstandingReceivableAmount,
            List<ExpenseBreakdownView> expenseBreakdown
    ) {
    }

    private record StoreProfitReportEnvelope(
            Store store,
            ProfitMetrics metrics
    ) {
    }

    private record FinanceGeneralLedgerEnvelope(
            Store store,
            GeneralLedgerMetrics metrics
    ) {
    }

    private record GeneralLedgerMetrics(
            int financeBillCount,
            int settledFinanceBillCount,
            BigDecimal salesIncomeAmount,
            BigDecimal receivableCollectedAmount,
            BigDecimal outstandingReceivableAmount,
            int outstandingReceivableOrderCount,
            BigDecimal payableNetAmount,
            BigDecimal settlementAmount,
            BigDecimal archivedVoucherAmount,
            BigDecimal issuedInvoiceAmount,
            BigDecimal redFlushInvoiceAmount,
            int pendingVoucherCount,
            int pendingInvoiceCount,
            int pendingSupplierDiscrepancyCount
    ) {
        private int pendingFinanceBillCount() {
            return Math.max(financeBillCount - settledFinanceBillCount, 0);
        }
    }

    private static final class ExpenseSummaryAccumulator {
        private final String expenseType;
        private BigDecimal expenseAmount = BigDecimal.ZERO;
        private int allocationCount = 0;

        private ExpenseSummaryAccumulator(String expenseType) {
            this.expenseType = expenseType;
        }

        private void add(BigDecimal amount) {
            expenseAmount = expenseAmount.add(amount);
            allocationCount++;
        }

        private ExpenseBreakdownView toView() {
            return new ExpenseBreakdownView(expenseType, expenseAmount, allocationCount);
        }
    }
}
