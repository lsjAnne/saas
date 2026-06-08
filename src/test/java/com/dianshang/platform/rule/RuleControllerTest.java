package com.dianshang.platform.rule;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.rule.application.RuleService;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.store.application.StoreChannelService;
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

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RuleControllerTest {

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
    private RuleService ruleService;

    @BeforeEach
    void setUp() {
        ruleService.clear();
        storeChannelService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldCreateListUpdateAndToggleAutomationRules() throws Exception {
        RuleFixture fixture = prepareFixture();

        MvcResult createResult = mockMvc.perform(post("/api/rules")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "ruleType": "profit_threshold",
                                  "ruleCategory": "product_selection",
                                  "riskCategory": "price",
                                  "ruleName": "minimum-profit",
                                  "ruleExpression": "profit >= 15",
                                  "enabled": true
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(fixture.storeId()))
                .andExpect(jsonPath("$.data.ruleType").value("profit_threshold"))
                .andExpect(jsonPath("$.data.ruleCategory").value("product_selection"))
                .andExpect(jsonPath("$.data.riskCategory").value("price"))
                .andExpect(jsonPath("$.data.ruleName").value("minimum-profit"))
                .andExpect(jsonPath("$.data.ruleExpression").value("profit >= 15"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andReturn();

        String ruleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("ruleId")
                .asText();

        mockMvc.perform(get("/api/rules")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].ruleId").value(ruleId))
                .andExpect(jsonPath("$.data[0].ruleCategory").value("product_selection"))
                .andExpect(jsonPath("$.data[0].riskCategory").value("price"))
                .andExpect(jsonPath("$.data[0].enabled").value(true));

        mockMvc.perform(put("/api/rules/{id}", ruleId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "ruleType": "live_inventory_guard",
                                  "ruleCategory": "live",
                                  "riskCategory": "fulfillment",
                                  "ruleName": "inventory-guard",
                                  "ruleExpression": "stock >= 5",
                                  "enabled": false
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleId").value(ruleId))
                .andExpect(jsonPath("$.data.ruleType").value("live_inventory_guard"))
                .andExpect(jsonPath("$.data.ruleCategory").value("live"))
                .andExpect(jsonPath("$.data.riskCategory").value("fulfillment"))
                .andExpect(jsonPath("$.data.ruleName").value("inventory-guard"))
                .andExpect(jsonPath("$.data.ruleExpression").value("stock >= 5"))
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(post("/api/rules/{id}/enable", ruleId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(post("/api/rules/{id}/disable", ruleId)
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void shouldRejectCrossTenantRuleAccess() throws Exception {
        RuleFixture fixture = prepareFixture();
        RuleFixture secondFixture = prepareFixture();

        MvcResult createResult = mockMvc.perform(post("/api/rules")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "ruleType": "qa_auto_reply",
                                  "ruleName": "reply-rule",
                                  "ruleExpression": "hit_faq == true",
                                  "enabled": true
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andReturn();

        String ruleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("ruleId")
                .asText();

        mockMvc.perform(post("/api/rules/{id}/disable", ruleId)
                        .header("X-Tenant-Id", secondFixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("1003"));
    }

    @Test
    void shouldRejectRuleAccessWithoutRuleManagePermission() throws Exception {
        RuleFixture fixture = prepareFixture();

        mockMvc.perform(get("/api/rules")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "guest-user")
                        .header("X-Operator-Type", "guest"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1009"));
    }

    @Test
    void shouldExposeRuleGovernanceOverviewAndOrchestrations() throws Exception {
        RuleFixture fixture = prepareFixture();
        createRule(fixture, "profit_threshold", "product_selection", "price", "minimum-profit", "profit >= 15", true);
        createRule(fixture, "live_inventory_guard", "live", "fulfillment", "inventory-guard", "stock >= 5", true);
        createRule(fixture, "fulfillment_retry_fallback", "fulfillment", "fulfillment", "delivery-fallback", "retry <= 2 then fallback manual review", true);
        createRule(fixture, "qa_reply_fallback", "qa", "content", "reply-fallback", "fallback faq answer", false);

        mockMvc.perform(get("/api/rules/governance-overview")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRules").value(4))
                .andExpect(jsonPath("$.data.enabledRules").value(3))
                .andExpect(jsonPath("$.data.disabledRules").value(1))
                .andExpect(jsonPath("$.data.fallbackRuleCount").value(2))
                .andExpect(jsonPath("$.data.manualReviewRuleCount").value(1))
                .andExpect(jsonPath("$.data.categoryStats.length()").value(4))
                .andExpect(jsonPath("$.data.riskStats.length()").value(3));

        mockMvc.perform(get("/api/rules/orchestrations")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$..executionStage", hasItems("pre_check", "runtime", "post_check")))
                .andExpect(jsonPath("$..fallbackEnabled", hasItems(true)))
                .andExpect(jsonPath("$..manualReviewRequired", hasItems(true)));
    }

    @Test
    void shouldSimulateFallbackStrategyWithMatchedEnabledRules() throws Exception {
        RuleFixture fixture = prepareFixture();
        createRule(fixture, "fulfillment_retry_fallback", "fulfillment", "fulfillment", "delivery-fallback", "retry <= 2 then fallback manual review", true);
        createRule(fixture, "live_inventory_guard", "live", "fulfillment", "inventory-guard", "stock >= 5", true);
        createRule(fixture, "fulfillment_disabled_fallback", "fulfillment", "fulfillment", "disabled-fallback", "fallback manual review", false);

        mockMvc.perform(post("/api/rules/fallback-simulations")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "scenarioCode": "fulfillment_timeout",
                                  "failureSource": "fulfillment",
                                  "requestedAction": "dispatch",
                                  "signalValue": 92
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coverageStatus").value("covered"))
                .andExpect(jsonPath("$.data.fallbackTriggered").value(true))
                .andExpect(jsonPath("$.data.retrySuggested").value(true))
                .andExpect(jsonPath("$.data.suggestedAction").value("manual_review"))
                .andExpect(jsonPath("$.data.terminalAction").value("pause_and_escalate"))
                .andExpect(jsonPath("$.data.matchedRules.length()").value(1))
                .andExpect(jsonPath("$.data.matchedRules[0].ruleType").value("fulfillment_retry_fallback"));
    }

    @Test
    void shouldReturnFallbackGapWhenNoEnabledRuleMatchesScenario() throws Exception {
        RuleFixture fixture = prepareFixture();
        createRule(fixture, "profit_threshold", "product_selection", "price", "minimum-profit", "profit >= 15", true);

        mockMvc.perform(post("/api/rules/fallback-simulations")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "scenarioCode": "fulfillment_timeout",
                                  "failureSource": "fulfillment",
                                  "requestedAction": "dispatch",
                                  "signalValue": 92
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coverageStatus").value("gap"))
                .andExpect(jsonPath("$.data.fallbackTriggered").value(false))
                .andExpect(jsonPath("$.data.retrySuggested").value(false))
                .andExpect(jsonPath("$.data.suggestedAction").value("manual_review"))
                .andExpect(jsonPath("$.data.matchedRules.length()").value(0));
    }

    private RuleFixture prepareFixture() throws Exception {
        JsonNode tenantData = registerTenant("rule-test-center");
        String tenantId = tenantData.path("tenantId").asText();
        String organizationId = tenantData.path("defaultOrganizationId").asText();
        String storeId = connectStore(tenantId, organizationId, "shop-rule-" + organizationId);
        return new RuleFixture(tenantId, organizationId, storeId);
    }

    private void createRule(RuleFixture fixture,
                            String ruleType,
                            String ruleCategory,
                            String riskCategory,
                            String ruleName,
                            String ruleExpression,
                            boolean enabled) throws Exception {
        mockMvc.perform(post("/api/rules")
                        .header("X-Tenant-Id", fixture.tenantId())
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "ruleType": "%s",
                                  "ruleCategory": "%s",
                                  "riskCategory": "%s",
                                  "ruleName": "%s",
                                  "ruleExpression": "%s",
                                  "enabled": %s
                                }
                                """.formatted(
                                fixture.storeId(),
                                ruleType,
                                ruleCategory,
                                riskCategory,
                                ruleName,
                                ruleExpression,
                                enabled)))
                .andExpect(status().isOk());
    }

    private JsonNode registerTenant(String tenantName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "rule-owner",
                                  "mobile": "13800000000"
                                }
                                """.formatted(tenantName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String connectStore(String tenantId, String organizationId, String platformShopId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stores/connect")
                        .header("X-Tenant-Id", tenantId)
                        .header("X-Operator-Id", "tenant-admin")
                        .header("X-Operator-Type", "tenant-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "douyin",
                                  "platformShopId": "%s",
                                  "shopName": "rule-store",
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
}

record RuleFixture(String tenantId, String organizationId, String storeId) {
}
