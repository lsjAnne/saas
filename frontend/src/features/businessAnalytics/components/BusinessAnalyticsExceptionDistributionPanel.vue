<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  BusinessAnalyticsExceptionDistributionItem
} from '@/features/businessAnalytics/composables/useBusinessAnalyticsOverview';
import type { DashboardRisk } from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface Props {
  items: BusinessAnalyticsExceptionDistributionItem[];
  risks: DashboardRisk[];
}

defineProps<Props>();
</script>

<template>
  <PanelCard
    eyebrow="Exception Distribution"
    title="异常分布"
    description="先看哪一类异常最集中，再看是否已经出现高风险堆积。"
  >
    <div v-if="items.length" class="exception-distribution">
      <article v-for="item in items" :key="item.exceptionType" class="exception-distribution__card">
        <div class="exception-distribution__row">
          <div>
            <p class="exception-distribution__type">{{ item.label }}</p>
            <h4 class="exception-distribution__count">{{ formatCount(item.totalCount, ' 个') }}</h4>
          </div>
          <StatusPill :label="`${formatCount(item.highSeverityCount, ' 高危')}`" :tone="item.tone" />
        </div>

        <div class="exception-distribution__meta">
          <span>待处理 {{ formatCount(item.pendingCount, ' 个') }}</span>
          <span>{{ item.topSuggestion || '优先回看异常原因与处理建议。' }}</span>
        </div>
      </article>
    </div>
    <p v-else class="exception-distribution__empty">当前筛选范围内没有异常任务。</p>

    <div v-if="risks.length" class="exception-distribution__risk-strip">
      <article v-for="risk in risks" :key="risk.riskCode" class="exception-distribution__risk-card">
        <strong>{{ risk.riskName }}</strong>
        <span>{{ formatCount(risk.riskCount, ' 次') }}</span>
        <p>{{ risk.suggestion }}</p>
      </article>
    </div>
  </PanelCard>
</template>

<style scoped>
.exception-distribution,
.exception-distribution__card,
.exception-distribution__risk-strip,
.exception-distribution__risk-card {
  display: grid;
  gap: 0.8rem;
}

.exception-distribution__card,
.exception-distribution__risk-card {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.exception-distribution__row {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.exception-distribution__type,
.exception-distribution__count,
.exception-distribution__meta span,
.exception-distribution__risk-card strong,
.exception-distribution__risk-card span,
.exception-distribution__risk-card p,
.exception-distribution__empty {
  margin: 0;
}

.exception-distribution__type {
  color: var(--color-ink-faint);
  font-size: 0.74rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.exception-distribution__count,
.exception-distribution__risk-card strong {
  color: var(--color-ink-strong);
}

.exception-distribution__meta,
.exception-distribution__risk-card span,
.exception-distribution__risk-card p,
.exception-distribution__empty {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

@media (max-width: 760px) {
  .exception-distribution__row {
    flex-direction: column;
  }
}
</style>
