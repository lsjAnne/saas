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

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaobaoChannelClientTest {

    private TaobaoChannelClient mockClient() {
        ChannelPlatformProperties props = new ChannelPlatformProperties();
        ChannelPlatformSettings s = new ChannelPlatformSettings();
        s.setMockMode(true);
        s.setEndpoint("https://eco.taobao.com/router/rest");
        s.setAppKey("cfg-tb-key");
        s.setAppSecret("cfg-tb-secret");
        props.setPlatforms(Map.of("taobao", s));
        return new TaobaoChannelClient(props);
    }

    private TaobaoChannelClient realClient() {
        ChannelPlatformProperties props = new ChannelPlatformProperties();
        ChannelPlatformSettings s = new ChannelPlatformSettings();
        s.setMockMode(false);
        s.setEndpoint("https://eco.taobao.com/router/rest");
        s.setAppKey("cfg-tb-key");
        s.setAppSecret("cfg-tb-secret");
        props.setPlatforms(Map.of("taobao", s));
        return new TaobaoChannelClient(props);
    }

    private ChannelAuthContext ctx(String appKey, String redirectUri, Map<String, Object> extra) {
        return new ChannelAuthContext("t1", "o1", "acc-1", "store-1",
                appKey, "ctx-secret", redirectUri, extra);
    }

    @Test
    void channelTypeIsTaobao() {
        assertThat(mockClient().channelType()).isEqualTo(ChannelType.TAOBAO);
    }

    // ---------------- mock-mode=true：逐方法契约断言 ----------------

    @Test
    void buildAuthorizeUrlUsesTaobaoShapeWithState() {
        TaobaoChannelClient client = mockClient();
        ChannelAuthContext context = ctx("app-key-456", "https://shop.example.com/callback",
                Map.of("state", "st-xyz"));

        String url = client.buildAuthorizeUrl(context);

        assertThat(url).startsWith("https://oauth.taobao.com/authorize?");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("client_id=app-key-456");
        assertThat(url).doesNotContain("app_id=");
        assertThat(url).contains("redirect_uri=" + URLEncoder.encode("https://shop.example.com/callback", StandardCharsets.UTF_8));
        assertThat(url).contains("state=st-xyz");
    }

    @Test
    void buildAuthorizeUrlOmitsStateWhenAbsent() {
        String url = mockClient().buildAuthorizeUrl(ctx("app-key-456", "https://shop.example.com/callback", Map.of()));
        assertThat(url).doesNotContain("state=");
        assertThat(url).contains("client_id=app-key-456");
    }

    @Test
    void handleCallbackReturnsMockToken() {
        ChannelToken token = mockClient().handleCallback(
                new ChannelCallbackRequest(ChannelType.TAOBAO, "auth-code", "st", Map.of()));

        assertThat(token.channelType()).isEqualTo("taobao");
        assertThat(token.accountName()).isEqualTo("淘宝测试店铺");
        assertThat(token.accessToken()).isEqualTo("mock-access-taobao");
        assertThat(token.refreshToken()).isEqualTo("mock-refresh-taobao");
        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresAt()).isAfter(OffsetDateTime.now().plusDays(6));
        assertThat(token.expiresAt()).isBefore(OffsetDateTime.now().plusDays(8));
        assertThat(token.refreshExpiresAt()).isAfter(OffsetDateTime.now().plusDays(29));
        assertThat(token.refreshExpiresAt()).isBefore(OffsetDateTime.now().plusDays(31));
        assertThat(token.createdAt()).isNotNull();
        assertThat(token.updatedAt()).isNotNull();
    }

    @Test
    void refreshTokenPreservesFieldsAndExtendsExpiry() {
        TaobaoChannelClient client = mockClient();
        ChannelToken current = client.handleCallback(
                new ChannelCallbackRequest(ChannelType.TAOBAO, "code", "st", Map.of()));

        ChannelToken refreshed = client.refreshToken(current);

        assertThat(refreshed.accessToken()).isEqualTo(current.accessToken());
        assertThat(refreshed.refreshToken()).isEqualTo(current.refreshToken());
        assertThat(refreshed.tokenType()).isEqualTo(current.tokenType());
        assertThat(refreshed.accountName()).isEqualTo(current.accountName());
        assertThat(refreshed.channelType()).isEqualTo(current.channelType());
        assertThat(refreshed.createdAt()).isEqualTo(current.createdAt());
        assertThat(refreshed.expiresAt()).isAfter(OffsetDateTime.now().plusDays(6));
        assertThat(refreshed.refreshExpiresAt()).isAfter(OffsetDateTime.now().plusDays(29));
    }

    @Test
    void checkConnectivityMockIsReachable() {
        ChannelConnectivity c = mockClient().checkConnectivity(ctx("k", "r", Map.of()));
        assertThat(c.reachable()).isTrue();
        assertThat(c.message()).contains("mock 通道可达（真实网关待联调）");
        assertThat(c.checkedAt()).isNotNull();
    }

    @Test
    void pullOrdersReturnsTwoSampleOrders() {
        List<ChannelOrder> orders = mockClient().pullOrders(ctx("k", "r", Map.of()));

        assertThat(orders).hasSize(2);
        ChannelOrder first = orders.get(0);
        assertThat(first.platformOrderId()).isEqualTo("tb-order-1001");
        assertThat(first.skuId()).isEqualTo("tb-sku-1");
        assertThat(first.productId()).isEqualTo("tb-prod-1001");
        assertThat(first.quantity()).isEqualTo(1);
        assertThat(first.unitPrice()).isEqualByComparingTo(new BigDecimal("9.90"));
        assertThat(first.buyerName()).isEqualTo("测试买家");
        assertThat(first.buyerPhoneMask()).isEqualTo("138****0000");
        assertThat(first.shippingAddress()).isEqualTo("浙江省杭州市余杭区测试路1号");
        assertThat(orders.get(1).platformOrderId()).isEqualTo("tb-order-1002");
        assertThat(orders.get(1).quantity()).isEqualTo(2);
    }

    @Test
    void pullProductsReturnsTwoOnSaleSamples() {
        List<ChannelProduct> products = mockClient().pullProducts(ctx("k", "r", Map.of()));

        assertThat(products).hasSize(2);
        assertThat(products.get(0).platformProductId()).isEqualTo("tb-prod-1001");
        assertThat(products.get(0).title()).isEqualTo("淘宝测试商品-1");
        assertThat(products.get(0).status()).isEqualTo("on_sale");
        assertThat(products.get(1).platformProductId()).isEqualTo("tb-prod-1002");
        assertThat(products.get(1).title()).isEqualTo("淘宝测试商品-2");
    }

    @Test
    void shipOrderReturnsOkWithPrefixedShipmentId() {
        ChannelShipRequest request = new ChannelShipRequest("t1", "o1", "acc", "store",
                "tb-order-1001", "顺丰", "SF456", Map.of());

        ChannelShipResult result = mockClient().shipOrder(request);

        assertThat(result.platformOrderId()).isEqualTo("tb-order-1001");
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("ok");
        assertThat(result.platformShipmentId()).isEqualTo("taobao-ship-tb-order-1001");
    }

    @Test
    void pullAfterSalesReturnsOneRefundingSample() {
        List<ChannelAfterSale> afterSales = mockClient().pullAfterSales(ctx("k", "r", Map.of()));

        assertThat(afterSales).hasSize(1);
        ChannelAfterSale as = afterSales.get(0);
        assertThat(as.afterSaleId()).isEqualTo("tb-as-1");
        assertThat(as.status()).isEqualTo("refunding");
        assertThat(as.reason()).isEqualTo("买家申请退款");
        assertThat(as.refundAmount()).isEqualByComparingTo(new BigDecimal("9.90"));
        assertThat(as.createdAt()).isNotNull();
    }

    // ---------------- mock-mode=false：授权 URL 形态 + 签名纯函数 ----------------

    @Test
    void realModeAuthorizeUrlKeepsTaobaoShape() {
        String url = realClient().buildAuthorizeUrl(ctx("real-app-key", "https://shop.example.com/cb",
                Map.of("state", "st")));

        assertThat(url).startsWith("https://oauth.taobao.com/authorize?");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("client_id=real-app-key");
    }

    @Test
    void taobaoSignMatchesHandComputedHmacMd5Vector() {
        // key="tb-secret"；消息 = 排序拼接 "methodtaobao.trades.sold.get" + "timestamp2024-01-01 00:00:00"
        Map<String, String> params = Map.of(
                "method", "taobao.trades.sold.get",
                "timestamp", "2024-01-01 00:00:00");
        String sign = TaobaoChannelClient.taobaoSign(params, "tb-secret");
        assertThat(sign).isEqualTo("79676010b31776a4cc6ffd20d2334dc1");
    }

    @Test
    void hmacMd5HexIsLowercase32() {
        String sign = TaobaoChannelClient.hmacMd5Hex("methodtaobao.trades.sold.gettimestamp2024-01-01 00:00:00", "tb-secret");
        assertThat(sign).isEqualTo("79676010b31776a4cc6ffd20d2334dc1");
        assertThat(sign).hasSize(32);
    }

    @Test
    void taobaoCommonParamsAreFixedKeys() {
        Map<String, String> params = TaobaoChannelClient.taobaoCommonParams("taobao.trades.sold.get", "sess", "2024-01-01 00:00:00");
        assertThat(params.get("method")).isEqualTo("taobao.trades.sold.get");
        assertThat(params.get("timestamp")).isEqualTo("2024-01-01 00:00:00");
        assertThat(params.get("format")).isEqualTo("json");
        assertThat(params.get("v")).isEqualTo("2.0");
        assertThat(params.get("session")).isEqualTo("sess");
    }

    // ---------------- registry 解析 ----------------

    @Test
    void registryResolvesTaobaoAndThrows8101ForUnknown() {
        TaobaoChannelClient client = mockClient();
        ChannelClientRegistry registry = new ChannelClientRegistry(List.of(client));

        assertThat(registry.resolve(ChannelType.TAOBAO)).isSameAs(client);
        assertThat(registry.supports(ChannelType.TAOBAO)).isTrue();
        assertThat(registry.supports(ChannelType.PDD)).isFalse();
        assertThat(registry.supportedTypes()).containsExactly(ChannelType.TAOBAO);

        assertThatThrownBy(() -> registry.resolve(ChannelType.PDD))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).code().equals("8101"));
    }
}
