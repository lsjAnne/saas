# API 分期开发清单文档

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v2.0 |
| 最后更新 | 2026-07-17 |
| 文档定位 | 历史 API 分期参考与当前 API 口径入口 |
| 当前状态 | 不再直接作为今日进度真值 |

## 1. 先看这一条

这份文档现在只做两件事：

- 保留 API 按阶段分期的历史参考。
- 告诉你当前应该去哪里看真实 API 状态。

这份文档不再直接回答“现在还缺哪些接口”。

当前真实 API 状态请优先看：

1. [接口规格文档](../../design/interface/01-接口规格文档.md)
2. [接口示例与字段说明](../../design/interface/02-接口示例与字段说明.md)
3. [SDD开发任务清单](../process/06-SDD开发任务清单.md)
4. `backend/src/main/java/backend/**/*Controller.java`

## 2. 当前源码快照结论

基于当前源码扫描，项目后端 API 已不是早期基线阶段。

截至 `2026-07-17`，本地源码可见：

- `32` 个 `Controller`
- `390` 个 `@*Mapping`

这说明：

- 这份文档里早期版本记录的接口数量已经是历史数字。
- 当前项目不能再按 2026-05-31 的 API 台账判断真实覆盖度。

## 3. 当前统一口径

当前 API 层应这样理解：

- 阶段 `0-12` 的核心业务 API 已基本形成。
- 前端需求补完线相关 API 已不再是空白，包括内容素材中心、租户外部集成偏好、经营分析、开放平台执行页等支撑接口。
- 当前真正主线不是继续补一批基础 CRUD API，而是阶段 `13` 的真实接线与治理 API 收口。

## 4. 历史分期参考

下面的分期仍可作为“系统是怎么逐阶段长出来的”参考，但不再直接表示今日完成度。

| 分期 | 历史范围 | 当前理解 |
| --- | --- | --- |
| 第 1 期 | SaaS 与租户基座 | 已完成并进入稳定基线 |
| 第 2 期 | 认证、组织与平台协同 | 已完成并进入稳定基线 |
| 第 3 期 | 店铺、渠道、商品与供应链 | 已完成并进入稳定基线 |
| 第 4 期 | 订单、履约、异常与售后 | 已完成并进入稳定基线 |
| 第 5 期 | 客服、知识库、直播与规则 | 已完成并进入稳定基线 |
| 第 6 期 | 审批、通知、营销、会员、财务与看板 | 已完成并进入稳定基线 |
| 后续增强 | 外部集成、观测、交付与治理深化 | 当前真实主线 |

## 5. 当前值得关注的 API 面

### 5.1 需求补完与前端支撑 API

当前源码中已能看到这类较新的接口面：

- 租户外部集成偏好：
  - `GET /api/tenant/external-integrations/preferences`
  - `PUT /api/tenant/external-integrations/preferences`
- 内容素材与发布闭环：
  - `POST /api/content-assets/generate`
  - `POST /api/content-assets/{id}/publish`
- 路由与外部规划：
  - `POST /api/tms/external-route-plan`
  - `POST /api/tms/external-distance-matrix`

### 5.2 阶段 13 管理与开放平台 API

当前主线相关的重要接口面包括：

- 管理侧 readiness：
  - `GET /api/admin/tenants/{id}/release-readiness`
  - `GET /api/admin/tenants/{id}/delivery-readiness`
  - `GET /api/admin/tenants/{id}/observability-readiness`
- 开放平台 readiness：
  - `GET /api/open/external/system/observability-readiness`
  - 以及对应的外部 baseline / overview / callback-bridge 接口

这些接口说明当前项目的 API 主线已经切到：

- 外部系统 readiness
- 观测 readiness
- 交付 readiness
- 双环境验收相关门禁

## 6. 当前仍未完成的 API 级缺口

当前 API 层的真实缺口，不是“有没有接口骨架”，而是以下几类是否完成真实接线和真实治理：

1. 外部系统真实接线 API。
2. 观测平台真实接线 API。
3. 发布与交付链路真实接线 API。
4. 标准 SaaS / 私有化双环境真实验收 API。

开放平台更细粒度 `scope` 拆分、防串租户校验收口、插件中心治理深化已在 `GOV-01 / GOV-02 / GOV-03` 中收口，不再列为当前缺口。

## 7. 如何继续维护

从现在开始，这份文档只保留：

- 历史分期视角
- 当前 API 口径入口
- 当前 API 主线与缺口摘要

不再继续维护：

- 长篇 endpoint 罗列
- 过期的 API 数量统计
- 旧的“状态：未完成”式口径
- 与 `02 / 05 / 06` 冲突的当前进度判断

如果需要看精确接口，请直接回到：

- [接口规格文档](../../design/interface/01-接口规格文档.md)
- [接口示例与字段说明](../../design/interface/02-接口示例与字段说明.md)
- `backend/src/main/java/backend/**/*Controller.java`
