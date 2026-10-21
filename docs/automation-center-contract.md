# 运营自动化中心 M1 · 契约文档

> 项目：E:\ideaCode\dianShangPingTai（分支 saas）
> 版本：M1（自动化主链闭环）
> 制定日期：2026-10-21
> 说明：本文所有接口路径、字段名、枚举值、配置项均以当前代码为准（`backend/automation/**`、`V030__automation_engine.sql`、`application.yml`、`PddChannelClient` 等）。

---

## 1. 概述

M1 把渠道、订单、履约、财务四个已有模块串成一条**可演示、可追踪、可审计**的自动化闭环：

```
渠道拉单(手动触发 / 定时调度) → 订单落库(syncOrder) → 自动风控(auditOrder)
  → 规则引擎按 ruleExpression 白名单匹配 → 命中动作按 priority 升序依次执行
      ├─ AUTO_SHIP      → 渠道回传发货 + 履约任务置 shipped
      ├─ GENERATE_BILL → 自动生成销售财务单据(sales, 按订单日期)
      ├─ AUTO_RECONCILE → 对最新 draft 单据执行对账
      ├─ NOTIFY         → 占位通知（result = "notified:{orderId}"）
      └─ PAUSE_REVIEW   → 挂起进人工确认队列，后续动作暂停；
                          人工 APPROVED → 续跑剩余动作 / REJECTED → 终止
全链路写入 automation_execution（幂等去重）与 automation_approval（人工队列）
```

- **演示载体**：抖店（douyin）/ 拼多多（pdd）等 6 平台 **mock 模式**，无需真实凭证即可跑通全流程。
- **权限基线**：控制器类级注解 `@RequireTenantPermission(AuthPermissionCodes.AUTOMATION_MANAGE)`，权限码常量值为 `automation.manage`；所有接口同时强制租户上下文（`TenantAccessSupport.requiredTenantId()`），跨租户访问一律按「对象不存在」处理。

---

## 2. 接口清单

统一约定：

- 基础路径前缀：`/api/automation/**`（规则本体 CRUD 复用既有 `/api/rules`，不在本文范围）。
- 统一响应包装：`ApiResponse<T>` = `{ "code": ..., "message": ..., "data": <T>, "traceId": ... }`。
- 分页结构 `AutomationPage<T>` = `{ "list": T[], "page": int, "size": int, "total": long }`；`page` 默认 1，`size` 默认 20、上限 200。
- 鉴权：HTTP Header `Authorization: Bearer <token>`；全部端点要求权限 `automation.manage` + 当前登录用户属于目标数据所属租户。
- 典型错误（均为 `BusinessException`）：

| code | HTTP | 触发场景 |
|---|---|---|
| 1003 | 404 | 对象不存在 / 店铺或动作不属于当前租户（按"不存在"脱敏） |
| 1004 | 400 | 动作绑定的规则不属于该店铺 |
| 1005 | 403 | 租户访问被拒（渠道同步编排层） |
| 8103 | 400 | 渠道未授权（未完成授权回调换 token） |
| 8601 | 400 | `actionType` 不在白名单五值内 |
| 8602 | 400 | 审批任务已被处理（非 PENDING 状态重复 approve/reject） |
| 8603 | 400 | 店铺找不到已授权的渠道账户 |

### 2.1 动作绑定管理（automation_action）

#### GET /api/automation/actions — 动作绑定分页查询

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| storeId | query | String | 否（但 listActions 内部会校验店铺归属） | 按店铺过滤 |
| ruleId | query | String | 否 | 按规则过滤 |
| enabled | query | Boolean | 否 | 按启用状态过滤 |
| page / size | query | Integer | 否 | 分页，默认 1 / 20 |

