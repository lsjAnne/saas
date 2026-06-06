package com.dianshang.platform.finance.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.finance.application.FinanceService;
import com.dianshang.platform.finance.application.FinanceService.FinanceBillDetailView;
import com.dianshang.platform.finance.application.FinanceService.FinanceBillReconcileView;
import com.dianshang.platform.finance.application.FinanceService.FinanceInvoice;
import com.dianshang.platform.finance.application.FinanceService.FinancePeriodClosingRecord;
import com.dianshang.platform.finance.application.FinanceService.FinanceVoucher;
import com.dianshang.platform.finance.application.FinanceService.GenerateFinanceBillRequest;
import com.dianshang.platform.finance.application.FinanceService.RecordCustomerPaymentRequest;
import com.dianshang.platform.finance.application.FinanceService.SettleFinanceBillRequest;
import com.dianshang.platform.finance.application.FinanceService.CustomerPaymentRecord;
import com.dianshang.platform.finance.application.FinanceService.ReceivableLedgerEntry;
import com.dianshang.platform.finance.model.FinanceBill;
import com.dianshang.platform.finance.model.SettlementRecord;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.FINANCE_MANAGE)
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/api/finance-bills")
    public ApiResponse<List<FinanceBill>> listBills() {
        return ApiResponse.success(
                financeService.listFinanceBills(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/receivable-ledgers")
    public ApiResponse<List<ReceivableLedgerEntry>> listReceivableLedgers() {
        return ApiResponse.success(
                financeService.listReceivableLedgers(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/customer-payments")
    public ApiResponse<List<CustomerPaymentRecord>> listCustomerPayments() {
        return ApiResponse.success(
                financeService.listCustomerPayments(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/erp-master-data-dictionaries")
    public ApiResponse<List<FinanceService.ErpMasterDataDictionaryEntry>> listErpMasterDataDictionaries(@RequestParam(required = false) String dictionaryType) {
        return ApiResponse.success(
                financeService.listErpMasterDataDictionaries(TenantAccessSupport.requiredTenantId(), dictionaryType),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/erp-master-data-dictionaries")
    public ApiResponse<FinanceService.ErpMasterDataDictionaryEntry> saveErpMasterDataDictionary(@Valid @RequestBody SaveErpMasterDataDictionaryPayload request) {
        return ApiResponse.success(
                financeService.saveErpMasterDataDictionary(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/erp-account-mappings")
    public ApiResponse<List<FinanceService.ErpAccountMappingEntry>> listErpAccountMappings(@RequestParam(required = false) String mappingCategory,
                                                                                            @RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                financeService.listErpAccountMappings(TenantAccessSupport.requiredTenantId(), mappingCategory, storeId),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/erp-account-mappings")
    public ApiResponse<FinanceService.ErpAccountMappingEntry> saveErpAccountMapping(@Valid @RequestBody SaveErpAccountMappingPayload request) {
        return ApiResponse.success(
                financeService.saveErpAccountMapping(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/profit-statements")
    public ApiResponse<FinanceService.ProfitStatementView> getProfitStatement(@RequestParam String storeId,
                                                                              @RequestParam LocalDate periodStart,
                                                                              @RequestParam LocalDate periodEnd) {
        return ApiResponse.success(
                financeService.getProfitStatement(
                        TenantAccessSupport.requiredTenantId(),
                        storeId,
                        periodStart,
                        periodEnd
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/store-profit-reports")
    public ApiResponse<List<FinanceService.StoreProfitReportView>> listStoreProfitReports(@RequestParam LocalDate periodStart,
                                                                                           @RequestParam LocalDate periodEnd) {
        return ApiResponse.success(
                financeService.listStoreProfitReports(
                        TenantAccessSupport.requiredTenantId(),
                        periodStart,
                        periodEnd
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/finance-general-ledgers")
    public ApiResponse<List<FinanceService.FinanceGeneralLedgerView>> listFinanceGeneralLedgers(@RequestParam LocalDate periodStart,
                                                                                                @RequestParam LocalDate periodEnd) {
        return ApiResponse.success(
                financeService.listFinanceGeneralLedgers(
                        TenantAccessSupport.requiredTenantId(),
                        periodStart,
                        periodEnd
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/finance-closing-checks")
    public ApiResponse<FinanceService.FinanceClosingCheckView> getFinanceClosingCheck(@RequestParam String storeId,
                                                                                      @RequestParam LocalDate periodStart,
                                                                                      @RequestParam LocalDate periodEnd) {
        return ApiResponse.success(
                financeService.getFinanceClosingCheck(
                        TenantAccessSupport.requiredTenantId(),
                        storeId,
                        periodStart,
                        periodEnd
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/finance-bills/generate")
    public ApiResponse<FinanceBill> generateBill(@Valid @RequestBody GenerateFinanceBillPayload request) {
        return ApiResponse.success(
                financeService.generateBill(TenantAccessSupport.requiredTenantId(), request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/finance-bills/{id}")
    public ApiResponse<FinanceBillDetailView> getBill(@PathVariable String id) {
        return ApiResponse.success(
                financeService.getFinanceBill(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/finance-bills/{id}/reconcile")
    public ApiResponse<FinanceBillReconcileView> reconcile(@PathVariable String id) {
        return ApiResponse.success(
                financeService.reconcile(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/finance-bills/{id}/settle")
    public ApiResponse<SettlementRecord> settle(@PathVariable String id,
                                                @RequestBody(required = false) SettleFinanceBillPayload request) {
        SettleFinanceBillPayload payload = request == null ? new SettleFinanceBillPayload(null) : request;
        return ApiResponse.success(
                financeService.settle(TenantAccessSupport.requiredTenantId(), id, payload.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/customer-payments")
    public ApiResponse<CustomerPaymentRecord> recordCustomerPayment(@Valid @RequestBody RecordCustomerPaymentPayload request) {
        return ApiResponse.success(
                financeService.recordCustomerPayment(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/finance-vouchers")
    public ApiResponse<List<FinanceVoucher>> listFinanceVouchers() {
        return ApiResponse.success(
                financeService.listFinanceVouchers(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/finance-invoices")
    public ApiResponse<List<FinanceInvoice>> listFinanceInvoices() {
        return ApiResponse.success(
                financeService.listFinanceInvoices(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/finance-invoices/issue")
    public ApiResponse<FinanceInvoice> issueFinanceInvoice(@Valid @RequestBody IssueFinanceInvoicePayload request) {
        return ApiResponse.success(
                financeService.issueFinanceInvoice(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/finance-invoices/{id}/archive")
    public ApiResponse<FinanceInvoice> archiveFinanceInvoice(@PathVariable String id,
                                                             @RequestBody(required = false) UpdateFinanceInvoicePayload request) {
        UpdateFinanceInvoicePayload payload = request == null ? new UpdateFinanceInvoicePayload(null) : request;
        return ApiResponse.success(
                financeService.archiveFinanceInvoice(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id,
                        payload.remark()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/finance-invoices/{id}/void")
    public ApiResponse<FinanceInvoice> voidFinanceInvoice(@PathVariable String id,
                                                          @RequestBody(required = false) UpdateFinanceInvoicePayload request) {
        UpdateFinanceInvoicePayload payload = request == null ? new UpdateFinanceInvoicePayload(null) : request;
        return ApiResponse.success(
                financeService.voidFinanceInvoice(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id,
                        payload.remark()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/finance-invoices/{id}/red-flush")
    public ApiResponse<FinanceInvoice> redFlushFinanceInvoice(@PathVariable String id,
                                                              @RequestBody(required = false) UpdateFinanceInvoicePayload request) {
        UpdateFinanceInvoicePayload payload = request == null ? new UpdateFinanceInvoicePayload(null) : request;
        return ApiResponse.success(
                financeService.redFlushFinanceInvoice(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id,
                        payload.remark()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/finance-vouchers/archive")
    public ApiResponse<FinanceVoucher> archiveFinanceVoucher(@Valid @RequestBody ArchiveFinanceVoucherPayload request) {
        return ApiResponse.success(
                financeService.archiveFinanceVoucher(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/finance-period-closings")
    public ApiResponse<List<FinancePeriodClosingRecord>> listFinancePeriodClosings() {
        return ApiResponse.success(
                financeService.listFinancePeriodClosings(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/finance-period-closings/close")
    public ApiResponse<FinancePeriodClosingRecord> closeFinancePeriod(@Valid @RequestBody CloseFinancePeriodPayload request) {
        return ApiResponse.success(
                financeService.closeFinancePeriod(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }
}

record GenerateFinanceBillPayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "billType is required")
        String billType,
        @NotNull(message = "periodStart is required")
        LocalDate periodStart,
        @NotNull(message = "periodEnd is required")
        LocalDate periodEnd
) {
    GenerateFinanceBillRequest toCommand() {
        return new GenerateFinanceBillRequest(storeId, billType, periodStart, periodEnd);
    }
}

record SettleFinanceBillPayload(
        String settlementType
) {
    SettleFinanceBillRequest toCommand() {
        return new SettleFinanceBillRequest(settlementType);
    }
}

record RecordCustomerPaymentPayload(
        @NotBlank(message = "orderId is required")
        String orderId,
        String paymentChannel,
        @NotNull(message = "paymentAmount is required")
        java.math.BigDecimal paymentAmount,
        String remark
) {
    RecordCustomerPaymentRequest toCommand() {
        return new RecordCustomerPaymentRequest(orderId, paymentChannel, paymentAmount, remark);
    }
}

record SaveErpMasterDataDictionaryPayload(
        @NotBlank(message = "dictionaryType is required")
        String dictionaryType,
        @NotBlank(message = "dictionaryCode is required")
        String dictionaryCode,
        @NotBlank(message = "dictionaryName is required")
        String dictionaryName,
        @NotBlank(message = "erpCode is required")
        String erpCode,
        @NotBlank(message = "erpName is required")
        String erpName,
        @NotNull(message = "enabled is required")
        Boolean enabled,
        String remark
) {
    FinanceService.SaveErpMasterDataDictionaryRequest toCommand() {
        return new FinanceService.SaveErpMasterDataDictionaryRequest(
                dictionaryType,
                dictionaryCode,
                dictionaryName,
                erpCode,
                erpName,
                enabled,
                remark
        );
    }
}

record SaveErpAccountMappingPayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "mappingCategory is required")
        String mappingCategory,
        @NotBlank(message = "businessType is required")
        String businessType,
        @NotBlank(message = "businessCode is required")
        String businessCode,
        @NotBlank(message = "subjectCode is required")
        String subjectCode,
        @NotBlank(message = "subjectName is required")
        String subjectName,
        @NotBlank(message = "direction is required")
        String direction,
        @NotNull(message = "enabled is required")
        Boolean enabled,
        String remark
) {
    FinanceService.SaveErpAccountMappingRequest toCommand() {
        return new FinanceService.SaveErpAccountMappingRequest(
                storeId,
                mappingCategory,
                businessType,
                businessCode,
                subjectCode,
                subjectName,
                direction,
                enabled,
                remark
        );
    }
}

record ArchiveFinanceVoucherPayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "referenceType is required")
        String referenceType,
        @NotBlank(message = "referenceId is required")
        String referenceId,
        @NotBlank(message = "voucherType is required")
        String voucherType,
        @NotNull(message = "voucherAmount is required")
        java.math.BigDecimal voucherAmount,
        String remark
) {
    FinanceService.ArchiveFinanceVoucherRequest toCommand() {
        return new FinanceService.ArchiveFinanceVoucherRequest(
                storeId,
                referenceType,
                referenceId,
                voucherType,
                voucherAmount,
                remark
        );
    }
}

record IssueFinanceInvoicePayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "referenceType is required")
        String referenceType,
        @NotBlank(message = "referenceId is required")
        String referenceId,
        @NotBlank(message = "invoiceTitle is required")
        String invoiceTitle,
        @NotBlank(message = "invoiceTaxNo is required")
        String invoiceTaxNo,
        @NotNull(message = "invoiceAmount is required")
        java.math.BigDecimal invoiceAmount,
        String remark
) {
    FinanceService.IssueFinanceInvoiceRequest toCommand() {
        return new FinanceService.IssueFinanceInvoiceRequest(
                storeId,
                referenceType,
                referenceId,
                invoiceTitle,
                invoiceTaxNo,
                invoiceAmount,
                remark
        );
    }
}

record UpdateFinanceInvoicePayload(
        String remark
) {
}

record CloseFinancePeriodPayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotNull(message = "periodStart is required")
        LocalDate periodStart,
        @NotNull(message = "periodEnd is required")
        LocalDate periodEnd,
        String remark
) {
    FinanceService.CloseFinancePeriodRequest toCommand() {
        return new FinanceService.CloseFinancePeriodRequest(storeId, periodStart, periodEnd, remark);
    }
}
