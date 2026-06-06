# SDD 开发任务清单

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v2.1 |
| 最后更新 | 2026-06-06 |
| 当前状态 | `001-180` 已完成，`181-188` 未完成，`189-260` 已按批次收口 |
| 标记规则 | `已完成` / `未完成` |

## 1. 任务总览

| 范围 | 状态 | 覆盖内容 |
| --- | --- | --- |
| 001-030 | 已完成 | 项目底座、认证、租户、SaaS、组织、审计、平台协同基础 |
| 031-060 | 已完成 | 店铺、渠道、商品、供应商、草稿、映射、库存、补货、订单、履约、售后基础 |
| 061-120 | 已完成 | 会话、FAQ、直播、自动化规则、审批基础 |
| 121-163 | 已完成 | 审批深化、规则承接、直播执行深化 |
| 164-180 | 已完成 | 通知中心、营销、会员、财务、经营看板、交付文档收口 |
| 181-188 | 未完成 | SaaS 商用化与合规底座 |
| 189-206 | 已完成 | ERP 核心深化 |
| 207-214 | 已完成 | CRM 深化 |
| 215-222 | 已完成 | OMS 深化 |
| 223-230 | 已完成 | SRM 深化 |
| 231-238 | 已完成 | WMS 深化 |
| 239-244 | 已完成 | TMS 深化 |
| 245-250 | 已完成 | 平台治理、自动化与直播高级治理 |
| 251-260 | 已完成 | BI 数据分析系统建设 |

## 1.1 功能覆盖补充矩阵

| 功能域 | 功能覆盖 | 对应任务区间 | 当前状态 |
| --- | --- | --- | --- |
| SaaS 平台底座 | 登录、登出、当前用户、租户注册、试用开通、订阅、续费、升级、降级、配额、席位、计费、开票、租户管理、停复服、功能开关 | `001-020` | 部分完成 |
| 组织协同与平台支持 | 组织创建、成员邀请、角色调整、租户上下文、租户审计日志、支持会话创建/查询/关闭 | `005-009`、`021-030` | 部分完成 |
| 店铺与渠道 | 店铺接入、列表、详情、配置，渠道账号创建与刷新授权 | `021-022` | 部分完成 |
| 商品、供应商、库存、补货 | 候选商品、供应商、草稿、映射、库存快照、安全库存、补货任务、补货审批 | `023-030`、`189-206`、`223-230`、`231-238` | 部分完成 |
| 订单、履约、异常、售后 | 订单同步、订单查询、履约确认/重试、物流记录、异常处理、工单、售后提审 | `031-045`、`215-244` | 部分完成 |
| 客服会话与知识库 | 会话、消息、回复建议、发送、转人工、FAQ 知识库 | `046-048`、`207-214` | 部分完成 |
| 直播运营中心 | 直播计划、商品池、脚本、发布、排期、并发校验、开播/暂停/恢复/停止、到期执行、模拟回调、跳过当前商品、强控场模式、复杂场景切换、人工接管、高风险承诺治理 | `049-060`、`245-250`、`251-260` | 部分完成 |
| 自动化规则与风控 | 规则中心 CRUD、规则分类、风控分类、失败兜底、审计要求 | `061-064`、`245-250` | 部分完成 |
| 审批与通知 | 审批流转、通知模板、通知任务、批量发送、定时执行、死信策略 | `065-080`、`164-170`、`245-250` | 部分完成 |
| 营销与会员 | 营销活动、优惠券模板、会员档案、会员标签、人群导出、会员分析 | `171-175`、`207-214`、`251-260` | 部分完成 |
| 财务与结算 | 财务账单、对账、结算、发票、利润核算、应收应付 | `176-177`、`189-206` | 部分完成 |
| 经营看板与 BI | 总览、趋势、风险、营销分析、会员分析、主题域指标、分层数仓、驾驶舱、订阅预警 | `178`、`251-260` | 部分完成 |
| 开放平台与集成 | 插件、集成凭据、Webhook 订阅、回调安全、第三方系统集成、双交付 | `181-188`、`245-250`、`251-260` | 部分完成 |

## 1.2 ERP / WMS / TMS / BI 自研策略

- ERP / WMS / TMS / BI 四个系统域采用“自研优先、外部集成为后置增强”的任务编排原则。
- 当前代码已经具备 ERP、WMS、TMS、BI 的第一批自研最小闭环，后续任务不再把“先买第三方系统”作为前置条件。
- 阶段 13 中保留外部 ERP / WMS / TMS / BI 数据同步与联调任务，但这些任务属于增强项，不改变四个系统当前按自研主线推进的判断。
- 代码托管与交付工程化后续统一纳入 GitHub 账号 `https://github.com/lsjAnne` 对应的仓库规范、Actions 流水线与发布管理任务。

## 2. 已完成任务清单

### 2.1 001-030：底座、SaaS 与供应链基础

