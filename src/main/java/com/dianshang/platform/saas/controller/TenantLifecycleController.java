package com.dianshang.platform.saas.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.saas.application.SaasTenantService.ComplianceAcceptance;
import com.dianshang.platform.saas.application.SaasTenantService.ComplianceAcceptanceRequest;
import com.dianshang.platform.saas.application.SaasTenantService.ComplianceDocument;
import com.dianshang.platform.saas.application.SaasTenantService.InvoiceCreateRequest;
import com.dianshang.platform.saas.application.SaasTenantService.PlanChangeRequest;
import com.dianshang.platform.saas.application.SaasTenantService.SeatPurchaseRequest;
import com.dianshang.platform.saas.application.SaasTenantService.SubscriptionActionRequest;
import com.dianshang.platform.saas.application.SaasTenantService.SubscriptionRenewRequest;
import com.dianshang.platform.saas.application.SaasTenantService.TenantCleanupPlanRequest;
import com.dianshang.platform.saas.application.SaasTenantService.TenantCleanupTaskView;
import com.dianshang.platform.saas.application.SaasTenantService.TenantDataExportDownload;
import com.dianshang.platform.saas.application.SaasTenantService.TenantDataExportRequest;
import com.dianshang.platform.saas.application.SaasTenantService.TenantDataExportTaskSummary;
import com.dianshang.platform.saas.dto.QuotaConsumeRequest;
import com.dianshang.platform.saas.dto.RegisterTenantRequest;
import com.dianshang.platform.saas.dto.RegisterTenantResponse;
import com.dianshang.platform.saas.model.SubscriptionPlan;
import com.dianshang.platform.saas.model.TenantProfile;
import com.dianshang.platform.saas.model.TenantSubscription;
import com.dianshang.platform.saas.model.UsageQuota;
import com.dianshang.platform.saas.model.BillingOrder;
import com.dianshang.platform.saas.model.InvoiceRequest;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequirePlatformRoles;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TenantLifecycleController {

    private final SaasTenantService saasTenantService;

    public TenantLifecycleController(SaasTenantService saasTenantService) {
        this.saasTenantService = saasTenantService;
    }

    @PostMapping("/api/tenants/register")
    public ApiResponse<RegisterTenantResponse> register(@Valid @RequestBody RegisterTenantRequest request) {
        return ApiResponse.success(saasTenantService.register(request), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/start-trial")
    @RequirePlatformRoles({"platform-ops", "platform-admin"})
    public ApiResponse<TenantProfile> startTrial(@PathVariable String id) {
        return ApiResponse.success(saasTenantService.startTrial(id), TraceIdHolder.get());
    }

    @GetMapping("/api/subscription-plans")
    public ApiResponse<List<SubscriptionPlan>> listSubscriptionPlans() {
        return ApiResponse.success(saasTenantService.getSubscriptionPlans(), TraceIdHolder.get());
    }

    @GetMapping("/api/tenants/{id}/subscription")
    @RequireTenantPermission(AuthPermissionCodes.SUBSCRIPTION_MANAGE)
    public ApiResponse<TenantSubscription> getSubscription(@PathVariable String id) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.getSubscription(id), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/suspend")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE)
    public ApiResponse<TenantProfile> suspend(@PathVariable String id) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.suspend(id), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/resume")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE)
    public ApiResponse<TenantProfile> resume(@PathVariable String id) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.resume(id), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/offboarding/request")
    @RequireTenantPermission(value = AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE, requireSecondaryConfirmation = true)
    public ApiResponse<TenantProfile> requestOffboarding(@PathVariable String id) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.requestOffboarding(id), TraceIdHolder.get());
    }

    @GetMapping("/api/tenants/{id}/cleanup-tasks")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE)
    public ApiResponse<List<TenantCleanupTaskView>> listCleanupTasks(@PathVariable String id) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.listTenantCleanupTasks(id), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/cleanup-tasks/plan")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE)
    public ApiResponse<TenantCleanupTaskView> planCleanupTask(@PathVariable String id,
                                                              @RequestBody TenantCleanupPlanRequest request) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.createTenantCleanupTask(id, request), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/cleanup-tasks/{taskId}/review")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE)
    public ApiResponse<TenantCleanupTaskView> reviewCleanupTask(@PathVariable String id,
                                                                @PathVariable String taskId) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.reviewTenantCleanupTask(id, taskId), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/cleanup-tasks/{taskId}/execute")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_LIFECYCLE_MANAGE)
    public ApiResponse<TenantCleanupTaskView> executeCleanupTask(@PathVariable String id,
                                                                 @PathVariable String taskId) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.executeTenantCleanupTask(id, taskId), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/subscription/subscribe")
    @RequireTenantPermission(AuthPermissionCodes.SUBSCRIPTION_MANAGE)
    public ApiResponse<TenantSubscription> subscribe(@PathVariable String id,
                                                     @Valid @RequestBody SubscriptionActionRequest request) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.subscribe(id, request), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/subscription/renew")
    @RequireTenantPermission(AuthPermissionCodes.SUBSCRIPTION_MANAGE)
    public ApiResponse<TenantSubscription> renew(@PathVariable String id,
                                                 @Valid @RequestBody SubscriptionRenewRequest request) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.renew(id, request), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/subscription/upgrade")
    @RequireTenantPermission(AuthPermissionCodes.SUBSCRIPTION_MANAGE)
    public ApiResponse<TenantSubscription> upgrade(@PathVariable String id,
                                                   @Valid @RequestBody PlanChangeRequest request) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.upgrade(id, request), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/subscription/downgrade")
    @RequireTenantPermission(AuthPermissionCodes.SUBSCRIPTION_MANAGE)
    public ApiResponse<TenantSubscription> downgrade(@PathVariable String id,
                                                     @Valid @RequestBody PlanChangeRequest request) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.downgrade(id, request), TraceIdHolder.get());
    }

    @GetMapping("/api/tenants/{id}/quotas")
    @RequireTenantPermission(AuthPermissionCodes.SUBSCRIPTION_MANAGE)
    public ApiResponse<List<UsageQuota>> getQuotas(@PathVariable String id) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.getQuotas(id), TraceIdHolder.get());
    }

    @GetMapping("/api/tenants/{id}/billing-orders")
    @RequireTenantPermission(AuthPermissionCodes.SUBSCRIPTION_MANAGE)
    public ApiResponse<List<BillingOrder>> getBillingOrders(@PathVariable String id) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.getBillingOrders(id), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/billing-orders/{billingOrderId}/settle")
    @RequireTenantPermission(AuthPermissionCodes.SUBSCRIPTION_MANAGE)
    public ApiResponse<BillingOrder> settleBillingOrder(@PathVariable String id,
                                                        @PathVariable String billingOrderId) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.settleBillingOrder(id, billingOrderId), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/quotas/consume")
    @RequireTenantPermission(AuthPermissionCodes.SUBSCRIPTION_MANAGE)
    public ApiResponse<UsageQuota> consumeQuota(@PathVariable String id,
                                                @Valid @RequestBody QuotaConsumeRequest request) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.consumeQuota(id, request), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/seats/purchase")
    @RequireTenantPermission(AuthPermissionCodes.SUBSCRIPTION_MANAGE)
    public ApiResponse<TenantSubscription> purchaseSeats(@PathVariable String id,
                                                         @Valid @RequestBody SeatPurchaseRequest request) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.purchaseSeats(id, request), TraceIdHolder.get());
    }

    @PostMapping("/api/tenants/{id}/invoice-requests")
    @RequireTenantPermission(AuthPermissionCodes.SUBSCRIPTION_MANAGE)
    public ApiResponse<InvoiceRequest> createInvoiceRequest(@PathVariable String id,
                                                            @Valid @RequestBody InvoiceCreateRequest request) {
        TenantAccessSupport.assertSameTenant(id);
        return ApiResponse.success(saasTenantService.createInvoiceRequest(id, request), TraceIdHolder.get());
    }

    @GetMapping("/api/compliance/privacy-policy")
    public ApiResponse<ComplianceDocument> getPrivacyPolicy() {
        return ApiResponse.success(saasTenantService.getPrivacyPolicy(), TraceIdHolder.get());
    }

    @GetMapping("/api/compliance/user-agreement")
    public ApiResponse<ComplianceDocument> getUserAgreement() {
        return ApiResponse.success(saasTenantService.getUserAgreement(), TraceIdHolder.get());
    }

    @GetMapping("/api/compliance/acceptances")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_COMPLIANCE_READ)
    public ApiResponse<List<ComplianceAcceptance>> listComplianceAcceptances() {
        return ApiResponse.success(
                saasTenantService.listComplianceAcceptances(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/compliance/acceptances")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_COMPLIANCE_ACCEPT)
    public ApiResponse<List<ComplianceAcceptance>> acceptComplianceDocuments(
            @RequestBody ComplianceAcceptanceRequest request
    ) {
        return ApiResponse.success(
                saasTenantService.acceptComplianceDocuments(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/tenant/data-exports")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE)
    public ApiResponse<List<TenantDataExportTaskSummary>> listTenantDataExports() {
        return ApiResponse.success(
                saasTenantService.listTenantDataExports(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/tenant/data-exports")
    @RequireTenantPermission(value = AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE, requireSecondaryConfirmation = true)
    public ApiResponse<TenantDataExportTaskSummary> createTenantDataExport(@RequestBody TenantDataExportRequest request) {
        return ApiResponse.success(
                saasTenantService.createTenantDataExport(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/tenant/data-exports/{taskId}/download")
    @RequireTenantPermission(AuthPermissionCodes.TENANT_DATA_EXPORT_MANAGE)
    public ResponseEntity<String> downloadTenantDataExport(@PathVariable String taskId) {
        TenantDataExportDownload download = saasTenantService.downloadTenantDataExport(
                TenantAccessSupport.requiredTenantId(),
                taskId
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.fileName() + "\"")
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }
}
