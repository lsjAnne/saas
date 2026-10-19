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

const riskLevelLabelMap: Record<string, string> = {
  normal: '正常',
  low: '低风险',
  medium: '中风险',
  high: '高风险',
  critical: '紧急'
};

function resolveRiskLevelLabel(level: string | null | undefined) {
  return riskLevelLabelMap[(level ?? '').toLowerCase()] ?? level ?? '正常';
}
</script>

<template>
  <PanelCard
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
            <StatusPill :label="resolveRiskLevelLabel(supplier.riskLevel)" :tone="toneFromRiskLevel(supplier.riskLevel)" />
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
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
}

.supplier-directory__filter--active {
  background: var(--color-accent-soft);
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.supplier-directory__item {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
  cursor: pointer;
  transition:
    transform var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.supplier-directory__item:hover {
  transform: translateY(-1px);
  border-color: var(--color-accent);
}

.supplier-directory__item--active {
  border-color: var(--color-accent);
  box-shadow: var(--shadow-card);
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
  background: var(--color-accent-soft);
  color: var(--color-ink);
  font-size: 0.8rem;
}

.supplier-directory__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  color: var(--color-ink-soft);
  line-height: 1.7;
}
</style>