- 已完成 001 建立 Spring Boot 基础工程与统一响应结构
- 已完成 002 建立 SDD 目录结构与模块边界
- 已完成 003 建立 JDBC 持久化模式
- 已完成 004 接入 H2 内存数据库用于本地开发
- 已完成 005 建立平台支持会话基础能力
- 已完成 006 建立租户审计日志能力
- 已完成 007 建立组织与成员模型
- 已完成 008 建立租户上下文接口
- 已完成 009 建立系统健康检查接口
- 已完成 010 建立 MySQL Profile
- 已完成 011 建立 Redis Profile
- 已完成 012 建立 Redis 启动校验能力
- 已完成 013 建立租户键前缀规范
- 已完成 014 建立迁移脚本装载机制
- 已完成 015 建立登录、登出、当前用户接口
- 已完成 016 建立租户注册与试用开通
- 已完成 017 建立套餐订阅、续费、升级、降级
- 已完成 018 建立配额查询与配额消耗
- 已完成 019 建立席位购买与开票申请
- 已完成 020 建立平台租户管理与功能开关
- 已完成 021 建立店铺接入与店铺管理
- 已完成 022 建立渠道账号管理
- 已完成 023 建立候选商品采集与更新
- 已完成 024 建立供应商管理
- 已完成 025 建立商品草稿生成
- 已完成 026 建立商品草稿编辑与发布
- 已完成 027 建立商品映射与切换供应商
- 已完成 028 建立库存快照与安全库存调整
- 已完成 029 建立补货任务创建
- 已完成 030 建立补货审批提交流程

### 2.2 031-060：订单、履约、客服、直播基础

- 已完成 031 建立订单同步
- 已完成 032 建立订单查询
- 已完成 033 建立履约任务查询
- 已完成 034 建立履约确认
- 已完成 035 建立履约重试
- 已完成 036 建立物流记录查询
- 已完成 037 建立物流记录新增
- 已完成 038 建立异常中心列表
- 已完成 039 建立异常详情
- 已完成 040 建立异常处理、忽略、升级
- 已完成 041 建立工单列表与详情
- 已完成 042 建立回复建议
- 已完成 043 建立售后单创建
- 已完成 044 建立售后详情
- 已完成 045 建立售后提审
- 已完成 046 建立会话列表、详情、消息接口
- 已完成 047 建立回复建议、消息发送、转人工
- 已完成 048 建立 FAQ 知识库增删改查基础
- 已完成 049 建立直播计划创建与详情
- 已完成 050 建立直播计划编辑
- 已完成 051 建立商品池查询
- 已完成 052 建立直播商品绑定与解绑
- 已完成 053 建立直播脚本生成
- 已完成 054 建立直播脚本审核
- 已完成 055 建立直播计划发布与排期
- 已完成 056 建立直播计划复制与取消
- 已完成 057 建立直播并发校验
- 已完成 058 建立开播、暂停、恢复、停止
- 已完成 059 建立直播会话状态查询
- 已完成 060 建立到期计划执行、模拟回调、跳过当前商品

### 2.3 061-080：规则、审批与通知基础

- 已完成 061 建立自动化规则列表
- 已完成 062 建立自动化规则新增
- 已完成 063 建立自动化规则编辑
- 已完成 064 建立自动化规则启用与停用
- 已完成 065 建立审批单列表
- 已完成 066 建立审批单创建
- 已完成 067 建立审批详情
- 已完成 068 建立审批通过
- 已完成 069 建立审批驳回
- 已完成 070 建立审批转交
- 已完成 071 建立补货审批联动
- 已完成 072 建立售后审批联动
- 已完成 073 建立 `approval_instance` 持久化落地
- 已完成 074 建立审批审计日志
- 已完成 075 建立通知模板仓储接口
- 已完成 076 建立通知任务仓储接口
- 已完成 077 建立通知模板默认种子机制
- 已完成 078 建立通知任务列表能力
- 已完成 079 建立手动发送通知能力
- 已完成 080 建立失败通知重试能力

### 2.4 081-163：阶段性深化与交付收口

- 已完成 081-120：原清单中的阶段性设计、接口分期、联调、测试与文档承接任务
- 已完成 121-163：原清单中的直播深化、治理补齐、交付收口任务
- 说明：本次重生以“状态校准”为目标，不对原编号区间内未在当前文档展开的历史任务重新命名，避免新增未经证实的任务标题

### 2.5 164-180：通知、营销、会员、财务、看板与交付收口

- 已完成 164 新增消息通知中心服务与默认模板补齐逻辑
- 已完成 165 新增通知任务接口：`GET /api/notifications`、`POST /api/notifications/send`、`POST /api/notifications/{id}/retry`
- 已完成 166 新增通知模板接口：`GET /api/notification-templates`、`PUT /api/notification-templates/{id}`
- 已完成 167 打通审批创建、通过、驳回、转交通知联动
- 已完成 168 补齐通知中心自动化测试与 JDBC 持久化装配校验
- 已完成 169 扩展 `sms`、`email`、`feishu_bot` 渠道模板与发送校验
- 已完成 170 新增优先级、批量发送、定时执行与死信转移能力
- 已完成 171 新增营销活动与优惠券模板模块，打通提审、审批、发布闭环
- 已完成 172 承接优惠券模板与活动分析能力，完成营销侧 API 收口
- 已完成 173 新增会员档案查询与详情能力
- 已完成 174 新增会员标签维护与人群导出能力
- 已完成 175 新增会员分析与生命周期基线能力
- 已完成 176 新增财务账单列表、生成与详情能力
- 已完成 177 新增账单对账与结算闭环能力
- 已完成 178 新增经营看板总览、趋势、风险、营销分析、会员分析接口
- 已完成 179 补齐 CI/CD、镜像、灰度发布与回滚文档基线
- 已完成 180 补齐备份、恢复、巡检、运维与交付 SOP

## 3. 阶段任务清单（181-260）

### 3.1 SaaS 商用化与合规底座（181-188）

