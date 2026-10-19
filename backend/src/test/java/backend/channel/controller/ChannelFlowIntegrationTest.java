package backend.channel.controller;

import backend.audit.application.AuditLogService;
import backend.auth.dto.LoginResponse;
import backend.channel.application.ChannelAuthService;
import backend.channel.domain.repository.ChannelAuthStateRepository;
import backend.channel.domain.repository.ChannelTokenRepository;
import backend.common.api.ApiResponse;
import backend.order.application.OrderApplicationService;
import backend.organization.application.OrganizationService;
import backend.product.domain.repository.ProductRepository;
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

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 渠道接入层端到端闭环验证（拼多多 PDD，mock-mode=true）。
 * 完整链路：注册租户 → 登录 → 建店 → 发起授权 → 公开回调换 token →
 * 渠道账号列表 → 令牌列表 → 订单同步 → 商品同步 → 连通性探测 → 令牌刷新。
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class ChannelFlowIntegrationTest {

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

    @BeforeEach
    void setUp() {
        storeChannelService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
        channelTokenRepository.deleteAll();
        channelAuthStateRepository.deleteAll();
        productRepository.deleteAll();
        orderApplicationService.clear();
    }

    @Test
    void shouldRunFullPddMockModeAuthorizationAndSyncFlow() throws Exception {
        // 1. 注册租户 + 登录
        JsonNode tenantData = registerTenant("channel-flow-" + UUID.randomUUID().toString().substring(0, 8));
        String ownerToken = login(MOBILE, BOOTSTRAP_PASSWORD);
        String defaultOrganizationId = tenantData.path("defaultOrganizationId").asText();
        assertThat(defaultOrganizationId).isNotBlank();

        // 2. 建店（platformType=pdd）
        MvcResult connectResult = mockMvc.perform(post("/api/stores/connect")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "pdd",
                                  "platformShopId": "shop-pdd-001",
                                  "shopName": "pdd-flagship-store",
                                  "profitThreshold": 18.50,
                                  "riskThreshold": 72.00
                                }
                                """.formatted(defaultOrganizationId)))
                .andExpect(status().isOk())
                .andReturn();
        String storeId = objectMapper.readTree(connectResult.getResponse().getContentAsString())
                .path("data").path("storeId").asText();
        assertThat(storeId).isNotBlank();

        // 给店铺种一个商品，避免订单同步时 resolveProduct 抛 7502（current store has no available product）
        productRepository.save(new Product(
                null,
                storeId,
                "platform-product-pdd-1",
                "draft-1001",
                "拼多多闭环测试商品",
                "published",
                100,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        ));

        // 3. 发起 PDD 授权：断言 authorizeUrl 含 app_id= 且 state 非空
        MvcResult authorizeResult = mockMvc.perform(post("/api/channels/pdd/authorize")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s"
                                }
                                """.formatted(defaultOrganizationId)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode authorizeData = objectMapper.readTree(authorizeResult.getResponse().getContentAsString()).path("data");
        String authorizeUrl = authorizeData.path("authorizeUrl").asText();
        String state = authorizeData.path("state").asText();
        assertThat(authorizeUrl).contains("app_id=");
        assertThat(state).isNotBlank();

        // 4. 公开 OAuth 回调（不带 Authorization 头）：断言返回文本含“授权成功”
        String callbackBody = mockMvc.perform(get("/api/channels/callback/pdd")
                        .param("code", "mock-code")
                        .param("state", state))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(callbackBody).contains("授权成功");

        // 5. 渠道账号列表：找到 channelType=pdd 的账号
        MvcResult accountsResult = mockMvc.perform(get("/api/channel-accounts")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode accounts = objectMapper.readTree(accountsResult.getResponse().getContentAsString()).path("data");
        JsonNode pddAccount = null;
        for (JsonNode account : accounts) {
            if ("pdd".equals(account.path("channelType").asText())) {
                pddAccount = account;
            }
        }
        assertThat(pddAccount).isNotNull();
        String channelAccountId = pddAccount.path("channelAccountId").asText();
        assertThat(channelAccountId).isNotBlank();

        // 6. 令牌列表：存在 pdd token 且 expiresAt 非空
        MvcResult tokensResult = mockMvc.perform(get("/api/channels/tokens")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tokens = objectMapper.readTree(tokensResult.getResponse().getContentAsString()).path("data");
        JsonNode pddToken = null;
        for (JsonNode token : tokens) {
            if ("pdd".equals(token.path("channelType").asText())) {
                pddToken = token;
            }
        }
        assertThat(pddToken).isNotNull();
        String channelTokenId = pddToken.path("channelTokenId").asText();
        assertThat(channelTokenId).isNotBlank();
        assertThat(pddToken.path("expiresAt").asText()).isNotBlank();
        OffsetDateTime expiresAtBeforeRefresh = OffsetDateTime.parse(pddToken.path("expiresAt").asText());

        // 7. 订单同步：断言 syncType=orders 且 succeededCount>=1（mock 拉到 2 条样例订单）
        MvcResult syncOrdersResult = mockMvc.perform(post("/api/channels/sync/orders")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channelAccountId": "%s",
                                  "storeId": "%s"
                                }
                                """.formatted(channelAccountId, storeId)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode syncOrdersData = objectMapper.readTree(syncOrdersResult.getResponse().getContentAsString()).path("data");
        assertThat(syncOrdersData.path("syncType").asText()).isEqualTo("orders");
        assertThat(syncOrdersData.path("succeededCount").asInt())
                .as("orders sync response = %s", syncOrdersData).isGreaterThanOrEqualTo(1);

        // 8. 商品同步：断言 syncType=products 且拉到商品列表（totalCount=mock 样例数）
        MvcResult syncProductsResult = mockMvc.perform(post("/api/channels/sync/products")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channelAccountId": "%s",
                                  "storeId": "%s"
                                }
                                """.formatted(channelAccountId, storeId)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode syncProductsData = objectMapper.readTree(syncProductsResult.getResponse().getContentAsString()).path("data");
        assertThat(syncProductsData.path("syncType").asText()).isEqualTo("products");
        assertThat(syncProductsData.path("totalCount").asInt()).isGreaterThanOrEqualTo(1);

        // 9. 连通性探测：断言 reachable=true
        MvcResult connectivityResult = mockMvc.perform(post("/api/channels/pdd/connectivity")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channelAccountId": "%s"
                                }
                                """.formatted(channelAccountId)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode connectivityData = objectMapper.readTree(connectivityResult.getResponse().getContentAsString()).path("data");
        assertThat(connectivityData.path("reachable").asBoolean()).isTrue();

        // 10. 刷新令牌：断言 expiresAt 被顺延（不早于刷新前）
        MvcResult refreshResult = mockMvc.perform(post("/api/channels/pdd/refresh-token")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channelTokenId": "%s"
                                }
                                """.formatted(channelTokenId)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode refreshData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");
        OffsetDateTime expiresAtAfterRefresh = OffsetDateTime.parse(refreshData.path("expiresAt").asText());
        assertThat(expiresAtAfterRefresh).isAfterOrEqualTo(expiresAtBeforeRefresh);
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
                .andExpect(status().isOk())
                .andReturn();
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
