# SDD开发任务清单

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v4.4 |
| 最后更新 | 2026-07-17 |
| 当前状态 | 本地业务补完与治理收口已完成当前轮次验证；当前本地执行面以文档/验证维护和等待阶段 13 外部条件为主 |
| 使用方式 | 当前开发过程中的唯一执行清单与停留点记录入口 |

## 1. 使用规则

- 这份文档只记录“现在接下来该做什么”。
- 开发时先看本文件，不先看 `02-最终交付说明.md`。
- 每次继续开发前，先核对真实代码、当前工作树、最近验证结果，再更新这里的状态。
- 本文件中的任务分为两类：
  - 本地可继续推进任务
  - 依赖外部条件任务

## 2. 当前任务盘点

### 2.1 本地可继续推进

| 编号 | 事项 | 状态 | 说明 |
| --- | --- | --- | --- |
| GOV-01 | 开放平台更细粒度 `scope` 拆分 | 已完成 | 已收紧为细粒度 scope 白名单，移除 `finance.read / order.read / inventory.write` 旧粗粒度创建与放行口径，并同步前后端与自动化测试 |
| GOV-02 | 防串租户校验收口 | 已完成 | 已将跨租户对象访问统一收口为 `object not found` 口径，补齐管理端跨租户访问回归覆盖，避免暴露对象存在性 |
| GOV-03 | 插件中心深化 | 已完成 | 已新增插件治理聚合接口与前端治理视图，基于 app / credential / webhook / log 形成插件治理闭环 |
| DOC-01 | 计划/停留点文档与真实进度同步 | 进行中 | `05 / 06 / 02` 需要持续与当前真实代码基线保持一致 |

### 2.2 外部条件依赖

| 编号 | 事项 | 状态 | 说明 |
| --- | --- | --- | --- |
| T13-01 | 外部 ERP / WMS / BI / 消息系统真实接线 | 进行中 | 当前只有 baseline、readiness、probe、本地 gate，不等于真实生产接线完成 |
| T13-02 | 观测平台真实接线 | 未完成 | 当前只有 observability-overview、probe 和本地 gate，不等于真实接线完成 |
| T13-03 | 发布与交付链路真实接线 | 未完成 | 当前只有 delivery-readiness、workflow/compose 资产、本地 gate，不等于真实发布完成 |
| T13-04 | 标准 SaaS / 私有化双环境正式验收 | 未完成 | 当前只有本地验收摘要与探测证据，不是正式验收完成 |

## 3. 已确认完成的本地收口

### 3.1 代码树与工程边界

以下事项已完成并已进入当前真实基线：

- `backend/` 已成为唯一后端工程目录。
- `frontend/` 已成为唯一前端工程目录。
- 旧 `src/main/java/com/dianshang/...` 和 `src/test/java/com/dianshang/...` 已处于迁移删除态，不再是开发入口。
- Maven、Dockerfile、脚本与文档已切换到 `backend` 基线。

### 3.2 需求补完首轮闭环

已确认完成首轮闭环的需求线：

- `REQ-P0-*`
- `REQ-P1-*`
- `REQ-P2-*`

说明：

- 这里的“完成”指的是本地代码与页面闭环已经具备，不代表真实外部系统已经接通。
- 后续不再把“页面缺失”当作当前默认主问题。

### 3.3 最近完成的业务/治理补丁

本轮已完成并验证：

1. 直播高级治理后端补齐：
   - `configure-commitment-whitelist`
   - `interaction/reply`
   - `risk-events`
   - `enable-control-mode`
   - `resume-system-mode`
   - `interaction/transfer`
   - `commitments/confirm`
   - `commitments/reject`
2. 直播页前端接线补齐：
   - 承诺白名单配置
   - 自动回复
   - 会话风险事件
   - 文档口径控场模式启用/恢复
3. 开放平台前端治理补齐：
   - 应用凭证列表
   - 刷新凭证
   - 吊销凭证
   - Webhook Secret 轮换
   - 更多 scope 选项

## 4. 当前真实停留点

当前停留点应统一理解为：

1. 本地可补的直播/开放平台最新缺口已收口。
2. 当前代码基线已经是 `backend/ + frontend/`，不是旧 `src/...`。
3. 截至 `2026-07-17`，未发现新的高优先级本地业务/治理红灯；当前默认本地动作应以 `DOC-01` 和验证维护为主。
4. 阶段 13 仍然是发布阻塞主线，但其很多事项依赖真实外部环境，不应假装成“本地再补几条 probe 就完成”。

## 5. 推荐执行顺序

### 5.1 下一轮本地开发顺序

1. 先完成 `DOC-01`：持续同步 `05 / 06 / 02` 与最新真实验证结果。
2. 如果继续做本地改动，先重新确认是否存在新的明确需求缺口或回归红灯，再进入实现。
3. 每做完一批本地改动，至少回归：
   - 定向后端测试
   - `cd frontend && npm run build`
   - `mvn test`
   - `mvn -q -DskipTests package`
4. 外部条件就绪后，再继续 `T13-*` 真接线与正式验收。

### 5.2 外部条件就绪后顺序

1. 先检查 `APP_EXTERNAL_*` 的真实 endpoint / credential / callback / broker 条件是否具备。
2. 再接 `APP_OBSERVABILITY_*`。
3. 再接 `APP_DELIVERY_*`。
4. 最后做标准 SaaS / 私有化双环境正式验收。

## 6. 最近已确认的验证

### 6.1 本轮新验证

```bash
mvn "-Dtest=OpenPlatformControllerTest,AdminTenantControllerTest,TenantSystemControllerTest" test
cd frontend && npm run build
mvn test
mvn -q -DskipTests package
```

已确认结果：

- `OpenPlatformControllerTest` 35/35 通过。
- `OpenPlatformControllerTest, AdminTenantControllerTest, TenantSystemControllerTest` 66/66 通过。
- `mvn test` 189/189 通过。
- 前端 `vue-tsc --noEmit && vite build` 通过。
- `mvn -q -DskipTests package` 通过。

### 6.2 历史有效工程验证

仓库中仍保留以下历史有效验证口径，可作为工程基线参考：

- `mvn test`
- `mvn -q -DskipTests package`
- `docker compose -f docker-compose.saas.yml config`
- `docker compose -f docker-compose.private.yml config`

注意：

- 这些口径只能证明本地工程闭环，不证明真实外部接线已完成。

## 7. 当前工作树说明

- 当前工作树仍然很脏，属于迁移态与持续收口态并存。
- 大量 `D src/...` 与 `?? backend/...`、`?? frontend/...` 是迁移后的正常表现，不应回退。
- 后续规划和开发判断必须基于 `backend/` 与 `frontend/` 的真实代码，而不是旧树的删除项。

## 8. 维护规则

后续每轮继续开发时，默认按下面方式留痕：

1. 先更新第 `2` 节任务状态。
2. 验证完成后更新第 `6` 节最近验证。
3. 停下来时更新第 `4` 节真实停留点。
4. 如果开发顺序变化，再更新第 `5` 节。
