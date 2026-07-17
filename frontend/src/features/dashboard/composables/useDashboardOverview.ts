import { computed, shallowRef } from 'vue';

import { getExceptions } from '@/services/exceptionCenterService';
import {
  getDashboardCampaignAnalysis,
  getDashboardMemberAnalysis,
  getDashboardRisks,
  getDashboardSummary
} from '@/services/dashboardService';
import { getInventorySnapshots, getReplenishmentTasks } from '@/services/inventoryReplenishmentCenterService';
import { getLivePlans, getLiveSessions } from '@/features/liveOperations/services/liveOperationsService';
import { getOmsWorkbench } from '@/services/orderFulfillmentService';
import { getProductDrafts } from '@/services/productCenterService';
import type {
  DashboardCampaignAnalysis,
  DashboardMemberAnalysis,
  DashboardRisk,
  DashboardSummary,
  ExceptionTask,
  InventorySnapshot,
  LivePlan,
  LiveSession,
  OmsWorkbenchView,
  ProductDraft,
  ReplenishmentTask
} from '@/services/apiTypes';

type PermissionMode = 'any' | 'all';

export interface DashboardPendingItem {
  id: string;
  title: string;
  description: string;
  count: number;
  to: string;
  requiredPermissions: string[];
  permissionMode?: PermissionMode;
  tone: 'neutral' | 'warn' | 'success';
}

export interface DashboardInventoryWarning {
  id: string;
  productId: string;
  skuId: string;
  availableStock: number;
  safetyStock: number;
  gap: number;
  hasPendingApproval: boolean;
  snapshotAt: string;
}

export interface DashboardActionItem {
  id: string;
  title: string;
  description: string;
  metricText: string;
  to: string;
  requiredPermissions: string[];
  permissionMode?: PermissionMode;
  tone: 'neutral' | 'warn' | 'success';
}

export interface DashboardQuickEntry {
  id: string;
  title: string;
  description: string;
  count: number;
  unit: string;
  to: string;
  requiredPermissions: string[];
  permissionMode?: PermissionMode;
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function isOpenExceptionStatus(status: string | null | undefined) {
  const normalized = normalizeText(status);
  return normalized !== 'resolved' && normalized !== 'ignored';
}

function isPendingPublishDraft(status: string | null | undefined) {
  return normalizeText(status).includes('pending_publish');
}

function isPendingApprovalTask(status: string | null | undefined) {
  return normalizeText(status) === 'pending_approval';
}

function isRunningLiveStatus(status: string | null | undefined) {
  const normalized = normalizeText(status);
  return normalized.includes('running') || normalized.includes('live');
}

function isScheduledLivePlan(status: string | null | undefined) {
  const normalized = normalizeText(status);
  return normalized.includes('scheduled') || normalized.includes('published');
}

export function useDashboardOverview() {
  const summary = shallowRef<DashboardSummary | null>(null);
  const risks = shallowRef<DashboardRisk[]>([]);
  const campaignAnalysis = shallowRef<DashboardCampaignAnalysis | null>(null);
  const memberAnalysis = shallowRef<DashboardMemberAnalysis | null>(null);
  const inventorySnapshots = shallowRef<InventorySnapshot[]>([]);
  const replenishmentTasks = shallowRef<ReplenishmentTask[]>([]);
  const exceptions = shallowRef<ExceptionTask[]>([]);
  const productDrafts = shallowRef<ProductDraft[]>([]);
  const livePlans = shallowRef<LivePlan[]>([]);
  const liveSessions = shallowRef<LiveSession[]>([]);
  const omsWorkbench = shallowRef<OmsWorkbenchView | null>(null);
  const isLoading = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);

  const openExceptions = computed(() =>
    exceptions.value.filter((item) => isOpenExceptionStatus(item.status))
  );

  const pendingPublishDrafts = computed(() =>
    productDrafts.value.filter((item) => isPendingPublishDraft(item.status))
  );

  const pendingApprovalTasks = computed(() =>
    replenishmentTasks.value.filter((item) => isPendingApprovalTask(item.taskStatus))
  );

  const runningLiveSessions = computed(() =>
    liveSessions.value.filter((item) => isRunningLiveStatus(item.sessionStatus))
  );

  const scheduledLivePlans = computed(() =>
    livePlans.value.filter((item) => isScheduledLivePlan(item.planStatus))
  );

