# SDD 开发任务清单

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v3.0 |
| 最后更新 | 2026-06-13 |
| 当前状态 | 主线功能已完成，阶段 13 持续推进中 |
| 使用方式 | 这份文档只用于继续开发，不用于讲完整历史 |

## 1. 使用规则

- 这里只记录“现在怎么继续做”。
- 已完成的大阶段只保留状态，不展开历史流水。
- 当前开发只盯住阶段 13。
- 每次继续开发，优先看“第 4 节 当前待办”和“第 5 节 最近验证”。

## 2. 阶段总览

| 阶段 | 范围 | 当前状态 | 说明 |
| --- | --- | --- | --- |
| 阶段 0-7 | 001-180 | 已完成 | 平台底座、订单、履约、营销、通知、财务、看板主链路已具备 |
| 阶段 8 | 181-188 | 已完成 | 商用认证、安全、合规、开放平台基础已具备 |
| 阶段 9 | 189-206 | 已完成 | ERP 基础闭环已具备 |
| 阶段 10 | 207-230 | 已完成 | CRM / OMS / SRM 主链路已具备 |
| 阶段 11 | 231-244 | 已完成 | WMS / TMS 主链路已具备 |
| 阶段 12 | 245-260 | 已完成 | 治理、BI、交付检查主链路已具备 |
| 阶段 13 | 外部集成 / 观测 / 交付 | 进行中 | 当前唯一主战场 |

## 3. 已完成范围

### 3.1 业务主链路

- 商品、供应商、库存、订单、履约、物流、售后主链路已完成。
- CRM、营销、审批、通知、财务、BI 主链路已完成。
- ERP / WMS / TMS / BI 具备自研最小闭环，不依赖先买第三方系统。

### 3.2 外部增强已落地

- `OSRM` 路由适配已落地：
  - `POST /api/tms/external-route-plan`
  - `POST /api/tms/external-distance-matrix`
- `Superset` 只读骨架与开放平台概览已落地：
  - `GET /api/bi/external-platform-overview`
  - `GET /api/open/external/bi/superset-overview`
- `OFBiz / OpenBoxes / RabbitMQ` 只读基线与回调桥接概览已落地：
  - `GET /api/open/external/erp/ofbiz-baseline`
  - `GET /api/open/external/wms/openboxes-baseline`
  - `GET /api/open/external/messaging/rabbitmq-baseline`
  - `GET /api/open/external/messaging/callback-bridge`

### 3.3 观测与交付骨架已落地