响应 `AutomationPage<AutomationActionView>`，`AutomationActionView` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| actionId | String | 动作绑定 ID（UUID） |
| ruleId | String | 关联规则 ID |
| ruleName | String? | 回填的规则名（规则已删时为 null） |
| ruleType | String? | 回填的规则类型，如 `order_risk` |
| ruleExpression | String? | 回填的规则表达式，如 `amount < 100` |
| actionType | String | 动作类型（五值之一） |
| actionConfig | String? | 动作配置（TEXT，JSON 字符串，M1 未强制使用） |
| priority | int | 执行优先级，升序执行 |
| enabled | boolean | 是否启用 |
| createdAt | OffsetDateTime | 创建时间 |

#### POST /api/automation/actions — 新建动作绑定

请求体 `SaveAutomationActionRequest`：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| storeId | String | 是 | 目标店铺（须属于当前租户） |
| ruleId | String | 是 | 关联规则（须属于该店铺，否则 1004） |
| actionType | String | 是 | 白名单五值，否则 8601 |
| actionConfig | String | 否 | 配置 JSON 字符串 |
| priority | Integer | 否 | 默认 0 |
| enabled | Boolean | 否 | 默认 true |

响应：`AutomationActionView`（结构同上）。

请求示例：

```json
{
  "storeId": "store-xxx",
  "ruleId": "rule-xxx",
  "actionType": "AUTO_SHIP",
  "priority": 10,
  "enabled": true
}
```

#### PUT /api/automation/actions/{actionId} — 更新动作绑定

路径参数 `actionId`；请求体 `UpdateAutomationActionRequest`（全部可选，null 表示不修改）：

| 字段 | 类型 | 说明 |
|---|---|---|
| actionType | String? | 若非空须为白名单五值，否则 8601 |
| actionConfig | String? | 非空则整体覆盖 |
| priority | Integer? | 非空则覆盖 |
| enabled | Boolean? | 非空则覆盖 |

响应：`AutomationActionView`。

#### DELETE /api/automation/actions/{actionId} — 删除动作绑定

路径参数 `actionId`；动作不存在或不属于当前租户 → 1003。响应 `data = null`。

### 2.2 规则视图（只读，复用既有规则域）

#### GET /api/automation/rules — 当前租户下规则列表（带动作匹配所需元数据）

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| enabled | query | Boolean | 否 | 按启用状态过滤 |

响应 `List<AutomationRuleView>`：

| 字段 | 类型 | 说明 |
|---|---|---|
| ruleId | String | 规则 ID |
| storeId | String | 所属店铺 |
| ruleType | String | 规则类型，如 `order_risk` |
| ruleName | String | 规则名 |
| ruleExpression | String | 白名单表达式，如 `amount < 100` |
| enabled | boolean | 是否启用 |

> 规则的创建/修改走既有 `POST /api/rules`、`PUT /api/rules/{id}`（不在本文范围；E2E 中使用 `ruleType=order_risk`）。

### 2.3 执行记录（automation_execution）

#### GET /api/automation/executions — 执行记录分页查询

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| storeId | query | String | 是 | 目标店铺（须属于当前租户，否则 1003） |
| status | query | String | 否 | 按状态过滤（SUCCESS / FAILED / PENDING_REVIEW / SKIPPED） |
| actionType | query | String | 否 | 按动作类型过滤 |
| triggerSource | query | String | 否 | 按触发来源过滤（MANUAL / SCHEDULE / EVENT） |
| page / size | query | Integer | 否 | 分页 |

`AutomationExecutionView` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| executionId | String | 执行记录 ID |
| storeId | String | 店铺 |
| ruleId | String | 命中规则 |
| ruleName | String? | 回填规则名 |
| actionType | String | 动作类型 |
| triggerSource | String | MANUAL / SCHEDULE / EVENT |
| targetType | String | M1 固定为 `ORDER` |
| targetId | String | 目标订单号 |
| result | String | 执行结果描述，如 `shipped:pdd-order-1001`、`bill_created:xxx`、`paused_for_review` |
| status | String | SUCCESS / FAILED / PENDING_REVIEW / SKIPPED |
| retryCount | int | 实际重试次数（attempts-1） |
| costMs | long | 执行耗时毫秒 |
| errorMessage | String? | 失败原因 |
| executedAt | OffsetDateTime | 执行时间 |

