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
</script>

<template>
  <PanelCard
    eyebrow="SaaS Operations"
    title="Commercial health, conversion, and churn in one view"
    description="The hero section turns the platform page into an operations cockpit by putting commercial metrics, growth, and conversion signals ahead of governance details."
    dark
  >
    <div class="admin-hero">
      <div class="admin-hero__copy">
        <p class="admin-hero__eyebrow">Current focus tenant</p>
        <h4 class="admin-hero__title">{{ selectedTenant?.tenantName || 'Select a tenant to inspect' }}</h4>
        <p class="admin-hero__summary">
          Current portfolio: {{ summary.totalTenantCount }} tenants, {{ summary.payingTenantCount }}
          paying, {{ summary.trialTenantCount }} in trial, and {{ summary.atRiskTenantCount }} at
          risk.
        </p>
        <p v-if="actionFeedback" class="admin-hero__feedback">{{ actionFeedback }}</p>
      </div>

      <div class="admin-hero__focus">
        <div class="admin-hero__focus-row">
          <div>
            <p class="admin-hero__eyebrow">Automation pulse</p>
            <h5 class="admin-hero__focus-title">
              {{ formatDateTime(subscriptionAutomationSummary?.executedAt) }}
            </h5>
          </div>
          <StatusPill
            :label="selectedTenant?.tenantStatus || 'idle'"
            :tone="selectedTenant?.tenantStatus === 'active' ? 'success' : 'warn'"
          />
        </div>
        <p class="admin-hero__focus-text">
          Selected tenant: {{ selectedTenant?.planName || '--' }} / {{ selectedTenant?.subscriptionStatus || '--' }}
          / {{ selectedTenant?.seatCount ?? '--' }} seats
        </p>
        <p class="admin-hero__focus-text">
          Last automation run: renewed {{ subscriptionAutomationSummary?.autoRenewedCount ?? 0 }},
          suspended {{ subscriptionAutomationSummary?.autoSuspendedCount ?? 0 }}, charge failed
          {{ subscriptionAutomationSummary?.autoChargeFailedCount ?? 0 }}.
        </p>
      </div>
    </div>

    <div class="admin-hero__metrics">
      <MetricCard label="Tenants" :value="formatCount(summary.totalTenantCount)" caption="Total portfolio size" />
      <MetricCard label="Paying" :value="formatCount(summary.payingTenantCount)" caption="Subscribed tenants" />
      <MetricCard label="New 30d" :value="formatCount(summary.newTenantCount30d)" caption="New tenants in the last 30 days" />
      <MetricCard label="Est. MRR" :value="formatCurrency(summary.estimatedMonthlyRevenue)" caption="Base recurring revenue estimate" />
      <MetricCard label="Trial conv." :value="formatPercent(trialConversion.conversionRate, 0)" caption="Current conversion snapshot" />
      <MetricCard label="At risk" :value="formatCount(summary.atRiskTenantCount)" caption="Overdue, suspended, expiring, or hot capacity" />
    </div>

    <div class="admin-hero__business-grid">
      <div class="admin-hero__business-card">
        <div class="admin-hero__focus-row">
          <div>
            <p class="admin-hero__eyebrow">Growth trend</p>
            <h5 class="admin-hero__focus-title">Recent tenant creation</h5>
          </div>
          <StatusPill label="6 months" tone="neutral" />
        </div>
        <div class="admin-hero__trend-list">
          <article v-for="point in growthTrend" :key="point.label" class="admin-hero__trend-item">
            <div class="admin-hero__focus-row">
              <strong>{{ point.label }}</strong>
              <span>{{ point.newTenantCount }} new</span>
            </div>
            <p class="admin-hero__focus-text">{{ point.payingTenantCount }} of them are paying tenants.</p>
          </article>
        </div>
      </div>

      <div class="admin-hero__business-card">
        <div class="admin-hero__focus-row">
          <div>
            <p class="admin-hero__eyebrow">Trial conversion</p>
            <h5 class="admin-hero__focus-title">Current funnel snapshot</h5>
          </div>
          <StatusPill
            :label="trialConversion.expiringSoonCount > 0 ? 'follow-up needed' : 'stable'"
            :tone="trialConversion.expiringSoonCount > 0 ? 'warn' : 'success'"
          />
        </div>
        <div class="admin-hero__trend-list">
          <article class="admin-hero__trend-item">
            <div class="admin-hero__focus-row">
              <strong>Active trials</strong>
              <span>{{ trialConversion.trialTenantCount }}</span>
            </div>
            <p class="admin-hero__focus-text">Tenants still inside trial or not yet converted.</p>
          </article>
          <article class="admin-hero__trend-item">
            <div class="admin-hero__focus-row">
              <strong>Converted tenants</strong>
              <span>{{ trialConversion.convertedTenantCount }}</span>
            </div>
            <p class="admin-hero__focus-text">Tenants already on active paid subscriptions.</p>
          </article>
          <article class="admin-hero__trend-item">
            <div class="admin-hero__focus-row">
              <strong>Expiring soon</strong>
              <span>{{ trialConversion.expiringSoonCount }}</span>
            </div>
            <p class="admin-hero__focus-text">Trials ending within 7 days need direct follow-up.</p>
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
  border: 1px solid rgba(255, 244, 236, 0.08);
}

.admin-hero__copy {
  background: linear-gradient(145deg, rgba(255, 244, 233, 0.12), rgba(255, 255, 255, 0.04));
}

.admin-hero__focus,
.admin-hero__business-card {
  background: rgba(255, 248, 240, 0.1);
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
  color: rgba(255, 244, 236, 0.7);
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.admin-hero__title,
.admin-hero__focus-title {
  color: #fff8f0;
}

.admin-hero__title {
  font-family: var(--font-display);
  font-size: clamp(1.9rem, 3vw, 2.7rem);
  line-height: 1.02;
}

.admin-hero__summary,
.admin-hero__feedback,
.admin-hero__focus-text,
.admin-hero__trend-item span {
  color: rgba(255, 244, 236, 0.78);
  line-height: 1.7;
}

.admin-hero__feedback {
  color: #f6d8b5;
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
  background: rgba(255, 255, 255, 0.08);
}

.admin-hero__trend-item strong {
  color: #fff8f0;
}

@media (max-width: 1180px) {
  .admin-hero,
  .admin-hero__business-grid,
  .admin-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
