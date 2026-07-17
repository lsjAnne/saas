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
    return props.subscription ? `${props.subscription.seatCount} seats` : '--';
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

const cycleSummary = computed(() => [
  {
    label: 'Status',
    value: props.subscription?.subscriptionStatus ?? '--',
    tone: props.subscription?.subscriptionStatus === 'active' ? 'success' : 'warn'
  },
  {
    label: 'Auto renew',
    value: props.subscription?.autoRenew ? 'Enabled' : 'Disabled',
    tone: props.subscription?.autoRenew ? 'success' : 'neutral'
  },
  {
    label: 'Billing type',
    value: currentPlan.value?.billingType ?? '--',
    tone: 'neutral'
  }
]);

const quotaMetrics = computed(() =>
  props.quotas.map((quota) => ({
    label: quota.quotaCode.replaceAll('_', ' '),
    value: `${quota.usedAmount} / ${quota.quotaLimit}`,
    caption: quota.resetAt ? `Reset at ${formatDate(quota.resetAt)}` : ''
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
    eyebrow="Subscription Snapshot"
    :title="subscription?.planName || 'Current plan'"
    description="The top section keeps subscription actions focused on plan changes, renewal, seats, and quota signals."
  >
    <div class="subscription-hero">
      <div class="subscription-hero__metrics">
        <MetricCard label="Renewal" :value="formattedRenewal" caption="Current expiration date" />
        <MetricCard label="Seat Status" :value="seatMetric" caption="Seat quota usage" />
        <MetricCard label="Latest Bill" :value="billMetric" caption="Latest payable amount" />
      </div>

      <div class="subscription-hero__details">
        <article class="subscription-hero__detail-card">
          <div class="subscription-hero__detail-head">
            <div>
              <p class="subscription-hero__detail-eyebrow">Current Package</p>
              <h4 class="subscription-hero__detail-title">
                {{ currentPlan?.planName || subscription?.planName || 'No paid plan yet' }}
              </h4>
            </div>
            <StatusPill
              :label="subscription?.subscriptionStatus || 'unknown'"
              :tone="subscription?.subscriptionStatus === 'active' ? 'success' : 'warn'"
            />
          </div>
          <p class="subscription-hero__detail-copy">
            Monthly {{ formatCurrency(currentPlan?.monthlyPrice) }} / Yearly
            {{ formatCurrency(currentPlan?.yearlyPrice) }} / Seat limit {{ currentPlan?.seatLimit ?? '--' }}
          </p>
          <label class="subscription-hero__field">
            <span class="subscription-hero__field-label">Target plan</span>
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
            {{ isSubmitting ? 'Submitting...' : 'Update plan' }}
          </button>
        </article>

        <article class="subscription-hero__detail-card">
          <div>
            <p class="subscription-hero__detail-eyebrow">Subscription Cycle</p>
            <h4 class="subscription-hero__detail-title">Renewal control</h4>
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
            <span class="subscription-hero__field-label">Renewal months</span>
            <select v-model.number="formState.renewMonths" class="subscription-hero__control">
              <option :value="1">1 month</option>
              <option :value="3">3 months</option>
              <option :value="6">6 months</option>
              <option :value="12">12 months</option>
            </select>
          </label>
          <button
            type="button"
            class="subscription-hero__ghost"
            :disabled="isSubmitting || !hasSubscription"
            @click="submitRenew"
          >
            {{ isSubmitting ? 'Submitting...' : 'Renew now' }}
          </button>
        </article>

        <article class="subscription-hero__detail-card">
          <div>
            <p class="subscription-hero__detail-eyebrow">Seats & Quotas</p>
            <h4 class="subscription-hero__detail-title">Capacity control</h4>
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
            <span class="subscription-hero__field-label">Extra seats</span>
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
            {{ isSubmitting ? 'Submitting...' : 'Purchase seats' }}
          </button>
        </article>
      </div>

      <p v-if="isLoading" class="subscription-hero__loading">Refreshing subscription snapshot...</p>
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
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.72);
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
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.74rem;
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
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 250, 245, 0.7);
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
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: calc(var(--radius-lg) - 0.25rem);
  padding: 0.72rem 0.8rem;
  background: rgba(255, 251, 247, 0.92);
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
  background: linear-gradient(135deg, #2e251d, #856243);
  color: #fff8f0;
}

.subscription-hero__ghost {
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.74);
  color: var(--color-ink);
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