#### GET /api/automation/executions/{executionId} — 单条执行记录详情

路径参数 `executionId`；不存在或店铺不属于当前租户 → 1003。响应 `AutomationExecutionView`。

### 2.4 总览统计

#### GET /api/automation/stats — 店铺维度自动化统计

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| storeId | query | String | 是 | 目标店铺 |

`AutomationStatsView` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| total | int | 执行记录总数（该店铺） |
| success | int | SUCCESS 数 |
| failed | int | FAILED 数 |
| pendingReview | int | PENDING_REVIEW 数 |
| skipped | int | SKIPPED 数（M1 引擎不主动写入该状态） |
| todayTotal | int | 当日执行数 |
| todaySuccess | int | 当日成功数 |
| todayFailed | int | 当日失败数 |
| pendingApprovalCount | long | 当前 PENDING 人工确认任务数 |
| avgCostMs | long | 平均执行耗时 |
| byActionType | List | 按动作类型聚合：`{ actionType, count, success, failed }` |

### 2.5 人工确认队列（automation_approval）

#### GET /api/automation/approvals — 审批任务分页查询

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| status | query | String | 否 | PENDING / APPROVED / REJECTED |
| storeId | query | String | 否 | 按店铺过滤（带归属校验） |
| page / size | query | Integer | 否 | 分页 |

`AutomationApprovalView` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| taskId | String | 审批任务 ID |
| executionId | String | 关联的 PAUSE_REVIEW 执行记录 |
| storeId | String | 店铺 |
| targetType | String | M1 固定 `ORDER` |
| targetId | String | 目标订单号 |
| reason | String | 挂起原因，如 `rule=大额单人工复核 action=PAUSE_REVIEW requires manual review` |
| status | String | PENDING / APPROVED / REJECTED |
| reviewedBy | String? | 审批人 operatorId（未处理为 null） |
| reviewedAt | OffsetDateTime? | 审批时间 |
| createdAt | OffsetDateTime | 任务创建时间 |

#### POST /api/automation/approvals/{taskId}/approve — 人工放行

- 路径参数 `taskId`。
- 请求体可选：`ReviewDecisionRequest = { "comment": "string" }`（可空）。
- 行为：审批置 APPROVED（记录操作人/时间）→ 挂起的 PAUSE_REVIEW 执行记录改置 SUCCESS（result = `approved:{comment}`）→ 以 `triggerSource=EVENT` **续跑该规则下剩余动作序列**（GENERATE_BILL / AUTO_RECONCILE 等）。
- 错误：任务不存在 → 1003；已处理过 → 8602。
- 响应：`AutomationApprovalView`。

#### POST /api/automation/approvals/{taskId}/reject — 人工驳回

- 路径参数 `taskId`；请求体可选 `{ "comment": "string" }`。
- 行为：审批置 REJECTED → PAUSE_REVIEW 执行记录改置 FAILED（errorMessage = `reviewer rejected: {comment}`），**后续动作不再执行**。
- 错误码同上。响应：`AutomationApprovalView`。

### 2.6 手动触发闭环

#### POST /api/automation/trigger/order-sync — 手动拉单并执行规则引擎

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| storeId | query | String | 是 | 目标店铺（须属于当前租户，否则 1003） |

行为顺序：校验店铺归属 → 解析已授权渠道账户（无则 8603，渠道未授权则 8103）→ `ChannelSyncService.syncOrders` mock 拉单落库 → 对店铺下全部订单先 `auditOrder` 自动风控 → 逐订单、逐启用规则用 `RuleMatcher` 匹配 → 命中动作按 priority 执行并写执行记录；同时把该店铺登记进调度器候选集合（`scheduler.registerStore`）。

响应 `OrderSyncTriggerView`：

