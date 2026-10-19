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

class PddChannelClientTest {

    private PddChannelClient mockClient() {
        ChannelPlatformProperties props = new ChannelPlatformProperties();
        ChannelPlatformSettings s = new ChannelPlatformSettings();
        s.setMockMode(true);
        s.setEndpoint("https://openapi.pinduoduo.com");
        s.setAppKey("cfg-pdd-key");
        s.setAppSecret("cfg-pdd-secret");
        props.setPlatforms(Map.of("pdd", s));
        return new PddChannelClient(props);
    }

    private PddChannelClient realClient() {
        ChannelPlatformProperties props = new ChannelPlatformProperties();
        ChannelPlatformSettings s = new ChannelPlatformSettings();
        s.setMockMode(false);
        s.setEndpoint("https://openapi.pinduoduo.com");
        s.setAppKey("cfg-pdd-key");
        s.setAppSecret("cfg-pdd-secret");
        props.setPlatforms(Map.of("pdd", s));
        return new PddChannelClient(props);
    }

    private ChannelAuthContext ctx(String appKey, String redirectUri, Map<String, Object> extra) {
        return new ChannelAuthContext("t1", "o1", "acc-1", "store-1",
                appKey, "ctx-secret", redirectUri, extra);
    }

    @Test
    void channelTypeIsPdd() {
        assertThat(mockClient().channelType()).isEqualTo(ChannelType.PDD);
    }

    // ---------------- mock-mode=true：逐方法契约断言 ----------------

    @Test
    void buildAuthorizeUrlUsesPddShapeWithState() {
        PddChannelClient client = mockClient();
        ChannelAuthContext context = ctx("app-key-123", "https://shop.example.com/callback",
                Map.of("state", "st-abc"));

        String url = client.buildAuthorizeUrl(context);

        assertThat(url).startsWith("https://fuwu.pinduoduo.com/oauth/authorize?");
        assertThat(url).contains("app_id=app-key-123");
        assertThat(url).contains("redirect_uri=" + URLEncoder.encode("https://shop.example.com/callback", StandardCharsets.UTF_8));
        assertThat(url).contains("state=st-abc");
    }

    @Test
    void buildAuthorizeUrlOmitsStateWhenAbsent() {
        String url = mockClient().buildAuthorizeUrl(ctx("app-key-123", "https://shop.example.com/callback", Map.of()));
        assertThat(url).doesNotContain("state=");
        assertThat(url).contains("app_id=app-key-123");
    }

    @Test
    void handleCallbackReturnsMockToken() {
        ChannelToken token = mockClient().handleCallback(
                new ChannelCallbackRequest(ChannelType.PDD, "auth-code", "st-abc", Map.of()));

        assertThat(token.channelType()).isEqualTo("pdd");
        assertThat(token.accountName()).isEqualTo("拼多多测试店铺");
        assertThat(token.accessToken()).isEqualTo("mock-access-pdd");
        assertThat(token.refreshToken()).isEqualTo("mock-refresh-pdd");
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
        PddChannelClient client = mockClient();
        ChannelToken current = client.handleCallback(
                new ChannelCallbackRequest(ChannelType.PDD, "code", "st", Map.of()));

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
        assertThat(first.platformOrderId()).isEqualTo("pdd-order-1001");
        assertThat(first.skuId()).isEqualTo("sku-1");
        assertThat(first.productId()).isEqualTo("pdd-prod-1001");
        assertThat(first.quantity()).isEqualTo(1);
        assertThat(first.unitPrice()).isEqualByComparingTo(new BigDecimal("9.90"));
        assertThat(first.buyerName()).isEqualTo("测试买家");
        assertThat(first.buyerPhoneMask()).isEqualTo("138****0000");
        assertThat(first.shippingAddress()).isEqualTo("浙江省杭州市余杭区测试路1号");
        assertThat(orders.get(1).platformOrderId()).isEqualTo("pdd-order-1002");
        assertThat(orders.get(1).quantity()).isEqualTo(2);
    }

