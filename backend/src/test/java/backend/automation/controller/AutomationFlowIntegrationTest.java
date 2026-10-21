package backend.automation.controller;

import backend.automation.application.AutomationExecutionService;
import backend.audit.application.AuditLogService;
import backend.auth.dto.LoginResponse;
import backend.channel.domain.repository.ChannelAuthStateRepository;
import backend.channel.domain.repository.ChannelTokenRepository;
import backend.common.api.ApiResponse;
import backend.finance.application.FinanceService;
import backend.fulfillment.domain.repository.FulfillmentTaskRepository;
import backend.order.application.OrderApplicationService;
import backend.organization.application.OrganizationService;
import backend.product.domain.repository.ProductRepository;
import backend.product.model.Product;
import backend.rule.application.RuleService;
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

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 运营自动化中心 M1 端到端闭环（PDD mock-mode）：
 * 注册租户 → 登录 → 建店 → mock 授权 → 建规则 → 绑定动作 → 手动拉单触发 →
 * 自动风控 → AUTO_SHIP 自动发货 succeeded → PAUSE_REVIEW 挂起进人工队列 →
 * 人工 APPROVED → 财务单据生成/对账 → 执行记录与统计可查 → 二次触发幂等去重。
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class AutomationFlowIntegrationTest {

    private static final String BOOTSTRAP_PASSWORD = "123456";
    private static final String MOBILE = "13800000000";

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
    private ChannelTokenRepository channelTokenRepository;

    @Autowired
    private ChannelAuthStateRepository channelAuthStateRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderApplicationService orderApplicationService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private FinanceService financeService;

    @Autowired
    private FulfillmentTaskRepository fulfillmentTaskRepository;

    @Autowired
    private AutomationExecutionService automationExecutionService;

    @BeforeEach
    void setUp() {
        clearSharedState();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        clearSharedState();
    }

    private void clearSharedState() {
        storeChannelService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
        channelTokenRepository.deleteAll();
        channelAuthStateRepository.deleteAll();
        productRepository.deleteAll();
        orderApplicationService.clear();
        ruleService.clear();
        financeService.clear();
        fulfillmentTaskRepository.deleteAll();
        automationExecutionService.clear();
    }

    @Test
    void shouldRunFullAutomationLoop() throws Exception {
        // 1. 注册租户 + 登录
        JsonNode tenantData = registerTenant("automation-flow-" + UUID.randomUUID().toString().substring(0, 8));
        String ownerToken = login(MOBILE, BOOTSTRAP_PASSWORD);
        String defaultOrganizationId = tenantData.path("defaultOrganizationId").asText();
        assertThat(defaultOrganizationId).isNotBlank();

        // 2. 建店（pdd）
        MvcResult connectResult = mockMvc.perform(post("/api/stores/connect")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "pdd",
                                  "platformShopId": "shop-auto-001",
                                  "shopName": "auto-pdd-store",
                                  "profitThreshold": 18.50,
                                  "riskThreshold": 72.00
                                }
                                """.formatted(defaultOrganizationId)))
                .andExpect(status().isOk()).andReturn();
        String storeId = objectMapper.readTree(connectResult.getResponse().getContentAsString())
                .path("data").path("storeId").asText();
        assertThat(storeId).isNotBlank();

        // 3. 种商品，避免订单同步 resolveProduct 报错
        productRepository.save(new Product(
                null, storeId, "platform-product-pdd-auto", "draft-2001",
                "自动化闭环测试商品", "published", 100, OffsetDateTime.now(), OffsetDateTime.now()));

        // 4. PDD mock 授权 + 公开回调换 token
        MvcResult authorizeResult = mockMvc.perform(post("/api/channels/pdd/authorize")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "organizationId": "%s" }
                                """.formatted(defaultOrganizationId)))
                .andExpect(status().isOk()).andReturn();
        String state = objectMapper.readTree(authorizeResult.getResponse().getContentAsString())
                .path("data").path("state").asText();
        assertThat(state).isNotBlank();
        mockMvc.perform(get("/api/channels/callback/pdd").param("code", "mock-code").param("state", state))
                .andExpect(status().isOk());

        // 5. 建规则1：小额单（9.90）全自动 → 发货 + 生成单据 + 对账
        String rule1Id = createRule(ownerToken, storeId, "小额单自动闭环", "amount < 100");
        createAction(ownerToken, storeId, rule1Id, "AUTO_SHIP", 10);
        createAction(ownerToken, storeId, rule1Id, "GENERATE_BILL", 20);
        createAction(ownerToken, storeId, rule1Id, "AUTO_RECONCILE", 30);

        // 6. 建规则2：大额单（398）发货后挂起人工确认，放行后生成单据+对账
        String rule2Id = createRule(ownerToken, storeId, "大额单人工复核", "amount >= 100");
        createAction(ownerToken, storeId, rule2Id, "AUTO_SHIP", 10);
        createAction(ownerToken, storeId, rule2Id, "PAUSE_REVIEW", 20);
        createAction(ownerToken, storeId, rule2Id, "GENERATE_BILL", 30);
        createAction(ownerToken, storeId, rule2Id, "AUTO_RECONCILE", 40);

        // 7. 手动触发拉单 + 规则引擎
        MvcResult triggerResult = mockMvc.perform(post("/api/automation/trigger/order-sync")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("storeId", storeId))
                .andExpect(status().isOk()).andReturn();
        JsonNode triggerData = objectMapper.readTree(triggerResult.getResponse().getContentAsString()).path("data");
        assertThat(triggerData.path("triggerSource").asText()).isEqualTo("MANUAL");
        assertThat(triggerData.path("pulledOrderCount").asInt())
                .as("trigger response = %s", triggerData).isEqualTo(2);
        assertThat(triggerData.path("matchedRuleCount").asInt()).isEqualTo(2);
        // 小额单 3 个动作 + 大额单 2 个动作（发货+挂起）= 5 条执行记录
        assertThat(triggerData.path("executionCount").asInt()).isEqualTo(5);
        assertThat(triggerData.path("createdExecutionIds").size()).isEqualTo(5);

        // 8. 执行记录：小额单三动作全部 SUCCESS，大额单发货 SUCCESS + 挂起 PENDING_REVIEW
        MvcResult executionsResult = mockMvc.perform(get("/api/automation/executions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("storeId", storeId))
                .andExpect(status().isOk()).andReturn();
        JsonNode executions = objectMapper.readTree(executionsResult.getResponse().getContentAsString())
                .path("data").path("list");
        assertThat(executions.size()).isEqualTo(5);
        long successCount = countStatus(executions, "SUCCESS");
        long pendingReviewCount = countStatus(executions, "PENDING_REVIEW");
        assertThat(successCount).isEqualTo(4);
        assertThat(pendingReviewCount).isEqualTo(1);

        // 9. 人工确认队列：1 条 PENDING
        MvcResult pendingResult = mockMvc.perform(get("/api/automation/approvals")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("storeId", storeId)
                        .param("status", "PENDING"))
                .andExpect(status().isOk()).andReturn();
        JsonNode pendingList = objectMapper.readTree(pendingResult.getResponse().getContentAsString())
                .path("data").path("list");
        assertThat(pendingList.size()).isEqualTo(1);
        String taskId = pendingList.get(0).path("taskId").asText();
        assertThat(taskId).isNotBlank();

        // 10. 人工 APPROVED → 续跑后续动作（生成单据+对账）
        mockMvc.perform(post("/api/automation/approvals/" + taskId + "/approve")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "comment": "ok to ship and bill" }
                                """))
                .andExpect(status().isOk());

        // 11. 审批后：队列清空，执行记录新增 2 条（GENERATE_BILL / AUTO_RECONCILE）
        MvcResult approvalsAfter = mockMvc.perform(get("/api/automation/approvals")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("storeId", storeId)
                        .param("status", "PENDING"))
                .andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(approvalsAfter.getResponse().getContentAsString())
                .path("data").path("list").size()).isZero();

        MvcResult executionsAfterResult = mockMvc.perform(get("/api/automation/executions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("storeId", storeId))
                .andExpect(status().isOk()).andReturn();
        JsonNode executionsAfter = objectMapper.readTree(executionsAfterResult.getResponse().getContentAsString())
                .path("data").path("list");
        assertThat(executionsAfter.size()).isEqualTo(7);

        // 12. 总览统计：全部成功、待审 0
        MvcResult statsResult = mockMvc.perform(get("/api/automation/stats")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("storeId", storeId))
                .andExpect(status().isOk()).andReturn();
        JsonNode stats = objectMapper.readTree(statsResult.getResponse().getContentAsString()).path("data");
        assertThat(stats.path("total").asInt()).isEqualTo(7);
        assertThat(stats.path("success").asInt()).isEqualTo(7);
        assertThat(stats.path("failed").asInt()).isZero();
        assertThat(stats.path("pendingReview").asInt()).isZero();
        assertThat(stats.path("pendingApprovalCount").asInt()).isZero();
        assertThat(stats.path("byActionType").isArray()).isTrue();
        assertThat(stats.path("byActionType").size()).isEqualTo(4);

        // 13. 单条执行记录可查
        String firstExecutionId = executionsAfter.get(0).path("executionId").asText();
        mockMvc.perform(get("/api/automation/executions/" + firstExecutionId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // 14. 二次触发幂等：同一 target+rule+action 不重复执行
        MvcResult secondTrigger = mockMvc.perform(post("/api/automation/trigger/order-sync")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("storeId", storeId))
                .andExpect(status().isOk()).andReturn();
        JsonNode secondData = objectMapper.readTree(secondTrigger.getResponse().getContentAsString()).path("data");
        assertThat(secondData.path("pulledOrderCount").asInt()).isEqualTo(2);
        assertThat(secondData.path("executionCount").asInt()).isZero();

        // 15. 动作列表与规则列表
        MvcResult actionsResult = mockMvc.perform(get("/api/automation/actions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("storeId", storeId))
                .andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(actionsResult.getResponse().getContentAsString())
                .path("data").path("list").size()).isEqualTo(7);

        MvcResult rulesResult = mockMvc.perform(get("/api/automation/rules")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("enabled", "true"))
                .andExpect(status().isOk()).andReturn();
        JsonNode rules = objectMapper.readTree(rulesResult.getResponse().getContentAsString()).path("data");
        assertThat(rules.size()).isEqualTo(2);
    }

    private long countStatus(JsonNode list, String status) {
        long count = 0;
        for (JsonNode item : list) {
            if (status.equals(item.path("status").asText())) {
                count++;
            }
        }
        return count;
    }

    private String createRule(String token, String storeId, String ruleName, String expression) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "ruleType": "order_risk",
                                  "ruleName": "%s",
                                  "ruleExpression": "%s",
                                  "enabled": true
                                }
                                """.formatted(storeId, ruleName, expression)))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("ruleId").asText();
    }

    private void createAction(String token, String storeId, String ruleId, String actionType, int priority) throws Exception {
        mockMvc.perform(post("/api/automation/actions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "ruleId": "%s",
                                  "actionType": "%s",
                                  "priority": %d,
                                  "enabled": true
                                }
                                """.formatted(storeId, ruleId, actionType, priority)))
                .andExpect(status().isOk());
    }

    private JsonNode registerTenant(String tenantName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "张三",
                                  "mobile": "%s"
                                }
                                """.formatted(tenantName, MOBILE)))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
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
                .andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        ApiResponse<LoginResponse> response = objectMapper.readValue(content, new TypeReference<>() {
        });
        return response.data().token();
    }
}
