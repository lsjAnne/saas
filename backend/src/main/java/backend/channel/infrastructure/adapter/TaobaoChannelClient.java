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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 淘宝开放平台（TAOBAO / TOP）渠道适配器。
 * <p>
 * mock-mode=true：返回契约文档第 6 节规定的离线样例，不发起任何网络请求。
 * mock-mode=false：按契约文档第 7 节组装公共参数与 HMAC-MD5 签名；真实 HTTP 调用隔离在私有方法中并标注待联调。
 */
@Component
public class TaobaoChannelClient implements ChannelClient {

    static final String AUTHORIZE_BASE = "https://oauth.taobao.com/authorize";
    private static final String ACCOUNT_NAME = "淘宝测试店铺";

    private final ChannelPlatformProperties properties;

    public TaobaoChannelClient(ChannelPlatformProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.TAOBAO;
    }

    // ---------------------------------------------------------------------
    // OAuth 授权 URL：mock/真实模式形态一致（response_type=code&client_id=...）
    // ---------------------------------------------------------------------

    @Override
    public String buildAuthorizeUrl(ChannelAuthContext context) {
        StringBuilder sb = new StringBuilder(AUTHORIZE_BASE)
                .append("?response_type=code")
                .append("&client_id=").append(urlEnc(context.appKey()))
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
        if (properties.isMockMode(ChannelType.TAOBAO)) {
            OffsetDateTime now = OffsetDateTime.now();
            return new ChannelToken(
                    null, null, null, ChannelType.TAOBAO.code(), ACCOUNT_NAME,
                    "mock-access-taobao", "mock-refresh-taobao", "Bearer", null,
                    now.plusDays(7), now.plusDays(30), Map.of(), now, now
            );
        }
        return doRealHandleCallback(request);
    }

    @Override
    public ChannelToken refreshToken(ChannelToken current) {
        if (properties.isMockMode(ChannelType.TAOBAO)) {
            OffsetDateTime now = OffsetDateTime.now();
            return new ChannelToken(
                    current.channelTokenId(), current.tenantId(), current.organizationId(),
                    current.channelType(), current.accountName(),
                    current.accessToken(), current.refreshToken(), current.tokenType(), current.scope(),
                    now.plusDays(7), now.plusDays(30), current.extraConfig(),
                    current.createdAt(), now
            );
        }
        // 待联调：需真实凭证验证 —— 用 session_key/refresh_token 向淘宝网关换取新会话
        throw new UnsupportedOperationException("TAOBAO refresh_token 待联调");
    }

    // ---------------------------------------------------------------------
    // 连通性
    // ---------------------------------------------------------------------

    @Override
    public ChannelConnectivity checkConnectivity(ChannelAuthContext context) {
        if (properties.isMockMode(ChannelType.TAOBAO)) {
            return new ChannelConnectivity(true, "mock 通道可达（真实网关待联调）", OffsetDateTime.now());
        }
        // 待联调：需真实凭证验证 —— 调用轻量接口探测 https://eco.taobao.com/router/rest
        throw new UnsupportedOperationException("TAOBAO 连通性检查待联调");
    }

    // ---------------------------------------------------------------------
    // 商品 / 订单 / 售后拉取
    // ---------------------------------------------------------------------

    @Override
    public List<ChannelProduct> pullProducts(ChannelAuthContext context) {
        if (!properties.isMockMode(ChannelType.TAOBAO)) {
            return doRealPullProducts(context);
        }
        List<ChannelProduct> list = new ArrayList<>(2);
        list.add(new ChannelProduct("tb-prod-1001", "淘宝测试商品-1", "tb-sku-1",
                new BigDecimal("9.90"), "on_sale", Map.of()));
        list.add(new ChannelProduct("tb-prod-1002", "淘宝测试商品-2", "tb-sku-2",
                new BigDecimal("199.00"), "on_sale", Map.of()));
        return list;
    }

    @Override
    public List<ChannelOrder> pullOrders(ChannelAuthContext context) {
        if (!properties.isMockMode(ChannelType.TAOBAO)) {
            return doRealPullOrders(context);
        }
        List<ChannelOrder> list = new ArrayList<>(2);
        list.add(new ChannelOrder("tb-order-1001", "tb-prod-1001", "tb-sku-1",
                1, new BigDecimal("9.90"), "测试买家", "138****0000",
                "浙江省杭州市余杭区测试路1号", Map.of()));
        list.add(new ChannelOrder("tb-order-1002", "tb-prod-1002", "tb-sku-2",
                2, new BigDecimal("199.00"), "测试买家", "138****0000",
                "浙江省杭州市余杭区测试路1号", Map.of()));
        return list;
    }

    @Override
    public List<ChannelAfterSale> pullAfterSales(ChannelAuthContext context) {
        if (!properties.isMockMode(ChannelType.TAOBAO)) {
            return doRealPullAfterSales(context);
        }
        return List.of(new ChannelAfterSale("tb-as-1", "tb-order-1001", "refunding",
                "买家申请退款", new BigDecimal("9.90"), OffsetDateTime.now(), Map.of()));
    }

    // ---------------------------------------------------------------------
    // 发货
    // ---------------------------------------------------------------------

    @Override
    public ChannelShipResult shipOrder(ChannelShipRequest request) {
        if (properties.isMockMode(ChannelType.TAOBAO)) {
            return new ChannelShipResult(request.platformOrderId(), true, "ok",
                    "taobao-ship-" + request.platformOrderId());
        }
        return doRealShip(request);
    }

