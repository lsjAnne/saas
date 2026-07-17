package backend.saas.admin.controller;

import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.saas.admin.dto.AdminTenantOverview;
import backend.saas.admin.dto.FeatureToggleUpdateRequest;
import backend.saas.application.SaasTenantService.AdminComplianceAcceptanceView;
import backend.saas.application.SaasTenantService.AdminComplianceDocumentView;
import backend.saas.application.SaasTenantService.ComplianceDocument;
import backend.saas.application.SaasTenantService.ComplianceDocumentPublishRequest;
import backend.saas.application.SaasTenantService.DeliveryReadinessView;
import backend.saas.application.SaasTenantService.ObservabilityReadinessView;
import backend.saas.application.SaasTenantService.PlanChangeRequest;
import backend.saas.application.SaasTenantService.ReleaseReadinessView;
import backend.saas.application.SaasTenantService.SeatPurchaseRequest;
import backend.saas.application.SaasTenantService;
import backend.saas.application.SaasTenantService.SubscriptionActionRequest;
import backend.saas.application.SaasTenantService.SubscriptionAutomationSummary;
import backend.saas.model.TenantProfile;
import backend.saas.model.TenantSubscription;
import backend.auth.security.RequirePlatformRoles;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tenants")
@RequirePlatformRoles({"platform-ops", "platform-admin"})
public class AdminTenantController {

    private final SaasTenantService saasTenantService;

    public AdminTenantController(SaasTenantService saasTenantService) {
        this.saasTenantService = saasTenantService;
    }

    @GetMapping
    public ApiResponse<List<AdminTenantOverview>> list() {
        return ApiResponse.success(saasTenantService.listAdminTenantOverviews(), TraceIdHolder.get());
    }

    @PutMapping("/{id}/feature-toggles")
    public ApiResponse<TenantProfile> updateFeatureToggles(@PathVariable String id,
                                                           @Valid @RequestBody FeatureToggleUpdateRequest request) {
        return ApiResponse.success(saasTenantService.updateFeatureToggles(id, request.featureFlags()), TraceIdHolder.get());
    }

    @PostMapping("/{id}/suspend")
    public ApiResponse<TenantProfile> suspend(@PathVariable String id) {
        return ApiResponse.success(saasTenantService.suspend(id), TraceIdHolder.get());
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<TenantProfile> resume(@PathVariable String id) {
        return ApiResponse.success(saasTenantService.resume(id), TraceIdHolder.get());
    }

    @PostMapping("/{id}/subscription/subscribe")
    public ApiResponse<TenantSubscription> subscribe(@PathVariable String id,
                                                     @Valid @RequestBody SubscriptionActionRequest request) {
        return ApiResponse.success(saasTenantService.subscribe(id, request), TraceIdHolder.get());
    }

    @PostMapping("/{id}/subscription/upgrade")
    public ApiResponse<TenantSubscription> upgrade(@PathVariable String id,
                                                   @Valid @RequestBody PlanChangeRequest request) {
        return ApiResponse.success(saasTenantService.upgrade(id, request), TraceIdHolder.get());
    }

    @PostMapping("/{id}/subscription/downgrade")
    public ApiResponse<TenantSubscription> downgrade(@PathVariable String id,
                                                     @Valid @RequestBody PlanChangeRequest request) {
        return ApiResponse.success(saasTenantService.downgrade(id, request), TraceIdHolder.get());
    }

    @PostMapping("/{id}/seats/purchase")
    public ApiResponse<TenantSubscription> purchaseSeats(@PathVariable String id,
                                                         @Valid @RequestBody SeatPurchaseRequest request) {
        return ApiResponse.success(saasTenantService.purchaseSeats(id, request), TraceIdHolder.get());
    }

    @PostMapping("/subscription-automation/reconcile")
    public ApiResponse<SubscriptionAutomationSummary> reconcileSubscriptionAutomation() {
        return ApiResponse.success(saasTenantService.reconcileSubscriptionAutomation(), TraceIdHolder.get());
    }

    @GetMapping("/{id}/release-readiness")
    public ApiResponse<ReleaseReadinessView> getReleaseReadiness(@PathVariable String id) {
        return ApiResponse.success(saasTenantService.getReleaseReadiness(id), TraceIdHolder.get());
    }

    @GetMapping("/{id}/delivery-readiness")
    public ApiResponse<DeliveryReadinessView> getDeliveryReadiness(@PathVariable String id) {
        return ApiResponse.success(saasTenantService.getDeliveryReadiness(id), TraceIdHolder.get());
    }

    @GetMapping("/{id}/observability-readiness")
    public ApiResponse<ObservabilityReadinessView> getObservabilityReadiness(@PathVariable String id) {
        return ApiResponse.success(saasTenantService.getObservabilityReadiness(id), TraceIdHolder.get());
    }

    @GetMapping("/compliance/documents")
    public ApiResponse<List<AdminComplianceDocumentView>> listComplianceDocuments() {
        return ApiResponse.success(saasTenantService.listAdminComplianceDocuments(), TraceIdHolder.get());
    }

    @PostMapping("/compliance/documents/{documentCode}/publish")
    public ApiResponse<ComplianceDocument> publishComplianceDocument(@PathVariable String documentCode,
                                                                    @Valid @RequestBody ComplianceDocumentPublishRequest request) {
        return ApiResponse.success(saasTenantService.publishComplianceDocument(documentCode, request), TraceIdHolder.get());
    }

    @GetMapping("/compliance/acceptances")
    public ApiResponse<List<AdminComplianceAcceptanceView>> listComplianceAcceptances(@RequestParam(required = false) String tenantId,
                                                                                      @RequestParam(required = false) String documentCode,
                                                                                      @RequestParam(required = false) String acceptanceStatus) {
        return ApiResponse.success(
                saasTenantService.listAdminComplianceAcceptances(tenantId, documentCode, acceptanceStatus),
                TraceIdHolder.get()
        );
    }
}

