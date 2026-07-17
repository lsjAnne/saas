<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  ContentAssetCategoryCard,
  ContentAssetCategoryKey
} from '@/features/contentAssetCenter/composables/useContentAssetCenterOverview';
import { formatCount } from '@/utils/formatters';

interface Props {
  items: ContentAssetCategoryCard[];
  selectedCategory: ContentAssetCategoryKey;
}

defineProps<Props>();
defineEmits<{
  select: [category: ContentAssetCategoryKey];
}>();
</script>

<template>
  <PanelCard
    eyebrow="Category Deck"
    title="素材分类视角"
    description="先切换商品、直播或客服素材，再决定右侧要继续深挖哪一段资产链路。"
  >
    <div class="category-panel">
      <button
        v-for="item in items"
        :key="item.key"
        :class="['category-panel__item', { 'category-panel__item--active': item.key === selectedCategory }]"
        type="button"
        @click="$emit('select', item.key)"
      >
        <div class="category-panel__head">
          <div>
            <p class="category-panel__label">{{ item.label }}</p>
            <p class="category-panel__count">{{ formatCount(item.count) }}</p>
          </div>
          <StatusPill :label="item.count ? 'ready' : 'empty'" :tone="item.tone" />
        </div>
        <p class="category-panel__description">{{ item.description }}</p>
      </button>
    </div>
  </PanelCard>
</template>

<style scoped>
.category-panel {
  display: grid;
  gap: 0.8rem;
}

.category-panel__item {
  padding: 1rem;
  border-radius: var(--radius-xl);
  border: 1px solid rgba(91, 72, 54, 0.09);
  background: rgba(255, 255, 255, 0.6);
  text-align: left;
}

.category-panel__item--active {
  border-color: rgba(169, 84, 52, 0.36);
  background: linear-gradient(145deg, rgba(255, 247, 240, 0.98), rgba(244, 231, 220, 0.92));
  box-shadow: 0 18px 40px rgba(143, 92, 58, 0.12);
}

.category-panel__head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.category-panel__label,
.category-panel__count,
.category-panel__description {
  margin: 0;
}

.category-panel__label {
  color: var(--color-ink-faint);
  font-size: 0.78rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.category-panel__count {
  margin-top: 0.45rem;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: 1.58rem;
}

.category-panel__description {
  margin-top: 0.75rem;
  color: var(--color-ink-soft);
  line-height: 1.68;
}
</style>
