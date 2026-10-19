<script setup lang="ts">
import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  AdminTenantOverview,
  SubscriptionAutomationSummary
} from '@/services/apiTypes';
import { formatCount, formatCurrency, formatDateTime, formatPercent } from '@/utils/formatters';

interface Summary {
  totalTenantCount: number;
  activeTenantCount: number;
  payingTenantCount: number;
  trialTenantCount: number;
  suspendedTenantCount: number;
  overdueTenantCount: number;
  newTenantCount30d: number;
  estimatedMonthlyRevenue: number;
  atRiskTenantCount: number;
  totalSeatCount: number;
  quotaUsageRate: number;
}

interface GrowthTrendPoint {
  label: string;
  newTenantCount: number;
  payingTenantCount: number;
}

interface TrialConversionSummary {
  trialTenantCount: number;
  convertedTenantCount: number;
  expiringSoonCount: number;
  conversionRate: number;
}

interface Props {
  summary: Summary;
  growthTrend: GrowthTrendPoint[];
  trialConversion: TrialConversionSummary;
  selectedTenant: AdminTenantOverview | null;
  subscriptionAutomationSummary: SubscriptionAutomationSummary | null;
  actionFeedback?: string | null;
}

withDefaults(defineProps<Props>(), {
  actionFeedback: null
});

function tenantStatusLabel(status?: string | null) {
  const map: Record<string, string> = {
    active: '正常',
    suspended: '已停用',
    trial: '试用',
    expiring: '临期'
  };
  return (status && map[status]) || '待命中';
}

function subscriptionStatusLabel(status?: string | null) {
  const map: Record<string, string> = {
    active: '生效',
    unsubscribed: '未订阅',
    trial: '试用',
    past_due: '已逾期'
  };
  return (status && map[status]) || '--';
}

const monthLabelMap: Record<string, string> = {
  Jan: '1月',
  Feb: '2月',
  Mar: '3月',
  Apr: '4月',
  May: '5月',
  Jun: '6月',
  Jul: '7月',
  Aug: '8月',
  Sep: '9月',
  Oct: '10月',
  Nov: '11月',
  Dec: '12月'
};
</script>

