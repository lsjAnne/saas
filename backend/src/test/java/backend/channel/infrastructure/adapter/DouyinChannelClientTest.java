package backend.channel.infrastructure.adapter;

import backend.channel.application.ChannelClientRegistry;
import backend.channel.application.ChannelPlatformProperties;
import backend.channel.model.ChannelAfterSale;
import backend.channel.model.ChannelAuthContext;
import backend.channel.model.ChannelCallbackRequest;
import backend.channel.model.ChannelConnectivity;
import backend.channel.model.ChannelOrder;
import backend.channel.model.ChannelProduct;
import backend.channel.model.ChannelShipRequest;
import backend.channel.model.ChannelShipResult;
import backend.channel.model.ChannelToken;
import backend.channel.model.ChannelType;
import backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DouyinChannelClientTest {

    private static final String APP_KEY = "dy-app-key-1";
    private static final String APP_SECRET = "dy-secret-1";
    private static final String REDIRECT_URI = "https://example.com/callback/douyin";

    private ChannelPlatformProperties mockProps() {
        ChannelPlatformProperties.ChannelPlatformSettings settings = new ChannelPlatformProperties.ChannelPlatformSettings();
        settings.setMockMode(true);
        settings.setEndpoint("https://open.douyin.com");
        settings.setAppKey(APP_KEY);
        settings.setAppSecret(APP_SECRET);
        ChannelPlatformProperties properties = new ChannelPlatformProperties();
        properties.setPlatforms(Map.of("douyin", settings));
        return properties;
    }

    private ChannelPlatformProperties realProps() {
        ChannelPlatformProperties.ChannelPlatformSettings settings = new ChannelPlatformProperties.ChannelPlatformSettings();
        settings.setMockMode(false);
        settings.setEndpoint("https://open.douyin.com");
        settings.setAppKey(APP_KEY);
        settings.setAppSecret(APP_SECRET);
        ChannelPlatformProperties properties = new ChannelPlatformProperties();
        properties.setPlatforms(Map.of("douyin", settings));
        return properties;
    }

    private ChannelAuthContext authContext(Map<String, Object> extra) {
        return new ChannelAuthContext(
                "tenant-1", "org-1", "acct-1", "store-1",
                APP_KEY, APP_SECRET, REDIRECT_URI, extra);
    }

    @Test
    void channelTypeIsDouyin() {
        DouyinChannelClient client = new DouyinChannelClient(mockProps());
        assertThat(client.channelType()).isEqualTo(ChannelType.DOUYIN);
    }

    // ---------------- mock-mode=true：逐方法契约断言 ----------------

    @Test
    void buildAuthorizeUrlInMockModeContainsClientKeyAndRedirectUriAndState() {
        DouyinChannelClient client = new DouyinChannelClient(mockProps());
        ChannelAuthContext context = authContext(Map.of("state", "state-abc", "scope", "user_info"));

        String url = client.buildAuthorizeUrl(context);

        assertThat(url).isEqualTo(
                "https://open.douyin.com/platform/oauth/connect/"
                        + "?client_key=dy-app-key-1"
                        + "&response_type=code"
                        + "&scope=user_info"
                        + "&redirect_uri=https://example.com/callback/douyin"
                        + "&state=state-abc");
    }

    @Test
    void buildAuthorizeUrlOmitsStateWhenAbsentAndDefaultsScope() {
        DouyinChannelClient client = new DouyinChannelClient(mockProps());
        ChannelAuthContext context = authContext(Map.of());

        String url = client.buildAuthorizeUrl(context);

        assertThat(url).startsWith("https://open.douyin.com/platform/oauth/connect/?client_key=dy-app-key-1");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("scope=user_info");
        assertThat(url).contains("redirect_uri=https://example.com/callback/douyin");
        assertThat(url).doesNotContain("state=");
    }

    @Test
    void handleCallbackInMockModeReturnsFixedTokenFields() {
        DouyinChannelClient client = new DouyinChannelClient(mockProps());
        ChannelCallbackRequest request = new ChannelCallbackRequest(ChannelType.DOUYIN, "code-123", "state-abc", Map.of());

        ChannelToken token = client.handleCallback(request);

        assertThat(token.accessToken()).isEqualTo("mock-access-douyin");
        assertThat(token.refreshToken()).isEqualTo("mock-refresh-douyin");
        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.channelType()).isEqualTo("douyin");
        assertThat(token.accountName()).isEqualTo("抖音测试店铺");
        assertThat(token.expiresAt()).isNotNull();
        assertThat(token.expiresAt()).isAfter(java.time.OffsetDateTime.now());
        assertThat(token.refreshExpiresAt()).isNotNull();
        assertThat(token.refreshExpiresAt()).isAfter(token.expiresAt());
        assertThat(token.createdAt()).isNotNull();
        assertThat(token.updatedAt()).isNotNull();
    }

    @Test
    void refreshTokenPreservesIdentityAndSlidesExpiry() {
        DouyinChannelClient client = new DouyinChannelClient(mockProps());
        ChannelToken old = client.handleCallback(
                new ChannelCallbackRequest(ChannelType.DOUYIN, "code-1", "s", Map.of()));

        ChannelToken fresh = client.refreshToken(old);

        assertThat(fresh.accountName()).isEqualTo(old.accountName());
        assertThat(fresh.accessToken()).isEqualTo(old.accessToken());
        assertThat(fresh.refreshToken()).isEqualTo(old.refreshToken());
        assertThat(fresh.tokenType()).isEqualTo(old.tokenType());
        assertThat(fresh.channelType()).isEqualTo(old.channelType());
        assertThat(fresh.tenantId()).isEqualTo(old.tenantId());
        assertThat(fresh.organizationId()).isEqualTo(old.organizationId());
        assertThat(fresh.expiresAt()).isNotNull();
        assertThat(fresh.expiresAt()).isAfter(java.time.OffsetDateTime.now());
        assertThat(fresh.refreshExpiresAt()).isAfter(fresh.expiresAt());
        assertThat(fresh.updatedAt()).isNotNull();
    }

    @Test
    void checkConnectivityInMockModeReachable() {
        DouyinChannelClient client = new DouyinChannelClient(mockProps());

        ChannelConnectivity connectivity = client.checkConnectivity(authContext(Map.of()));

        assertThat(connectivity.reachable()).isTrue();
        assertThat(connectivity.message()).contains("mock 通道可达（真实网关待联调）");
        assertThat(connectivity.checkedAt()).isNotNull();
    }

    @Test
    void pullOrdersInMockModeReturnsTwoSamplesWithContractFields() {
        DouyinChannelClient client = new DouyinChannelClient(mockProps());

        List<ChannelOrder> orders = client.pullOrders(authContext(Map.of()));

        assertThat(orders).hasSize(2);
        ChannelOrder first = orders.get(0);
        assertThat(first.platformOrderId()).isEqualTo("dy-order-1001");
        assertThat(first.skuId()).isEqualTo("dy-sku-1");
        assertThat(first.quantity()).isBetween(1, 2);
        assertThat(first.unitPrice()).isBetween(new BigDecimal("9.9"), new BigDecimal("199"));
        assertThat(first.buyerName()).isEqualTo("测试买家");
        assertThat(first.buyerPhoneMask()).isEqualTo("138****0000");
        assertThat(first.shippingAddress()).isEqualTo("浙江省杭州市余杭区测试路1号");
    }

    @Test
    void pullProductsInMockModeReturnsOnSaleSamples() {
        DouyinChannelClient client = new DouyinChannelClient(mockProps());

        List<ChannelProduct> products = client.pullProducts(authContext(Map.of()));

        assertThat(products).hasSize(2);
        assertThat(products.get(0).platformProductId()).isEqualTo("dy-prod-1001");
        assertThat(products.get(0).title()).isEqualTo("抖音测试商品-1");
        assertThat(products.get(0).status()).isEqualTo("on_sale");
        assertThat(products.get(1).platformProductId()).isEqualTo("dy-prod-1002");
    }

    @Test
    void shipOrderInMockModeSucceedsWithPrefixedShipmentId() {
        DouyinChannelClient client = new DouyinChannelClient(mockProps());
        ChannelShipRequest request = new ChannelShipRequest(
                "tenant-1", "org-1", "acct-1", "store-1",
                "dy-order-1001", "顺丰速运", "SF1234567890", Map.of());

        ChannelShipResult result = client.shipOrder(request);

        assertThat(result.success()).isTrue();
        assertThat(result.platformOrderId()).isEqualTo("dy-order-1001");
        assertThat(result.message()).isEqualTo("ok");
        assertThat(result.platformShipmentId()).isEqualTo("douyin-ship-dy-order-1001");
    }

    @Test
    void pullAfterSalesInMockModeReturnsOneRefundingSample() {
        DouyinChannelClient client = new DouyinChannelClient(mockProps());

        List<ChannelAfterSale> afterSales = client.pullAfterSales(authContext(Map.of()));

        assertThat(afterSales).hasSize(1);
        ChannelAfterSale afterSale = afterSales.get(0);
        assertThat(afterSale.afterSaleId()).isEqualTo("dy-as-1");
        assertThat(afterSale.platformOrderId()).isEqualTo("dy-order-1001");
        assertThat(afterSale.status()).isEqualTo("refunding");
        assertThat(afterSale.reason()).isEqualTo("买家申请退款");
        assertThat(afterSale.refundAmount()).isEqualByComparingTo("9.90");
        assertThat(afterSale.createdAt()).isNotNull();
    }

    // ---------------- mock-mode=false：真实 URL 形态 & 纯函数断言 ----------------

    @Test
    void buildAuthorizeUrlInRealModeUsesSameRealAuthorizeShape() {
        DouyinChannelClient client = new DouyinChannelClient(realProps());
        ChannelAuthContext context = authContext(Map.of("state", "state-xyz", "scope", "user_info"));

        String url = client.buildAuthorizeUrl(context);

        assertThat(url).startsWith("https://open.douyin.com/platform/oauth/connect/?");
        assertThat(url).contains("client_key=dy-app-key-1");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("scope=user_info");
        assertThat(url).contains("redirect_uri=https://example.com/callback/douyin");
        assertThat(url).contains("state=state-xyz");
    }

    @Test
    void buildAccessTokenRequestBodyPureFunctionMatchesExpectedJson() {
        String body = DouyinChannelClient.buildAccessTokenRequestBody("dy-app-key-1", "dy-secret-1", "code-xyz");

        assertThat(body).isEqualTo(
                "{\"client_key\":\"dy-app-key-1\","
                        + "\"client_secret\":\"dy-secret-1\","
                        + "\"code\":\"code-xyz\","
                        + "\"grant_type\":\"authorization_code\"}");
    }

    // ---------------- registry 解析 ----------------

    @Test
    void registryResolvesDouyinClientAndThrows8101ForUnknownType() {
        DouyinChannelClient client = new DouyinChannelClient(mockProps());
        ChannelClientRegistry registry = new ChannelClientRegistry(List.of(client));

        assertThat(registry.supports(ChannelType.DOUYIN)).isTrue();
        assertThat(registry.resolve(ChannelType.DOUYIN)).isSameAs(client);
        assertThat(registry.supports(ChannelType.WECHAT_CHANNELS)).isFalse();

        assertThatThrownBy(() -> registry.resolve(ChannelType.WECHAT_CHANNELS))
                .isInstanceOf(BusinessException.class)
                .matches(error -> ((BusinessException) error).code().equals("8101"));
    }
}
