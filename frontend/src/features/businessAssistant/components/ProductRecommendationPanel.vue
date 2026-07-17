<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ProductRecommendationView } from '@/services/apiTypes';
import { formatCurrency } from '@/utils/formatters';

interface Props {
  recommendations: ProductRecommendationView[];
}

defineProps<Props>();

const emit = defineEmits<{
  openRoute: [];
}>();

function resolveTone(riskLevel: string | null | undefined): 'neutral' | 'warn' | 'success' {
  const normalized = (riskLevel ?? '').toLowerCase();
  if (normalized === 'high') {
    return 'warn';
  }

  if (normalized === 'low') {
    return 'success';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Product Opportunities"
    title="商品机会清单"
    description="把推荐商品的理由、利润空间和风险解释放在同一行里，方便直接跳进商品中心继续孵化。"
  >
    <div v-if="recommendations.length" class="product-recommendation">
      <article
        v-for="item in recommendations"
        :key="item.candidateProductId"
        class="product-recommendation__item"
      >
        <div class="product-recommendation__item-top">
          <div>
            <p class="product-recommendation__title">{{ item.title }}</p>
            <p class="product-recommendation__meta">
              {{ item.category || '未分类' }} / {{ item.candidateProductId }}
            </p>
          </div>
          <StatusPill
            :label="item.riskLevel || 'unknown'"
            :tone="resolveTone(item.riskLevel)"
          />
        </div>
        <p class="product-recommendation__reason">{{ item.recommendationReason }}</p>
        <p class="product-recommendation__summary">{{ item.aiSummary || '暂无 AI 摘要' }}</p>
        <div class="product-recommendation__metrics">
          <span>利润 {{ formatCurrency(item.estimatedProfit) }}</span>
          <span>得分 {{ item.score }}</span>
          <span>{{ item.suggestedAction }}</span>
        </div>
      </article>
      <button type="button" class="product-recommendation__route" @click="emit('openRoute')">
        前往商品中心执行
      </button>
    </div>
    <div v-else class="product-recommendation__empty">当前范围下没有商品建议。</div>
  </PanelCard>
</template>

<style scoped>
.product-recommendation {
  display: grid;
  gap: 0.9rem;
}

.product-recommendation__item,
.product-recommendation__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.7);
  display: grid;
  gap: 0.7rem;
}

.product-recommendation__item-top {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.product-recommendation__title,
.product-recommendation__meta,
.product-recommendation__reason,
.product-recommendation__summary,
.product-recommendation__empty {
  margin: 0;
}

.product-recommendation__title {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.product-recommendation__meta,
.product-recommendation__summary,
.product-recommendation__empty {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.product-recommendation__reason {
  color: var(--color-ink);
  line-height: 1.7;
}

.product-recommendation__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.product-recommendation__route {
  min-height: 2.8rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.66);
  color: var(--color-ink);
  font: inherit;
}
</style>
