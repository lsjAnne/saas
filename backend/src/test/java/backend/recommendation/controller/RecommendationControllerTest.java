package backend.recommendation.controller;

import backend.audit.application.AuditLogService;
import backend.auth.dto.LoginResponse;
import backend.campaign.application.CampaignService;
import backend.campaign.domain.repository.CampaignActivityRepository;
import backend.campaign.model.CampaignActivity;
import backend.common.api.ApiResponse;
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
import backend.order.application.OrderApplicationService;
import backend.order.domain.repository.OrderRepository;
import backend.order.model.OrderMain;
import backend.organization.application.OrganizationService;
import backend.product.application.ProductService;
import backend.product.domain.repository.CandidateProductRepository;
import backend.product.domain.repository.ProductRepository;
import backend.product.model.CandidateProduct;
import backend.product.model.Product;
import backend.saas.application.SaasTenantService;
import backend.store.application.StoreChannelService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class RecommendationControllerTest {

    private static final String OWNER_MOBILE = "13800000000";
    private static final String BOOTSTRAP_PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaasTenantService saasTenantService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private StoreChannelService storeChannelService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private OrderApplicationService orderApplicationService;

    @Autowired
    private CandidateProductRepository candidateProductRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private CampaignActivityRepository campaignActivityRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ExceptionTaskRepository exceptionTaskRepository;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Autowired
    private InventorySnapshotRepository inventorySnapshotRepository;

    @Autowired
    private ReplenishmentTaskRepository replenishmentTaskRepository;

    @BeforeEach
    void setUp() {
        orderApplicationService.clear();
        campaignService.clear();
        productService.clear();
        replenishmentTaskRepository.deleteAll();
        inventorySnapshotRepository.deleteAll();
        fulfillmentTaskRepository.deleteAll();
        exceptionTaskRepository.deleteAll();
        memberProfileRepository.deleteAll();
        campaignActivityRepository.deleteAll();
        orderRepository.deleteAll();
        storeChannelService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldExposeSmartRecommendationCenterViews() throws Exception {
        TenantFixture fixture = prepareStore("recommend-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);

        CandidateProduct bestCandidate = candidateProductRepository.save(new CandidateProduct(
                null,
                fixture.storeId(),
                "1688",
                "https://source.example.com/p/blender",
                "hash-blender",
                "travel-blender",
                "kitchen",
                "testable",
                BigDecimal.valueOf(39.90),
                "low",
                "portable summer demand",
                "fit for short video and live conversion",
                OffsetDateTime.now().minusDays(2)
        ));
        candidateProductRepository.save(new CandidateProduct(
                null,
                fixture.storeId(),
                "1688",
                "https://source.example.com/p/machine",
                "hash-machine",
                "heavy-machine",
                "hardware",
                "pending_review",
                BigDecimal.valueOf(12.00),
                "high",
                "industrial demand only",
                "long decision cycle",
                OffsetDateTime.now().minusDays(1)
        ));

        MemberProfile reviveVip = memberProfileRepository.save(new MemberProfile(
                null,
                fixture.storeId(),
                "customer-vip",
                "vip-buyer",
                "gold",
                8,
                BigDecimal.valueOf(1888.00),
                OffsetDateTime.now().minusDays(100),
                OffsetDateTime.now().minusDays(240)
        ));
        memberProfileRepository.save(new MemberProfile(
                null,
                fixture.storeId(),
                "customer-grow",
                "growth-buyer",
                "silver",
                4,
                BigDecimal.valueOf(699.00),
                OffsetDateTime.now().minusDays(7),
                OffsetDateTime.now().minusDays(90)
        ));

        orderRepository.save(new OrderMain(
                null,
                fixture.storeId(),
                "platform-order-1",
                "paid",
                "pending_fulfillment",
                BigDecimal.valueOf(299.00),
                BigDecimal.valueOf(69.00),
                "buyer-a",
                "138****0001",
                "hangzhou",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().minusDays(1)
        ));
        orderRepository.save(new OrderMain(
                null,
                fixture.storeId(),
                "platform-order-2",
                "paid",
                "shipped",
                BigDecimal.valueOf(199.00),
                BigDecimal.valueOf(48.00),
                "buyer-b",
                "138****0002",
                "ningbo",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().minusDays(2)
        ));

        campaignActivityRepository.save(new CampaignActivity(
                null,
                fixture.storeId(),
                "discount",
                "old-live-campaign",
                "published",
                OffsetDateTime.now().minusDays(3),
                OffsetDateTime.now().plusDays(2),
                List.of("product-seeded-1"),
                Map.of("discountRate", BigDecimal.valueOf(0.85)),
                null,
                true,
                OffsetDateTime.now().minusDays(5)
        ));

        mockMvc.perform(get("/api/recommendations/overview")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headline").value("smart_recommendation_ready"))
                .andExpect(jsonPath("$.data.productRecommendationCount").value(1))
                .andExpect(jsonPath("$.data.memberRecommendationCount").value(2))
                .andExpect(jsonPath("$.data.campaignRecommendationCount").value(3));

        mockMvc.perform(get("/api/recommendations/products")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].candidateProductId").value(bestCandidate.candidateProductId()))
                .andExpect(jsonPath("$.data[0].title").value("travel-blender"))
                .andExpect(jsonPath("$.data[0].suggestedAction").value("generate_product_draft"));

        mockMvc.perform(get("/api/recommendations/members")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memberId").value(reviveVip.memberId()))
                .andExpect(jsonPath("$.data[0].segmentCode").value("revive_vip"))
                .andExpect(jsonPath("$.data[0].suggestedAction").value("create_reactivation_touch_task"));

        mockMvc.perform(get("/api/recommendations/campaigns")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].campaignType").value("member_reactivation"))
                .andExpect(jsonPath("$.data[0].recommendedCandidateProductIds[0]").value(bestCandidate.candidateProductId()))
                .andExpect(jsonPath("$.data[0].targetMemberSegment").value("revive_vip"));
    }

    @Test
    void shouldAnswerBusinessAssistantQuestionFromServerSideContext() throws Exception {
        TenantFixture fixture = prepareStore("assistant-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);

        CandidateProduct candidate = candidateProductRepository.save(new CandidateProduct(
                null,
                fixture.storeId(),
                "1688",
                "https://source.example.com/p/assistant-kettle",
                "hash-assistant-kettle",
                "assistant-kettle",
                "kitchen",
                "in_pool",
                BigDecimal.valueOf(45.90),
                "low",
                "high live conversion margin",
                "good for digital-human live room",
                OffsetDateTime.now().minusDays(1)
        ));
        Product publishedProduct = productRepository.save(new Product(
                null,
                fixture.storeId(),
                "platform-assistant-001",
                "draft-1001",
                "assistant-kettle",
                "online",
                92,
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().minusDays(1)
        ));
        memberProfileRepository.save(new MemberProfile(
                null,
                fixture.storeId(),
                "customer-assistant-vip",
                "vip-customer",
                "gold",
                6,
                BigDecimal.valueOf(1666.00),
                OffsetDateTime.now().minusDays(80),
                OffsetDateTime.now().minusDays(180)
        ));
        OrderMain order = orderRepository.save(new OrderMain(
                null,
                fixture.storeId(),
                "assistant-order-001",
                "paid",
                "pending_fulfillment",
                BigDecimal.valueOf(299.00),
                BigDecimal.valueOf(66.00),
                "buyer-assistant",
                "138****1234",
                "hangzhou",
                OffsetDateTime.now().minusHours(1),
                OffsetDateTime.now().minusDays(1)
        ));
        exceptionTaskRepository.save(new ExceptionTask(
                null,
                fixture.storeId(),
                "order",
                order.orderId(),
                "fulfillment_timeout",
                "high",
                "new",
                "need manual fulfillment follow-up",
                null,
                OffsetDateTime.now().minusMinutes(30)
        ));
        fulfillmentTaskRepository.save(new FulfillmentTask(
                null,
                fixture.storeId(),
                order.orderId(),
                "idem-assistant-001",
                "processing",
                2,
                OffsetDateTime.now().minusMinutes(10),
                "channel authorization expired",
                OffsetDateTime.now().minusHours(2)
        ));
        inventorySnapshotRepository.save(new InventorySnapshot(
                null,
                fixture.storeId(),
                publishedProduct.productId(),
                "sku-1001",
                4,
                6,
                8,
                OffsetDateTime.now().minusMinutes(20),
                OffsetDateTime.now().minusMinutes(20)
        ));
        replenishmentTaskRepository.save(new ReplenishmentTask(
                null,
                fixture.storeId(),
                publishedProduct.productId(),
                "sku-1001",
                50,
                "draft",
                "not_required",
                "low stock with active live demand",
                OffsetDateTime.now().minusMinutes(15)
        ));
        campaignActivityRepository.save(new CampaignActivity(
                null,
                fixture.storeId(),
                "discount",
                "assistant-live-campaign",
                "published",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusDays(1),
                List.of(publishedProduct.productId()),
                Map.of("discountRate", BigDecimal.valueOf(0.88)),
                null,
                true,
                OffsetDateTime.now().minusDays(1)
        ));

        mockMvc.perform(post("/api/recommendations/assistant/ask")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "question": "哪些订单今天必须人工处理？"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("order"))
                .andExpect(jsonPath("$.data.storeName").value("recommend-store"))
                .andExpect(jsonPath("$.data.summary").value(org.hamcrest.Matchers.containsString(order.orderId())))
                .andExpect(jsonPath("$.data.evidence[1]").value(org.hamcrest.Matchers.containsString("order total")))
                .andExpect(jsonPath("$.data.insights[0]").value(org.hamcrest.Matchers.containsString(order.orderId())))
                .andExpect(jsonPath("$.data.actions[0].route").value("/app/order-fulfillment-center"));

        mockMvc.perform(post("/api/recommendations/assistant/ask")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "question": "哪些商品应该优先补货？"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("inventory"))
                .andExpect(jsonPath("$.data.title").value(org.hamcrest.Matchers.containsString("assistant-kettle")))
                .andExpect(jsonPath("$.data.insights[0]").value(org.hamcrest.Matchers.containsString("sku-1001")))
                .andExpect(jsonPath("$.data.actions[0].route").value("/app/inventory-replenishment-center"));
    }

    private TenantFixture prepareStore(String tenantName) throws Exception {
        MvcResult tenantResult = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "owner-a",
                                  "mobile": "13800000000"
                                }
                                """.formatted(tenantName)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tenantData = objectMapper.readTree(tenantResult.getResponse().getContentAsString()).path("data");
        String tenantId = tenantData.path("tenantId").asText();
        String organizationId = tenantData.path("defaultOrganizationId").asText();
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);

        MvcResult storeResult = mockMvc.perform(post("/api/stores/connect")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "douyin",
                                  "platformShopId": "shop-001",
                                  "shopName": "recommend-store",
                                  "profitThreshold": 18.00,
                                  "riskThreshold": 70.00
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk())
                .andReturn();
        String storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .path("data")
                .path("storeId")
                .asText();
        return new TenantFixture(tenantId, organizationId, storeId);
    }

    private String login(String username, String password) throws Exception {
        String content = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ApiResponse<LoginResponse> response = objectMapper.readValue(
                content,
                new TypeReference<>() {
                }
        );
        return response.data().token();
    }

    private record TenantFixture(String tenantId, String organizationId, String storeId) {
    }
}

