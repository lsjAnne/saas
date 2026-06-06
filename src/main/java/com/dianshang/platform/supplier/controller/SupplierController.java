package com.dianshang.platform.supplier.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.supplier.application.SupplierService;
import com.dianshang.platform.supplier.application.SupplierService.SaveSupplierAdmissionReviewRequest;
import com.dianshang.platform.supplier.application.SupplierService.SaveSupplierDeliveryAppointmentRequest;
import com.dianshang.platform.supplier.application.SupplierService.SaveSupplierInquiryRequest;
import com.dianshang.platform.supplier.application.SupplierService.SaveSupplierRiskEventRequest;
import com.dianshang.platform.supplier.application.SupplierService.SaveSupplierScorecardRequest;
import com.dianshang.platform.supplier.application.SupplierService.SaveSupplierSettlementStatementRequest;
import com.dianshang.platform.supplier.application.SupplierService.SupplierAdmissionReviewView;
import com.dianshang.platform.supplier.application.SupplierService.SupplierDeliveryAppointmentView;
import com.dianshang.platform.supplier.application.SupplierService.SupplierInquiryQuoteView;
import com.dianshang.platform.supplier.application.SupplierService.SupplierInquiryView;
import com.dianshang.platform.supplier.application.SupplierService.SupplierRiskEventView;
import com.dianshang.platform.supplier.application.SupplierService.SupplierScorecardView;
import com.dianshang.platform.supplier.application.SupplierService.SupplierSettlementStatementView;
import com.dianshang.platform.supplier.application.SupplierService.SupplierSrmLinkageView;
import com.dianshang.platform.supplier.dto.CreateSupplierRequest;
import com.dianshang.platform.supplier.dto.UpdateSupplierRequest;
import com.dianshang.platform.supplier.model.Supplier;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.SUPPLIER_MANAGE)
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping("/api/suppliers")
    public ApiResponse<List<Supplier>> list() {
        return ApiResponse.success(
                supplierService.listSuppliers(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/suppliers")
    public ApiResponse<Supplier> create(@Valid @RequestBody CreateSupplierRequest request) {
        return ApiResponse.success(
                supplierService.createSupplier(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/suppliers/{id}")
    public ApiResponse<Supplier> detail(@PathVariable String id) {
        return ApiResponse.success(
                supplierService.getSupplier(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/suppliers/{id}")
    public ApiResponse<Supplier> update(@PathVariable String id,
                                        @Valid @RequestBody UpdateSupplierRequest request) {
        return ApiResponse.success(
                supplierService.updateSupplier(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/suppliers/{id}/set-primary")
    public ApiResponse<Supplier> setPrimary(@PathVariable String id) {
        return ApiResponse.success(
                supplierService.setPrimary(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/suppliers/{id}/set-backup")
    public ApiResponse<Supplier> setBackup(@PathVariable String id) {
        return ApiResponse.success(
                supplierService.setBackup(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/suppliers/{id}/admission-review")
    public ApiResponse<SupplierAdmissionReviewView> reviewAdmission(@PathVariable String id,
                                                                    @Valid @RequestBody SaveSupplierAdmissionReviewPayload request) {
        return ApiResponse.success(
                supplierService.reviewAdmission(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/suppliers/{id}/scorecards")
    public ApiResponse<SupplierScorecardView> saveScorecard(@PathVariable String id,
                                                            @Valid @RequestBody SaveSupplierScorecardPayload request) {
        return ApiResponse.success(
                supplierService.saveScorecard(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/suppliers/{id}/delivery-appointments")
    public ApiResponse<SupplierDeliveryAppointmentView> createDeliveryAppointment(@PathVariable String id,
                                                                                  @Valid @RequestBody SaveSupplierDeliveryAppointmentPayload request) {
        return ApiResponse.success(
                supplierService.createDeliveryAppointment(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/suppliers/{id}/risk-events")
    public ApiResponse<SupplierRiskEventView> createRiskEvent(@PathVariable String id,
                                                              @Valid @RequestBody SaveSupplierRiskEventPayload request) {
        return ApiResponse.success(
                supplierService.createRiskEvent(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/suppliers/{id}/srm-linkage")
    public ApiResponse<SupplierSrmLinkageView> getSrmLinkage(@PathVariable String id) {
        return ApiResponse.success(
                supplierService.getSrmLinkage(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/supplier-inquiries")
    public ApiResponse<SupplierInquiryView> createInquiry(@Valid @RequestBody SaveSupplierInquiryPayload request) {
        return ApiResponse.success(
                supplierService.createInquiry(TenantAccessSupport.requiredTenantId(), request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/supplier-settlement-statements")
    public ApiResponse<SupplierSettlementStatementView> createSettlementStatement(
            @Valid @RequestBody SaveSupplierSettlementStatementPayload request) {
        return ApiResponse.success(
                supplierService.createSettlementStatement(TenantAccessSupport.requiredTenantId(), request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/supplier-settlement-statements")
    public ApiResponse<List<SupplierSettlementStatementView>> listSettlementStatements(@RequestParam String storeId) {
        return ApiResponse.success(
                supplierService.listSettlementStatements(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }
}

record SaveSupplierAdmissionReviewPayload(
        @NotEmpty(message = "qualificationDocs are required")
        List<String> qualificationDocs,
        @NotBlank(message = "decision is required")
        String decision,
        String remark
) {
    SaveSupplierAdmissionReviewRequest toCommand() {
        return new SaveSupplierAdmissionReviewRequest(qualificationDocs, decision, remark);
    }
}

record SaveSupplierScorecardPayload(
        @NotNull(message = "deliveryScore is required")
        Integer deliveryScore,
        @NotNull(message = "fulfillmentScore is required")
        Integer fulfillmentScore,
        @NotNull(message = "qualityScore is required")
        Integer qualityScore
) {
    SaveSupplierScorecardRequest toCommand() {
        return new SaveSupplierScorecardRequest(deliveryScore, fulfillmentScore, qualityScore);
    }
}

record SaveSupplierInquiryPayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "productId is required")
        String productId,
        @NotBlank(message = "inquiryTitle is required")
        String inquiryTitle,
        @NotNull(message = "targetQty is required")
        @Positive(message = "targetQty must be greater than 0")
        Integer targetQty,
        @NotEmpty(message = "quotes are required")
        List<SaveSupplierInquiryQuotePayload> quotes
) {
    SaveSupplierInquiryRequest toCommand() {
        return new SaveSupplierInquiryRequest(
                storeId,
                productId,
                inquiryTitle,
                targetQty,
                quotes.stream()
                        .map(item -> new SupplierInquiryQuoteView(item.supplierId(), item.quotedUnitPrice(), item.deliveryDays()))
                        .toList()
        );
    }
}

record SaveSupplierInquiryQuotePayload(
        @NotBlank(message = "supplierId is required")
        String supplierId,
        @NotNull(message = "quotedUnitPrice is required")
        BigDecimal quotedUnitPrice,
        @NotNull(message = "deliveryDays is required")
        @Positive(message = "deliveryDays must be greater than 0")
        Integer deliveryDays
) {
}

record SaveSupplierDeliveryAppointmentPayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "purchaseReference is required")
        String purchaseReference,
        @NotNull(message = "appointmentDate is required")
        LocalDate appointmentDate,
        @NotNull(message = "plannedQty is required")
        @Positive(message = "plannedQty must be greater than 0")
        Integer plannedQty,
        String remark
) {
    SaveSupplierDeliveryAppointmentRequest toCommand() {
        return new SaveSupplierDeliveryAppointmentRequest(storeId, purchaseReference, appointmentDate, plannedQty, remark);
    }
}

record SaveSupplierSettlementStatementPayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "supplierId is required")
        String supplierId,
        @NotBlank(message = "statementPeriod is required")
        String statementPeriod,
        @NotNull(message = "accountPeriodDays is required")
        @Positive(message = "accountPeriodDays must be greater than 0")
        Integer accountPeriodDays,
        @NotNull(message = "payableAmount is required")
        BigDecimal payableAmount,
        @NotNull(message = "dueDate is required")
        LocalDate dueDate
) {
    SaveSupplierSettlementStatementRequest toCommand() {
        return new SaveSupplierSettlementStatementRequest(
                storeId,
                supplierId,
                statementPeriod,
                accountPeriodDays,
                payableAmount,
                dueDate
        );
    }
}

record SaveSupplierRiskEventPayload(
        @NotBlank(message = "riskType is required")
        String riskType,
        @NotBlank(message = "severity is required")
        String severity,
        String remark
) {
    SaveSupplierRiskEventRequest toCommand() {
        return new SaveSupplierRiskEventRequest(riskType, severity, remark);
    }
}
