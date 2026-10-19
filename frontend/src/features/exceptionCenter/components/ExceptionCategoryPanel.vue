<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import { formatCount } from '@/utils/formatters';

interface ExceptionCategoryView {
  code: string;
  label: string;
  count: number;
  openCount: number;
}

interface Props {
  categories: ExceptionCategoryView[];
  activeCategoryCode: string;
}

interface Emits {
  select: [categoryCode: string];
}

defineProps<Props>();
const emit = defineEmits<Emits>();
</script>

<template>
  <PanelCard
    title="异常分类导航"
    description="先按异常类型收口，再进入详情和动作。保持分类清楚，避免队列失焦。"
  >
    <div class="exception-category">
      <button
        v-for="category in categories"
        :key="category.code"
        type="button"
        :class="[
          'exception-category__item',
          { 'exception-category__item--active': category.code === activeCategoryCode }
        ]"
        @click="emit('select', category.code)"
      >
        <div class="exception-category__item-head">
          <div>
            <p class="exception-category__title">{{ category.label }}</p>
            <p class="exception-category__meta">
              总量 {{ formatCount(category.count, ' 项') }} · 开放 {{ formatCount(category.openCount, ' 项') }}
            </p>
          </div>
          <StatusPill
            :label="formatCount(category.openCount, ' 项')"
            :tone="category.openCount > 0 ? 'warn' : 'success'"
          />
        </div>
      </button>
    </div>
  </PanelCard>
</template>

<style scoped>
.exception-category {
  display: grid;
  gap: 0.75rem;
}

.exception-category__item {
  width: 100%;
  display: grid;
  gap: 0.35rem;
  padding: 0.9rem 1rem;
  text-align: left;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
  transition:
    transform var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.exception-category__item:hover {
  transform: translateY(-1px);
  border-color: var(--color-accent);
}

.exception-category__item--active {
  border-color: var(--color-accent);
  box-shadow: var(--shadow-card);
}

.exception-category__item-head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: flex-start;
}

.exception-category__title,
.exception-category__meta {
  margin: 0;
}

.exception-category__title {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.exception-category__meta {
  margin-top: 0.28rem;
  color: var(--color-ink-soft);
  line-height: 1.55;
}
</style>
