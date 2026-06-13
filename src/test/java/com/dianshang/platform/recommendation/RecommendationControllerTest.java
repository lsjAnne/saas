package com.dianshang.platform.recommendation;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.auth.dto.LoginResponse;
import com.dianshang.platform.campaign.application.CampaignService;
import com.dianshang.platform.campaign.domain.repository.CampaignActivityRepository;
import com.dianshang.platform.campaign.model.CampaignActivity;
import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.member.domain.repository.MemberProfileRepository;
import com.dianshang.platform.member.model.MemberProfile;
import com.dianshang.platform.order.application.OrderApplicationService;
import com.dianshang.platform.order.domain.repository.OrderRepository;
import com.dianshang.platform.order.model.OrderMain;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.product.application.ProductService;
import com.dianshang.platform.product.domain.repository.CandidateProductRepository;
import com.dianshang.platform.product.model.CandidateProduct;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.store.application.StoreChannelService;
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
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private CampaignActivityRepository campaignActivityRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderApplicationService.clear();
        campaignService.clear();
        productService.clear();
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
