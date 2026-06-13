package com.dianshang.platform.recommendation.controller;

import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.recommendation.application.RecommendationService;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.DASHBOARD_READ)
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/api/recommendations/overview")
    public ApiResponse<RecommendationService.RecommendationOverviewView> getOverview(@RequestParam(required = false) String storeId) {
        return ApiResponse.success(
                recommendationService.getOverview(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/recommendations/products")
    public ApiResponse<List<RecommendationService.ProductRecommendationView>> listProducts(@RequestParam(required = false) String storeId,
                                                                                           @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(
                recommendationService.listProductRecommendations(TenantAccessSupport.requiredTenantId(), storeId, limit),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/recommendations/members")
    public ApiResponse<List<RecommendationService.MemberRecommendationView>> listMembers(@RequestParam(required = false) String storeId,
                                                                                         @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(
                recommendationService.listMemberRecommendations(TenantAccessSupport.requiredTenantId(), storeId, limit),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/recommendations/campaigns")
    public ApiResponse<List<RecommendationService.CampaignRecommendationView>> listCampaigns(@RequestParam(required = false) String storeId,
                                                                                              @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(
                recommendationService.listCampaignRecommendations(TenantAccessSupport.requiredTenantId(), storeId, limit),
                TraceIdHolder.get()
        );
    }
}
