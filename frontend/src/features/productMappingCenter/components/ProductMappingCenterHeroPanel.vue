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
      <p class="mapping-hero__eyebrow">Source Mapping Atelier</p>
      <h3 class="mapping-hero__title">
        把平台商品、主备货源和映射校验压进同一张可执行的选品编排台
      </h3>
      <p class="mapping-hero__subtitle">
        当前聚焦店铺：{{ selectedStoreName || '待选择商品' }}。这里不把映射当配置表处理，而是把主货源落位、备用兜底和风险复核放在同一条操作链里。
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
              ? 'product missing'
              : activePrimarySupplier
                ? 'primary ready'
                : 'pending primary'
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
  background:
    radial-gradient(circle at top left, rgba(181, 77, 37, 0.2), transparent 34%),
    linear-gradient(145deg, rgba(255, 249, 239, 0.96), rgba(248, 236, 221, 0.95));
  border: 1px solid rgba(121, 74, 40, 0.12);
}

.mapping-hero__focus {
  display: grid;
  gap: 1rem;
  background:
    linear-gradient(160deg, rgba(31, 46, 53, 0.98), rgba(14, 25, 29, 0.98)),
    radial-gradient(circle at top right, rgba(145, 208, 197, 0.16), transparent 36%);
  color: #f5f7f8;
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

.mapping-hero__eyebrow,
.mapping-hero__focus-label {
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.mapping-hero__eyebrow {
  color: var(--color-ink-faint);
}

.mapping-hero__title {
  margin-top: 0.55rem;
  max-width: 15ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3.4rem);
  line-height: 1.04;
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
  border: 1px solid rgba(121, 74, 40, 0.14);
  background: rgba(255, 255, 255, 0.56);
  color: var(--color-ink-strong);
}

.mapping-hero__button--solid {
  background: #a94f2d;
  border-color: #a94f2d;
  color: #fff6ef;
}

.mapping-hero__focus-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.mapping-hero__focus-label {
  color: rgba(235, 242, 244, 0.62);
}

.mapping-hero__focus-title {
  margin-top: 0.45rem;
  font-family: var(--font-display);
  font-size: 1.52rem;
}

.mapping-hero__focus-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  color: rgba(235, 242, 244, 0.82);
  font-size: 0.86rem;
}

.mapping-hero__focus-meta span {
  display: inline-flex;
  min-height: 2rem;
  align-items: center;
  padding: 0 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.08);
}

.mapping-hero__focus-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.mapping-hero__focus-item {
  padding: 0.92rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.08);
}

.mapping-hero__focus-item-label {
  color: rgba(235, 242, 244, 0.62);
  font-size: 0.78rem;
}

.mapping-hero__focus-item-value {
  margin-top: 0.55rem;
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
