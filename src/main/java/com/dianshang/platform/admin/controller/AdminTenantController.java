package com.dianshang.platform.admin.controller;

import com.dianshang.platform.admin.dto.AdminTenantOverview;
import com.dianshang.platform.admin.dto.FeatureToggleUpdateRequest;
import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.saas.application.SaasTenantService.AdminComplianceAcceptanceView;
import com.dianshang.platform.saas.application.SaasTenantService.AdminComplianceDocumentView;
import com.dianshang.platform.saas.application.SaasTenantService.ComplianceDocument;
import com.dianshang.platform.saas.application.SaasTenantService.ComplianceDocumentPublishRequest;
import com.dianshang.platform.saas.application.SaasTenantService.DeliveryReadinessView;
import com.dianshang.platform.saas.application.SaasTenantService.ReleaseReadinessView;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.saas.application.SaasTenantService.SubscriptionAutomationSummary;
import com.dianshang.platform.saas.model.TenantProfile;
import com.dianshang.platform.tenant.security.RequirePlatformRoles;
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
