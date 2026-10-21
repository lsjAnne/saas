# 运营自动化中心 M1：自动化主链闭环 · 实施计划

> 项目：E:\ideaCode\dianShangPingTai（分支 saas，HEAD=d4393f1）
> 制定日期：2026-10-21
> 范围：M1（自动化主线闭环）。M2 营销自动化 / M3 选品智能化与规模化另立计划。

---

## 1. 目标

把项目**已经存在的能力模块**串成一条可演示、可追踪的自动化闭环：

```
渠道拉单(定时/手动) → 自动风控审核 → 规则引擎判定
    ├─ 命中 AUTO_SHIP    → 自动发货回传 + 履约更新 + 自动生成财务单据
    └─ 命中 PAUSE_REVIEW → 挂起人工确认队列（L2 合规底线）
全链路写入执行记录（可审计、可追溯）
```

演示载体：抖店/拼多多 mock 模式（无需真实凭证即可跑通全流程）。

## 2. 现状盘点（已核验，避免重复开发）

### 2.1 已有可直接复用
| 域 | 模块 | 现有能力 |
|---|---|---|
| 渠道接入 | `backend/channel` | 6 平台 OAuth + 订单/商品/发货/售后同步（ChannelClient，mock-mode） |
| 订单 | `backend/order` | 订单同步入口、风险审核（金额/地址/电话）、拆合单、路由、OMS 工作台 |
| 履约 | `backend/fulfillment` | 履约任务、物流记录、外部路由（OSRM） |
| 财务 | `backend/finance` | 财务单据、结算、对账、凭证、发票/红冲、结账、利润报表、ERP 科目映射（已完整） |
| 规则定义 | `backend/rule` | 规则 CRUD、分类/风险治理、兜底模拟、编排视图（**缺执行引擎**） |
| 营销/选品 | `backend/campaign` `backend/product` | 活动/优惠券、选品候选/草稿/映射/发布 |
| 前端 | `frontend/src/pages` | 30 个页面（工作台/订单履约/财务结算/店铺渠道等） |

### 2.2 缺口（本次 M1 交付）
1. **规则执行引擎**：`backend/rule` 目前只有"规则定义与治理"，没有"触发→匹配→执行→日志"的引擎。
2. **订单自动闭环**：`syncOrder`→`auditOrder`→发货→财务单据 目前全是手动接口，无自动流转。
3. **自动调度器**：无定时拉单任务。
4. **前端自动化控制台**：无总览/规则动作配置/执行记录/人工确认队列页面。
5. **数据迁移**：缺规则动作绑定、执行记录、人工确认队列三张表。

## 3. 范围边界

### 3.1 本次做（M1）
- 新增自动化引擎模块 `backend/automation`：
  - 动作定义（ActionType）：`AUTO_SHIP`（自动发货）、`PAUSE_REVIEW`（挂起人工）、`AUTO_RECONCILE`（自动对账）、`GENERATE_BILL`（自动生成财务单据）、`NOTIFY`（通知）
  - 规则匹配器（按 ruleType + ruleExpression 匹配目标对象：订单/财务单据）
  - 执行服务（幂等、重试、补偿、人工确认挂起队列）
  - 调度器（@Scheduled 定时拉单，mock 数据）
- 订单自动闭环接线：channel ↔ order ↔ fulfillment ↔ finance
- 迁移脚本 `V030__automation_engine.sql`（达梦兼容：TEXT/VARCHAR，追加至 application.yml 全部 4 处 schema-locations）
- 前端「自动化控制台」页（总览 / 规则动作配置 / 执行记录 / 人工确认队列），接入导航
- 测试：单元测试 + 端到端集成测试（mock 全链路闭环）+ 全量 `mvn test`
- 契约文档 `docs/automation-center-contract.md`

### 3.2 本次不做（M2/M3）
- 营销自动发券、达人邀约自动化（M2）
- 选品数据引擎、ERP/WMS 真实对接、平台真实凭证联调（M3，保持 mock-mode）

## 4. 技术设计要点

