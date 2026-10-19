<script setup lang="ts">
import { reactive, shallowRef, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { Store, UpdateStoreSettingsPayload } from '@/services/apiTypes';

interface Props {
  selectedStore: Store | null;
  isSubmitting: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  saveSettings: [payload: UpdateStoreSettingsPayload];
}>();

const localError = shallowRef<string | null>(null);

const settingsForm = reactive({
  profitThreshold: '0',
  riskThreshold: '0',
  defaultShipConfigText: '{}'
});

watch(
  () => props.selectedStore,
  (store) => {
    settingsForm.profitThreshold = String(store?.profitThreshold ?? 0);
    settingsForm.riskThreshold = String(store?.riskThreshold ?? 0);
    settingsForm.defaultShipConfigText = JSON.stringify(
      store?.defaultShipConfig ?? {},
      null,
      2
    );
    localError.value = null;
  },
  { immediate: true }
);

function toNumber(value: string, label: string) {
  const parsed = Number(value);

  if (Number.isNaN(parsed) || parsed < 0) {
    throw new Error(`${label} 必须是大于等于 0 的数字`);
  }

  return parsed;
}

function parseConfig(text: string) {
  const normalized = text.trim();

  if (!normalized) {
    return {};
  }

  const parsed = JSON.parse(normalized);

  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('默认发货配置必须是 JSON 对象');
  }

  return parsed as Record<string, unknown>;
}

function submitSettings() {
  if (!props.selectedStore) {
    return;
  }

  localError.value = null;

  try {
    emit('saveSettings', {
      profitThreshold: toNumber(settingsForm.profitThreshold, '利润阈值'),
      riskThreshold: toNumber(settingsForm.riskThreshold, '风险阈值'),
      defaultShipConfig: parseConfig(settingsForm.defaultShipConfigText)
    });
  } catch (error) {
    localError.value =
      error instanceof Error ? error.message : '初始化设置解析失败，请检查输入内容';
  }
}

function authStatusLabel(status: string) {
  const normalized = status.toLowerCase();
  if (normalized.includes('author')) {
    return '已授权';
  }
  if (normalized.includes('pending')) {
    return '待授权';
  }
  return '未授权';
}
</script>

<template>
  <PanelCard
    title="初始化设置区"
    description="把利润阈值、风险阈值和默认发货配置落成真实店铺设置。"
  >
    <div v-if="selectedStore" class="onboarding-init">
      <div class="onboarding-init__summary">
        <div>
          <p class="onboarding-init__label">当前店铺</p>
          <strong class="onboarding-init__title">{{ selectedStore.shopName }}</strong>
          <p class="onboarding-init__copy">
            {{ selectedStore.platformType }} · {{ selectedStore.platformShopId }}
          </p>
        </div>
        <StatusPill
          :label="authStatusLabel(selectedStore.authStatus)"
          :tone="selectedStore.authStatus.toLowerCase().includes('author') ? 'success' : 'warn'"
        />
      </div>

      <p v-if="localError" class="onboarding-init__error">{{ localError }}</p>

      <form class="onboarding-init__form" @submit.prevent="submitSettings">
        <label class="onboarding-init__field">
          <span>利润阈值</span>
          <input v-model="settingsForm.profitThreshold" type="number" min="0" step="0.01" required />
        </label>
        <label class="onboarding-init__field">
          <span>风险阈值</span>
          <input v-model="settingsForm.riskThreshold" type="number" min="0" step="0.01" required />
        </label>
        <label class="onboarding-init__field onboarding-init__field--full">
          <span>默认发货配置 JSON</span>
          <textarea v-model="settingsForm.defaultShipConfigText" rows="9" />
        </label>
        <button class="onboarding-init__primary" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '保存中...' : '完成初始化设置' }}
        </button>
      </form>
    </div>

    <p v-else class="onboarding-init__empty">
      先在上一个区域接入并选中一间店铺，这里才会出现可落地的初始化设置。
    </p>
  </PanelCard>
</template>

<style scoped>
.onboarding-init,
.onboarding-init__form,
.onboarding-init__field {
  display: grid;
  gap: 0.8rem;
}

.onboarding-init__summary {
  display: flex;
  gap: 0.8rem;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.onboarding-init__label,
.onboarding-init__title,
.onboarding-init__copy,
.onboarding-init__error,
.onboarding-init__empty {
  margin: 0;
}

.onboarding-init__label,
.onboarding-init__copy,
.onboarding-init__field span {
  color: var(--color-ink-soft);
  font-size: 0.84rem;
}

.onboarding-init__title {
  color: var(--color-ink-strong);
  font-size: 1.1rem;
}

.onboarding-init__error {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  background: rgba(183, 117, 47, 0.12);
  color: var(--color-warning);
}

.onboarding-init__form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.onboarding-init__field input,
.onboarding-init__field textarea {
  width: 100%;
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  background: #fff;
  padding: 0.8rem 0.9rem;
  color: var(--color-ink-strong);
  font: inherit;
}

.onboarding-init__field--full,
.onboarding-init__primary {
  grid-column: 1 / -1;
}

.onboarding-init__primary {
  min-height: 2.5rem;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--color-accent);
  color: #ffffff;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.onboarding-init__primary:hover {
  background: var(--color-accent-hover);
  color: #ffffff;
}

.onboarding-init__primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.onboarding-init__empty {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

@media (max-width: 860px) {
  .onboarding-init__summary,
  .onboarding-init__form {
    grid-template-columns: 1fr;
  }
}
</style>
