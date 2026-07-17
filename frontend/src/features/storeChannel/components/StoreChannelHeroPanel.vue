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
    eyebrow="Store & Channel"
    title="让店铺接入、渠道授权与阈值配置进入同一条交付主线"
    description="这一页只处理真正影响运营接入面的对象：店铺、渠道账号、授权时效与默认发货策略。动作后全部回读后端真相源，不在前端制造假成功。"
    dark
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
          <p class="store-hero__metric-label">Connected Stores</p>
          <strong class="store-hero__metric-value">
            {{ formatCount(summary.connectedStoreCount) }}
          </strong>
        </article>
        <article class="store-hero__metric">
          <p class="store-hero__metric-label">Platforms</p>
          <strong class="store-hero__metric-value">
            {{ formatCount(summary.activePlatformCount) }}
          </strong>
        </article>
        <article class="store-hero__metric">
          <p class="store-hero__metric-label">Policy Schemes</p>
          <strong class="store-hero__metric-value">
            {{ formatCount(summary.configuredShipSchemeCount) }}
          </strong>
        </article>
        <article class="store-hero__metric">
          <p class="store-hero__metric-label">Expiring Auth</p>
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
  color: rgba(255, 244, 236, 0.68);
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.store-hero__title {
  max-width: 14ch;
  color: #fffaf4;
  font-family: var(--font-display);
  font-size: clamp(1.9rem, 3vw, 2.6rem);
  line-height: 1.06;
}

.store-hero__meta {
  color: rgba(255, 244, 236, 0.76);
  line-height: 1.65;
}

.store-hero__feedback {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.08);
  color: #fff8f0;
  line-height: 1.65;
}

.store-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.store-hero__primary,
.store-hero__ghost {
  min-height: 3rem;
  padding: 0 1.15rem;
  border-radius: var(--radius-pill);
}

.store-hero__primary {
  border: 0;
  background: linear-gradient(135deg, #f5d6ae, #e5b881);
  color: #352617;
}

.store-hero__ghost {
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.04);
  color: #fff7ee;
}

.store-hero__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
}

.store-hero__metric {
  padding: 0.95rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.store-hero__metric-label,
.store-hero__metric-value {
  margin: 0;
}

.store-hero__metric-label {
  color: rgba(255, 244, 236, 0.62);
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.store-hero__metric-value {
  display: block;
  margin-top: 0.45rem;
  color: #fffaf4;
  font-size: 1.55rem;
}

.store-hero__platforms {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.store-hero__platform-chip {
  padding: 0.45rem 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 247, 238, 0.88);
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
