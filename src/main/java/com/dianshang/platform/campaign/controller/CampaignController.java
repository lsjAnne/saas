package com.dianshang.platform.campaign.controller;

import com.dianshang.platform.campaign.application.CampaignService;
import com.dianshang.platform.campaign.application.CampaignService.SaveCampaignRequest;
import com.dianshang.platform.campaign.application.CampaignService.SaveCouponTemplateRequest;
import com.dianshang.platform.campaign.model.CampaignActivity;
import com.dianshang.platform.campaign.model.CouponTemplate;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequireTenantPermission(AuthPermissionCodes.CAMPAIGN_MANAGE)
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping("/api/campaigns")
    public ApiResponse<List<CampaignActivity>> listCampaigns() {
        return ApiResponse.success(
                campaignService.listCampaigns(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/campaigns")
    public ApiResponse<CampaignActivity> createCampaign(@Valid @RequestBody SaveCampaignPayload request) {
        return ApiResponse.success(
                campaignService.createCampaign(TenantAccessSupport.requiredTenantId(), request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/campaigns/{id}")
    public ApiResponse<CampaignActivity> getCampaign(@PathVariable String id) {
        return ApiResponse.success(
                campaignService.getCampaign(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/campaigns/{id}")
    public ApiResponse<CampaignActivity> updateCampaign(@PathVariable String id,
                                                        @Valid @RequestBody SaveCampaignPayload request) {
        return ApiResponse.success(
                campaignService.updateCampaign(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/campaigns/{id}/submit-approval")
    public ApiResponse<CampaignActivity> submitApproval(@PathVariable String id) {
        return ApiResponse.success(
                campaignService.submitApproval(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        id
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/campaigns/{id}/publish")
    public ApiResponse<CampaignActivity> publishCampaign(@PathVariable String id) {
        return ApiResponse.success(
                campaignService.publishCampaign(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/coupon-templates")
    public ApiResponse<List<CouponTemplate>> listCouponTemplates() {
        return ApiResponse.success(
                campaignService.listCouponTemplates(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/coupon-templates")
    public ApiResponse<CouponTemplate> createCouponTemplate(@Valid @RequestBody SaveCouponTemplatePayload request) {
        return ApiResponse.success(
                campaignService.createCouponTemplate(TenantAccessSupport.requiredTenantId(), request.toCommand()),
                TraceIdHolder.get()
        );
    }
}

record SaveCampaignPayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "activityType is required")
        String activityType,
        @NotBlank(message = "activityName is required")
        String activityName,
        @NotNull(message = "startAt is required")
        OffsetDateTime startAt,
        @NotNull(message = "endAt is required")
        OffsetDateTime endAt,
        List<String> productIds,
        Map<String, Object> rule,
        String couponTemplateId
) {
    SaveCampaignRequest toCommand() {
        return new SaveCampaignRequest(storeId, activityType, activityName, startAt, endAt, productIds, rule, couponTemplateId);
    }
}

record SaveCouponTemplatePayload(
        @NotBlank(message = "storeId is required")
        String storeId,
        @NotBlank(message = "templateName is required")
        String templateName,
        @NotBlank(message = "discountType is required")
        String discountType,
        @NotNull(message = "discountValue is required")
        BigDecimal discountValue,
        BigDecimal thresholdAmount,
        String status
) {
    SaveCouponTemplateRequest toCommand() {
        return new SaveCouponTemplateRequest(storeId, templateName, discountType, discountValue, thresholdAmount, status);
    }
}
