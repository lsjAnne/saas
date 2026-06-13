package com.dianshang.platform.recommendation.application;

import com.dianshang.platform.campaign.domain.repository.CampaignActivityRepository;
import com.dianshang.platform.campaign.model.CampaignActivity;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.member.domain.repository.MemberProfileRepository;
import com.dianshang.platform.member.model.MemberProfile;
import com.dianshang.platform.order.domain.repository.OrderRepository;
import com.dianshang.platform.order.model.OrderMain;
import com.dianshang.platform.product.domain.repository.CandidateProductRepository;
import com.dianshang.platform.product.model.CandidateProduct;
import com.dianshang.platform.store.domain.repository.StoreRepository;
import com.dianshang.platform.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final StoreRepository storeRepository;
    private final CandidateProductRepository candidateProductRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final OrderRepository orderRepository;
    private final CampaignActivityRepository campaignActivityRepository;

    public RecommendationService(StoreRepository storeRepository,
                                 CandidateProductRepository candidateProductRepository,
                                 MemberProfileRepository memberProfileRepository,
                                 OrderRepository orderRepository,
                                 CampaignActivityRepository campaignActivityRepository) {
        this.storeRepository = storeRepository;
        this.candidateProductRepository = candidateProductRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.orderRepository = orderRepository;
        this.campaignActivityRepository = campaignActivityRepository;
    }

    public RecommendationOverviewView getOverview(String tenantId, String storeId) {
        List<ProductRecommendationView> productRecommendations = buildProductRecommendations(tenantId, storeId);
        List<MemberRecommendationView> memberRecommendations = buildMemberRecommendations(tenantId, storeId);
        List<CampaignRecommendationView> campaignRecommendations = buildCampaignRecommendations(tenantId, storeId,
                productRecommendations,
                memberRecommendations);
        List<String> recommendedActions = new ArrayList<>();
        if (!productRecommendations.isEmpty()) {
            recommendedActions.add("prioritize_top_product_for_draft");
        }
        if (memberRecommendations.stream().anyMatch(item -> "revive_vip".equals(item.segmentCode()))) {
            recommendedActions.add("launch_vip_reactivation");
        }
        if (!campaignRecommendations.isEmpty()) {
            recommendedActions.add("prepare_campaign_execution");
        }
        return new RecommendationOverviewView(
                "smart_recommendation_ready",
                productRecommendations.size(),
                memberRecommendations.size(),
                campaignRecommendations.size(),
                recommendedActions
        );
    }

    public List<ProductRecommendationView> listProductRecommendations(String tenantId, String storeId, Integer limit) {
        return limit(buildProductRecommendations(tenantId, storeId), limit);
    }

    public List<MemberRecommendationView> listMemberRecommendations(String tenantId, String storeId, Integer limit) {
        return limit(buildMemberRecommendations(tenantId, storeId), limit);
    }

    public List<CampaignRecommendationView> listCampaignRecommendations(String tenantId, String storeId, Integer limit) {
        List<ProductRecommendationView> productRecommendations = buildProductRecommendations(tenantId, storeId);
        List<MemberRecommendationView> memberRecommendations = buildMemberRecommendations(tenantId, storeId);
        return limit(buildCampaignRecommendations(tenantId, storeId, productRecommendations, memberRecommendations), limit);
    }

    private List<ProductRecommendationView> buildProductRecommendations(String tenantId, String storeId) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        Map<String, Long> recentPaidOrderCountByStore = orderRepository.findByStoreIds(storeIds).stream()
                .filter(order -> isRecentWithinDays(order.createdAt(), 30))
                .filter(order -> List.of("paid", "processing", "completed").contains(defaultIfBlank(order.orderStatus(), "pending")))
                .collect(java.util.stream.Collectors.groupingBy(OrderMain::storeId, java.util.stream.Collectors.counting()));
        return candidateProductRepository.findByStoreIds(storeIds).stream()
                .map(candidate -> toProductRecommendation(candidate, recentPaidOrderCountByStore.getOrDefault(candidate.storeId(), 0L)))
                .filter(item -> item.score() >= 20)
                .sorted(Comparator.comparingInt(ProductRecommendationView::score).reversed()
                        .thenComparing(ProductRecommendationView::candidateProductId))
                .toList();
    }

    private ProductRecommendationView toProductRecommendation(CandidateProduct candidate, long recentPaidOrderCount) {
        int score = statusScore(candidate.status())
                + profitScore(candidate.estimatedProfit())
                + riskScore(candidate.riskLevel())
                + completenessScore(candidate.recommendationReason(), candidate.aiSummary())
                + (int) Math.min(recentPaidOrderCount * 2L, 10L);
        String suggestedAction = switch (defaultIfBlank(candidate.status(), "pending_review")) {
            case "in_pool", "testable" -> "generate_product_draft";
            case "pending_review" -> "review_candidate_product";
            default -> "observe_candidate_product";
        };
        return new ProductRecommendationView(
                candidate.candidateProductId(),
                candidate.storeId(),
                candidate.title(),
                candidate.category(),
                candidate.status(),
                candidate.estimatedProfit(),
                candidate.riskLevel(),
                candidate.recommendationReason(),
                candidate.aiSummary(),
                score,
                suggestedAction
        );
    }

    private List<MemberRecommendationView> buildMemberRecommendations(String tenantId, String storeId) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        return memberProfileRepository.findByStoreIds(storeIds).stream()
                .map(this::toMemberRecommendation)
                .filter(item -> item != null)
                .sorted(Comparator.comparingInt(MemberRecommendationView::score).reversed()
                        .thenComparing(MemberRecommendationView::memberId))
                .toList();
    }

    private MemberRecommendationView toMemberRecommendation(MemberProfile member) {
        if (isDormant(member.lastOrderAt(), 60) && member.totalPaidAmount().compareTo(BigDecimal.valueOf(1000)) >= 0) {
            return new MemberRecommendationView(
                    member.memberId(),
                    member.storeId(),
                    member.nickname(),
                    member.levelCode(),
                    member.totalOrderCount(),
                    member.totalPaidAmount(),
                    "revive_vip",
                    "high-value member has been dormant for over 60 days",
                    95,
                    "create_reactivation_touch_task"
            );
        }
        if (member.totalOrderCount() >= 3 && isRecentWithinDays(member.lastOrderAt(), 30)) {
            return new MemberRecommendationView(
                    member.memberId(),
                    member.storeId(),
                    member.nickname(),
                    member.levelCode(),
                    member.totalOrderCount(),
                    member.totalPaidAmount(),
                    "growth_upsell",
                    "member has stable recent orders and can accept bundle upsell",
                    78,
                    "create_cross_sell_touch_task"
            );
        }
        if (member.totalOrderCount() <= 1 && isRecentWithinDays(member.lastOrderAt(), 15)) {
            return new MemberRecommendationView(
                    member.memberId(),
                    member.storeId(),
                    member.nickname(),
                    member.levelCode(),
                    member.totalOrderCount(),
                    member.totalPaidAmount(),
                    "new_member_nurture",
                    "new member should receive onboarding incentive",
                    68,
                    "create_new_member_touch_task"
            );
        }
        return null;
    }

    private List<CampaignRecommendationView> buildCampaignRecommendations(String tenantId,
                                                                          String storeId,
                                                                          List<ProductRecommendationView> productRecommendations,
                                                                          List<MemberRecommendationView> memberRecommendations) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        List<CampaignActivity> campaigns = campaignActivityRepository.findByStoreIds(storeIds);
        List<OrderMain> recentOrders = orderRepository.findByStoreIds(storeIds).stream()
                .filter(order -> isRecentWithinDays(order.createdAt(), 30))
                .toList();
        ProductRecommendationView topProduct = productRecommendations.isEmpty() ? null : productRecommendations.get(0);
        List<CampaignRecommendationView> views = new ArrayList<>();
        if (topProduct != null && memberRecommendations.stream().anyMatch(item -> "revive_vip".equals(item.segmentCode()))) {
            views.add(new CampaignRecommendationView(
                    "member_reactivation",
                    "reactivate dormant vip members",
                    topProduct.storeId(),
                    List.of(topProduct.candidateProductId()),
                    "revive_vip",
                    "pair high-margin product with dormant vip reactivation",
                    Map.of("couponType", "exclusive_reactivation", "touchChannel", "sms+crm"),
                    92,
                    "create_campaign_draft"
            ));
        }
        if (topProduct != null && recentOrders.size() >= 2) {
            views.add(new CampaignRecommendationView(
                    "live_conversion_boost",
                    "boost live conversion for top product",
                    topProduct.storeId(),
                    List.of(topProduct.candidateProductId()),
                    "all_active_buyers",
                    hasActivePublishedCampaign(campaigns)
                            ? "active campaign exists, extend it with live-room conversion assets"
                            : "recent orders validate demand, suitable for live-room conversion campaign",
                    Map.of("contentMode", "live_room", "discountRate", BigDecimal.valueOf(0.90)),
                    84,
                    "prepare_live_campaign"
            ));
        }
        if (topProduct != null && memberRecommendations.stream().anyMatch(item -> "growth_upsell".equals(item.segmentCode()))) {
            views.add(new CampaignRecommendationView(
                    "growth_member_bundle",
                    "upsell bundle for growth members",
                    topProduct.storeId(),
                    List.of(topProduct.candidateProductId()),
                    "growth_upsell",
                    "growth members show repeat demand and can accept bundle offer",
                    Map.of("bundleStrategy", "second_item_discount", "discountRate", BigDecimal.valueOf(0.88)),
                    76,
                    "create_bundle_campaign"
            ));
        }
        return views;
    }

    private List<String> resolveStoreIds(String tenantId, String storeId) {
        List<Store> stores = storeRepository.findByTenantId(tenantId);
        if (storeId == null || storeId.isBlank()) {
            return stores.stream().map(Store::storeId).toList();
        }
        return stores.stream()
                .filter(store -> storeId.equals(store.storeId()))
                .findFirst()
                .map(store -> List.of(store.storeId()))
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
    }

    private <T> List<T> limit(List<T> items, Integer requestedLimit) {
        int safeLimit = requestedLimit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
        return items.stream().limit(safeLimit).toList();
    }

    private int statusScore(String status) {
        return switch (defaultIfBlank(status, "pending_review")) {
            case "in_pool" -> 35;
            case "testable" -> 30;
            case "pending_review" -> 10;
            case "not_recommended" -> -40;
            default -> 0;
        };
    }

    private int profitScore(BigDecimal estimatedProfit) {
        if (estimatedProfit == null) {
            return 0;
        }
        return Math.min(estimatedProfit.multiply(BigDecimal.valueOf(0.8)).intValue(), 30);
    }

    private int riskScore(String riskLevel) {
        return switch (defaultIfBlank(riskLevel, "medium")) {
            case "low" -> 20;
            case "medium" -> 5;
            case "high" -> -25;
            default -> 0;
        };
    }

    private int completenessScore(String recommendationReason, String aiSummary) {
        int score = 0;
        if (recommendationReason != null && !recommendationReason.isBlank()) {
            score += 5;
        }
        if (aiSummary != null && !aiSummary.isBlank()) {
            score += 5;
        }
        return score;
    }

    private boolean isDormant(OffsetDateTime lastOrderAt, int thresholdDays) {
        if (lastOrderAt == null) {
            return false;
        }
        return ChronoUnit.DAYS.between(lastOrderAt.toLocalDate(), OffsetDateTime.now().toLocalDate()) > thresholdDays;
    }

    private boolean isRecentWithinDays(OffsetDateTime time, int days) {
        if (time == null) {
            return false;
        }
        return !time.toLocalDate().isBefore(OffsetDateTime.now().toLocalDate().minusDays(days - 1L));
    }

    private boolean hasActivePublishedCampaign(List<CampaignActivity> campaigns) {
        OffsetDateTime now = OffsetDateTime.now();
        return campaigns.stream()
                .anyMatch(campaign -> "published".equals(campaign.status())
                        && !campaign.startAt().isAfter(now)
                        && !campaign.endAt().isBefore(now));
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public record RecommendationOverviewView(
            String headline,
            int productRecommendationCount,
            int memberRecommendationCount,
            int campaignRecommendationCount,
            List<String> recommendedActions
    ) {
    }

    public record ProductRecommendationView(
            String candidateProductId,
            String storeId,
            String title,
            String category,
            String status,
            BigDecimal estimatedProfit,
            String riskLevel,
            String recommendationReason,
            String aiSummary,
            int score,
            String suggestedAction
    ) {
    }

    public record MemberRecommendationView(
            String memberId,
            String storeId,
            String nickname,
            String levelCode,
            int totalOrderCount,
            BigDecimal totalPaidAmount,
            String segmentCode,
            String recommendationReason,
            int score,
            String suggestedAction
    ) {
    }

    public record CampaignRecommendationView(
            String campaignType,
            String campaignName,
            String targetStoreId,
            List<String> recommendedCandidateProductIds,
            String targetMemberSegment,
            String recommendationReason,
            Map<String, Object> suggestedRule,
            int score,
            String suggestedAction
    ) {
    }
}
