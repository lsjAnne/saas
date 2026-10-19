<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import IntegrationOptionCard from '@/features/integrations/components/IntegrationOptionCard.vue';
import type {
  ExternalIntegrationOption,
  IntegrationReadinessDescriptor
} from '@/services/apiTypes';

interface Props {
  options: ExternalIntegrationOption[];
  selectedSystemCodes: string[];
  selectedCount: number;
  readyCount: number;
  isSaving: boolean;
  hasChanges: boolean;
  errorMessage?: string | null;
  observabilityError?: string | null;
  lastSavedAt?: string | null;
  readinessMap: Record<string, IntegrationReadinessDescriptor>;
}

const props = withDefaults(defineProps<Props>(), {
  errorMessage: null,
  observabilityError: null,
  lastSavedAt: null
});

defineEmits<{
  retry: [];
  toggle: [systemCode: string];
  restore: [];
  save: [];
}>();

const saveStatus = computed(() => {
  if (!props.lastSavedAt) {
    return '尚未保存新变更';
  }

  return `最近保存于 ${new Date(props.lastSavedAt).toLocaleTimeString('zh-CN')}`;
});
</script>

<template>
  <PanelCard
    title="外部软件可选配置"
    description="配置租户需要的外部软件交付范围，开关即是否纳入对应接入门禁。"
  >
    <div class="integration-section">
      <div class="integration-section__summary">
        <div class="integration-section__summary-card">
          <p class="integration-section__summary-label">已选系统</p>
          <strong class="integration-section__summary-value">{{ selectedCount }}</strong>
          <p class="integration-section__summary-caption">当前纳入交付范围的系统数量</p>
        </div>

        <div class="integration-section__summary-card">
          <p class="integration-section__summary-label">已就绪</p>
          <strong class="integration-section__summary-value">{{ readyCount }}</strong>
          <p class="integration-section__summary-caption">当前已满足 readiness 的系统数量</p>
        </div>

        <div class="integration-section__summary-card">
          <p class="integration-section__summary-label">保存状态</p>
          <strong class="integration-section__summary-value integration-section__summary-value--small">
            {{ saveStatus }}
          </strong>
          <p class="integration-section__summary-caption">保存后会刷新偏好与 readiness 摘要</p>
        </div>
      </div>

      <InlineErrorCard
        v-if="errorMessage"
        :message="errorMessage"
        @retry="$emit('retry')"
      />

      <div v-if="observabilityError" class="integration-section__soft-notice">
        readiness 摘要暂时不可用，你仍然可以调整交付范围并保存。
      </div>

      <div class="integration-section__grid">
        <IntegrationOptionCard
          v-for="option in options"
          :key="option.systemCode"
          :option="option"
          :selected="selectedSystemCodes.includes(option.systemCode)"
          :readiness="readinessMap[option.systemCode]"
          @toggle="$emit('toggle', $event)"
        />
      </div>

      <div class="integration-section__actions">
        <button type="button" class="integration-section__primary" :disabled="!hasChanges || isSaving" @click="$emit('save')">
          {{ isSaving ? '保存中...' : '保存交付范围' }}
        </button>
        <button type="button" class="integration-section__ghost" @click="$emit('restore')">
          恢复默认推荐
        </button>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.integration-section {
  display: grid;
  gap: 1rem;
}

.integration-section__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.9rem;
}

.integration-section__summary-card {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.integration-section__summary-label,
.integration-section__summary-caption {
  margin: 0;
  color: var(--color-ink-soft);
}

.integration-section__summary-label {
  font-size: 0.78rem;
  font-weight: 600;
}

.integration-section__summary-value {
  display: block;
  margin-top: 0.55rem;
  color: var(--color-ink-strong);
  font-size: 1.8rem;
}

.integration-section__summary-value--small {
  font-size: 1rem;
  line-height: 1.5;
}

.integration-section__summary-caption {
  margin-top: 0.35rem;
  font-size: 0.83rem;
  line-height: 1.6;
}

.integration-section__soft-notice {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.integration-section__grid {
  display: grid;
  gap: 0.9rem;
}

.integration-section__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.integration-section__primary,
.integration-section__ghost {
  min-height: 2.5rem;
  padding: 0 0.95rem;
  border-radius: var(--radius-md);
  font: inherit;
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast),
    background var(--transition-fast);
}

.integration-section__primary {
  border: 0;
  background: var(--color-accent);
  color: #ffffff;
}

.integration-section__primary:hover {
  background: var(--color-accent-hover);
  color: #ffffff;
}

.integration-section__primary:disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

.integration-section__ghost {
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
}

.integration-section__ghost:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

@media (max-width: 820px) {
  .integration-section__summary {
    grid-template-columns: 1fr;
  }
}
</style>
