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
 * 视频号小店适配器（ChannelType.WECHAT_CHANNELS）。
 * <p>
 * mock-mode=true 时按契约第 6 节返回样例数据，不发任何真实网络请求；
 * mock-mode=false 时走 https://api.weixin.qq.com/channels/ec/... 网关，
 * access_token 通过 cgi-bin/token(gettoken) 获取并在类内缓存（简化版，待联调）。
 * 网关地址 / appKey / appSecret / mock 开关全部从 {@link ChannelPlatformProperties} 读取，
 * 授权 URL 与 gettoken 请求参数构造为包级可见纯函数，便于离线单测断言。
 */
@Component
public class WechatChannelsChannelClient implements ChannelClient {

    /** 视频号小店授权页地址（平台公共参数约定，可硬编码）。 */
    static final String AUTHORIZE_URL = "https://channels.weixin.qq.com/shop/open";
    /** gettoken 接口路径（拼接在网关 endpoint 之后）。 */
    static final String GET_TOKEN_PATH = "/cgi-bin/token";

    /** 类内简化 access_token 缓存（待联调：后续可替换为 Redis 分布式缓存）。 */
    private volatile String cachedAccessToken;
    private volatile OffsetDateTime tokenExpiresAt;

    private final ChannelPlatformProperties properties;

    public WechatChannelsChannelClient(ChannelPlatformProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.WECHAT_CHANNELS;
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
        return buildAuthorizeUrl(context.appKey(), context.redirectUri(), state);
    }

    /**
     * 纯函数：视频号小店授权 URL 拼接。
     * 形态：{@code https://channels.weixin.qq.com/shop/open?appid=..&redirect_uri=..&state=..}
     * state 为空时省略该参数。
     */
    static String buildAuthorizeUrl(String appid, String redirectUri, String state) {
        StringBuilder sb = new StringBuilder(AUTHORIZE_URL)
                .append("?appid=").append(appid)
                .append("&redirect_uri=").append(redirectUri);
        if (state != null && !state.isBlank()) {
            sb.append("&state=").append(state);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // gettoken（真实模式）：请求 URL 构造为纯函数
    // ------------------------------------------------------------------

    /**
     * 纯函数：视频号小店 gettoken 请求 URL。
     * 形态：{@code {base}/cgi-bin/token?grant_type=client_credential&appid=..&secret=..}
     *
     * @param baseUrl 网关根地址，如 https://api.weixin.qq.com
     */
    static String buildGetTokenUrl(String baseUrl, String appid, String secret) {
        String base = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return base + GET_TOKEN_PATH
                + "?grant_type=client_credential"
                + "&appid=" + appid
                + "&secret=" + secret;
    }

    /**
     * 简化版 access_token 缓存：未过期直接返回缓存，过期则重新 gettoken。
     * 待联调：需真实凭证验证；拉取与解析逻辑在 HTTP 隔离方法中完成。
     */
    private String getAccessToken(ChannelPlatformSettings settings) {
        if (cachedAccessToken != null
                && tokenExpiresAt != null
                && tokenExpiresAt.isAfter(OffsetDateTime.now().plusSeconds(60))) {
            return cachedAccessToken;
        }
        String url = buildGetTokenUrl(settings.getEndpoint(), settings.getAppKey(), settings.getAppSecret());
        String responseJson = httpGet(url);
        // 待联调：需真实凭证验证；解析 responseJson 中的 access_token / expires_in，
        // 回填 cachedAccessToken 与 tokenExpiresAt（now + expires_in 秒 - 60s 缓冲）
        throw new UnsupportedOperationException(
                "wechat_channels getAccessToken 真实模式待联调, gettoken url=" + url
                        + ", response=" + responseJson);
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
        // 待联调：需真实凭证验证；视频号小店授权 code 换取店铺 access_token，
        // 店铺维度 token 与账号维度 gettoken 不同，需按官方文档对接 /channels/ec/permit 相关接口
        getAccessToken(settings);
        throw new UnsupportedOperationException("wechat_channels handleCallback 真实模式待联调");
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
                "视频号测试店铺",
                "mock-access-wechat_channels",
                "mock-refresh-wechat_channels",
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
        // 待联调：需真实凭证验证；携带 access_token 调用 /channels/ec/order/list 等接口判定可达性
        ChannelPlatformSettings settings = properties.settings(channelType());
        getAccessToken(settings);
        return new ChannelConnectivity(true, "ok", OffsetDateTime.now());
    }

    // ------------------------------------------------------------------
    // 商品 / 订单 / 发货 / 售后（mock 样例数据）
    // ------------------------------------------------------------------

    @Override
    public List<ChannelProduct> pullProducts(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            List<ChannelProduct> products = new ArrayList<>();
            products.add(new ChannelProduct("wxch-prod-1001", "视频号测试商品-1", "wxch-sku-1",
                    new BigDecimal("99.00"), "on_sale", null));
            products.add(new ChannelProduct("wxch-prod-1002", "视频号测试商品-2", "wxch-sku-2",
                    new BigDecimal("199.00"), "on_sale", null));
            return products;
        }
        // 待联调：需真实凭证验证；GET /channels/ec/commodity/list 等，query 带 access_token
        throw new UnsupportedOperationException("wechat_channels pullProducts 真实模式待联调");
    }

    @Override
    public List<ChannelOrder> pullOrders(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            List<ChannelOrder> orders = new ArrayList<>();
            orders.add(new ChannelOrder("wxch-order-1001", "wxch-prod-1001", "wxch-sku-1",
                    1, new BigDecimal("99.00"),
                    "测试买家", "138****0000", "浙江省杭州市余杭区测试路1号", null));
            orders.add(new ChannelOrder("wxch-order-1002", "wxch-prod-1001", "wxch-sku-1",
                    2, new BigDecimal("19.90"),
                    "测试买家", "138****0000", "浙江省杭州市余杭区测试路1号", null));
            return orders;
        }
        // 待联调：需真实凭证验证；POST /channels/ec/order/list 等，query 带 access_token
        throw new UnsupportedOperationException("wechat_channels pullOrders 真实模式待联调");
    }

    @Override
    public ChannelShipResult shipOrder(ChannelShipRequest request) {
        if (properties.isMockMode(channelType())) {
            return new ChannelShipResult(
                    request.platformOrderId(),
                    true,
                    "ok",
                    "wechat_channels-ship-" + request.platformOrderId());
        }
        // 待联调：需真实凭证验证；POST /channels/ec/order/delivery/send 等，query 带 access_token
        throw new UnsupportedOperationException("wechat_channels shipOrder 真实模式待联调");
    }

    @Override
    public List<ChannelAfterSale> pullAfterSales(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            List<ChannelAfterSale> afterSales = new ArrayList<>();
            afterSales.add(new ChannelAfterSale("wxch-as-1", "wxch-order-1001", "refunding",
                    "买家申请退款", new BigDecimal("9.90"), OffsetDateTime.now(), null));
            return afterSales;
        }
        // 待联调：需真实凭证验证；POST /channels/ec/aftersale/list 等，query 带 access_token
        throw new UnsupportedOperationException("wechat_channels pullAfterSales 真实模式待联调");
    }

    // ------------------------------------------------------------------
    // HTTP 调用隔离点（真实模式）
    // ------------------------------------------------------------------

    private String httpGet(String url) {
        // 待联调：需真实凭证验证；此处为隔离的 HTTP 调用点，离线单测不进入
        throw new UnsupportedOperationException("real http call pending integration: GET " + url);
    }
}
