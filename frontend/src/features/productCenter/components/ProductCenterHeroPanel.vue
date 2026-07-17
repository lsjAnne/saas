<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { CandidateProduct, Product, ProductDraft, Store } from '@/services/apiTypes';
import { formatCount, formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  selectedStore: Store | null;
  selectedCandidate: CandidateProduct | null;
  selectedDraft: ProductDraft | null;
  sessionPublishedProducts: Product[];
  actionFeedback?: string | null;
  summary: {
    candidateCount: number;
    pendingReviewCount: number;
    pendingPublishCount: number;
    publishedDraftCount: number;
    sessionPublishedCount: number;
    highRiskCount: number;
  };
}

withDefaults(defineProps<Props>(), {
  actionFeedback: null
});

defineEmits<{
  openCreate: [];
  openGenerate: [];
  openPublish: [];
}>();

function resolveTone(status: string | null | undefined) {
  const normalized = (status ?? '').toLowerCase();

  if (normalized.includes('published') || normalized.includes('pool')) {
    return 'success';
  }

  if (normalized.includes('reject') || normalized.includes('risk')) {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Product Center"
    title="把选品、草稿生成与上架节奏压进一条高密度商品孵化链"
    description="这张页不去假装自己是完整商品主数据总表，而是专注做真正决定上架效率的前半程：候选筛入、风险判断、草稿打磨与发布出栈。"
    dark
  >
    <div class="product-hero">
      <div class="product-hero__focus">
        <div class="product-hero__headline">
          <p class="product-hero__label">当前聚焦</p>
          <h4 class="product-hero__title">
            {{ selectedCandidate?.title ?? selectedDraft?.title ?? '先挑一个候选商品，或直接新建候选池条目' }}
          </h4>
          <div class="product-hero__status-row">
            <StatusPill
              :label="selectedCandidate?.status ?? selectedDraft?.status ?? 'idle'"
              :tone="resolveTone(selectedCandidate?.status ?? selectedDraft?.status)"
            />
            <span class="product-hero__meta">
              {{
                selectedStore
                  ? `${selectedStore.shopName} / ${selectedStore.platformType}`
                  : '当前尚未关联到具体店铺'
              }}
            </span>
          </div>
        </div>

        <div v-if="actionFeedback" class="product-hero__feedback">
          {{ actionFeedback }}
        </div>

        <div class="product-hero__actions">
          <button type="button" class="product-hero__primary" @click="$emit('openCreate')">
            新增候选商品
          </button>
          <button type="button" class="product-hero__ghost" @click="$emit('openGenerate')">
            生成商品草稿
          </button>
          <button type="button" class="product-hero__ghost" @click="$emit('openPublish')">
            发布当前草稿
          </button>
        </div>
      </div>

      <div class="product-hero__metrics">
        <article class="product-hero__metric">
          <p class="product-hero__metric-label">Candidates</p>
          <strong class="product-hero__metric-value">
            {{ formatCount(summary.candidateCount) }}
          </strong>
        </article>
        <article class="product-hero__metric">
          <p class="product-hero__metric-label">Pending Review</p>
          <strong class="product-hero__metric-value">
            {{ formatCount(summary.pendingReviewCount) }}
          </strong>
        </article>
        <article class="product-hero__metric">
          <p class="product-hero__metric-label">Pending Publish</p>
          <strong class="product-hero__metric-value">
            {{ formatCount(summary.pendingPublishCount) }}
          </strong>
        </article>
        <article class="product-hero__metric">
          <p class="product-hero__metric-label">High Risk</p>
          <strong class="product-hero__metric-value">
            {{ formatCount(summary.highRiskCount) }}
          </strong>
        </article>
      </div>

      <div class="product-hero__published">
        <div class="product-hero__published-head">
          <p class="product-hero__label">本次会话已发布</p>
          <span class="product-hero__published-caption">
            后端当前没有“已发布商品列表”接口，这里只保留本次会话的发布结果。
          </span>
        </div>

        <div v-if="sessionPublishedProducts.length" class="product-hero__published-grid">
          <article
            v-for="product in sessionPublishedProducts"
            :key="product.productId"
            class="product-hero__published-card"
          >
            <strong class="product-hero__published-title">{{ product.title }}</strong>
            <p class="product-hero__published-meta">
              {{ product.platformProductId }} / 健康分 {{ product.healthScore ?? '--' }}
            </p>
            <p class="product-hero__published-meta">
              发布于 {{ formatDateTime(product.publishedAt) }}
            </p>
          </article>
        </div>

        <p v-else class="product-hero__published-empty">
          还没有新的发布结果。先把候选商品推进到草稿，再从草稿区出栈。
        </p>

        <div v-if="selectedDraft" class="product-hero__price-band">
          <span>建议售价：{{ formatCurrency(selectedDraft.suggestedPrice) }}</span>
          <span>AI 版本：{{ selectedDraft.aiVersion || '--' }}</span>
          <span>已发布草稿：{{ formatCount(summary.publishedDraftCount) }}</span>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.product-hero {
  display: grid;
  gap: 1rem;
}

.product-hero__focus,
.product-hero__headline {
  display: grid;
  gap: 0.75rem;
}

.product-hero__label,
.product-hero__title,
.product-hero__meta,
.product-hero__published-caption,
.product-hero__published-empty,
.product-hero__published-title,
.product-hero__published-meta {
  margin: 0;
}

.product-hero__label {
  color: rgba(255, 244, 236, 0.66);
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.product-hero__title {
  max-width: 15ch;
  color: #fffaf4;
  font-family: var(--font-display);
  font-size: clamp(1.95rem, 3vw, 2.75rem);
  line-height: 1.04;
}

.product-hero__status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.product-hero__meta,
.product-hero__published-caption,
.product-hero__published-empty,
.product-hero__published-meta {
  color: rgba(255, 244, 236, 0.76);
  line-height: 1.6;
}

.product-hero__feedback {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.08);
  color: #fff8f0;
}

.product-hero__actions,
.product-hero__price-band {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.product-hero__primary,
.product-hero__ghost {
  min-height: 3rem;
  padding: 0 1.15rem;
  border-radius: var(--radius-pill);
}

.product-hero__primary {
  border: 0;
  background: linear-gradient(135deg, #f4d7b4, #e2b37b);
  color: #352617;
}

.product-hero__ghost {
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.04);
  color: #fff7ee;
}

.product-hero__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
}

.product-hero__metric {
  padding: 0.95rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.product-hero__metric-label,
.product-hero__metric-value {
  margin: 0;
}

.product-hero__metric-label {
  color: rgba(255, 244, 236, 0.62);
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.product-hero__metric-value {
  display: block;
  margin-top: 0.45rem;
  color: #fffaf4;
  font-size: 1.5rem;
}

.product-hero__published {
  display: grid;
  gap: 0.7rem;
}

.product-hero__published-head {
  display: grid;
  gap: 0.35rem;
}

.product-hero__published-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.product-hero__published-card {
  padding: 0.85rem 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.06);
}

.product-hero__published-title {
  color: #fffaf4;
}

.product-hero__price-band span {
  padding: 0.45rem 0.75rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 247, 238, 0.9);
  font-size: 0.82rem;
}

@media (max-width: 1080px) {
  .product-hero__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-hero__published-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .product-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
