# SDD 开发任务清单

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v3.0 |
| 最后更新 | 2026-06-10 |
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
- `Superset` 只读骨架已落地：
  - `GET /api/bi/external-platform-overview`
- `OFBiz / OpenBoxes / RabbitMQ` 只读基线已落地：
  - `GET /api/open/external/erp/ofbiz-baseline`
  - `GET /api/open/external/wms/openboxes-baseline`
  - `GET /api/open/external/messaging/rabbitmq-baseline`

### 3.3 观测与交付骨架已落地

- `GET /api/tenant/system/observability-overview`
- `GET /api/admin/tenants/{id}/release-readiness`
- `GET /api/admin/tenants/{id}/delivery-readiness`
- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`

## 4. 当前待办

### 4.1 第一优先级

| 编号 | 事项 | 状态 | 说明 |
| --- | --- | --- | --- |
| T13-01 | 外部 ERP/WMS/BI/消息系统真实接线 | 进行中 | 当前已具备骨架、配置可见性与 delivery-readiness 本地探测，不是全部真实联通 |
| T13-02 | 观测平台真实接线 | 未完成 | 日志、链路、告警、dashboard 还未做真联通 |
| T13-03 | 发布与交付链路真实接线 | 未完成 | 仓库凭据、镜像仓库、release key、灰度发布待完成 |
| T13-04 | 双交付真实验收 | 未完成 | 标准 SaaS 和私有化环境还需真实验证 |

### 4.2 第二优先级

| 编号 | 事项 | 状态 | 说明 |
| --- | --- | --- | --- |
| T13-05 | `OFBiz` 字段口径与接线骨架 | 已完成 | 已落地开放接口只读基线、观测暴露与 release-readiness provider 文案 |
| T13-06 | `OpenBoxes` 字段口径与接线骨架 | 已完成 | 已落地开放接口只读基线、观测暴露与 release-readiness provider 文案 |
| T13-07 | `RabbitMQ` 接线骨架 | 已完成 | 已落地开放接口只读基线、观测暴露与 release-readiness provider 文案 |

## 5. 最近验证

### 5.1 本轮直接通过

```bash
mvn "-Dtest=OpenPlatformControllerTest,TenantSystemControllerTest,AdminTenantControllerTest" test
```

结果：

- `26` 运行
- `26` 通过
- `0` 失败
- `0` 错误

覆盖内容：

- `OFBiz / OpenBoxes / RabbitMQ` 只读基线开放接口
- 租户系统观测视图与 actuator 外部平台摘要
- `release-readiness` 外部集成 provider 文案
- `delivery-readiness` 外部接线 HTTP/TCP 探测摘要

### 5.2 之前已通过且仍有效

- `OrderFulfillmentControllerTest` 中的 `OSRM` 相关定向回归已通过。
- `OpenPlatformControllerTest`
- `AdminTenantControllerTest`
- `TenantSystemControllerTest`

## 6. 当前真实停留点

当前代码停留点已经很明确：

1. 内部业务模块不要再大面积扩写。
2. 当前应继续做阶段 13 的真实接线。
3. `Superset` 只读骨架已经完成，不再把它当“下一步待做”。
4. `OFBiz / OpenBoxes / RabbitMQ` 骨架已完成，下一步不再补骨架，直接转真实 endpoint / 凭据 / 回调联通。

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
