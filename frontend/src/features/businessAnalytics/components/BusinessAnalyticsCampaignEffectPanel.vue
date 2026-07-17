<script setup lang="ts">
import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import type { DashboardCampaignAnalysis } from '@/services/apiTypes';
import { formatCount, formatPercent } from '@/utils/formatters';

interface Props {
  campaignAnalysis: DashboardCampaignAnalysis | null;
}

defineProps<Props>();
</script>

<template>
  <PanelCard
    eyebrow="Campaign Effect"
    title="活动效果"
    description="活动页不只看数量，更要看审批积压和最终发布成功率。"
  >
    <div class="campaign-effect">
      <section class="campaign-effect__copy">
        <p class="campaign-effect__eyebrow">推荐动作</p>
        <h4 class="campaign-effect__headline">
          {{ campaignAnalysis?.recommendedStrategy || '当前正在整理活动策略建议。' }}
        </h4>
      </section>

      <div class="campaign-effect__metrics">
        <MetricCard
          label="发布成功率"
          :value="formatPercent(campaignAnalysis?.publishSuccessRate)"
          :caption="`${formatCount(campaignAnalysis?.publishedCampaignCount)} / ${formatCount(campaignAnalysis?.totalCampaignCount)}`"
        />
        <MetricCard
          label="待审批活动"
          :value="formatCount(campaignAnalysis?.pendingApprovalCount, ' 个')"
          caption="审批积压会直接拖慢经营动作落地"
        />
        <MetricCard
          label="活动总量"
          :value="formatCount(campaignAnalysis?.totalCampaignCount, ' 个')"
          :caption="`券模板 ${formatCount(campaignAnalysis?.totalCouponTemplateCount, ' 个')}`"
        />
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.campaign-effect,
.campaign-effect__copy {
  display: grid;
  gap: 0.9rem;
}

.campaign-effect__copy {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.campaign-effect__eyebrow,
.campaign-effect__headline {
  margin: 0;
}

.campaign-effect__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.74rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.campaign-effect__headline {
  color: var(--color-ink-strong);
  line-height: 1.45;
}

.campaign-effect__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

@media (max-width: 960px) {
  .campaign-effect__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