- 未完成 181 建立正式认证中心，替换演示账号与伪 token 机制
  - 当前进展：已移除默认演示账号注入，认证测试已切换为“租户注册 -> 登录 -> Bearer token 访问”，并补齐登录与 `GET /api/me` 的权限码返回；当前已补齐 `POST /api/auth/password/change`、`GET /api/auth/security-status`、`GET /api/auth/rbac/roles`、`GET /api/auth/access-checks`、`POST /api/auth/sensitive-operation-confirmations` 接口与对应集成测试，认证中心已从“只校验登录态”推进到“可查询密码治理状态、可自助改密、可返回租户角色矩阵与权限校验结果、可确认敏感操作并回传二次确认状态”的最小闭环。
- 未完成 182 建立密码加密、密钥管理、环境变量配置治理
  - 当前进展：`PasswordHashService` 已补齐强密码校验，`AuthSecurityConfigVerifier` 已暴露 token secret / bootstrap password 强度与 `requireExplicitSecrets` 状态，密码变更链路已要求“当前密码正确 + 新密码至少 12 位且同时包含字母和数字 + 新旧密码不能相同”，并已补齐弱配置状态查询与参数校验回归。
- 未完成 183 建立租户级 RBAC 权限矩阵与操作授权校验
  - 当前进展：已补齐组织成员与认证用户的同步开通链路，成员邀请/角色变更会同步更新 `auth_user`、角色码与 `operatorType`；本轮新增 `owner/admin/operator/service` 四类租户角色权限矩阵查询、权限自检接口，以及组织成员邀请/改角色时的租户角色归一化与授权校验，已覆盖“非法平台角色不可分配”“非 owner 不可将成员提升为 owner”等边界。
- 未完成 184 建立数据导出、删除、注销、留痕能力
  - 当前进展：已补齐租户侧停服、复服、退租申请状态流转接口，新增 `tenant_data_export_task`、`tenant_cleanup_task` 持久化表与 `GET/POST /api/tenant/data-exports`、`GET /api/tenant/data-exports/{taskId}/download`、`GET /api/tenants/{id}/cleanup-tasks`、`POST /api/tenants/{id}/cleanup-tasks/plan`、`POST /api/tenants/{id}/cleanup-tasks/{taskId}/review`、`POST /api/tenants/{id}/cleanup-tasks/{taskId}/execute` 接口；导出范围已扩展到租户档案、订阅摘要、审计日志、协议确认记录、订单经营数据、商品经营数据、库存补货数据、会员经营数据；退租清理已补齐“申请/规划 -> 复核 -> 执行”三段式状态机、`requestedBy/reviewedBy/executedBy` 三方留痕与“已复核任务不可重复建单”约束，清理执行继续保持仅删除经营数据、保留治理、订阅、审计与导出留痕闭环，并为 `tenant_cleanup_task` 新增 `cleanupScopes` 选定范围能力，支持按 `orders/products/inventory/members` 控制 impact/result summary 与实际删除范围，仅在全量范围下删除 `store/channel_account`；本轮已将 `POST /api/tenants/{id}/offboarding/request` 与 `POST /api/tenant/data-exports` 接入敏感操作二次确认门禁。
- 未完成 185 建立开放平台授权、回调验签、防重放机制
  - 当前进展：已补齐开放平台应用凭据鉴权、回调签名校验、5 分钟时间窗校验、防重放记录与调用日志留痕；新增 `POST /api/open/webhooks/{id}/secret/rotate` 支持 webhook 密钥轮换，轮换后旧签名会被拒绝、新签名可继续通过；本轮继续把外部 ERP 读取接口从单一 `finance.read` 推进到细粒度 `permissionScope` 授权边界，`/api/open/external/erp/master-data-dictionaries` 支持 `erp.master_data.read`、`/api/open/external/erp/account-mappings` 支持 `erp.account_mapping.read`、`/api/open/external/erp/integration-baseline` 支持 `erp.integration_baseline.read`，同时继续兼容 broad scope `finance.read`；缺 scope 仍会返回 `1009` 并记录 `rejected_scope` 调用日志，此外 `POST /api/open/apps/{id}/credentials/refresh` 与 `POST /api/open/webhooks/{id}/secret/rotate` 已接入敏感操作二次确认，开放平台安全链路已具备“凭据认证 + scope 授权分级 + 回调防重放 + 密钥轮换 + 高风险操作二次确认”的最小闭环。
- 未完成 186 建立商用计费、续费、停复服自动化流程
  - 当前进展：已新增平台运维巡检接口 `POST /api/admin/tenants/subscription-automation/reconcile`，支持对已到期的付费订阅执行自动续费与自动停服编排：`autoRenew=true` 且 `featureFlags.billing_auto_charge_authorized` 未关闭的租户会自动续期、补记 `auto_renew` 账单并保持或恢复 `active`；`autoRenew=false` 的租户会自动切换订阅状态为 `expired`，并将租户状态标记为 `expired_suspended`；当 `featureFlags.billing_auto_charge_authorized=false` 时，会生成 `paymentStatus=pending` 的 `auto_renew` 账单、将订阅标记为 `past_due`、将租户标记为 `payment_overdue`，并通过新增的 `POST /api/tenants/{id}/billing-orders/{billingOrderId}/settle` 在账单结清后恢复 `active`，形成“自动巡检 -> 待支付账单 -> 人工结清恢复”的最小失败补偿闭环。
