import { createRouter, createWebHistory } from 'vue-router';

import ApprovalCenterPage from '@/pages/ApprovalCenterPage.vue';
import AuditExportPage from '@/pages/AuditExportPage.vue';
import BusinessAnalyticsPage from '@/pages/BusinessAnalyticsPage.vue';
import BusinessAssistantPage from '@/pages/BusinessAssistantPage.vue';
import CampaignCenterPage from '@/pages/CampaignCenterPage.vue';
import ContentAssetCenterPage from '@/pages/ContentAssetCenterPage.vue';
import CustomerServiceAssistantPage from '@/pages/CustomerServiceAssistantPage.vue';
import CustomerServiceCenterPage from '@/pages/CustomerServiceCenterPage.vue';
import TenantConsoleLayout from '@/app/layouts/TenantConsoleLayout.vue';
import DashboardHomePage from '@/pages/DashboardHomePage.vue';
import ExceptionCenterPage from '@/pages/ExceptionCenterPage.vue';
import FinanceSettlementCenterPage from '@/pages/FinanceSettlementCenterPage.vue';
import InventoryReplenishmentCenterPage from '@/pages/InventoryReplenishmentCenterPage.vue';
import LiveOperationsPage from '@/pages/LiveOperationsPage.vue';
import LoginPage from '@/pages/LoginPage.vue';
import MemberCrmCenterPage from '@/pages/MemberCrmCenterPage.vue';
import NotificationsCenterPage from '@/pages/NotificationsCenterPage.vue';
import OpenPlatformPage from '@/pages/OpenPlatformPage.vue';
import OrderFulfillmentCenterPage from '@/pages/OrderFulfillmentCenterPage.vue';
import OrganizationMembersPage from '@/pages/OrganizationMembersPage.vue';
import PlatformAdminPage from '@/pages/PlatformAdminPage.vue';
import ProductCenterPage from '@/pages/ProductCenterPage.vue';
import ProductMappingCenterPage from '@/pages/ProductMappingCenterPage.vue';
import StoreChannelCenterPage from '@/pages/StoreChannelCenterPage.vue';
import SubscriptionBillingPage from '@/pages/SubscriptionBillingPage.vue';
import SupplierCenterPage from '@/pages/SupplierCenterPage.vue';
import SupportWorkbenchPage from '@/pages/SupportWorkbenchPage.vue';
import SystemSettingsPage from '@/pages/SystemSettingsPage.vue';
import TenantCenterPage from '@/pages/TenantCenterPage.vue';
import { useAuthStore } from '@/stores/authStore';

type ConsoleRouteMeta = {
  title: string;
  subtitle: string;
  navLabel: string;
  navDescription: string;
  focusItems: string[];
  requiredPermissions: string[];
  permissionMode?: 'any' | 'all';
};

export type ConsoleRouteDefinition = {
  path: string;
  name: string;
  component: object;
  meta: ConsoleRouteMeta;
};

