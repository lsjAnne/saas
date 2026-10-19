# 真实直播平台弹幕接入指引

> 模块位置：`backend/src/main/java/backend/live/barrage/`（连接器端口与适配器）
> 配置：`application.yml` → `app.live.barrage.*`
> 前端：数字人无人直播页「弹幕源接入」面板（`BarrageSourcePanel.vue`）

## 一、架构与流转闭环

```
直播平台（抖音/快手/视频号）
        │  WebSocket / 消息推送（真实协议，待凭证联调）
        ▼
BarrageConnector（平台适配器，端口在 backend.live.barrage.application）
        │  BarrageSink.onBarrage(LiveBarrageMessage)
        ▼
BarrageConnectorManager（按组织启停 + 弹幕流转）
        │  进程内调用 DigitalHumanInteractionService.submitBarrage(...)
        ▼
数字人问答服务（混合路由：①向量语义 ②商品+关键词 ③兜底+学习记忆）
        ▼
互动记录落库 → 前端直播间聊天区实时上屏
```

- **mock 模式（默认）**：`MockBarrageEmitter` 每 1~2 秒随机产生一条贴近真实直播间的观众弹幕（覆盖多商品/多问题类型/闲聊），驱动完整闭环，无真实凭证即可演示。
- **真实模式**：平台适配器按各平台开放能力接入，需真实 app-key/app-secret/room-id 联调（实现处 `TODO(联调)` 标注）。

## 二、连接器清单

| 平台 | 类 | 真实接入要点 |
| --- | --- | --- |
| 抖音 | `DouyinBarrageConnector` | 开放平台「直播-互动开放能力」主播授权 → access_token → WebSocket 评论订阅（心跳+重连） |
| 快手 | `KuaishouBarrageConnector` | 开放平台直播弹幕开放能力，主播授权后订阅评论推送（WebSocket/回调） |
| 视频号 | `WechatChannelsBarrageConnector` | 视频号直播开放能力，商家主体授权后订阅场次评论事件 |
| 模拟 | `MockBarrageConnector` | 共享模拟弹幕源，演示/联调用 |

## 三、配置

```yaml
app:
  live:
    barrage:
      enabled: true
      connectors:
        douyin:
          enabled: true
          mock-mode: ${APP_LIVE_BARRAGE_DOUYIN_MOCK_MODE:true}   # 默认模拟模式
          app-key: ${APP_LIVE_BARRAGE_DOUYIN_APP_KEY:}           # 真实模式必填
          app-secret: ${APP_LIVE_BARRAGE_DOUYIN_APP_SECRET:}
          room-id: ${APP_LIVE_BARRAGE_DOUYIN_ROOM_ID:}
          ws-endpoint: ${APP_LIVE_BARRAGE_DOUYIN_WS_ENDPOINT:https://open.douyin.com}
        kuaishou:   # 同上字段
        wechat_channels:  # 同上字段
```

## 四、接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/live/barrage-connections` | 全部平台弹幕源状态（running/mode/连接时间） |
| GET | `/api/live/barrage-connections/active` | 当前生效弹幕源 |
| POST | `/api/live/barrage-connections/{platformType}/start` | 启动该平台拉流（幂等） |
| POST | `/api/live/barrage-connections/{platformType}/stop` | 停止该平台拉流 |

全部端点要求 `digital.human.manage` 权限，弹幕入问答闭环复用 `/api/digital-humans/barrages` 现有数据面。

## 五、验收方式

- 后端单测：`BarrageConnectionControllerTest`（启动→模拟弹幕→问答落库→停止，2 用例）。
- 浏览器实测：数字人页「弹幕源接入」→ 启动任一平台 → 直播间互动面板弹幕/回答持续刷新，互动概览计数增长。
- 当前实现说明：连接器为单实例 Bean、按组织启停（后启动覆盖前启动）；多租户并行接入需将连接器改为按组织工厂化实例化。

## 六、联调清单（真实模式）

1. 在各平台开放平台创建应用，获取 app-key/app-secret，配置直播间 room-id；
2. 将对应平台 `mock-mode` 置为 `false` 并配置环境变量（`APP_LIVE_BARRAGE_*`）；
3. 按平台开放能力文档补齐 `AbstractPlatformBarrageConnector.start()` 中的握手/心跳/解析逻辑（注释 `TODO(联调)` 处）；
4. 单条弹幕处理失败默认不中断拉流；需要错误上报可扩展 `BarrageSink` 回调的异常通道。

