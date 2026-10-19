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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KuaishouChannelClientTest {

    private ChannelPlatformProperties props(boolean mockMode) {
        ChannelPlatformProperties properties = new ChannelPlatformProperties();
        ChannelPlatformSettings settings = new ChannelPlatformSettings();
        settings.setMockMode(mockMode);
        settings.setEndpoint("https://open.kwaixiaodian.com");
        settings.setAppKey("ks-app");
        settings.setAppSecret("ks-secret");
        properties.setPlatforms(Map.of("kuaishou", settings));
        return properties;
    }

    private ChannelAuthContext authContext() {
        return new ChannelAuthContext("t1", "o1", "acct-1", "store-1",
                "ks-app", "ks-secret", "https://shop.example.com/callback",
                Map.of("state", "xyz-state"));
    }

    // ------------------------------------------------------------------
    // mock-mode = true：逐方法断言契约第 6 节
    // ------------------------------------------------------------------

    @Test
    void channelTypeIsKuaishou() {
        KuaishouChannelClient client = new KuaishouChannelClient(props(true));
        assertThat(client.channelType()).isEqualTo(ChannelType.KUAISHOU);
    }

    @Test
    void mockBuildAuthorizeUrlUsesAppIdParam() {
        KuaishouChannelClient client = new KuaishouChannelClient(props(true));
        String url = client.buildAuthorizeUrl(authContext());
        assertThat(url).startsWith("https://open.kwaixiaodian.com/oauth/authorize?");
        assertThat(url).contains("app_id=ks-app");
        assertThat(url).contains("redirect_uri=");
        assertThat(url).contains("state=xyz-state");
    }

    @Test
    void mockHandleCallbackReturnsFixedToken() {
        KuaishouChannelClient client = new KuaishouChannelClient(props(true));
        ChannelCallbackRequest request = new ChannelCallbackRequest(
                ChannelType.KUAISHOU, "code-123", "xyz-state", Map.of());
        ChannelToken token = client.handleCallback(request);

        assertThat(token.channelType()).isEqualTo("kuaishou");
        assertThat(token.accountName()).isEqualTo("快手测试店铺");
        assertThat(token.accessToken()).isEqualTo("mock-access-kuaishou");
        assertThat(token.refreshToken()).isEqualTo("mock-refresh-kuaishou");
        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresAt()).isNotNull().isAfter(OffsetDateTime.now().plusDays(6));
        assertThat(token.refreshExpiresAt()).isNotNull().isAfter(OffsetDateTime.now().plusDays(29));
        assertThat(token.createdAt()).isNotNull();
        assertThat(token.updatedAt()).isNotNull();
    }

    @Test
    void mockRefreshTokenKeepsFieldsAndExtendsExpiry() {
        KuaishouChannelClient client = new KuaishouChannelClient(props(true));
        ChannelToken current = client.handleCallback(
                new ChannelCallbackRequest(ChannelType.KUAISHOU, "c", "s", Map.of()));
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
        KuaishouChannelClient client = new KuaishouChannelClient(props(true));
        ChannelConnectivity connectivity = client.checkConnectivity(authContext());
        assertThat(connectivity.reachable()).isTrue();
        assertThat(connectivity.message()).contains("mock 通道可达（真实网关待联调）");
        assertThat(connectivity.checkedAt()).isNotNull();
    }

    @Test
    void mockPullOrdersReturnsSamples() {
        KuaishouChannelClient client = new KuaishouChannelClient(props(true));
        List<ChannelOrder> orders = client.pullOrders(authContext());

        assertThat(orders).hasSizeBetween(1, 2);
        ChannelOrder first = orders.get(0);
        assertThat(first.platformOrderId()).startsWith("ks-order-1001");
        assertThat(first.skuId()).startsWith("ks-sku-");
        assertThat(first.quantity()).isBetween(1, 2);
        assertThat(first.unitPrice()).isBetween(new java.math.BigDecimal("9.9"), new java.math.BigDecimal("199"));
        assertThat(first.buyerName()).isEqualTo("测试买家");
        assertThat(first.buyerPhoneMask()).isEqualTo("138****0000");
        assertThat(first.shippingAddress()).isEqualTo("浙江省杭州市余杭区测试路1号");
    }

    @Test
    void mockPullProductsReturnsSamples() {
        KuaishouChannelClient client = new KuaishouChannelClient(props(true));
        List<ChannelProduct> products = client.pullProducts(authContext());

        assertThat(products).hasSizeBetween(1, 2);
        ChannelProduct first = products.get(0);
        assertThat(first.platformProductId()).startsWith("ks-prod-1001");
        assertThat(first.title()).startsWith("快手测试商品");
        assertThat(first.status()).isEqualTo("on_sale");
        assertThat(first.price()).isNotNull();
    }

    @Test
    void mockShipOrderSucceeds() {
        KuaishouChannelClient client = new KuaishouChannelClient(props(true));
        ChannelShipRequest request = new ChannelShipRequest(
                "t1", "o1", "acct-1", "store-1", "ks-order-1001", "顺丰", "SF1001", null);
        ChannelShipResult result = client.shipOrder(request);

        assertThat(result.platformOrderId()).isEqualTo("ks-order-1001");
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("ok");
        assertThat(result.platformShipmentId()).isEqualTo("kuaishou-ship-ks-order-1001");
    }

    @Test
    void mockPullAfterSalesReturnsSample() {
        KuaishouChannelClient client = new KuaishouChannelClient(props(true));
        List<ChannelAfterSale> afterSales = client.pullAfterSales(authContext());

        assertThat(afterSales).hasSize(1);
        ChannelAfterSale first = afterSales.get(0);
        assertThat(first.afterSaleId()).startsWith("ks-as-1");
        assertThat(first.status()).isEqualTo("refunding");
        assertThat(first.createdAt()).isNotNull();
    }

    // ------------------------------------------------------------------
    // mock-mode = false：授权 URL 形态 + 签名纯函数已知向量
    // ------------------------------------------------------------------

    @Test
    void realBuildAuthorizeUrlStillUsesKuaishouAuthorizeShape() {
        KuaishouChannelClient client = new KuaishouChannelClient(props(false));
        String url = client.buildAuthorizeUrl(authContext());
        assertThat(url).startsWith("https://open.kwaixiaodian.com/oauth/authorize?");
        assertThat(url).contains("app_id=ks-app");
        assertThat(url).contains("state=xyz-state");
    }

    @Test
    void md5HexMatchesKnownVector() {
        assertThat(KuaishouChannelClient.md5Hex("test"))
                .isEqualTo("098f6bcd4621d373cade4e832627b4f6");
    }

    @Test
    void buildSignMatchesHandComputedVector() {
        // 待签名串 = appKey + ak + signMethod + md5 + timestamp + 1700000000 + sec（字典序 key 拼接）
        java.util.Map<String, String> params = new java.util.TreeMap<>();
        params.put("appKey", "ak");
        params.put("signMethod", "md5");
        params.put("timestamp", "1700000000");
        assertThat(KuaishouChannelClient.buildSign(params, "sec"))
                .isEqualTo("6b789bad18bcb666b8186aa7e67fe8c3");
    }

    // ------------------------------------------------------------------
    // registry 解析
    // ------------------------------------------------------------------

    @Test
    void registryResolvesKuaishouAndRejectsUnknown() {
        KuaishouChannelClient client = new KuaishouChannelClient(props(true));
        ChannelClientRegistry registry = new ChannelClientRegistry(List.of(client));

        assertThat(registry.resolve(ChannelType.KUAISHOU)).isSameAs(client);
        assertThat(registry.supports(ChannelType.KUAISHOU)).isTrue();
        assertThat(registry.supports(ChannelType.JD)).isFalse();
        assertThat(registry.supportedTypes()).containsExactly(ChannelType.KUAISHOU);

        assertThatThrownBy(() -> registry.resolve(ChannelType.JD))
                .isInstanceOf(BusinessException.class)
                .matches(error -> ((BusinessException) error).code().equals("8101"));
    }
}