| 字段 | 类型 | 说明 |
|---|---|---|
| triggerSource | String | 手动触发固定返回 `MANUAL` |
| pulledOrderCount | int | 本次同步拉到的订单数（mock PDD = 2） |
| matchedRuleCount | int | 规则命中次数（订单 × 命中规则） |
| executionCount | int | 本次新建执行记录数（幂等去重后的净增量） |
| createdExecutionIds | List\<String\> | 新建执行记录 ID 列表 |

幂等语义：同一 `(tenantId, targetId=订单号, ruleId, actionType)` 组合已有执行记录时，直接跳过不重复执行（二次触发时 `executionCount=0`，但 `pulledOrderCount` 仍为 2，因为拉单本身仍会执行、订单同步走 reused 计数）。

---

## 3. 数据模型

迁移脚本：`V030__automation_engine.sql`（达梦兼容：TEXT/VARCHAR 替代 JSON，主键 VARCHAR(36) 存 UUID，`create table if not exists`，已追加至 `application.yml` 全部 4 处 schema-locations）。

### 3.1 automation_action（规则动作绑定）

| 列名 | 类型 | 说明 |
|---|---|---|
| action_id | VARCHAR(36) PK | 动作绑定 ID（UUID） |
| tenant_id | VARCHAR(36) | 租户 |
| store_id | VARCHAR(36) | 店铺 |
| rule_id | VARCHAR(36) | 关联规则 |
| action_type | VARCHAR(32) | 动作类型（枚举见下） |
| action_config | TEXT | 动作配置（JSON 字符串，M1 仅存储未消费） |
| priority | INT | 默认 0；同规则下按升序执行 |
| enabled | BOOLEAN | 默认 TRUE |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

索引：`(store_id, enabled, priority)`、`(rule_id)`。

### 3.2 automation_execution（执行记录）

| 列名 | 类型 | 说明 |
|---|---|---|
| execution_id | VARCHAR(36) PK | 执行记录 ID |
| tenant_id | VARCHAR(36) | 租户 |
| store_id | VARCHAR(36) | 店铺 |
| rule_id | VARCHAR(36) | 命中规则 |
| action_type | VARCHAR(32) | 本次执行的动作 |
| trigger_source | VARCHAR(16) | 触发来源（枚举见下） |
| target_type | VARCHAR(32) | 目标类型，M1 固定 `ORDER` |
| target_id | VARCHAR(64) | 目标 ID（订单号） |
| result | TEXT | 结果描述字符串 |
| status | VARCHAR(20) | 执行状态（枚举见下） |
| retry_count | INT | 默认 0；实际重试次数 |
| cost_ms | BIGINT | 默认 0；耗时毫秒 |
| error_message | TEXT | 失败信息 |
| executed_at | TIMESTAMP | 执行时间 |

索引：`(store_id, status, executed_at)`、`(tenant_id, target_id, rule_id, action_type)`（幂等去重键的查询索引）。

### 3.3 automation_approval（人工确认队列）

| 列名 | 类型 | 说明 |
|---|---|---|
| task_id | VARCHAR(36) PK | 审批任务 ID |
| execution_id | VARCHAR(36) | 关联的 PAUSE_REVIEW 执行记录 |
| tenant_id | VARCHAR(36) | 租户 |
| store_id | VARCHAR(36) | 店铺 |
| target_type | VARCHAR(32) | 目标类型，M1 固定 `ORDER` |
| target_id | VARCHAR(64) | 目标订单号 |
| reason | TEXT | 挂起原因 |
| status | VARCHAR(20) | 审批状态（枚举见下） |
| reviewed_by | VARCHAR(64) | 审批人 operatorId |
| reviewed_at | TIMESTAMP | 审批时间 |
| created_at | TIMESTAMP | 任务创建时间 |

索引：`(store_id, status, created_at)`。

### 3.4 枚举取值（以代码为准）

