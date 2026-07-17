import { computed, reactive, shallowRef } from 'vue';

import { getDashboardCampaignAnalysis, getDashboardMemberAnalysis, getDashboardRisks, getDashboardTrends } from '@/services/dashboardService';
import { getExceptions } from '@/services/exceptionCenterService';
import { getProfitStatement, getStoreProfitReports } from '@/services/financeSettlementCenterService';
import { getProducts } from '@/services/productMappingCenterService';
import { getStores } from '@/services/storeChannelService';
import type {
  DashboardCampaignAnalysis,
  DashboardMemberAnalysis,
  DashboardRisk,
  DashboardTrend,
  ExceptionTask,
  Product,
  ProfitStatementView,
  Store,
  StoreProfitReportView
} from '@/services/apiTypes';

type Tone = 'neutral' | 'warn' | 'success';

export interface StoreOption {
  value: string;
  label: string;
}

export interface BusinessAnalyticsProductRankingItem extends Product {
  rank: number;
  storeName: string;
  statusLabel: string;
  statusTone: Tone;
  healthLabel: string;
}

export interface BusinessAnalyticsExceptionDistributionItem {
  exceptionType: string;
  label: string;
  totalCount: number;
  pendingCount: number;
  highSeverityCount: number;
  topSuggestion: string;
  tone: Tone;
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function pad(value: number) {
  return String(value).padStart(2, '0');
}

function formatDateInput(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function createCurrentPeriod() {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1);

  return {
    periodStart: formatDateInput(start),
    periodEnd: formatDateInput(now)
  };
}

function sortProducts(items: Product[]) {
  return [...items].sort((left, right) => {
    const leftStatus = normalizeText(left.status);
    const rightStatus = normalizeText(right.status);
    const leftPublished = leftStatus === 'active' || leftStatus === 'published';
    const rightPublished = rightStatus === 'active' || rightStatus === 'published';
    if (leftPublished !== rightPublished) {
      return Number(rightPublished) - Number(leftPublished);
    }

    const healthGap = Number(right.healthScore ?? -1) - Number(left.healthScore ?? -1);
    if (healthGap !== 0) {
      return healthGap;
    }

    return (
      toTimestamp(right.publishedAt ?? right.createdAt) -
      toTimestamp(left.publishedAt ?? left.createdAt)
    );
  });
}

function resolveProductStatus(status: string | null | undefined): { label: string; tone: Tone } {
  const normalized = normalizeText(status);

  if (normalized === 'active' || normalized === 'published') {
    return {
      label: '已发布',
      tone: 'success'
    };
  }

  if (normalized) {
    return {
      label: '待优化',
      tone: 'warn'
    };
  }

  return {
    label: '未知',
    tone: 'neutral'
  };
}

function resolveExceptionLabel(exceptionType: string) {
  const normalized = normalizeText(exceptionType);
  return normalized || 'unknown';
}

function resolveSeverityWeight(severity: string | null | undefined) {
  const normalized = normalizeText(severity);
  if (normalized === 'high' || normalized === 'critical') {
    return 3;
  }

  if (normalized === 'medium') {
    return 2;
  }

  if (normalized) {
    return 1;
  }

  return 0;
}

export function useBusinessAnalyticsOverview() {
  const stores = shallowRef<Store[]>([]);
  const trends = shallowRef<DashboardTrend[]>([]);
  const risks = shallowRef<DashboardRisk[]>([]);
  const campaignAnalysis = shallowRef<DashboardCampaignAnalysis | null>(null);
  const memberAnalysis = shallowRef<DashboardMemberAnalysis | null>(null);
  const products = shallowRef<Product[]>([]);
  const exceptions = shallowRef<ExceptionTask[]>([]);
  const storeProfitReports = shallowRef<StoreProfitReportView[]>([]);
  const selectedStoreProfit = shallowRef<ProfitStatementView | null>(null);
  const activeStoreId = shallowRef('all');
  const analysisPeriod = reactive(createCurrentPeriod());
  const sessionToken = shallowRef<string | null>(null);
  const isLoading = shallowRef(false);
  const isRefreshingPeriod = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);

  const storeMap = computed(() => new Map(stores.value.map((store) => [store.storeId, store])));

  const storeOptions = computed<StoreOption[]>(() => [
    {
      value: 'all',
      label: '全部店铺'
    },
    ...stores.value
      .slice()
      .sort((left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt))
      .map((store) => ({
        value: store.storeId,
        label: store.shopName
      }))
  ]);