- 未完成 187 建立隐私政策、用户协议、审计留痕配套接口
  - 当前进展：已新增隐私政策、用户协议查询接口，以及 `tenant_compliance_acceptance` 持久化表，支持协议确认留痕与查询；租户治理链路已经接入 Bearer token 权限校验，并通过审计导出、协议确认查询/确认、退租清理规划/复核/执行形成合规留痕闭环；本轮继续补齐 `GET /api/admin/tenants/compliance/documents`、`POST /api/admin/tenants/compliance/documents/{documentCode}/publish`、`GET /api/admin/tenants/compliance/acceptances`，支持平台侧协议版本发布、全局确认检索与 `pending_reacceptance` 待重签识别，并对 `tenant.audit.export`、`tenant.data.export.manage`、`tenant.lifecycle.manage`、`openplatform.manage` 落地“只拦待重签”的强制重签敏感操作门禁；后续仍需继续补更细粒度的受控删除策略。
- 未完成 188 建立商用版安全与合规验收清单
  - 当前进展：已在《测试与验收文档》中新增“3.12 商用安全与合规专项”，并继续补齐“正式放行总则、正式放行清单、放行证据要求、一票否决项、放行结论分级、验收模板补充项”，将 `181-190` 已实现能力沉淀为统一放行口径；本轮继续新增平台侧 `GET /api/admin/tenants/{id}/release-readiness`，聚合默认弱密钥/默认初始密码、导出/退租/协议/账单证据、租户审计可追溯性与自动化回归证据状态，输出 `allow_release/conditional_release/reject_release` 放行结论、阻塞原因、证据摘要与四项 checklist，避免功能已实现但缺少可执行的正式放行检查视图。

### 3.2 ERP 核心深化（189-206）

- 未完成 189 建立采购申请单模型与接口
  - 当前进展：已在现有供应链链路下补齐采购申请单模型与基础接口，新增 `GET/POST /api/purchase-requests`、`GET /api/purchase-requests/{id}`、`POST /api/purchase-requests/{id}/submit`，支持按租户校验店铺、供应商、商品归属，校验采购数量与目标单价，自动汇总申请总数、生成申请编号并记录审计日志，当前状态流转已覆盖 `draft -> submitted`，为后续采购单创建与审批下发预留衔接点。
- 未完成 190 建立采购单创建、审批、下发能力
  - 当前进展：已在现有供应链链路中新增采购单基础模型与接口，补齐 `GET/POST /api/purchase-orders`、`GET /api/purchase-orders/{id}`、`POST /api/purchase-orders/{id}/submit-approval`、`POST /api/purchase-orders/{id}/dispatch`，支持基于已提交采购申请单生成采购单、限制重复生成、复制采购明细与交期、接入审批中心 `purchase_order` 关联对象，并在审批通过后回写采购单状态为 `approved`、完成下发后切换为 `dispatched`，形成“申请单 -> 采购单 -> 审批 -> 下发”的最小闭环。
- 未完成 191 建立采购收货与采购入库能力
  - 当前进展：已在现有供应链链路中补齐采购收货与采购入库基础接口，新增 `GET /api/purchase-receipts`、`POST /api/purchase-orders/{id}/receive`，支持对已下发采购单执行收货校验、限制收货数量不超过采购数量、生成采购收货记录，并在收货完成后同步更新或创建库存快照，将采购单状态切换为 `received`、收货状态切换为 `completed`、入库状态切换为 `completed`，形成“下发 -> 收货 -> 入库 -> 库存更新”的最小闭环。
- 未完成 192 建立采购退货与入库差异处理能力
  - 当前进展：已在现有供应链链路中补齐采购入库差异与采购退货基础接口，新增 `GET /api/purchase-receipt-discrepancies`、`GET /api/purchase-returns`、`POST /api/purchase-orders/{id}/returns`，支持在少收入库时自动生成 `short_receive` 差异记录、回写采购单 `discrepancyStatus=reported`，并支持对已收货采购单发起部分退货、扣减库存快照可用量、生成退货记录并回写采购单 `returnStatus=partial_returned`，形成“收货 -> 差异登记 -> 退货 -> 库存回退”的最小闭环。
- 未完成 193 建立库存台账与出入库流水能力
  - 当前进展：已在现有供应链链路中补齐库存台账与出入库流水基础接口，新增 `GET /api/inventory-ledgers`、`GET /api/inventory-transactions`，支持在采购收货时自动记录 `purchase_receipt` 入库流水、在采购退货时自动记录 `purchase_return` 出库流水，并基于库存快照与流水累计值生成库存台账视图，返回 `available/reserved/safety/inboundTotal/outboundTotal/lastTransactionAt/riskLevel` 等关键字段，形成“库存快照 -> 出入库流水 -> 台账汇总”的最小闭环。
- 未完成 194 建立仓间调拨与库存冻结能力
  - 当前进展：已在现有供应链链路中补齐仓间调拨与库存冻结基础接口，新增 `GET /api/inventory-transfers`、`POST /api/inventory-transfers`、`POST /api/inventory-transfers/{id}/complete`、`GET /api/inventory-freezes`，支持按租户校验调出/调入店铺归属、校验源仓可用库存、发起调拨时自动冻结源仓库存并占用批次可用量，完成调拨后自动释放冻结、同步扣减源仓预留库存、向目标店铺回补库存并记录 `inventory_transfer_out` / `inventory_transfer_in` 流水，形成“冻结 -> 调拨完成 -> 冻结释放 -> 双仓库存更新”的最小闭环。
