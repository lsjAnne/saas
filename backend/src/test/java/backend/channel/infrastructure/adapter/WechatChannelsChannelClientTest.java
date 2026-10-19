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

class WechatChannelsChannelClientTest {

    private static final String APP_KEY = "wxch-app-id-1";
    private static final String APP_SECRET = "wxch-secret-1";
    private static final String REDIRECT_URI = "https://example.com/callback/wechat_channels";

    private ChannelPlatformProperties mockProps() {
        ChannelPlatformProperties.ChannelPlatformSettings settings = new ChannelPlatformProperties.ChannelPlatformSettings();
        settings.setMockMode(true);
        settings.setEndpoint("https://api.weixin.qq.com");
        settings.setAppKey(APP_KEY);
        settings.setAppSecret(APP_SECRET);
        ChannelPlatformProperties properties = new ChannelPlatformProperties();
        properties.setPlatforms(Map.of("wechat_channels", settings));
        return properties;
    }

    private ChannelPlatformProperties realProps() {
        ChannelPlatformProperties.ChannelPlatformSettings settings = new ChannelPlatformProperties.ChannelPlatformSettings();
        settings.setMockMode(false);
        settings.setEndpoint("https://api.weixin.qq.com");
        settings.setAppKey(APP_KEY);
        settings.setAppSecret(APP_SECRET);
        ChannelPlatformProperties properties = new ChannelPlatformProperties();
        properties.setPlatforms(Map.of("wechat_channels", settings));
        return properties;
    }

    private ChannelAuthContext authContext(Map<String, Object> extra) {
        return new ChannelAuthContext(
                "tenant-1", "org-1", "acct-1", "store-1",
                APP_KEY, APP_SECRET, REDIRECT_URI, extra);
    }

    @Test
    void channelTypeIsWechatChannels() {
        WechatChannelsChannelClient client = new WechatChannelsChannelClient(mockProps());
        assertThat(client.channelType()).isEqualTo(ChannelType.WECHAT_CHANNELS);
    }

    // ---------------- mock-mode=true：逐方法契约断言 ----------------

    @Test
    void buildAuthorizeUrlInMockModeContainsAppidAndRedirectUriAndState() {
        WechatChannelsChannelClient client = new WechatChannelsChannelClient(mockProps());
        ChannelAuthContext context = authContext(Map.of("state", "state-abc"));

        String url = client.buildAuthorizeUrl(context);

        assertThat(url).isEqualTo(
                "https://channels.weixin.qq.com/shop/open"
                        + "?appid=wxch-app-id-1"
                        + "&redirect_uri=https://example.com/callback/wechat_channels"
                        + "&state=state-abc");
    }

    @Test
    void buildAuthorizeUrlOmitsStateWhenAbsent() {
        WechatChannelsChannelClient client = new WechatChannelsChannelClient(mockProps());
        ChannelAuthContext context = authContext(Map.of());

        String url = client.buildAuthorizeUrl(context);

        assertThat(url).startsWith("https://channels.weixin.qq.com/shop/open?appid=wxch-app-id-1");
        assertThat(url).contains("redirect_uri=https://example.com/callback/wechat_channels");
        assertThat(url).doesNotContain("state=");
    }