  const inventoryWarnings = computed<DashboardInventoryWarning[]>(() => {
    const pendingApprovalKeys = new Set(
      pendingApprovalTasks.value.map((item) => `${item.productId}::${item.skuId}`)
    );

    return [...inventorySnapshots.value]
      .filter((item) => item.availableStock <= item.safetyStock)
      .sort((left, right) => {
        const gapDiff =
          right.safetyStock - right.availableStock - (left.safetyStock - left.availableStock);
        if (gapDiff !== 0) {
          return gapDiff;
        }

        return toTimestamp(right.snapshotAt) - toTimestamp(left.snapshotAt);
      })
      .slice(0, 5)
      .map((item) => ({
        id: item.inventorySnapshotId,
        productId: item.productId,
        skuId: item.skuId,
        availableStock: item.availableStock,
        safetyStock: item.safetyStock,
        gap: Math.max(item.safetyStock - item.availableStock, 0),
        hasPendingApproval: pendingApprovalKeys.has(`${item.productId}::${item.skuId}`),
        snapshotAt: item.snapshotAt
      }));
  });

  const pendingItems = computed<DashboardPendingItem[]>(() => {
    const items: DashboardPendingItem[] = [
      {
        id: 'order-risk',
        title: '订单风险与人工回放',
        description: '优先回到订单履约中心处理风险单、人工回放和逆向动作。',
        count: (omsWorkbench.value?.riskOrderCount ?? 0) + (omsWorkbench.value?.manualReplayCount ?? 0),
        to: '/app/order-fulfillment-center',
        requiredPermissions: ['order.manage'],
        permissionMode: 'all',
        tone: 'warn'
      },
      {
        id: 'exception-backlog',
        title: '异常中心积压',
        description: '处理仍处于新建、待办或处理中状态的异常任务。',
        count: openExceptions.value.length,
        to: '/app/exception-center',
        requiredPermissions: ['exception.manage'],
        permissionMode: 'all',
        tone: openExceptions.value.length ? 'warn' : 'success'
      },
      {
        id: 'pending-confirm',
        title: '待确认订单',
        description: '今天还有待确认订单，需要尽快完成核单与履约推进。',
        count: summary.value?.pendingConfirmCount ?? 0,
        to: '/app/order-fulfillment-center',
        requiredPermissions: ['order.manage'],
        permissionMode: 'all',
        tone: (summary.value?.pendingConfirmCount ?? 0) > 0 ? 'warn' : 'success'
      },
      {
        id: 'replenishment-approval',
        title: '补货待审批',
        description: '低库存补货任务已经生成，等待进入审批或采购动作。',
        count: pendingApprovalTasks.value.length,
        to: '/app/inventory-replenishment-center',
        requiredPermissions: ['inventory.manage'],
        permissionMode: 'all',
        tone: pendingApprovalTasks.value.length ? 'warn' : 'neutral'
      },
      {
        id: 'pending-publish',
        title: '商品草稿待发布',
        description: '草稿已经接近上架，适合尽快推进商品出栈。',
        count: pendingPublishDrafts.value.length,
        to: '/app/product-center',
        requiredPermissions: ['product.manage'],
        permissionMode: 'all',
        tone: pendingPublishDrafts.value.length ? 'neutral' : 'success'
      },
      {
        id: 'live-queue',
        title: '直播运行与排期',
        description: '查看在播会话和待开播计划，避免现场节奏断档。',
        count: runningLiveSessions.value.length + scheduledLivePlans.value.length,
        to: '/app/live-operations',
        requiredPermissions: ['live.manage'],
        permissionMode: 'all',
        tone: runningLiveSessions.value.length ? 'success' : 'neutral'
      }
    ];

    return items.sort((left, right) => right.count - left.count);
  });

