import { computed, shallowRef } from 'vue';

import { getCampaigns } from '@/features/campaignCenter/services/campaignCenterService';
import { getExceptions } from '@/services/exceptionCenterService';
import { getInventorySnapshots, getReplenishmentTasks } from '@/services/inventoryReplenishmentCenterService';
import { getMemberCrmAnalysis } from '@/services/memberCrmCenterService';
import { getFulfillmentTasks, getOrders } from '@/services/orderFulfillmentService';
import { getProducts } from '@/services/productMappingCenterService';
import {
  askBusinessAssistant,
  getCampaignRecommendations,
  getMemberRecommendations,
  getProductRecommendations,
  getRecommendationOverview
} from '@/services/recommendationService';
import { getStores } from '@/services/storeChannelService';
import type {
  BusinessAssistantAction,
  BusinessAssistantConfidence,
  BusinessAssistantResponse,
  CampaignActivity,
  CampaignRecommendationView,
  ExceptionTask,
  FulfillmentTask,
  InventorySnapshot,
  MemberCrmAnalysisView,
  MemberRecommendationView,
  OrderMain,
  Product,
  ProductRecommendationView,
  RecommendationOverviewView,
  ReplenishmentTask,
  Store
} from '@/services/apiTypes';
import { formatCurrency, formatDateTime } from '@/utils/formatters';

export type { BusinessAssistantAction, BusinessAssistantConfidence, BusinessAssistantResponse };
export type BusinessAssistantIntent = BusinessAssistantResponse['intent'];

interface AssistantSnapshot {
  recommendationCount: number;
  orderRiskCount: number;
  blockedFulfillmentCount: number;
  lowStockCount: number;
  activeCampaignCount: number;
  dormantMemberCount: number;
}

interface IntentResolution {
  intent: BusinessAssistantIntent;
  intentLabel: string;
  matchedSignals: string[];
}

interface IntentRule {
  intent: BusinessAssistantIntent;
  intentLabel: string;
  keywords: string[];
  bonus?: (question: string) => number;
}

const BASE_RECOMMENDED_QUESTIONS = [
  '今天卖什么最稳妥？',
  '哪些订单今天必须人工处理？',
  '为什么这单没自动发货？',
  '哪些商品应该优先补货？',
  '这周要不要做活动？',
  '有哪些沉默会员值得召回？',
  '订单履约现在卡在哪？',
  '今天经营上最该关注什么？'
] as const;

const INTENT_RULES: IntentRule[] = [
  {
    intent: 'fulfillment',
    intentLabel: '履约追查',
    keywords: ['自动发货', '发货', '履约', '物流', '迟发', '没发货', '未发货', '卡单'],
    bonus: (question) => (question.includes('为什么') ? 3 : 0)
  },
  {
    intent: 'inventory',
    intentLabel: '库存补货',
    keywords: ['补货', '缺货', '库存', '安全库存', 'sku', '采购', '断货']
  },
  {
    intent: 'campaign',
    intentLabel: '活动建议',
    keywords: ['活动', '促销', '优惠', '拉新', '投放', '上活动', '大促']
  },
  {
    intent: 'member',
    intentLabel: '会员经营',
    keywords: ['会员', '复购', '召回', '沉默会员', '流失', '高价值会员', '分群']
  },
  {
    intent: 'order',
    intentLabel: '订单盘点',
    keywords: ['订单', '成交', '超时', '待发', '客单', '履约进度']
  },
  {
    intent: 'risk',
    intentLabel: '风险排查',
    keywords: ['风险', '异常', '告警', '问题单', '预警']
  },
  {
    intent: 'product',
    intentLabel: '商品建议',
    keywords: ['卖什么', '选品', '主推', '商品', '上新', '爆品', '推荐商品']
  }
];

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function severityRank(value: string | null | undefined) {
  const normalized = normalizeText(value);

  if (normalized === 'critical') {
    return 3;
  }

  if (normalized === 'high') {
    return 2;
  }

  if (normalized === 'medium') {
    return 1;
  }

  return 0;
}

function isOpenStatus(value: string | null | undefined) {
  const normalized = normalizeText(value);

  return !['done', 'completed', 'confirmed', 'closed', 'published', 'approved', 'success'].includes(
    normalized
  );
}

function isPastDue(value: string | null | undefined) {
  return Boolean(value) && toTimestamp(value) > 0 && toTimestamp(value) <= Date.now();
}