  const selectedStoreName = computed(() => {
    if (activeStoreId.value === 'all') {
      return '全部店铺';
    }

    return storeMap.value.get(activeStoreId.value)?.shopName ?? activeStoreId.value;
  });

  const filteredProducts = computed(() =>
    activeStoreId.value === 'all'
      ? products.value
      : products.value.filter((product) => product.storeId === activeStoreId.value)
  );

  const filteredExceptions = computed(() =>
    activeStoreId.value === 'all'
      ? exceptions.value
      : exceptions.value.filter((item) => item.storeId === activeStoreId.value)
  );

  const aggregateProfitSnapshot = computed(() => {
    if (selectedStoreProfit.value && activeStoreId.value !== 'all') {
      return {
        salesIncomeAmount: selectedStoreProfit.value.salesIncomeAmount,
        netProfitAmount: selectedStoreProfit.value.netProfitAmount,
        profitMarginRate: selectedStoreProfit.value.profitMarginRate,
        outstandingReceivableAmount: selectedStoreProfit.value.outstandingReceivableAmount
      };
    }

    const totalSales = storeProfitReports.value.reduce(
      (sum, item) => sum + Number(item.salesIncomeAmount ?? 0),
      0
    );
    const totalNetProfit = storeProfitReports.value.reduce(
      (sum, item) => sum + Number(item.netProfitAmount ?? 0),
      0
    );
    const totalOutstandingReceivable = storeProfitReports.value.reduce(
      (sum, item) => sum + Number(item.outstandingReceivableAmount ?? 0),
      0
    );

    return {
      salesIncomeAmount: totalSales,
      netProfitAmount: totalNetProfit,
      profitMarginRate: totalSales > 0 ? totalNetProfit / totalSales : 0,
      outstandingReceivableAmount: totalOutstandingReceivable
    };
  });

  const salesTrendSummary = computed(() => {
    const totalSales = trends.value.reduce((sum, item) => sum + Number(item.salesAmount ?? 0), 0);
    const averageOrders =
      trends.value.length > 0
        ? trends.value.reduce((sum, item) => sum + Number(item.orderCount ?? 0), 0) /
          trends.value.length
        : 0;
    return {
      totalSales,
      averageOrders
    };
  });

  const grossProfitTrendSummary = computed(() => {
    const totalGrossProfit = trends.value.reduce(
      (sum, item) => sum + Number(item.grossProfit ?? 0),
      0
    );

    return {
      totalGrossProfit,
      averageMargin:
        salesTrendSummary.value.totalSales > 0
          ? totalGrossProfit / salesTrendSummary.value.totalSales
          : 0
    };
  });

  const productRanking = computed<BusinessAnalyticsProductRankingItem[]>(() =>
    sortProducts(filteredProducts.value)
      .slice(0, 6)
      .map((product, index) => {
        const status = resolveProductStatus(product.status);
        return {
          ...product,
          rank: index + 1,
          storeName: storeMap.value.get(product.storeId)?.shopName ?? product.storeId,
          statusLabel: status.label,
          statusTone: status.tone,
          healthLabel: product.healthScore == null ? '--' : `${product.healthScore}`
        };
      })
  );

  const exceptionDistribution = computed<BusinessAnalyticsExceptionDistributionItem[]>(() => {
    const grouped = new Map<
      string,
      {
        exceptionType: string;
        label: string;
        totalCount: number;
        pendingCount: number;
        highSeverityCount: number;
        topSuggestion: string;
        tone: Tone;
      }
    >();

    for (const item of filteredExceptions.value) {
      const key = resolveExceptionLabel(item.exceptionType);
      const current = grouped.get(key) ?? {
        exceptionType: item.exceptionType,
        label: item.exceptionType,
        totalCount: 0,
        pendingCount: 0,
        highSeverityCount: 0,
        topSuggestion: item.suggestionText,
        tone: 'neutral' as Tone
      };

      current.totalCount += 1;
      if (['pending', 'processing', 'open'].includes(normalizeText(item.status))) {
        current.pendingCount += 1;
      }
      if (resolveSeverityWeight(item.severity) >= 3) {
        current.highSeverityCount += 1;
      }
      if (!current.topSuggestion && item.suggestionText) {
        current.topSuggestion = item.suggestionText;
      }
      current.tone =
        current.highSeverityCount > 0
          ? 'warn'
          : current.pendingCount > 0
            ? 'success'
            : 'neutral';

      grouped.set(key, current);
    }

    return [...grouped.values()]
      .sort((left, right) => {
        if (left.highSeverityCount !== right.highSeverityCount) {
          return right.highSeverityCount - left.highSeverityCount;
        }
        if (left.pendingCount !== right.pendingCount) {
          return right.pendingCount - left.pendingCount;
        }
        return right.totalCount - left.totalCount;
      })
      .slice(0, 6);
  });