- 未完成 195 建立批次、有效期、成本批追踪能力
  - 当前进展：已在现有采购收货、退货、调拨链路中补齐批次与成本批基础接口，新增 `GET /api/inventory-batches`、`GET /api/inventory-cost-lots`，支持采购收货时写入批次号、生产日期、有效期、单位成本并自动生成成本批，支持采购退货按批次扣减可用量与成本批剩余数量，支持仓间调拨沿用原批次与单位成本在目标店铺生成新批次和新成本批，返回 `availableQty/lockedQty/remainingQty/remainingAmount/unitCost/expiryDate/sourceType` 等关键字段，形成“收货建批 -> 退货扣批 -> 调拨继承批次与成本”的最小闭环。
- 未完成 196 建立应付账款台账与供应商对账能力
  - 当前进展：已在现有采购链路中补齐应付账款台账与供应商对账基础接口，新增 `GET /api/payable-ledgers`、`GET /api/supplier-reconciliations`，支持采购收货时按收货数量与采购单价自动生成 `payable_increase` 应付分录、采购退货时自动生成 `payable_decrease` 冲减分录，并按供应商汇总 `receiptAmount/returnAmount/netPayableAmount/pendingDiscrepancyCount/reconciliationStatus` 等关键字段，形成“收货挂账 -> 退货冲减 -> 供应商对账汇总”的最小闭环。
- 未完成 197 建立采购费用分摊与采购成本归集能力
  - 当前进展：已在现有采购链路中补齐采购费用分摊与采购成本归集基础接口，新增 `GET/POST /api/purchase-expense-allocations`、`GET /api/purchase-cost-collections`，支持按采购单录入运费等采购费用、基于净收货数量计算单件分摊费用，并按采购收货成本减退货冲减后叠加分摊费用，返回 `basePurchaseCost/allocatedExpense/totalCollectedCost/unitCollectedCost/netReceiptQty/costLotCount` 等关键字段，形成“采购成本 -> 费用分摊 -> 成本归集”的最小闭环。
- 未完成 198 建立采购、库存、结算联动测试
  - 当前进展：已在现有集成测试链路中补齐采购、库存、结算联动回归，扩展 `SupplyChainControllerTest` 与 `MemberFinanceDashboardControllerTest`，覆盖“采购收货 -> 退货 -> 调拨 -> 应付挂账/冲减 -> 费用分摊 -> 成本归集 -> 应收台账 -> 客户回款 -> 财务账单生成/对账/结算 -> 凭证归档 -> 账期结转”的最小闭环，并完成专项联动测试与全量回归。
- 未完成 199 建立应收账款台账与客户回款记录能力
  - 当前进展：已在现有财务链路中补齐应收账款台账与客户回款记录基础接口，新增 `GET /api/receivable-ledgers`、`GET/POST /api/customer-payments`，支持基于订单自动生成应收台账视图、按订单累计已回款金额、限制回款金额不超过应收余额，并返回 `receivableAmount/collectedAmount/outstandingAmount/receivableStatus/paymentStatus` 等关键字段，形成“订单应收 -> 客户回款 -> 余额更新”的最小闭环。
- 未完成 200 建立财务凭证归档与账期结转能力
  - 当前进展：已在现有财务链路中补齐财务凭证归档与账期结转基础接口，新增 `GET /api/finance-vouchers`、`POST /api/finance-vouchers/archive`、`GET /api/finance-period-closings`、`POST /api/finance-period-closings/close`，支持按门店归档结算凭证与回款凭证、在结账检查全部通过后执行账期关闭，并返回 `linkedFinanceBillCount/archivedVoucherCount/closingStatus` 等关键字段，形成“结算/回款 -> 凭证归档 -> 账期结转”的最小闭环。
- 当前停留点（2026-06-06）
- 今日已继续推进 `181-188` 的阶段 8 主线，收口到 `181/183/184/185/186/187/188`：已在 `AuthControllerTest` / `AuthService` / `AuthController` / `AuthorizationInterceptor` / `OpenPlatformControllerTest` / `AdminTenantControllerTest` / `TenantLifecycleControllerTest` / `TenantLifecycleController` / `OpenPlatformController` 新增敏感操作二次确认接口、权限自检确认状态返回，并把高风险门禁从开放平台继续扩到退租申请与租户数据导出创建；同时保持细粒度 ERP 外部读取 scope 授权、平台侧放行检查接口 `GET /api/admin/tenants/{id}/release-readiness`、自动续费失败待支付账单、账单结清恢复服务与 JDBC `featureFlags` 兼容解析能力不回退。
- 最新验证结果为：`mvn -Dtest=AuthControllerTest test`、`mvn -Dtest=TenantLifecycleControllerTest test`、`mvn -Dtest=OpenPlatformControllerTest test`、`mvn -Dtest=AdminTenantControllerTest test` 定向/整类回归通过，`mvn test` 全量回归通过，当前全量测试为 `120` 个通过、`0` 失败、`0` 错误。
  - 当前状态仍属于“继续开发中”，不是“正式放行完成”；后续放行仍必须以测试与验收文档为唯一总纲。
  - 下一工作面继续收敛为 `181-188 SaaS 商用化与合规底座` 的剩余收口，优先处理 `181-183` 的生产级密钥注入、敏感操作二次确认与更多自动化放行证据沉淀，再回到阶段 13 的开放集成、观测与双交付深化。
