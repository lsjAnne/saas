package com.dianshang.platform.live;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.auth.dto.LoginResponse;
import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.live.application.LiveService;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.store.application.StoreChannelService;
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

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class LiveControllerTest {

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
    private LiveService liveService;

    @BeforeEach
    void setUp() {
        liveService.clear();
        storeChannelService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldCreateListGetLivePlanGenerateScriptAndListLiveAccounts() throws Exception {
        LiveFixture fixture = prepareFixture(false);

        mockMvc.perform(get("/api/live-accounts")
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].liveAccountId").value(fixture.liveAccountId()))
                .andExpect(jsonPath("$.data[0].occupied").value(false));

        MvcResult createPlanResult = mockMvc.perform(post("/api/live-plans")
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "liveAccountId": "%s",
                                  "planName": "live-plan-a",
                                  "scheduledStartAt": "2026-06-01T10:00:00+08:00",
                                  "scheduledEndAt": "2026-06-01T12:00:00+08:00",
                                  "anchorProfileName": "default-anchor"
                                }
                                """.formatted(fixture.storeId(), fixture.liveAccountId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("draft"))
                .andReturn();

        String livePlanId = objectMapper.readTree(createPlanResult.getResponse().getContentAsString())
                .path("data")
                .path("livePlanId")
                .asText();

        mockMvc.perform(get("/api/live-plans")
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].livePlanId").value(livePlanId));

        mockMvc.perform(get("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("draft"))
                .andExpect(jsonPath("$.data.liveAccountId").value(fixture.liveAccountId()));

        mockMvc.perform(post("/api/live-plans/{id}/generate-script", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.livePlanId").value(livePlanId))
                .andExpect(jsonPath("$.data.scriptVersion").value("v1"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.scriptContent").value(Matchers.containsString("live-plan-a")));

        mockMvc.perform(post("/api/live-plans/{id}/review-script", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.livePlanId").value(livePlanId))
                .andExpect(jsonPath("$.data.scriptVersion").value("v1"))
                .andExpect(jsonPath("$.data.reviewed").value(true))
                .andExpect(jsonPath("$.data.publishable").value(true))
                .andExpect(jsonPath("$.data.scriptSnippet").value(Matchers.containsString("live-plan-a")));

        mockMvc.perform(post("/api/live-plans/{id}/publish", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.livePlanId").value(livePlanId))
                .andExpect(jsonPath("$.data.planStatus").value("ready"));

        mockMvc.perform(get("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("ready"));
    }

    @Test
    void shouldGetLiveProductPoolForPlan() throws Exception {
        LiveFixture fixture = prepareFixture(false);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-product-pool");
        String candidateOnlyId = createCandidateProduct(fixture.ownerToken(), fixture.storeId(), "candidate-only", "testable");
        String candidatePublishedId = createCandidateProduct(fixture.ownerToken(), fixture.storeId(), "candidate-published", "in_pool");
        String draftId = generateProductDraft(fixture.ownerToken(), candidatePublishedId);
        publishProductDraft(fixture.ownerToken(), draftId, "platform-live-001");

        mockMvc.perform(get("/api/live-plans/{id}/product-pool", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].productId").isNotEmpty())
                .andExpect(jsonPath("$.data[0].candidateProductId").value(candidatePublishedId))
                .andExpect(jsonPath("$.data[0].productStatus").value("online"))
                .andExpect(jsonPath("$.data[1].candidateProductId").value(candidateOnlyId))
                .andExpect(jsonPath("$.data[1].productId").isEmpty())
                .andExpect(jsonPath("$.data[1].candidateStatus").value("testable"));
    }

    @Test
    void shouldBindListAndRemoveLivePlanProducts() throws Exception {
        LiveFixture fixture = prepareFixture(false);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-product-bind");
        String candidateProductId = createCandidateProduct(fixture.ownerToken(), fixture.storeId(), "candidate-bind", "in_pool");
        String productDraftId = generateProductDraft(fixture.ownerToken(), candidateProductId);
        String productId = publishProductDraft(fixture.ownerToken(), productDraftId, "platform-live-bind-001");

        MvcResult bindResult = bindLiveProduct(fixture.ownerToken(), livePlanId, productId, null, null, 10);
        String liveProductItemId = objectMapper.readTree(bindResult.getResponse().getContentAsString())
                .path("data")
                .path("liveProductItemId")
                .asText();

        mockMvc.perform(get("/api/live-plans/{id}/products", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].liveProductItemId").value(liveProductItemId))
                .andExpect(jsonPath("$.data[0].productId").value(productId))
                .andExpect(jsonPath("$.data[0].productDraftId").value(productDraftId))
                .andExpect(jsonPath("$.data[0].candidateProductId").value(candidateProductId))
                .andExpect(jsonPath("$.data[0].sortOrder").value(10))
                .andExpect(jsonPath("$.data[0].title").value(Matchers.containsString("candidate-bind")));

        mockMvc.perform(delete("/api/live-plans/{id}/products/{itemId}", livePlanId, liveProductItemId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liveProductItemId").value(liveProductItemId))
                .andExpect(jsonPath("$.data.productId").value(productId));

        mockMvc.perform(get("/api/live-plans/{id}/products", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRebuildActiveScriptAfterRemovingBoundProduct() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-remove-rebuild");
        PublishedLiveProductFixture firstProduct = preparePublishedLiveProduct(fixture.ownerToken(), fixture.storeId(), "remove-rebuild-a", "in_pool");
        PublishedLiveProductFixture secondProduct = preparePublishedLiveProduct(fixture.ownerToken(), fixture.storeId(), "remove-rebuild-b", "in_pool");
        MvcResult firstBindResult = bindLiveProduct(fixture.ownerToken(), livePlanId, firstProduct.productId(), null, null, 10);
        String firstItemId = objectMapper.readTree(firstBindResult.getResponse().getContentAsString())
                .path("data")
                .path("liveProductItemId")
                .asText();
        bindLiveProduct(fixture.ownerToken(), livePlanId, secondProduct.productId(), null, null, 20);
        generateScript(fixture.ownerToken(), livePlanId);

        mockMvc.perform(delete("/api/live-plans/{id}/products/{itemId}", livePlanId, firstItemId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(firstProduct.productId()));

        mockMvc.perform(get("/api/live-plans/{id}/products", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productId").value(secondProduct.productId()));

        MvcResult startResult = startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());
        String liveSessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data")
                .path("liveSessionId")
                .asText();

        mockMvc.perform(get("/api/live-sessions/{id}/status", liveSessionId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeScriptVersion").value("v2"))
                .andExpect(jsonPath("$.data.currentProductId").value(secondProduct.productId()))
                .andExpect(jsonPath("$.data.currentScriptSnippet").value(Matchers.containsString(secondProduct.productTitle())))
                .andExpect(jsonPath("$.data.currentScriptSnippet", Matchers.not(Matchers.containsString(firstProduct.productTitle()))));
    }

    @Test
    void shouldUpgradeExistingBindingWhenProductSourceBecomesPublished() throws Exception {
        LiveFixture fixture = prepareFixture(false);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-bind-upgrade");
        String candidateProductId = createCandidateProduct(fixture.ownerToken(), fixture.storeId(), "candidate-upgrade", "testable");

        MvcResult firstBindResult = bindLiveProduct(fixture.ownerToken(), livePlanId, null, null, candidateProductId, 30);
        String liveProductItemId = objectMapper.readTree(firstBindResult.getResponse().getContentAsString())
                .path("data")
                .path("liveProductItemId")
                .asText();

        String productDraftId = generateProductDraft(fixture.ownerToken(), candidateProductId);
        String productId = publishProductDraft(fixture.ownerToken(), productDraftId, "platform-live-upgrade-001");

        mockMvc.perform(post("/api/live-plans/{id}/products/bind", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "sortOrder": 5
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liveProductItemId").value(liveProductItemId))
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.productDraftId").value(productDraftId))
                .andExpect(jsonPath("$.data.candidateProductId").value(candidateProductId))
                .andExpect(jsonPath("$.data.sortOrder").value(5));

        mockMvc.perform(get("/api/live-plans/{id}/products", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].liveProductItemId").value(liveProductItemId))
                .andExpect(jsonPath("$.data[0].productId").value(productId))
                .andExpect(jsonPath("$.data[0].sortOrder").value(5));
    }

    @Test
    void shouldGenerateScriptFromBoundProductsInOrder() throws Exception {
        LiveFixture fixture = prepareFixture(false);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-script-bound");
        PublishedLiveProductFixture firstProduct = preparePublishedLiveProduct(fixture.ownerToken(), fixture.storeId(), "script-bound-a", "in_pool");
        PublishedLiveProductFixture secondProduct = preparePublishedLiveProduct(fixture.ownerToken(), fixture.storeId(), "script-bound-b", "in_pool");
        bindLiveProduct(fixture.ownerToken(), livePlanId, firstProduct.productId(), null, null, 10);
        bindLiveProduct(fixture.ownerToken(), livePlanId, secondProduct.productId(), null, null, 20);

        mockMvc.perform(post("/api/live-plans/{id}/generate-script", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(firstProduct.productId()))
                .andExpect(jsonPath("$.data.scriptContent").value(Matchers.containsString("Segment 01")))
                .andExpect(jsonPath("$.data.scriptContent").value(Matchers.containsString("Segment 02")));
    }

    @Test
    void shouldSkipCurrentBoundProductAndAdvanceToNextBoundProduct() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-skip-bound");
        PublishedLiveProductFixture firstProduct = preparePublishedLiveProduct(fixture.ownerToken(), fixture.storeId(), "skip-bound-a", "in_pool");
        PublishedLiveProductFixture secondProduct = preparePublishedLiveProduct(fixture.ownerToken(), fixture.storeId(), "skip-bound-b", "in_pool");
        bindLiveProduct(fixture.ownerToken(), livePlanId, firstProduct.productId(), null, null, 10);
        bindLiveProduct(fixture.ownerToken(), livePlanId, secondProduct.productId(), null, null, 20);
        generateScript(fixture.ownerToken(), livePlanId);
        MvcResult startResult = startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());
        String liveSessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data")
                .path("liveSessionId")
                .asText();

        skipCurrentProduct(fixture.ownerToken(), liveSessionId)
                .andExpect(jsonPath("$.data.skippedScriptSnippet").value(Matchers.containsString(firstProduct.productTitle())))
                .andExpect(jsonPath("$.data.nextScriptSnippet").value(Matchers.containsString(secondProduct.productTitle())))
                .andExpect(jsonPath("$.data.activeScriptVersion").value("v2"));

        mockMvc.perform(get("/api/live-sessions/{id}/status", liveSessionId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeScriptVersion").value("v2"))
                .andExpect(jsonPath("$.data.currentProductId").value(secondProduct.productId()))
                .andExpect(jsonPath("$.data.currentProductTitle").value(secondProduct.productTitle()))
                .andExpect(jsonPath("$.data.currentProductSourceType").value("1688"))
                .andExpect(jsonPath("$.data.currentScriptSnippet").value(Matchers.containsString(secondProduct.productTitle())));
    }

    @Test
    void shouldManageAdvancedLiveGovernanceControls() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-governance");
        PublishedLiveProductFixture product = preparePublishedLiveProduct(fixture.ownerToken(), fixture.storeId(), "governance-a", "in_pool");
        bindLiveProduct(fixture.ownerToken(), livePlanId, product.productId(), null, null, 10);
        generateScript(fixture.ownerToken(), livePlanId);
        mockMvc.perform(post("/api/live-plans/{id}/publish", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("ready"));

        MvcResult startResult = startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());
        String liveSessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data")
                .path("liveSessionId")
                .asText();

        mockMvc.perform(post("/api/live-sessions/{id}/strong-control", liveSessionId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "controlMode": "strict_control",
                                  "reason": "price commitment risk"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.controlMode").value("strict_control"));

        mockMvc.perform(post("/api/live-sessions/{id}/switch-scene", liveSessionId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetScene": "flash_sale",
                                  "reason": "boost conversion"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentScene").value("flash_sale"));

        mockMvc.perform(post("/api/live-sessions/{id}/manual-takeover", liveSessionId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "takeoverOperator": "risk-operator",
                                  "reason": "human intervention required"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.takeoverStatus").value("manual"))
                .andExpect(jsonPath("$.data.takeoverOperator").value("risk-operator"));

        mockMvc.perform(post("/api/live-sessions/{id}/promise-audit", liveSessionId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "promiseText": "guaranteed lowest price",
                                  "riskLevel": "high",
                                  "decision": "rejected",
                                  "remark": "need human review"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.promiseAuditStatus").value("rejected"))
                .andExpect(jsonPath("$.data.promiseAuditRemark").value("need human review"));

        mockMvc.perform(get("/api/live-sessions/{id}/status", liveSessionId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.controlMode").value("strict_control"))
                .andExpect(jsonPath("$.data.currentScene").value("flash_sale"))
                .andExpect(jsonPath("$.data.takeoverStatus").value("manual"))
                .andExpect(jsonPath("$.data.takeoverOperator").value("risk-operator"))
                .andExpect(jsonPath("$.data.promiseAuditStatus").value("rejected"))
                .andExpect(jsonPath("$.data.promiseAuditRemark").value("need human review"));
    }

    @Test
    void shouldReturnCurrentBoundProductInLiveSessionStatus() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-status-bound");
        PublishedLiveProductFixture firstProduct = preparePublishedLiveProduct(fixture.ownerToken(), fixture.storeId(), "status-bound-a", "in_pool");
        PublishedLiveProductFixture secondProduct = preparePublishedLiveProduct(fixture.ownerToken(), fixture.storeId(), "status-bound-b", "in_pool");
        bindLiveProduct(fixture.ownerToken(), livePlanId, firstProduct.productId(), null, null, 10);
        bindLiveProduct(fixture.ownerToken(), livePlanId, secondProduct.productId(), null, null, 20);
        generateScript(fixture.ownerToken(), livePlanId);
        MvcResult startResult = startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());
        String liveSessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data")
                .path("liveSessionId")
                .asText();

        mockMvc.perform(get("/api/live-sessions/{id}/status", liveSessionId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeScriptVersion").value("v1"))
                .andExpect(jsonPath("$.data.currentProductId").value(firstProduct.productId()))
                .andExpect(jsonPath("$.data.currentProductTitle").value(firstProduct.productTitle()))
                .andExpect(jsonPath("$.data.currentProductSourceType").value("1688"))
                .andExpect(jsonPath("$.data.currentScriptSnippet").value(Matchers.containsString(firstProduct.productTitle())));
    }

    @Test
    void shouldRejectBindProductWhenPlanStatusNotEditable() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-bind-running");
        String candidateProductId = createCandidateProduct(fixture.ownerToken(), fixture.storeId(), "candidate-running", "in_pool");
        String productDraftId = generateProductDraft(fixture.ownerToken(), candidateProductId);
        String productId = publishProductDraft(fixture.ownerToken(), productDraftId, "platform-live-running-001");
        generateScript(fixture.ownerToken(), livePlanId);
        startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());

        mockMvc.perform(post("/api/live-plans/{id}/products/bind", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "sortOrder": 1
                                }
                                """.formatted(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("7121"));
    }

    @Test
    void shouldRejectBindProductFromAnotherStore() throws Exception {
        LiveFixture fixture = prepareFixture(false);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-bind-store-check");
        String secondStoreId = connectStore(fixture.ownerToken(), fixture.organizationId(), "shop-live-002");
        String candidateProductId = createCandidateProduct(fixture.ownerToken(), secondStoreId, "candidate-other-store", "in_pool");
        String productDraftId = generateProductDraft(fixture.ownerToken(), candidateProductId);
        String productId = publishProductDraft(fixture.ownerToken(), productDraftId, "platform-live-store-check-001");

        mockMvc.perform(post("/api/live-plans/{id}/products/bind", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "sortOrder": 1
                                }
                                """.formatted(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("7123"));
    }

    @Test
    void shouldRejectPublishWhenActiveScriptMissing() throws Exception {
        LiveFixture fixture = prepareFixture(false);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-no-script");

        mockMvc.perform(post("/api/live-plans/{id}/publish", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("7116"));
    }

    @Test
    void shouldUpdateLivePlanWhenStatusEditable() throws Exception {
        LiveFixture fixture = prepareFixture(false);
        String secondAccountId = createChannelAccount(fixture.ownerToken(), fixture.organizationId(), "live-account-update");
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-update");

        mockMvc.perform(put("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "liveAccountId": "%s",
                                  "planName": "live-plan-update-v2",
                                  "scheduledStartAt": "2026-06-02T11:00:00+08:00",
                                  "scheduledEndAt": "2026-06-02T13:00:00+08:00",
                                  "anchorProfileName": "anchor-v2"
                                }
                                """.formatted(secondAccountId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.livePlanId").value(livePlanId))
                .andExpect(jsonPath("$.data.liveAccountId").value(secondAccountId))
                .andExpect(jsonPath("$.data.planName").value("live-plan-update-v2"))
                .andExpect(jsonPath("$.data.anchorProfileName").value("anchor-v2"))
                .andExpect(jsonPath("$.data.planStatus").value("draft"));

        mockMvc.perform(get("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liveAccountId").value(secondAccountId))
                .andExpect(jsonPath("$.data.planName").value("live-plan-update-v2"));
    }

    @Test
    void shouldRejectValidateConcurrencyWhenLiveAccountOccupied() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-b1");
        liveService.createRunningSessionSeed(fixture.tenantId(), livePlanId, fixture.liveAccountId());

        mockMvc.perform(post("/api/live-plans/{id}/validate-concurrency", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.accountOccupied").value(true))
                .andExpect(jsonPath("$.data.tenantQuotaExceeded").value(false))
                .andExpect(jsonPath("$.data.tenantQuotaLimit").value(2))
                .andExpect(jsonPath("$.data.reason").value("live account occupied"));
    }

    @Test
    void shouldRejectValidateConcurrencyWhenTenantQuotaExceeded() throws Exception {
        LiveFixture fixture = prepareFixture(false);
        String secondAccountId = createChannelAccount(fixture.ownerToken(), fixture.organizationId(), "live-account-b");
        String occupiedPlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-c1");
        String candidatePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), secondAccountId, "live-plan-c2");
        liveService.createRunningSessionSeed(fixture.tenantId(), occupiedPlanId, fixture.liveAccountId());

        mockMvc.perform(post("/api/live-plans/{id}/validate-concurrency", candidatePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.accountOccupied").value(false))
                .andExpect(jsonPath("$.data.tenantQuotaExceeded").value(true))
                .andExpect(jsonPath("$.data.tenantRunningCount").value(1))
                .andExpect(jsonPath("$.data.tenantQuotaLimit").value(1))
                .andExpect(jsonPath("$.data.reason").value("tenant live concurrency quota exceeded"));
    }

    @Test
    void shouldStartPauseStopLiveSessionAndListSessions() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-d1");

        generateScript(fixture.ownerToken(), livePlanId);
        MvcResult startResult = startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());

        String liveSessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data")
                .path("liveSessionId")
                .asText();

        mockMvc.perform(get("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("running"));

        mockMvc.perform(get("/api/live-sessions")
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].liveSessionId").value(liveSessionId))
                .andExpect(jsonPath("$.data[0].sessionStatus").value("running"));

        mockMvc.perform(post("/api/live-plans/{id}/pause", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liveSessionId").value(liveSessionId))
                .andExpect(jsonPath("$.data.sessionStatus").value("paused"));

        mockMvc.perform(get("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("ready"));

        mockMvc.perform(post("/api/live-plans/{id}/stop", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liveSessionId").value(liveSessionId))
                .andExpect(jsonPath("$.data.sessionStatus").value("ended"));

        mockMvc.perform(get("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("ended"));
    }

    @Test
    void shouldRejectUpdateLivePlanWhenRunning() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-running-update");
        generateScript(fixture.ownerToken(), livePlanId);
        startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());

        mockMvc.perform(put("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "liveAccountId": "%s",
                                  "planName": "live-plan-running-update-v2",
                                  "scheduledStartAt": "2026-06-03T11:00:00+08:00",
                                  "scheduledEndAt": "2026-06-03T13:00:00+08:00",
                                  "anchorProfileName": "anchor-running"
                                }
                                """.formatted(fixture.liveAccountId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("7110"));
    }

    @Test
    void shouldScheduleLivePlanAndRunDuePlan() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-scheduled-1");
        String startAt = OffsetDateTime.now().minusMinutes(2).toString();
        String endAt = OffsetDateTime.now().plusHours(1).toString();
        updatePlan(fixture.ownerToken(), livePlanId, fixture.liveAccountId(), "live-plan-scheduled-1", startAt, endAt, "scheduled-anchor");
        generateScript(fixture.ownerToken(), livePlanId);

        schedulePlan(fixture.ownerToken(), livePlanId)
                .andExpect(jsonPath("$.data.planStatus").value("scheduled"));

        mockMvc.perform(post("/api/live-plans/run-due")
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scannedCount").value(1))
                .andExpect(jsonPath("$.data.startedCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.startedSessions.length()").value(1))
                .andExpect(jsonPath("$.data.startedSessions[0].livePlanId").value(livePlanId));

        mockMvc.perform(get("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("running"));
    }

    @Test
    void shouldResumePausedLiveSession() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-resume-1");
        generateScript(fixture.ownerToken(), livePlanId);
        MvcResult startResult = startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());
        String liveSessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data")
                .path("liveSessionId")
                .asText();

        mockMvc.perform(post("/api/live-plans/{id}/pause", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liveSessionId").value(liveSessionId))
                .andExpect(jsonPath("$.data.sessionStatus").value("paused"));

        resumeLive(fixture.ownerToken(), livePlanId)
                .andExpect(jsonPath("$.data.liveSessionId").value(liveSessionId))
                .andExpect(jsonPath("$.data.sessionStatus").value("running"));

        mockMvc.perform(get("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("running"));
    }

    @Test
    void shouldRejectResumeWhenLiveAccountOccupied() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String pausedPlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-resume-paused");
        generateScript(fixture.ownerToken(), pausedPlanId);
        startLive(fixture.ownerToken(), pausedPlanId, fixture.liveAccountId());
        mockMvc.perform(post("/api/live-plans/{id}/pause", pausedPlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk());

        String runningPlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-resume-running");
        generateScript(fixture.ownerToken(), runningPlanId);
        startLive(fixture.ownerToken(), runningPlanId, fixture.liveAccountId());

        mockMvc.perform(post("/api/live-plans/{id}/resume", pausedPlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("7102"));
    }

    @Test
    void shouldDuplicateLivePlanAsDraftWithCopiedBoundProductsButWithoutCopyingScript() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-duplicate-source");
        PublishedLiveProductFixture firstProduct = preparePublishedLiveProduct(fixture.ownerToken(), fixture.storeId(), "duplicate-bound-a", "in_pool");
        PublishedLiveProductFixture secondProduct = preparePublishedLiveProduct(fixture.ownerToken(), fixture.storeId(), "duplicate-bound-b", "in_pool");
        bindLiveProduct(fixture.ownerToken(), livePlanId, firstProduct.productId(), null, null, 10);
        bindLiveProduct(fixture.ownerToken(), livePlanId, secondProduct.productId(), null, null, 20);
        generateScript(fixture.ownerToken(), livePlanId);

        MvcResult duplicateResult = duplicatePlan(fixture.ownerToken(), livePlanId);
        String duplicatedPlanId = objectMapper.readTree(duplicateResult.getResponse().getContentAsString())
                .path("data")
                .path("livePlanId")
                .asText();

        mockMvc.perform(get("/api/live-plans/{id}", duplicatedPlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.livePlanId").value(duplicatedPlanId))
                .andExpect(jsonPath("$.data.planStatus").value("draft"))
                .andExpect(jsonPath("$.data.planName").value("live-plan-duplicate-source-copy"))
                .andExpect(jsonPath("$.data.storeId").value(fixture.storeId()))
                .andExpect(jsonPath("$.data.liveAccountId").value(fixture.liveAccountId()));

        mockMvc.perform(get("/api/live-plans/{id}/products", duplicatedPlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].productId").value(firstProduct.productId()))
                .andExpect(jsonPath("$.data[1].productId").value(secondProduct.productId()));

        mockMvc.perform(post("/api/live-plans/{id}/generate-script", duplicatedPlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.livePlanId").value(duplicatedPlanId))
                .andExpect(jsonPath("$.data.scriptVersion").value("v1"))
                .andExpect(jsonPath("$.data.productId").value(firstProduct.productId()))
                .andExpect(jsonPath("$.data.scriptContent").value(Matchers.containsString("Segment 02")));
    }

    @Test
    void shouldCancelScheduledPlanAndRejectCancelRunningPlan() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String cancelPlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-cancel-ready");
        String startAt = OffsetDateTime.now().plusMinutes(10).toString();
        String endAt = OffsetDateTime.now().plusHours(2).toString();
        updatePlan(fixture.ownerToken(), cancelPlanId, fixture.liveAccountId(), "live-plan-cancel-ready", startAt, endAt, "anchor-cancel");
        generateScript(fixture.ownerToken(), cancelPlanId);
        schedulePlan(fixture.ownerToken(), cancelPlanId);

        cancelPlan(fixture.ownerToken(), cancelPlanId)
                .andExpect(jsonPath("$.data.livePlanId").value(cancelPlanId))
                .andExpect(jsonPath("$.data.planStatus").value("cancelled"));

        mockMvc.perform(get("/api/live-plans/{id}", cancelPlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("cancelled"));

        String runningPlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-cancel-running");
        generateScript(fixture.ownerToken(), runningPlanId);
        startLive(fixture.ownerToken(), runningPlanId, fixture.liveAccountId());

        mockMvc.perform(post("/api/live-plans/{id}/cancel", runningPlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("7113"));
    }

    @Test
    void shouldSkipCurrentProductAndAdvanceActiveScriptSegment() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-skip-1");
        generateScript(fixture.ownerToken(), livePlanId);
        MvcResult startResult = startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());
        String liveSessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data")
                .path("liveSessionId")
                .asText();

        skipCurrentProduct(fixture.ownerToken(), liveSessionId).andReturn();

        mockMvc.perform(get("/api/live-sessions/{id}/status", liveSessionId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liveSessionId").value(liveSessionId))
                .andExpect(jsonPath("$.data.activeScriptVersion").value("v2"))
                .andExpect(jsonPath("$.data.currentScriptSnippet", Matchers.containsString("default-anchor")))
                .andExpect(jsonPath("$.data.currentScriptSnippet", Matchers.not(Matchers.containsString("live-store"))));
    }

    @Test
    void shouldRejectSkipCurrentProductWhenNoNextSegmentAvailable() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-skip-2");
        generateScript(fixture.ownerToken(), livePlanId);
        MvcResult startResult = startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());
        String liveSessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data")
                .path("liveSessionId")
                .asText();

        skipCurrentProduct(fixture.ownerToken(), liveSessionId)
                .andExpect(jsonPath("$.data.activeScriptVersion").value("v2"));
        skipCurrentProduct(fixture.ownerToken(), liveSessionId)
                .andExpect(jsonPath("$.data.activeScriptVersion").value("v3"));
        skipCurrentProduct(fixture.ownerToken(), liveSessionId)
                .andExpect(jsonPath("$.data.activeScriptVersion").value("v4"));

        mockMvc.perform(post("/api/live-sessions/{id}/skip-current-product", liveSessionId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("7115"));
    }

    @Test
    void shouldGetLiveSessionConcurrencyOverview() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String secondAccountId = createChannelAccount(fixture.ownerToken(), fixture.organizationId(), "live-account-b");
        String firstPlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-e1");
        String secondPlanId = createPlan(fixture.ownerToken(), fixture.storeId(), secondAccountId, "live-plan-e2");
        liveService.createRunningSessionSeed(fixture.tenantId(), firstPlanId, fixture.liveAccountId());
        liveService.createRunningSessionSeed(fixture.tenantId(), secondPlanId, secondAccountId);

        mockMvc.perform(get("/api/live-sessions/concurrency-overview")
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(fixture.tenantId()))
                .andExpect(jsonPath("$.data.runningSessionCount").value(2))
                .andExpect(jsonPath("$.data.tenantQuotaLimit").value(2))
                .andExpect(jsonPath("$.data.remainingQuota").value(0))
                .andExpect(jsonPath("$.data.runningSessions.length()").value(2))
                .andExpect(jsonPath("$.data.occupiedAccounts.length()").value(2))
                .andExpect(jsonPath("$.data.runningSessions[*].livePlanId", Matchers.hasItems(firstPlanId, secondPlanId)))
                .andExpect(jsonPath("$.data.occupiedAccounts[*].liveAccountId", Matchers.hasItems(fixture.liveAccountId(), secondAccountId)));
    }

    @Test
    void shouldGetRunningLiveSessionStatusDetail() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-status-1");
        generateScript(fixture.ownerToken(), livePlanId);
        MvcResult startResult = startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());
        String liveSessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data")
                .path("liveSessionId")
                .asText();

        mockMvc.perform(get("/api/live-sessions/{id}/status", liveSessionId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liveSessionId").value(liveSessionId))
                .andExpect(jsonPath("$.data.livePlanId").value(livePlanId))
                .andExpect(jsonPath("$.data.planStatus").value("running"))
                .andExpect(jsonPath("$.data.sessionStatus").value("running"))
                .andExpect(jsonPath("$.data.liveAccountId").value(fixture.liveAccountId()))
                .andExpect(jsonPath("$.data.liveAccountName").value("live-account-a"))
                .andExpect(jsonPath("$.data.accountOccupied").value(true))
                .andExpect(jsonPath("$.data.tenantQuotaLimit").value(2))
                .andExpect(jsonPath("$.data.tenantRemainingQuota").value(1))
                .andExpect(jsonPath("$.data.activeScriptVersion").value("v1"))
                .andExpect(jsonPath("$.data.currentScriptSnippet").value(Matchers.containsString("live-plan-status-1")))
                .andExpect(jsonPath("$.data.durationSeconds").isNumber());
    }

    @Test
    void shouldRunDuePlansWithPartialFailureWhenAccountOccupied() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String secondAccountId = createChannelAccount(fixture.ownerToken(), fixture.organizationId(), "live-account-c");
        String occupiedPlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-occupied-seed");
        generateScript(fixture.ownerToken(), occupiedPlanId);
        startLive(fixture.ownerToken(), occupiedPlanId, fixture.liveAccountId());

        String blockedPlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-due-blocked");
        String runnablePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), secondAccountId, "live-plan-due-runnable");
        String startAt = OffsetDateTime.now().minusMinutes(3).toString();
        String endAt = OffsetDateTime.now().plusHours(1).toString();
        updatePlan(fixture.ownerToken(), blockedPlanId, fixture.liveAccountId(), "live-plan-due-blocked", startAt, endAt, "anchor-blocked");
        updatePlan(fixture.ownerToken(), runnablePlanId, secondAccountId, "live-plan-due-runnable", startAt, endAt, "anchor-runnable");
        generateScript(fixture.ownerToken(), blockedPlanId);
        generateScript(fixture.ownerToken(), runnablePlanId);

        schedulePlan(fixture.ownerToken(), blockedPlanId);
        schedulePlan(fixture.ownerToken(), runnablePlanId);

        mockMvc.perform(post("/api/live-plans/run-due")
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scannedCount").value(2))
                .andExpect(jsonPath("$.data.startedCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.startedSessions[0].livePlanId").value(runnablePlanId))
                .andExpect(jsonPath("$.data.failures[0].livePlanId").value(blockedPlanId))
                .andExpect(jsonPath("$.data.failures[0].reason").value("live account occupied"));

        mockMvc.perform(get("/api/live-plans/{id}", blockedPlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("scheduled"));

        mockMvc.perform(get("/api/live-plans/{id}", runnablePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("running"));
    }

    @Test
    void shouldSimulatePauseResumeAndEndedCallbacks() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-f1");
        generateScript(fixture.ownerToken(), livePlanId);
        MvcResult startResult = startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());
        String liveSessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data")
                .path("liveSessionId")
                .asText();

        simulateCallback(fixture.ownerToken(), liveSessionId, "paused", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionStatus").value("paused"));

        mockMvc.perform(get("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("ready"));

        simulateCallback(fixture.ownerToken(), liveSessionId, "resumed", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionStatus").value("running"));

        mockMvc.perform(get("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("running"));

        simulateCallback(fixture.ownerToken(), liveSessionId, "ended", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionStatus").value("ended"));

        simulateCallback(fixture.ownerToken(), liveSessionId, "ended", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionStatus").value("ended"));

        mockMvc.perform(get("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("ended"));
    }

    @Test
    void shouldSimulateInterruptedCallbackAndAllowRestart() throws Exception {
        LiveFixture fixture = prepareFixture(true);
        String livePlanId = createPlan(fixture.ownerToken(), fixture.storeId(), fixture.liveAccountId(), "live-plan-g1");
        generateScript(fixture.ownerToken(), livePlanId);
        MvcResult startResult = startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());
        String firstSessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("data")
                .path("liveSessionId")
                .asText();

        simulateCallback(fixture.ownerToken(), firstSessionId, "interrupted", "platform disconnect")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionStatus").value("failed"))
                .andExpect(jsonPath("$.data.errorMessage").value("platform disconnect"));

        mockMvc.perform(get("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("ready"));

        mockMvc.perform(get("/api/live-sessions/{id}/status", firstSessionId)
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionStatus").value("failed"))
                .andExpect(jsonPath("$.data.planStatus").value("ready"))
                .andExpect(jsonPath("$.data.accountOccupied").value(false))
                .andExpect(jsonPath("$.data.tenantRemainingQuota").value(2))
                .andExpect(jsonPath("$.data.errorMessage").value("platform disconnect"));

        MvcResult restartResult = startLive(fixture.ownerToken(), livePlanId, fixture.liveAccountId());
        String secondSessionId = objectMapper.readTree(restartResult.getResponse().getContentAsString())
                .path("data")
                .path("liveSessionId")
                .asText();

        mockMvc.perform(get("/api/live-sessions")
                        .header("Authorization", "Bearer " + fixture.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].liveSessionId", Matchers.hasItems(firstSessionId, secondSessionId)))
                .andExpect(jsonPath("$.data[0].sessionStatus", Matchers.isOneOf("running", "failed")))
                .andExpect(jsonPath("$.data[1].sessionStatus", Matchers.isOneOf("running", "failed")));
    }

    private LiveFixture prepareFixture(boolean subscribeBasic) throws Exception {
        JsonNode tenantData = registerTenant("live-test-center");
        String tenantId = tenantData.path("tenantId").asText();
        String organizationId = tenantData.path("defaultOrganizationId").asText();
        String ownerToken = login("13800000000", BOOTSTRAP_PASSWORD);
        if (subscribeBasic) {
            mockMvc.perform(post("/api/tenants/{id}/subscription/subscribe", tenantId)
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "planCode": "basic",
                                      "seatCount": 1,
                                      "autoRenew": false
                                    }
                                    """))
                    .andExpect(status().isOk());
        }
        String storeId = connectStore(ownerToken, organizationId, "shop-live-001");
        String liveAccountId = createChannelAccount(ownerToken, organizationId, "live-account-a");
        return new LiveFixture(tenantId, organizationId, storeId, liveAccountId, ownerToken);
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

    private String connectStore(String ownerToken, String organizationId, String platformShopId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stores/connect")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "douyin",
                                  "platformShopId": "%s",
                                  "shopName": "live-store",
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

    private String createChannelAccount(String ownerToken, String organizationId, String accountName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/channel-accounts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "channelType": "douyin-live",
                                  "accountName": "%s",
                                  "extraConfig": {
                                    "scene": "digital-human-live"
                                  }
                                }
                                """.formatted(organizationId, accountName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("channelAccountId")
                .asText();
    }

    private String createPlan(String ownerToken, String storeId, String liveAccountId, String planName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/live-plans")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "liveAccountId": "%s",
                                  "planName": "%s",
                                  "scheduledStartAt": "2026-06-01T10:00:00+08:00",
                                  "scheduledEndAt": "2026-06-01T12:00:00+08:00",
                                  "anchorProfileName": "default-anchor"
                                }
                                """.formatted(storeId, liveAccountId, planName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("livePlanId")
                .asText();
    }

    private String createCandidateProduct(String ownerToken, String storeId, String title, String status) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/candidate-products")
                        .header("Authorization", "Bearer " + ownerToken)
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
                                  "aiSummary": "fit for live"
                                }
                                """.formatted(storeId, title, title)))
                .andExpect(status().isOk())
                .andReturn();
        String candidateProductId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("candidateProductId")
                .asText();
        mockMvc.perform(post("/api/candidate-products/{id}/status", candidateProductId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "%s"
                                }
                                """.formatted(status)))
                .andExpect(status().isOk());
        return candidateProductId;
    }

    private String generateProductDraft(String ownerToken, String candidateProductId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product-drafts/generate")
                        .header("Authorization", "Bearer " + ownerToken)
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

    private String publishProductDraft(String ownerToken, String productDraftId, String platformProductId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product-drafts/{id}/publish", productDraftId)
                        .header("Authorization", "Bearer " + ownerToken)
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

    private PublishedLiveProductFixture preparePublishedLiveProduct(String ownerToken,
                                                                    String storeId,
                                                                    String title,
                                                                    String status) throws Exception {
        String candidateProductId = createCandidateProduct(ownerToken, storeId, title, status);
        String productDraftId = generateProductDraft(ownerToken, candidateProductId);
        String productId = publishProductDraft(ownerToken, productDraftId, "platform-" + title);
        return new PublishedLiveProductFixture(
                candidateProductId,
                productDraftId,
                productId,
                title + "丨精选款"
        );
    }

    private MvcResult bindLiveProduct(String ownerToken,
                                      String livePlanId,
                                      String productId,
                                      String productDraftId,
                                      String candidateProductId,
                                      int sortOrder) throws Exception {
        String payload = """
                {
                  "productId": %s,
                  "productDraftId": %s,
                  "candidateProductId": %s,
                  "sortOrder": %d
                }
                """.formatted(
                productId == null ? "null" : "\"" + productId + "\"",
                productDraftId == null ? "null" : "\"" + productDraftId + "\"",
                candidateProductId == null ? "null" : "\"" + candidateProductId + "\"",
                sortOrder
        );
        return mockMvc.perform(post("/api/live-plans/{id}/products/bind", livePlanId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
    }

    private void generateScript(String ownerToken, String livePlanId) throws Exception {
        mockMvc.perform(post("/api/live-plans/{id}/generate-script", livePlanId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    private void updatePlan(String ownerToken,
                            String livePlanId,
                            String liveAccountId,
                            String planName,
                            String scheduledStartAt,
                            String scheduledEndAt,
                            String anchorProfileName) throws Exception {
        mockMvc.perform(put("/api/live-plans/{id}", livePlanId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "liveAccountId": "%s",
                                  "planName": "%s",
                                  "scheduledStartAt": "%s",
                                  "scheduledEndAt": "%s",
                                  "anchorProfileName": "%s"
                                }
                                """.formatted(liveAccountId, planName, scheduledStartAt, scheduledEndAt, anchorProfileName)))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions schedulePlan(String ownerToken, String livePlanId) throws Exception {
        return mockMvc.perform(post("/api/live-plans/{id}/schedule", livePlanId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions resumeLive(String ownerToken, String livePlanId) throws Exception {
        return mockMvc.perform(post("/api/live-plans/{id}/resume", livePlanId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    private MvcResult duplicatePlan(String ownerToken, String livePlanId) throws Exception {
        return mockMvc.perform(post("/api/live-plans/{id}/duplicate", livePlanId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions cancelPlan(String ownerToken, String livePlanId) throws Exception {
        return mockMvc.perform(post("/api/live-plans/{id}/cancel", livePlanId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions skipCurrentProduct(String ownerToken, String liveSessionId) throws Exception {
        return mockMvc.perform(post("/api/live-sessions/{id}/skip-current-product", liveSessionId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    private MvcResult startLive(String ownerToken, String livePlanId, String liveAccountId) throws Exception {
        return mockMvc.perform(post("/api/live-plans/{id}/start", livePlanId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.livePlanId").value(livePlanId))
                .andExpect(jsonPath("$.data.liveAccountId").value(liveAccountId))
                .andExpect(jsonPath("$.data.sessionStatus").value("running"))
                .andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions simulateCallback(String ownerToken,
                                                                               String liveSessionId,
                                                                               String eventType,
                                                                               String errorMessage) throws Exception {
        String payload = errorMessage == null
                ? """
                {
                  "eventType": "%s"
                }
                """.formatted(eventType)
                : """
                {
                  "eventType": "%s",
                  "errorMessage": "%s"
                }
                """.formatted(eventType, errorMessage);
        return mockMvc.perform(post("/api/live-sessions/{id}/simulate-callback", liveSessionId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload));
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

record LiveFixture(String tenantId, String organizationId, String storeId, String liveAccountId, String ownerToken) {
}

record PublishedLiveProductFixture(String candidateProductId,
                                   String productDraftId,
                                   String productId,
                                   String productTitle) {
}