| 枚举 | 取值 | 出处 |
|---|---|---|
| action_type | `AUTO_SHIP` / `PAUSE_REVIEW` / `GENERATE_BILL` / `AUTO_RECONCILE` / `NOTIFY` | `AutomationActionService.SUPPORTED_ACTION_TYPES` |
| trigger_source | `MANUAL`（手动触发）/ `SCHEDULE`（定时调度）/ `EVENT`（人工放行后续跑） | 触发器/执行服务 |
| execution status | `SUCCESS` / `FAILED` / `PENDING_REVIEW` / `SKIPPED` | 查询/统计代码；`SKIPPED` 在统计口径中存在，M1 引擎不主动写入 |
| approval status | `PENDING` / `APPROVED` / `REJECTED` | 执行服务 |
| target_type | `ORDER`（M1 仅此一种；计划中的 `FINANCE_BILL` 目标尚未实现） | `AutomationExecutionService.TARGET_ORDER` |

### 3.5 幂等键与重试

- **幂等键**：`(tenant_id, target_id, rule_id, action_type)`。执行前按此键查 `automation_execution`，已存在则跳过该动作（二次触发净增量为 0）。
- **重试上限**：`MAX_RETRY = 3`（同进程内同步重试，无退避）；`retry_count` 落库为 `attempts - 1`。三次均失败后写 FAILED 并记 errorMessage。
- **PAUSE_REVIEW 挂起语义**：动作序列按 priority 升序执行；一旦遇到 `PAUSE_REVIEW`，写一条 `status=PENDING_REVIEW` 的执行记录 + 一条 `PENDING` 审批任务，**立即停止本序列后续动作**；人工 APPROVED 后从该规则的全部启用动作中重新续跑（幂等键保证已执行的动作不重复），REJECTED 则终止并把挂起记录改 FAILED。

---

## 4. 规则表达式白名单语法（RuleMatcher）

严格三段式：**字段 运算符 字面值**，空白分隔。不做任意脚本执行，杜绝注入。

- **允许的字段（仅三个）**：`amount`（订单总金额）、`status`（订单状态）、`platform`（店铺平台类型）。
- **允许的运算符**：`>=` `<=` `==` `!=` `=` `>` `<`（按出现的最早位置匹配运算符）。
- **字段语义**：
  - `amount`：与阈值做 `BigDecimal.compareTo` 比较；仅 `>= <= > < == != =` 有意义；字面值非数字时按不匹配处理。
  - `status` / `platform`：大小写不敏感的字符串等值/不等值比较；仅支持 `=` `==` `!=`，其余运算符一律不匹配。
- **命中上下文**：`RuleMatchContext(amount=order.totalAmount, orderStatus=order.orderStatus, platform=store.platformType)`。

示例：

```
amount < 100          # 小额单
amount >= 100         # 大额单
amount == 9.9
status = pending_fulfillment
status != shipped
platform = pdd
platform != douyin
```

**非法表达式行为**：任何无法解析的表达式（空、无运算符、字段不在白名单、字面值为空、amount 非数字等）一律返回 `false`（不匹配），**不抛错、不阻断引擎**。

---

## 5. Mock 行为说明

### 5.1 mock-mode 判定

- 配置入口：`app.channel.platforms.<平台>.mock-mode`（环境变量 `APP_CHANNEL_<平台>_MOCK_MODE`），六个平台默认均为 `true`（pdd/taobao/douyin/wechat_channels/kuaishou/jd）。
- 判定方法：`ChannelPlatformProperties.isMockMode(ChannelType)`，各适配器（如 `PddChannelClient`）在每个方法入口判断。

### 5.2 mock 下各方法返回

以 PDD 为例（其余平台适配器同构）：

| 方法 | mock 返回 |
|---|---|
| `handleCallback` | 伪 token：`accessToken=mock-access-pdd`、`refreshToken=mock-refresh-pdd`、Bearer、7 天/30 天过期 |
| `refreshToken` | 原 token 续期 7/30 天（不发起网络请求） |
| `checkConnectivity` | `success=true, message="mock 通道可达（真实网关待联调）"` |
| `pullOrders` | 固定 2 单：`pdd-order-1001`（金额 9.90）、`pdd-order-1002`（金额 199.00），买家"测试买家"、掩码电话、杭州测试地址 |
| `pullProducts` | 固定 2 个商品（9.90 / 199.00） |
| `pullAfterSales` | 1 条退款中售后单 |
| `shipOrder` | `success=true, message="ok", platformShipmentId="pdd-ship-{platformOrderId}"` |

