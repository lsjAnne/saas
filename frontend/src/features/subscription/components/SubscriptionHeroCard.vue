<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  BillingOrder,
  SubscriptionPlan,
  TenantSubscription,
  UsageQuota
} from '@/services/apiTypes';
import { formatCurrency, formatDate } from '@/utils/formatters';

interface Props {
  subscription: TenantSubscription | null;
  plans: SubscriptionPlan[];
  quotas: UsageQuota[];
  currentBill: BillingOrder | null;
  isLoading?: boolean;
  isSubmitting?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  isLoading: false,
  isSubmitting: false
});
const emit = defineEmits<{
  changePlan: [planCode: string];
  renew: [months: number];
  purchaseSeats: [seatCount: number];
}>();

const formState = reactive({
  targetPlanCode: '',
  renewMonths: 12,
  extraSeatCount: 1
});

const hasSubscription = computed(() => Boolean(props.subscription?.subscriptionId));

const formattedRenewal = computed(() => {
  if (!props.subscription?.expiredAt) {
    return '--';
  }

  return formatDate(props.subscription.expiredAt);
});

const seatMetric = computed(() => {
  const seatQuota = props.quotas.find((quota) => quota.quotaCode === 'seat_count');
  if (!seatQuota) {
    return props.subscription ? `${props.subscription.seatCount} 个坐席` : '--';
  }

  return `${seatQuota.usedAmount} / ${seatQuota.quotaLimit}`;
});

const billMetric = computed(() => {
  if (!props.currentBill) {
    return '--';
  }

  return formatCurrency(props.currentBill.payableAmount);
});

const currentPlan = computed(
  () => props.plans.find((plan) => plan.planCode === props.subscription?.planCode) ?? null
);

const selectablePlans = computed(() => props.plans.filter((plan) => plan.planCode !== 'trial'));

const cycleValueMap: Record<string, string> = {
  active: '生效中',
  trialing: '试用中',
  trial: '试用中',
  expired: '已过期',
  canceled: '已取消',
  monthly: '月付',
  yearly: '年付'
};

const quotaLabelMap: Record<string, string> = {
  seat_count: '坐席数',
  api_quota: 'API 调用',
  storage_quota: '存储空间',
  message_quota: '消息量'
};

const cycleSummary = computed(() => [
  {
    label: '状态',
    value: cycleValueMap[props.subscription?.subscriptionStatus ?? ''] ?? props.subscription?.subscriptionStatus ?? '--',
    tone: props.subscription?.subscriptionStatus === 'active' ? 'success' : 'warn'
  },
  {
    label: '自动续费',
    value: props.subscription?.autoRenew ? '已启用' : '已停用',
    tone: props.subscription?.autoRenew ? 'success' : 'neutral'
  },
  {
    label: '计费方式',
    value: cycleValueMap[currentPlan.value?.billingType ?? ''] ?? currentPlan.value?.billingType ?? '--',
    tone: 'neutral'
  }
]);

const quotaMetrics = computed(() =>
  props.quotas.map((quota) => ({
    label: quotaLabelMap[quota.quotaCode] ?? quota.quotaCode.replaceAll('_', ' '),
    value: `${quota.usedAmount} / ${quota.quotaLimit}`,
    caption: quota.resetAt ? `重置于 ${formatDate(quota.resetAt)}` : ''
  }))
);

watch(
  () => [props.subscription?.planCode, props.plans],
  () => {
    formState.targetPlanCode =
      props.plans.find((plan) => plan.planCode !== props.subscription?.planCode)?.planCode ??
      props.subscription?.planCode ??
      props.plans[0]?.planCode ??
      '';
    formState.renewMonths = 12;
    formState.extraSeatCount = 1;
  },
  { immediate: true }
);

function submitPlanChange() {
  if (!formState.targetPlanCode) {
    return;
  }

  emit('changePlan', formState.targetPlanCode);
}

function submitRenew() {
  emit('renew', formState.renewMonths);
}

function submitSeatPurchase() {
  emit('purchaseSeats', formState.extraSeatCount);
}
</script>

