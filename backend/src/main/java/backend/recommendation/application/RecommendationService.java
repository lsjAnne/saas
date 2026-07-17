package backend.recommendation.application;

import backend.campaign.domain.repository.CampaignActivityRepository;
import backend.campaign.model.CampaignActivity;
import backend.common.exception.BusinessException;
import backend.exceptioncenter.domain.repository.ExceptionTaskRepository;
import backend.exceptioncenter.model.ExceptionTask;
import backend.fulfillment.domain.repository.FulfillmentTaskRepository;
import backend.fulfillment.model.FulfillmentTask;
import backend.inventory.domain.repository.InventorySnapshotRepository;
import backend.inventory.domain.repository.ReplenishmentTaskRepository;
import backend.inventory.model.InventorySnapshot;
import backend.inventory.model.ReplenishmentTask;
import backend.member.domain.repository.MemberProfileRepository;
import backend.member.model.MemberProfile;
import backend.order.domain.repository.OrderRepository;
import backend.order.model.OrderMain;
import backend.product.domain.repository.CandidateProductRepository;
import backend.product.domain.repository.ProductRepository;
import backend.product.model.CandidateProduct;
import backend.product.model.Product;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final List<String> CLOSED_EXCEPTION_STATUSES = List.of("resolved", "ignored", "closed", "done");
    private static final List<String> CLOSED_FULFILLMENT_STATUSES = List.of("done", "completed", "confirmed", "success");

    private final StoreRepository storeRepository;
    private final CandidateProductRepository candidateProductRepository;
    private final ProductRepository productRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final OrderRepository orderRepository;
    private final CampaignActivityRepository campaignActivityRepository;
    private final ExceptionTaskRepository exceptionTaskRepository;
    private final FulfillmentTaskRepository fulfillmentTaskRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final ReplenishmentTaskRepository replenishmentTaskRepository;

    public RecommendationService(StoreRepository storeRepository,
                                 CandidateProductRepository candidateProductRepository,
                                 ProductRepository productRepository,
                                 MemberProfileRepository memberProfileRepository,
                                 OrderRepository orderRepository,
                                 CampaignActivityRepository campaignActivityRepository,
                                 ExceptionTaskRepository exceptionTaskRepository,
                                 FulfillmentTaskRepository fulfillmentTaskRepository,
                                 InventorySnapshotRepository inventorySnapshotRepository,
                                 ReplenishmentTaskRepository replenishmentTaskRepository) {
        this.storeRepository = storeRepository;
        this.candidateProductRepository = candidateProductRepository;
        this.productRepository = productRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.orderRepository = orderRepository;
        this.campaignActivityRepository = campaignActivityRepository;
        this.exceptionTaskRepository = exceptionTaskRepository;
        this.fulfillmentTaskRepository = fulfillmentTaskRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.replenishmentTaskRepository = replenishmentTaskRepository;
    }

    public RecommendationOverviewView getOverview(String tenantId, String storeId) {
        List<ProductRecommendationView> productRecommendations = buildProductRecommendations(tenantId, storeId);
        List<MemberRecommendationView> memberRecommendations = buildMemberRecommendations(tenantId, storeId);
        List<CampaignRecommendationView> campaignRecommendations = buildCampaignRecommendations(
                tenantId,
                storeId,
                productRecommendations,
                memberRecommendations
        );
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

    public BusinessAssistantResponseView answerBusinessQuestion(String tenantId, String storeId, String question) {
        String normalizedQuestion = normalize(question);
        AssistantContext context = buildAssistantContext(tenantId, storeId, question);
        AssistantIntent intent = resolveIntent(normalizedQuestion);
        return switch (intent) {
            case PRODUCT -> buildProductAssistantResponse(context, normalizedQuestion);
            case MEMBER -> buildMemberAssistantResponse(context, normalizedQuestion);
            case ORDER -> buildOrderAssistantResponse(context, normalizedQuestion);
            case RISK -> buildRiskAssistantResponse(context, normalizedQuestion);
            case FULFILLMENT -> buildFulfillmentAssistantResponse(context, normalizedQuestion);
            case INVENTORY -> buildInventoryAssistantResponse(context, normalizedQuestion);
            case CAMPAIGN -> buildCampaignAssistantResponse(context, normalizedQuestion);
            case SUMMARY -> buildSummaryAssistantResponse(context, normalizedQuestion);
        };
    }

    private AssistantContext buildAssistantContext(String tenantId, String storeId, String question) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        List<Store> stores = storeRepository.findByTenantId(tenantId).stream()
                .filter(store -> storeIds.contains(store.storeId()))
                .toList();
        String storeName = storeId == null || storeId.isBlank()
                ? stores.size() == 1 ? stores.get(0).shopName() : "all-stores"
                : stores.stream().findFirst().map(Store::shopName).orElse(storeId);
        List<ProductRecommendationView> productRecommendations = buildProductRecommendations(tenantId, storeId);
        List<MemberRecommendationView> memberRecommendations = buildMemberRecommendations(tenantId, storeId);
        List<CampaignRecommendationView> campaignRecommendations = buildCampaignRecommendations(
                tenantId,
                storeId,
                productRecommendations,
                memberRecommendations
        );
        List<MemberProfile> memberProfiles = memberProfileRepository.findByStoreIds(storeIds);
        List<OrderMain> orders = orderRepository.findByStoreIds(storeIds).stream()
                .sorted(Comparator.comparing(OrderMain::createdAt).reversed())
                .toList();
        List<ExceptionTask> openExceptions = exceptionTaskRepository.findByStoreIds(storeIds).stream()
                .filter(task -> !CLOSED_EXCEPTION_STATUSES.contains(normalize(task.status())))
                .sorted(Comparator.comparingInt((ExceptionTask task) -> severityRank(task.severity())).reversed()
                        .thenComparing(ExceptionTask::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<FulfillmentTask> blockedFulfillmentTasks = fulfillmentTaskRepository.findByStoreIds(storeIds).stream()
                .filter(task -> !CLOSED_FULFILLMENT_STATUSES.contains(normalize(task.status()))
                        || (task.lastErrorMessage() != null && !task.lastErrorMessage().isBlank()))
                .sorted(Comparator.comparing(FulfillmentTask::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<InventorySnapshot> lowStockSnapshots = inventorySnapshotRepository.findByStoreIds(storeIds).stream()
                .filter(snapshot -> snapshot.availableStock() <= snapshot.safetyStock())
                .sorted(Comparator.comparingInt(InventorySnapshot::availableStock)
                        .thenComparingInt(InventorySnapshot::safetyStock))
                .toList();
        List<ReplenishmentTask> replenishmentTasks = replenishmentTaskRepository.findByStoreIds(storeIds).stream()
                .sorted(Comparator.comparingInt(ReplenishmentTask::suggestedQty).reversed()
                        .thenComparing(ReplenishmentTask::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<CampaignActivity> campaigns = campaignActivityRepository.findByStoreIds(storeIds);
        Map<String, Product> productMap = productRepository.findByStoreIds(storeIds).stream()
                .collect(Collectors.toMap(Product::productId, product -> product, (left, right) -> left, LinkedHashMap::new));
        return new AssistantContext(
                "assistant-" + System.currentTimeMillis(),
                question,
                storeName,
                productRecommendations,
                memberRecommendations,
                campaignRecommendations,
                memberProfiles,
                orders,
                openExceptions,
                blockedFulfillmentTasks,
                lowStockSnapshots,
                replenishmentTasks,
                campaigns,
                productMap,
                OffsetDateTime.now()
        );
    }

    private BusinessAssistantResponseView buildProductAssistantResponse(AssistantContext context, String normalizedQuestion) {
        List<ProductRecommendationView> topItems = context.productRecommendations().stream().limit(3).toList();
        ProductRecommendationView topItem = topItems.isEmpty() ? null : topItems.get(0);
        return buildAssistantResponse(
                context,
                AssistantIntent.PRODUCT,
                "product-focus",
                topItem == null ? "review product pool first" : "promote " + topItem.title(),
                topItem == null
                        ? "No new high-priority product recommendation is available right now."
                        : "The top product to push now is %s.".formatted(topItem.title()),
                resolveConfidence(normalizedQuestion, topItems.isEmpty() ? 1 : topItems.size()),
                buildConfidenceReason(normalizedQuestion, "product signals were loaded"),
                List.of("product", "campaign", "recommendation"),
                uniqueStrings(listOfNullable(
                        "product recommendations: " + context.productRecommendations().size(),
                        topItem == null ? null : "top product: " + topItem.title()
                )),
                List.of(
                        "scope: " + context.storeName(),
                        "product recommendations: " + context.productRecommendations().size(),
                        "campaign recommendations: " + context.campaignRecommendations().size()
                ),
                topItems.isEmpty()
                        ? List.of("Review candidate products, mapping coverage, and draft readiness first.")
                        : topItems.stream()
                        .map(item -> "%s | reason=%s | profit=%s | risk=%s".formatted(
                                item.title(),
                                defaultIfBlank(item.recommendationReason(), "n/a"),
                                item.estimatedProfit() == null ? "n/a" : item.estimatedProfit(),
                                defaultIfBlank(item.riskLevel(), "medium")
                        ))
                        .toList(),
                List.of(
                        "Which product should go live first?",
                        "Should the risky products be removed from the plan?",
                        "Which campaign should support this product?"
                ),
                List.of(
                        new BusinessAssistantActionView("Open Product Center", "/app/product-center", "Continue product draft and publish work."),
                        new BusinessAssistantActionView("View Product Mapping", "/app/product-mapping-center", "Check product and source mapping."),
                        new BusinessAssistantActionView("Open Campaign Center", "/app/campaign-center", "Connect the product with a campaign plan.")
                ),
                List.of("recommendation-overview", "product-recommendation")
        );
    }

    private BusinessAssistantResponseView buildMemberAssistantResponse(AssistantContext context, String normalizedQuestion) {
        List<MemberRecommendationView> topMembers = context.memberRecommendations().stream().limit(3).toList();
        long dormantMembers = context.memberProfiles().stream()
                .filter(member -> isDormant(member.lastOrderAt(), 60))
                .count();
        MemberRecommendationView topMember = topMembers.isEmpty() ? null : topMembers.get(0);
        return buildAssistantResponse(
                context,
                AssistantIntent.MEMBER,
                "member-growth",
                topMember == null ? "stabilize member segmentation" : "engage " + topMember.nickname(),
                topMember == null
                        ? "No new member operation recommendation is available right now."
                        : "The highest value member action is %s with segment %s.".formatted(topMember.nickname(), topMember.segmentCode()),
                resolveConfidence(normalizedQuestion, topMembers.isEmpty() ? 1 : topMembers.size() + 1),
                buildConfidenceReason(normalizedQuestion, "member signals were loaded"),
                List.of("member", "crm", "reactivation"),
                uniqueStrings(listOfNullable(
                        "member recommendations: " + context.memberRecommendations().size(),
                        dormantMembers > 0 ? "dormant members: " + dormantMembers : null
                )),
                List.of(
                        "scope: " + context.storeName(),
                        "member recommendations: " + context.memberRecommendations().size(),
                        "dormant members: " + dormantMembers
                ),
                topMembers.isEmpty()
                        ? List.of("Review CRM segments, recall tasks, and lifecycle rules first.")
                        : topMembers.stream()
                        .map(item -> "%s | segment=%s | paid=%s | action=%s".formatted(
                                item.nickname(),
                                item.segmentCode(),
                                item.totalPaidAmount(),
                                item.suggestedAction()
                        ))
                        .toList(),
                List.of(
                        "Which dormant members should be recalled first?",
                        "Is this week better for recall or upsell?",
                        "Which campaign should serve this segment?"
                ),
                List.of(
                        new BusinessAssistantActionView("Open Member CRM", "/app/member-crm-center", "Continue segmentation and outreach work."),
                        new BusinessAssistantActionView("Open Campaign Center", "/app/campaign-center", "Connect the segment with a campaign plan.")
                ),
                List.of("member-recommendation", "member-analysis")
        );
    }

    private BusinessAssistantResponseView buildOrderAssistantResponse(AssistantContext context, String normalizedQuestion) {
        List<OrderMain> atRiskOrders = context.orders().stream()
                .filter(order -> hasOpenException(order.orderId(), context.openExceptions())
                        || hasBlockedFulfillment(order.orderId(), context.blockedFulfillmentTasks())
                        || isPastDue(order.timeoutAt()))
                .limit(3)
                .toList();
        OrderMain topOrder = atRiskOrders.isEmpty() ? null : atRiskOrders.get(0);
        return buildAssistantResponse(
                context,
                AssistantIntent.ORDER,
                "order-check",
                topOrder == null ? "order flow looks stable" : "review order " + topOrder.orderId(),
                topOrder == null
                        ? "No order needs immediate manual fallback right now."
                        : "Order %s currently needs manual attention.".formatted(topOrder.orderId()),
                resolveConfidence(normalizedQuestion, atRiskOrders.isEmpty() ? 1 : atRiskOrders.size() + 1),
                buildConfidenceReason(normalizedQuestion, "order, exception, and fulfillment signals were loaded"),
                List.of("order", "fulfillment", "risk"),
                uniqueStrings(listOfNullable(
                        "orders: " + context.orders().size(),
                        context.openExceptions().isEmpty() ? null : "exceptions: " + context.openExceptions().size(),
                        context.blockedFulfillmentTasks().isEmpty() ? null : "blocked fulfillment tasks: " + context.blockedFulfillmentTasks().size()
                )),
                List.of(
                        "scope: " + context.storeName(),
                        "order total: " + context.orders().size(),
                        "risk orders: " + atRiskOrders.size()
                ),
                atRiskOrders.isEmpty()
                        ? List.of("Keep watching timeout, exception, and fulfillment replay surfaces.")
                        : atRiskOrders.stream()
                        .map(order -> "order=%s | status=%s | logistics=%s | timeout=%s".formatted(
                                order.orderId(),
                                order.orderStatus(),
                                order.logisticsStatus(),
                                order.timeoutAt()
                        ))
                        .toList(),
                List.of(
                        "Which orders need manual handling today?",
                        "Is this mainly a config problem or an execution problem?",
                        "Which orders will timeout first?"
                ),
                List.of(
                        new BusinessAssistantActionView("Open Order Fulfillment Center", "/app/order-fulfillment-center", "Inspect orders, fulfillment tasks, and replay flow."),
                        new BusinessAssistantActionView("Open Exception Center", "/app/exception-center", "Review exception tasks and manual fallback work.")
                ),
                List.of("order-flow", "exception-center")
        );
    }

    private BusinessAssistantResponseView buildRiskAssistantResponse(AssistantContext context, String normalizedQuestion) {
        List<ExceptionTask> topExceptions = context.openExceptions().stream().limit(3).toList();
        ExceptionTask topException = topExceptions.isEmpty() ? null : topExceptions.get(0);
        return buildAssistantResponse(
                context,
                AssistantIntent.RISK,
                "risk-check",
                topException == null ? "no new high-risk exception" : "review " + topException.exceptionType(),
                topException == null
                        ? "No new high-risk exception is open right now."
                        : "The top open risk is %s for %s.".formatted(topException.exceptionType(), topException.relatedId()),
                resolveConfidence(normalizedQuestion, topExceptions.isEmpty() ? 1 : topExceptions.size() + 1),
                buildConfidenceReason(normalizedQuestion, "exception signals were loaded"),
                List.of("exception", "alert", "manual"),
                uniqueStrings(listOfNullable(
                        context.openExceptions().isEmpty() ? null : "open exceptions: " + context.openExceptions().size(),
                        topException == null ? null : "top exception: " + topException.exceptionType()
                )),
                List.of(
                        "scope: " + context.storeName(),
                        "open exceptions: " + context.openExceptions().size()
                ),
                topExceptions.isEmpty()
                        ? List.of("Keep watching fulfillment timeout, complaint, and low-stock surfaces.")
                        : topExceptions.stream()
                        .map(item -> "%s | severity=%s | related=%s | suggestion=%s".formatted(
                                item.exceptionType(),
                                item.severity(),
                                item.relatedId(),
                                defaultIfBlank(item.suggestionText(), "n/a")
                        ))
                        .toList(),
                List.of(
                        "Which risk should be escalated first?",
                        "Will these risks affect today delivery flow?",
                        "Which exceptions should be handed to a human directly?"
                ),
                List.of(
                        new BusinessAssistantActionView("Open Exception Center", "/app/exception-center", "Continue exception processing and escalation."),
                        new BusinessAssistantActionView("Open Order Fulfillment Center", "/app/order-fulfillment-center", "Verify whether risk already spread to fulfillment.")
                ),
                List.of("exception-center")
        );
    }

    private BusinessAssistantResponseView buildFulfillmentAssistantResponse(AssistantContext context, String normalizedQuestion) {
        List<FulfillmentTask> topTasks = context.blockedFulfillmentTasks().stream().limit(3).toList();
        FulfillmentTask topTask = topTasks.isEmpty() ? null : topTasks.get(0);
        return buildAssistantResponse(
                context,
                AssistantIntent.FULFILLMENT,
                "fulfillment-check",
                topTask == null ? "fulfillment flow looks stable" : "review order " + topTask.orderId(),
                topTask == null
                        ? "No obvious fulfillment blocker is open right now."
                        : "Order %s has a fulfillment blocker that should be checked first.".formatted(topTask.orderId()),
                resolveConfidence(normalizedQuestion, topTasks.isEmpty() ? 1 : topTasks.size() + 1),
                buildConfidenceReason(normalizedQuestion, "fulfillment signals were loaded"),
                List.of("fulfillment", "logistics", "order"),
                uniqueStrings(listOfNullable(
                        context.blockedFulfillmentTasks().isEmpty() ? null : "blocked fulfillment tasks: " + context.blockedFulfillmentTasks().size(),
                        topTask == null || topTask.lastErrorMessage() == null ? null : "latest error: " + topTask.lastErrorMessage()
                )),
                List.of(
                        "scope: " + context.storeName(),
                        "blocked fulfillment tasks: " + context.blockedFulfillmentTasks().size()
                ),
                topTasks.isEmpty()
                        ? List.of("Review channel auth and default shipping config if needed.")
                        : topTasks.stream()
                        .map(task -> "order=%s | status=%s | retry=%d | error=%s".formatted(
                                task.orderId(),
                                task.status(),
                                task.retryCount() == null ? 0 : task.retryCount(),
                                defaultIfBlank(task.lastErrorMessage(), "n/a")
                        ))
                        .toList(),
                List.of(
                        "Is this mainly a config issue or an execution issue?",
                        "Which orders will timeout because of the blocker?",
                        "Which channel config should be checked first?"
                ),
                List.of(
                        new BusinessAssistantActionView("Open Order Fulfillment Center", "/app/order-fulfillment-center", "Inspect fulfillment tasks and replay flow."),
                        new BusinessAssistantActionView("Open Store Channel Center", "/app/store-channel-center", "Check auth and shipping configuration.")
                ),
                List.of("fulfillment-task", "exception-center")
        );
    }

    private BusinessAssistantResponseView buildInventoryAssistantResponse(AssistantContext context, String normalizedQuestion) {
        List<InventorySnapshot> topSnapshots = context.lowStockSnapshots().stream().limit(3).toList();
        InventorySnapshot topSnapshot = topSnapshots.isEmpty() ? null : topSnapshots.get(0);
        return buildAssistantResponse(
                context,
                AssistantIntent.INVENTORY,
                "inventory-replenishment",
                topSnapshot == null ? "inventory is above safety line" : "replenish " + productTitle(context, topSnapshot.productId()),
                topSnapshot == null
                        ? "No inventory snapshot is currently below the safety line."
                        : "%d SKUs are already at or below safety stock.".formatted(context.lowStockSnapshots().size()),
                resolveConfidence(normalizedQuestion, topSnapshots.isEmpty() ? 1 : topSnapshots.size() + 1),
                buildConfidenceReason(normalizedQuestion, "inventory and replenishment signals were loaded"),
                List.of("inventory", "replenishment", "procurement"),
                uniqueStrings(listOfNullable(
                        context.lowStockSnapshots().isEmpty() ? null : "low-stock skus: " + context.lowStockSnapshots().size(),
                        context.replenishmentTasks().isEmpty() ? null : "replenishment tasks: " + context.replenishmentTasks().size()
                )),
                List.of(
                        "scope: " + context.storeName(),
                        "low-stock skus: " + context.lowStockSnapshots().size(),
                        "replenishment tasks: " + context.replenishmentTasks().size()
                ),
                topSnapshots.isEmpty()
                        ? List.of("Inventory is stable now, but live and campaign products still need attention.")
                        : topSnapshots.stream()
                        .map(item -> "%s | sku=%s | available=%d | reserved=%d | safety=%d".formatted(
                                productTitle(context, item.productId()),
                                item.skuId(),
                                item.availableStock(),
                                item.reservedStock(),
                                item.safetyStock()
                        ))
                        .toList(),
                List.of(
                        "Which products should be replenished first?",
                        "Should priority follow stock gap or sales velocity?",
                        "Should supplier alignment or safety-stock tuning happen first?"
                ),
                List.of(
                        new BusinessAssistantActionView("Open Inventory Replenishment Center", "/app/inventory-replenishment-center", "Continue replenishment and procurement work."),
                        new BusinessAssistantActionView("Open Supplier Center", "/app/supplier-center", "Check supplier capacity for replenishment.")
                ),
                List.of("inventory-snapshot", "replenishment-task")
        );
    }

    private BusinessAssistantResponseView buildCampaignAssistantResponse(AssistantContext context, String normalizedQuestion) {
        List<CampaignRecommendationView> topCampaigns = context.campaignRecommendations().stream().limit(3).toList();
        long activeCampaignCount = context.campaigns().stream()
                .filter(campaign -> List.of("created", "pending_approval", "published", "running").contains(normalize(campaign.status())))
                .count();
        CampaignRecommendationView topCampaign = topCampaigns.isEmpty() ? null : topCampaigns.get(0);
        return buildAssistantResponse(
                context,
                AssistantIntent.CAMPAIGN,
                "campaign-planning",
                topCampaign == null ? "confirm campaign prerequisites first" : "prepare " + topCampaign.campaignName(),
                topCampaign == null
                        ? "No new campaign recommendation is available right now."
                        : "The best next campaign is %s.".formatted(topCampaign.campaignName()),
                resolveConfidence(normalizedQuestion, topCampaigns.isEmpty() ? 1 : topCampaigns.size() + 1),
                buildConfidenceReason(normalizedQuestion, "campaign signals were loaded"),
                List.of("campaign", "member", "product"),
                uniqueStrings(listOfNullable(
                        context.campaignRecommendations().isEmpty() ? null : "campaign recommendations: " + context.campaignRecommendations().size(),
                        activeCampaignCount > 0 ? "active campaigns: " + activeCampaignCount : null
                )),
                List.of(
                        "scope: " + context.storeName(),
                        "campaign recommendations: " + context.campaignRecommendations().size(),
                        "active campaigns: " + activeCampaignCount
                ),
                topCampaigns.isEmpty()
                        ? List.of("Review product readiness, member segment readiness, and stock coverage first.")
                        : topCampaigns.stream()
                        .map(item -> "%s | reason=%s | action=%s".formatted(
                                item.campaignName(),
                                item.recommendationReason(),
                                item.suggestedAction()
                        ))
                        .toList(),
                List.of(
                        "How should campaign and top product work together?",
                        "Is this week better for acquisition or recall?",
                        "Should dormant members be tied to the campaign?"
                ),
                List.of(
                        new BusinessAssistantActionView("Open Campaign Center", "/app/campaign-center", "Continue campaign setup and approval work."),
                        new BusinessAssistantActionView("Open Member CRM", "/app/member-crm-center", "Check segment readiness for campaign touch points.")
                ),
                List.of("campaign-recommendation", "member-analysis")
        );
    }

    private BusinessAssistantResponseView buildSummaryAssistantResponse(AssistantContext context, String normalizedQuestion) {
        ProductRecommendationView topProduct = context.productRecommendations().isEmpty() ? null : context.productRecommendations().get(0);
        ExceptionTask topException = context.openExceptions().isEmpty() ? null : context.openExceptions().get(0);
        InventorySnapshot topLowStock = context.lowStockSnapshots().isEmpty() ? null : context.lowStockSnapshots().get(0);
        CampaignRecommendationView topCampaign = context.campaignRecommendations().isEmpty() ? null : context.campaignRecommendations().get(0);
        List<String> insights = uniqueStrings(listOfNullable(
                topProduct == null ? null : "product: %s should be pushed first".formatted(topProduct.title()),
                topException == null ? null : "risk: %s for %s".formatted(topException.exceptionType(), topException.relatedId()),
                context.blockedFulfillmentTasks().isEmpty() ? null : "fulfillment: blocker on order %s".formatted(context.blockedFulfillmentTasks().get(0).orderId()),
                topLowStock == null ? null : "inventory: %s / %s is at safety line".formatted(productTitle(context, topLowStock.productId()), topLowStock.skuId()),
                topCampaign == null ? null : "campaign: %s is the best next action".formatted(topCampaign.campaignName())
        ));
        return buildAssistantResponse(
                context,
                AssistantIntent.SUMMARY,
                "business-summary",
                "start from the highest-value action today",
                "Cross-domain business signals have been aggregated from recommendation, order, inventory, campaign, and member surfaces.",
                resolveConfidence(normalizedQuestion, insights.size()),
                buildConfidenceReason(normalizedQuestion, "cross-domain signals were loaded"),
                List.of("product", "order", "inventory", "campaign", "member"),
                uniqueStrings(listOfNullable(
                        "total recommendations: " + (context.productRecommendations().size()
                                + context.memberRecommendations().size()
                                + context.campaignRecommendations().size()),
                        context.openExceptions().isEmpty() ? null : "open exceptions: " + context.openExceptions().size(),
                        context.lowStockSnapshots().isEmpty() ? null : "low-stock skus: " + context.lowStockSnapshots().size()
                )),
                List.of(
                        "scope: " + context.storeName(),
                        "recommendations: " + (context.productRecommendations().size()
                                + context.memberRecommendations().size()
                                + context.campaignRecommendations().size()),
                        "open exceptions: " + context.openExceptions().size(),
                        "blocked fulfillment tasks: " + context.blockedFulfillmentTasks().size(),
                        "low-stock skus: " + context.lowStockSnapshots().size()
                ),
                insights.isEmpty()
                        ? List.of("No strong cross-domain warning is open right now.")
                        : insights,
                uniqueStrings(listOfNullable(
                        topProduct == null ? null : "Which product is the safest to push today?",
                        context.openExceptions().isEmpty() ? null : "Which orders need manual handling today?",
                        context.lowStockSnapshots().isEmpty() ? null : "Which products need replenishment first?",
                        topCampaign == null ? null : "Should a campaign be launched this week?"
                )),
                List.of(
                        new BusinessAssistantActionView("Open Business Analytics", "/app/business-analytics", "Review sales, profit, and anomaly trend."),
                        new BusinessAssistantActionView("Open Order Fulfillment Center", "/app/order-fulfillment-center", "Resolve the most urgent order execution issue."),
                        new BusinessAssistantActionView("Open Product Center", "/app/product-center", "Turn the top product recommendation into action.")
                ),
                List.of("recommendation-overview", "order-flow", "inventory-snapshot", "campaign-recommendation", "member-analysis")
        );
    }

    private BusinessAssistantResponseView buildAssistantResponse(AssistantContext context,
                                                                 AssistantIntent intent,
                                                                 String intentLabel,
                                                                 String title,
                                                                 String summary,
                                                                 String confidence,
                                                                 String confidenceReason,
                                                                 List<String> coverageLabels,
                                                                 List<String> matchedSignals,
                                                                 List<String> evidence,
                                                                 List<String> insights,
                                                                 List<String> followUpQuestions,
                                                                 List<BusinessAssistantActionView> actions,
                                                                 List<String> sourceLabels) {
        return new BusinessAssistantResponseView(
                context.responseId(),
                intent.name().toLowerCase(Locale.ROOT),
                intentLabel,
                context.question(),
                context.storeName(),
                title,
                summary,
                confidence,
                confidenceLabel(confidence),
                confidenceReason,
                coverageLabels,
                uniqueStrings(matchedSignals),
                uniqueStrings(evidence),
                uniqueStrings(insights),
                uniqueStrings(followUpQuestions),
                actions,
                uniqueStrings(sourceLabels),
                context.generatedAt()
        );
    }

    private AssistantIntent resolveIntent(String normalizedQuestion) {
        if (containsAny(normalizedQuestion, "ship", "delivery", "fulfillment", "发货", "物流", "履约")) {
            return AssistantIntent.FULFILLMENT;
        }
        if (containsAny(normalizedQuestion, "stock", "inventory", "replenish", "库存", "补货", "采购")) {
            return AssistantIntent.INVENTORY;
        }
        if (containsAny(normalizedQuestion, "campaign", "promotion", "活动", "促销", "拉新")) {
            return AssistantIntent.CAMPAIGN;
        }
        if (containsAny(normalizedQuestion, "member", "crm", "recall", "会员", "召回", "复购")) {
            return AssistantIntent.MEMBER;
        }
        if (containsAny(normalizedQuestion, "risk", "exception", "refund", "complaint", "异常", "风险", "退款", "投诉")) {
            return AssistantIntent.RISK;
        }
        if (containsAny(normalizedQuestion, "order", "timeout", "订单", "超时")) {
            return AssistantIntent.ORDER;
        }
        if (containsAny(normalizedQuestion, "product", "sku", "sell", "商品", "选品", "卖什么")) {
            return AssistantIntent.PRODUCT;
        }
        return AssistantIntent.SUMMARY;
    }

    private List<ProductRecommendationView> buildProductRecommendations(String tenantId, String storeId) {
        List<String> storeIds = resolveStoreIds(tenantId, storeId);
        Map<String, Long> recentPaidOrderCountByStore = orderRepository.findByStoreIds(storeIds).stream()
                .filter(order -> isRecentWithinDays(order.createdAt(), 30))
                .filter(order -> List.of("paid", "processing", "completed").contains(defaultIfBlank(order.orderStatus(), "pending")))
                .collect(Collectors.groupingBy(OrderMain::storeId, Collectors.counting()));
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
                .filter(Objects::nonNull)
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

    private boolean hasOpenException(String orderId, List<ExceptionTask> tasks) {
        return tasks.stream().anyMatch(task -> Objects.equals(orderId, task.relatedId()));
    }

    private boolean hasBlockedFulfillment(String orderId, List<FulfillmentTask> tasks) {
        return tasks.stream().anyMatch(task -> Objects.equals(orderId, task.orderId()));
    }

    private boolean isPastDue(OffsetDateTime time) {
        return time != null && !time.isAfter(OffsetDateTime.now());
    }

    private String productTitle(AssistantContext context, String productId) {
        Product product = context.productMap().get(productId);
        return product == null ? productId : product.title();
    }

    private String resolveConfidence(String normalizedQuestion, int evidenceCount) {
        int keywordCount = assistantKeywordCount(normalizedQuestion);
        if (keywordCount >= 2 && evidenceCount >= 2) {
            return "high";
        }
        if ((keywordCount >= 1 && evidenceCount >= 1) || evidenceCount >= 3) {
            return "medium";
        }
        return "low";
    }

    private String confidenceLabel(String confidence) {
        return switch (defaultIfBlank(confidence, "low")) {
            case "high" -> "high-confidence";
            case "medium" -> "medium-confidence";
            default -> "low-confidence";
        };
    }

    private String buildConfidenceReason(String normalizedQuestion, String dataSummary) {
        int keywordCount = assistantKeywordCount(normalizedQuestion);
        if (keywordCount > 0) {
            return "Matched %d keyword groups and %s.".formatted(keywordCount, dataSummary);
        }
        return "Used the summary path because no strong keyword group was matched and %s.".formatted(dataSummary);
    }

    private int assistantKeywordCount(String normalizedQuestion) {
        int count = 0;
        if (containsAny(normalizedQuestion, "product", "sku", "sell", "商品", "选品", "卖什么")) {
            count++;
        }
        if (containsAny(normalizedQuestion, "member", "crm", "recall", "会员", "召回", "复购")) {
            count++;
        }
        if (containsAny(normalizedQuestion, "order", "ship", "fulfillment", "订单", "发货", "履约")) {
            count++;
        }
        if (containsAny(normalizedQuestion, "stock", "inventory", "replenish", "库存", "补货", "采购")) {
            count++;
        }
        if (containsAny(normalizedQuestion, "campaign", "promotion", "活动", "促销", "拉新")) {
            count++;
        }
        if (containsAny(normalizedQuestion, "risk", "exception", "refund", "complaint", "风险", "异常", "退款", "投诉")) {
            count++;
        }
        return count;
    }

    private List<String> listOfNullable(String... items) {
        List<String> values = new ArrayList<>();
        for (String item : items) {
            values.add(item);
        }
        return values;
    }

    private List<String> uniqueStrings(List<String> items) {
        return items.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private int severityRank(String severity) {
        return switch (defaultIfBlank(severity, "low")) {
            case "critical" -> 4;
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replace("？", "")
                .replace("?", "")
                .replace("！", "")
                .replace("!", "")
                .replace("，", "")
                .replace(",", "")
                .replace("。", "")
                .replace(".", "")
                .replace("：", "")
                .replace(":", "")
                .trim();
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

    public record BusinessAssistantActionView(
            String label,
            String route,
            String description
    ) {
    }

    public record BusinessAssistantResponseView(
            String id,
            String intent,
            String intentLabel,
            String question,
            String storeName,
            String title,
            String summary,
            String confidence,
            String confidenceLabel,
            String confidenceReason,
            List<String> coverageLabels,
            List<String> matchedSignals,
            List<String> evidence,
            List<String> insights,
            List<String> followUpQuestions,
            List<BusinessAssistantActionView> actions,
            List<String> sourceLabels,
            OffsetDateTime createdAt
    ) {
    }

    private record AssistantContext(
            String responseId,
            String question,
            String storeName,
            List<ProductRecommendationView> productRecommendations,
            List<MemberRecommendationView> memberRecommendations,
            List<CampaignRecommendationView> campaignRecommendations,
            List<MemberProfile> memberProfiles,
            List<OrderMain> orders,
            List<ExceptionTask> openExceptions,
            List<FulfillmentTask> blockedFulfillmentTasks,
            List<InventorySnapshot> lowStockSnapshots,
            List<ReplenishmentTask> replenishmentTasks,
            List<CampaignActivity> campaigns,
            Map<String, Product> productMap,
            OffsetDateTime generatedAt
    ) {
    }

    private enum AssistantIntent {
        SUMMARY,
        PRODUCT,
        MEMBER,
        ORDER,
        RISK,
        FULFILLMENT,
        INVENTORY,
        CAMPAIGN
    }
}
