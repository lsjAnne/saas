package backend.channel.infrastructure.adapter;

import backend.channel.application.ChannelClient;
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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 抖音开放平台适配器（ChannelType.DOUYIN）。
 * <p>
 * mock-mode=true 时按契约第 6 节返回样例数据，不发任何真实网络请求；
 * mock-mode=false 时走 https://open.douyin.com 网关，鉴权用请求头 access-token。
 * 网关地址 / appKey / appSecret / mock 开关全部从 {@link ChannelPlatformProperties} 读取，
 * 授权 URL 与换 token 请求体构造为包级可见纯函数，便于离线单测断言。
 */
@Component
public class DouyinChannelClient implements ChannelClient {

    /** 抖音授权页地址（平台公共参数约定，可硬编码）。 */
    static final String AUTHORIZE_URL = "https://open.douyin.com/platform/oauth/connect/";
    /** 抖音换 token 接口路径（拼接在网关 endpoint 之后）。 */
    static final String ACCESS_TOKEN_PATH = "/oauth/access_token/";
    /** mock 模式默认 scope。 */
    static final String DEFAULT_SCOPE = "user_info";

    private final ChannelPlatformProperties properties;

    public DouyinChannelClient(ChannelPlatformProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.DOUYIN;
    }

    // ------------------------------------------------------------------
    // 授权 URL 拼接（mock / 真实模式共用同一形态，纯函数）
    // ------------------------------------------------------------------

    @Override
    public String buildAuthorizeUrl(ChannelAuthContext context) {
        String state = null;
        Object stateValue = context.extra() == null ? null : context.extra().get("state");
        if (stateValue instanceof String s && !s.isBlank()) {
            state = s;
        }
        String scope = DEFAULT_SCOPE;
        Object scopeValue = context.extra() == null ? null : context.extra().get("scope");
        if (scopeValue instanceof String s && !s.isBlank()) {
            scope = s;
        }
        return buildAuthorizeUrl(context.appKey(), context.redirectUri(), scope, state);
    }

    /**
     * 纯函数：抖音授权 URL 拼接。
     * 形态：{@code https://open.douyin.com/platform/oauth/connect/?client_key=..&response_type=code&scope=..&redirect_uri=..&state=..}
     * state 为空时省略该参数。
     */
    static String buildAuthorizeUrl(String clientKey, String redirectUri, String scope, String state) {
        StringBuilder sb = new StringBuilder(AUTHORIZE_URL)
                .append("?client_key=").append(clientKey)
                .append("&response_type=code");
        if (scope != null && !scope.isBlank()) {
            sb.append("&scope=").append(scope);
        }
        sb.append("&redirect_uri=").append(redirectUri);
        if (state != null && !state.isBlank()) {
            sb.append("&state=").append(state);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 换 token（真实模式）：请求体构造为纯函数
    // ------------------------------------------------------------------

    /**
     * 纯函数：抖音 {@code /oauth/access_token/} POST 请求体 JSON。
     * 字段：client_key / client_secret / code / grant_type=authorization_code。
     */
    static String buildAccessTokenRequestBody(String clientKey, String clientSecret, String code) {
        return "{"
                + "\"client_key\":\"" + clientKey + "\","
                + "\"client_secret\":\"" + clientSecret + "\","
                + "\"code\":\"" + code + "\","
                + "\"grant_type\":\"authorization_code\""
                + "}";
    }

    // ------------------------------------------------------------------
    // OAuth 回调 & token 刷新
    // ------------------------------------------------------------------

    @Override
    public ChannelToken handleCallback(ChannelCallbackRequest request) {
        if (properties.isMockMode(channelType())) {
            return mockToken();
        }
        ChannelPlatformSettings settings = properties.settings(channelType());
        // 待联调：需真实凭证验证；POST {endpoint}/oauth/access_token/，请求体见 buildAccessTokenRequestBody
        String url = settings.getEndpoint() + ACCESS_TOKEN_PATH;
        String body = buildAccessTokenRequestBody(settings.getAppKey(), settings.getAppSecret(), request.code());
        String responseJson = postJson(url, body);
        // 待联调：解析 responseJson 中的 data.access_token / data.refresh_token / data.expires_in / data.open_id ...
        throw new UnsupportedOperationException(
                "douyin handleCallback 真实模式待联调, exchanged code via " + url + ", raw body=" + body
                        + ", response=" + responseJson);
    }

    @Override
    public ChannelToken refreshToken(ChannelToken current) {
        // mock / 真实模式共用：保留原身份与凭证字段，仅顺延过期时间与 updatedAt
        OffsetDateTime now = OffsetDateTime.now();
        return new ChannelToken(
                current.channelTokenId(),
                current.tenantId(),
                current.organizationId(),
                current.channelType(),
                current.accountName(),
                current.accessToken(),
                current.refreshToken(),
                current.tokenType(),
                current.scope(),
                now.plusDays(7),
                now.plusDays(30),
                current.extraConfig(),
                current.createdAt() == null ? now : current.createdAt(),
                now
        );
    }

    private ChannelToken mockToken() {
        OffsetDateTime now = OffsetDateTime.now();
        return new ChannelToken(
                null,
                null,
                null,
                channelType().code(),
                "抖音测试店铺",
                "mock-access-douyin",
                "mock-refresh-douyin",
                "Bearer",
                null,
                now.plusDays(7),
                now.plusDays(30),
                null,
                now,
                now
        );
    }

    // ------------------------------------------------------------------
    // 连通性
    // ------------------------------------------------------------------

    @Override
    public ChannelConnectivity checkConnectivity(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            return new ChannelConnectivity(true, "mock 通道可达（真实网关待联调）", OffsetDateTime.now());
        }
        // 待联调：需真实凭证验证；携带 access-token 请求头调用网关健康/订单查询接口判定可达性
        boolean reachable = pingGateway(context);
        return new ChannelConnectivity(reachable, reachable ? "ok" : "unreachable", OffsetDateTime.now());
    }

    private boolean pingGateway(ChannelAuthContext context) {
        // 待联调：需真实凭证验证；隔离的 HTTP 调用点，离线单测不进入本方法
        throw new UnsupportedOperationException("douyin pingGateway 真实模式待联调");
    }

    // ------------------------------------------------------------------
    // 商品 / 订单 / 发货 / 售后（mock 样例数据）
    // ------------------------------------------------------------------

    @Override
    public List<ChannelProduct> pullProducts(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            List<ChannelProduct> products = new ArrayList<>();
            products.add(new ChannelProduct("dy-prod-1001", "抖音测试商品-1", "dy-sku-1",
                    new BigDecimal("99.00"), "on_sale", null));
            products.add(new ChannelProduct("dy-prod-1002", "抖音测试商品-2", "dy-sku-2",
                    new BigDecimal("199.00"), "on_sale", null));
            return products;
        }
        // 待联调：需真实凭证验证；GET /product/list 等，鉴权请求头 access-token
        throw new UnsupportedOperationException("douyin pullProducts 真实模式待联调");
    }

