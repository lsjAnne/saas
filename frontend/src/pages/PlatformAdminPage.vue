<script setup lang="ts">
import { onMounted, reactive } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import ComplianceGovernancePanel from '@/features/platformAdmin/components/ComplianceGovernancePanel.vue';
import PlatformAdminHeroPanel from '@/features/platformAdmin/components/PlatformAdminHeroPanel.vue';
import ReadinessCommandPanel from '@/features/platformAdmin/components/ReadinessCommandPanel.vue';
import TenantMatrixPanel from '@/features/platformAdmin/components/TenantMatrixPanel.vue';
import {
  platformFeatureTogglePresets,
  usePlatformAdminOverview
} from '@/features/platformAdmin/composables/usePlatformAdminOverview';
import type {
  ComplianceDocumentPublishPayload,
  FeatureToggleUpdatePayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const platformAdmin = reactive(usePlatformAdminOverview());

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await platformAdmin.load(authStore.token);
}

async function withToken<T>(callback: (token: string) => Promise<T>) {
  if (!authStore.token) {
    return;
  }

  await callback(authStore.token);
}

async function handleSelectTenant(tenantId: string) {
  await withToken((token) => platformAdmin.selectTenant(tenantId, token));
}

async function handleUpdateFeatureToggles(payload: FeatureToggleUpdatePayload) {
  await withToken((token) => platformAdmin.submitFeatureToggles(payload, token));
}

async function handleSuspendTenant() {
  await withToken((token) => platformAdmin.submitSuspendTenant(token));
}

async function handleResumeTenant() {
  await withToken((token) => platformAdmin.submitResumeTenant(token));
}

async function handleRunAutomation() {
  await withToken((token) => platformAdmin.submitSubscriptionAutomation(token));
}

async function handleUpdateFilters(payload: {
  tenantId?: string;
  documentCode?: string;
  acceptanceStatus?: string;
}) {
  await withToken((token) => platformAdmin.updateAcceptanceFilters(payload, token));
}

async function handlePublishDocument(
  documentCode: string,
  payload: ComplianceDocumentPublishPayload
) {
  await withToken((token) =>
    platformAdmin.submitPublishComplianceDocument(documentCode, payload, token)
  );
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="platform-admin-page">
    <header class="platform-admin-page__header">
      <p class="platform-admin-page__eyebrow">平台运营</p>
      <h2 class="platform-admin-page__title">平台管理</h2>
      <p class="platform-admin-page__subtitle">
        平台治理、租户矩阵与就绪度
      </p>
    </header>

    <InlineErrorCard
      v-if="platformAdmin.errorMessage"
      :message="platformAdmin.errorMessage"
      @retry="bootstrapPage"
    />

    <PlatformAdminHeroPanel
      :summary="platformAdmin.summary"
      :growth-trend="platformAdmin.growthTrend"
      :trial-conversion="platformAdmin.trialConversion"
      :selected-tenant="platformAdmin.selectedTenant"
      :subscription-automation-summary="platformAdmin.subscriptionAutomationSummary"
      :action-feedback="platformAdmin.actionFeedback"
    />

    <div class="platform-admin-page__stack">
      <TenantMatrixPanel
        :tenants="platformAdmin.tenants"
        :selected-tenant-id="platformAdmin.selectedTenantId"
        :selected-tenant="platformAdmin.selectedTenant"
        :feature-presets="platformFeatureTogglePresets"
        :resolved-feature-flags="platformAdmin.resolvedFeatureFlags"
        :customer-success-follow-ups="platformAdmin.customerSuccessFollowUps"
        :churn-warnings="platformAdmin.churnWarnings"
        :is-submitting="platformAdmin.isRunningAction"
        :action-error="platformAdmin.actionError"
        @select-tenant="handleSelectTenant"
        @update-feature-toggles="handleUpdateFeatureToggles"
        @suspend-tenant="handleSuspendTenant"
        @resume-tenant="handleResumeTenant"
        @run-automation="handleRunAutomation"
      />

      <ReadinessCommandPanel
        :selected-tenant="platformAdmin.selectedTenant"
        :release-readiness="platformAdmin.releaseReadiness"
        :delivery-readiness="platformAdmin.deliveryReadiness"
        :observability-readiness="platformAdmin.observabilityReadiness"
        :is-refreshing="platformAdmin.isRefreshingReadiness"
      />

      <ComplianceGovernancePanel
        :documents="platformAdmin.complianceDocuments"
        :acceptances="platformAdmin.complianceAcceptances"
        :selected-tenant-id="platformAdmin.selectedTenantId"
        :filters="platformAdmin.acceptanceFilters"
        :is-submitting="platformAdmin.isRunningAction"
        :action-error="platformAdmin.actionError"
        @update-filters="handleUpdateFilters"
        @publish-document="handlePublishDocument"
      />
    </div>

    <p
      v-if="platformAdmin.isLoading || platformAdmin.isRefreshingReadiness"
      class="platform-admin-page__footer-note"
    >
      正在加载经营指标、跟进队列与治理状态...
    </p>
  </section>
</template>

<style scoped>
.platform-admin-page {
  display: grid;
  gap: 1.2rem;
}

.platform-admin-page__header {
  display: grid;
  gap: 0.35rem;
}

.platform-admin-page__eyebrow,
.platform-admin-page__title,
.platform-admin-page__subtitle,
.platform-admin-page__footer-note {
  margin: 0;
}

.platform-admin-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.platform-admin-page__title {
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
}

.platform-admin-page__subtitle,
.platform-admin-page__footer-note {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.platform-admin-page__stack {
  display: grid;
  gap: 1rem;
}
</style>
