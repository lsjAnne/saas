<script setup lang="ts">
import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import type { DashboardMemberAnalysis } from '@/services/apiTypes';
import { formatCount, formatCurrency, formatPercent } from '@/utils/formatters';

interface Props {
  memberAnalysis: DashboardMemberAnalysis | null;
}

defineProps<Props>();
</script>

<template>
  <PanelCard
    eyebrow="Member Repurchase"
    title="会员复购"
    description="复购率、客单价和沉睡人群一起看，才知道会员经营有没有起作用。"
  >
    <div class="member-repurchase">
      <section class="member-repurchase__copy">
        <p class="member-repurchase__eyebrow">会员经营信号</p>
        <h4 class="member-repurchase__headline">
          先追回可激活会员，再把复购率拉起来，经营助手和活动策略才会更有效。
        </h4>
      </section>

      <div class="member-repurchase__metrics">
        <MetricCard
          label="复购率"
          :value="formatPercent(memberAnalysis?.repurchaseRate)"
          :caption="`${formatCount(memberAnalysis?.totalMembers, ' 人')} 总会员`"
        />
        <MetricCard
          label="平均付费"
          :value="formatCurrency(memberAnalysis?.averagePaidAmount)"
          :caption="`VIP ${formatCount(memberAnalysis?.vipMembers, ' 人')}`"
        />
        <MetricCard
          label="沉睡会员"
          :value="formatCount(memberAnalysis?.dormantMembers, ' 人')"
          caption="沉睡会员越高，说明复购和触达都还要补课"
        />
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.member-repurchase,
.member-repurchase__copy {
  display: grid;
  gap: 0.9rem;
}

.member-repurchase__copy {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.member-repurchase__eyebrow,
.member-repurchase__headline {
  margin: 0;
}

.member-repurchase__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.74rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.member-repurchase__headline {
  color: var(--color-ink-strong);
  line-height: 1.45;
}

.member-repurchase__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

@media (max-width: 960px) {
  .member-repurchase__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