    // ---------------------------------------------------------------------
    // 真实模式：网关调用结构（签名为纯函数，可离线单测；HTTP 调用待联调）
    // ---------------------------------------------------------------------

    private ChannelToken doRealHandleCallback(ChannelCallbackRequest request) {
        ChannelPlatformSettings s = properties.settings(ChannelType.TAOBAO);
        Map<String, String> biz = taobaoCommonParams("taobao.top.auth.token.create",
                null, "2024-01-01 00:00:00");
        biz.put("app_key", nullToEmpty(s.getAppKey()));
        biz.put("code", nullToEmpty(request.code()));
        String sign = taobaoSign(biz, nullToEmpty(s.getAppSecret()));
        biz.put("sign", sign);
        // 待联调：需真实凭证验证 —— POST s.getEndpoint()（https://eco.taobao.com/router/rest）并解析 session
        throw new UnsupportedOperationException("TAOBAO 真实 token 换取待联调，endpoint=" + s.getEndpoint());
    }

    private List<ChannelOrder> doRealPullOrders(ChannelAuthContext context) {
        ChannelPlatformSettings s = properties.settings(ChannelType.TAOBAO);
        Map<String, String> biz = taobaoCommonParams("taobao.trades.sold.get",
                context.appSecret(), "2024-01-01 00:00:00");
        biz.put("app_key", nullToEmpty(s.getAppKey()));
        String sign = taobaoSign(biz, nullToEmpty(s.getAppSecret()));
        biz.put("sign", sign);
        // 待联调：需真实凭证验证 —— 调用 taobao.trades.sold.get 并映射为 ChannelOrder 列表
        throw new UnsupportedOperationException("TAOBAO 真实订单拉取待联调，endpoint=" + s.getEndpoint());
    }

    private List<ChannelProduct> doRealPullProducts(ChannelAuthContext context) {
        ChannelPlatformSettings s = properties.settings(ChannelType.TAOBAO);
        Map<String, String> biz = taobaoCommonParams("taobao.items.onsale.get",
                context.appSecret(), "2024-01-01 00:00:00");
        biz.put("app_key", nullToEmpty(s.getAppKey()));
        String sign = taobaoSign(biz, nullToEmpty(s.getAppSecret()));
        biz.put("sign", sign);
        // 待联调：需真实凭证验证 —— 调用 taobao.items.onsale.get 并映射为 ChannelProduct 列表
        throw new UnsupportedOperationException("TAOBAO 真实商品拉取待联调，endpoint=" + s.getEndpoint());
    }

    private ChannelShipResult doRealShip(ChannelShipRequest request) {
        ChannelPlatformSettings s = properties.settings(ChannelType.TAOBAO);
        Map<String, String> biz = taobaoCommonParams("taobao.logistics.offline.send",
                null, "2024-01-01 00:00:00");
        biz.put("app_key", nullToEmpty(s.getAppKey()));
        biz.put("tid", nullToEmpty(request.platformOrderId()));
        biz.put("company_code", nullToEmpty(request.logisticsCompany()));
        biz.put("out_sid", nullToEmpty(request.logisticsNo()));
        String sign = taobaoSign(biz, nullToEmpty(s.getAppSecret()));
        biz.put("sign", sign);
        // 待联调：需真实凭证验证 —— 调用 taobao.logistics.offline.send 发货并回传 platformShipmentId
        throw new UnsupportedOperationException("TAOBAO 真实发货待联调，endpoint=" + s.getEndpoint());
    }

    private List<ChannelAfterSale> doRealPullAfterSales(ChannelAuthContext context) {
        ChannelPlatformSettings s = properties.settings(ChannelType.TAOBAO);
        Map<String, String> biz = taobaoCommonParams("taobao.aftersale.get",
                context.appSecret(), "2024-01-01 00:00:00");
        biz.put("app_key", nullToEmpty(s.getAppKey()));
        String sign = taobaoSign(biz, nullToEmpty(s.getAppSecret()));
        biz.put("sign", sign);
        // 待联调：需真实凭证验证 —— 调用售后单接口并映射为 ChannelAfterSale 列表
        throw new UnsupportedOperationException("TAOBAO 真实售后拉取待联调，endpoint=" + s.getEndpoint());
    }

    // ---------------------------------------------------------------------
    // 纯函数：公共参数 / 待签名串 / HMAC-MD5 摘要（可离线单测断言）
    // ---------------------------------------------------------------------

    /**
     * 淘宝 TOP 公共参数（不含 app_key 与 sign；timestamp 由调用方传入以保证纯函数可断言）。
     */
    static Map<String, String> taobaoCommonParams(String method, String session, String timestamp) {
        Map<String, String> p = new TreeMap<>();
        p.put("method", method);
        p.put("timestamp", timestamp);
        p.put("format", "json");
        p.put("v", "2.0");
        if (session != null && !session.isBlank()) {
            p.put("session", session);
        }
        return p;
    }

    /**
     * 淘宝 HMAC-MD5 签名：所有参数按 key 字典序拼接(k+v)，用 app_secret 作为 HMAC key 取摘要，32 位小写。
     */
    static String taobaoSign(Map<String, String> params, String secret) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        sorted.forEach((k, v) -> sb.append(k).append(v));
        return hmacMd5Hex(sb.toString(), nullToEmpty(secret));
    }

    static String hmacMd5Hex(String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(new SecretKeySpec(nullToEmpty(secret).getBytes(StandardCharsets.UTF_8), "HmacMD5"));
            byte[] digest = mac.doFinal(nullToEmpty(message).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-MD5 摘要失败", e);
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