- 未完成 201 建立利润核算、费用归集、门店利润报表
  - 当前进展：已在现有财务链路中补齐利润核算与门店利润报表基础接口，新增 `GET /api/profit-statements`、`GET /api/store-profit-reports`，支持按周期聚合订单收入、预估销售成本、采购费用分摊、净利润、利润率、利润阈值校验、费用类型拆分与应收未回款金额，并按门店输出利润报表汇总，形成“订单收入/利润 -> 费用归集 -> 门店利润报表”的最小闭环。
- 未完成 202 建立发票开具、作废、红冲、归档联动能力
  - 当前进展：已在现有财务链路中补齐发票开具、作废、红冲、归档基础接口，新增 `GET /api/finance-invoices`、`POST /api/finance-invoices/issue`、`POST /api/finance-invoices/{id}/archive`、`POST /api/finance-invoices/{id}/void`、`POST /api/finance-invoices/{id}/red-flush`，支持基于已 `settled` 的财务账单开具发票、归档纸票、对原票作废、按原票生成负向红冲票，并将 `finance bill detail` 的 `invoiceCheckStatus` 动态联动到真实发票状态，形成“账单结算 -> 开票 -> 作废/红冲 -> 发票归档”的最小闭环。
- 未完成 203 建立财务总账汇总视图与结账检查能力
  - 当前进展：已在现有财务链路中补齐 `GET /api/finance-general-ledgers`、`GET /api/finance-closing-checks`，支持按周期聚合财务账单、回款、应收未回款、应付净额、结算金额、归档凭证金额、发票金额、待归档数量与关账状态，并输出 `finance_bill_settlement/receivable_collection/voucher_archiving/invoice_archiving/supplier_reconciliation` 五类结账检查项，形成“总账汇总 -> 阻塞项识别 -> 关账放行判断”的闭环视图。
- 已完成 204 建立 ERP 财务月结、对账、结算自动化测试
  - 当前进展：已在 `MemberFinanceDashboardControllerTest` 补齐 ERP 财务月结、对账、结算自动化验收，覆盖“账单生成 -> 对账 -> 结算 -> 发票/凭证归档 -> 结账检查 -> 月结关闭”全链路，并验证“检查未通过时禁止月结、回款与凭证补齐后允许月结并生成 closing record”的关键门禁。
- 已完成 205 建立 ERP 主数据字典与科目映射配置
  - 当前进展：已在现有财务链路中补齐 `GET/POST /api/erp-master-data-dictionaries`、`GET/POST /api/erp-account-mappings`，支持租户按门店维护 ERP 主数据字典、凭证/业务类型到会计科目的映射配置，支持同键更新覆盖、按字典类型和映射分类筛选查询，并形成“业务编码 -> ERP 字典 -> 会计科目”的最小配置闭环。
- 已完成 206 建立 ERP 系统对外集成接口基线
  - 当前进展：已基于开放平台现有 `X-Open-App-Key/X-Open-App-Secret` 鉴权链路补齐 `GET /api/open/external/erp/master-data-dictionaries`、`GET /api/open/external/erp/account-mappings`、`GET /api/open/external/erp/integration-baseline`，支持外部 ERP 应用读取主数据字典、科目映射和集成基线资源清单，并沿用开放平台调用日志对外部访问进行留痕。

### 3.3 CRM 深化（207-214）

- 已完成 207 建立客户主档、联系人、客户来源字段扩展
- 已完成 208 建立客户分层、生命周期、沉默客户识别能力
- 已完成 209 建立客服工单 SLA、响应时效、满意度统计
- 已完成 210 建立会员标签自动化打标与客户分群规则
- 已完成 211 建立复购营销、会员召回、关怀触达任务能力
- 已完成 212 建立客户价值分析与客户流失预警能力
- 已完成 213 建立 CRM 与售后、订单、营销联动视图
- 已完成 214 建立 CRM 模块自动化测试与指标验收
  - 当前进展：已补齐 `POST /api/members/{id}/crm-profile`、`POST /api/member-segment-rules`、`POST /api/member-segment-rules/{id}/execute`、`GET /api/member-crm-analysis`、`POST/GET /api/member-touch-tasks`、`GET /api/members/{id}/crm-linkage`、`POST /api/tickets/{id}/satisfaction`、`GET /api/tickets/sla-overview`，支持客户主档扩展、沉默客识别、自动分群打标、召回触达任务、客户价值与流失预警、工单 SLA 与满意度统计，以及 CRM 对订单/售后/营销的联动视图；`MemberFinanceDashboardControllerTest`、`ServiceCaseControllerTest`、`CampaignControllerTest` 专项回归和 `mvn test` 全量回归均已通过。

### 3.4 OMS 深化（215-222）

- 已完成 215 建立全渠道订单汇单总线与标准化订单模型
- 已完成 216 建立订单审单、风险校验、地址校验能力
- 已完成 217 建立拆单、合单、组合单、赠品单处理能力
- 已完成 218 建立订单优先级编排、仓库路由、发货策略能力
- 已完成 219 建立退款中订单、售后中订单、逆向订单状态机
- 已完成 220 建立 OMS 与 WMS、TMS、财务的联动编排
- 已完成 221 建立 OMS 运营工作台与异常重放能力
- 已完成 222 建立 OMS 回归测试与压测基线
  - 当前进展：已补齐 `GET /api/orders/oms-standardized`、`POST /api/orders/{id}/audit-review`、`POST /api/orders/{id}/split`、`POST /api/orders/merge`、`POST /api/orders/{id}/route-plan`、`POST /api/orders/{id}/reverse-status`、`GET /api/orders/{id}/orchestration-view`、`GET /api/orders/oms-workbench` 与 `POST /api/fulfillment-tasks/{id}/exception-replay`，形成 OMS 汇单、审单、拆合单、路由编排、逆向订单、运营工作台与异常重放的最小闭环，并已通过 `OrderFulfillmentControllerTest` 定向验证。

