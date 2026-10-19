<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import IntegrationPreferenceSection from '@/features/integrations/components/IntegrationPreferenceSection.vue';
import { useIntegrationPreferences } from '@/features/integrations/composables/useIntegrationPreferences';
import { useObservabilityReadiness } from '@/features/integrations/composables/useObservabilityReadiness';
import ComplianceDocumentPanel from '@/features/systemSettings/components/ComplianceDocumentPanel.vue';
import SystemReadinessPanel from '@/features/systemSettings/components/SystemReadinessPanel.vue';
import { useSystemSettingsOverview } from '@/features/systemSettings/composables/useSystemSettingsOverview';
import { getTenantExternalIntegrationPreferences } from '@/services/tenantExternalIntegrationService';
import { getTenantObservabilityOverview } from '@/services/tenantObservabilityService';
import type { IntegrationReadinessDescriptor } from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const systemSettingsOverview = useSystemSettingsOverview();
const integrationPreferences = useIntegrationPreferences();
const observabilityReadiness = useObservabilityReadiness();
const isBootstrapping = shallowRef(false);

authStore.hydrate();

const readinessMap = computed<Record<string, IntegrationReadinessDescriptor>>(() =>
  Object.fromEntries(
    integrationPreferences.options.map((option) => [
      option.systemCode,
      observabilityReadiness.describeSystem(
        option.systemCode,
        integrationPreferences.selectedSystemCodes
      )
    ])
  )
);

async function loadPreferences() {
  if (!authStore.token) {
    return;
  }

  const payload = await getTenantExternalIntegrationPreferences(authStore.token);
  integrationPreferences.applyPreferences(payload);
}

async function loadObservability() {
  if (!authStore.token) {
    return;
  }

  try {
    const payload = await getTenantObservabilityOverview(authStore.token);
    observabilityReadiness.setOverview(payload);
  } catch (error) {
    observabilityReadiness.setObservabilityError(
      error instanceof Error ? error.message : '系统设置中的 readiness 摘要暂时不可用'
    );
  }
}

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  isBootstrapping.value = true;

  try {
    await Promise.all([
      loadPreferences(),
      loadObservability(),
      systemSettingsOverview.load(authStore.token)
    ]);
  } finally {
    isBootstrapping.value = false;
  }
}

async function handleSave() {
  if (!authStore.token) {
    return;
  }

  await integrationPreferences.save(authStore.token);
  await loadObservability();
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="system-settings-page">
    <header class="system-settings-page__header">
      <p class="system-settings-page__eyebrow">偏好配置</p>
      <h2 class="system-settings-page__title">系统设置</h2>
      <p class="system-settings-page__subtitle">
        合规文档、就绪度与外部集成配置
      </p>
    </header>

    <InlineErrorCard
      v-if="systemSettingsOverview.errorMessage"
      :message="systemSettingsOverview.errorMessage"
      @retry="bootstrapPage"
    />

    <div class="system-settings-page__grid">
      <SystemReadinessPanel :overview="observabilityReadiness.overview" />
      <ComplianceDocumentPanel
        :privacy-policy="systemSettingsOverview.privacyPolicy"
        :user-agreement="systemSettingsOverview.userAgreement"
        :acceptances="systemSettingsOverview.acceptances"
      />
    </div>

    <IntegrationPreferenceSection
      :options="integrationPreferences.options"
      :selected-system-codes="integrationPreferences.selectedSystemCodes"
      :selected-count="observabilityReadiness.selectedCount"
      :ready-count="observabilityReadiness.readyCount"
      :is-saving="integrationPreferences.isSaving"
      :has-changes="integrationPreferences.hasChanges"
      :error-message="integrationPreferences.preferencesError"
      :observability-error="observabilityReadiness.observabilityError"
      :last-saved-at="integrationPreferences.lastSavedAt"
      :readiness-map="readinessMap"
      @retry="bootstrapPage"
      @toggle="integrationPreferences.toggleSystem"
      @restore="integrationPreferences.restoreRecommendedSelection"
      @save="handleSave"
    />

    <p v-if="isBootstrapping" class="system-settings-page__note">
      正在同步系统设置、合规文档和 readiness 摘要...
    </p>
  </section>
</template>

<style scoped>
.system-settings-page {
  display: grid;
  gap: 1.2rem;
}

.system-settings-page__header {
  display: grid;
  gap: 0.35rem;
}

.system-settings-page__eyebrow,
.system-settings-page__title,
.system-settings-page__subtitle,
.system-settings-page__note {
  margin: 0;
}

.system-settings-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.system-settings-page__title {
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
}

.system-settings-page__subtitle,
.system-settings-page__note {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.system-settings-page__grid {
  display: grid;
  grid-template-columns: 1.02fr 0.98fr;
  gap: 1rem;
}

@media (max-width: 1080px) {
  .system-settings-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
