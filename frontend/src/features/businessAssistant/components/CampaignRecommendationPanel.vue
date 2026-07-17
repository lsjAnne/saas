<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { CampaignRecommendationView } from '@/services/apiTypes';

interface Props {
  recommendations: CampaignRecommendationView[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  openRoute: [];
}>();

const renderedRecommendations = computed(() =>
  props.recommendations.map((item) => ({
    ...item,
    ruleText: JSON.stringify(item.suggestedRule)
  }))
);
</script>

<template>
  <PanelCard
    eyebrow="Campaign Suggestions"
    title="活动联动建议"
    description="这一组建议强调商品与会员段的联动，不单独停留在活动标题，而是给出规则口径和建议动作。"
  >
    <div v-if="renderedRecommendations.length" class="campaign-recommendation">
      <article
        v-for="item in renderedRecommendations"
        :key="`${item.campaignName}-${item.targetStoreId}`"
        class="campaign-recommendation__item"
      >
        <div class="campaign-recommendation__item-top">
          <div>
            <p class="campaign-recommendation__title">{{ item.campaignName }}</p>
            <p class="campaign-recommendation__meta">
              {{ item.campaignType }} / {{ item.targetStoreId }}
            </p>
          </div>
          <StatusPill :label="`${item.score} 分`" tone="success" />
        </div>
        <p class="campaign-recommendation__reason">{{ item.recommendationReason }}</p>
        <p class="campaign-recommendation__copy">
          目标分群：{{ item.targetMemberSegment || '未指定' }}
        </p>
        <p class="campaign-recommendation__copy">
          联动商品：{{ item.recommendedCandidateProductIds.join(', ') || '无' }}
        </p>
        <p class="campaign-recommendation__copy">
          建议规则：{{ item.ruleText }}
        </p>
        <p class="campaign-recommendation__copy">{{ item.suggestedAction }}</p>
      </article>
      <button type="button" class="campaign-recommendation__route" @click="emit('openRoute')">
        前往营销活动中心执行
      </button>
    </div>
    <div v-else class="campaign-recommendation__empty">当前范围下没有活动建议。</div>
  </PanelCard>
</template>

<style scoped>
.campaign-recommendation {
  display: grid;
  gap: 0.9rem;
}

.campaign-recommendation__item,
.campaign-recommendation__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.7);
  display: grid;
  gap: 0.7rem;
}

.campaign-recommendation__item-top {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.campaign-recommendation__title,
.campaign-recommendation__meta,
.campaign-recommendation__reason,
.campaign-recommendation__copy,
.campaign-recommendation__empty {
  margin: 0;
}

.campaign-recommendation__title {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.campaign-recommendation__meta,
.campaign-recommendation__copy,
.campaign-recommendation__empty {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.campaign-recommendation__reason {
  color: var(--color-ink);
  line-height: 1.7;
}

.campaign-recommendation__route {
  min-height: 2.8rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.66);
  color: var(--color-ink);
  font: inherit;
}
</style>