function buildHistoryId() {
  return `assistant-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
}

function createFallbackInsights(lines: string[]) {
  return lines.length ? lines : ['当前没有足够的新线索，建议先切到对应业务中心查看明细。'];
}

function uniqueStrings(items: Array<string | null | undefined>, limit = items.length) {
  return [...new Set(items.filter((item): item is string => Boolean(item && item.trim())).map((item) => item.trim()))].slice(
    0,
    limit
  );
}

function resolveIntent(question: string): IntentResolution {
  const normalized = normalizeText(question);
  let bestRule: IntentRule | null = null;
  let bestMatches: string[] = [];
  let bestScore = 0;

  for (const rule of INTENT_RULES) {
    const matches = rule.keywords.filter((keyword) => normalized.includes(keyword));
    const score = matches.length * 10 + (rule.bonus?.(normalized) ?? 0);

    if (score > bestScore) {
      bestRule = rule;
      bestMatches = matches;
      bestScore = score;
    }
  }

  if (!bestRule) {
    return {
      intent: 'summary',
      intentLabel: '经营综述',
      matchedSignals: []
    };
  }

  return {
    intent: bestRule.intent,
    intentLabel: bestRule.intentLabel,
    matchedSignals: bestMatches
  };
}

function resolveConfidence(
  keywordMatchCount: number,
  evidenceCount: number
): BusinessAssistantConfidence {
  if (keywordMatchCount >= 2 && evidenceCount >= 2) {
    return 'high';
  }

  if ((keywordMatchCount >= 1 && evidenceCount >= 1) || evidenceCount >= 3) {
    return 'medium';
  }

  return 'low';
}

function confidenceLabel(confidence: BusinessAssistantConfidence) {
  if (confidence === 'high') {
    return '高置信';
  }

  if (confidence === 'medium') {
    return '中置信';
  }

  return '低置信';
}

function sortByScore<T extends { score: number }>(items: T[]) {
  return [...items].sort((left, right) => right.score - left.score);
}

export function useBusinessAssistantCopilot() {
  const stores = shallowRef<Store[]>([]);
  const overview = shallowRef<RecommendationOverviewView | null>(null);
  const productRecommendations = shallowRef<ProductRecommendationView[]>([]);
  const memberRecommendations = shallowRef<MemberRecommendationView[]>([]);
  const campaignRecommendations = shallowRef<CampaignRecommendationView[]>([]);
  const memberAnalysis = shallowRef<MemberCrmAnalysisView | null>(null);

  const exceptions = shallowRef<ExceptionTask[]>([]);
  const orders = shallowRef<OrderMain[]>([]);
  const fulfillmentTasks = shallowRef<FulfillmentTask[]>([]);
  const inventorySnapshots = shallowRef<InventorySnapshot[]>([]);
  const replenishmentTasks = shallowRef<ReplenishmentTask[]>([]);
  const products = shallowRef<Product[]>([]);
  const campaigns = shallowRef<CampaignActivity[]>([]);

  const activeStoreId = shallowRef('all');
  const draftQuestion = shallowRef('');
  const currentResponse = shallowRef<BusinessAssistantResponse | null>(null);
  const history = shallowRef<BusinessAssistantResponse[]>([]);
  const errorMessage = shallowRef<string | null>(null);
  const isBootstrapping = shallowRef(false);
  const isRefreshingCore = shallowRef(false);
  const isAnswering = shallowRef(false);
  const operationsLoaded = shallowRef(false);
  const inventoryLoaded = shallowRef(false);
  const campaignsLoaded = shallowRef(false);

  const sortedStores = computed(() =>
    [...stores.value].sort(
      (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const selectedStoreName = computed(() => {
    if (activeStoreId.value === 'all') {
      return '全部店铺';
    }

    return (
      sortedStores.value.find((store) => store.storeId === activeStoreId.value)?.shopName ??
      activeStoreId.value
    );
  });

  const filteredExceptions = computed(() =>
    activeStoreId.value === 'all'
      ? exceptions.value
      : exceptions.value.filter((item) => item.storeId === activeStoreId.value)
  );

  const filteredOrders = computed(() =>
    activeStoreId.value === 'all'
      ? orders.value
      : orders.value.filter((item) => item.storeId === activeStoreId.value)
  );

  const filteredFulfillmentTasks = computed(() =>
    activeStoreId.value === 'all'
      ? fulfillmentTasks.value
      : fulfillmentTasks.value.filter((item) => item.storeId === activeStoreId.value)
  );

  const filteredInventorySnapshots = computed(() =>
    activeStoreId.value === 'all'
      ? inventorySnapshots.value
      : inventorySnapshots.value.filter((item) => item.storeId === activeStoreId.value)
  );

  const filteredReplenishmentTasks = computed(() =>
    activeStoreId.value === 'all'
      ? replenishmentTasks.value
      : replenishmentTasks.value.filter((item) => item.storeId === activeStoreId.value)
  );

  const filteredCampaigns = computed(() =>
    activeStoreId.value === 'all'
      ? campaigns.value
      : campaigns.value.filter((item) => item.storeId === activeStoreId.value)
  );

  const topProductRecommendations = computed(() => sortByScore(productRecommendations.value).slice(0, 3));
  const topMemberRecommendations = computed(() => sortByScore(memberRecommendations.value).slice(0, 3));
  const topCampaignRecommendations = computed(() => sortByScore(campaignRecommendations.value).slice(0, 3));

  const blockedFulfillmentTasks = computed(() =>
    [...filteredFulfillmentTasks.value]
      .filter((item) => isOpenStatus(item.status) || Boolean(item.lastErrorMessage))
      .sort((left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt))
  );

  const topRiskExceptions = computed(() =>
    [...filteredExceptions.value].sort(
      (left, right) =>
        severityRank(right.severity) - severityRank(left.severity) ||
        toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const lowStockSnapshots = computed(() =>
    [...filteredInventorySnapshots.value]
      .filter((item) => item.availableStock <= item.safetyStock)
      .sort(
        (left, right) =>
          left.availableStock - right.availableStock ||
          left.safetyStock - right.safetyStock
      )
  );

  const priorityReplenishmentTasks = computed(() =>
    [...filteredReplenishmentTasks.value].sort(
      (left, right) =>
        right.suggestedQty - left.suggestedQty ||
        toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const overdueOrders = computed(() =>
    [...filteredOrders.value]
      .filter((item) => isPastDue(item.timeoutAt))
      .sort((left, right) => toTimestamp(left.timeoutAt) - toTimestamp(right.timeoutAt))
  );

  const atRiskOrders = computed(() => {
    const exceptionOrderIds = new Set(topRiskExceptions.value.map((item) => item.relatedId));
    const blockedOrderIds = new Set(blockedFulfillmentTasks.value.map((item) => item.orderId));

    return [...filteredOrders.value]
      .filter(
        (item) =>
          exceptionOrderIds.has(item.orderId) ||
          blockedOrderIds.has(item.orderId) ||
          isPastDue(item.timeoutAt)
      )
      .sort(
        (left, right) =>
          Number(isPastDue(right.timeoutAt)) - Number(isPastDue(left.timeoutAt)) ||
          (right.totalAmount ?? 0) - (left.totalAmount ?? 0) ||
          toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
      );
  });

  const productMap = computed(
    () => new Map(products.value.map((item) => [item.productId, item]))
  );

  const activeCampaignCount = computed(() =>
    filteredCampaigns.value.filter((item) =>
      ['created', 'pending_approval', 'published', 'running'].includes(normalizeText(item.status))
    ).length
  );

  const snapshot = computed<AssistantSnapshot>(() => ({
    recommendationCount:
      productRecommendations.value.length +
      memberRecommendations.value.length +
      campaignRecommendations.value.length,
    orderRiskCount: filteredExceptions.value.length,
    blockedFulfillmentCount: blockedFulfillmentTasks.value.length,
    lowStockCount: lowStockSnapshots.value.length,
    activeCampaignCount: activeCampaignCount.value,
    dormantMemberCount: memberAnalysis.value?.dormantMembers ?? 0
  }));

  const recommendedQuestions = computed(() => {
    const questions = new Set<string>(BASE_RECOMMENDED_QUESTIONS);

    if (snapshot.value.lowStockCount > 0) {
      questions.add('低库存商品会不会影响本周销量？');
    }

    if (snapshot.value.orderRiskCount > 0 || snapshot.value.blockedFulfillmentCount > 0) {
      questions.add('哪些订单今天一定要人工兜底？');
    }

    if (snapshot.value.dormantMemberCount > 0) {
      questions.add('沉默会员更适合召回还是做活动？');
    }

    if (snapshot.value.activeCampaignCount > 0) {
      questions.add('当前活动和商品主推要怎么配合？');
    }

    return Array.from(questions).slice(0, 8);
  });

  function currentStoreId() {
    return activeStoreId.value === 'all' ? undefined : activeStoreId.value;
  }

  function clearResponse() {
    currentResponse.value = null;
    errorMessage.value = null;
  }

  function productTitle(productId: string) {
    return productMap.value.get(productId)?.title ?? productId;
  }

  function buildMatchedSignals(
    resolution: IntentResolution,
    dataSignals: Array<string | null | undefined>
  ) {
    return uniqueStrings(
      [
        ...resolution.matchedSignals.map((keyword) => `关键词：${keyword}`),
        ...dataSignals
      ],
      6
    );
  }

  function buildConfidenceReason(
    resolution: IntentResolution,
    dataSummary: string
  ) {
    if (resolution.matchedSignals.length) {
      return `命中 ${resolution.matchedSignals.join('、')}，并且 ${dataSummary}。`;
    }

    return `未命中特定业务关键词，按经营综述回答，并且 ${dataSummary}。`;
  }

  function buildBaseResponse(
    intent: BusinessAssistantIntent,
    intentLabel: string,
    question: string
  ): Pick<
    BusinessAssistantResponse,
    'id' | 'intent' | 'intentLabel' | 'question' | 'storeName' | 'createdAt'
  > {
    return {
      id: buildHistoryId(),
      intent,
      intentLabel,
      question,
      storeName: selectedStoreName.value,
      createdAt: new Date().toISOString()
    };
  }

  function buildProductResponse(
    question: string,
    resolution: IntentResolution
  ): BusinessAssistantResponse {
    const topItems = topProductRecommendations.value;
    const confidence = resolveConfidence(resolution.matchedSignals.length, topItems.length);

    return {
      ...buildBaseResponse('product', resolution.intentLabel, question),
      title: topItems.length ? `今天先推 ${topItems[0].title}` : '今天先回看商品建议池',
      summary: topItems.length
        ? `当前最适合优先推进的是 ${topItems.map((item) => item.title).slice(0, 2).join('、')}，这批商品已经在推荐中心形成了明确的经营建议。`
        : '当前没有新的商品推荐，建议先回到商品中心检查候选池、草稿和发布节奏。',
      confidence,
      confidenceLabel: confidenceLabel(confidence),
      confidenceReason: buildConfidenceReason(
        resolution,
        `读取到 ${productRecommendations.value.length} 条商品推荐`
      ),
      coverageLabels: ['商品', '活动'],
      matchedSignals: buildMatchedSignals(resolution, [
        `商品推荐 ${productRecommendations.value.length} 条`,
        overview.value?.headline ? `概览：${overview.value.headline}` : null
      ]),
      evidence: [
        `当前范围：${selectedStoreName.value}`,
        `商品建议 ${productRecommendations.value.length} 条`,
        `会员建议 ${memberRecommendations.value.length} 条`,
        `活动建议 ${campaignRecommendations.value.length} 条`
      ],
      insights: topItems.length
        ? topItems.map((item) => {
            const profit =
              item.estimatedProfit == null
                ? '利润待估算'
                : `预计利润 ${formatCurrency(item.estimatedProfit)}`;
            const risk = item.riskLevel ? `风险 ${item.riskLevel}` : '风险待复核';
            return `${item.title}：${item.recommendationReason}；${profit}；${risk}。`;
          })
        : createFallbackInsights([
            '没有新商品建议时，优先回看商品映射、内容素材和上架节奏，避免前端有入口但后台没有可执行商品。'
          ]),
      followUpQuestions: [
        '这些商品里哪个应该先上架？',
        '高风险商品要不要先从计划里剔除？',
        '现在适合配什么活动一起推？'
      ],
      actions: [
        {
          label: '打开商品中心',
          route: '/app/product-center',
          description: '继续处理候选商品、草稿和发布动作。'
        },
        {
          label: '查看商品映射',
          route: '/app/product-mapping-center',
          description: '核对平台商品与货源映射，避免后续履约断链。'
        },
        {
          label: '进入活动中心',
          route: '/app/campaign-center',
          description: '把主推商品和活动计划联动起来。'
        }
      ],
      sourceLabels: ['推荐概览', '商品推荐']
    };
  }

  function buildMemberResponse(
    question: string,
    resolution: IntentResolution
  ): BusinessAssistantResponse {
    const topMembers = topMemberRecommendations.value;
    const dormantMembers = memberAnalysis.value?.dormantMembers ?? 0;
    const confidence = resolveConfidence(resolution.matchedSignals.length, topMembers.length + Number(dormantMembers > 0));

    return {
      ...buildBaseResponse('member', resolution.intentLabel, question),
      title: topMembers.length ? `先经营 ${topMembers[0].nickname}` : '先回看会员分群和召回机会',
      summary: topMembers.length
        ? `当前最值得先跟进的是 ${topMembers[0].nickname} 所在分群，同时还有 ${dormantMembers} 个沉默会员可以承接召回动作。`
        : `当前没有新的会员推荐，但沉默会员仍有 ${dormantMembers} 个，建议先回到会员 CRM 确认分群和召回任务。`,
      confidence,
      confidenceLabel: confidenceLabel(confidence),
      confidenceReason: buildConfidenceReason(
        resolution,
        `读取到 ${memberRecommendations.value.length} 条会员推荐和 ${dormantMembers} 个沉默会员`
      ),
      coverageLabels: ['会员', '活动'],
      matchedSignals: buildMatchedSignals(resolution, [
        `会员推荐 ${memberRecommendations.value.length} 条`,
        dormantMembers > 0 ? `沉默会员 ${dormantMembers} 个` : '沉默会员暂未积压'
      ]),
      evidence: [
        `会员推荐 ${memberRecommendations.value.length} 条`,
        `高价值会员 ${memberAnalysis.value?.highValueMembers ?? 0} 个`,
        `沉默会员 ${dormantMembers} 个`,
        `召回任务 ${memberAnalysis.value?.activeRecallTaskCount ?? 0} 个`
      ],
      insights: topMembers.length
        ? topMembers.map((item) => {
            const paidAmount =
              item.totalPaidAmount == null ? '累计支付待补齐' : `累计支付 ${formatCurrency(item.totalPaidAmount)}`;
            return `${item.nickname}：${item.recommendationReason}；分群 ${item.segmentCode}；${paidAmount}。`;
          })
        : createFallbackInsights([
            '没有会员级推荐时，优先回看沉默会员、高价值会员和召回任务，避免活动触达没有明确人群承接。'
          ]),
      followUpQuestions: [
        '沉默会员这周更适合召回还是做活动？',
        '哪些高价值会员值得人工跟进？',
        '会员分群和商品主推应该怎么配？'
      ],
      actions: [
        {
          label: '进入会员 CRM',
          route: '/app/member-crm-center',
          description: '继续处理会员分群、召回任务和客户跟进。'
        },
        {
          label: '查看活动中心',
          route: '/app/campaign-center',
          description: '把会员策略落到活动和触达动作。'
        }
      ],
      sourceLabels: ['会员推荐', '会员分析']
    };
  }

  function buildOrderResponse(
    question: string,
    resolution: IntentResolution
  ): BusinessAssistantResponse {
    const priorityOrders = atRiskOrders.value.length ? atRiskOrders.value : overdueOrders.value;
    const topOrders = priorityOrders.slice(0, 3);
    const confidence = resolveConfidence(
      resolution.matchedSignals.length,
      topOrders.length + Number(blockedFulfillmentTasks.value.length > 0)
    );

    return {
      ...buildBaseResponse('order', resolution.intentLabel, question),
      title: topOrders.length ? `先盯订单 ${topOrders[0].orderId}` : '当前订单盘面没有明显阻塞',
      summary: topOrders.length
        ? `当前最需要先处理的是 ${topOrders[0].orderId}，订单盘面里还有 ${blockedFulfillmentTasks.value.length} 个履约阻塞和 ${overdueOrders.value.length} 个超时订单。`
        : '订单盘面暂时没有读到明显阻塞，建议继续回看异常中心和履约任务，确认是否存在隐性积压。',
      confidence,
      confidenceLabel: confidenceLabel(confidence),
      confidenceReason: buildConfidenceReason(
        resolution,
        `读取到 ${filteredOrders.value.length} 笔订单、${blockedFulfillmentTasks.value.length} 个阻塞履约任务`
      ),
      coverageLabels: ['订单', '履约', '风险'],
      matchedSignals: buildMatchedSignals(resolution, [
        `订单 ${filteredOrders.value.length} 笔`,
        blockedFulfillmentTasks.value.length ? `阻塞履约 ${blockedFulfillmentTasks.value.length} 个` : null,
        overdueOrders.value.length ? `超时订单 ${overdueOrders.value.length} 笔` : null
      ]),
      evidence: [
        `订单总量 ${filteredOrders.value.length} 笔`,
        `超时订单 ${overdueOrders.value.length} 笔`,
        `阻塞履约 ${blockedFulfillmentTasks.value.length} 个`,
        `异常任务 ${filteredExceptions.value.length} 条`
      ],
      insights: topOrders.length
        ? topOrders.map((item) => {
            const timeoutCopy = item.timeoutAt
              ? `时限 ${formatDateTime(item.timeoutAt)}`
              : '时限待确认';
            const amountCopy =
              item.totalAmount == null ? '金额待补齐' : `金额 ${formatCurrency(item.totalAmount)}`;
            return `${item.orderId}：状态 ${item.orderStatus} / 物流 ${item.logisticsStatus}；${timeoutCopy}；${amountCopy}。`;
          })
        : createFallbackInsights([
            '如果没有读到异常订单，建议继续核对履约任务、店铺授权和异常中心，避免订单状态正常但后链路阻塞。'
          ]),
      followUpQuestions: [
        '哪些订单今天一定要人工兜底？',
        '超时订单和履约阻塞是否重叠？',
        '订单问题更像渠道配置还是履约执行问题？'
      ],
      actions: [
        {
          label: '进入订单履约中心',
          route: '/app/order-fulfillment-center',
          description: '继续检查订单、履约任务和物流链路。'
        },
        {
          label: '打开异常中心',
          route: '/app/exception-center',
          description: '把订单问题映射到异常优先级和 SOP。'
        }
      ],
      sourceLabels: ['订单主链路', '履约任务', '异常中心']
    };
  }

  function buildRiskResponse(
    question: string,
    resolution: IntentResolution
  ): BusinessAssistantResponse {
    const topRisks = topRiskExceptions.value.slice(0, 3);
    const confidence = resolveConfidence(resolution.matchedSignals.length, topRisks.length);

    return {
      ...buildBaseResponse('risk', resolution.intentLabel, question),
      title: topRisks.length ? '先处理订单与履约异常' : '当前没有读到明显订单风险',
      summary: topRisks.length
        ? `异常中心当前有 ${topRiskExceptions.value.length} 条与当前范围相关的异常，优先级最高的是 ${topRisks[0].exceptionType}。`
        : '异常中心暂未读到高优先级订单风险，建议继续回看履约任务和超时订单，确认是否存在未归档问题。',
      confidence,
      confidenceLabel: confidenceLabel(confidence),
      confidenceReason: buildConfidenceReason(
        resolution,
        `读取到 ${filteredExceptions.value.length} 条异常和 ${blockedFulfillmentTasks.value.length} 个阻塞履约任务`
      ),
      coverageLabels: ['风险', '订单', '履约'],
      matchedSignals: buildMatchedSignals(resolution, [
        `异常任务 ${filteredExceptions.value.length} 条`,
        blockedFulfillmentTasks.value.length ? `阻塞履约 ${blockedFulfillmentTasks.value.length} 个` : null
      ]),
      evidence: [
        `异常任务 ${filteredExceptions.value.length} 条`,
        `订单总量 ${filteredOrders.value.length} 笔`,
        `阻塞履约 ${blockedFulfillmentTasks.value.length} 个`
      ],
      insights: topRisks.length
        ? topRisks.map((item) => {
            const suggestion = item.suggestionText || '建议进入异常中心继续处理';
            return `${item.relatedId}：${item.exceptionType}；严重度 ${item.severity}；${suggestion}。`;
          })
        : createFallbackInsights([
            '没有异常记录不代表履约绝对安全，仍建议核对自动履约任务是否存在重试或待确认积压。'
          ]),
      followUpQuestions: [
        '哪些异常会直接影响今天发货？',
        '高严重度异常有没有重复出现？',
        '要不要先按异常优先级重新排履约队列？'
      ],
      actions: [
        {
          label: '进入异常中心',
          route: '/app/exception-center',
          description: '优先处理高严重度异常并执行 SOP。'
        },
        {
          label: '查看订单履约',
          route: '/app/order-fulfillment-center',
          description: '确认异常是否已经影响到履约编排。'
        }
      ],
      sourceLabels: ['异常中心', '订单主链路', '履约任务']
    };
  }

  function buildFulfillmentResponse(
    question: string,
    resolution: IntentResolution
  ): BusinessAssistantResponse {
    const latestTask = blockedFulfillmentTasks.value[0] ?? null;
    const relatedOrder = latestTask
      ? filteredOrders.value.find((item) => item.orderId === latestTask.orderId) ?? null
      : null;
    const confidence = resolveConfidence(
      resolution.matchedSignals.length,
      blockedFulfillmentTasks.value.length
    );

    return {
      ...buildBaseResponse('fulfillment', resolution.intentLabel, question),
      title: latestTask ? `自动发货先看订单 ${latestTask.orderId}` : '当前没有发现明显自动发货阻塞',
      summary: latestTask
        ? `最近有 ${blockedFulfillmentTasks.value.length} 个履约任务没有自动闭环，最值得先看的订单是 ${latestTask.orderId}。`
        : '当前履约任务里没有明显的失败或重试积压；如果仍感觉发货慢，建议继续核对店铺授权与默认发货配置。',
      confidence,
      confidenceLabel: confidenceLabel(confidence),
      confidenceReason: buildConfidenceReason(
        resolution,
        `读取到 ${filteredFulfillmentTasks.value.length} 个履约任务和 ${filteredExceptions.value.length} 条异常`
      ),
      coverageLabels: ['履约', '订单', '渠道'],
      matchedSignals: buildMatchedSignals(resolution, [
        `履约任务 ${filteredFulfillmentTasks.value.length} 个`,
        blockedFulfillmentTasks.value.length ? `阻塞任务 ${blockedFulfillmentTasks.value.length} 个` : null,
        latestTask?.lastErrorMessage ? `最近错误：${latestTask.lastErrorMessage}` : null
      ]),
      evidence: [
        `履约任务 ${filteredFulfillmentTasks.value.length} 个`,
        `阻塞任务 ${blockedFulfillmentTasks.value.length} 个`,
        `相关异常 ${filteredExceptions.value.length} 条`
      ],
      insights: blockedFulfillmentTasks.value.length
        ? blockedFulfillmentTasks.value.slice(0, 3).map((item) => {
            const detail = item.lastErrorMessage ? `原因 ${item.lastErrorMessage}` : '暂无明确错误文本';
            const orderCopy =
              item.orderId === relatedOrder?.orderId && relatedOrder?.timeoutAt
                ? `；订单时限 ${formatDateTime(relatedOrder.timeoutAt)}`
                : '';
            return `订单 ${item.orderId}：状态 ${item.status}；重试 ${item.retryCount ?? 0} 次；${detail}${orderCopy}。`;
          })
        : createFallbackInsights([
            '如果没有阻塞任务，优先检查店铺授权状态、默认发货配置和仓配规则是否已经写入。'
          ]),
      followUpQuestions: [
        '履约阻塞最像配置问题还是执行问题？',
        '哪些订单会因为履约阻塞在今天超时？',
        '店铺渠道配置要先检查哪一项？'
      ],
      actions: [
        {
          label: '进入订单履约中心',
          route: '/app/order-fulfillment-center',
          description: '查看履约任务、物流记录和异常重放。'
        },
        {
          label: '检查店铺渠道',
          route: '/app/store-channel-center',
          description: '确认店铺授权、发货配置和阈值没有失效。'
        }
      ],
      sourceLabels: ['履约任务', '异常中心']
    };
  }

  function buildInventoryResponse(
    question: string,
    resolution: IntentResolution
  ): BusinessAssistantResponse {
    const lowStockItems = lowStockSnapshots.value;
    const confidence = resolveConfidence(
      resolution.matchedSignals.length,
      lowStockItems.length + Number(priorityReplenishmentTasks.value.length > 0)
    );

    return {
      ...buildBaseResponse('inventory', resolution.intentLabel, question),
      title: lowStockItems.length ? '补货优先级已经出现' : '当前库存还没有压到安全线下方',
      summary: lowStockItems.length
        ? `当前有 ${lowStockItems.length} 个 SKU 已经压到安全库存线，建议优先补最先触线的商品。`
        : '库存快照还没有读到需要立刻补货的 SKU，可以先把精力放到活动或商品执行。',
      confidence,
      confidenceLabel: confidenceLabel(confidence),
      confidenceReason: buildConfidenceReason(
        resolution,
        `读取到 ${filteredInventorySnapshots.value.length} 条库存快照和 ${filteredReplenishmentTasks.value.length} 条补货任务`
      ),
      coverageLabels: ['库存', '商品', '采购'],
      matchedSignals: buildMatchedSignals(resolution, [
        `低库存 SKU ${lowStockItems.length} 个`,
        `补货任务 ${filteredReplenishmentTasks.value.length} 条`
      ]),
      evidence: [
        `低库存 SKU ${lowStockItems.length} 个`,
        `补货任务 ${filteredReplenishmentTasks.value.length} 条`,
        `库存快照 ${filteredInventorySnapshots.value.length} 条`
      ],
      insights: lowStockItems.length
        ? lowStockItems.slice(0, 3).map((item) => {
            return `${productTitle(item.productId)} / ${item.skuId}：可用 ${item.availableStock}；预留 ${item.reservedStock}；安全库存 ${item.safetyStock}。`;
          })
        : createFallbackInsights(
            priorityReplenishmentTasks.value.slice(0, 3).map((item) => {
              return `${productTitle(item.productId)}：已有补货任务，建议数量 ${item.suggestedQty}。`;
            })
          ),
      followUpQuestions: [
        '低库存商品会不会影响本周销量？',
        '补货优先级应该先看缺口还是销量？',
        '需要先联动供应商还是先调安全库存？'
      ],
      actions: [
        {
          label: '进入库存补货中心',
          route: '/app/inventory-replenishment-center',
          description: '继续处理补货建议、采购请求和 WMS 联动。'
        },
        {
          label: '查看供应商中心',
          route: '/app/supplier-center',
          description: '确认主备供应商和采购协同能否承接补货动作。'
        }
      ],
      sourceLabels: ['库存快照', '补货任务', '商品列表']
    };
  }

  function buildCampaignResponse(
    question: string,
    resolution: IntentResolution
  ): BusinessAssistantResponse {
    const topCampaign = topCampaignRecommendations.value[0] ?? null;
    const dormantMembers = memberAnalysis.value?.dormantMembers ?? 0;
    const confidence = resolveConfidence(
      resolution.matchedSignals.length,
      topCampaignRecommendations.value.length + Number(dormantMembers > 0)
    );

    return {
      ...buildBaseResponse('campaign', resolution.intentLabel, question),
      title: topCampaign ? '这周建议做活动' : '这周先谨慎开活动',
      summary: topCampaign
        ? `当前最适合的活动方向是 ${topCampaign.campaignName}，同时还有 ${dormantMembers} 个沉默会员可承接触达。`
        : `当前没有新的活动推荐；如果要做活动，建议先确认 ${dormantMembers} 个沉默会员是否值得优先唤醒。`,
      confidence,
      confidenceLabel: confidenceLabel(confidence),
      confidenceReason: buildConfidenceReason(
        resolution,
        `读取到 ${campaignRecommendations.value.length} 条活动推荐和 ${activeCampaignCount.value} 个进行中活动`
      ),
      coverageLabels: ['活动', '会员', '商品'],
      matchedSignals: buildMatchedSignals(resolution, [
        `活动推荐 ${campaignRecommendations.value.length} 条`,
        `进行中活动 ${activeCampaignCount.value} 个`,
        dormantMembers > 0 ? `沉默会员 ${dormantMembers} 个` : null
      ]),
      evidence: [
        `活动推荐 ${campaignRecommendations.value.length} 条`,
        `进行中活动 ${activeCampaignCount.value} 个`,
        `沉默会员 ${dormantMembers} 个`
      ],
      insights: topCampaign
        ? topCampaignRecommendations.value.map(
            (item) => `${item.campaignName}：${item.recommendationReason}；建议动作 ${item.suggestedAction}。`
          )
        : createFallbackInsights([
            '没有新活动建议时，优先回到会员 CRM 做分群和召回，再决定是否发起新活动。'
          ]),
      followUpQuestions: [
        '当前活动和商品主推应该怎么配？',
        '这周更适合拉新还是召回老客？',
        '沉默会员要不要直接承接到活动里？'
      ],
      actions: [
        {
          label: '进入活动中心',
          route: '/app/campaign-center',
          description: '继续处理活动创建、审批和发布动作。'
        },
        {
          label: '查看会员 CRM',
          route: '/app/member-crm-center',
          description: '确认会员分群、沉默用户和触达任务是否就绪。'
        }
      ],
      sourceLabels: ['活动推荐', '活动中心', '会员分析']
    };
  }

  function buildSummaryResponse(
    question: string,
    resolution: IntentResolution
  ): BusinessAssistantResponse {
    const topProduct = topProductRecommendations.value[0] ?? null;
    const topRisk = topRiskExceptions.value[0] ?? null;
    const topLowStock = lowStockSnapshots.value[0] ?? null;
    const topCampaign = topCampaignRecommendations.value[0] ?? null;
    const dormantMembers = memberAnalysis.value?.dormantMembers ?? 0;
    const crossDomainInsights = uniqueStrings(
      [
        topProduct ? `商品：${topProduct.title} 可作为今天优先推进的主推项。` : null,
        topRisk ? `风险：${topRisk.relatedId} 存在 ${topRisk.exceptionType}。` : null,
        blockedFulfillmentTasks.value[0]
          ? `履约：订单 ${blockedFulfillmentTasks.value[0].orderId} 仍在阻塞。`
          : null,
        topLowStock
          ? `库存：${productTitle(topLowStock.productId)} / ${topLowStock.skuId} 已压到安全库存线。`
          : null,
        topCampaign ? `活动：${topCampaign.campaignName} 当前最适合承接触达。` : null,
        dormantMembers > 0 ? `会员：当前还有 ${dormantMembers} 个沉默会员可以召回。` : null
      ],
      4
    );
    const confidence = resolveConfidence(
      resolution.matchedSignals.length,
      crossDomainInsights.length
    );

    return {
      ...buildBaseResponse('summary', resolution.intentLabel, question),
      title: '先从今天最值得做的经营动作开始',
      summary:
        overview.value?.headline ||
        '当前已经回读推荐概览、订单、异常、库存、活动和会员分析，可以直接从最优先动作开始推进。',
      confidence,
      confidenceLabel: confidenceLabel(confidence),
      confidenceReason: buildConfidenceReason(
        resolution,
        '已汇总商品、订单、库存、活动和会员五类经营信号'
      ),
      coverageLabels: ['商品', '订单', '库存', '活动', '会员'],
      matchedSignals: buildMatchedSignals(resolution, [
        `总建议量 ${snapshot.value.recommendationCount} 条`,
        snapshot.value.orderRiskCount ? `订单风险 ${snapshot.value.orderRiskCount} 条` : null,
        snapshot.value.lowStockCount ? `低库存 ${snapshot.value.lowStockCount} 个` : null,
        snapshot.value.activeCampaignCount ? `进行中活动 ${snapshot.value.activeCampaignCount} 个` : null
      ]),
      evidence: [
        `当前范围：${selectedStoreName.value}`,
        `商品/会员/活动建议 ${snapshot.value.recommendationCount} 条`,
        `订单风险 ${snapshot.value.orderRiskCount} 条`,
        `阻塞履约 ${snapshot.value.blockedFulfillmentCount} 个`,
        `低库存 ${snapshot.value.lowStockCount} 个`,
        `沉默会员 ${snapshot.value.dormantMemberCount} 个`
      ],
      insights: crossDomainInsights.length
        ? crossDomainInsights
        : createFallbackInsights([
            '当前没有明显的跨域预警，建议先回到经营分析页确认销售、利润和异常趋势，再决定优先动作。'
          ]),
      followUpQuestions: uniqueStrings(
        [
          topProduct ? '今天卖什么最稳妥？' : null,
          snapshot.value.orderRiskCount > 0 ? '哪些订单今天必须人工处理？' : null,
          snapshot.value.lowStockCount > 0 ? '哪些商品应该优先补货？' : null,
          snapshot.value.activeCampaignCount > 0 ? '这周要不要做活动？' : null,
          snapshot.value.dormantMemberCount > 0 ? '有哪些沉默会员值得召回？' : null
        ],
        4
      ),
      actions: [
        {
          label: '查看经营分析',
          route: '/app/business-analytics',
          description: '先把销售、利润和异常趋势看清。'
        },
        {
          label: '进入订单履约中心',
          route: '/app/order-fulfillment-center',
          description: '优先处理会影响当天履约的订单问题。'
        },
        {
          label: '进入商品中心',
          route: '/app/product-center',
          description: '把最值得转成经营动作的商品建议立即执行。'
        }
      ],
      sourceLabels: ['推荐概览', '订单主链路', '库存快照', '活动推荐', '会员分析']
    };
  }

  function buildResponse(
    resolution: IntentResolution,
    question: string
  ): BusinessAssistantResponse {
    if (resolution.intent === 'product') {
      return buildProductResponse(question, resolution);
    }

    if (resolution.intent === 'member') {
      return buildMemberResponse(question, resolution);
    }

    if (resolution.intent === 'order') {
      return buildOrderResponse(question, resolution);
    }

    if (resolution.intent === 'risk') {
      return buildRiskResponse(question, resolution);
    }

    if (resolution.intent === 'fulfillment') {
      return buildFulfillmentResponse(question, resolution);
    }

    if (resolution.intent === 'inventory') {
      return buildInventoryResponse(question, resolution);
    }

    if (resolution.intent === 'campaign') {
      return buildCampaignResponse(question, resolution);
    }

    return buildSummaryResponse(question, resolution);
  }

  async function refreshCore(token: string) {
    isRefreshingCore.value = true;
    errorMessage.value = null;

    try {
      const storesPayload = await getStores(token);
      stores.value = storesPayload;

      if (
        activeStoreId.value !== 'all' &&
        !storesPayload.some((store) => store.storeId === activeStoreId.value)
      ) {
        activeStoreId.value = storesPayload[0]?.storeId ?? 'all';
      }

      if (activeStoreId.value === 'all' && storesPayload.length === 1) {
        activeStoreId.value = storesPayload[0].storeId;
      }

      const storeId = currentStoreId();
      const [
        overviewPayload,
        productsPayload,
        membersPayload,
        campaignsPayload,
        memberAnalysisPayload
      ] = await Promise.all([
        getRecommendationOverview(storeId, token),
        getProductRecommendations({ storeId, limit: 6 }, token),
        getMemberRecommendations({ storeId, limit: 6 }, token),
        getCampaignRecommendations({ storeId, limit: 6 }, token),
        getMemberCrmAnalysis(storeId, token)
      ]);

      overview.value = overviewPayload;
      productRecommendations.value = productsPayload;
      memberRecommendations.value = membersPayload;
      campaignRecommendations.value = campaignsPayload;
      memberAnalysis.value = memberAnalysisPayload;
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '经营助手上下文加载失败，请稍后重试';
      throw error;
    } finally {
      isRefreshingCore.value = false;
    }
  }

  async function ensureOperations(token: string) {
    if (operationsLoaded.value) {
      return;
    }

    const [exceptionsPayload, ordersPayload, fulfillmentPayload] = await Promise.all([
      getExceptions(token),
      getOrders(token),
      getFulfillmentTasks(token)
    ]);

    exceptions.value = exceptionsPayload;
    orders.value = ordersPayload;
    fulfillmentTasks.value = fulfillmentPayload;
    operationsLoaded.value = true;
  }

  async function ensureInventory(token: string) {
    if (inventoryLoaded.value) {
      return;
    }

    const [snapshotsPayload, replenishmentPayload, productsPayload] = await Promise.all([
      getInventorySnapshots(token),
      getReplenishmentTasks(token),
      getProducts(token)
    ]);

    inventorySnapshots.value = snapshotsPayload;
    replenishmentTasks.value = replenishmentPayload;
    products.value = productsPayload;
    inventoryLoaded.value = true;
  }

  async function ensureCampaigns(token: string) {
    if (campaignsLoaded.value) {
      return;
    }

    campaigns.value = await getCampaigns(token);
    campaignsLoaded.value = true;
  }

  async function ensureIntentData(intent: BusinessAssistantIntent, token: string) {
    if (intent === 'summary') {
      await Promise.all([ensureOperations(token), ensureInventory(token), ensureCampaigns(token)]);
      return;
    }

    if (intent === 'risk' || intent === 'fulfillment' || intent === 'order') {
      await ensureOperations(token);
      return;
    }

    if (intent === 'inventory') {
      await ensureInventory(token);
      return;
    }

    if (intent === 'campaign') {
      await ensureCampaigns(token);
    }
  }

  async function bootstrap(token: string) {
    isBootstrapping.value = true;

    try {
      await refreshCore(token);
    } finally {
      isBootstrapping.value = false;
    }
  }

  async function selectStore(storeId: string, token: string) {
    activeStoreId.value = storeId;
    clearResponse();
    await refreshCore(token);
  }

  function updateDraftQuestion(value: string) {
    draftQuestion.value = value;
  }

  async function askQuestion(question: string, token: string) {
    const normalized = question.trim();

    if (!normalized) {
      return;
    }

    isAnswering.value = true;
    errorMessage.value = null;
    draftQuestion.value = normalized;

    try {
      const response = await askBusinessAssistant(
        {
          storeId: currentStoreId(),
          question: normalized
        },
        token
      );
      currentResponse.value = response;
      history.value = [response, ...history.value].slice(0, 10);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '生成经营建议失败，请稍后重试';
      throw error;
    } finally {
      isAnswering.value = false;
    }
  }

  function replayHistory(question: string) {
    draftQuestion.value = question;
  }

  return {
    stores: sortedStores,
    overview,
    snapshot,
    selectedStoreName,
    activeStoreId,
    draftQuestion,
    currentResponse,
    history,
    recommendedQuestions,
    isBootstrapping,
    isRefreshingCore,
    isAnswering,
    errorMessage,
    bootstrap,
    selectStore,
    updateDraftQuestion,
    askQuestion,
    replayHistory,
    formatDateTime
  };
}
