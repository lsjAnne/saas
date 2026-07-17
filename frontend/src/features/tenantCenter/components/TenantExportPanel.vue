<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import type {
  AdminTenantOverview,
  PlanChangePayload,
  SeatPurchasePayload,
  SubscriptionActionPayload,
  SubscriptionPlan
} from '@/services/apiTypes';
import { formatCount, formatPercent } from '@/utils/formatters';

interface Props {
  selectedTenant: AdminTenantOverview | null;
  selectedPlan: SubscriptionPlan | null;
  plans: SubscriptionPlan[];
  isSubmitting: boolean;
  actionError?: string | null;
}

interface Emits {
  submitPlanChange: [payload: SubscriptionActionPayload | PlanChangePayload];
  purchaseSeats: [payload: SeatPurchasePayload];
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});
const emit = defineEmits<Emits>();

const planForm = reactive({
  planCode: '',
  seatCount: 1,
  autoRenew: true,
  extraSeatCount: 1
});

watch(
  () => props.selectedTenant,
  (tenant) => {
    planForm.planCode = tenant?.planCode || props.plans[0]?.planCode || '';
    planForm.seatCount = tenant?.seatCount || 1;
    planForm.autoRenew = true;
    planForm.extraSeatCount = 1;
  },
  { immediate: true }
);

const quotaUsageRate = computed(() =>
  props.selectedTenant
    ? props.selectedTenant.totalUsedAmount / Math.max(props.selectedTenant.totalQuotaLimit, 1)
    : 0
);

const hasSubscription = computed(() => Boolean(props.selectedTenant?.subscriptionId));

function submitPlan() {
  if (!planForm.planCode) {
    return;
  }

  if (!hasSubscription.value) {
    emit('submitPlanChange', {
      planCode: planForm.planCode,
      seatCount: planForm.seatCount,
      autoRenew: planForm.autoRenew
    });
    return;
  }

  emit('submitPlanChange', {
    planCode: planForm.planCode
  });
}