<template>
  <PanelCard
    :title="subscription?.planName || '当前套餐'"
    description="套餐变更、续费、坐席与配额用量一览。"
  >
    <div class="subscription-hero">
      <div class="subscription-hero__metrics">
        <MetricCard label="续费截止" :value="formattedRenewal" caption="当前套餐到期时间" />
        <MetricCard label="坐席用量" :value="seatMetric" caption="坐席配额使用情况" />
        <MetricCard label="最新账单" :value="billMetric" caption="最新应付金额" />
      </div>

      <div class="subscription-hero__details">
        <article class="subscription-hero__detail-card">
          <div class="subscription-hero__detail-head">
            <div>
              <p class="subscription-hero__detail-eyebrow">当前套餐</p>
              <h4 class="subscription-hero__detail-title">
                {{ currentPlan?.planName || subscription?.planName || '暂无付费套餐' }}
              </h4>
            </div>
            <StatusPill
              :label="cycleValueMap[subscription?.subscriptionStatus ?? ''] || subscription?.subscriptionStatus || '未知'"
              :tone="subscription?.subscriptionStatus === 'active' ? 'success' : 'warn'"
            />
          </div>
          <p class="subscription-hero__detail-copy">
            月付 {{ formatCurrency(currentPlan?.monthlyPrice) }} / 年付
            {{ formatCurrency(currentPlan?.yearlyPrice) }} / 坐席上限 {{ currentPlan?.seatLimit ?? '--' }}
          </p>
          <label class="subscription-hero__field">
            <span class="subscription-hero__field-label">目标套餐</span>
            <select v-model="formState.targetPlanCode" class="subscription-hero__control">
              <option v-for="plan in selectablePlans" :key="plan.planCode" :value="plan.planCode">
                {{ plan.planName }} / {{ plan.planCode }}
              </option>
            </select>
          </label>
          <button
            type="button"
            class="subscription-hero__solid"
            :disabled="isSubmitting || !formState.targetPlanCode"
            @click="submitPlanChange"
          >
            {{ isSubmitting ? '提交中...' : '更新套餐' }}
          </button>
        </article>

        <article class="subscription-hero__detail-card">
          <div>
            <p class="subscription-hero__detail-eyebrow">续费周期</p>
            <h4 class="subscription-hero__detail-title">续费管理</h4>
          </div>
          <div class="subscription-hero__pill-row">
            <StatusPill
              v-for="item in cycleSummary"
              :key="item.label"
              :label="`${item.label}: ${item.value}`"
              :tone="item.tone as 'neutral' | 'warn' | 'success'"
            />
          </div>
          <label class="subscription-hero__field">
            <span class="subscription-hero__field-label">续费月数</span>
            <select v-model.number="formState.renewMonths" class="subscription-hero__control">
              <option :value="1">1 个月</option>
              <option :value="3">3 个月</option>
              <option :value="6">6 个月</option>
              <option :value="12">12 个月</option>
            </select>
          </label>
          <button
            type="button"
            class="subscription-hero__ghost"
            :disabled="isSubmitting || !hasSubscription"
            @click="submitRenew"
          >
            {{ isSubmitting ? '提交中...' : '立即续费' }}
          </button>
        </article>

        <article class="subscription-hero__detail-card">
          <div>
            <p class="subscription-hero__detail-eyebrow">坐席与配额</p>
            <h4 class="subscription-hero__detail-title">容量管理</h4>
          </div>
          <div class="subscription-hero__quota-grid">
            <div
              v-for="metric in quotaMetrics"
              :key="metric.label"
              class="subscription-hero__quota-item"
            >
              <p class="subscription-hero__quota-label">{{ metric.label }}</p>
              <strong class="subscription-hero__quota-value">{{ metric.value }}</strong>
              <p class="subscription-hero__quota-caption">{{ metric.caption }}</p>
            </div>
          </div>
          <label class="subscription-hero__field">
            <span class="subscription-hero__field-label">额外坐席</span>
            <input
              v-model.number="formState.extraSeatCount"
              class="subscription-hero__control"
              type="number"
              min="1"
            />
          </label>
          <button
            type="button"
            class="subscription-hero__ghost"
            :disabled="isSubmitting || !hasSubscription"
            @click="submitSeatPurchase"
          >
            {{ isSubmitting ? '提交中...' : '购买坐席' }}
          </button>
        </article>
      </div>

      <p v-if="isLoading" class="subscription-hero__loading">正在刷新订阅快照...</p>
    </div>
  </PanelCard>
</template>

<style scoped>
.subscription-hero {
  display: grid;
  gap: 1.2rem;
}

.subscription-hero__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.9rem;
}

.subscription-hero__details {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.9rem;
}

.subscription-hero__detail-card {
  display: grid;
  gap: 0.85rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.subscription-hero__detail-head {
  display: flex;
  justify-content: space-between;
  gap: 0.85rem;
  align-items: flex-start;
}

.subscription-hero__detail-eyebrow,
.subscription-hero__detail-copy,
.subscription-hero__field-label,
.subscription-hero__quota-label,
.subscription-hero__quota-caption,
.subscription-hero__loading {
  margin: 0;
  color: var(--color-ink-soft);
}

.subscription-hero__detail-eyebrow,
.subscription-hero__field-label,
.subscription-hero__quota-label {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.subscription-hero__detail-title {
  margin: 0.45rem 0 0;
  color: var(--color-ink-strong);
  font-size: 1.2rem;
}

.subscription-hero__pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.subscription-hero__quota-grid {
  display: grid;
  gap: 0.75rem;
}

.subscription-hero__quota-item {
  padding: 0.9rem;
  border-radius: calc(var(--radius-lg) - 0.15rem);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.subscription-hero__quota-value {
  display: block;
  margin-top: 0.45rem;
  color: var(--color-ink-strong);
  font-size: 1.1rem;
}

.subscription-hero__field {
  display: grid;
  gap: 0.55rem;
}

.subscription-hero__control {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--color-line);
  border-radius: calc(var(--radius-lg) - 0.25rem);
  padding: 0.72rem 0.8rem;
  background: var(--color-surface);
  color: var(--color-ink-strong);
  font: inherit;
}

.subscription-hero__solid,
.subscription-hero__ghost {
  min-height: 2.9rem;
  padding: 0 1.05rem;
  border-radius: var(--radius-pill);
}

.subscription-hero__solid {
  border: 0;
  background: var(--color-accent);
  color: #ffffff;
}

.subscription-hero__solid:hover {
  background: var(--color-accent-hover);
  color: #ffffff;
}

.subscription-hero__ghost {
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
}

.subscription-hero__ghost:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

@media (max-width: 1120px) {
  .subscription-hero__details {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .subscription-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