### 4.1 数据模型（V030，达梦兼容）
```sql
-- 规则动作绑定：一条规则可绑多个动作
automation_action(
  action_id, rule_id, action_type,        -- AUTO_SHIP / PAUSE_REVIEW / AUTO_RECONCILE / GENERATE_BILL / NOTIFY
  action_config TEXT,                     -- 阈值/目标状态等 JSON（达梦用 TEXT）
  priority INT, enabled BOOLEAN, created_at TIMESTAMP
)
-- 执行记录（可审计）
automation_execution(
  execution_id, tenant_id, store_id, rule_id, action_type,
  trigger_source,                         -- SCHEDULE / MANUAL / EVENT
  target_type, target_id,                 -- ORDER / FINANCE_BILL
  result, status,                         -- SUCCESS / FAILED / PENDING_REVIEW / SKIPPED
  retry_count, cost_ms, error_message, executed_at
)
-- 人工确认队列（L2 挂起）
automation_approval(
  task_id, execution_id, tenant_id, store_id, target_type, target_id,
  reason, status,                         -- PENDING / APPROVED / REJECTED
  reviewed_by, reviewed_at, created_at
)
```
沿用 V026 风格：TEXT/VARCHAR 替代 JSON 类型，主键 VARCHAR(36)，`create table if not exists`。

### 4.2 引擎流程
```
TriggerSource(定时/手动/事件)
  → 加载租户启用规则（rule.enabled=true）
  → 按 ruleType 定位目标域（订单/财务单据）
  → RuleMatcher 解析 ruleExpression（金额/状态/平台比较）
  → 命中动作按 priority 依次执行
  → 写 automation_execution（幂等：同 target+rule+action 去重）
  → 失败重试（上限 3 次）/ PAUSE_REVIEW 挂起进队列
  → 人工确认后继续（APPROVED 触发后续动作 / REJECTED 终止）
```

### 4.3 复用与约定
- 复用：`ChannelClient.syncOrders`、`OrderApplicationService.syncOrder/auditOrder`、`channel.ship`、`FinanceService.generateBill`、`AuditLogService`、`@RequireTenantPermission`、mock-mode 配置范式
- 新增权限码（如 `AUTOMATION_MANAGE`）需同时追加 `AuthPermissionCodes` + `AuthBootstrapService.seedRolePermissions` 四个角色各一行（纯追加）
- 分层：application / controller / domain / infrastructure / model + dto，与既有模块一致
- Jdbc + InMemory 双仓储；`ApiResponse` / `BusinessException`；MockMvc 测试范式；jacoco 门（LINE≥0.80 / BRANCH≥0.50）
- 注意：`.mvn/maven.config` 将本地仓库放 `target/m2repo`，Windows 下 `mvn clean` 会自锁失败 → 用 `mvn test`（不带 clean），或确认 target 下无进程占用

## 5. 验收标准
1. 全量 `mvn test` 全绿（0 失败 0 错误，jacoco 达标）
2. 端到端集成测试通过：注册租户 → 登录 → 建店 → mock 拉单 → 自动风控 → AUTO_SHIP 自动发货（succeeded）→ PAUSE_REVIEW 挂起进人工队列 → 人工确认 → 财务单据生成 → 执行记录可查（全链路 10+ 步）
3. 前端「自动化控制台」页可操作（配置规则动作 / 查看执行记录 / 处理人工确认），浏览器实测截图
4. `git diff` 核对：既有 V001-V029 与既有业务行为零改动；tracked 修改仅限约定追加文件；新提交推送 GitHub saas 分支
5. 契约文档交付：`docs/automation-center-contract.md`（接口清单 + mock 行为 + 真实接入要点 + 验收记录）

## 6. 任务拆解（执行顺序）
1. V030 迁移脚本 + application.yml 4 处 schema-locations 追加
2. 自动化引擎：模型/仓储(Jdbc+InMemory)/服务/控制器 + 权限码种子
3. 订单自动闭环接线（channel↔order↔fulfillment↔finance）
4. 调度器（定时拉单，mock）
5. 前端自动化控制台页 + 路由/导航接入
6. 测试：单测 + 端到端集成 + 全量 mvn test
7. 验证交付：启动实测（后端 8080 + 前端 5173）、截图、契约文档、git 提交推送

## 7. 风险与依赖
- 规则表达式解析：限定白名单语法（金额阈值/状态/平台枚举比较），不做任意脚本，避免注入
- 人工确认是合规底线：任何涉及资金/退款的动作默认 PAUSE_REVIEW，不在 M1 做全自动退款
- 真实平台联调依赖用户提供凭证（M1 不阻塞，mock 可演示）
