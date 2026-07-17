# Tenant Console Frontend

## 说明

这是 `dianShangPingTai` 仓库内的独立前端子工程，用于承载租户登录后后台控制台与平台管理员控制台的真实前端实现。

当前已实现：

- 登录页与基于权限的路由守卫
- 登录后租户后台壳子
- 平台管理员控制台
- 订阅与计费中心、外部软件可选配置与 readiness 摘要
- 驾驶舱、经营助手、商品、供应商、库存补货、店铺渠道、订单履约、直播运营、客服辅助、组织成员、客服售后、营销、会员 CRM、财务结算、租户中心、审计导出、开放平台、审批、支持工单、通知中心、系统设置等页面

## 环境变量

复制 `.env.example` 并按实际后端地址调整：

```env
VITE_API_BASE_URL=http://127.0.0.1:8080
VITE_DEV_PROXY_TARGET=http://127.0.0.1:8080
```

说明：

- `VITE_API_BASE_URL`
  生产或独立部署时使用
- `VITE_DEV_PROXY_TARGET`
  本地 `vite dev server` 代理目标

## 本地启动

```bash
npm install
npm run dev
```

默认前端地址：

- `http://127.0.0.1:5173`

## 生产构建

```bash
npm run build
```

构建产物输出到：

- `frontend/dist`
