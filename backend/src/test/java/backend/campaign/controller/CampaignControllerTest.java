package backend.campaign.controller;

import backend.approval.application.ApprovalService;
import backend.audit.application.AuditLogService;
import backend.auth.dto.LoginResponse;
import backend.campaign.application.CampaignService;
import backend.common.api.ApiResponse;
import backend.notification.application.NotificationService;
import backend.organization.application.OrganizationService;
import backend.product.application.ProductService;
import backend.saas.application.SaasTenantService;
import backend.store.application.StoreChannelService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class CampaignControllerTest {

    private static final String OWNER_MOBILE = "13800000000";
    private static final String BOOTSTRAP_PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ProductService productService;

    @Autowired
    private StoreChannelService storeChannelService;

    @Autowired
    private SaasTenantService saasTenantService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        campaignService.clear();
        notificationService.clear();
        approvalService.clear();
        productService.clear();
        storeChannelService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldCreateUpdateApproveAndPublishCampaign() throws Exception {
        CampaignFixture fixture = prepareFixture("campaign-flow");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        String productId = preparePublishedProduct(fixture.storeId(), "campaign-a", ownerToken);

        MvcResult couponCreateResult = mockMvc.perform(post("/api/coupon-templates")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "templateName": "new-customer-offer",
                                  "discountType": "amount",
                                  "discountValue": 20,
                                  "thresholdAmount": 99,
                                  "status": "enabled"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("enabled"))
                .andReturn();
        String couponTemplateId = objectMapper.readTree(couponCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("couponTemplateId")
                .asText();

        mockMvc.perform(get("/api/coupon-templates")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].couponTemplateId").value(couponTemplateId));

        MvcResult campaignCreateResult = mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "activityType": "full_reduction",
                                  "activityName": "weekend-campaign",
                                  "startAt": "2026-06-01T00:00:00+08:00",
                                  "endAt": "2026-06-03T23:59:59+08:00",
                                  "productIds": ["%s"],
                                  "couponTemplateId": "%s",
                                  "rule": {
                                    "thresholdAmount": 99,
                                    "discountAmount": 20
                                  }
                                }
                                """.formatted(fixture.storeId(), productId, couponTemplateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.needApproval").value(true))
                .andReturn();
        String campaignId = objectMapper.readTree(campaignCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("campaignId")
                .asText();

        mockMvc.perform(get("/api/campaigns")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].campaignId").value(campaignId));

        mockMvc.perform(get("/api/campaigns/{id}", campaignId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.campaignId").value(campaignId))
                .andExpect(jsonPath("$.data.productIds[0]").value(productId))
                .andExpect(jsonPath("$.data.couponTemplateId").value(couponTemplateId));

        mockMvc.perform(put("/api/campaigns/{id}", campaignId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "activityType": "full_reduction",
                                  "activityName": "weekend-campaign-upgrade",
                                  "startAt": "2026-06-01T00:00:00+08:00",
                                  "endAt": "2026-06-04T23:59:59+08:00",
                                  "productIds": ["%s"],
                                  "couponTemplateId": "%s",
                                  "rule": {
                                    "thresholdAmount": 129,
                                    "discountAmount": 25
                                  }
                                }
                                """.formatted(fixture.storeId(), productId, couponTemplateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityName").value("weekend-campaign-upgrade"))
                .andExpect(jsonPath("$.data.rule.thresholdAmount").value(129))
                .andExpect(jsonPath("$.data.rule.discountAmount").value(25));

        mockMvc.perform(post("/api/campaigns/{id}/submit-approval", campaignId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_approval"));

        MvcResult approvalListResult = mockMvc.perform(get("/api/approvals")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].approvalType").value("campaign_publish"))
                .andExpect(jsonPath("$.data[0].relatedType").value("campaign_activity"))
                .andReturn();
        String approvalId = objectMapper.readTree(approvalListResult.getResponse().getContentAsString())
                .path("data")
                .path(0)
                .path("approvalId")
                .asText();

        mockMvc.perform(post("/api/approvals/{id}/approve", approvalId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "campaign approved"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"));

        mockMvc.perform(get("/api/campaigns/{id}", campaignId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"));

        mockMvc.perform(post("/api/campaigns/{id}/publish", campaignId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("published"))
                .andExpect(jsonPath("$.data.activityName").value("weekend-campaign-upgrade"));
    }

    @Test
    void shouldRejectCampaignAndBlockPublishBeforeApproval() throws Exception {
        CampaignFixture fixture = prepareFixture("campaign-reject");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        String productId = preparePublishedProduct(fixture.storeId(), "campaign-b", ownerToken);
        String campaignId = createCampaign(fixture.storeId(), productId, ownerToken);

        mockMvc.perform(post("/api/campaigns/{id}/publish", campaignId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("approved")));

        mockMvc.perform(post("/api/campaigns/{id}/submit-approval", campaignId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_approval"));

        MvcResult approvalListResult = mockMvc.perform(get("/api/approvals")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode approvals = objectMapper.readTree(approvalListResult.getResponse().getContentAsString()).path("data");
        String approvalId = findApprovalId(approvals, campaignId);

        mockMvc.perform(post("/api/approvals/{id}/reject", approvalId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "margin is too low"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("rejected"));

        mockMvc.perform(get("/api/campaigns/{id}", campaignId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("rejected"));
    }

    private CampaignFixture prepareFixture(String tenantName) throws Exception {
        JsonNode tenantData = registerTenant(tenantName);
        String tenantId = tenantData.path("tenantId").asText();
        String organizationId = tenantData.path("defaultOrganizationId").asText();
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);
        String storeId = connectStore(organizationId, "shop-" + tenantName, ownerToken);
        return new CampaignFixture(tenantId, organizationId, storeId);
    }

    private JsonNode registerTenant(String tenantName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
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
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String connectStore(String organizationId, String platformShopId, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stores/connect")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "douyin",
                                  "platformShopId": "%s",
                                  "shopName": "campaign-store",
                                  "profitThreshold": 18.50,
                                  "riskThreshold": 72.00
                                }
                                """.formatted(organizationId, platformShopId)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("storeId")
                .asText();
    }

    private String preparePublishedProduct(String storeId, String title, String token) throws Exception {
        String candidateProductId = createCandidateProduct(storeId, title, token);
        String productDraftId = generateProductDraft(candidateProductId, token);
        return publishProductDraft(productDraftId, "platform-" + title, token);
    }

    private String createCandidateProduct(String storeId, String title, String token) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/candidate-products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "sourceType": "1688",
                                  "sourceUrl": "https://source.example.com/%s",
                                  "title": "%s",
                                  "category": "cup",
                                  "estimatedProfit": 26.50,
                                  "riskLevel": "low",
                                  "recommendationReason": "good margin",
                                  "aiSummary": "fit for campaign"
                                }
                                """.formatted(storeId, title, title)))
                .andExpect(status().isOk())
                .andReturn();
        String candidateProductId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("candidateProductId")
                .asText();
        mockMvc.perform(post("/api/candidate-products/{id}/status", candidateProductId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "in_pool"
                                }
                                """))
                .andExpect(status().isOk());
        return candidateProductId;
    }

    private String generateProductDraft(String candidateProductId, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product-drafts/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "candidateProductId": "%s",
                                  "aiVersion": "ai-stage2-v1",
                                  "suggestedPrice": 99.90
                                }
                                """.formatted(candidateProductId)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("productDraftId")
                .asText();
    }

    private String publishProductDraft(String productDraftId, String platformProductId, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product-drafts/{id}/publish", productDraftId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformProductId": "%s"
                                }
                                """.formatted(platformProductId)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("productId")
                .asText();
    }

    private String createCampaign(String storeId, String productId, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "activityType": "full_reduction",
                                  "activityName": "test-campaign",
                                  "startAt": "2026-06-05T00:00:00+08:00",
                                  "endAt": "2026-06-07T23:59:59+08:00",
                                  "productIds": ["%s"],
                                  "rule": {
                                    "thresholdAmount": 129,
                                    "discountAmount": 30
                                  }
                                }
                                """.formatted(storeId, productId)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("campaignId")
                .asText();
    }

    private String findApprovalId(JsonNode approvals, String campaignId) {
        for (JsonNode approval : approvals) {
            if ("campaign_activity".equals(approval.path("relatedType").asText())
                    && campaignId.equals(approval.path("relatedId").asText())) {
                return approval.path("approvalId").asText();
            }
        }
        throw new IllegalStateException("campaign approval not found");
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
}

record CampaignFixture(String tenantId, String organizationId, String storeId) {
}

