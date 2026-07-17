<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import BusinessAnalyticsCampaignEffectPanel from '@/features/businessAnalytics/components/BusinessAnalyticsCampaignEffectPanel.vue';
import BusinessAnalyticsExceptionDistributionPanel from '@/features/businessAnalytics/components/BusinessAnalyticsExceptionDistributionPanel.vue';
import BusinessAnalyticsGrossProfitTrendPanel from '@/features/businessAnalytics/components/BusinessAnalyticsGrossProfitTrendPanel.vue';
import BusinessAnalyticsHeroPanel from '@/features/businessAnalytics/components/BusinessAnalyticsHeroPanel.vue';
import BusinessAnalyticsMemberRepurchasePanel from '@/features/businessAnalytics/components/BusinessAnalyticsMemberRepurchasePanel.vue';
import BusinessAnalyticsProductRankingPanel from '@/features/businessAnalytics/components/BusinessAnalyticsProductRankingPanel.vue';
import BusinessAnalyticsSalesTrendPanel from '@/features/businessAnalytics/components/BusinessAnalyticsSalesTrendPanel.vue';
import { useBusinessAnalyticsOverview } from '@/features/businessAnalytics/composables/useBusinessAnalyticsOverview';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const analyticsOverview = useBusinessAnalyticsOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await analyticsOverview.load(authStore.token);
}

async function handleSelectStore(storeId: string) {
  await analyticsOverview.selectStore(storeId);
}

async function handleUpdatePeriod(payload: { periodStart: string; periodEnd: string }) {
  await analyticsOverview.updatePeriod(payload);
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="business-analytics-page">
    <header class="business-analytics-page__header">
      <p class="business-analytics-page__eyebrow">Business Analytics</p>
      <h2 class="business-analytics-page__title">
        让销售趋势、毛利趋势、商品排行、异常分布、活动效果和会员复购在同一页互相解释
      </h2>
      <p class="business-analytics-page__subtitle">
        这页严格对应需求原型里的经营分析页，不再把分析能力拆散在首页、财务和活动页面里。
      </p>
    </header>

    <InlineErrorCard
      v-if="analyticsOverview.errorMessage"
      :message="analyticsOverview.errorMessage"
      @retry="bootstrapPage"
    />

    <BusinessAnalyticsHeroPanel
      :store-options="analyticsOverview.storeOptions"
      :active-store-id="analyticsOverview.activeStoreId"
      :selected-store-name="analyticsOverview.selectedStoreName"
      :analysis-period-start="analyticsOverview.analysisPeriod.periodStart"
      :analysis-period-end="analyticsOverview.analysisPeriod.periodEnd"
      :sales-income-amount="analyticsOverview.heroSummary.salesIncomeAmount"
      :net-profit-amount="analyticsOverview.heroSummary.netProfitAmount"
      :profit-margin-rate="analyticsOverview.heroSummary.profitMarginRate"
      :outstanding-receivable-amount="analyticsOverview.heroSummary.outstandingReceivableAmount"
      :product-count="analyticsOverview.heroSummary.productCount"
      :exception-count="analyticsOverview.heroSummary.exceptionCount"
      :risk-headline="analyticsOverview.heroSummary.riskHeadline"
      :is-refreshing="analyticsOverview.isRefreshingPeriod"
      @select-store="handleSelectStore"
      @update-period="handleUpdatePeriod"
    />

    <div class="business-analytics-page__grid">
      <BusinessAnalyticsSalesTrendPanel
        :trends="analyticsOverview.trends"
        :total-sales="analyticsOverview.salesTrendSummary.totalSales"
        :average-orders="analyticsOverview.salesTrendSummary.averageOrders"
      />
      <BusinessAnalyticsGrossProfitTrendPanel
        :trends="analyticsOverview.trends"
        :total-gross-profit="analyticsOverview.grossProfitTrendSummary.totalGrossProfit"
        :average-margin="analyticsOverview.grossProfitTrendSummary.averageMargin"
      />
      <BusinessAnalyticsProductRankingPanel :items="analyticsOverview.productRanking" />
      <BusinessAnalyticsExceptionDistributionPanel
        :items="analyticsOverview.exceptionDistribution"
        :risks="analyticsOverview.topRiskSignals"
      />
      <BusinessAnalyticsCampaignEffectPanel
        :campaign-analysis="analyticsOverview.campaignAnalysis"
      />
      <BusinessAnalyticsMemberRepurchasePanel
        :member-analysis="analyticsOverview.memberAnalysis"
      />
    </div>

    <p
      v-if="analyticsOverview.isLoading || analyticsOverview.isRefreshingPeriod"
      class="business-analytics-page__footer-note"
    >
      正在回读经营趋势、利润窗口、商品健康度和异常分布...
    </p>
  </section>
</template>

<style scoped>
.business-analytics-page {
  display: grid;
  gap: 1.2rem;
}

.business-analytics-page__header {
  display: grid;
  gap: 0.75rem;
}

.business-analytics-page__eyebrow,
.business-analytics-page__title,
.business-analytics-page__subtitle,
.business-analytics-page__footer-note {
  margin: 0;
}

.business-analytics-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.business-analytics-page__title {
  max-width: 16ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 3.8rem);
  line-height: 1.04;
}

.business-analytics-page__subtitle,
.business-analytics-page__footer-note {
  max-width: 66rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.business-analytics-page__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  align-items: start;
}

@media (max-width: 1180px) {
  .business-analytics-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
