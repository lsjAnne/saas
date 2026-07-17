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
      <p class="platform-admin-page__eyebrow">SaaS Operations</p>
      <h2 class="platform-admin-page__title">
        Run the commercial SaaS portfolio from one operating console
      </h2>
      <p class="platform-admin-page__subtitle">
        This view shifts the platform page from governance-only tooling to an operations cockpit:
        growth, conversion, churn risk, and customer-success follow-up stay on top, while readiness
        and compliance controls remain available below.
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
      Loading portfolio metrics, follow-up queues, and governance status...
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
  gap: 0.75rem;
}

.platform-admin-page__eyebrow,
.platform-admin-page__title,
.platform-admin-page__subtitle,
.platform-admin-page__footer-note {
  margin: 0;
}

.platform-admin-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.platform-admin-page__title {
  max-width: 18ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 3.8rem);
  line-height: 1.04;
}

.platform-admin-page__subtitle,
.platform-admin-page__footer-note {
  max-width: 68rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.platform-admin-page__stack {
  display: grid;
  gap: 1rem;
}
</style>
