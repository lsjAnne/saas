<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { Store } from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface Props {
  selectedStore: Store | null;
  actionFeedback?: string | null;
  summary: {
    connectedStoreCount: number;
    activePlatformCount: number;
    configuredShipSchemeCount: number;
    expiringAccountCount: number;
  };
  platformBreakdown: Array<{
    platformType: string;
    count: number;
  }>;
}

withDefaults(defineProps<Props>(), {
  actionFeedback: null
});

defineEmits<{
  openStore: [];
  openChannel: [];
}>();
</script>

<template>
  <PanelCard
    title="店铺与渠道概览"
    description="店铺接入、渠道账号授权与发货策略的整体状态。"
  >
    <div class="store-hero">
      <div class="store-hero__primary">
        <div class="store-hero__headline">
          <p class="store-hero__label">当前聚焦店铺</p>
          <h4 class="store-hero__title">
            {{ selectedStore?.shopName ?? '先选择店铺，或直接从这里发起接入' }}
          </h4>
          <div class="store-hero__status">
            <StatusPill
              :label="selectedStore?.authStatus ?? 'waiting-selection'"
              :tone="selectedStore ? 'success' : 'neutral'"
            />
            <span class="store-hero__meta">
              {{
                selectedStore
                  ? `${selectedStore.platformType} / ${selectedStore.platformShopId}`
                  : '支持按组织维度管理店铺与渠道账号'
              }}
            </span>
          </div>
        </div>

        <div v-if="actionFeedback" class="store-hero__feedback">
          {{ actionFeedback }}
        </div>

        <div class="store-hero__actions">
          <button type="button" class="store-hero__primary" @click="$emit('openStore')">
            接入新店铺
          </button>
          <button type="button" class="store-hero__ghost" @click="$emit('openChannel')">
            新增渠道账号
          </button>
        </div>
      </div>

      <div class="store-hero__metrics">
        <article class="store-hero__metric">
          <p class="store-hero__metric-label">已接入店铺</p>
          <strong class="store-hero__metric-value">
            {{ formatCount(summary.connectedStoreCount) }}
          </strong>
        </article>
        <article class="store-hero__metric">
          <p class="store-hero__metric-label">活跃平台</p>
          <strong class="store-hero__metric-value">
            {{ formatCount(summary.activePlatformCount) }}
          </strong>
        </article>
        <article class="store-hero__metric">
          <p class="store-hero__metric-label">发货策略</p>
          <strong class="store-hero__metric-value">
            {{ formatCount(summary.configuredShipSchemeCount) }}
          </strong>
        </article>
        <article class="store-hero__metric">
          <p class="store-hero__metric-label">临期授权</p>
          <strong class="store-hero__metric-value">
            {{ formatCount(summary.expiringAccountCount) }}
          </strong>
        </article>
      </div>

      <div class="store-hero__platforms">
        <span
          v-for="platform in platformBreakdown"
          :key="platform.platformType"
          class="store-hero__platform-chip"
        >
          {{ platform.platformType }} · {{ platform.count }}
        </span>
        <span v-if="!platformBreakdown.length" class="store-hero__platform-chip">
          暂无已接入平台
        </span>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.store-hero {
  display: grid;
  gap: 1rem;
}

.store-hero__primary {
  display: grid;
  gap: 0.9rem;
}

.store-hero__headline,
.store-hero__status {
  display: grid;
  gap: 0.55rem;
}

.store-hero__label,
.store-hero__title,
.store-hero__meta {
  margin: 0;
}

.store-hero__label {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.store-hero__title {
  color: var(--color-ink-strong);
  font-size: 1.35rem;
  line-height: 1.3;
}

.store-hero__meta {
  color: var(--color-ink-soft);
  line-height: 1.65;
}

.store-hero__feedback {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: var(--color-accent-soft);
  color: var(--color-accent);
  line-height: 1.65;
}

.store-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.store-hero__primary,
.store-hero__ghost {
  min-height: 2.5rem;
  padding: 0 1rem;
  border-radius: var(--radius-md);
  font-size: 0.88rem;
}

.store-hero__primary {
  border: 0;
  background: var(--color-accent);
  color: #ffffff;
}

.store-hero__primary:hover {
  background: var(--color-accent-hover);
}

.store-hero__ghost {
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
}

.store-hero__ghost:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.store-hero__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
}

.store-hero__metric {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.store-hero__metric-label,
.store-hero__metric-value {
  margin: 0;
}

.store-hero__metric-label {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  font-weight: 600;
}

.store-hero__metric-value {
  display: block;
  margin-top: 0.4rem;
  color: var(--color-ink-strong);
  font-size: 1.5rem;
}

.store-hero__platforms {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.store-hero__platform-chip {
  padding: 0.4rem 0.75rem;
  border-radius: var(--radius-md);
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 0.82rem;
}

@media (max-width: 980px) {
  .store-hero__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .store-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
