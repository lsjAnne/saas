package backend.recommendation.controller;

import backend.auth.security.AuthPermissionCodes;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.recommendation.application.RecommendationService;
import backend.auth.security.RequireTenantPermission;
import backend.tenant.context.TenantAccessSupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/api/recommendations/assistant/ask")
    public ApiResponse<RecommendationService.BusinessAssistantResponseView> askAssistant(@Valid @RequestBody AskBusinessAssistantRequest request) {
        return ApiResponse.success(
                recommendationService.answerBusinessQuestion(
                        TenantAccessSupport.requiredTenantId(),
                        request.storeId(),
                        request.question()
                ),
                TraceIdHolder.get()
        );
    }

    public record AskBusinessAssistantRequest(
            String storeId,
            @NotBlank(message = "question must not be blank")
            String question
    ) {
    }
}

