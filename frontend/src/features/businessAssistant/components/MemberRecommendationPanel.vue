<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { MemberRecommendationView } from '@/services/apiTypes';
import { formatCurrency } from '@/utils/formatters';

interface Props {
  recommendations: MemberRecommendationView[];
}

defineProps<Props>();

const emit = defineEmits<{
  openRoute: [];
}>();

function resolveTone(segmentCode: string): 'neutral' | 'warn' | 'success' {
  if (segmentCode === 'revive_vip') {
    return 'warn';
  }

  if (segmentCode === 'growth_upsell') {
    return 'success';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Member Recommendations"
    title="会员触达建议"
    description="把高价值唤醒、成长型加购和一般维护三类会员建议拉平，让 CRM 页只负责继续执行。"
  >
    <div v-if="recommendations.length" class="member-recommendation">
      <article
        v-for="item in recommendations"
        :key="item.memberId"
        class="member-recommendation__item"
      >
        <div class="member-recommendation__item-top">
          <div>
            <p class="member-recommendation__title">{{ item.nickname }}</p>
            <p class="member-recommendation__meta">
              {{ item.levelCode }} / {{ item.memberId }}
            </p>
          </div>
          <StatusPill :label="item.segmentCode" :tone="resolveTone(item.segmentCode)" />
        </div>
        <p class="member-recommendation__reason">{{ item.recommendationReason }}</p>
        <div class="member-recommendation__metrics">
          <span>订单 {{ item.totalOrderCount }}</span>
          <span>累计支付 {{ formatCurrency(item.totalPaidAmount) }}</span>
          <span>得分 {{ item.score }}</span>
        </div>
        <p class="member-recommendation__action">{{ item.suggestedAction }}</p>
      </article>
      <button type="button" class="member-recommendation__route" @click="emit('openRoute')">
        前往会员 CRM 中心执行
      </button>
    </div>
    <div v-else class="member-recommendation__empty">当前范围下没有会员建议。</div>
  </PanelCard>
</template>

<style scoped>
.member-recommendation {
  display: grid;
  gap: 0.9rem;
}

.member-recommendation__item,
.member-recommendation__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.7);
  display: grid;
  gap: 0.7rem;
}

.member-recommendation__item-top {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.member-recommendation__title,
.member-recommendation__meta,
.member-recommendation__reason,
.member-recommendation__action,
.member-recommendation__empty {
  margin: 0;
}

.member-recommendation__title {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.member-recommendation__meta,
.member-recommendation__action,
.member-recommendation__empty {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.member-recommendation__reason {
  color: var(--color-ink);
  line-height: 1.7;
}

.member-recommendation__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.member-recommendation__route {
  min-height: 2.8rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.66);
  color: var(--color-ink);
  font: inherit;
}
</style>
