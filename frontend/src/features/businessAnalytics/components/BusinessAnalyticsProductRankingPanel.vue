<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { BusinessAnalyticsProductRankingItem } from '@/features/businessAnalytics/composables/useBusinessAnalyticsOverview';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  items: BusinessAnalyticsProductRankingItem[];
}

defineProps<Props>();
</script>

<template>
  <PanelCard
    eyebrow="Product Ranking"
    title="商品排行"
    description="按发布状态、健康分和发布时间把最值得优先看的商品压到前面。"
  >
    <div v-if="items.length" class="product-ranking">
      <article v-for="item in items" :key="item.productId" class="product-ranking__card">
        <div class="product-ranking__row">
          <div>
            <p class="product-ranking__rank">TOP {{ item.rank }}</p>
            <h4 class="product-ranking__title">{{ item.title }}</h4>
          </div>
          <StatusPill :label="item.statusLabel" :tone="item.statusTone" />
        </div>

        <div class="product-ranking__meta">
          <span>{{ item.storeName }}</span>
          <span>{{ item.platformProductId }}</span>
        </div>

        <div class="product-ranking__metrics">
          <strong>健康分 {{ item.healthLabel }}</strong>
          <span>发布时间 {{ formatDateTime(item.publishedAt ?? item.createdAt) }}</span>
        </div>
      </article>
    </div>
    <p v-else class="product-ranking__empty">当前筛选范围内还没有可用于排行的商品。</p>
  </PanelCard>
</template>

<style scoped>
.product-ranking,
.product-ranking__card {
  display: grid;
  gap: 0.8rem;
}

.product-ranking__card {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.product-ranking__row,
.product-ranking__metrics {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.product-ranking__rank,
.product-ranking__title,
.product-ranking__meta span,
.product-ranking__metrics strong,
.product-ranking__metrics span,
.product-ranking__empty {
  margin: 0;
}

.product-ranking__rank {
  color: var(--color-ink-faint);
  font-size: 0.74rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.product-ranking__title,
.product-ranking__metrics strong {
  color: var(--color-ink-strong);
}

.product-ranking__meta,
.product-ranking__metrics span,
.product-ranking__empty {
  color: var(--color-ink-soft);
}

.product-ranking__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

@media (max-width: 760px) {
  .product-ranking__row,
  .product-ranking__metrics {
    flex-direction: column;
  }
}
</style>