- `GET /api/tenant/system/observability-overview`
- `GET /api/open/external/system/observability-readiness`
- `GET /api/open/external/delivery/readiness`
- `GET /api/admin/tenants/{id}/release-readiness`
- `GET /api/admin/tenants/{id}/delivery-readiness`
- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`

## 4. 当前待办

### 4.1 第一优先级

| 编号 | 事项 | 状态 | 说明 |
| --- | --- | --- | --- |
| T13-01 | 外部 ERP/WMS/BI/消息系统真实接线 | 进行中 | 当前已具备骨架、配置可见性、开放平台外部概览、baseline readiness 字段与 delivery-readiness 本地探测，不是全部真实联通 |
| T13-02 | 观测平台真实接线 | 未完成 | 日志、链路、告警、dashboard 已具备配置可见性、本地 probe 证据，以及 `sourceType/sourceName/defaultValue/trusted/status` 来源诊断与显式可信配置 gate，仍未做真联通 |
| T13-03 | 发布与交付链路真实接线 | 未完成 | `delivery-readiness` 已补齐 pipeline、本地 workflow/compose 资产、双交付验收地址本地 HTTP probe，以及 `releaseKeyControl/registryAuthControl/githubPublishingControl/canaryControl` 来源诊断；`stage13-local` 已能在本机直跑与 compose 默认配置下形成可运行闭环，`github publish mode` 仅依赖默认值、`release key` 或 `registry auth` 来自解析式非可信来源、`registry auth` 缺失与 `canary strategy` 缺失现已直接阻塞 `pipeline.ready`，`dual_delivery_acceptance` 已消费 `probe reachable 2/2` 证据，仍待真实仓库/registry/secrets/灰度发布接线 |
| T13-04 | 双交付真实验收 | 未完成 | 标准 SaaS 和私有化环境已具备本地 probe 与门禁证据，不等于真实环境验收完成 |

### 4.2 第二优先级

| 编号 | 事项 | 状态 | 说明 |
| --- | --- | --- | --- |
| T13-05 | `OFBiz` 字段口径与接线骨架 | 已完成 | 已落地开放接口只读基线、观测暴露与 release-readiness provider 文案 |
| T13-06 | `OpenBoxes` 字段口径与接线骨架 | 已完成 | 已落地开放接口只读基线、观测暴露与 release-readiness provider 文案 |
| T13-07 | `RabbitMQ` 接线骨架 | 已完成 | 已落地开放接口只读基线、观测暴露与 release-readiness provider 文案 |

## 5. 最近验证

### 5.1 本轮直接通过

```bash
mvn "-Dtest=OpenPlatformControllerTest,TenantSystemControllerTest,AdminTenantControllerTest,MemberFinanceDashboardControllerTest" test
```

结果：

- `52` 运行
- `52` 通过
- `0` 失败
- `0` 错误

覆盖内容：

- 管理侧 `delivery-readiness` 中 `pipeline.workflowAsset / standardSaasComposeAsset / privateComposeAsset`
- `release-readiness` 中 `delivery_pipeline_readiness` 对本地 workflow/compose 资产缺失的阻塞
- 管理侧 `delivery-readiness` 中双交付验收证据的 `verificationFresh / verificationStatus / verificationAgeDays`
- `release-readiness` 中 `dual_delivery_acceptance` 对过期验收证据的阻塞
- 开放平台 `observability-readiness` readiness 总览接口
- 开放平台 `delivery/readiness` readiness 总览接口
- 开放平台 `system.observability_readiness.read`
- 开放平台 `delivery.readiness.read`
- 开放平台 `delivery/readiness` 中按 `pipeline / externalIntegrations / acceptance` 视图对齐的 `blockingReasons`
- 开放平台 `delivery/readiness` 中本地发布资产缺失时的 `workflow asset is missing or invalid`、`compose asset is missing or invalid`
- 开放平台 `delivery/readiness` 中 `release key injection must be explicitly configured` 真实阻塞原因
- 开放平台 `delivery/readiness` 中 `github publish mode must be explicitly configured` 真实阻塞原因
- 开放平台 `delivery/readiness` 中 `container registry publish credentials must be explicitly configured` 真实阻塞原因
- 开放平台 `delivery/readiness` 中双交付验收证据过期时的 `verification evidence is stale` 阻塞原因
- 管理侧 `observability-readiness` 中 `logAggregation/trace/alertRouter/dashboard` 的 `sourceType/sourceName/defaultValue/trusted/status`
- 开放平台 `observability-readiness` 中观测 endpoint 的显式可信配置 gate
- 高优先级 placeholder property source 不再被误判成下层显式可信来源
- `Superset` 开放平台外部概览接口
- `RabbitMQ` 回调桥接外部概览接口
- `Superset` 外部概览中的 `credentialConfigured / readinessStatus / missingParts / probeReachable / probeDetail` 接线证据

### 5.2 本地可运行闭环验证

```bash
docker compose -f docker-compose.saas.yml config
docker compose -f docker-compose.private.yml config
mvn -q -DskipTests package
java -jar target/dianShangPingTai-1.0.0-SNAPSHOT.jar --spring.profiles.active=stage13-local --server.port=18090
```

结果：

- `docker compose` 两份配置均渲染通过
- `package` 构建通过
- `/actuator/info` 已返回 `externalDependencies.deliveryPipelineReady=true`
- `/actuator/health` 已返回 `UP`
- `externalDependencies.observabilityStackReady=true`
- `externalDependencies.dualDeliveryAcceptanceReady=true`
- 本地闭环仅用于 stage-13 门禁验证，不代表真实 GitHub、registry、第三方系统或双环境已完成接线
- `RabbitMQ` callback-bridge 概览中的 `credentialConfigured / callbackRequired / callbackUrlConfigured / readinessStatus / missingParts / probeReachable / probeDetail` 接线证据
- `RabbitMQ` callback-bridge 概览中的 `callbackWorkerEnabled / callbackWorkerProvider / callbackWorkerMaskedEndpoint / callbackWorkerConsumerGroup / callbackWorkerReady / callbackWorkerMissingParts`
- `RabbitMQ` callback-bridge 概览中的 `callbackWorkerProbeReachable / callbackWorkerProbeDetail / callback_worker_unreachable`
- `release-readiness` 外部集成检查中的 callback worker readiness / probe 门禁
- `OFBiz / OpenBoxes / RabbitMQ` 只读基线开放接口
- `OFBiz / OpenBoxes / RabbitMQ` baseline 中的 `credentialConfigured / readinessStatus / missingParts` 接线证据
- `OFBiz / OpenBoxes / RabbitMQ` baseline 中的 `probeReachable / probeDetail` 联通证据
- `RabbitMQ` baseline 中的 `callbackRequired / callbackUrlConfigured` 回调接线证据
- `RabbitMQ` baseline 中的 `callbackWorkerEnabled / callbackWorkerProvider / callbackWorkerMaskedEndpoint / callbackWorkerConsumerGroup / callbackWorkerReady / callbackWorkerMissingParts / callbackWorkerProbeReachable / callbackWorkerProbeDetail`
- 租户系统观测视图与 actuator 外部平台摘要
- `externalMessagingPlatform` 摘要中的 `callbackWorkerEnabled / callbackWorkerProvider / callbackWorkerMaskedEndpoint / callbackWorkerConsumerGroup / callbackWorkerReady / callbackWorkerMissingParts / callbackWorkerProbeReachable / callbackWorkerProbeDetail`
- `observabilityStack` 中的 `logAggregation / trace / alertRouter / dashboard` 的 `probeReachable / probeDetail`
- `GET /api/admin/tenants/{id}/observability-readiness` 结构化观测 readiness 视图
- `release-readiness` 外部集成 provider 文案
- `delivery-readiness` 外部接线 HTTP/TCP 探测摘要
- `delivery-readiness` 双交付验收地址的 `protocol / reachable / detail` 本地 probe 摘要
- `release-readiness` 外部集成检查中的 `probe reachable x/y` 证据
- `observability-overview` / actuator 外部接线 `externalIntegrationConnectivity` 探测快照
- `release-readiness` 观测栈检查中的 `probe reachable 4/4` 证据
- `release-readiness` 双交付验收检查中的 `probe reachable 2/2` 证据

### 5.2 之前已通过且仍有效

- `OrderFulfillmentControllerTest` 中的 `OSRM` 相关定向回归已通过。
- `OpenPlatformControllerTest`
- `AdminTenantControllerTest`
- `TenantSystemControllerTest`

## 6. 当前真实停留点

当前代码停留点已经很明确：

1. 内部业务模块不要再大面积扩写。
2. 当前应继续做阶段 13 的真实接线。
3. `Superset` 已不止租户内只读骨架，租户内与开放平台两个概览面都已具备 readiness / probe 证据，不再把“再补一个 BI 只读视图”当下一步。
4. `OFBiz / OpenBoxes / RabbitMQ` 已具备 baseline、callback-bridge 概览、readiness 字段、probe 联通证据、callback worker readiness / worker probe 证据，以及 release-readiness 对 worker 的门禁校验，下一步不再补骨架，直接转真实 endpoint / 凭据 / broker / 回调 worker 联通。
5. `APP_OBSERVABILITY_*` 已补到 `observability-overview` / actuator / `release-readiness` 三个面统一消费本地 probe 证据，不再补“只看配置”的观测摘要。
6. `APP_DELIVERY_*` 已补到管理侧 `delivery-readiness`、开放平台 `GET /api/open/external/delivery/readiness`、`release-readiness` 与租户侧 `observability-overview` 四个面统一消费标准 SaaS / 私有化验收地址的本地 probe 证据、验收证据新鲜度、本地 workflow/compose 发布资产状态，以及 `releaseKeyControl/registryAuthControl/githubPublishingControl/canaryControl` 来源诊断；其中 `github publish mode` 仅依赖默认值、`release key` 或 `registry auth` 来自解析式非可信来源、`registry auth` 缺失与 `canary strategy` 缺失现已进入真实 gate，开放平台侧也会直接返回对应 `blockingReasons`，下一步不再补新的交付摘要，直接转真实发布链路与双环境验收接线。

截至 `2026-06-14`，当前仓库完成记录已从 `APP_EXTERNAL_*` 证据补齐推进到 `APP_OBSERVABILITY_*` 本地 probe 证据补齐，并继续推进到观测栈 endpoint 的来源诊断与显式可信配置 gate，再推进到 `APP_DELIVERY_*` 的双交付验收地址 probe、验收证据新鲜度门禁、本地 workflow/compose 资产门禁、开放平台 readiness 总览与阻塞原因对齐；本轮继续把 `APP_EXTERNAL_*` 的 `credential-configured`、`callback-url` 与 `callback worker enabled/endpoint/consumer group` 收口到统一 trusted gate，`release-readiness`、开放平台 `delivery/readiness`、租户 `observability-overview` 与 actuator `externalDependencies` 已同步消费这些真实阻塞；`stage13-local` 本地闭环现已可运行，当前没有新增半截功能残留，继续开发时直接沿现有停留点进入真实交付链路与双环境验收即可。

- 最新关键回归：`mvn "-Dtest=AdminTenantControllerTest,OpenPlatformControllerTest,TenantSystemControllerTest,MemberFinanceDashboardControllerTest" test`，`55/55` 通过。

## 7. 下次继续开发时怎么做

下次继续开发时，默认按下面顺序直接往下做：

1. 先检查 `APP_EXTERNAL_*` 现状。
2. 再补真实外部系统接线。
3. 然后检查 `APP_OBSERVABILITY_*`。
4. 再补真实观测平台接线。
5. 最后检查 `APP_DELIVERY_*` 并补交付链路。

## 8. 维护规则

从现在开始，这份 `06` 只允许保留：

- 阶段状态
- 当前待办
- 最近验证
- 当前停留点
- 下一步顺序

禁止再继续堆：

- 全量历史任务长列表
- 大段“当前进展”散文
- 过时的旧停留点
- 重复的接口解释