    @Test
    void pullProductsReturnsTwoOnSaleSamples() {
        List<ChannelProduct> products = mockClient().pullProducts(ctx("k", "r", Map.of()));

        assertThat(products).hasSize(2);
        assertThat(products.get(0).platformProductId()).isEqualTo("pdd-prod-1001");
        assertThat(products.get(0).title()).isEqualTo("拼多多测试商品-1");
        assertThat(products.get(0).status()).isEqualTo("on_sale");
        assertThat(products.get(1).platformProductId()).isEqualTo("pdd-prod-1002");
        assertThat(products.get(1).title()).isEqualTo("拼多多测试商品-2");
    }

    @Test
    void shipOrderReturnsOkWithPrefixedShipmentId() {
        ChannelShipRequest request = new ChannelShipRequest("t1", "o1", "acc", "store",
                "pdd-order-1001", "圆通", "YT123", Map.of());

        ChannelShipResult result = mockClient().shipOrder(request);

        assertThat(result.platformOrderId()).isEqualTo("pdd-order-1001");
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("ok");
        assertThat(result.platformShipmentId()).isEqualTo("pdd-ship-pdd-order-1001");
    }

    @Test
    void pullAfterSalesReturnsOneRefundingSample() {
        List<ChannelAfterSale> afterSales = mockClient().pullAfterSales(ctx("k", "r", Map.of()));

        assertThat(afterSales).hasSize(1);
        ChannelAfterSale as = afterSales.get(0);
        assertThat(as.afterSaleId()).isEqualTo("pdd-as-1");
        assertThat(as.status()).isEqualTo("refunding");
        assertThat(as.reason()).isEqualTo("买家申请退款");
        assertThat(as.refundAmount()).isEqualByComparingTo(new BigDecimal("9.90"));
        assertThat(as.createdAt()).isNotNull();
    }

    // ---------------- mock-mode=false：授权 URL 形态 + 签名纯函数 ----------------

    @Test
    void realModeAuthorizeUrlKeepsPddShape() {
        String url = realClient().buildAuthorizeUrl(ctx("real-app-key", "https://shop.example.com/cb",
                Map.of("state", "st")));

        assertThat(url).startsWith("https://fuwu.pinduoduo.com/oauth/authorize?");
        assertThat(url).contains("app_id=real-app-key");
        assertThat(url).doesNotContain("client_id=");
    }

    @Test
    void md5HexMatchesKnownVector() {
        assertThat(PddChannelClient.md5Hex("test")).isEqualTo("098f6bcd4621d373cade4e832627b4f6");
    }

    @Test
    void pddSignMatchesHandComputedVector() {
        // clientId="cid" + 排序拼接 "a1"+"b2" + secret="sec" => "cida1b2sec"
        String sign = PddChannelClient.pddSign(Map.of("a", "1", "b", "2"), "cid", "sec");
        assertThat(sign).isEqualTo("9c46a2ffcdb80a09501d4bf783641b22");
    }

    @Test
    void pddCommonParamsAreSortedWithFixedKeys() {
        Map<String, String> params = PddChannelClient.pddCommonParams("pdd.order.list.increment.get", "tok", 1700000000L);
        assertThat(params.get("type")).isEqualTo("pdd.order.list.increment.get");
        assertThat(params.get("timestamp")).isEqualTo("1700000000");
        assertThat(params.get("data_type")).isEqualTo("json");
        assertThat(params.get("sign_method")).isEqualTo("md5");
        assertThat(params.get("access_token")).isEqualTo("tok");
    }

    // ---------------- registry 解析 ----------------

    @Test
    void registryResolvesPddAndThrows8101ForUnknown() {
        PddChannelClient client = mockClient();
        ChannelClientRegistry registry = new ChannelClientRegistry(List.of(client));

        assertThat(registry.resolve(ChannelType.PDD)).isSameAs(client);
        assertThat(registry.supports(ChannelType.PDD)).isTrue();
        assertThat(registry.supports(ChannelType.TAOBAO)).isFalse();
        assertThat(registry.supportedTypes()).containsExactly(ChannelType.PDD);

        assertThatThrownBy(() -> registry.resolve(ChannelType.TAOBAO))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).code().equals("8101"));
    }
}
