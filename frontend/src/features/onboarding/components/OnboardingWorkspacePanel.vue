<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import OnboardingInitializationPanel from '@/features/onboarding/components/OnboardingInitializationPanel.vue';
import OnboardingStoreStatusPanel from '@/features/onboarding/components/OnboardingStoreStatusPanel.vue';
import type {
  ConnectStorePayload,
  RegisterTenantResponse,
  Store,
  SubscriptionPlan,
  TenantContextView,
  TenantSubscription,
  UpdateStoreSettingsPayload,
  UsageQuota
} from '@/services/apiTypes';

interface Props {
  registration: RegisterTenantResponse | null;
  context: TenantContextView | null;
  subscription: TenantSubscription | null;
  quotas: UsageQuota[];
  plans: SubscriptionPlan[];
  stores: Store[];
  selectedStoreId: string | null;
  selectedStore: Store | null;
  defaultOrganizationId: string;
  currentOperatorId: string;
  isRefreshing: boolean;
  isUpdatingPlan: boolean;
  isConnectingStore: boolean;
  isSavingSettings: boolean;
  feedbackMessage?: string | null;
  errorMessage?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  feedbackMessage: null,
  errorMessage: null
});

const emit = defineEmits<{
  refresh: [];
  enterConsole: [];
  logout: [];
  selectPlan: [planCode: string];
  selectStore: [storeId: string];
  connectStore: [payload: ConnectStorePayload];
  saveSettings: [payload: UpdateStoreSettingsPayload];
}>();

const organizationId = computed(
  () => props.context?.defaultOrganizationId || props.registration?.defaultOrganizationId || '--'
);
const trialEndAt = computed(
  () => props.registration?.trialEndAt || props.subscription?.expiredAt || null
);
const paidPlans = computed(() =>
  props.plans.filter((plan) => plan.planCode.toLowerCase() !== 'trial')
);

function formatDate(value: string | null) {
  if (!value) {
    return '--';
  }

  return new Date(value).toLocaleDateString('zh-CN');
}

function formatQuota(quota: UsageQuota) {
  return `${quota.usedAmount} / ${quota.quotaLimit}`;
}

function planTone(planCode: string) {
  return planCode.toLowerCase().includes('trial') ? 'warn' : 'success';
}

function isPlanCurrent(planCode: string) {
  return planCode === props.subscription?.planCode;
}

function isPlanDisabled(planCode: string) {
  if (isPlanCurrent(planCode)) {
    return true;
  }

  return planCode.toLowerCase() === 'trial';
}
</script>

