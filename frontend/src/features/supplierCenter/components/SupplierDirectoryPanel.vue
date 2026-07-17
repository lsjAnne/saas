<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { Store, Supplier } from '@/services/apiTypes';
import { formatDate } from '@/utils/formatters';

interface Props {
  stores: Store[];
  suppliers: Supplier[];
  selectedSupplierId: string | null;
  activeStoreId: string;
}

defineProps<Props>();

defineEmits<{
  selectStore: [storeId: string];
  selectSupplier: [supplierId: string];
}>();

function toneFromRiskLevel(riskLevel: string | null | undefined) {
  return (riskLevel ?? '').toLowerCase().includes('high') ? 'warn' : 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Supplier Directory"
    title="供应商主档"
    description="先选店铺，再在当前店铺下切换供应商，右侧所有流程与摘要都跟随主档变化。"
  >
    <div class="supplier-directory">
      <div class="supplier-directory__filters">
        <button
          type="button"
          :class="['supplier-directory__filter', { 'supplier-directory__filter--active': activeStoreId === 'all' }]"
          @click="$emit('selectStore', 'all')"
        >
          全部店铺
        </button>
        <button
          v-for="store in stores"
          :key="store.storeId"
          type="button"
          :class="['supplier-directory__filter', { 'supplier-directory__filter--active': activeStoreId === store.storeId }]"
          @click="$emit('selectStore', store.storeId)"
        >
          {{ store.shopName }}
        </button>
      </div>

      <div v-if="suppliers.length" class="supplier-directory__list">
        <article
          v-for="supplier in suppliers"
          :key="supplier.supplierId"
          :class="['supplier-directory__item', { 'supplier-directory__item--active': selectedSupplierId === supplier.supplierId }]"
          @click="$emit('selectSupplier', supplier.supplierId)"
        >
          <div class="supplier-directory__item-head">
            <div>
              <h4 class="supplier-directory__item-title">{{ supplier.supplierName }}</h4>
              <p class="supplier-directory__item-meta">
                {{ supplier.supplierPlatformType }} · {{ supplier.supplierPlatformId }}
              </p>
            </div>
            <StatusPill :label="supplier.riskLevel || 'normal'" :tone="toneFromRiskLevel(supplier.riskLevel)" />
          </div>

          <div class="supplier-directory__item-tags">
            <span v-if="supplier.primary">主供应商</span>
            <span v-if="supplier.backup">备选供应商</span>
            <span v-if="supplier.dropshipSupportFlag">支持代发</span>
            <span v-if="supplier.blacklistFlag">已拉黑</span>
          </div>

          <div class="supplier-directory__scores">
            <span>价格 {{ supplier.priceScore ?? '--' }}</span>
            <span>交付 {{ supplier.deliveryScore ?? '--' }}</span>
            <span>稳定 {{ supplier.stabilityScore ?? '--' }}</span>
          </div>

          <p class="supplier-directory__item-date">接入时间 {{ formatDate(supplier.createdAt) }}</p>
        </article>
      </div>

      <div v-else class="supplier-directory__empty">
        当前筛选下还没有供应商主档，先在右侧创建第一条供应商记录。
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.supplier-directory,
.supplier-directory__list {
  display: grid;
  gap: 0.9rem;
}

.supplier-directory__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.supplier-directory__filter {
  min-height: 2.2rem;
  padding: 0 0.95rem;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.6);
  color: var(--color-ink);
}

.supplier-directory__filter--active {
  background: rgba(133, 98, 67, 0.12);
  border-color: rgba(133, 98, 67, 0.22);
  color: var(--color-ink-strong);
}

.supplier-directory__item {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  transition:
    transform var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.supplier-directory__item:hover {
  transform: translateY(-1px);
  border-color: rgba(133, 98, 67, 0.18);
}

.supplier-directory__item--active {
  border-color: rgba(133, 98, 67, 0.32);
  box-shadow: inset 0 0 0 1px rgba(133, 98, 67, 0.14);
}

.supplier-directory__item-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.supplier-directory__item-title,
.supplier-directory__item-meta,
.supplier-directory__item-date {
  margin: 0;
}

.supplier-directory__item-title {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.supplier-directory__item-meta,
.supplier-directory__item-date {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.supplier-directory__item-tags,
.supplier-directory__scores {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.supplier-directory__item-tags span,
.supplier-directory__scores span {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(217, 192, 163, 0.18);
  color: var(--color-ink);
  font-size: 0.8rem;
}

.supplier-directory__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.64);
  color: var(--color-ink-soft);
  line-height: 1.7;
}
</style>
