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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 京东（JD 宙斯）渠道适配器。
 *
 * <p>mock-mode=true：严格按渠道契约第 6 节返回样例数据，不发任何网络请求。
 * mock-mode=false：网关 https://api.jd.com/routerjson，公共参数 app_key/timestamp/sign/method/360buy_param_json/access_token，
 * MD5 签名（待签名串 = appSecret + 字典序拼接 key+value + appSecret）。
 * 授权 URL 拼接、公共参数构造（含 360buy_param_json 业务参数 JSON）、待签名串拼装与 MD5 摘要均为
 * package-private 纯函数，可离线单测；真实 HTTP 调用经 {@link #postRouterJson} 隔离，标注待联调。
 */
@Component
public class JdChannelClient implements ChannelClient {

    static final String AUTHORIZE_URL = "https://openapi.jd.com/oauth/authorize";
    private static final String DEFAULT_GATEWAY = "https://api.jd.com/routerjson";
    private static final String MOCK_REACHABLE_MESSAGE = "mock 通道可达（真实网关待联调）";

    private final ChannelPlatformProperties properties;

    public JdChannelClient(ChannelPlatformProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.JD;
    }

    // ------------------------------------------------------------------
    // 授权 URL（mock / 真实同形态，纯函数拼接）
    // ------------------------------------------------------------------

    @Override
    public String buildAuthorizeUrl(ChannelAuthContext context) {
        ChannelPlatformSettings settings = properties.settings(channelType());
        String appKey = notBlank(context.appKey()) ? context.appKey() : settings.getAppKey();
        String redirectUri = notBlank(context.redirectUri()) ? context.redirectUri() : settings.getCallbackPath();
        String state = null;
        if (context.extra() != null && context.extra().get("state") != null) {
            state = String.valueOf(context.extra().get("state"));
        }
        return buildAuthorizeUrl(appKey, redirectUri, state);
    }

    /** 纯函数：拼装京东授权 URL（app_key / response_type=code / redirect_uri / state）。 */
    static String buildAuthorizeUrl(String appKey, String redirectUri, String state) {
        StringBuilder sb = new StringBuilder(AUTHORIZE_URL)
                .append("?app_key=").append(urlEncode(appKey))
                .append("&response_type=code")
                .append("&redirect_uri=").append(urlEncode(redirectUri));
        if (notBlank(state)) {
            sb.append("&state=").append(urlEncode(state));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 回调换 token
    // ------------------------------------------------------------------

    @Override
    public ChannelToken handleCallback(ChannelCallbackRequest request) {
        if (properties.isMockMode(channelType())) {
            return handleCallbackMock();
        }
        return handleCallbackReal(request);
    }

    private ChannelToken handleCallbackMock() {
        OffsetDateTime now = OffsetDateTime.now();
        return new ChannelToken(
                null,
                "mock-tenant",
                "mock-org",
                channelType().code(),
                "京东测试店铺",
                "mock-access-jd",
                "mock-refresh-jd",
                "Bearer",
                null,
                now.plusDays(7),
                now.plusDays(30),
                null,
                now,
                now
        );
    }

    private ChannelToken handleCallbackReal(ChannelCallbackRequest request) {
        ChannelPlatformSettings settings = properties.settings(channelType());
        // 待联调：需真实凭证验证。
        // 待签名串 = appSecret + 字典序拼接 key+value + appSecret，再 MD5 小写（见 buildSign）。
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("code", request.code());
        String paramsJson = toBuyerParamJson(biz);
        Map<String, String> params = routerJsonParams(
                settings.getAppKey(), null, "jingdong.pop.oauth.token",
                timestamp(), paramsJson);
        params.put("sign", buildSign(params, settings.getAppSecret()));
        String response = postRouterJson(settings.getEndpoint(), params);
        // 待联调：解析宙斯 access_token JSON 响应（access_token / expires_in / refresh_token / nickName）映射为 ChannelToken
        throw new IllegalStateException("待联调：京东 access_token 响应解析需真实凭证验证；原始响应=" + response);
    }

    // ------------------------------------------------------------------
    // 刷新 token
    // ------------------------------------------------------------------

    @Override
    public ChannelToken refreshToken(ChannelToken current) {
        OffsetDateTime now = OffsetDateTime.now();
        if (properties.isMockMode(channelType())) {
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
                    current.createdAt(),
                    now
            );
        }
        // 待联调：需真实凭证验证。调 jingdong.pop.oauth.refreshToken，业务参数放 360buy_param_json。
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("refresh_token", current.refreshToken());
        Map<String, String> params = routerJsonParams(
                properties.appKey(channelType()), null, "jingdong.pop.oauth.refreshToken",
                timestamp(), toBuyerParamJson(biz));
        params.put("sign", buildSign(params, properties.appSecret(channelType())));
        String response = postRouterJson(properties.endpoint(channelType()), params);
        throw new IllegalStateException("待联调：京东 refresh_token 响应解析需真实凭证验证；原始响应=" + response);
    }

    // ------------------------------------------------------------------
    // 连通性
    // ------------------------------------------------------------------

    @Override
    public ChannelConnectivity checkConnectivity(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            return new ChannelConnectivity(true, MOCK_REACHABLE_MESSAGE, OffsetDateTime.now());
        }
        // 待联调：需真实凭证验证。调 jingdong.shop.info.get，HTTP 200 即视为可达。
        Map<String, String> params = routerJsonParams(
                properties.appKey(channelType()), null, "jingdong.shop.info.get",
                timestamp(), "{}");
        params.put("sign", buildSign(params, properties.appSecret(channelType())));
        String response = postRouterJson(properties.endpoint(channelType()), params);
        throw new IllegalStateException("待联调：京东连通性探测需真实凭证验证；原始响应=" + response);
    }

    // ------------------------------------------------------------------
    // 商品 / 订单 / 售后拉取
    // ------------------------------------------------------------------

    @Override
    public List<ChannelProduct> pullProducts(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            return List.of(
                    new ChannelProduct("jd-prod-1001", "京东测试商品-1", "jd-sku-1",
                            new BigDecimal("99.90"), "on_sale", null),
                    new ChannelProduct("jd-prod-1002", "京东测试商品-2", "jd-sku-2",
                            new BigDecimal("19.90"), "on_sale", null)
            );
        }
        // 待联调：需真实凭证验证。调 jingdong.readWare.getList，业务参数放 360buy_param_json。
        Map<String, String> params = routerJsonParams(
                properties.appKey(channelType()), context == null ? null : context.channelAccountId(),
                "jingdong.readWare.getList", timestamp(), "{}");
        params.put("sign", buildSign(params, properties.appSecret(channelType())));
        String response = postRouterJson(properties.endpoint(channelType()), params);
        throw new IllegalStateException("待联调：京东商品拉取需真实凭证验证；原始响应=" + response);
    }

    @Override
    public List<ChannelOrder> pullOrders(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            return List.of(
                    new ChannelOrder("jd-order-1001", "jd-prod-1001", "jd-sku-1",
                            1, new BigDecimal("99.90"),
                            "测试买家", "138****0000", "浙江省杭州市余杭区测试路1号", null),
                    new ChannelOrder("jd-order-1002", "jd-prod-1002", "jd-sku-2",
                            2, new BigDecimal("19.90"),
                            "测试买家", "138****0000", "浙江省杭州市余杭区测试路1号", null)
            );
        }
        // 待联调：需真实凭证验证。调 jingdong.pop.order.list，业务参数放 360buy_param_json。
        Map<String, String> params = routerJsonParams(
                properties.appKey(channelType()), context == null ? null : context.channelAccountId(),
                "jingdong.pop.order.list", timestamp(), "{}");
        params.put("sign", buildSign(params, properties.appSecret(channelType())));
        String response = postRouterJson(properties.endpoint(channelType()), params);
        throw new IllegalStateException("待联调：京东订单拉取需真实凭证验证；原始响应=" + response);
    }

    @Override
    public List<ChannelAfterSale> pullAfterSales(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            return List.of(
                    new ChannelAfterSale("jd-as-1", "jd-order-1001", "refunding",
                            "买家申请退款", new BigDecimal("9.90"), OffsetDateTime.now(), null)
            );
        }
        // 待联调：需真实凭证验证。调 jingdong.pop.afssale.list，业务参数放 360buy_param_json。
        Map<String, String> params = routerJsonParams(
                properties.appKey(channelType()), context == null ? null : context.channelAccountId(),
                "jingdong.pop.afssale.list", timestamp(), "{}");
        params.put("sign", buildSign(params, properties.appSecret(channelType())));
        String response = postRouterJson(properties.endpoint(channelType()), params);
        throw new IllegalStateException("待联调：京东售后拉取需真实凭证验证；原始响应=" + response);
    }

    // ------------------------------------------------------------------
    // 发货
    // ------------------------------------------------------------------

    @Override
    public ChannelShipResult shipOrder(ChannelShipRequest request) {
        if (properties.isMockMode(channelType())) {
            return new ChannelShipResult(request.platformOrderId(), true, "ok",
                    "jd-ship-" + request.platformOrderId());
        }
        // 待联调：需真实凭证验证。调 jingdong.pop.order.ship，业务参数放 360buy_param_json。
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("orderId", request.platformOrderId());
        biz.put("logisticsCompany", request.logisticsCompany());
        biz.put("logisticsNo", request.logisticsNo());
        Map<String, String> params = routerJsonParams(
                properties.appKey(channelType()), request.channelAccountId(),
                "jingdong.pop.order.ship", timestamp(), toBuyerParamJson(biz));
        params.put("sign", buildSign(params, properties.appSecret(channelType())));
        String response = postRouterJson(properties.endpoint(channelType()), params);
        throw new IllegalStateException("待联调：京东发货回执解析需真实凭证验证；原始响应=" + response);
    }

    // ------------------------------------------------------------------
    // 纯函数：公共参数 / 签名 / MD5（离线单测断言用）
    // ------------------------------------------------------------------

    /**
     * 宙斯网关公共参数（不含 sign）：app_key / method / timestamp / 360buy_param_json / access_token。
     */
    static Map<String, String> routerJsonParams(String appKey, String accessToken,
                                               String method, String timestamp, String buyerParamJson) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_key", appKey);
        params.put("method", method);
        params.put("timestamp", timestamp);
        params.put("360buy_param_json", buyerParamJson == null ? "{}" : buyerParamJson);
        if (notBlank(accessToken)) {
            params.put("access_token", accessToken);
        }
        return params;
    }

    /**
     * 京东待签名串：appSecret + 公共参数按 key 字典序拼接 key+value + appSecret，整体 MD5 小写。
     * 360buy_param_json 作为普通参数参与签名（值为业务参数 JSON 原文）。
     */
    static String buildSign(Map<String, String> params, String appSecret) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        String secret = appSecret == null ? "" : appSecret;
        StringBuilder sb = new StringBuilder(secret);
        sorted.forEach((k, v) -> sb.append(k).append(v));
        sb.append(secret);
        return md5Hex(sb.toString());
    }

    /** 极小业务参数 JSON 序列化（不引入第三方依赖，保证签名输入字节级稳定可断言）。 */
    static String toBuyerParamJson(Map<String, Object> bizParams) {
        if (bizParams == null || bizParams.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        bizParams.forEach((k, v) -> sb.append('"').append(escape(k)).append("\":").append(jsonValue(v)).append(','));
        sb.setLength(sb.length() - 1);
        return sb.append('}').toString();
    }

    static String md5Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                        .append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm unavailable", e);
        }
    }

    private static String timestamp() {
        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .format(OffsetDateTime.now());
    }

    private static String jsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return '"' + escape(String.valueOf(value)) + '"';
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 真实 HTTP 调用隔离点。
     * 待联调：需真实凭证验证 —— 用 RestClient/WebClient POST 表单到 routerjson 网关，带签名公共参数。
     * 离线单测不触发真实网络，真实模式下由联调环境替换为真实实现。
     */
    protected String postRouterJson(String endpoint, Map<String, String> params) {
        String base = notBlank(endpoint) ? endpoint : DEFAULT_GATEWAY;
        // 待联调：需真实凭证验证
        throw new UnsupportedOperationException(
                "待联调：京东宙斯真实网关 POST " + base + " 需真实凭证验证，离线环境不发起网络请求");
    }
}