> 注：自动化引擎发货时还会拼接自有运单号 `MOCK-SF-{8位UUID}` 传给渠道（`ChannelShipRequest.trackingNumber`），mock 渠道不校验、直接回传成功。

### 5.3 对账与单据生成行为（mock 链路）

- `GENERATE_BILL`：按订单日期生成 `sales` 类型财务单据；若当日单据已存在（财务侧业务码 `7402`），降级为 `result="bill_already_exists:{storeId}"` 仍记 SUCCESS，不阻断。
- `AUTO_RECONCILE`：取该店铺最新一条 draft 单据执行对账；若单据已非 draft，返回 `already_reconciled:{id}:{status}` 仍记 SUCCESS。
- `syncOrders` 二次拉单：订单同步走 reused 计数，不重复建单；执行记录幂等键拦截，`executionCount=0`。

---

## 6. 真实接入要点

接真实平台需替换/配置的点：

1. **凭证**：将 `app.channel.platforms.<平台>.mock-mode` 置 `false`，并配置 `app-key` / `app-secret`（环境变量 `APP_CHANNEL_<P>_APP_KEY/APP_SECRET`）。当前真实方法（`doRealPullOrders` / `doRealShip` / `doRealHandleCallback` 等）已按平台签名协议组装公共参数与 MD5 签名（纯函数可离线单测），但 HTTP 调用处均标注「待联调」并抛 `UnsupportedOperationException`，需在真实网关切通后补完。
2. **授权回调**：真实模式下走平台 OAuth → 回调路径 `app.channel.platforms.<平台>.callback-path`（如 `/api/channels/callback/pdd`）换取真实 access/refresh token；`ChannelSyncService` 会自动处理过期 token 刷新。
3. **回调与幂等**：真实订单推送/发货回传须复用现有订单幂等（`syncOrder` reused 语义）与执行引擎幂等键；对外回调需自行保障重复投递安全。
4. **人工确认底线**：任何涉及资金/退款的动作默认 `PAUSE_REVIEW` 挂起；**M1 不提供全自动退款**（无 REFUND 类动作类型），退款须走人工确认/售后域既有流程。
5. **调度器开关**：`app.automation.scheduler.enabled`（环境变量 `APP_AUTOMATION_SCHEDULER_ENABLED`，**默认 false**）。开启后（`@ConditionalOnProperty`）仅对「手动触发过的店铺」（`registerStore` 登记）按 `fixed-delay-ms`（默认 1800000ms = 30 分钟）周期性执行 mock 拉单 + 规则引擎；关闭时调度器 Bean 不装配，控制器中通过 `ObjectProvider.ifAvailable` 兜底，手动触发不受影响。

---

## 7. 验收记录

### 7.1 全量后端测试

- 命令：`mvn test`（注：`.mvn/maven.config` 将本地仓库置于 `target/m2repo`，Windows 下勿用 `mvn clean`，避免仓库自锁）
- 结果：**Tests run 344 / Failures 0 / Errors 0 / Skipped 0，BUILD SUCCESS**
- jacoco 门：**LINE 83.1%（≥0.80）、BRANCH 54.23%（≥0.50）通过**

### 7.2 E2E 全链路（AutomationFlowIntegrationTest.shouldRunFullAutomationLoop）

PDD mock-mode 全链路，10+ 步断言全过：

