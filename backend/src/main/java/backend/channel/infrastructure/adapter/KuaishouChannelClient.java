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
 * 快手小店（KUAISHOU）渠道适配器。
 *
 * <p>mock-mode=true：严格按渠道契约第 6 节返回样例数据，不发任何网络请求。
 * mock-mode=false：网关 https://open.kwaixiaodian.com，公共参数 appKey/signMethod/timestamp/sign，
 * MD5 签名。授权 URL 拼接、公共参数构造、待签名串拼装与 MD5 摘要均为 package-private 纯函数，
 * 可离线单测；真实 HTTP 调用经 {@link #postForm} 隔离，标注待联调。
 */
@Component
public class KuaishouChannelClient implements ChannelClient {

    static final String AUTHORIZE_URL = "https://open.kwaixiaodian.com/oauth/authorize";
    private static final String DEFAULT_GATEWAY = "https://open.kwaixiaodian.com";
    private static final String MOCK_REACHABLE_MESSAGE = "mock 通道可达（真实网关待联调）";

    private final ChannelPlatformProperties properties;

    public KuaishouChannelClient(ChannelPlatformProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.KUAISHOU;
    }

    // ------------------------------------------------------------------
    // 授权 URL（mock / 真实同形态，纯函数拼接）
    // ------------------------------------------------------------------

    @Override
    public String buildAuthorizeUrl(ChannelAuthContext context) {
        ChannelPlatformSettings settings = properties.settings(channelType());
        String appId = notBlank(context.appKey()) ? context.appKey() : settings.getAppKey();
        String redirectUri = notBlank(context.redirectUri()) ? context.redirectUri() : settings.getCallbackPath();
        String state = null;
        if (context.extra() != null && context.extra().get("state") != null) {
            state = String.valueOf(context.extra().get("state"));
        }
        return buildAuthorizeUrl(appId, redirectUri, state);
    }

    /** 纯函数：拼装快手授权 URL（app_id / redirect_uri / state）。 */
    static String buildAuthorizeUrl(String appId, String redirectUri, String state) {
        StringBuilder sb = new StringBuilder(AUTHORIZE_URL)
                .append("?app_id=").append(urlEncode(appId))
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
                "快手测试店铺",
                "mock-access-kuaishou",
                "mock-refresh-kuaishou",
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
        // 待签名串 = 按 key 字典序拼接 key+value 后末尾追加 appSecret，再 MD5 小写（见 buildSign）。
        Map<String, String> params = new TreeMap<>();
        params.put("appKey", settings.getAppKey());
        params.put("signMethod", "md5");
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
        params.put("code", request.code());
        params.put("sign", buildSign(params, settings.getAppSecret()));
        String response = postForm(settings.getEndpoint(), "/oauth/access_token", params);
        // 待联调：解析快手 access_token JSON 响应（access_token / expires_in / refresh_token / shop_name）映射为 ChannelToken
        throw new IllegalStateException("待联调：快手 access_token 响应解析需真实凭证验证；原始响应=" + response);
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
        // 待联调：需真实凭证验证。用 current.refreshToken() 调 /oauth/refresh_token，重签后复用 handleCallbackReal 的响应解析。
        Map<String, String> params = new TreeMap<>();
        params.put("appKey", properties.appKey(channelType()));
        params.put("signMethod", "md5");
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
        params.put("refresh_token", current.refreshToken());
        params.put("sign", buildSign(params, properties.appSecret(channelType())));
        String response = postForm(properties.endpoint(channelType()), "/oauth/refresh_token", params);
        throw new IllegalStateException("待联调：快手 refresh_token 响应解析需真实凭证验证；原始响应=" + response);
    }

    // ------------------------------------------------------------------
    // 连通性
    // ------------------------------------------------------------------

    @Override
    public ChannelConnectivity checkConnectivity(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            return new ChannelConnectivity(true, MOCK_REACHABLE_MESSAGE, OffsetDateTime.now());
        }
        // 待联调：需真实凭证验证。GET endpoint/shop/basicInfo 带签名公共参数，HTTP 200 即可达。
        Map<String, String> params = new TreeMap<>();
        params.put("appKey", properties.appKey(channelType()));
        params.put("signMethod", "md5");
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
        params.put("sign", buildSign(params, properties.appSecret(channelType())));
        String response = postForm(properties.endpoint(channelType()), "/shop/basicInfo", params);
        throw new IllegalStateException("待联调：快手连通性探测需真实凭证验证；原始响应=" + response);
    }

    // ------------------------------------------------------------------
    // 商品 / 订单 / 售后拉取
    // ------------------------------------------------------------------

    @Override
    public List<ChannelProduct> pullProducts(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            return List.of(
                    new ChannelProduct("ks-prod-1001", "快手测试商品-1", "ks-sku-1",
                            new BigDecimal("99.90"), "on_sale", null),
                    new ChannelProduct("ks-prod-1002", "快手测试商品-2", "ks-sku-2",
                            new BigDecimal("19.90"), "on_sale", null)
            );
        }
        // 待联调：需真实凭证验证。调 openProduct.list，按 buildSign 签名后解析商品列表。
        Map<String, String> params = new TreeMap<>();
        params.put("appKey", properties.appKey(channelType()));
        params.put("signMethod", "md5");
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
        params.put("method", "openProduct.list");
        params.put("sign", buildSign(params, properties.appSecret(channelType())));
        String response = postForm(properties.endpoint(channelType()), "/openProduct/list", params);
        throw new IllegalStateException("待联调：快手商品拉取需真实凭证验证；原始响应=" + response);
    }

    @Override
    public List<ChannelOrder> pullOrders(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            return List.of(
                    new ChannelOrder("ks-order-1001", "ks-prod-1001", "ks-sku-1",
                            1, new BigDecimal("99.90"),
                            "测试买家", "138****0000", "浙江省杭州市余杭区测试路1号", null),
                    new ChannelOrder("ks-order-1002", "ks-prod-1002", "ks-sku-2",
                            2, new BigDecimal("19.90"),
                            "测试买家", "138****0000", "浙江省杭州市余杭区测试路1号", null)
            );
        }
        // 待联调：需真实凭证验证。调 openOrder.list，按 buildSign 签名后解析订单列表。
        Map<String, String> params = new TreeMap<>();
        params.put("appKey", properties.appKey(channelType()));
        params.put("signMethod", "md5");
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
        params.put("method", "openOrder.list");
        params.put("sign", buildSign(params, properties.appSecret(channelType())));
        String response = postForm(properties.endpoint(channelType()), "/openOrder/list", params);
        throw new IllegalStateException("待联调：快手订单拉取需真实凭证验证；原始响应=" + response);
    }

    @Override
    public List<ChannelAfterSale> pullAfterSales(ChannelAuthContext context) {
        if (properties.isMockMode(channelType())) {
            return List.of(
                    new ChannelAfterSale("ks-as-1", "ks-order-1001", "refunding",
                            "买家申请退款", new BigDecimal("9.90"), OffsetDateTime.now(), null)
            );
        }
        // 待联调：需真实凭证验证。调 openAfterSale.list，按 buildSign 签名后解析售后列表。
        Map<String, String> params = new TreeMap<>();
        params.put("appKey", properties.appKey(channelType()));
        params.put("signMethod", "md5");
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
        params.put("method", "openAfterSale.list");
        params.put("sign", buildSign(params, properties.appSecret(channelType())));
        String response = postForm(properties.endpoint(channelType()), "/openAfterSale/list", params);
        throw new IllegalStateException("待联调：快手售后拉取需真实凭证验证；原始响应=" + response);
    }

    // ------------------------------------------------------------------
    // 发货
    // ------------------------------------------------------------------

    @Override
    public ChannelShipResult shipOrder(ChannelShipRequest request) {
        if (properties.isMockMode(channelType())) {
            return new ChannelShipResult(request.platformOrderId(), true, "ok",
                    "kuaishou-ship-" + request.platformOrderId());
        }
        // 待联调：需真实凭证验证。调 openOrder.logisticsUpload，入参 platformOrderId/logisticsCompany/logisticsNo，按 buildSign 签名。
        Map<String, String> params = new TreeMap<>();
        params.put("appKey", properties.appKey(channelType()));
        params.put("signMethod", "md5");
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
        params.put("method", "openOrder.logisticsUpload");
        params.put("orderId", request.platformOrderId());
        params.put("logisticsCompany", request.logisticsCompany());
        params.put("logisticsNo", request.logisticsNo());
        params.put("sign", buildSign(params, properties.appSecret(channelType())));
        String response = postForm(properties.endpoint(channelType()), "/openOrder/logisticsUpload", params);
        throw new IllegalStateException("待联调：快手发货回执解析需真实凭证验证；原始响应=" + response);
    }

    // ------------------------------------------------------------------
    // 纯函数：公共参数 / 签名 / MD5（离线单测断言用）
    // ------------------------------------------------------------------

    /**
     * 快手待签名串：公共参数按 key 字典序拼接 key+value，末尾追加 appSecret，整体 MD5 小写。
     */
    static String buildSign(Map<String, String> params, String appSecret) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        sorted.forEach((k, v) -> sb.append(k).append(v));
        sb.append(appSecret == null ? "" : appSecret);
        return md5Hex(sb.toString());
    }

    /** 公共参数（不含 sign）。 */
    static Map<String, String> commonParams(String appKey, String timestamp, String method) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("appKey", appKey);
        params.put("signMethod", "md5");
        params.put("timestamp", timestamp);
        params.put("method", method);
        return params;
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

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 真实 HTTP 调用隔离点。
     * 待联调：需真实凭证验证 —— 用 RestClient/WebClient POST 表单到 endpoint + path，带签名公共参数。
     * 离线单测不触发真实网络，真实模式下由联调环境替换为真实实现。
     */
    protected String postForm(String endpoint, String path, Map<String, String> params) {
        String base = notBlank(endpoint) ? endpoint : DEFAULT_GATEWAY;
        // 待联调：需真实凭证验证
        throw new UnsupportedOperationException(
                "待联调：快手小店真实网关 POST " + base + path + " 需真实凭证验证，离线环境不发起网络请求");
    }
}