  const suggestedActions = computed<DashboardActionItem[]>(() => {
    const items: DashboardActionItem[] = [];

    if (openExceptions.value.length > 0) {
      items.push({
        id: 'action-exception',
        title: '先清理异常积压',
        description: '异常中心里还有未关闭任务，先把最高优先级风险收口。',
        metricText: `${openExceptions.value.length} 条未关闭异常`,
        to: '/app/exception-center',
        requiredPermissions: ['exception.manage'],
        permissionMode: 'all',
        tone: 'warn'
      });
    }

    if (inventoryWarnings.value.length > 0) {
      items.push({
        id: 'action-inventory',
        title: '处理低库存预警',
        description: '低库存 SKU 已压到安全线下方，优先创建或推进补货审批。',
        metricText: `${inventoryWarnings.value.length} 个低库存 SKU`,
        to: '/app/inventory-replenishment-center',
        requiredPermissions: ['inventory.manage'],
        permissionMode: 'all',
        tone: 'warn'
      });
    }

    if (pendingPublishDrafts.value.length > 0) {
      items.push({
        id: 'action-product',
        title: '推进商品草稿发布',
        description: '把已接近完成的草稿推进到发布动作，避免候选与草稿长期积压。',
        metricText: `${pendingPublishDrafts.value.length} 个待发布草稿`,
        to: '/app/product-center',
        requiredPermissions: ['product.manage'],
        permissionMode: 'all',
        tone: 'neutral'
      });
    }

    if ((campaignAnalysis.value?.pendingApprovalCount ?? 0) > 0) {
      items.push({
        id: 'action-campaign',
        title: '清理活动审批积压',
        description: '活动中心仍有待审批项，先处理完再放大投放节奏。',
        metricText: `${campaignAnalysis.value?.pendingApprovalCount ?? 0} 个待审批活动`,
        to: '/app/campaign-center',
        requiredPermissions: ['campaign.manage'],
        permissionMode: 'all',
        tone: 'neutral'
      });
    }

    if ((memberAnalysis.value?.dormantMembers ?? 0) > 0) {
      items.push({
        id: 'action-member',
        title: '回收沉睡会员',
        description: '会员沉睡数量仍高，适合切到 CRM 中心安排触达或召回动作。',
        metricText: `${memberAnalysis.value?.dormantMembers ?? 0} 位沉睡会员`,
        to: '/app/member-crm-center',
        requiredPermissions: ['member.manage'],
        permissionMode: 'all',
        tone: 'neutral'
      });
    }

    if (runningLiveSessions.value.length > 0 || scheduledLivePlans.value.length > 0) {
      items.push({
        id: 'action-live',
        title: '盯住直播现场',
        description: '直播计划已经进入排期或执行阶段，建议尽快检查并发与风险信号。',
        metricText: `${runningLiveSessions.value.length} 场在播 / ${scheduledLivePlans.value.length} 场待开播`,
        to: '/app/live-operations',
        requiredPermissions: ['live.manage'],
        permissionMode: 'all',
        tone: runningLiveSessions.value.length > 0 ? 'success' : 'neutral'
      });
    }

    if (!items.length) {
      items.push({
        id: 'action-assistant',
        title: '查看系统建议动作',
        description: '当前没有明显积压，去经营助手里继续看商品、会员和活动建议。',
        metricText: summary.value?.topSuggestion || '系统正在生成下一步建议',
        to: '/app/business-assistant',
        requiredPermissions: ['dashboard.read', 'product.manage', 'member.manage', 'campaign.manage'],
        permissionMode: 'any',
        tone: 'success'
      });
    }

    return items.slice(0, 4);
  });

  const quickEntries = computed<DashboardQuickEntry[]>(() => [
    {
      id: 'quick-order',
      title: '查看订单',
      description: '回到订单与履约中心查看风险单、回放和逆向任务。',
      count: omsWorkbench.value?.standardizedOrderCount ?? 0,
      unit: '单',
      to: '/app/order-fulfillment-center',
      requiredPermissions: ['order.manage'],
      permissionMode: 'all'
    },
    {
      id: 'quick-exception',
      title: '查看异常',
      description: '进入异常中心处理未关闭风险与 SOP 建议动作。',
      count: openExceptions.value.length,
      unit: '条',
      to: '/app/exception-center',
      requiredPermissions: ['exception.manage'],
      permissionMode: 'all'
    },
    {
      id: 'quick-draft',
      title: '查看商品草稿',
      description: '直接进入商品中心处理待发布草稿和候选推进节奏。',
      count: pendingPublishDrafts.value.length,
      unit: '个',
      to: '/app/product-center',
      requiredPermissions: ['product.manage'],
      permissionMode: 'all'
    },
    {
      id: 'quick-live',
      title: '查看直播',
      description: '进入直播运营中心查看在播会话、排期与风险恢复状态。',
      count: runningLiveSessions.value.length + scheduledLivePlans.value.length,
      unit: '场',
      to: '/app/live-operations',
      requiredPermissions: ['live.manage'],
      permissionMode: 'all'
    }
  ]);

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [
        summaryPayload,
        riskPayload,
        campaignPayload,
        memberPayload,
        inventoryPayload,
        replenishmentPayload,
        exceptionPayload,
        draftPayload,
        livePlanPayload,
        liveSessionPayload,
        omsPayload
      ] = await Promise.all([
        getDashboardSummary(token),
        getDashboardRisks(token),
        getDashboardCampaignAnalysis(token),
        getDashboardMemberAnalysis(token),
        getInventorySnapshots(token),
        getReplenishmentTasks(token),
        getExceptions(token),
        getProductDrafts(token),
        getLivePlans(token),
        getLiveSessions(token),
        getOmsWorkbench(token)
      ]);

      summary.value = summaryPayload;
      risks.value = riskPayload;
      campaignAnalysis.value = campaignPayload;
      memberAnalysis.value = memberPayload;
      inventorySnapshots.value = inventoryPayload;
      replenishmentTasks.value = replenishmentPayload;
      exceptions.value = exceptionPayload;
      productDrafts.value = draftPayload;
      livePlans.value = livePlanPayload;
      liveSessions.value = liveSessionPayload;
      omsWorkbench.value = omsPayload;
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '首页驾驶舱数据加载失败，请稍后重试';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  return {
    summary,
    risks,
    campaignAnalysis,
    memberAnalysis,
    inventoryWarnings,
    pendingItems,
    suggestedActions,
    quickEntries,
    isLoading,
    errorMessage,
    load
  };
}
