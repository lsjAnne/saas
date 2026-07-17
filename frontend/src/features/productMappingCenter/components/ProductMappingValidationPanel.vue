<script setup lang="ts">
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ProductMappingValidationItem } from '@/features/productMappingCenter/composables/useProductMappingCenterOverview';

interface Props {
  items: ProductMappingValidationItem[];
}

defineProps<Props>();
</script>

<template>
  <section class="mapping-validation">
    <div class="mapping-validation__head">
      <div>
        <p class="mapping-validation__eyebrow">Validation</p>
        <h4 class="mapping-validation__title">校验结果区</h4>
      </div>
      <p class="mapping-validation__hint">把主备映射、风险状态和候选池完整度统一翻译成可执行结论。</p>
    </div>

    <div v-if="items.length" class="mapping-validation__list">
      <article v-for="item in items" :key="item.code" class="mapping-validation__item">
        <div>
          <h5 class="mapping-validation__item-title">{{ item.title }}</h5>
          <p class="mapping-validation__item-detail">{{ item.detail }}</p>
        </div>
        <StatusPill :label="item.tone === 'warn' ? '需处理' : item.tone === 'success' ? '已通过' : '提示'" :tone="item.tone" />
      </article>
    </div>

    <div v-else class="mapping-validation__empty">
      选择商品后，这里会展示当前映射的通过项、缺口项和风险项。
    </div>
  </section>
</template>

<style scoped>
.mapping-validation,
.mapping-validation__list {
  display: grid;
  gap: 0.9rem;
}

.mapping-validation {
  padding: 1.15rem;
  border-radius: var(--radius-shell);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: var(--shadow-card);
}

.mapping-validation__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
}

.mapping-validation__eyebrow,
.mapping-validation__title,
.mapping-validation__hint,
.mapping-validation__item-title,
.mapping-validation__item-detail {
  margin: 0;
}

.mapping-validation__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.mapping-validation__title {
  margin-top: 0.35rem;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: 1.32rem;
}

.mapping-validation__hint,
.mapping-validation__item-detail {
  color: var(--color-ink-soft);
  line-height: 1.65;
}

.mapping-validation__item {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background:
    linear-gradient(150deg, rgba(255, 255, 255, 0.95), rgba(243, 238, 231, 0.94));
}

.mapping-validation__item-title {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.mapping-validation__item-detail {
  margin-top: 0.45rem;
}

.mapping-validation__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(91, 72, 54, 0.05);
  color: var(--color-ink-soft);
  line-height: 1.72;
}

@media (max-width: 780px) {
  .mapping-validation__head,
  .mapping-validation__item {
    display: grid;
  }
}
</style>
