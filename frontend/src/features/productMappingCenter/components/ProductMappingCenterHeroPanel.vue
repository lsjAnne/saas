<script setup lang="ts">
import MetricCard from '@/components/cards/MetricCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ProductMappingCatalogItem } from '@/features/productMappingCenter/composables/useProductMappingCenterOverview';
import type { Supplier } from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface Summary {
  totalProducts: number;
  mappedProducts: number;
  primaryGapProducts: number;
  backupCoveredProducts: number;
  riskyMappings: number;
}

interface Props {
  selectedProduct: ProductMappingCatalogItem | null;
  selectedStoreName: string;
  summary: Summary;
  activePrimarySupplier: Supplier | null;
  backupSuppliers: Supplier[];
  actionFeedback?: string | null;
}

withDefaults(defineProps<Props>(), {
  actionFeedback: null
});

defineEmits<{
  openPrimary: [];
  openBackup: [];
}>();
</script>

<template>
  <section class="mapping-hero">
    <div class="mapping-hero__copy">
      <p class="mapping-hero__eyebrow">货源映射</p>
      <h3 class="mapping-hero__title">
        主备货源与映射校验一屏编排
      </h3>
      <p class="mapping-hero__subtitle">
        当前聚焦店铺：{{ selectedStoreName || '待选择商品' }}，主货源落位、备用兜底与风险复核在同一操作链完成。
      </p>

      <div class="mapping-hero__actions">
        <button
          class="mapping-hero__button mapping-hero__button--solid"
          type="button"
          @click="$emit('openPrimary')"
        >
          建立主映射
        </button>
        <button class="mapping-hero__button" type="button" @click="$emit('openBackup')">
          设置备用货源
        </button>
      </div>

      <p v-if="actionFeedback" class="mapping-hero__feedback">{{ actionFeedback }}</p>
    </div>

    <div class="mapping-hero__focus">
      <div class="mapping-hero__focus-head">
        <div>
          <p class="mapping-hero__focus-label">当前聚焦商品</p>
          <h4 class="mapping-hero__focus-title">
            {{ selectedProduct?.title || '请选择平台商品' }}
          </h4>
        </div>
        <StatusPill
          :label="
            selectedProduct?.productMissing
              ? '主数据缺失'
              : activePrimarySupplier
                ? '主货源就绪'
                : '待建主货源'
          "
          :tone="selectedProduct?.productMissing ? 'warn' : activePrimarySupplier ? 'success' : 'warn'"
        />
      </div>

      <div v-if="selectedProduct" class="mapping-hero__focus-meta">
        <span>{{ selectedProduct.platformProductId || '待补齐平台商品号' }}</span>
        <span>{{ selectedProduct.status }}</span>
        <span>{{ selectedStoreName }}</span>
      </div>

      <div class="mapping-hero__focus-grid">
        <article class="mapping-hero__focus-item">
          <p class="mapping-hero__focus-item-label">主货源</p>
          <p class="mapping-hero__focus-item-value">
            {{
              selectedProduct?.productMissing
                ? '待修复'
                : activePrimarySupplier?.supplierName || '未建立'
            }}
          </p>
        </article>
        <article class="mapping-hero__focus-item">
          <p class="mapping-hero__focus-item-label">备用货源</p>
          <p class="mapping-hero__focus-item-value">
            {{ formatCount(backupSuppliers.length) }}
          </p>
        </article>
        <article class="mapping-hero__focus-item">
          <p class="mapping-hero__focus-item-label">候选池状态</p>
          <p class="mapping-hero__focus-item-value">
            {{
              selectedProduct?.productMissing
                ? '主数据待修复'
                : backupSuppliers.length
                  ? '已兜底'
                  : '待补齐'
            }}
          </p>
        </article>
      </div>
    </div>
  </section>

  <section class="mapping-hero__metrics">
    <MetricCard
      label="平台商品数"
      :value="formatCount(summary.totalProducts)"
      caption="当前租户可用于映射编排的商品数量"
    />
    <MetricCard
      label="已建映射商品"
      :value="formatCount(summary.mappedProducts)"
      caption="已经进入货源映射闭环的商品数量"
    />
    <MetricCard
      label="主货源缺口"
      :value="formatCount(summary.primaryGapProducts)"
      caption="仍缺主货源或主数据异常的商品数量"
    />
    <MetricCard
      label="备用覆盖"
      :value="formatCount(summary.backupCoveredProducts)"
      caption="已经配置备用货源的可用商品数量"
    />
    <MetricCard
      label="风险映射"
      :value="formatCount(summary.riskyMappings)"
      caption="映射或供应商处于待复核状态的记录数"
    />
  </section>
</template>

<style scoped>
.mapping-hero,
.mapping-hero__metrics {
  display: grid;
  gap: 1rem;
}

.mapping-hero {
  grid-template-columns: 1.08fr 0.92fr;
}

.mapping-hero__copy,
.mapping-hero__focus {
  padding: 1.45rem;
  border-radius: var(--radius-shell);
  box-shadow: var(--shadow-card);
}

.mapping-hero__copy {
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.mapping-hero__focus {
  display: grid;
  gap: 1rem;
  background: var(--color-surface);
  border: 1px solid var(--color-line);
  color: var(--color-ink);
}

.mapping-hero__eyebrow,
.mapping-hero__title,
.mapping-hero__subtitle,
.mapping-hero__feedback,
.mapping-hero__focus-label,
.mapping-hero__focus-title,
.mapping-hero__focus-item-label,
.mapping-hero__focus-item-value {
  margin: 0;
}

.mapping-hero__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.mapping-hero__focus-label {
  color: var(--color-ink-soft);
}

.mapping-hero__title {
  margin-top: 0.55rem;
  max-width: 15ch;
  color: var(--color-ink-strong);
  font-size: 1.5rem;
  font-weight: 600;
  line-height: 1.25;
}

.mapping-hero__subtitle,
.mapping-hero__feedback {
  margin-top: 0.8rem;
  max-width: 56rem;
  color: var(--color-ink-soft);
  line-height: 1.72;
}

.mapping-hero__feedback {
  color: var(--color-success);
}

.mapping-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  margin-top: 1.1rem;
}

.mapping-hero__button {
  min-height: 2.8rem;
  padding: 0 1.05rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
}

.mapping-hero__button--solid {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: #ffffff;
}

.mapping-hero__button--solid:hover {
  background: var(--color-accent-hover);
  color: #ffffff;
}

.mapping-hero__focus-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.mapping-hero__focus-label {
  color: var(--color-ink-soft);
  font-size: 0.78rem;
}

.mapping-hero__focus-title {
  margin-top: 0.45rem;
  color: var(--color-ink-strong);
  font-size: 1.32rem;
}

.mapping-hero__focus-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  color: var(--color-ink-soft);
  font-size: 0.86rem;
}

.mapping-hero__focus-meta span {
  display: inline-flex;
  min-height: 2rem;
  align-items: center;
  padding: 0 0.8rem;
  border-radius: var(--radius-pill);
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.mapping-hero__focus-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.mapping-hero__focus-item {
  padding: 0.92rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.mapping-hero__focus-item-label {
  color: var(--color-ink-soft);
  font-size: 0.78rem;
}

.mapping-hero__focus-item-value {
  margin-top: 0.55rem;
  color: var(--color-ink-strong);
  font-size: 1.18rem;
  font-weight: 600;
}

.mapping-hero__metrics {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

@media (max-width: 1240px) {
  .mapping-hero,
  .mapping-hero__metrics,
  .mapping-hero__focus-grid {
    grid-template-columns: 1fr;
  }
}
</style>
