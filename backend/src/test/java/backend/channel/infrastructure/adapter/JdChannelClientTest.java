package backend.channel.infrastructure.adapter;

import backend.channel.application.ChannelClientRegistry;
import backend.channel.application.ChannelPlatformProperties;
import backend.channel.application.ChannelPlatformProperties.ChannelPlatformSettings;
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

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdChannelClientTest {

    private ChannelPlatformProperties props(boolean mockMode) {
        ChannelPlatformProperties properties = new ChannelPlatformProperties();
        ChannelPlatformSettings settings = new ChannelPlatformSettings();
        settings.setMockMode(mockMode);
        settings.setEndpoint("https://api.jd.com/routerjson");
        settings.setAppKey("jd-app");
        settings.setAppSecret("jd-secret");
        properties.setPlatforms(Map.of("jd", settings));
        return properties;
    }

    private ChannelAuthContext authContext() {
        return new ChannelAuthContext("t1", "o1", "acct-1", "store-1",
                "jd-app", "jd-secret", "https://shop.example.com/callback",
                Map.of("state", "xyz-state"));
    }

    // ------------------------------------------------------------------
    // mock-mode = true：逐方法断言契约第 6 节
    // ------------------------------------------------------------------

    @Test
    void channelTypeIsJd() {
        JdChannelClient client = new JdChannelClient(props(true));
        assertThat(client.channelType()).isEqualTo(ChannelType.JD);
    }

    @Test
    void mockBuildAuthorizeUrlUsesAppKeyParam() {
        JdChannelClient client = new JdChannelClient(props(true));
        String url = client.buildAuthorizeUrl(authContext());
        assertThat(url).startsWith("https://openapi.jd.com/oauth/authorize?");
        assertThat(url).contains("app_key=jd-app");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("redirect_uri=");
        assertThat(url).contains("state=xyz-state");
    }

    @Test
    void mockHandleCallbackReturnsFixedToken() {
        JdChannelClient client = new JdChannelClient(props(true));
        ChannelCallbackRequest request = new ChannelCallbackRequest(
                ChannelType.JD, "code-123", "xyz-state", Map.of());
        ChannelToken token = client.handleCallback(request);

        assertThat(token.channelType()).isEqualTo("jd");
        assertThat(token.accountName()).isEqualTo("京东测试店铺");
        assertThat(token.accessToken()).isEqualTo("mock-access-jd");
        assertThat(token.refreshToken()).isEqualTo("mock-refresh-jd");
        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresAt()).isNotNull().isAfter(OffsetDateTime.now().plusDays(6));
        assertThat(token.refreshExpiresAt()).isNotNull().isAfter(OffsetDateTime.now().plusDays(29));
        assertThat(token.createdAt()).isNotNull();
        assertThat(token.updatedAt()).isNotNull();
    }

    @Test
    void mockRefreshTokenKeepsFieldsAndExtendsExpiry() {
        JdChannelClient client = new JdChannelClient(props(true));
        ChannelToken current = client.handleCallback(
                new ChannelCallbackRequest(ChannelType.JD, "c", "s", Map.of()));
        ChannelToken refreshed = client.refreshToken(current);

        assertThat(refreshed.channelTokenId()).isEqualTo(current.channelTokenId());
        assertThat(refreshed.tenantId()).isEqualTo(current.tenantId());
        assertThat(refreshed.organizationId()).isEqualTo(current.organizationId());
        assertThat(refreshed.channelType()).isEqualTo(current.channelType());
        assertThat(refreshed.accountName()).isEqualTo(current.accountName());
        assertThat(refreshed.accessToken()).isEqualTo(current.accessToken());
        assertThat(refreshed.refreshToken()).isEqualTo(current.refreshToken());
        assertThat(refreshed.tokenType()).isEqualTo(current.tokenType());
        assertThat(refreshed.scope()).isEqualTo(current.scope());
        assertThat(refreshed.expiresAt()).isAfter(OffsetDateTime.now().plusDays(6));
        assertThat(refreshed.refreshExpiresAt()).isAfter(OffsetDateTime.now().plusDays(29));
        assertThat(refreshed.updatedAt()).isNotNull();
    }

    @Test
    void mockCheckConnectivityReachable() {
        JdChannelClient client = new JdChannelClient(props(true));
        ChannelConnectivity connectivity = client.checkConnectivity(authContext());
        assertThat(connectivity.reachable()).isTrue();
        assertThat(connectivity.message()).contains("mock 通道可达（真实网关待联调）");
        assertThat(connectivity.checkedAt()).isNotNull();
    }

    @Test
    void mockPullOrdersReturnsSamples() {
        JdChannelClient client = new JdChannelClient(props(true));
        List<ChannelOrder> orders = client.pullOrders(authContext());

        assertThat(orders).hasSizeBetween(1, 2);
        ChannelOrder first = orders.get(0);
        assertThat(first.platformOrderId()).startsWith("jd-order-1001");
        assertThat(first.skuId()).startsWith("jd-sku-");
        assertThat(first.quantity()).isBetween(1, 2);
        assertThat(first.unitPrice()).isBetween(new java.math.BigDecimal("9.9"), new java.math.BigDecimal("199"));
        assertThat(first.buyerName()).isEqualTo("测试买家");
        assertThat(first.buyerPhoneMask()).isEqualTo("138****0000");
        assertThat(first.shippingAddress()).isEqualTo("浙江省杭州市余杭区测试路1号");
    }

    @Test
    void mockPullProductsReturnsSamples() {
        JdChannelClient client = new JdChannelClient(props(true));
        List<ChannelProduct> products = client.pullProducts(authContext());

        assertThat(products).hasSizeBetween(1, 2);
        ChannelProduct first = products.get(0);
        assertThat(first.platformProductId()).startsWith("jd-prod-1001");
        assertThat(first.title()).startsWith("京东测试商品");
        assertThat(first.status()).isEqualTo("on_sale");
        assertThat(first.price()).isNotNull();
    }

    @Test
    void mockShipOrderSucceeds() {
        JdChannelClient client = new JdChannelClient(props(true));
        ChannelShipRequest request = new ChannelShipRequest(
                "t1", "o1", "acct-1", "store-1", "jd-order-1001", "顺丰", "SF1001", null);
        ChannelShipResult result = client.shipOrder(request);

        assertThat(result.platformOrderId()).isEqualTo("jd-order-1001");
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("ok");
        assertThat(result.platformShipmentId()).isEqualTo("jd-ship-jd-order-1001");
    }

    @Test
    void mockPullAfterSalesReturnsSample() {
        JdChannelClient client = new JdChannelClient(props(true));
        List<ChannelAfterSale> afterSales = client.pullAfterSales(authContext());

        assertThat(afterSales).hasSize(1);
        ChannelAfterSale first = afterSales.get(0);
        assertThat(first.afterSaleId()).startsWith("jd-as-1");
        assertThat(first.status()).isEqualTo("refunding");
        assertThat(first.createdAt()).isNotNull();
    }

    // ------------------------------------------------------------------
    // mock-mode = false：授权 URL 形态 + 签名纯函数已知向量
    // ------------------------------------------------------------------

    @Test
    void realBuildAuthorizeUrlStillUsesJdAuthorizeShape() {
        JdChannelClient client = new JdChannelClient(props(false));
        String url = client.buildAuthorizeUrl(authContext());
        assertThat(url).startsWith("https://openapi.jd.com/oauth/authorize?");
        assertThat(url).contains("app_key=jd-app");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("state=xyz-state");
    }

    @Test
    void md5HexMatchesKnownVector() {
        assertThat(JdChannelClient.md5Hex("test"))
                .isEqualTo("098f6bcd4621d373cade4e832627b4f6");
    }

    @Test
    void routerJsonParamsContain360buyParamJson() {
        Map<String, String> params = JdChannelClient.routerJsonParams(
                "jdak", "token-1", "jingdong.pop.order.list.get", "2024-01-01 00:00:00", "{\"page\":1}");
        assertThat(params.get("app_key")).isEqualTo("jdak");
        assertThat(params.get("method")).isEqualTo("jingdong.pop.order.list.get");
        assertThat(params.get("timestamp")).isEqualTo("2024-01-01 00:00:00");
        assertThat(params.get("360buy_param_json")).isEqualTo("{\"page\":1}");
        assertThat(params.get("access_token")).isEqualTo("token-1");
    }

    @Test
    void toBuyerParamJsonSerializesBizParams() {
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("page", 1);
        biz.put("orderId", "A123");
        assertThat(JdChannelClient.toBuyerParamJson(biz)).isEqualTo("{\"page\":1,\"orderId\":\"A123\"}");
    }

    @Test
    void buildSignMatchesHandComputedVector() {
        // 待签名串 = jdsec + 公共参数按 key 字典序拼接 key+value + jdsec：
        // jdsec360buy_param_json{"page":1}app_keyjdakmethodjingdong.pop.order.list.gettimestamp2024-01-01 00:00:00jdsec
        Map<String, String> params = JdChannelClient.routerJsonParams(
                "jdak", null, "jingdong.pop.order.list.get", "2024-01-01 00:00:00", "{\"page\":1}");
        assertThat(JdChannelClient.buildSign(params, "jdsec"))
                .isEqualTo("c6fca7a259902a9c0e1bb0dfdc812f1d");
    }

    // ------------------------------------------------------------------
    // registry 解析
    // ------------------------------------------------------------------

    @Test
    void registryResolvesJdAndRejectsUnknown() {
        JdChannelClient client = new JdChannelClient(props(true));
        ChannelClientRegistry registry = new ChannelClientRegistry(List.of(client));

        assertThat(registry.resolve(ChannelType.JD)).isSameAs(client);
        assertThat(registry.supports(ChannelType.JD)).isTrue();
        assertThat(registry.supports(ChannelType.KUAISHOU)).isFalse();
        assertThat(registry.supportedTypes()).containsExactly(ChannelType.JD);

        assertThatThrownBy(() -> registry.resolve(ChannelType.KUAISHOU))
                .isInstanceOf(BusinessException.class)
                .matches(error -> ((BusinessException) error).code().equals("8101"));
    }
}
