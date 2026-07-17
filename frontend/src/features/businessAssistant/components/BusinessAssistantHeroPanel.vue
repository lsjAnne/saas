<script setup lang="ts">
import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  CampaignRecommendationView,
  MemberRecommendationView,
  ProductRecommendationView,
  RecommendationOverviewView,
  Store
} from '@/services/apiTypes';
import { formatPercent } from '@/utils/formatters';

interface Summary {
  totalRecommendationCount: number;
  highRiskProductCount: number;
  reviveVipCount: number;
  comboCampaignCount: number;
}

interface Props {
  stores: Store[];
  activeStoreId: string;
  selectedStoreName: string;
  overview: RecommendationOverviewView | null;
  topProduct: ProductRecommendationView | null;
  topMember: MemberRecommendationView | null;
  topCampaign: CampaignRecommendationView | null;
  summary: Summary;
}

defineProps<Props>();

const emit = defineEmits<{
  selectStore: [storeId: string];
  openRoute: [target: 'product' | 'member' | 'campaign'];
}>();

function scorePercent(score: number | null | undefined) {
  if (score == null) {
    return '--';
  }

  return formatPercent(score / 100, 0);
}
</script>

<template>
  <PanelCard
    eyebrow="Business Assistant"
    title="把推荐解释、经营动作和跨域落点压进一张决策首屏"
    description="这页紧跟 dashboard，不再只是看数，而是直接把商品、会员、活动三类建议变成可以顺手转去执行页的动作清单。"
    dark
  >
    <div class="business-hero">
      <div class="business-hero__copy">
        <p class="business-hero__eyebrow">当前建议范围</p>
        <h4 class="business-hero__title">{{ selectedStoreName }}</h4>
        <p class="business-hero__headline">
          {{ overview?.headline || '正在汇总今天最值得先动手的经营建议。' }}
        </p>
        <div class="business-hero__actions">
          <button
            type="button"
            :class="['business-hero__filter', { 'business-hero__filter--active': activeStoreId === 'all' }]"
            @click="emit('selectStore', 'all')"
          >
            全部店铺
          </button>
          <button
            v-for="store in stores"
            :key="store.storeId"
            type="button"
            :class="[
              'business-hero__filter',
              { 'business-hero__filter--active': activeStoreId === store.storeId }
            ]"
            @click="emit('selectStore', store.storeId)"
          >
            {{ store.shopName }}
          </button>
        </div>
      </div>

      <div class="business-hero__rail">
        <button
          type="button"
          class="business-hero__route"
          @click="emit('openRoute', 'product')"
        >
          <span>商品执行</span>
          <strong>{{ topProduct?.title || '暂无商品建议' }}</strong>
        </button>
        <button
          type="button"
          class="business-hero__route"
          @click="emit('openRoute', 'member')"
        >
          <span>会员执行</span>
          <strong>{{ topMember?.nickname || '暂无会员建议' }}</strong>
        </button>
        <button
          type="button"
          class="business-hero__route"
          @click="emit('openRoute', 'campaign')"
        >
          <span>活动执行</span>
          <strong>{{ topCampaign?.campaignName || '暂无活动建议' }}</strong>
        </button>
      </div>
    </div>

    <div class="business-hero__metrics">
      <MetricCard
        label="建议总量"
        :value="`${summary.totalRecommendationCount}`"
        caption="商品、会员、活动三类建议合计"
      />
      <MetricCard
        label="高风险商品"
        :value="`${summary.highRiskProductCount}`"
        caption="优先避免误把高风险商品推进孵化"
      />
      <MetricCard
        label="VIP 唤醒"
        :value="`${summary.reviveVipCount}`"
        caption="值得优先投放会员触达动作"
      />
      <MetricCard
        label="组合活动"
        :value="`${summary.comboCampaignCount}`"
        caption="带商品联动的活动建议数"
      />
    </div>

    <div class="business-hero__focus">
      <article class="business-hero__focus-card">
        <p class="business-hero__focus-label">商品得分</p>
        <h5 class="business-hero__focus-value">{{ scorePercent(topProduct?.score) }}</h5>
        <p class="business-hero__focus-copy">{{ topProduct?.suggestedAction || '--' }}</p>
      </article>
      <article class="business-hero__focus-card">
        <p class="business-hero__focus-label">会员段</p>
        <h5 class="business-hero__focus-value">{{ topMember?.segmentCode || '--' }}</h5>
        <p class="business-hero__focus-copy">{{ topMember?.suggestedAction || '--' }}</p>
      </article>
      <article class="business-hero__focus-card">
        <p class="business-hero__focus-label">活动类型</p>
        <h5 class="business-hero__focus-value">{{ topCampaign?.campaignType || '--' }}</h5>
        <p class="business-hero__focus-copy">{{ topCampaign?.suggestedAction || '--' }}</p>
      </article>
    </div>

    <div class="business-hero__recommended">
      <StatusPill
        v-for="action in overview?.recommendedActions ?? []"
        :key="action"
        :label="action"
        tone="success"
      />
    </div>
  </PanelCard>
</template>

<style scoped>
.business-hero,
.business-hero__copy,
.business-hero__rail {
  display: grid;
  gap: 0.9rem;
}

.business-hero {
  grid-template-columns: 1.08fr 0.92fr;
}

.business-hero__copy,
.business-hero__rail,
.business-hero__focus-card {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 244, 236, 0.08);
  background: rgba(255, 248, 240, 0.08);
}

.business-hero__eyebrow,
.business-hero__title,
.business-hero__headline,
.business-hero__focus-label,
.business-hero__focus-value,
.business-hero__focus-copy {
  margin: 0;
}

.business-hero__eyebrow,
.business-hero__focus-label {
  color: rgba(255, 244, 236, 0.72);
  font-size: 0.76rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.business-hero__title,
.business-hero__focus-value {
  color: #fff8f0;
}

.business-hero__title {
  font-family: var(--font-display);
  font-size: clamp(1.9rem, 3vw, 2.7rem);
}

.business-hero__headline,
.business-hero__focus-copy {
  color: rgba(255, 244, 236, 0.78);
  line-height: 1.7;
}

.business-hero__actions,
.business-hero__recommended {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.business-hero__filter,
.business-hero__route {
  border: 1px solid rgba(255, 244, 236, 0.12);
  background: rgba(255, 255, 255, 0.08);
  color: #fff8f0;
}

.business-hero__filter {
  min-height: 2.5rem;
  padding: 0 0.9rem;
  border-radius: var(--radius-pill);
  font: inherit;
}

.business-hero__filter--active {
  background: linear-gradient(135deg, rgba(117, 82, 59, 0.96), rgba(36, 25, 18, 0.96));
  border-color: transparent;
}

.business-hero__route {
  min-height: 5rem;
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  display: grid;
  gap: 0.35rem;
  text-align: left;
}

.business-hero__route span {
  color: rgba(255, 244, 236, 0.72);
  font-size: 0.8rem;
}

.business-hero__route strong {
  color: #fff8f0;
  font-size: 1rem;
  line-height: 1.5;
}

.business-hero__metrics,
.business-hero__focus {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
  margin-top: 1rem;
}

.business-hero__focus {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 1180px) {
  .business-hero,
  .business-hero__metrics,
  .business-hero__focus {
    grid-template-columns: 1fr;
  }
}
</style>
