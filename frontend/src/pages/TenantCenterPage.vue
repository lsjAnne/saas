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
      <p class="tenant-center-page__eyebrow">租户运营</p>
      <h2 class="tenant-center-page__title">租户中心</h2>
      <p class="tenant-center-page__subtitle">
        租户列表、合规与生命周期管理
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
  gap: 0.35rem;
}

.tenant-center-page__eyebrow,
.tenant-center-page__title,
.tenant-center-page__subtitle,
.tenant-center-page__footer-note {
  margin: 0;
}

.tenant-center-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.tenant-center-page__title {
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
}

.tenant-center-page__subtitle,
.tenant-center-page__footer-note {
  color: var(--color-ink-soft);
  line-height: 1.6;
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