### 3.5 SRM 深化（223-230）

- 已完成 223 建立供应商准入、资质审核、黑白名单能力
- 已完成 224 建立供应商评级、交付评分、履约评分能力
- 已完成 225 建立询价、比价、议价记录能力
- 已完成 226 建立采购协同、送货预约、收货确认能力
- 已完成 227 建立供应商账期、对账单、结算状态管理
- 
- 已完成 228 建立供应商异常、替补策略、风险预警能力
- 已完成 229 建立 SRM 与 ERP 采购、WMS 入库联动能力
- 已完成 230 建立 SRM 模块自动化测试与验收脚本
  - 当前进展：已补齐 `POST /api/suppliers/{id}/admission-review`、`POST /api/suppliers/{id}/scorecards`、`POST /api/supplier-inquiries`、`POST /api/suppliers/{id}/delivery-appointments`、`POST /api/supplier-settlement-statements`、`GET /api/supplier-settlement-statements`、`POST /api/suppliers/{id}/risk-events`、`GET /api/suppliers/{id}/srm-linkage`，形成供应商准入、评级、询价、预约送货、结算协同、风险预警与 SRM 联动视图闭环，并已通过 `SupplyChainControllerTest` 定向验证。

### 3.6 WMS 深化（231-238）

- 当前进展（2026-06-04）：已补齐 `POST /api/wms/warehouses`、`POST /api/wms/inbound-tasks`、`POST /api/wms/waves`、`POST /api/wms/locks`、`POST /api/wms/cycle-count-tasks`、`POST /api/wms/reverse-inbounds`、`GET /api/wms/linkage`，形成仓库、入库任务、波次、锁库、盘点、逆向入库与联动总览的最小闭环，并通过 `SupplyChainControllerTest#shouldManageWmsWarehouseExecutionWaveLockCountReverseAndLinkage` 验证。
- 已完成 231 建立仓库、库区、库位主数据能力
- 已完成 232 建立上架、拣货、复核、打包、出库执行能力
- 已完成 233 建立波次、分配、拣货策略引擎能力
- 已完成 234 建立批次、序列号、锁库、占库能力
- 已完成 235 建立盘点、差异调整、报损报溢能力
- 已完成 236 建立退货入库、换货入库、逆向库存处理能力
- 已完成 237 建立 WMS 与 OMS、ERP、TMS 联动能力
- 已完成 238 建立 WMS 模块自动化测试与盘点验收脚本

### 3.7 TMS 深化（239-244）

- 当前进展（2026-06-04）：已补齐 `POST /api/tms/carriers`、`POST /api/tms/shipments`、`POST /api/tms/shipments/{id}/tracking-events`、`POST /api/tms/freight-settlements`、`POST /api/tms/shipments/{id}/sign-off`、`POST /api/tms/reverse-logistics`、`GET /api/tms/control-tower`，形成承运商、运单、轨迹、运费结算、签收回单、逆向物流与控制塔的最小闭环，并通过 `OrderFulfillmentControllerTest#shouldManageTmsCarrierShipmentTrackingSettlementPodReverseAndTower` 验证。
- 已完成 239 建立承运商档案与物流渠道配置能力
- 已完成 240 建立面单、运单、轨迹回传、在途监控能力
- 已完成 241 建立运费计费规则与运费结算能力
- 已完成 242 建立签收、拒收、异常签收、回单归档能力
- 已完成 243 建立逆向物流与售后退回物流协同能力
- 已完成 244 建立 TMS 模块自动化测试与时效报表

### 3.8 平台治理、自动化与直播高级治理（245-250）

- 已完成 245 建立合同审批、付款审批、费用审批流程能力
  - 当前进展：已新增 `POST /api/governance-approval-requests`、`GET /api/governance-approval-requests`、`GET /api/governance-approval-requests/{id}`，支持合同、付款、费用三类治理审批申请单创建、列表与详情查询，并复用现有 `/api/approvals/{id}/approve|reject|transfer` 形成真实审批闭环。
- 已完成 246 建立平台治理审批模板与审批流编排能力
  - 当前进展：已新增 `GET /api/approval-templates`、`POST /api/approval-templates`，支持按 `approvalType` 配置多阶段审批模板；合同审批已验证“两级审批中间节点保持 pending、最终节点 approved”，付款审批已验证单级驳回后治理申请单同步切换为 `rejected`。
- 已完成 247 建立租户支持会话申请、审批、审计闭环
  - 当前进展：已新增 `POST /api/support-sessions/apply`、`GET /api/support-sessions`、`POST /api/admin/support-sessions/{id}/approve`、`POST /api/admin/support-sessions/{id}/reject`，并沿用 `POST /api/admin/support-sessions/{id}/close` 形成“租户申请 -> 平台审批/驳回 -> 会话关闭 -> 审计留痕”的最小闭环。
- 已完成 248 建立自动化规则分类落地（选品、发布、履约、异常、问答、直播、驾驶舱提醒）
  - 当前进展：已在 `automation_rule`、`RuleService`、`RuleController` 中补齐 `ruleCategory` 字段，规则创建、列表、更新、启停链路均已透出分类信息。
