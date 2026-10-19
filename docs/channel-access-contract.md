# 渠道接入层共享核心 · 适配器契约（Channel Access Contract）

> 本文档是后续 3 个并行适配器任务（拼多多/淘宝、抖音/视频号、快手/京东）的**唯一契约来源**。
> 实现适配器时只依赖本文档 + 已编译通过的 backend.channel 共享核心，**不得修改共享核心任何文件**。
> 冻结版本：channel 接入层第一阶段（V026）。

---

## 1. ChannelType（恰好 6 个值）

```java
public enum ChannelType {
    PDD("pdd"), TAOBAO("taobao"), DOUYIN("douyin"),
    WECHAT_CHANNELS("wechat_channels"), KUAISHOU("kuaishou"), JD("jd");
    String code();                     // "pdd"/"taobao"/"douyin"/"wechat_channels"/"kuaishou"/"jd"
    static ChannelType fromCode(String code);          // 未知抛 BusinessException("8104", ...)
    static Optional<ChannelType> tryFromCode(String code);
}
```

---

## 2. ChannelClient 接口（签名逐字冻结）

包：`backend.channel.application`

```java
public interface ChannelClient {
    ChannelType channelType();
    String buildAuthorizeUrl(ChannelAuthContext context);
    ChannelToken handleCallback(ChannelCallbackRequest request);
    ChannelToken refreshToken(ChannelToken current);
    ChannelConnectivity checkConnectivity(ChannelAuthContext context);
    List<ChannelProduct> pullProducts(ChannelAuthContext context);
    List<ChannelOrder> pullOrders(ChannelAuthContext context);
    ChannelShipResult shipOrder(ChannelShipRequest request);
    List<ChannelAfterSale> pullAfterSales(ChannelAuthContext context);
}
```

## 3. 契约记录（字段逐字，均在 backend.channel.model）

```java
record ChannelAuthContext(String tenantId, String organizationId, String channelAccountId,
                          String storeId, String appKey, String appSecret,
                          String redirectUri, Map<String,Object> extra)

record ChannelToken(String channelTokenId, String tenantId, String organizationId,
                    String channelType /* 平台 code 字符串 */, String accountName,
                    String accessToken, String refreshToken, String tokenType, String scope,
                    OffsetDateTime expiresAt, OffsetDateTime refreshExpiresAt,
                    Map<String,Object> extraConfig,
                    OffsetDateTime createdAt, OffsetDateTime updatedAt)

record ChannelCallbackRequest(ChannelType channelType, String code, String state, Map<String,String> rawParams)

record ChannelConnectivity(boolean reachable, String message, OffsetDateTime checkedAt)

record ChannelProduct(String platformProductId, String title, String skuId,
                      BigDecimal price, String status, Map<String,Object> raw)

record ChannelOrder(String platformOrderId, String productId, String skuId,
                    Integer quantity, BigDecimal unitPrice,
                    String buyerName, String buyerPhoneMask, String shippingAddress,
                    Map<String,Object> raw)

record ChannelShipRequest(String tenantId, String organizationId, String channelAccountId,
                          String storeId, String platformOrderId,
                          String logisticsCompany, String logisticsNo, Map<String,Object> extra)

record ChannelShipResult(String platformOrderId, boolean success,
                         String message, String platformShipmentId)

record ChannelAfterSale(String afterSaleId, String platformOrderId, String status,
                       String reason, BigDecimal refundAmount,
                       OffsetDateTime createdAt, Map<String,Object> raw)

record ChannelAuthState(String authStateId, String tenantId, String organizationId,
                       String channelType, String channelAccountId, String state,
                       String redirectUri, OffsetDateTime createdAt, OffsetDateTime expiresAt)
```

---

## 4. ChannelPlatformProperties 用法

包：`backend.channel.application`，绑定 `app.channel.platforms.<code>`。

适配器通过构造器注入 `ChannelPlatformProperties`，按自己的 ChannelType 取配置：

