<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import TenantCleanupPanel from '@/features/tenantCenter/components/TenantCleanupPanel.vue';
import TenantCompliancePanel from '@/features/tenantCenter/components/TenantCompliancePanel.vue';
import TenantExportPanel from '@/features/tenantCenter/components/TenantExportPanel.vue';
import TenantIdentityCard from '@/features/tenantCenter/components/TenantIdentityCard.vue';
import { useTenantCenterOverview } from '@/features/tenantCenter/composables/useTenantCenterOverview';
import type {
  PlanChangePayload,
  RegisterTenantRequest,
  SeatPurchasePayload,
  SubscriptionActionPayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const tenantCenterOverview = useTenantCenterOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await tenantCenterOverview.load(authStore.token);
}

async function withToken(callback: (token: string) => Promise<void>) {
  if (!authStore.token) {
    return;
  }

  await callback(authStore.token);
}

async function handleCreateTenant(payload: RegisterTenantRequest) {
  await withToken((token) => tenantCenterOverview.createTenant(payload, token));
}

async function handleStartTrial() {
  await withToken((token) => tenantCenterOverview.startTrial(token));
}

async function handleSuspendTenant() {
  await withToken((token) => tenantCenterOverview.suspendTenant(token));
}

async function handleResumeTenant() {
  await withToken((token) => tenantCenterOverview.resumeTenant(token));
}

async function handlePlanChange(payload: SubscriptionActionPayload | PlanChangePayload) {
  await withToken((token) => tenantCenterOverview.submitPlanChange(payload, token));
}

async function handlePurchaseSeats(payload: SeatPurchasePayload) {
  await withToken((token) => tenantCenterOverview.purchaseSeats(payload, token));
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="tenant-center-page">
    <header class="tenant-center-page__header">
      <p class="tenant-center-page__eyebrow">Tenant Center</p>
      <h2 class="tenant-center-page__title">把租户开通、套餐配额和停复服动作收进同一个运营视角</h2>
      <p class="tenant-center-page__subtitle">
        这里按原型里的 SaaS 租户中心口径组织页面，先看租户列表和状态，再处理套餐、席位、试用到期与停复服动作。
      </p>
    </header>

    <InlineErrorCard
      v-if="tenantCenterOverview.errorMessage"
      :message="tenantCenterOverview.errorMessage"
      @retry="bootstrapPage"
    />

    <TenantIdentityCard
      :summary="tenantCenterOverview.summary"
      :selected-tenant="tenantCenterOverview.selectedTenant"
      :selected-plan="tenantCenterOverview.selectedPlan"
      :action-feedback="tenantCenterOverview.actionFeedback"
    />

    <div class="tenant-center-page__grid">
      <TenantCompliancePanel
        :tenants="tenantCenterOverview.tenants"
        :selected-tenant-id="tenantCenterOverview.selectedTenantId"
        @select-tenant="tenantCenterOverview.selectTenant"
      />

      <TenantExportPanel
        :selected-tenant="tenantCenterOverview.selectedTenant"
        :selected-plan="tenantCenterOverview.selectedPlan"
        :plans="tenantCenterOverview.plans"
        :is-submitting="tenantCenterOverview.isRunningAction"
        :action-error="tenantCenterOverview.actionError"
        @submit-plan-change="handlePlanChange"
        @purchase-seats="handlePurchaseSeats"
      />
    </div>

    <TenantCleanupPanel
      :selected-tenant="tenantCenterOverview.selectedTenant"
      :expiring-tenants="tenantCenterOverview.expiringTenants"
      :is-submitting="tenantCenterOverview.isRunningAction"
      :action-error="tenantCenterOverview.actionError"
      @create-tenant="handleCreateTenant"
      @start-trial="handleStartTrial"
      @suspend-tenant="handleSuspendTenant"
      @resume-tenant="handleResumeTenant"
    />

    <p v-if="tenantCenterOverview.isLoading" class="tenant-center-page__footer-note">
      正在读取租户列表、套餐配额与生命周期动作...
    </p>
  </section>
</template>

<style scoped>
.tenant-center-page {
  display: grid;
  gap: 1.2rem;
}

.tenant-center-page__header {
  display: grid;
  gap: 0.75rem;
}

.tenant-center-page__eyebrow,
.tenant-center-page__title,
.tenant-center-page__subtitle,
.tenant-center-page__footer-note {
  margin: 0;
}

.tenant-center-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.tenant-center-page__title {
  max-width: 16ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.1rem, 3.8vw, 3.6rem);
  line-height: 1.05;
}

.tenant-center-page__subtitle,
.tenant-center-page__footer-note {
  max-width: 60rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.tenant-center-page__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  align-items: start;
}

@media (max-width: 1080px) {
  .tenant-center-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