<template>
  <PanelCard
    title="经营健康与转化概览"
    description="商业指标、增长与转化信号一览，治理细节置于下方。"
  >
    <div class="admin-hero">
      <div class="admin-hero__copy">
        <p class="admin-hero__eyebrow">当前关注租户</p>
        <h4 class="admin-hero__title">{{ selectedTenant?.tenantName || '请选择租户查看' }}</h4>
        <p class="admin-hero__summary">
          当前组合共 {{ summary.totalTenantCount }} 个租户，其中付费 {{ summary.payingTenantCount }}
          个、试用 {{ summary.trialTenantCount }} 个、风险 {{ summary.atRiskTenantCount }} 个。
        </p>
        <p v-if="actionFeedback" class="admin-hero__feedback">{{ actionFeedback }}</p>
      </div>

      <div class="admin-hero__focus">
        <div class="admin-hero__focus-row">
          <div>
            <p class="admin-hero__eyebrow">自动化执行</p>
            <h5 class="admin-hero__focus-title">
              {{ formatDateTime(subscriptionAutomationSummary?.executedAt) }}
            </h5>
          </div>
          <StatusPill
            :label="tenantStatusLabel(selectedTenant?.tenantStatus)"
            :tone="selectedTenant?.tenantStatus === 'active' ? 'success' : 'warn'"
          />
        </div>
        <p class="admin-hero__focus-text">
          当前租户：{{ selectedTenant?.planName || '--' }} / {{ subscriptionStatusLabel(selectedTenant?.subscriptionStatus) }}
          / {{ selectedTenant?.seatCount ?? '--' }} 席
        </p>
        <p class="admin-hero__focus-text">
          上次自动化执行：自动续费 {{ subscriptionAutomationSummary?.autoRenewedCount ?? 0 }} 个、
          自动停用 {{ subscriptionAutomationSummary?.autoSuspendedCount ?? 0 }} 个、扣费失败
          {{ subscriptionAutomationSummary?.autoChargeFailedCount ?? 0 }} 个。
        </p>
      </div>
    </div>

    <div class="admin-hero__metrics">
      <MetricCard label="租户数" :value="formatCount(summary.totalTenantCount)" caption="当前组合整体规模" />
      <MetricCard label="付费租户" :value="formatCount(summary.payingTenantCount)" caption="已订阅的租户数" />
      <MetricCard label="近30天新增" :value="formatCount(summary.newTenantCount30d)" caption="最近 30 天新开租户" />
      <MetricCard label="预计月收入" :value="formatCurrency(summary.estimatedMonthlyRevenue)" caption="经常性基础收入预估" />
      <MetricCard label="试用转化率" :value="formatPercent(trialConversion.conversionRate, 0)" caption="当前转化快照" />
      <MetricCard label="风险租户" :value="formatCount(summary.atRiskTenantCount)" caption="逾期、停用、临期或容量紧张" />
    </div>

    <div class="admin-hero__business-grid">
      <div class="admin-hero__business-card">
        <div class="admin-hero__focus-row">
          <div>
            <p class="admin-hero__eyebrow">增长趋势</p>
            <h5 class="admin-hero__focus-title">近期租户创建</h5>
          </div>
          <StatusPill label="近 6 个月" tone="neutral" />
        </div>
        <div class="admin-hero__trend-list">
          <article v-for="point in growthTrend" :key="point.label" class="admin-hero__trend-item">
            <div class="admin-hero__focus-row">
              <strong>{{ monthLabelMap[point.label] ?? point.label }}</strong>
              <span>新增 {{ point.newTenantCount }}</span>
            </div>
            <p class="admin-hero__focus-text">其中 {{ point.payingTenantCount }} 家已转为付费租户。</p>
          </article>
        </div>
      </div>

      <div class="admin-hero__business-card">
        <div class="admin-hero__focus-row">
          <div>
            <p class="admin-hero__eyebrow">试用转化</p>
            <h5 class="admin-hero__focus-title">当前漏斗快照</h5>
          </div>
          <StatusPill
            :label="trialConversion.expiringSoonCount > 0 ? '需跟进' : '稳定'"
            :tone="trialConversion.expiringSoonCount > 0 ? 'warn' : 'success'"
          />
        </div>
        <div class="admin-hero__trend-list">
          <article class="admin-hero__trend-item">
            <div class="admin-hero__focus-row">
              <strong>进行中试用</strong>
              <span>{{ trialConversion.trialTenantCount }}</span>
            </div>
            <p class="admin-hero__focus-text">仍在试用期或尚未转化的租户。</p>
          </article>
          <article class="admin-hero__trend-item">
            <div class="admin-hero__focus-row">
              <strong>已转化租户</strong>
              <span>{{ trialConversion.convertedTenantCount }}</span>
            </div>
            <p class="admin-hero__focus-text">已转为有效付费订阅的租户。</p>
          </article>
          <article class="admin-hero__trend-item">
            <div class="admin-hero__focus-row">
              <strong>临期试用</strong>
              <span>{{ trialConversion.expiringSoonCount }}</span>
            </div>
            <p class="admin-hero__focus-text">7 天内到期的试用需直接跟进。</p>
          </article>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.admin-hero,
.admin-hero__copy,
.admin-hero__focus,
.admin-hero__business-card,
.admin-hero__trend-list {
  display: grid;
  gap: 0.9rem;
}

.admin-hero {
  grid-template-columns: 1.05fr 0.95fr;
}

.admin-hero__copy,
.admin-hero__focus,
.admin-hero__business-card {
  padding: 1.05rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
}

.admin-hero__copy {
  background: var(--color-accent-soft);
}

.admin-hero__focus,
.admin-hero__business-card {
  background: var(--color-surface);
}

.admin-hero__eyebrow,
.admin-hero__title,
.admin-hero__summary,
.admin-hero__feedback,
.admin-hero__focus-title,
.admin-hero__focus-text {
  margin: 0;
}

.admin-hero__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.admin-hero__title,
.admin-hero__focus-title {
  color: var(--color-ink-strong);
}

.admin-hero__title {
  font-size: 1.4rem;
  font-weight: 600;
  line-height: 1.2;
}

.admin-hero__summary,
.admin-hero__feedback,
.admin-hero__focus-text,
.admin-hero__trend-item span {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.admin-hero__feedback {
  color: var(--color-accent);
}

.admin-hero__focus-row {
  display: flex;
  justify-content: space-between;
  gap: 0.85rem;
  align-items: start;
}

.admin-hero__metrics {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 0.85rem;
  margin-top: 1rem;
}

.admin-hero__business-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.admin-hero__trend-item {
  display: grid;
  gap: 0.35rem;
  padding: 0.85rem;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.admin-hero__trend-item strong {
  color: var(--color-ink-strong);
}

@media (max-width: 1180px) {
  .admin-hero,
  .admin-hero__business-grid,
  .admin-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
