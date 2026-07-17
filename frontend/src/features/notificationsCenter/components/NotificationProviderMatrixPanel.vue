<script setup lang="ts">
import { computed, shallowRef, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  NotificationGatewayProviderConfigView,
  NotificationGatewayProviderView,
  UpdateNotificationGatewayProviderPayload
} from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface Props {
  providers: NotificationGatewayProviderConfigView[];
  providerStats: NotificationGatewayProviderView[];
  isSubmitting: boolean;
}

interface Emits {
  saveProviders: [providers: UpdateNotificationGatewayProviderPayload[]];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const providerDrafts = shallowRef<UpdateNotificationGatewayProviderPayload[]>([]);

watch(
  () => props.providers,
  (providers) => {
    providerDrafts.value = providers.map((provider) => ({
      gatewayCode: provider.gatewayCode,
      notifyType: provider.notifyType,
      enabled: provider.enabled,
      mockMode: provider.mockMode,
      receiptSupported: provider.receiptSupported,
      endpoint: provider.endpoint,
      receiptCallbackPath: provider.receiptCallbackPath,
      description: provider.description
    }));
  },
  { immediate: true, deep: true }
);

const providerRows = computed(() =>
  providerDrafts.value.map((provider) => {
    const stats = props.providerStats.find((item) => item.gatewayCode === provider.gatewayCode);

    return {
      draft: provider,
      routedTaskCount: stats?.routedTaskCount ?? 0,
      deliveredTaskCount: stats?.deliveredTaskCount ?? 0,
      failedTaskCount: stats?.failedTaskCount ?? 0
    };
  })
);

function handleSave() {
  emit('saveProviders', providerDrafts.value);
}
</script>

<template>
  <PanelCard
    eyebrow="Provider Controls"
    title="网关 Provider 管理"
    description="集中维护 provider 的启用状态、Mock 标识、回执能力与目标地址。这里保存后，新的通知会按最新网关状态派发。"
  >
    <div class="provider-matrix__topbar">
      <button
        class="provider-matrix__save"
        type="button"
        :disabled="isSubmitting"
        @click="handleSave"
      >
        {{ isSubmitting ? '保存中...' : '保存 Provider 配置' }}
      </button>
    </div>

    <div v-if="providerRows.length" class="provider-matrix">
      <article
        v-for="provider in providerRows"
        :key="provider.draft.gatewayCode"
        class="provider-matrix__card"
      >
        <div class="provider-matrix__header">
          <div>
            <p class="provider-matrix__name">{{ provider.draft.gatewayCode }}</p>
            <p class="provider-matrix__type">{{ provider.draft.notifyType }}</p>
          </div>
          <div class="provider-matrix__pills">
            <StatusPill
              :label="provider.draft.enabled ? '已启用' : '已停用'"
              :tone="provider.draft.enabled ? 'success' : 'neutral'"
            />
            <StatusPill
              :label="provider.draft.mockMode ? 'Mock' : 'Live'"
              :tone="provider.draft.mockMode ? 'warn' : 'success'"
            />
          </div>
        </div>

        <div class="provider-matrix__toggles">
          <label class="provider-matrix__toggle">
            <input v-model="provider.draft.enabled" type="checkbox" />
            <span>启用 provider</span>
          </label>
          <label class="provider-matrix__toggle">
            <input v-model="provider.draft.mockMode" type="checkbox" />
            <span>使用 Mock 模式</span>
          </label>
          <label class="provider-matrix__toggle">
            <input v-model="provider.draft.receiptSupported" type="checkbox" />
            <span>支持回执</span>
          </label>
        </div>

        <label class="provider-matrix__field">
          <span class="provider-matrix__field-label">Endpoint</span>
          <input v-model="provider.draft.endpoint" class="provider-matrix__input" type="text" />
        </label>

        <label class="provider-matrix__field">
          <span class="provider-matrix__field-label">Receipt Callback Path</span>
          <input
            v-model="provider.draft.receiptCallbackPath"
            class="provider-matrix__input"
            type="text"
          />
        </label>

        <label class="provider-matrix__field">
          <span class="provider-matrix__field-label">Description</span>
          <textarea
            v-model="provider.draft.description"
            class="provider-matrix__textarea"
            rows="3"
          />
        </label>

        <div class="provider-matrix__metrics">
          <article class="provider-matrix__metric">
            <p class="provider-matrix__metric-label">路由量</p>
            <h5 class="provider-matrix__metric-value">
              {{ formatCount(provider.routedTaskCount, ' 条') }}
            </h5>
          </article>
          <article class="provider-matrix__metric">
            <p class="provider-matrix__metric-label">送达量</p>
            <h5 class="provider-matrix__metric-value">
              {{ formatCount(provider.deliveredTaskCount, ' 条') }}
            </h5>
          </article>
          <article class="provider-matrix__metric">
            <p class="provider-matrix__metric-label">失败量</p>
            <h5 class="provider-matrix__metric-value">
              {{ formatCount(provider.failedTaskCount, ' 条') }}
            </h5>
          </article>
        </div>
      </article>
    </div>

    <p v-else class="provider-matrix__empty">当前没有可维护的通知 provider。</p>
  </PanelCard>
</template>

<style scoped>
.provider-matrix__topbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1rem;
}

.provider-matrix__save {
  min-width: 10rem;
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.72rem 1.15rem;
  background: #8f5f3a;
  color: #fff8f0;
  font: inherit;
  cursor: pointer;
}

.provider-matrix__save:disabled {
  cursor: wait;
  opacity: 0.7;
}

.provider-matrix {
  display: grid;
  gap: 0.85rem;
}

.provider-matrix__card {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background:
    radial-gradient(circle at top right, rgba(197, 170, 137, 0.14), transparent 42%),
    rgba(255, 255, 255, 0.76);
}

.provider-matrix__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.provider-matrix__pills,
.provider-matrix__toggles {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.provider-matrix__name,
.provider-matrix__type,
.provider-matrix__metric-label,
.provider-matrix__metric-value,
.provider-matrix__empty,
.provider-matrix__field-label {
  margin: 0;
}

.provider-matrix__name,
.provider-matrix__metric-value {
  color: var(--color-ink-strong);
}

.provider-matrix__name {
  font-size: 1.08rem;
}

.provider-matrix__type,
.provider-matrix__empty,
.provider-matrix__field-label {
  color: var(--color-ink-soft);
}

.provider-matrix__type {
  margin-top: 0.3rem;
}

.provider-matrix__toggle,
.provider-matrix__field {
  display: grid;
  gap: 0.4rem;
}

.provider-matrix__toggle {
  grid-auto-flow: column;
  justify-content: start;
  align-items: center;
  color: var(--color-ink-soft);
}

.provider-matrix__field-label {
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.provider-matrix__input,
.provider-matrix__textarea {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: calc(var(--radius-lg) - 0.25rem);
  padding: 0.7rem 0.8rem;
  background: rgba(255, 251, 247, 0.92);
  color: var(--color-ink-strong);
  font: inherit;
  box-sizing: border-box;
}

.provider-matrix__textarea {
  resize: vertical;
}

.provider-matrix__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.provider-matrix__metric {
  padding: 0.8rem;
  border-radius: calc(var(--radius-lg) - 0.25rem);
  background: rgba(255, 251, 247, 0.86);
  border: 1px solid rgba(91, 72, 54, 0.06);
}

.provider-matrix__metric-label {
  color: var(--color-ink-faint);
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.provider-matrix__metric-value {
  margin-top: 0.45rem;
  font-size: 1.25rem;
}

@media (max-width: 720px) {
  .provider-matrix__header,
  .provider-matrix__metrics {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