1. 注册租户 → 登录（Bearer token）
2. 建店（platformType=pdd，profitThreshold=18.50 / riskThreshold=72.00）
3. 种商品（避免订单同步 resolveProduct 报错）
4. PDD mock 授权 + 公开回调 `/api/channels/callback/pdd?code=mock-code&state=...` 换 token
5. 建规则 1「小额单自动闭环」`amount < 100`，绑 AUTO_SHIP(10) + GENERATE_BILL(20) + AUTO_RECONCILE(30)
6. 建规则 2「大额单人工复核」`amount >= 100`，绑 AUTO_SHIP(10) + PAUSE_REVIEW(20) + GENERATE_BILL(30) + AUTO_RECONCILE(40)
7. 手动触发 `POST /api/automation/trigger/order-sync`：`triggerSource=MANUAL`、**pulledOrderCount=2**、matchedRuleCount=2、**executionCount=5**
8. 执行记录：SUCCESS=4（小额单三动作 + 大额单发货）、PENDING_REVIEW=1（大额单挂起）
9. 人工确认队列：PENDING 任务 1 条
10. `POST /approvals/{taskId}/approve`（comment="ok to ship and bill"）→ 队列清空、执行记录新增 2 条（GENERATE_BILL / AUTO_RECONCILE），总数 7
11. 统计：total=7、success=7、failed=0、pendingReview=0、pendingApprovalCount=0、byActionType 4 类
12. 单条执行记录 `GET /executions/{id}` 可查
13. 二次触发：pulledOrderCount=2、**executionCount=0**（幂等去重生效）
14. 动作列表 7 条、启用规则列表 2 条

### 7.3 前端

- `npm run build` 通过（vue-tsc + vite build 0 错误）
- 自动化控制台页 `/app/automation-center` 已接入「订单履约」导航分组

### 7.4 运行实测（浏览器）

| 字段 | 内容 |
|---|---|
| 实测日期 | 2026-10-21 23:47–23:50（第一轮）；收尾因环境限制暂停（见下） |
| 环境 | 后端 8080（H2 内存库）+ 前端 5173（Vite dev） |
| 操作步骤 | 登录 → 进入自动化控制台 `/app/automation-center` → 查看总览（店铺选择器、手动触发拉单按钮、统计卡、按动作类型分布）→ 查看规则列表与动作绑定配置 → 点击手动触发拉单 |
| 截图文件路径 | `E:\ideaCode\dianShangPingTai\docs\screenshots\`：① `01-post-login-overview.png` 登录后首页（经营总览）；② `02-console-overview.png` 自动化控制台总览（初始数据 0）；③ `03-rules-actions.png` 规则列表与动作配置；④ `04-action-binding-table.png` 动作绑定表；⑤ `05-trigger-feedback.png` 手动触发返回「channel not authorized, please authorize first」（当时渠道授权步骤未完成） |
| 结果 | 自动化控制台页渲染与操作（店铺选择 / 规则动作绑定表单 / 手动触发按钮 / 统计卡）实测正常。**全链路数据闭环（拉单→风控→AUTO_SHIP→PAUSE_REVIEW→人工确认→财务单据→执行记录）已由端到端集成测试 `AutomationFlowIntegrationTest.shouldRunFullAutomationLoop` 在服务端完整验证（10+ 步断言全过，见上文 7.2）**；浏览器端「授权→触发→执行记录→人工确认」的收尾演示因当晚环境限制（机器休眠、系统确认弹窗、子代理执行通道故障）未能在浏览器补全，待用户在场时重启服务补做 |
| 当前服务器状态 | 后端（8080）与前端（5173）仍在运行（前端数据为空库状态） |
| 验收结论 | 页面可操作性与渲染 = 实测通过；服务端闭环 = E2E 测试通过；浏览器全链路补做 = 待办（不阻塞代码交付） |

---

## 8. 变更记录

| 日期 | 范围 | 说明 | 关联 commit |
|---|---|---|---|
| 2026-10-21 | M1 | 初版：接口清单 / 三表模型 / 规则白名单语法 / mock 行为 / 真实接入要点 / 验收记录 | 提交后补充 |
| 2026-10-22 | M1 | 补充浏览器运行实测记录（7.4）与关联 commit | M1 运营自动化引擎实施提交（2026-10-22，saas 分支，见 git log） |
