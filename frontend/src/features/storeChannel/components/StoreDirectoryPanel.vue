<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { Store } from '@/services/apiTypes';
import { formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  stores: Store[];
  selectedStore: Store | null;
  selectedStoreId: string | null;
}

defineProps<Props>();

defineEmits<{
  select: [storeId: string];
  edit: [];
}>();
</script>

<template>
  <PanelCard
    eyebrow="Directory"
    title="店铺目录与阈值详情"
    description="左侧保持店铺目录，右侧只展示当前选中的利润阈值、风险阈值与默认发货配置。高频切换在同一屏内完成，不打断主链路。"
  >
    <div class="store-directory">
      <div class="store-directory__list">
        <button
          v-for="store in stores"
          :key="store.storeId"
          type="button"
          :class="[
            'store-directory__item',
            { 'store-directory__item--active': store.storeId === selectedStoreId }
          ]"
          @click="$emit('select', store.storeId)"
        >
          <div class="store-directory__item-copy">
            <strong class="store-directory__item-title">{{ store.shopName }}</strong>
            <span class="store-directory__item-meta">
              {{ store.platformType }} / {{ store.platformShopId }}
            </span>
          </div>
          <StatusPill
            :label="store.authStatus"
            :tone="store.authStatus === 'connected' ? 'success' : 'warn'"
          />
        </button>

        <p v-if="!stores.length" class="store-directory__empty">
          还没有已接入店铺。可以先在右侧操作台发起第一条店铺接入。
        </p>
      </div>

      <div class="store-directory__detail">
        <template v-if="selectedStore">
          <div class="store-directory__detail-header">
            <div>
              <p class="store-directory__detail-label">Selected Store</p>
              <h4 class="store-directory__detail-title">{{ selectedStore.shopName }}</h4>
            </div>
            <button type="button" class="store-directory__detail-action" @click="$emit('edit')">
              编辑阈值
            </button>
          </div>

          <dl class="store-directory__facts">
            <div class="store-directory__fact">
              <dt>利润阈值</dt>
              <dd>{{ formatCurrency(selectedStore.profitThreshold) }}</dd>
            </div>
            <div class="store-directory__fact">
              <dt>风险阈值</dt>
              <dd>{{ formatCurrency(selectedStore.riskThreshold) }}</dd>
            </div>
            <div class="store-directory__fact">
              <dt>组织 / 店主</dt>
              <dd>{{ selectedStore.organizationId }} / {{ selectedStore.ownerUserId }}</dd>
            </div>
            <div class="store-directory__fact">
              <dt>接入时间</dt>
              <dd>{{ formatDateTime(selectedStore.createdAt) }}</dd>
            </div>
          </dl>

          <div class="store-directory__config">
            <p class="store-directory__detail-label">Default Ship Config</p>
            <pre class="store-directory__config-code">{{
              JSON.stringify(selectedStore.defaultShipConfig ?? {}, null, 2)
            }}</pre>
          </div>
        </template>

        <div v-else class="store-directory__placeholder">
          选择一间店铺后，这里会显示它的阈值配置、归属关系与默认发货策略。
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.store-directory {
  display: grid;
  grid-template-columns: 0.92fr 1.08fr;
  gap: 0.95rem;
}

.store-directory__list,
.store-directory__detail {
  display: grid;
  gap: 0.8rem;
}

.store-directory__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.9rem;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.7);
  text-align: left;
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.store-directory__item:hover,
.store-directory__item--active {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(58, 43, 28, 0.08);
}

.store-directory__item--active {
  border-color: rgba(133, 98, 67, 0.34);
  background: linear-gradient(135deg, rgba(250, 242, 233, 0.94), rgba(245, 232, 213, 0.88));
}

.store-directory__item-copy {
  display: grid;
  gap: 0.25rem;
}

.store-directory__item-title,
.store-directory__item-meta,
.store-directory__detail-label,
.store-directory__detail-title,
.store-directory__empty,
.store-directory__placeholder {
  margin: 0;
}

.store-directory__item-title,
.store-directory__detail-title {
  color: var(--color-ink-strong);
}

.store-directory__item-meta,
.store-directory__detail-label,
.store-directory__empty,
.store-directory__placeholder {
  color: var(--color-ink-soft);
}

.store-directory__detail {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(248, 244, 238, 0.8);
}

.store-directory__detail-header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 0.85rem;
}

.store-directory__detail-label {
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.store-directory__detail-title {
  margin-top: 0.4rem;
  font-family: var(--font-display);
  font-size: 1.6rem;
}

.store-directory__detail-action {
  min-height: 2.8rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-ink);
}

.store-directory__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
  margin: 0;
}

.store-directory__fact {
  display: grid;
  gap: 0.35rem;
  padding: 0.8rem 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.72);
}

.store-directory__fact dt {
  color: var(--color-ink-soft);
  font-size: 0.8rem;
}

.store-directory__fact dd {
  margin: 0;
  color: var(--color-ink-strong);
}

.store-directory__config {
  display: grid;
  gap: 0.55rem;
}

.store-directory__config-code {
  margin: 0;
  padding: 0.95rem;
  border-radius: var(--radius-lg);
  background: #2f261e;
  color: #fff8f0;
  overflow: auto;
  font-size: 0.8rem;
  line-height: 1.6;
}

.store-directory__empty,
.store-directory__placeholder {
  line-height: 1.7;
}

@media (max-width: 1120px) {
  .store-directory {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .store-directory__facts {
    grid-template-columns: 1fr;
  }
}
</style>