function submitSeatPurchase() {
  emit('purchaseSeats', {
    seatCount: planForm.extraSeatCount
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Package & Quota"
    title="套餐与配额区"
    description="这里直接调整套餐和席位，并同步查看当前配额压力。"
  >
    <div v-if="selectedTenant" class="tenant-package">
      <div class="tenant-package__overview">
        <article class="tenant-package__metric">
          <p class="tenant-package__metric-label">当前套餐</p>
          <h4 class="tenant-package__metric-value">
            {{ selectedTenant.planName || selectedPlan?.planName || '未订阅' }}
          </h4>
        </article>
        <article class="tenant-package__metric">
          <p class="tenant-package__metric-label">当前席位</p>
          <h4 class="tenant-package__metric-value">
            {{ formatCount(selectedTenant.seatCount, ' 席') }}
          </h4>
        </article>
        <article class="tenant-package__metric">
          <p class="tenant-package__metric-label">配额数量</p>
          <h4 class="tenant-package__metric-value">
            {{ formatCount(selectedTenant.quotaCount, ' 项') }}
          </h4>
        </article>
        <article class="tenant-package__metric">
          <p class="tenant-package__metric-label">配额使用率</p>
          <h4 class="tenant-package__metric-value">
            {{ formatPercent(quotaUsageRate, 0) }}
          </h4>
        </article>
      </div>

      <div class="tenant-package__forms">
        <div class="tenant-package__card">
          <div>
            <p class="tenant-package__card-eyebrow">Plan Operation</p>
            <h4 class="tenant-package__card-title">
              {{ hasSubscription ? '调整套餐' : '开通订阅' }}
            </h4>
          </div>

          <label class="tenant-package__field">
            <span class="tenant-package__field-label">目标套餐</span>
            <select v-model="planForm.planCode" class="tenant-package__select">
              <option v-for="plan in plans" :key="plan.planCode" :value="plan.planCode">
                {{ plan.planName }} / {{ plan.planCode }}
              </option>
            </select>
          </label>

          <label v-if="!hasSubscription" class="tenant-package__field">
            <span class="tenant-package__field-label">开通席位</span>
            <input v-model.number="planForm.seatCount" class="tenant-package__input" type="number" min="1" />
          </label>

          <label v-if="!hasSubscription" class="tenant-package__toggle">
            <input v-model="planForm.autoRenew" type="checkbox" />
            <span>同时开启自动续费</span>
          </label>

          <button class="tenant-package__primary" type="button" :disabled="isSubmitting" @click="submitPlan">
            {{ isSubmitting ? '提交中...' : hasSubscription ? '提交套餐变更' : '开通订阅' }}
          </button>
        </div>

        <div class="tenant-package__card">
          <div>
            <p class="tenant-package__card-eyebrow">Seat Operation</p>
            <h4 class="tenant-package__card-title">调整席位</h4>
          </div>

          <label class="tenant-package__field">
            <span class="tenant-package__field-label">新增席位数</span>
            <input
              v-model.number="planForm.extraSeatCount"
              class="tenant-package__input"
              type="number"
              min="1"
            />
          </label>

          <button
            class="tenant-package__secondary"
            type="button"
            :disabled="!hasSubscription || isSubmitting"
            @click="submitSeatPurchase"
          >
            {{ isSubmitting ? '提交中...' : '购买席位' }}
          </button>
        </div>
      </div>

      <p v-if="actionError" class="tenant-package__error">{{ actionError }}</p>
    </div>

    <p v-else class="tenant-package__empty">先从左侧选择一个租户，再处理套餐和配额。</p>
  </PanelCard>
</template>

<style scoped>
.tenant-package,
.tenant-package__overview,
.tenant-package__forms {
  display: grid;
  gap: 0.85rem;
}

.tenant-package__overview {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.tenant-package__forms {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.tenant-package__metric,
.tenant-package__card {
  padding: 0.95rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.1);
  background: rgba(255, 255, 255, 0.74);
}

.tenant-package__card,
.tenant-package__field {
  display: grid;
  gap: 0.7rem;
}

.tenant-package__metric-label,
.tenant-package__metric-value,
.tenant-package__card-eyebrow,
.tenant-package__card-title,
.tenant-package__field-label,
.tenant-package__error,
.tenant-package__empty {
  margin: 0;
}

.tenant-package__metric-label,
.tenant-package__card-eyebrow,
.tenant-package__field-label {
  color: var(--color-ink-faint);
  font-size: 0.75rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.tenant-package__metric-value,
.tenant-package__card-title {
  color: var(--color-ink-strong);
}

.tenant-package__metric-value {
  margin-top: 0.45rem;
}

.tenant-package__select,
.tenant-package__input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: calc(var(--radius-lg) - 0.25rem);
  padding: 0.72rem 0.8rem;
  background: rgba(255, 251, 247, 0.92);
  color: var(--color-ink-strong);
  font: inherit;
}

.tenant-package__toggle {
  display: flex;
  gap: 0.55rem;
  align-items: center;
  color: var(--color-ink-soft);
}

.tenant-package__primary,
.tenant-package__secondary {
  min-height: 2.8rem;
  border-radius: var(--radius-md);
  border: 0;
  padding: 0 1rem;
  font-weight: 600;
  cursor: pointer;
}

.tenant-package__primary {
  background: linear-gradient(135deg, #3f5f55, #1f2c28);
  color: #fffaf4;
}

.tenant-package__secondary {
  background: rgba(61, 46, 36, 0.08);
  color: var(--color-ink-strong);
}

.tenant-package__error {
  color: #b55d2f;
  line-height: 1.6;
}

@media (max-width: 1080px) {
  .tenant-package__overview,
  .tenant-package__forms {
    grid-template-columns: 1fr;
  }
}
</style>
