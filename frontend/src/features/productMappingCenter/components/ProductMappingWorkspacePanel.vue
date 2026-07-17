<script setup lang="ts">
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  ProductMappingCatalogItem,
  ProductMappingSkuRow
} from '@/features/productMappingCenter/composables/useProductMappingCenterOverview';
import type { Supplier } from '@/services/apiTypes';
import { formatDate } from '@/utils/formatters';

interface Props {
  selectedProduct: ProductMappingCatalogItem | null;
  selectedStoreName: string;
  suppliers: Supplier[];
  activePrimarySupplier: Supplier | null;
  backupSuppliers: Supplier[];
  skuRows: ProductMappingSkuRow[];
  isSubmitting: boolean;
  actionError?: string | null;
}

withDefaults(defineProps<Props>(), {
  actionError: null
});

defineEmits<{
  createPrimary: [supplierId: string];
  switchPrimary: [supplierId: string];
  createBackup: [supplierId: string];
}>();
</script>

<template>
  <section class="mapping-workspace">
    <div class="mapping-workspace__section">
      <div class="mapping-workspace__head">
        <div>
          <p class="mapping-workspace__eyebrow">Candidate Sources</p>
          <h4 class="mapping-workspace__title">货源候选区</h4>
        </div>
        <p class="mapping-workspace__hint">
          {{
            selectedProduct
              ? `店铺 ${selectedStoreName} 当前可选货源 ${suppliers.length} 个`
              : '先在左侧选择平台商品'
          }}
        </p>
      </div>

      <div v-if="selectedProduct?.productMissing" class="mapping-workspace__empty">
        当前商品只剩映射关系，主数据已经缺失。请先回商品中心补齐或重新发布商品，再处理主备货源。
      </div>
      <div
        v-else-if="selectedProduct && suppliers.length"
        class="mapping-workspace__supplier-grid"
      >
        <article
          v-for="supplier in suppliers"
          :key="supplier.supplierId"
          class="mapping-workspace__supplier-card"
        >
          <div class="mapping-workspace__supplier-head">
            <div>
              <h5 class="mapping-workspace__supplier-title">{{ supplier.supplierName }}</h5>
              <p class="mapping-workspace__supplier-meta">
                {{ supplier.supplierPlatformType }} 路 {{ supplier.supplierPlatformId }}
              </p>
            </div>
            <StatusPill
              :label="supplier.riskLevel || 'normal'"
              :tone="supplier.blacklistFlag ? 'warn' : 'neutral'"
            />
          </div>

          <div class="mapping-workspace__supplier-tags">
            <span v-if="supplier.dropshipSupportFlag">支持代发</span>
            <span v-if="activePrimarySupplier?.supplierId === supplier.supplierId">当前主货源</span>
            <span
              v-if="backupSuppliers.some((item) => item.supplierId === supplier.supplierId)"
            >
              备用已配置
            </span>
          </div>

          <div class="mapping-workspace__supplier-stats">
            <span>价格 {{ supplier.priceScore ?? '--' }}</span>
            <span>交付 {{ supplier.deliveryScore ?? '--' }}</span>
            <span>稳定 {{ supplier.stabilityScore ?? '--' }}</span>
          </div>

          <div class="mapping-workspace__supplier-actions">
            <button
              class="mapping-workspace__button mapping-workspace__button--solid"
              type="button"
              :disabled="isSubmitting"
              @click="$emit(activePrimarySupplier ? 'switchPrimary' : 'createPrimary', supplier.supplierId)"
            >
              {{ activePrimarySupplier ? '切换为主货源' : '建立主映射' }}
            </button>
            <button
              class="mapping-workspace__button"
              type="button"
              :disabled="isSubmitting"
              @click="$emit('createBackup', supplier.supplierId)"
            >
              设置备用货源
            </button>
          </div>

          <p class="mapping-workspace__supplier-date">接入时间 {{ formatDate(supplier.createdAt) }}</p>
        </article>
      </div>
      <div v-else class="mapping-workspace__empty">
        该商品所在店铺还没有可操作的候选货源，请先到供应商中心补齐货源池。
      </div>
    </div>

    <div class="mapping-workspace__split">
      <div class="mapping-workspace__section">
        <div class="mapping-workspace__head">
          <div>
            <p class="mapping-workspace__eyebrow">SKU Map</p>
            <h4 class="mapping-workspace__title">SKU 映射区</h4>
          </div>
          <p class="mapping-workspace__hint">
            当前仓库尚未拆出独立 SKU 实体，这里先按现有主备映射口径回看。
          </p>
        </div>

        <div v-if="skuRows.length" class="mapping-workspace__sku-list">
          <article v-for="row in skuRows" :key="row.key" class="mapping-workspace__sku-row">
            <div>
              <p class="mapping-workspace__sku-label">{{ row.mappingTypeLabel }}</p>
              <h5 class="mapping-workspace__sku-title">{{ row.platformSku }}</h5>
              <p class="mapping-workspace__sku-copy">{{ row.supplierName }} 路 {{ row.sourceSku }}</p>
            </div>
            <StatusPill :label="row.statusLabel" :tone="row.tone" />
          </article>
        </div>
        <div v-else class="mapping-workspace__empty">
          当前商品还没有映射记录，建立主货源后这里会回显映射状态。
        </div>
      </div>

      <div class="mapping-workspace__section">
        <div class="mapping-workspace__head">
          <div>
            <p class="mapping-workspace__eyebrow">Backup Pool</p>
            <h4 class="mapping-workspace__title">备用货源区</h4>
          </div>
          <p class="mapping-workspace__hint">主货源出现风险时，这里决定你能否无缝切换。</p>
        </div>

        <div v-if="backupSuppliers.length" class="mapping-workspace__backup-list">
          <article
            v-for="supplier in backupSuppliers"
            :key="supplier.supplierId"
            class="mapping-workspace__backup-card"
          >
            <div>
              <h5 class="mapping-workspace__supplier-title">{{ supplier.supplierName }}</h5>
              <p class="mapping-workspace__supplier-meta">
                {{ supplier.supplierPlatformType }} 路 {{ supplier.supplierPlatformId }}
              </p>
            </div>
            <div class="mapping-workspace__backup-actions">
              <StatusPill
                :label="supplier.riskLevel || 'normal'"
                :tone="supplier.blacklistFlag ? 'warn' : 'neutral'"
              />
              <button
                class="mapping-workspace__button"
                type="button"
                :disabled="isSubmitting || !!selectedProduct?.productMissing"
                @click="$emit('switchPrimary', supplier.supplierId)"
              >
                切为主货源
              </button>
            </div>
          </article>
        </div>
        <div v-else class="mapping-workspace__empty">
          还没有备用货源，建议至少补一条兜底线路。
        </div>

        <p v-if="actionError" class="mapping-workspace__error">{{ actionError }}</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.mapping-workspace,
