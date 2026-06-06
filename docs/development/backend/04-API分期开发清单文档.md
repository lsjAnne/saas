# API 分期开发清单文档

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v1.28 |
| 最后更新 | 2026-05-31 |
| 当前状态 | 基线 API 已完成，会员、财务、看板接口已落地 |
| 统计口径 | 基于 `src/main/java/com/dianshang/platform/**/*Controller.java` |

## 1. API 总览

- ✅ 当前已落地 159 个 API 映射
- ✅ 已覆盖 18 个数据库迁移脚本：`V001` 到 `V018`
- ✅ 已形成“租户 -> 店铺 -> 商品 -> 库存 -> 订单 -> 履约 -> 售后 -> 审批 -> 通知 -> 营销 -> 会员 -> 财务 -> 看板”的完整主链路

## 2. 第 1 期：SaaS 与租户基座

状态：✅ 已完成

- `POST /api/tenants/register`
- `POST /api/tenants/{id}/start-trial`
- `GET /api/subscription-plans`
- `GET /api/tenants/{id}/subscription`
- `POST /api/tenants/{id}/subscription/subscribe`
- `POST /api/tenants/{id}/subscription/renew`
- `POST /api/tenants/{id}/subscription/upgrade`
- `POST /api/tenants/{id}/subscription/downgrade`
- `GET /api/tenants/{id}/quotas`
- `POST /api/tenants/{id}/quotas/consume`
- `POST /api/tenants/{id}/seats/purchase`
- `GET /api/tenants/{id}/billing-orders`
- `POST /api/tenants/{id}/invoice-requests`
- `GET /api/tenant/context`
- `GET /api/admin/tenants`
- `PUT /api/admin/tenants/{id}/feature-toggles`
- `POST /api/admin/tenants/{id}/suspend`
- `POST /api/admin/tenants/{id}/resume`
- `GET /api/tenant/system/health`

## 3. 第 2 期：认证、组织与平台协同