## 七、抖音真实弹幕（内网专线回调模式，2026-10-19 已落地）

> 抖音「弹幕小玩法」官方推荐的接入方式：开放平台把评论/礼物/点赞/粉丝团数据**推送到开发者后端**，后端经抖音云 WebSocket 下发给直播客户端。与上文 WebSocket 拉流模型并存，本模式是抖音真实上线的首选路径（免域名备案、内网专线、免鉴权调用 OpenAPI）。

### 1. 端点到服务

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/live_room/callback` | 无业务 JWT（公开） | 抖音云内网专线回调入口：校验 `x-tt-source` 含 `internal-callback` 免验签；非内网来源走 MD5 签名验签 |
| POST | `/start_game` | 无业务 JWT（公开） | 玩法启动握手，返回 `{roomId, ok}`，用于客户端联调定位直播间 |

### 2. 代码位置

- `backend/live/barrage/infrastructure/DouyinCallbackSignatureVerifier`：官方签名算法（header 参与签名键 `x-nonce-str/x-timestamp/x-roomid/x-msg-type` 字典序拼接 → +body → +secret → MD5(16 bytes) → Base64）
- `backend/live/barrage/application/DouyinLiveCallbackService`：来源校验 → payload 数组解析 → 按 `x-msg-type` 分发（live_comment 进 `DigitalHumanInteractionService.submitBarrage` 问答闭环并回推答案；live_gift/live_like/fansclub 仅计数）→ `msg_id` 幂等去重（容量 5000 自动清空）
- `backend/live/barrage/controller/DouyinLiveCallbackController`：`/live_room/callback`、`/start_game`
- `backend/live/barrage/infrastructure/DouyinCloudPushGateway`：HTTP 调抖音云 WebSocket 网关推送指令（`gateway-endpoint` 未配置时 log+跳过，不阻塞 ack）

### 3. 配置项（application.yml → `app.live.barrage.connectors.douyin`）

```yaml
douyin:
  callback-path: ${APP_LIVE_BARRAGE_DOUYIN_CALLBACK_PATH:/live_room/callback}
  callback-secret: ${APP_LIVE_BARRAGE_DOUYIN_CALLBACK_SECRET:}   # 非内网来源验签用
  gateway-endpoint: ${APP_LIVE_BARRAGE_DOUYIN_GATEWAY_ENDPOINT:} # 抖音云 WebSocket 网关地址
  tenant-id: ${APP_LIVE_BARRAGE_DOUYIN_TENANT_ID:}               # 回调不带业务租户标识，落库归属
  organization-id: ${APP_LIVE_BARRAGE_DOUYIN_ORGANIZATION_ID:}   # 同上；格式须带 tenant-/org- 前缀
```

### 4. 运行中后端实测（2026-10-19，真实 HTTP 回调）

| 推送 | 响应关键值 | 结果 |
| --- | --- | --- |
| live_comment（有内容） | `commentSubmitted=1` | 弹幕落库 + 问答闭环 + fallback 回答生成 |
| live_gift | `giftCount=1` | 仅计数 |
| live_like | `likeCount=1` | 仅计数 |
| fansclub | `fansCount=1` | 仅计数 |
| 重复 msg_id | `skippedDuplicate=1` | 幂等去重生效 |
| 空白内容评论 | `skippedBlank=1` | 过滤 |
| 前端表现 | 直播间互动面板「收到弹幕 3」、成长与记忆面板出现「正在记忆 2」条真实问题 | 全链路可见 |

### 5. 上线前提（详见 `docs/抖音云部署方案-后端服务上云.docx`）

1. 抖音开放平台「弹幕小玩法」应用已建（AppID `tt5a492cf4c4304bda10`，AppSecret 已生成仅显示一次，建议只走环境变量注入）；
2. 开通抖音云（火山引擎实名同步）→ 部署容器服务 `danmu-service` → 配置抖音回调 `/live_room/callback`；
3. 开放平台「直播间数据能力开发配置」：协议选抖音云，评论/礼物/点赞/粉丝团四类数据先配置 dev 再 prod（仅支持一个推送目标），用自查工具推测试数据验证；
4. 平台硬规则：内网专线免验签靠 `x-tt-source=internal-callback`；推送 ack=2XX 且 2 秒内（礼物 3s）；连续 10 次失败熔断；消息可重复需按 msg_id 去重；点赞上游 2s 合并；回查仅礼物/粉丝团（前 10w 条缓存一天）。

