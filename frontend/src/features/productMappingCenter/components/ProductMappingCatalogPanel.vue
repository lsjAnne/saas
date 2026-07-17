<script setup lang="ts">
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ProductMappingCatalogItem } from '@/features/productMappingCenter/composables/useProductMappingCenterOverview';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  products: ProductMappingCatalogItem[];
  selectedProductId: string | null;
}

defineProps<Props>();

defineEmits<{
  select: [productId: string];
}>();
</script>

<template>
  <section class="mapping-catalog">
    <div class="mapping-catalog__head">
      <div>
        <p class="mapping-catalog__eyebrow">Platform Products</p>
        <h4 class="mapping-catalog__title">平台商品列表</h4>
      </div>
      <p class="mapping-catalog__hint">优先处理缺主货源、缺备用兜底或主数据异常的商品。</p>
    </div>

    <div v-if="products.length" class="mapping-catalog__list">
      <button
        v-for="product in products"
        :key="product.productId"
        :class="[
          'mapping-catalog__item',
          { 'mapping-catalog__item--active': selectedProductId === product.productId }
        ]"
        type="button"
        @click="$emit('select', product.productId)"
      >
        <div class="mapping-catalog__item-head">
          <div>
            <h5 class="mapping-catalog__item-title">{{ product.title }}</h5>
            <p class="mapping-catalog__item-meta">
              {{ product.storeName }} 路 {{ product.platformProductId || '待补齐平台商品号' }}
            </p>
          </div>
          <StatusPill
            :label="
              product.productMissing
                ? '主数据缺失'
                : product.validationTone === 'warn'
                  ? '待补齐'
                  : '已就绪'
            "
            :tone="product.validationTone === 'warn' ? 'warn' : 'success'"
          />
        </div>

        <div class="mapping-catalog__stats">
          <span>总映射 {{ product.mappingCount }}</span>
          <span>主映射 {{ product.activePrimaryCount }}</span>
          <span>备用 {{ product.activeBackupCount }}</span>
        </div>

        <p class="mapping-catalog__item-date">
          {{
            product.productMissing
              ? `最近映射时间 ${formatDateTime(product.createdAt)}`
              : `上架时间 ${formatDateTime(product.publishedAt || product.createdAt)}`
          }}
        </p>
      </button>
    </div>

    <div v-else class="mapping-catalog__empty">
      还没有可用于映射的商品。先在商品中心发布平台商品，再回到这里建立货源关系。
    </div>
  </section>
</template>

<style scoped>
.mapping-catalog,
.mapping-catalog__list {
  display: grid;
  gap: 0.9rem;
}

.mapping-catalog {
  padding: 1.15rem;
  border-radius: var(--radius-shell);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: var(--shadow-card);
}

.mapping-catalog__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
}

.mapping-catalog__eyebrow,
.mapping-catalog__title,
.mapping-catalog__hint,
.mapping-catalog__item-title,
.mapping-catalog__item-meta,
.mapping-catalog__item-date {
  margin: 0;
}

.mapping-catalog__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.mapping-catalog__title {
  margin-top: 0.35rem;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: 1.35rem;
}

.mapping-catalog__hint,
.mapping-catalog__item-meta,
.mapping-catalog__item-date {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.mapping-catalog__item {
  display: grid;
  gap: 0.85rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.94), rgba(247, 241, 232, 0.9));
  text-align: left;
}

.mapping-catalog__item--active {
  border-color: rgba(169, 79, 45, 0.4);
  box-shadow: 0 18px 34px rgba(169, 79, 45, 0.12);
  transform: translateY(-1px);
}

.mapping-catalog__item-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.mapping-catalog__item-title {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.mapping-catalog__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.mapping-catalog__stats span {
  display: inline-flex;
  min-height: 2rem;
  align-items: center;
  padding: 0 0.75rem;
  border-radius: var(--radius-pill);
  background: rgba(91, 72, 54, 0.08);
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.mapping-catalog__empty {
  padding: 1.05rem;
  border-radius: var(--radius-lg);
  background: rgba(91, 72, 54, 0.05);
  color: var(--color-ink-soft);
  line-height: 1.7;
}

@media (max-width: 780px) {
  .mapping-catalog__head,
  .mapping-catalog__item-head {
    display: grid;
  }
}
</style>