    @Test
    void handleCallbackInMockModeReturnsFixedTokenFields() {
        WechatChannelsChannelClient client = new WechatChannelsChannelClient(mockProps());
        ChannelCallbackRequest request = new ChannelCallbackRequest(
                ChannelType.WECHAT_CHANNELS, "code-123", "state-abc", Map.of());

        ChannelToken token = client.handleCallback(request);

        assertThat(token.accessToken()).isEqualTo("mock-access-wechat_channels");
        assertThat(token.refreshToken()).isEqualTo("mock-refresh-wechat_channels");
        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.channelType()).isEqualTo("wechat_channels");
        assertThat(token.accountName()).isEqualTo("视频号测试店铺");
        assertThat(token.expiresAt()).isNotNull();
        assertThat(token.expiresAt()).isAfter(java.time.OffsetDateTime.now());
        assertThat(token.refreshExpiresAt()).isNotNull();
        assertThat(token.refreshExpiresAt()).isAfter(token.expiresAt());
        assertThat(token.createdAt()).isNotNull();
        assertThat(token.updatedAt()).isNotNull();
    }

    @Test
    void refreshTokenPreservesIdentityAndSlidesExpiry() {
        WechatChannelsChannelClient client = new WechatChannelsChannelClient(mockProps());
        ChannelToken old = client.handleCallback(
                new ChannelCallbackRequest(ChannelType.WECHAT_CHANNELS, "code-1", "s", Map.of()));

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
        WechatChannelsChannelClient client = new WechatChannelsChannelClient(mockProps());

        ChannelConnectivity connectivity = client.checkConnectivity(authContext(Map.of()));

        assertThat(connectivity.reachable()).isTrue();
        assertThat(connectivity.message()).contains("mock 通道可达（真实网关待联调）");
        assertThat(connectivity.checkedAt()).isNotNull();
    }

    @Test
    void pullOrdersInMockModeReturnsTwoSamplesWithContractFields() {
        WechatChannelsChannelClient client = new WechatChannelsChannelClient(mockProps());

        List<ChannelOrder> orders = client.pullOrders(authContext(Map.of()));

        assertThat(orders).hasSize(2);
        ChannelOrder first = orders.get(0);
        assertThat(first.platformOrderId()).isEqualTo("wxch-order-1001");
        assertThat(first.skuId()).isEqualTo("wxch-sku-1");
        assertThat(first.quantity()).isBetween(1, 2);
        assertThat(first.unitPrice()).isBetween(new BigDecimal("9.9"), new BigDecimal("199"));
        assertThat(first.buyerName()).isEqualTo("测试买家");
        assertThat(first.buyerPhoneMask()).isEqualTo("138****0000");
        assertThat(first.shippingAddress()).isEqualTo("浙江省杭州市余杭区测试路1号");
    }

    @Test
    void pullProductsInMockModeReturnsOnSaleSamples() {
        WechatChannelsChannelClient client = new WechatChannelsChannelClient(mockProps());

        List<ChannelProduct> products = client.pullProducts(authContext(Map.of()));

        assertThat(products).hasSize(2);
        assertThat(products.get(0).platformProductId()).isEqualTo("wxch-prod-1001");
        assertThat(products.get(0).title()).isEqualTo("视频号测试商品-1");
        assertThat(products.get(0).status()).isEqualTo("on_sale");
        assertThat(products.get(1).platformProductId()).isEqualTo("wxch-prod-1002");
    }

    @Test
    void shipOrderInMockModeSucceedsWithPrefixedShipmentId() {
        WechatChannelsChannelClient client = new WechatChannelsChannelClient(mockProps());
        ChannelShipRequest request = new ChannelShipRequest(
                "tenant-1", "org-1", "acct-1", "store-1",
                "wxch-order-1001", "顺丰速运", "SF1234567890", Map.of());

        ChannelShipResult result = client.shipOrder(request);

        assertThat(result.success()).isTrue();
        assertThat(result.platformOrderId()).isEqualTo("wxch-order-1001");
        assertThat(result.message()).isEqualTo("ok");
        assertThat(result.platformShipmentId()).isEqualTo("wechat_channels-ship-wxch-order-1001");
    }

    @Test
    void pullAfterSalesInMockModeReturnsOneRefundingSample() {
        WechatChannelsChannelClient client = new WechatChannelsChannelClient(mockProps());

        List<ChannelAfterSale> afterSales = client.pullAfterSales(authContext(Map.of()));

        assertThat(afterSales).hasSize(1);
        ChannelAfterSale afterSale = afterSales.get(0);
        assertThat(afterSale.afterSaleId()).isEqualTo("wxch-as-1");
        assertThat(afterSale.platformOrderId()).isEqualTo("wxch-order-1001");
        assertThat(afterSale.status()).isEqualTo("refunding");
        assertThat(afterSale.reason()).isEqualTo("买家申请退款");
        assertThat(afterSale.refundAmount()).isEqualByComparingTo("9.90");
        assertThat(afterSale.createdAt()).isNotNull();
    }

    // ---------------- mock-mode=false：真实 URL 形态 & 纯函数断言 ----------------

    @Test
    void buildAuthorizeUrlInRealModeUsesSameRealAuthorizeShape() {
        WechatChannelsChannelClient client = new WechatChannelsChannelClient(realProps());
        ChannelAuthContext context = authContext(Map.of("state", "state-xyz"));

        String url = client.buildAuthorizeUrl(context);

        assertThat(url).startsWith("https://channels.weixin.qq.com/shop/open?");
        assertThat(url).contains("appid=wxch-app-id-1");
        assertThat(url).contains("redirect_uri=https://example.com/callback/wechat_channels");
        assertThat(url).contains("state=state-xyz");
    }

    @Test
    void buildGetTokenUrlPureFunctionMatchesExpectedQuery() {
        String url = WechatChannelsChannelClient.buildGetTokenUrl(
                "https://api.weixin.qq.com", "wxch-app-id-1", "wxch-secret-1");

        assertThat(url).isEqualTo(
                "https://api.weixin.qq.com/cgi-bin/token"
                        + "?grant_type=client_credential"
                        + "&appid=wxch-app-id-1"
                        + "&secret=wxch-secret-1");
    }

    @Test
    void buildGetTokenUrlStripsTrailingSlashOfBase() {
        String url = WechatChannelsChannelClient.buildGetTokenUrl(
                "https://api.weixin.qq.com/", "appid-x", "secret-y");

        assertThat(url).startsWith("https://api.weixin.qq.com/cgi-bin/token?");
        assertThat(url).contains("appid=appid-x");
        assertThat(url).contains("secret=secret-y");
    }

    // ---------------- registry 解析 ----------------

    @Test
    void registryResolvesWechatChannelsClientAndThrows8101ForUnknownType() {
        WechatChannelsChannelClient client = new WechatChannelsChannelClient(mockProps());
        ChannelClientRegistry registry = new ChannelClientRegistry(List.of(client));

        assertThat(registry.supports(ChannelType.WECHAT_CHANNELS)).isTrue();
        assertThat(registry.resolve(ChannelType.WECHAT_CHANNELS)).isSameAs(client);
        assertThat(registry.supports(ChannelType.DOUYIN)).isFalse();

        assertThatThrownBy(() -> registry.resolve(ChannelType.DOUYIN))
                .isInstanceOf(BusinessException.class)
                .matches(error -> ((BusinessException) error).code().equals("8101"));
    }
}