.mapping-workspace__section,
.mapping-workspace__split,
.mapping-workspace__supplier-grid,
.mapping-workspace__sku-list,
.mapping-workspace__backup-list {
  display: grid;
  gap: 0.9rem;
}

.mapping-workspace__section {
  padding: 1.15rem;
  border-radius: var(--radius-shell);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: var(--shadow-card);
}

.mapping-workspace__split {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.mapping-workspace__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
}

.mapping-workspace__eyebrow,
.mapping-workspace__title,
.mapping-workspace__hint,
.mapping-workspace__supplier-title,
.mapping-workspace__supplier-meta,
.mapping-workspace__supplier-date,
.mapping-workspace__sku-label,
.mapping-workspace__sku-title,
.mapping-workspace__sku-copy,
.mapping-workspace__error {
  margin: 0;
}

.mapping-workspace__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.mapping-workspace__title {
  margin-top: 0.35rem;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: 1.32rem;
}

.mapping-workspace__hint,
.mapping-workspace__supplier-meta,
.mapping-workspace__supplier-date,
.mapping-workspace__sku-copy {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.mapping-workspace__supplier-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.mapping-workspace__supplier-card,
.mapping-workspace__sku-row,
.mapping-workspace__backup-card {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background:
    linear-gradient(155deg, rgba(255, 255, 255, 0.95), rgba(244, 239, 233, 0.94));
}

.mapping-workspace__supplier-head,
.mapping-workspace__sku-row,
.mapping-workspace__backup-card,
.mapping-workspace__backup-actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.mapping-workspace__supplier-tags,
.mapping-workspace__supplier-stats,
.mapping-workspace__supplier-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.mapping-workspace__supplier-tags span,
.mapping-workspace__supplier-stats span {
  display: inline-flex;
  min-height: 2rem;
  align-items: center;
  padding: 0 0.75rem;
  border-radius: var(--radius-pill);
  background: rgba(91, 72, 54, 0.08);
  color: var(--color-ink-soft);
  font-size: 0.8rem;
}

.mapping-workspace__button {
  min-height: 2.5rem;
  padding: 0 0.95rem;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(91, 72, 54, 0.16);
  background: rgba(255, 255, 255, 0.6);
  color: var(--color-ink-strong);
}

.mapping-workspace__button--solid {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: #fff8f0;
}

.mapping-workspace__sku-label {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.mapping-workspace__sku-title {
  margin-top: 0.45rem;
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.mapping-workspace__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(91, 72, 54, 0.05);
  color: var(--color-ink-soft);
  line-height: 1.72;
}

.mapping-workspace__error {
  color: var(--color-danger);
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .mapping-workspace__split,
  .mapping-workspace__supplier-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 780px) {
  .mapping-workspace__head,
  .mapping-workspace__supplier-head,
  .mapping-workspace__sku-row,
  .mapping-workspace__backup-card,
  .mapping-workspace__backup-actions {
    display: grid;
  }
}
</style>
