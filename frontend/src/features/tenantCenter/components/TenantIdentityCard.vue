<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { AdminTenantOverview, SubscriptionPlan } from '@/services/apiTypes';
import { formatCount, formatDate, formatPercent } from '@/utils/formatters';

interface Props {
  summary: {
    totalTenantCount: number;
    activeTenantCount: number;
    trialTenantCount: number;
    suspendedTenantCount: number;
    expiringSoonCount: number;
    totalSeatCount: number;
    totalQuotaUsageRate: number;
  };
  selectedTenant: AdminTenantOverview | null;
  selectedPlan: SubscriptionPlan | null;
  actionFeedback?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionFeedback: null
});

function tenantStatusLabel(status?: string | null) {
  const map: Record<string, string> = {
    active: '正常',
    suspended: '已停用',
    trial: '试用',
    expiring: '临期'
  };
  return (status && map[status]) || '未知';
}

function subscriptionStatusLabel(status?: string | null) {
  const map: Record<string, string> = {
    active: '生效',
    unsubscribed: '未订阅',
    trial: '试用',
    past_due: '已逾期'
  };
  return (status && map[status]) || '未订阅';
}

const metricItems = computed(() => [
  {
    label: '租户总数',
    value: formatCount(props.summary.totalTenantCount, ' 个'),
    caption: '当前已纳入运营视角的租户数'
  },
  {
    label: '活跃租户',
    value: formatCount(props.summary.activeTenantCount, ' 个'),
    caption: '租户状态为正常'
  },
  {
    label: '试用临期',
    value: formatCount(props.summary.expiringSoonCount, ' 个'),
    caption: '7 天内到期的试用租户'
  },
  {
    label: '席位规模',
    value: formatCount(props.summary.totalSeatCount, ' 席'),
    caption: `总体配额使用率 ${formatPercent(props.summary.totalQuotaUsageRate, 0)}`
  }
]);
</script>

<template>
  <PanelCard
    title="租户整体概览"
    :description="
      selectedTenant
        ? `${selectedTenant.tenantName} 当前状态 ${selectedTenant.tenantStatus}，套餐 ${selectedTenant.planName || '未订阅'}，试用到期 ${formatDate(selectedTenant.trialEndAt)}。`
        : '先从左侧租户列表选定一个租户，再处理套餐、席位和生命周期动作。'
    "
  >
    <div class="tenant-identity">
      <div class="tenant-identity__summary">
        <div class="tenant-identity__metrics">
          <article
            v-for="metric in metricItems"
            :key="metric.label"
            class="tenant-identity__metric-card"
          >
            <p class="tenant-identity__metric-label">{{ metric.label }}</p>
            <h4 class="tenant-identity__metric-value">{{ metric.value }}</h4>
            <p class="tenant-identity__metric-caption">{{ metric.caption }}</p>
          </article>
        </div>
        <p v-if="actionFeedback" class="tenant-identity__feedback">{{ actionFeedback }}</p>
      </div>

      <div class="tenant-identity__focus">
        <div class="tenant-identity__focus-header">
          <p class="tenant-identity__focus-eyebrow">已选租户</p>
          <h4 class="tenant-identity__focus-title">
            {{ selectedTenant?.tenantName || '等待选择租户' }}
          </h4>
        </div>

        <div v-if="selectedTenant" class="tenant-identity__focus-grid">
          <div class="tenant-identity__focus-card">
            <p class="tenant-identity__focus-label">租户状态</p>
            <StatusPill
              :label="tenantStatusLabel(selectedTenant.tenantStatus)"
              :tone="selectedTenant.tenantStatus === 'active' ? 'success' : 'warn'"
            />
          </div>
          <div class="tenant-identity__focus-card">
            <p class="tenant-identity__focus-label">订阅状态</p>
            <StatusPill
              :label="subscriptionStatusLabel(selectedTenant.subscriptionStatus)"
              :tone="selectedTenant.subscriptionStatus === 'active' ? 'success' : 'neutral'"
            />
          </div>
          <div class="tenant-identity__focus-card">
            <p class="tenant-identity__focus-label">套餐</p>
            <p class="tenant-identity__focus-value">
              {{ selectedTenant.planName || selectedPlan?.planName || '未订阅' }}
            </p>
          </div>
          <div class="tenant-identity__focus-card">
            <p class="tenant-identity__focus-label">试用到期</p>
            <p class="tenant-identity__focus-value">{{ formatDate(selectedTenant.trialEndAt) }}</p>
          </div>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.tenant-identity {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(18rem, 0.92fr);
  gap: 1rem;
}

.tenant-identity__summary,
.tenant-identity__focus,
.tenant-identity__focus-header {
  display: grid;
  gap: 0.85rem;
}

.tenant-identity__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.tenant-identity__metric-card,
.tenant-identity__focus,
.tenant-identity__focus-card {
  border-radius: calc(var(--radius-xl) - 0.2rem);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.tenant-identity__metric-card,
.tenant-identity__focus-card {
  padding: 1rem;
}

.tenant-identity__focus {
  padding: 1rem;
}

.tenant-identity__metric-label,
.tenant-identity__metric-value,
.tenant-identity__metric-caption,
.tenant-identity__feedback,
.tenant-identity__focus-eyebrow,
.tenant-identity__focus-title,
.tenant-identity__focus-label,
.tenant-identity__focus-value {
  margin: 0;
}

.tenant-identity__metric-label,
.tenant-identity__focus-eyebrow,
.tenant-identity__focus-label {
  color: var(--color-ink-soft);
  font-size: 0.78rem;
  font-weight: 600;
}

.tenant-identity__metric-value,
.tenant-identity__focus-title,
.tenant-identity__focus-value {
  color: var(--color-ink-strong);
}

.tenant-identity__metric-value {
  margin-top: 0.4rem;
  font-size: 1.5rem;
}

.tenant-identity__metric-caption,
.tenant-identity__feedback {
  margin-top: 0.45rem;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.tenant-identity__focus-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

@media (max-width: 1080px) {
  .tenant-identity,
  .tenant-identity__metrics,
  .tenant-identity__focus-grid {
    grid-template-columns: 1fr;
  }
}
</style>