  const topRiskSignals = computed(() => risks.value.slice(0, 4));

  const heroSummary = computed(() => ({
    salesIncomeAmount: aggregateProfitSnapshot.value.salesIncomeAmount,
    netProfitAmount: aggregateProfitSnapshot.value.netProfitAmount,
    profitMarginRate: aggregateProfitSnapshot.value.profitMarginRate,
    outstandingReceivableAmount: aggregateProfitSnapshot.value.outstandingReceivableAmount,
    productCount: filteredProducts.value.length,
    exceptionCount: filteredExceptions.value.length,
    riskHeadline:
      topRiskSignals.value[0]?.suggestion ??
      campaignAnalysis.value?.recommendedStrategy ??
      '优先清理高风险异常，再看活动和复购动作。'
  }));

  function requireSessionToken() {
    if (!sessionToken.value) {
      throw new Error('当前登录状态已失效，请重新登录后重试。');
    }

    return sessionToken.value;
  }

  async function refreshProfitSnapshot(token: string) {
    const reportsPromise = getStoreProfitReports(
      analysisPeriod.periodStart,
      analysisPeriod.periodEnd,
      token
    );
    const storeProfitPromise =
      activeStoreId.value === 'all'
        ? Promise.resolve(null)
        : getProfitStatement(
            activeStoreId.value,
            analysisPeriod.periodStart,
            analysisPeriod.periodEnd,
            token
          );

    const [reportsPayload, profitPayload] = await Promise.all([
      reportsPromise,
      storeProfitPromise
    ]);

    storeProfitReports.value = reportsPayload;
    selectedStoreProfit.value = profitPayload;
  }

  async function load(token: string) {
    sessionToken.value = token;
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [
        storesPayload,
        trendsPayload,
        risksPayload,
        campaignPayload,
        memberPayload,
        productsPayload,
        exceptionsPayload
      ] = await Promise.all([
        getStores(token),
        getDashboardTrends(token),
        getDashboardRisks(token),
        getDashboardCampaignAnalysis(token),
        getDashboardMemberAnalysis(token),
        getProducts(token),
        getExceptions(token)
      ]);

      stores.value = storesPayload;
      trends.value = trendsPayload;
      risks.value = risksPayload;
      campaignAnalysis.value = campaignPayload;
      memberAnalysis.value = memberPayload;
      products.value = productsPayload;
      exceptions.value = exceptionsPayload;

      if (
        activeStoreId.value !== 'all' &&
        !storesPayload.some((store) => store.storeId === activeStoreId.value)
      ) {
        activeStoreId.value = 'all';
      }

      await refreshProfitSnapshot(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '经营分析数据加载失败，请稍后重试';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  async function selectStore(storeId: string) {
    activeStoreId.value = storeId;

    const token = requireSessionToken();
    isRefreshingPeriod.value = true;
    errorMessage.value = null;

    try {
      await refreshProfitSnapshot(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '切换店铺失败，请稍后重试';
      throw error;
    } finally {
      isRefreshingPeriod.value = false;
    }
  }

  async function updatePeriod(payload: { periodStart: string; periodEnd: string }) {
    analysisPeriod.periodStart = payload.periodStart;
    analysisPeriod.periodEnd = payload.periodEnd;

    const token = requireSessionToken();
    isRefreshingPeriod.value = true;
    errorMessage.value = null;

    try {
      await refreshProfitSnapshot(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '刷新经营分析窗口失败，请稍后重试';
      throw error;
    } finally {
      isRefreshingPeriod.value = false;
    }
  }

  return {
    trends,
    campaignAnalysis,
    memberAnalysis,
    productRanking,
    exceptionDistribution,
    topRiskSignals,
    storeOptions,
    activeStoreId,
    selectedStoreName,
    analysisPeriod,
    heroSummary,
    salesTrendSummary,
    grossProfitTrendSummary,
    isLoading,
    isRefreshingPeriod,
    errorMessage,
    load,
    selectStore,
    updatePeriod
  };
}