状态：✅ 已完成

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/me`
- `GET /api/organizations`
- `POST /api/organizations`
- `GET /api/organizations/{id}/members`
- `POST /api/organizations/{id}/members/invite`
- `PUT /api/organizations/{id}/members/{memberId}/role`
- `GET /api/tenant/audit-logs`
- `POST /api/admin/support-sessions`
- `GET /api/admin/support-sessions`
- `POST /api/admin/support-sessions/{id}/close`

## 4. 第 3 期：店铺、渠道、商品与供应链

状态：✅ 已完成

### 4.1 店铺与渠道

- `POST /api/stores/connect`
- `GET /api/stores`
- `GET /api/stores/{id}`
- `PUT /api/stores/{id}/settings`
- `GET /api/channel-accounts`
- `POST /api/channel-accounts`
- `POST /api/channel-accounts/{id}/refresh-auth`

### 4.2 商品与供应商

- `GET /api/candidate-products`
- `POST /api/candidate-products`
- `GET /api/candidate-products/{id}`
- `PUT /api/candidate-products/{id}`
- `POST /api/candidate-products/{id}/status`
- `GET /api/suppliers`
- `POST /api/suppliers`
- `GET /api/suppliers/{id}`
- `PUT /api/suppliers/{id}`
- `POST /api/suppliers/{id}/set-primary`
- `POST /api/suppliers/{id}/set-backup`
- `GET /api/product-drafts`
- `POST /api/product-drafts/generate`
- `GET /api/product-drafts/{id}`
- `PUT /api/product-drafts/{id}`
- `POST /api/product-drafts/{id}/publish`

### 4.3 供应链与库存

- `GET /api/product-mappings`
- `POST /api/product-mappings`
- `POST /api/product-mappings/{id}/switch-supplier`
- `GET /api/inventory-snapshots`
- `PUT /api/inventory-snapshots/{id}/safety-stock`
- `GET /api/replenishment-tasks`
- `POST /api/replenishment-tasks`
- `POST /api/replenishment-tasks/{id}/submit-approval`

## 5. 第 4 期：订单、履约、异常与售后

状态：✅ 已完成

- `GET /api/orders`
- `GET /api/orders/{id}`
- `POST /api/orders/sync`
- `GET /api/fulfillment-tasks`
- `GET /api/fulfillment-tasks/{id}`
- `POST /api/fulfillment-tasks/{id}/confirm`
- `POST /api/fulfillment-tasks/{id}/retry`
- `GET /api/fulfillment-tasks/{id}/logistics-records`
- `POST /api/fulfillment-tasks/{id}/logistics-records`
- `GET /api/exceptions`
- `GET /api/exceptions/{id}`
- `POST /api/exceptions/{id}/process`
- `POST /api/exceptions/{id}/ignore`
- `POST /api/exceptions/{id}/escalate`
- `GET /api/tickets`
- `GET /api/tickets/{id}`
- `POST /api/tickets/{id}/reply-suggestion`
- `POST /api/after-sales`
- `GET /api/after-sales/{id}`
- `POST /api/after-sales/{id}/submit-approval`

## 6. 第 5 期：客服、知识库、直播与规则

状态：✅ 已完成

### 6.1 会话与知识库

- `GET /api/conversations`
- `GET /api/conversations/{id}`
- `GET /api/conversations/{id}/messages`
- `POST /api/conversations/{id}/reply-suggestion`
- `POST /api/conversations/{id}/send`
- `POST /api/conversations/{id}/transfer-manual`
- `GET /api/faq-knowledge`
- `POST /api/faq-knowledge`
- `PUT /api/faq-knowledge/{id}`

### 6.2 直播计划与会话

- `GET /api/live-plans`
- `POST /api/live-plans`
- `GET /api/live-plans/{id}`
- `PUT /api/live-plans/{id}`
- `GET /api/live-plans/{id}/product-pool`
- `GET /api/live-plans/{id}/products`
- `POST /api/live-plans/{id}/products/bind`
- `DELETE /api/live-plans/{id}/products/{itemId}`
- `POST /api/live-plans/{id}/generate-script`
- `POST /api/live-plans/{id}/review-script`
- `POST /api/live-plans/{id}/review-scene-template`
- `POST /api/live-plans/{id}/configure-commitment-whitelist`
- `POST /api/live-plans/{id}/publish`
- `POST /api/live-plans/{id}/schedule`
- `POST /api/live-plans/{id}/duplicate`
- `POST /api/live-plans/{id}/cancel`
- `GET /api/live-accounts`
- `POST /api/live-plans/{id}/validate-concurrency`
- `POST /api/live-plans/{id}/start`
- `POST /api/live-plans/{id}/pause`
- `POST /api/live-plans/{id}/resume`
- `POST /api/live-plans/{id}/stop`
- `GET /api/live-sessions`
- `GET /api/live-sessions/{id}/status`
- `GET /api/live-sessions/concurrency-overview`
- `POST /api/live-plans/run-due`
- `POST /api/live-sessions/{id}/simulate-callback`
- `POST /api/live-sessions/{id}/skip-current-product`
- `POST /api/live-sessions/{id}/switch-scene`
- `POST /api/live-sessions/{id}/enable-control-mode`
- `POST /api/live-sessions/{id}/manual-takeover`
- `POST /api/live-sessions/{id}/resume-system-mode`
- `POST /api/live-sessions/{id}/interaction/reply`
- `POST /api/live-sessions/{id}/interaction/transfer`
- `POST /api/live-sessions/{id}/commitments/confirm`
- `POST /api/live-sessions/{id}/commitments/reject`
- `GET /api/live-sessions/{id}/risk-events`

### 6.3 自动化规则

- `GET /api/rules`
- `POST /api/rules`
- `PUT /api/rules/{id}`
- `POST /api/rules/{id}/enable`
- `POST /api/rules/{id}/disable`

## 7. 第 6 期：审批、通知、营销、会员、财务与看板

状态：✅ 已完成

### 7.1 审批中心

- `GET /api/approvals`
- `POST /api/approvals`
- `GET /api/approvals/{id}`
- `POST /api/approvals/{id}/approve`
- `POST /api/approvals/{id}/reject`
- `POST /api/approvals/{id}/transfer`

### 7.2 通知中心

- `GET /api/notifications`
- `POST /api/notifications/send`
- `POST /api/notifications/batch-send`
- `POST /api/notifications/{id}/retry`
- `POST /api/notifications/run-due`
- `GET /api/notification-templates`
- `PUT /api/notification-templates/{id}`

### 7.3 营销活动中心

- `GET /api/campaigns`
- `POST /api/campaigns`
- `GET /api/campaigns/{id}`
- `PUT /api/campaigns/{id}`
- `POST /api/campaigns/{id}/submit-approval`
- `POST /api/campaigns/{id}/publish`
- `GET /api/coupon-templates`
- `POST /api/coupon-templates`

### 7.4 会员中心

- `GET /api/members`
- `GET /api/members/{id}`
- `POST /api/members/{id}/tags`
- `DELETE /api/members/{id}/tags/{tagId}`
- `GET /api/member-tags`
- `POST /api/member-groups/export`

### 7.5 财务中心

- `GET /api/finance-bills`
- `POST /api/finance-bills/generate`
- `GET /api/finance-bills/{id}`
- `POST /api/finance-bills/{id}/reconcile`
- `POST /api/finance-bills/{id}/settle`

### 7.6 经营看板

- `GET /api/dashboard/summary`
- `GET /api/dashboard/trends`
- `GET /api/dashboard/risks`
- `GET /api/dashboard/campaign-analysis`
- `GET /api/dashboard/member-analysis`

## 8. 后续增强 API

状态：未完成

- 通知网关增强 API：真实短信、邮件、飞书、企微通道实接与回执回写
- 监控运营增强 API：指标导出、告警回调、巡检任务结果回写
- 财税外部增强 API：发票归档、分账清分、财务凭证对接

## 9. 关联文档

- [接口规格文档](../../design/interface/01-接口规格文档.md)
- [接口示例与字段说明](../../design/interface/02-接口示例与字段说明.md)
- [数据库表结构设计文档](../../design/data/02-数据库表结构设计文档.md)
- [SDD 开发任务清单](../process/06-SDD开发任务清单.md)