```java
ChannelPlatformSettings s = properties.settings(ChannelType.PDD);
s.isMockMode();   // 默认 true；application.yml 中对应平台配置 mock-mode: false 时走真实网关
s.getEndpoint();  // 默认 internal://mock/<code>，真实模式取配置的开放平台网关
s.getAppKey();    // 应用 app_key/client_key/app_id
s.getAppSecret(); // 应用 secret
s.getCallbackPath(); // /api/channels/callback/<code>
```

**所有网关地址、appKey、appSecret、是否 mock，都必须从 ChannelPlatformProperties 读取，禁止硬编码在适配器里**（URL 拼接/签名参数构造可硬编码，因为它们是平台公共参数约定）。

---

## 5. 适配器类命名与包（后续任务创建）

包：`backend.channel.infrastructure.adapter`，每个一个 `@Component`，构造器注入 `ChannelPlatformProperties`：

| 平台 | 类名 | ChannelType |
|------|------|-------------|
| 拼多多 | `PddChannelClient` | PDD |
| 淘宝 | `TaobaoChannelClient` | TAOBAO |
| 抖音 | `DouyinChannelClient` | DOUYIN |
| 视频号小店 | `WechatChannelsChannelClient` | WECHAT_CHANNELS |
| 快手小店 | `KuaishouChannelClient` | KUAISHOU |
| 京东宙斯 | `JdChannelClient` | JD |

只要实现 `ChannelClient` 并加 `@Component`，`ChannelClientRegistry` 会自动收集，无需手工注册。

---

## 6. Mock 行为契约（mock-mode=true 时逐方法断言依据）

所有平台在 mock 模式下必须满足以下行为（不发任何真实网络请求）：

- `channelType()`：返回对应枚举。
- `buildAuthorizeUrl(context)`：按平台真实授权 URL 形态拼接（含各自的 app key/client_id/app_id 参数名差异），把 `context.appKey()` 填进对应参数、`context.redirectUri()` 填进回调参数、`context.state()` 填进 state。
- `handleCallback(request)` 返回：
  - `accessToken = "mock-access-" + 平台code`
  - `refreshToken = "mock-refresh-" + 平台code`
  - `tokenType = "Bearer"`
  - `expiresAt = now + 7天`
  - `refreshExpiresAt = now + 30天`
  - `accountName = "<平台中文名>测试店铺"`（拼多多测试店铺 / 淘宝测试店铺 / 抖音测试店铺 / 视频号测试店铺 / 快手测试店铺 / 京东测试店铺）
- `refreshToken(current)`：保留 accountName、token、accountName，把 `expiresAt` 延到 `now + 7天`，`refreshExpiresAt` 顺延或保留。
- `checkConnectivity(context)`：`reachable=true`，`message` 含"mock 通道可达（真实网关待联调）"。
- `pullOrders(context)`：返回 1~2 条样例：
  - 订单号前缀：`pdd-order-1001` / `tb-order-1001` / `dy-order-1001` / `wxch-order-1001` / `ks-order-1001` / `jd-order-1001`
  - `quantity` 1~2，`unitPrice` 9.9~199，`buyerName="测试买家"`，`buyerPhoneMask="138****0000"`，`shippingAddress="浙江省杭州市余杭区测试路1号"`
- `pullProducts(context)`：返回 1~2 条样例，`platformProductId` 前缀 `pdd-prod-1001` / `tb-prod-1001` 等，`status="on_sale"`。
- `shipOrder(request)`：`success=true`，`platformShipmentId = "<平台code>-ship-" + request.platformOrderId()`，`message="ok"`。
- `pullAfterSales(context)`：返回 1 条样例，`afterSaleId` 前缀 `<平台code>-as-1`，`status="refunding"`。

---

## 7. 真实模式要求（mock-mode=false 时）

- 按各平台官方开放平台文档实现网关调用、签名/鉴权。
- **真实凭证验证/网络调用部分必须在代码注释标注 `// 待联调：...`**，离线单测不得发起真实请求。
- URL 拼接、签名参数构造（公共参数、待签名串拼装、MD5/HMAC 摘要）必须写成纯函数，可离线单测断言输出。
- HTTP 客户端调用通过受保护方法隔离，便于 mock；单测只覆盖纯函数部分。