- 已完成 249 建立风控分类落地（内容、问答、直播、价格、供应商、履约）及直播高级治理对象，包括强控场、场景切换、人工接管、承诺审计
  - 当前进展：已在 `automation_rule` 中补齐 `riskCategory` 字段，并在 `live_session` / `LiveApplicationService` 中补齐 `strong-control`、`switch-scene`、`manual-takeover`、`promise-audit` 四类治理接口与会话治理态字段。
- 已完成 250 建立平台治理、自动化规则与直播高级治理自动化测试
  - 当前进展：已扩展 `SupportSessionAdminControllerTest`、`RuleControllerTest`、`LiveControllerTest` 覆盖支持会话审批闭环、规则分类/风控分类、直播高级治理链路，并通过定向、组合与全量回归验证。

### 3.9 BI 数据分析系统（251-260）

- 已完成 251 建立 BI 主题域模型：订单、商品、供应商、会员、客服、财务
  - 当前进展：已新增 `GET /api/bi/theme-domains`，按租户与店铺维度输出订单、商品、供应商、会员、客服、财务六大主题域的对象量、指标量、分层归属与新鲜度状态。
- 已完成 252 建立 BI 指标字典与口径管理能力
  - 当前进展：已新增 `GET/POST /api/bi/metric-dictionaries`，支持内置经营指标口径查询，以及租户级自定义指标字典维护。
- 已完成 253 建立 ODS/DWD/DWS/ADS 或等价分析分层方案
  - 当前进展：已新增 `GET /api/bi/layers`，输出 ODS/DWD/DWS/ADS 四层分析分层定义、粒度说明和输出用途。
- 已完成 254 建立经营总览、利润分析、复购分析看板
  - 当前进展：已新增 `GET /api/bi/overview`，输出最近 30 天销售额、净利润、复购率、活动活跃度和经营建议。
- 已完成 255 建立履约分析、供应商分析、客服分析看板
  - 当前进展：已新增 `GET /api/bi/operations-analysis`，输出待确认履约、超时履约、高风险供应商、客服工单与满意度等运营侧分析指标。
- 已完成 256 建立营销投产比、活动转化、优惠券效果看板
  - 当前进展：已新增 `GET /api/bi/marketing-analysis`，输出营销 ROI、活动转化率、优惠券模板数量与活动绑定效果。
- 已完成 257 建立租户级数据权限隔离与导出权限能力
  - 当前进展：已新增 `GET/POST /api/bi/export-tasks`，沿用租户上下文与 `tenant.data.export.manage` 权限进行店铺归属校验和导出任务管理。
- 已完成 258 建立管理驾驶舱、日报、周报、预警订阅能力
  - 当前进展：已新增 `GET /api/bi/cockpit`、`GET/POST /api/bi/subscriptions`，支持驾驶舱摘要、预警项汇总以及日报/周报/告警订阅。
- 已完成 259 建立 BI 数据校验、回灌、修复机制
  - 当前进展：已新增 `GET /api/bi/data-quality-checks`、`POST /api/bi/data-repair-tasks`，支持低库存、审批积压、通知死信、沉默会员等质量检查与修复任务编排。
- 已完成 260 建立 BI 系统联调、验收和交付文档
  - 当前进展：已新增 `GET /api/bi/delivery-checklist`，结合 BI 集成测试与三份状态文档同步，形成最小联调、验收和交付检查清单。

## 4. 当前验证记录

### 4.1 已完成验证

- 已完成：迁移脚本目录已存在 `24` 个版本脚本
- 已完成：接口映射已统计到 `308` 个
- 已完成：顶层业务模块已统计到 `28` 个
- 已完成：测试代码已统计到 `28` 个测试类、`117` 个测试方法

### 4.2 未完成验证

- 已完成：当前机器已在 `Java 17.0.17` 环境下执行 `mvn test` 并通过
- 已完成：本轮已完成完整测试复核，结果为 `117` 个测试全部通过
- 未完成：`181-188` 仍未全部进入实现、联调和验收阶段，阶段 13 的外部集成与双交付深化也尚未收口

## 5. 后续增强项

- 未完成：通知第三方网关实接、回执回写、死信人工重放
- 未完成：真实观测平台接入（指标、日志、链路、告警路由）
- 未完成：财税外部系统对接（发票、分账、归档）
- 未完成：CI/CD 平台真实接线与镜像仓库凭据治理
- 未完成：自动化规则分类与风控分级落地
- 未完成：直播高级治理对象与专项分析落地，包括强控场模式、复杂场景切换、人工接管、高风险承诺治理
- 未完成：ERP / OMS / SRM / WMS / TMS / BI 外部系统集成与数据同步
- 未完成：基于 GitHub 账号 `https://github.com/lsjAnne` 的仓库规范、Actions 流水线、发布标签与交付自动化

## 6. 当前继续开发优先级

1. `181-188` SaaS 商用化与合规底座
2. 阶段 13 其余开放集成、观测与双交付能力
3. 外部 BI / ERP / WMS / TMS 数据同步与观测实接

## 7. 关联文档

- [从 0 到落地全链路开发计划](./05-从0到落地全链路开发计划.md)
- [系统架构设计文档](../../design/architecture/01-系统架构设计文档.md)
- [技术架构设计文档](../../design/architecture/13-技术架构设计文档.md)
- [接口规格文档](../../design/interface/01-接口规格文档.md)
- [数据库表结构设计文档](../../design/data/02-数据库表结构设计文档.md)