export const consoleRoutes: ConsoleRouteDefinition[] = [
  {
    path: 'dashboard',
    name: 'dashboard',
    component: DashboardHomePage,
    meta: {
      title: '经营首页',
      subtitle: '追踪经营趋势、活动回报与关键风险信号',
      navLabel: '经营首页',
      navDescription: '总览经营走势、风险与活动分析',
      focusItems: ['经营摘要', '趋势节奏', '风险处理'],
      requiredPermissions: ['dashboard.read'],
      permissionMode: 'all'
    }
  },
  {
    path: 'business-analytics',
    name: 'business-analytics',
    component: BusinessAnalyticsPage,
    meta: {
      title: '经营分析',
      subtitle: '把销售趋势、毛利趋势、商品排行、异常分布、活动效果与会员复购收成统一经营诊断台',
      navLabel: '经营分析',
      navDescription: '查看趋势、利润、异常、活动和复购信号',
      focusItems: ['销售趋势', '毛利趋势', '异常分布'],
      requiredPermissions: ['dashboard.read', 'finance.manage', 'exception.manage', 'product.manage'],
      permissionMode: 'any'
    }
  },
  {
    path: 'business-assistant',
    name: 'business-assistant',
    component: BusinessAssistantPage,
    meta: {
      title: '经营助手',
      subtitle: '承接推荐中心、风险解释和执行跳转，让首页之后立即进入可行动的决策视图',
      navLabel: '经营助手',
      navDescription: '聚合商品、会员、活动建议与风险说明',
      focusItems: ['推荐总览', '机会清单', '风险解释'],
      requiredPermissions: ['dashboard.read', 'product.manage', 'member.manage', 'campaign.manage'],
      permissionMode: 'any'
    }
  },
  {
    path: 'product-center',
    name: 'product-center',
    component: ProductCenterPage,
    meta: {
      title: '\u5546\u54c1\u4e2d\u5fc3',
      subtitle:
        '\u7528\u5019\u9009\u6c60\u3001\u8349\u7a3f\u5de5\u4f4d\u4e0e\u53d1\u5e03\u51fa\u6808\u8282\u594f\u7edf\u4e00\u63a7\u4f4f\u5546\u54c1\u5b75\u5316\u94fe\u8def',
      navLabel: '\u5546\u54c1\u4e2d\u5fc3',
      navDescription:
        '\u7ba1\u7406\u5019\u9009\u5546\u54c1\u3001\u8349\u7a3f\u751f\u6210\u4e0e\u4e0a\u67b6\u53d1\u5e03',
      focusItems: [
        '\u5019\u9009\u6c60',
        '\u8349\u7a3f\u5de5\u4f4d',
        '\u53d1\u5e03\u51fa\u6808'
      ],
      requiredPermissions: ['product.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'product-mapping-center',
    name: 'product-mapping-center',
    component: ProductMappingCenterPage,
    meta: {
      title: '商品与货源映射',
      subtitle:
        '把平台商品、主备货源与映射校验统一放进同一张可执行的映射编排页',
      navLabel: '商品货源映射',
      navDescription:
        '管理平台商品、货源候选、SKU 映射与备用货源切换',
      focusItems: ['平台商品', '货源候选', '校验结果'],
      requiredPermissions: ['product.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'content-asset-center',
    name: 'content-asset-center',
    component: ContentAssetCenterPage,
    meta: {
      title: '内容素材中心',
      subtitle:
        '统一沉淀商品、直播与客服素材资产，让上传、生成、预览、归档与引用复用在同一页闭环。',
      navLabel: '内容素材中心',
      navDescription:
        '管理素材分类、素材列表、版本记录与引用来源，直接驱动商品、直播和客服场景复用。',
      focusItems: ['素材分类', '预览舞台', '版本与引用'],
      requiredPermissions: ['product.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'supplier-center',
    name: 'supplier-center',
    component: SupplierCenterPage,
    meta: {
      title: '供应商中心',
      subtitle: '围绕供应商主档、准入评审、评分卡和结算对账统一承接 SRM 主链',
      navLabel: '供应商中心',
      navDescription: '管理供应商主档、风险、预约与对账',
      focusItems: ['供应商主档', '准入评审', '结算对账'],
      requiredPermissions: ['supplier.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'inventory-replenishment-center',
    name: 'inventory-replenishment-center',
    component: InventoryReplenishmentCenterPage,
    meta: {
      title: '库存与补货中心',
      subtitle: '用一页串起库存快照、补货采购、WMS 联动和成本对账的执行主走廊',
      navLabel: '库存与补货',
      navDescription: '查看库存、补货、采购与 WMS 摘要',
      focusItems: ['库存快照', '补货采购', 'WMS 联动'],
      requiredPermissions: ['inventory.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'store-channel-center',
    name: 'store-channel-center',
    component: StoreChannelCenterPage,
    meta: {
      title: '\u5e97\u94fa\u4e0e\u6e20\u9053\u4e2d\u5fc3',
      subtitle:
        '\u5728\u540c\u4e00\u9875\u91cc\u7edf\u4e00\u5904\u7406\u5e97\u94fa\u63a5\u5165\u3001\u6e20\u9053\u6388\u6743\u4e0e\u9608\u503c\u7b56\u7565\uff0c\u907f\u514d\u4ea4\u4ed8\u8fb9\u754c\u88ab\u6253\u6563',
      navLabel: '\u5e97\u94fa\u4e0e\u6e20\u9053',
      navDescription:
        '\u7ba1\u7406\u5e97\u94fa\u63a5\u5165\u3001\u6e20\u9053\u8d26\u53f7\u4e0e\u6388\u6743\u6709\u6548\u671f',
      focusItems: [
        '\u5e97\u94fa\u63a5\u5165',
        '\u6388\u6743\u77e9\u9635',
        '\u9608\u503c\u7b56\u7565'
      ],
      requiredPermissions: ['store.manage', 'channel.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'order-fulfillment-center',
    name: 'order-fulfillment-center',
    component: OrderFulfillmentCenterPage,
    meta: {
      title: '订单与履约中心',
      subtitle:
        '把订单同步、OMS 摘要、履约任务、物流记录与逆向动作统一到同一条订单处理主链路',
      navLabel: '订单与履约',
      navDescription:
        '查看订单主队列、履约任务、物流记录与高风险处理动作',
      focusItems: ['订单主队列', '履约任务', '逆向处理'],
      requiredPermissions: ['order.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'exception-center',
    name: 'exception-center',
    component: ExceptionCenterPage,
    meta: {
      title: '异常中心',
      subtitle: '集中处理系统识别出的异常、推荐 SOP 和人工升级动作',
      navLabel: '异常中心',
      navDescription: '处理异常主队列、SOP 建议与升级动作',
      focusItems: ['异常分类', '主队列', '处理动作'],
      requiredPermissions: ['exception.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'live-operations',
    name: 'live-operations',
    component: LiveOperationsPage,
    meta: {
      title: '直播运营中心',
      subtitle: '聚合直播计划、运行会话、并发占用与风险恢复动作的统一指挥台',
      navLabel: '直播运营中心',
      navDescription: '查看开播计划、在播会话与并发风控状态',
      focusItems: ['计划跑道', '会话指挥', '风险雷达'],
      requiredPermissions: ['live.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'service-assistant',
    name: 'service-assistant',
    component: CustomerServiceAssistantPage,
    meta: {
      title: '客户服务辅助中心',
      subtitle: '围绕会话消息、AI 建议和人工接管形成闭环处理台',
      navLabel: '客户服务辅助',
      navDescription: '处理会话消息、风险提示与人工接管',
      focusItems: ['会话列表', '风险命中', '人工接管'],
      requiredPermissions: ['qa.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'organization-members',
    name: 'organization-members',
    component: OrganizationMembersPage,
    meta: {
      title: '组织与成员',
      subtitle: '统一查看组织协作、成员结构与会员经营视角',
      navLabel: '组织与成员',
      navDescription: '查看组织结构、成员与会员经营信号',
      focusItems: ['组织列表', '成员结构', '会员经营信号'],
      requiredPermissions: ['organization.manage', 'member.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'customer-service-center',
    name: 'customer-service-center',
    component: CustomerServiceCenterPage,
    meta: {
      title: '客服售后中心',
      subtitle: '明确承接工单与售后单主链，不再让客服域只停留在会话辅助或支持联动层',
      navLabel: '客服售后中心',
      navDescription: '处理工单、售后申请、SLA 与满意度',
      focusItems: ['工单池', '售后申请', 'SLA 总览'],
      requiredPermissions: ['servicecase.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'campaign-center',
    name: 'campaign-center',
    component: CampaignCenterPage,
    meta: {
      title: '营销活动中心',
      subtitle: '把活动、优惠券、审批和发布轨道收敛到一张营销业务页',
      navLabel: '营销活动中心',
      navDescription: '管理活动列表、模板池和发布流程',
      focusItems: ['活动列表', '优惠券模板', '发布轨道'],
      requiredPermissions: ['campaign.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'member-crm-center',
    name: 'member-crm-center',
    component: MemberCrmCenterPage,
    meta: {
      title: '会员与 CRM 中心',
      subtitle: '围绕会员池、标签、分群、触达和 CRM 分析形成独立的会员经营主页',
      navLabel: '会员与 CRM',
      navDescription: '管理会员池、标签、分群与触达任务',
      focusItems: ['会员池', '分群规则', '触达任务'],
      requiredPermissions: ['member.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'finance-settlement-center',
    name: 'finance-settlement-center',
    component: FinanceSettlementCenterPage,
    meta: {
      title: '财务结算中心',
      subtitle: '先围绕账单、回款、利润和票据形成财务主链，再逐步展开 ERP 与关账治理',
      navLabel: '财务结算中心',
      navDescription: '查看账单、结算、利润和发票凭证',
      focusItems: ['账单主链', '应收回款', '利润摘要'],
      requiredPermissions: ['finance.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'tenant-center',
    name: 'tenant-center',
    component: TenantCenterPage,
    meta: {
      title: '租户中心',
      subtitle: '聚合租户上下文、配额、导出与生命周期动作',
      navLabel: '租户中心',
      navDescription: '管理租户上下文、配额与合规动作',
      focusItems: ['租户上下文', '合规确认', '生命周期摘要'],
      requiredPermissions: ['platform.tenant.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'subscription',
    name: 'subscription',
    component: SubscriptionBillingPage,
    meta: {
      title: '订阅与计费中心',
      subtitle: '保持套餐边界、软件选择与观测门禁的一致口径',
      navLabel: '订阅与计费中心',
      navDescription: '维护套餐、外部集成偏好与交付边界',
      focusItems: ['当前套餐', '席位与配额', '外部软件选择'],
      requiredPermissions: ['subscription.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'audit-exports',
    name: 'audit-exports',
    component: AuditExportPage,
    meta: {
      title: '审计与导出',
      subtitle: '覆盖审计留痕、导出治理与数据交付责任',
      navLabel: '审计与导出',
      navDescription: '追踪审计日志与数据导出治理',
      focusItems: ['审计日志', '导出治理', '留痕下载'],
      requiredPermissions: ['tenant.audit.read', 'tenant.data.export.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'open-platform',
    name: 'open-platform',
    component: OpenPlatformPage,
    meta: {
      title: '开放接口',
      subtitle: '沉淀 apps、webhooks 与集成审计的统一控制台',
      navLabel: '开放接口',
      navDescription: '统一管理 apps、webhooks 与调用日志',
      focusItems: ['接口总览', 'Webhook 编排', '集成审计'],
      requiredPermissions: ['openplatform.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'approval-center',
    name: 'approval-center',
    component: ApprovalCenterPage,
    meta: {
      title: '审批与协作中心',
      subtitle: '聚合审批队列、模板治理与协作责任流转的统一工作台',
      navLabel: '审批与协作',
      navDescription: '查看审批积压、模板阶段与治理请求流转',
      focusItems: ['审批队列', '模板矩阵', '治理请求'],
      requiredPermissions: ['approval.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'support-workbench',
    name: 'support-workbench',
    component: SupportWorkbenchPage,
    meta: {
      title: '支持与工单',
      subtitle: '围绕工单 SLA、服务会话与通知链路联动处理',
      navLabel: '支持与工单',
      navDescription: '聚合 SLA、会话、通知与服务队列',
      focusItems: ['工单 SLA', '服务会话', '通知网关'],
      requiredPermissions: ['servicecase.manage', 'approval.manage', 'notification.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'notifications-center',
    name: 'notifications-center',
    component: NotificationsCenterPage,
    meta: {
      title: '消息通知中心',
      subtitle: '聚合通知网关、渠道统计、失败重试与死信压力的统一总控页',
      navLabel: '消息通知中心',
      navDescription: '查看通知网关状态、通道质量与最近任务时间线',
      focusItems: ['网关矩阵', '通道质量', '异常任务'],
      requiredPermissions: ['notification.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'system-settings',
    name: 'system-settings',
    component: SystemSettingsPage,
    meta: {
      title: '系统设置',
      subtitle: '统一维护策略版本、readiness 与高级参数',
      navLabel: '系统设置',
      navDescription: '维护策略版本、readiness 与高级配置',
      focusItems: ['高级配置', 'Readiness 门禁', '合规版本'],
      requiredPermissions: ['tenant.compliance.read', 'subscription.manage'],
      permissionMode: 'all'
    }
  },
  {
    path: 'platform-admin',
    name: 'platform-admin',
    component: PlatformAdminPage,
    meta: {
      title: '平台管理中心',
      subtitle: '专供平台角色处理租户治理、功能开关、readiness 三联和合规文档工作流',
      navLabel: '平台管理中心',
      navDescription: '处理租户矩阵、readiness 与合规治理',
      focusItems: ['租户矩阵', 'Feature Toggle', 'Readiness'],
      requiredPermissions: ['platform.tenant.manage', 'platform.support.manage'],
      permissionMode: 'any'
    }
  }
];

export function canAccessConsoleRoute(
  authStore: ReturnType<typeof useAuthStore>,
  route: ConsoleRouteDefinition
) {
  return route.meta.permissionMode === 'any'
    ? authStore.hasAnyPermission(route.meta.requiredPermissions)
    : authStore.hasAllPermissions(route.meta.requiredPermissions);
}

export function resolveFirstAccessibleConsolePath(
  authStore: ReturnType<typeof useAuthStore> = useAuthStore()
) {
  const firstAccessibleRoute = consoleRoutes.find((route) => canAccessConsoleRoute(authStore, route));

  return firstAccessibleRoute ? `/app/${firstAccessibleRoute.path}` : '/login';
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/app/dashboard'
    },
    {
      path: '/login',
      name: 'login',
      component: LoginPage
    },
    {
      path: '/app',
      component: TenantConsoleLayout,
      children: [
        {
          path: '',
          redirect: '/app/dashboard'
        }
      ].concat(consoleRoutes)
    }
  ]
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  authStore.hydrate();

  if (to.path.startsWith('/app') && !authStore.isAuthenticated) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    };
  }

  if (to.path.startsWith('/app') && authStore.isAuthenticated) {
    const requiredPermissions = to.matched.flatMap(
      (record) => ((record.meta.requiredPermissions as string[] | undefined) ?? [])
    );
    const permissionMode =
      (to.meta.permissionMode as 'any' | 'all' | undefined) ?? 'all';

    const granted =
      permissionMode === 'any'
        ? authStore.hasAnyPermission(requiredPermissions)
        : authStore.hasAllPermissions(requiredPermissions);

    if (requiredPermissions.length && !granted) {
      const fallbackPath = resolveFirstAccessibleConsolePath();

      if (fallbackPath === '/login') {
        authStore.clearSession();
        return '/login';
      }

      if (to.path !== fallbackPath) {
        return {
          path: fallbackPath,
          query: {
            denied: '1'
          }
        };
      }
    }
  }

  return true;
});

export default router;
