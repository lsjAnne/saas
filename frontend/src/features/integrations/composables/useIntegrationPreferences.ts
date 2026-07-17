import { computed, ref, shallowRef } from 'vue';

import { updateTenantExternalIntegrationPreferences } from '@/services/tenantExternalIntegrationService';
import type {
  ExternalIntegrationOption,
  ExternalIntegrationPreferences
} from '@/services/apiTypes';

export function useIntegrationPreferences() {
  const options = ref<ExternalIntegrationOption[]>([]);
  const selectedSystemCodes = ref<string[]>([]);
  const initialSelectedSystemCodes = ref<string[]>([]);
  const featureFlags = ref<Record<string, boolean>>({});
  const isSaving = shallowRef(false);
  const lastSavedAt = shallowRef<string | null>(null);
  const preferencesError = shallowRef<string | null>(null);

  const hasChanges = computed(() => {
    const current = [...selectedSystemCodes.value].sort().join('|');
    const initial = [...initialSelectedSystemCodes.value].sort().join('|');
    return current !== initial;
  });

  function applyPreferences(payload: ExternalIntegrationPreferences) {
    options.value = payload.options;
    selectedSystemCodes.value = payload.options
      .filter((option) => option.selected)
      .map((option) => option.systemCode);
    initialSelectedSystemCodes.value = [...selectedSystemCodes.value];
    featureFlags.value = payload.featureFlags;
    preferencesError.value = null;
  }

  function setPreferencesError(message: string) {
    preferencesError.value = message;
  }

  function toggleSystem(systemCode: string) {
    const selected = new Set(selectedSystemCodes.value);

    if (selected.has(systemCode)) {
      selected.delete(systemCode);
    } else {
      selected.add(systemCode);
    }

    selectedSystemCodes.value = options.value
      .map((option) => option.systemCode)
      .filter((code) => selected.has(code));
  }

  function restoreRecommendedSelection() {
    const recommendedSet = new Set(['erp', 'messaging']);
    selectedSystemCodes.value = options.value
      .map((option) => option.systemCode)
      .filter((code) => recommendedSet.has(code));
  }

  async function save(token: string) {
    isSaving.value = true;
    preferencesError.value = null;

    try {
      const payload = await updateTenantExternalIntegrationPreferences(
        selectedSystemCodes.value,
        token
      );
      applyPreferences(payload);
      lastSavedAt.value = new Date().toISOString();
      return payload;
    } catch (error) {
      preferencesError.value =
        error instanceof Error ? error.message : '保存交付范围失败，请稍后重试';
      throw error;
    } finally {
      isSaving.value = false;
    }
  }

  return {
    options,
    selectedSystemCodes,
    featureFlags,
    isSaving,
    lastSavedAt,
    preferencesError,
    hasChanges,
    applyPreferences,
    setPreferencesError,
    toggleSystem,
    restoreRecommendedSelection,
    save
  };
}
