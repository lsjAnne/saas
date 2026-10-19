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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 拼多多开放平台（PDD）渠道适配器。
 * <p>
 * mock-mode=true：返回契约文档第 6 节规定的离线样例，不发起任何网络请求。
 * mock-mode=false：按契约文档第 7 节组装公共参数与 MD5 签名；真实 HTTP 调用隔离在私有方法中并标注待联调。
 */
@Component
public class PddChannelClient implements ChannelClient {

    static final String AUTHORIZE_BASE = "https://fuwu.pinduoduo.com/oauth/authorize";
    private static final String ACCOUNT_NAME = "拼多多测试店铺";

    private final ChannelPlatformProperties properties;

    public PddChannelClient(ChannelPlatformProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.PDD;
    }

    // ---------------------------------------------------------------------
    // OAuth 授权 URL：mock/真实模式形态一致（都是引导商家跳转的平台授权页）
    // ---------------------------------------------------------------------

    @Override
    public String buildAuthorizeUrl(ChannelAuthContext context) {
        StringBuilder sb = new StringBuilder(AUTHORIZE_BASE)
                .append("?app_id=").append(urlEnc(context.appKey()))
                .append("&redirect_uri=").append(urlEnc(context.redirectUri()));
        String state = extractState(context);
        if (!state.isEmpty()) {
            sb.append("&state=").append(urlEnc(state));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // 令牌
    // ---------------------------------------------------------------------

    @Override
    public ChannelToken handleCallback(ChannelCallbackRequest request) {
        if (properties.isMockMode(ChannelType.PDD)) {
            OffsetDateTime now = OffsetDateTime.now();
            return new ChannelToken(
                    null, null, null, ChannelType.PDD.code(), ACCOUNT_NAME,
                    "mock-access-pdd", "mock-refresh-pdd", "Bearer", null,
                    now.plusDays(7), now.plusDays(30), Map.of(), now, now
            );
        }
        return doRealHandleCallback(request);
    }

    @Override
    public ChannelToken refreshToken(ChannelToken current) {
        if (properties.isMockMode(ChannelType.PDD)) {
            OffsetDateTime now = OffsetDateTime.now();
            return new ChannelToken(
                    current.channelTokenId(), current.tenantId(), current.organizationId(),
                    current.channelType(), current.accountName(),
                    current.accessToken(), current.refreshToken(), current.tokenType(), current.scope(),
                    now.plusDays(7), now.plusDays(30), current.extraConfig(),
                    current.createdAt(), now
            );
        }
        // 待联调：需真实凭证验证 —— 用 refresh_token 向拼多多网关换取新 access_token
        throw new UnsupportedOperationException("PDD refresh_token 待联调");
    }

    // ---------------------------------------------------------------------
    // 连通性
    // ---------------------------------------------------------------------

    @Override
    public ChannelConnectivity checkConnectivity(ChannelAuthContext context) {
        if (properties.isMockMode(ChannelType.PDD)) {
            return new ChannelConnectivity(true, "mock 通道可达（真实网关待联调）", OffsetDateTime.now());
        }
        // 待联调：需真实凭证验证 —— 调用轻量接口探测 https://openapi.pinduoduo.com
        throw new UnsupportedOperationException("PDD 连通性检查待联调");
    }

    // ---------------------------------------------------------------------
    // 商品 / 订单 / 售后拉取
    // ---------------------------------------------------------------------

    @Override
    public List<ChannelProduct> pullProducts(ChannelAuthContext context) {
        if (!properties.isMockMode(ChannelType.PDD)) {
            return doRealPullProducts(context);
        }
        List<ChannelProduct> list = new ArrayList<>(2);
        list.add(new ChannelProduct("pdd-prod-1001", "拼多多测试商品-1", "pdd-sku-1",
                new BigDecimal("9.90"), "on_sale", Map.of()));
        list.add(new ChannelProduct("pdd-prod-1002", "拼多多测试商品-2", "pdd-sku-2",
                new BigDecimal("199.00"), "on_sale", Map.of()));
        return list;
    }

    @Override
    public List<ChannelOrder> pullOrders(ChannelAuthContext context) {
        if (!properties.isMockMode(ChannelType.PDD)) {
            return doRealPullOrders(context);
        }
        List<ChannelOrder> list = new ArrayList<>(2);
        list.add(new ChannelOrder("pdd-order-1001", "pdd-prod-1001", "sku-1",
                1, new BigDecimal("9.90"), "测试买家", "138****0000",
                "浙江省杭州市余杭区测试路1号", Map.of()));
        list.add(new ChannelOrder("pdd-order-1002", "pdd-prod-1002", "sku-2",
                2, new BigDecimal("199.00"), "测试买家", "138****0000",
                "浙江省杭州市余杭区测试路1号", Map.of()));
        return list;
    }

    @Override
    public List<ChannelAfterSale> pullAfterSales(ChannelAuthContext context) {
        if (!properties.isMockMode(ChannelType.PDD)) {
            return doRealPullAfterSales(context);
        }
        return List.of(new ChannelAfterSale("pdd-as-1", "pdd-order-1001", "refunding",
                "买家申请退款", new BigDecimal("9.90"), OffsetDateTime.now(), Map.of()));
    }

    // ---------------------------------------------------------------------
    // 发货
    // ---------------------------------------------------------------------

    @Override
    public ChannelShipResult shipOrder(ChannelShipRequest request) {
        if (properties.isMockMode(ChannelType.PDD)) {
            return new ChannelShipResult(request.platformOrderId(), true, "ok",
                    "pdd-ship-" + request.platformOrderId());
        }
        return doRealShip(request);
    }

    // ---------------------------------------------------------------------
    // 真实模式：网关调用结构（签名为纯函数，可离线单测；HTTP 调用待联调）
    // ---------------------------------------------------------------------

    private ChannelToken doRealHandleCallback(ChannelCallbackRequest request) {
        ChannelPlatformSettings s = properties.settings(ChannelType.PDD);
        Map<String, String> biz = new TreeMap<>();
        biz.put("type", "pdd.goods.authorization.token");
        biz.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        biz.put("data_type", "json");
        biz.put("sign_method", "md5");
        biz.put("code", nullToEmpty(request.code()));
        biz.put("grant_type", "authorization_code");
        String sign = pddSign(biz, nullToEmpty(s.getAppKey()), nullToEmpty(s.getAppSecret()));
        biz.put("sign", sign);
        // 待联调：需真实凭证验证 —— POST s.getEndpoint()（https://openapi.pinduoduo.com）并解析 access_token / refresh_token
        throw new UnsupportedOperationException("PDD 真实 token 换取待联调，endpoint=" + s.getEndpoint());
    }

    private List<ChannelOrder> doRealPullOrders(ChannelAuthContext context) {
        ChannelPlatformSettings s = properties.settings(ChannelType.PDD);
        Map<String, String> biz = pddCommonParams("pdd.order.list.increment.get",
                context.appSecret(), System.currentTimeMillis() / 1000L);
        String sign = pddSign(biz, nullToEmpty(s.getAppKey()), nullToEmpty(s.getAppSecret()));
        biz.put("sign", sign);
        // 待联调：需真实凭证验证 —— 调用 pdd.order.list.increment.get 并映射为 ChannelOrder 列表
        throw new UnsupportedOperationException("PDD 真实订单拉取待联调，endpoint=" + s.getEndpoint());
    }

    private List<ChannelProduct> doRealPullProducts(ChannelAuthContext context) {
        ChannelPlatformSettings s = properties.settings(ChannelType.PDD);
        Map<String, String> biz = pddCommonParams("pdd.goods.list.get",
                context.appSecret(), System.currentTimeMillis() / 1000L);
        String sign = pddSign(biz, nullToEmpty(s.getAppKey()), nullToEmpty(s.getAppSecret()));
        biz.put("sign", sign);
        // 待联调：需真实凭证验证 —— 调用商品列表接口并映射为 ChannelProduct 列表
        throw new UnsupportedOperationException("PDD 真实商品拉取待联调，endpoint=" + s.getEndpoint());
    }

    private ChannelShipResult doRealShip(ChannelShipRequest request) {
        ChannelPlatformSettings s = properties.settings(ChannelType.PDD);
        Map<String, String> biz = pddCommonParams("pdd.logistics.online.send",
                null, System.currentTimeMillis() / 1000L);
        biz.put("order_sn", nullToEmpty(request.platformOrderId()));
        biz.put("logistics_id", nullToEmpty(request.logisticsCompany()));
        biz.put("tracking_number", nullToEmpty(request.logisticsNo()));
        String sign = pddSign(biz, nullToEmpty(s.getAppKey()), nullToEmpty(s.getAppSecret()));
        biz.put("sign", sign);
        // 待联调：需真实凭证验证 —— 调用 pdd.logistics.online.send 发货并回传 platformShipmentId
        throw new UnsupportedOperationException("PDD 真实发货待联调，endpoint=" + s.getEndpoint());
    }

    private List<ChannelAfterSale> doRealPullAfterSales(ChannelAuthContext context) {
        ChannelPlatformSettings s = properties.settings(ChannelType.PDD);
        Map<String, String> biz = pddCommonParams("pdd.refund.information.get",
                context.appSecret(), System.currentTimeMillis() / 1000L);
        String sign = pddSign(biz, nullToEmpty(s.getAppKey()), nullToEmpty(s.getAppSecret()));
        biz.put("sign", sign);
        // 待联调：需真实凭证验证 —— 调用售后单接口并映射为 ChannelAfterSale 列表
        throw new UnsupportedOperationException("PDD 真实售后拉取待联调，endpoint=" + s.getEndpoint());
    }

    // ---------------------------------------------------------------------
    // 纯函数：公共参数 / 待签名串 / MD5 摘要（可离线单测断言）
    // ---------------------------------------------------------------------

    /**
     * 拼多多网关公共参数（不含 client_id 与 sign；client_id 参与签名前缀，由调用方在签名时传入）。
     */
    static Map<String, String> pddCommonParams(String type, String accessToken, long epochSeconds) {
        Map<String, String> p = new TreeMap<>();
        p.put("type", type);
        p.put("timestamp", String.valueOf(epochSeconds));
        p.put("data_type", "json");
        p.put("sign_method", "md5");
        if (accessToken != null && !accessToken.isBlank()) {
            p.put("access_token", accessToken);
        }
        return p;
    }

    /**
     * 拼多多 MD5 签名：待签名串 = client_id + 业务参数按 key 字典序拼接(k+v) + client_secret，取 32 位小写 MD5。
     */
    static String pddSign(Map<String, String> businessParams, String clientId, String clientSecret) {
        TreeMap<String, String> sorted = new TreeMap<>(businessParams);
        StringBuilder sb = new StringBuilder(nullToEmpty(clientId));
        sorted.forEach((k, v) -> sb.append(k).append(v));
        sb.append(nullToEmpty(clientSecret));
        return md5Hex(sb.toString());
    }

    static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(nullToEmpty(input).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 摘要失败", e);
        }
    }

    // ---------------------------------------------------------------------
    // 工具
    // ---------------------------------------------------------------------

    private static String extractState(ChannelAuthContext context) {
        if (context.extra() == null) {
            return "";
        }
        Object state = context.extra().get("state");
        return state == null ? "" : state.toString();
    }

    private static String urlEnc(String value) {
        return URLEncoder.encode(nullToEmpty(value), StandardCharsets.UTF_8);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