### 各平台真实接入要点

#### 拼多多 PDD
- 授权：`https://fuwu.pinduoduo.com/oauth/authorize?app_id=...&redirect_uri=...&state=...`
- 网关：`https://openapi.pinduoduo.com`
- 公共参数：`client_id` / `timestamp` / `sign` / `sign_method=md5` / `data_type=json` / `type=<接口名>` / `access_token`
- 签名：MD5

#### 淘宝 TAOBAO（TOP）
- 授权：`https://oauth.taobao.com/authorize?response_type=code&client_id=...&redirect_uri=...&state=...`
- 网关：`https://eco.taobao.com/router/rest`
- 公共参数：`method` / `app_key` / `timestamp` / `sign` / `session` / `format=json` / `v=2.0`
- 签名：HMAC-MD5
- 常用 API：订单 `taobao.trades.sold.get`、发货 `taobao.logistics.offline.send`

#### 抖音 DOUYIN
- 授权：`https://open.douyin.com/platform/oauth/connect/?client_key=...&response_type=code&scope=...&redirect_uri=...&state=...`
- 网关：`https://open.douyin.com`
- 换 token：`/oauth/access_token/`
- 鉴权：请求头 `access-token`

#### 视频号小店 WECHAT_CHANNELS
- 授权：`https://channels.weixin.qq.com/shop/open?appid=...&redirect_uri=...&state=...`
- 网关：`https://api.weixin.qq.com/channels/ec/...`
- access_token：通过 `gettoken` 获取并缓存

#### 快手小店 KUAISHOU
- 授权：`https://open.kwaixiaodian.com/oauth/authorize?app_id=...&redirect_uri=...&state=...`
- 网关：`https://open.kwaixiaodian.com`
- 公共参数：`appKey` / `signMethod` / `timestamp` / `sign` / ...
- 签名：MD5

#### 京东 JD（宙斯）
- 授权：`https://openapi.jd.com/oauth/authorize?app_key=...&response_type=code&redirect_uri=...&state=...`
- 网关：`https://api.jd.com/routerjson`
- 公共参数：`app_key` / `timestamp` / `sign` / `method` / `360buy_param_json` / `access_token`
- 签名：MD5

---

## 8. 适配器单测要求（验收标准）

每个平台适配器必须有 JUnit 单测（不启动 Spring 上下文）：

1. **mock-mode=true**：逐方法断言第 6 节的契约输出（授权 URL 形态、handleCallback 返回的 token 字段、pullOrders 样例数量与字段、shipOrder 成功结构）。
2. **mock-mode=false**：断言 `buildAuthorizeUrl` 走真实授权 URL 形态（含正确的 app_id/client_key/app_id 参数名）；纯函数签名构造方法输出与手工计算一致。
3. **registry 解析**：构造 `ChannelClientRegistry(List.of(适配器实例))`，断言 `resolve(对应ChannelType)` 返回该实例、`supports` 正确、未知类型抛 8101。
4. 不得有真实网络调用；不得依赖 H2/Spring。

---

## 9. 范围边界（适配器任务禁止触碰）

- 禁止修改/新增 `backend.channel.application`、`backend.channel.model`、`backend.channel.domain.repository`、`backend.channel.infrastructure.persistence` 下共享核心已有文件。
- 禁止修改 V001~V026 任何迁移脚本。
- 禁止修改 application.yml 的 `app.channel.platforms` 既有键（如需新增环境变量，用 `${APP_CHANNEL_<平台>_<字段>:默认值}` 风格追加）。
- 禁止实现 ERP、物流电子面单/轨迹回传、新手向导、前端页面。
- 新增文件仅限：`backend.channel.infrastructure.adapter.<平台>ChannelClient.java` 及其对应单测 `src/test/java/backend/channel/infrastructure/adapter/`。
- 禁止 git commit。