    @Override
    public List<ChannelOrder> pullOrders(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            List<ChannelOrder> orders = new ArrayList<>();
            orders.add(new ChannelOrder("dy-order-1001", "dy-prod-1001", "dy-sku-1",
                    1, new BigDecimal("99.00"),
                    "测试买家", "138****0000", "浙江省杭州市余杭区测试路1号", null));
            orders.add(new ChannelOrder("dy-order-1002", "dy-prod-1001", "dy-sku-1",
                    2, new BigDecimal("19.90"),
                    "测试买家", "138****0000", "浙江省杭州市余杭区测试路1号", null));
            return orders;
        }
        // 待联调：需真实凭证验证；GET /order/searchList 等，鉴权请求头 access-token
        throw new UnsupportedOperationException("douyin pullOrders 真实模式待联调");
    }

    @Override
    public ChannelShipResult shipOrder(ChannelShipRequest request) {
        if (properties.isMockMode(channelType())) {
            return new ChannelShipResult(
                    request.platformOrderId(),
                    true,
                    "ok",
                    "douyin-ship-" + request.platformOrderId());
        }
        // 待联调：需真实凭证验证；POST /order/logisticsAdd，鉴权请求头 access-token
        throw new UnsupportedOperationException("douyin shipOrder 真实模式待联调");
    }

    @Override
    public List<ChannelAfterSale> pullAfterSales(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            List<ChannelAfterSale> afterSales = new ArrayList<>();
            afterSales.add(new ChannelAfterSale("dy-as-1", "dy-order-1001", "refunding",
                    "买家申请退款", new BigDecimal("9.90"), OffsetDateTime.now(), null));
            return afterSales;
        }
        // 待联调：需真实凭证验证；GET /afterSale/list 等，鉴权请求头 access-token
        throw new UnsupportedOperationException("douyin pullAfterSales 真实模式待联调");
    }

    // ------------------------------------------------------------------
    // HTTP 调用隔离点（真实模式）
    // ------------------------------------------------------------------

    private String postJson(String url, String json) {
        // 待联调：需真实凭证验证；此处为隔离的 HTTP 调用点，离线单测不进入
        throw new UnsupportedOperationException("real http call pending integration: POST " + url);
    }
}
