<script setup lang="ts">
import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import type { SupportSession, TicketSlaOverviewView } from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface Props {
  ticketSlaOverview: TicketSlaOverviewView | null;
  supportSessions: SupportSession[];
}

const props = defineProps<Props>();
</script>

<template>
  <PanelCard
    eyebrow="Support SLA"
    title="支持会话与 SLA 体征"
    description="支持中心先看服务压力，再看是否需要平台支持会话介入。"
  >
    <div class="support-sla">
      <div class="support-sla__metrics">
        <MetricCard
          label="工单总量"
          :value="formatCount(ticketSlaOverview?.totalTicketCount, ' 单')"
          caption="当前租户客服工单规模"
        />
        <MetricCard
          label="逾期工单"
          :value="formatCount(ticketSlaOverview?.overdueTicketCount, ' 单')"
          caption="最直接的客服压力指标"
        />
        <MetricCard
          label="支持会话"
          :value="formatCount(props.supportSessions.length, ' 个')"
          caption="租户当前申请或已批准的支持会话"
        />
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.support-sla__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

@media (max-width: 960px) {
  .support-sla__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