<template>
  <section class="onboarding-workspace">
    <header class="onboarding-workspace__header">
      <div>
        <p class="onboarding-workspace__eyebrow">Onboarding Workspace</p>
        <h2 class="onboarding-workspace__title">登录后不要立刻跳控制台，先把租户接入闭环做完整</h2>
        <p class="onboarding-workspace__subtitle">
          这里展示默认组织建立结果、当前订阅状态、店铺授权状态和初始化阈值，让 `/login`
          变成真正的接入工作台。
        </p>
      </div>
      <div class="onboarding-workspace__actions">
        <button type="button" class="onboarding-workspace__ghost" @click="emit('refresh')">
          {{ isRefreshing ? '刷新中...' : '刷新当前状态' }}
        </button>
        <button type="button" class="onboarding-workspace__primary" @click="emit('enterConsole')">
          完成后进入控制台
        </button>
        <button type="button" class="onboarding-workspace__ghost" @click="emit('logout')">
          退出登录
        </button>
      </div>
    </header>

    <div
      v-if="feedbackMessage || errorMessage"
      :class="[
        'onboarding-workspace__banner',
        { 'onboarding-workspace__banner--error': errorMessage }
      ]"
    >
      {{ errorMessage || feedbackMessage }}
    </div>

    <div class="onboarding-workspace__top-grid">
      <PanelCard
        eyebrow="Organization"
        title="组织创建结果"
        description="当前后端没有独立的组织创建接口，所以这里直接展示 register 后自动建立的默认组织与租户上下文。"
      >
        <div class="onboarding-workspace__facts">
          <div>
            <span>租户名称</span>
            <strong>{{ context?.tenantName || registration?.tenantName || '--' }}</strong>
          </div>
          <div>
            <span>租户编码</span>
            <strong>{{ context?.tenantCode || registration?.tenantCode || '--' }}</strong>
          </div>
          <div>
            <span>默认组织 ID</span>
            <strong>{{ organizationId }}</strong>
          </div>
          <div>
            <span>试用到期</span>
            <strong>{{ formatDate(trialEndAt) }}</strong>
          </div>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Subscription"
        title="当前套餐与配额"
        description="套餐动作走真实订阅接口，配额直接回读后端额度，避免页面只停留在展示层。"
      >
        <div class="onboarding-workspace__subscription-head">
          <div>
            <p class="onboarding-workspace__plan-name">
              {{ subscription?.planName || '试用版' }}
            </p>
            <p class="onboarding-workspace__plan-copy">
              状态 {{ subscription?.subscriptionStatus || 'trialing' }} · 席位
              {{ subscription?.seatCount ?? 1 }}
            </p>
          </div>
          <StatusPill
            :label="subscription?.planCode || 'trial'"
            :tone="planTone(subscription?.planCode || 'trial')"
          />
        </div>

        <div class="onboarding-workspace__quota-grid">
          <article v-for="quota in quotas" :key="quota.quotaCode" class="onboarding-workspace__quota">
            <p>{{ quota.quotaCode }}</p>
            <strong>{{ formatQuota(quota) }}</strong>
            <span>重置 {{ formatDate(quota.resetAt) }}</span>
          </article>
        </div>
      </PanelCard>
    </div>

    <PanelCard
      eyebrow="Plan Switch"
      title="套餐选择区"
      description="试用态会自动走 subscribe，正式套餐之间会根据目标等级走 upgrade 或 downgrade。"
    >
      <div class="onboarding-workspace__plan-grid">
        <article
          v-for="plan in plans"
          :key="plan.planCode"
          :class="[
            'onboarding-workspace__plan-card',
            { 'onboarding-workspace__plan-card--active': isPlanCurrent(plan.planCode) }
          ]"
        >
          <div class="onboarding-workspace__plan-card-head">
            <div>
              <p class="onboarding-workspace__plan-card-title">{{ plan.planName }}</p>
              <p class="onboarding-workspace__plan-card-meta">
                月付 ¥{{ plan.monthlyPrice }} · 年付 ¥{{ plan.yearlyPrice }} · 席位上限 {{ plan.seatLimit }}
              </p>
            </div>
            <StatusPill :label="plan.planCode" :tone="planTone(plan.planCode)" />
          </div>
          <button
            type="button"
            class="onboarding-workspace__plan-button"
            :disabled="isUpdatingPlan || isPlanDisabled(plan.planCode)"
            @click="emit('selectPlan', plan.planCode)"
          >
            {{
              isPlanCurrent(plan.planCode)
                ? '当前套餐'
                : plan.planCode.toLowerCase() === 'trial'
                  ? '试用版不可回退'
                  : isUpdatingPlan
                    ? '切换中...'
                    : '切换到此套餐'
            }}
          </button>
        </article>
      </div>
      <p class="onboarding-workspace__plan-note">
        可切换套餐：{{ paidPlans.map((plan) => plan.planName).join(' / ') || '暂无' }}
      </p>
    </PanelCard>

    <OnboardingStoreStatusPanel
      :stores="stores"
      :selected-store-id="selectedStoreId"
      :default-organization-id="defaultOrganizationId"
      :current-operator-id="currentOperatorId"
      :is-submitting="isConnectingStore"
      @select-store="emit('selectStore', $event)"
      @connect-store="emit('connectStore', $event)"
    />

    <OnboardingInitializationPanel
      :selected-store="selectedStore"
      :is-submitting="isSavingSettings"
      @save-settings="emit('saveSettings', $event)"
    />
  </section>
</template>

<style scoped>
.onboarding-workspace,
.onboarding-workspace__header {
  display: grid;
  gap: 1rem;
}

.onboarding-workspace__header {
  grid-template-columns: 1.1fr 0.9fr;
  align-items: end;
}

.onboarding-workspace__eyebrow,
.onboarding-workspace__title,
.onboarding-workspace__subtitle,
.onboarding-workspace__plan-name,
.onboarding-workspace__plan-copy,
.onboarding-workspace__plan-note,
.onboarding-workspace__banner {
  margin: 0;
}

.onboarding-workspace__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.onboarding-workspace__title {
  max-width: 16ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3.4rem);
  line-height: 1.02;
}

.onboarding-workspace__subtitle,
.onboarding-workspace__plan-note {
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.onboarding-workspace__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: flex-end;
}

.onboarding-workspace__primary,
.onboarding-workspace__ghost,
.onboarding-workspace__plan-button {
  min-height: 2.95rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.onboarding-workspace__primary,
.onboarding-workspace__plan-button {
  border: 0;
  background: linear-gradient(135deg, #2d241c, #856243);
  color: #fff8f0;
  font-weight: 700;
}

.onboarding-workspace__ghost {
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-ink);
}

.onboarding-workspace__banner {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  background: rgba(47, 109, 83, 0.1);
  color: var(--color-success);
}

.onboarding-workspace__banner--error {
  background: rgba(183, 117, 47, 0.12);
  color: var(--color-warning);
}

.onboarding-workspace__top-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.onboarding-workspace__facts,
.onboarding-workspace__quota-grid,
.onboarding-workspace__plan-grid {
  display: grid;
  gap: 0.8rem;
}

.onboarding-workspace__facts {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.onboarding-workspace__facts span,
.onboarding-workspace__facts strong,
.onboarding-workspace__quota p,
.onboarding-workspace__quota strong,
.onboarding-workspace__quota span,
.onboarding-workspace__plan-card-title,
.onboarding-workspace__plan-card-meta {
  margin: 0;
}

.onboarding-workspace__facts span,
.onboarding-workspace__quota p,
.onboarding-workspace__quota span,
.onboarding-workspace__plan-copy,
.onboarding-workspace__plan-card-meta {
  color: var(--color-ink-soft);
  font-size: 0.86rem;
}

.onboarding-workspace__facts strong,
.onboarding-workspace__plan-name,
.onboarding-workspace__quota strong,
.onboarding-workspace__plan-card-title {
  color: var(--color-ink-strong);
}

.onboarding-workspace__subscription-head,
.onboarding-workspace__plan-card-head {
  display: flex;
  gap: 0.75rem;
  justify-content: space-between;
  align-items: center;
}

.onboarding-workspace__quota-grid,
.onboarding-workspace__plan-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 1rem;
}

.onboarding-workspace__quota,
.onboarding-workspace__plan-card {
  display: grid;
  gap: 0.5rem;
  padding: 0.95rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.72);
}

.onboarding-workspace__plan-card--active {
  border-color: rgba(133, 98, 67, 0.42);
  box-shadow: 0 14px 28px rgba(33, 24, 16, 0.08);
}

.onboarding-workspace__plan-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 1180px) {
  .onboarding-workspace__header,
  .onboarding-workspace__top-grid,
  .onboarding-workspace__quota-grid,
  .onboarding-workspace__plan-grid,
  .onboarding-workspace__facts {
    grid-template-columns: 1fr;
  }

  .onboarding-workspace__actions {
    justify-content: flex-start;
  }
}
</style>
