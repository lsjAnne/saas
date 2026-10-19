<script setup lang="ts">
import { computed } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import type {
  AfterSaleRecord,
  ConversationMessage,
  CustomerConversation,
  CustomerServiceTicket,
  TicketSlaOverviewView
} from '@/services/apiTypes';
import { formatDateTime, formatPercent } from '@/utils/formatters';

interface Props {
  ticketSlaOverview: TicketSlaOverviewView | null;
  filteredTicketCount: number;
  selectedStoreName: string;
  selectedTicket: CustomerServiceTicket | null;
  selectedConversation: CustomerConversation | null;
  conversationMessages: ConversationMessage[];
  afterSaleRecord: AfterSaleRecord | null;
}

const props = defineProps<Props>();

const withinSlaRate = computed(() => {
  const total = props.ticketSlaOverview?.totalTicketCount ?? 0;
  if (total <= 0) {
    return 0;
  }

  return (props.ticketSlaOverview?.withinSlaCount ?? 0) / total;
});

const evidenceItems = computed(() => {
  const items: Array<{ label: string; detail: string }> = [];

  if (props.selectedTicket) {
    items.push({
      label: '工单',
      detail: `${props.selectedTicket.ticketId} / ${props.selectedTicket.orderId} / ${formatDateTime(props.selectedTicket.createdAt)}`
    });
  }

  if (props.selectedConversation) {
    items.push({
      label: '会话',
      detail: `${props.selectedConversation.platformType} / ${props.selectedConversation.platformConversationId} / ${labelOf(props.selectedConversation.conversationStatus)}`
    });
  }

  props.conversationMessages.slice(-3).forEach((message) => {
    items.push({
      label: `${labelOf(message.senderType)} / ${labelOf(message.messageType)}`,
      detail: message.contentText
    });
  });

  if (props.afterSaleRecord) {
    items.push({
      label: '售后',
      detail: `${props.afterSaleRecord.afterSaleId} / ${labelOf(props.afterSaleRecord.status)}`
    });
  }

  return items;
});

const riskNotes = computed(() => {
  const notes: string[] = [];

  if (props.selectedTicket?.riskFlag) {
    notes.push('当前选中工单已被标记为风险工单。');
  }

  if (props.selectedConversation?.riskFlag) {
    notes.push('关联会话需要人工接管处理。');
  }

  if ((props.ticketSlaOverview?.overdueTicketCount ?? 0) > 0) {
    notes.push(
      `当前范围内已有 ${props.ticketSlaOverview?.overdueTicketCount ?? 0} 个工单逾期。`
    );
  }

  if (!props.afterSaleRecord && props.selectedTicket) {
    notes.push('当前工单还未关联售后单记录。');
  }

  if (!notes.length) {
    notes.push('当前选择范围内没有明显的风险信号。');
  }

  return notes;
});

const enumLabelMap: Record<string, string> = {
  waiting_manual: '待人工接管',
  closed: '已结束',
  pending: '待处理',
  processing: '处理中',
  resolved: '已解决',
  customer: '客户',
  agent: '客服',
  ai: '智能助手',
  system: '系统',
  text: '文本',
  image: '图片',
  video: '视频'
};

function labelOf(value: string) {
  return enumLabelMap[value] ?? value;
}

function formatMinutes(value: number | null | undefined) {
  if (value == null || value <= 0) {
    return '--';
  }

  return `${Number(value).toFixed(1)} 分钟`;
}

function formatScore(value: number | null | undefined) {
  if (value == null || value <= 0) {
    return '--';
  }

  return `${Number(value).toFixed(1)} / 5`;
}
</script>

<template>
  <PanelCard
    title="SLA 与证据链洞察"
    description="集中查看 SLA 概览、风险提示与最新客户证据，便于快速结案。"
  >
    <div class="sla-insight">
      <div class="sla-insight__metrics">
        <MetricCard
          label="筛选工单"
          :value="`${filteredTicketCount}`"
          :caption="selectedStoreName"
        />
        <MetricCard
          label="SLA 达标率"
          :value="formatPercent(withinSlaRate, 0)"
          caption="达标越高代表队列越稳定"
        />
        <MetricCard
          label="平均首响"
          :value="formatMinutes(ticketSlaOverview?.averageFirstResponseMinutes)"
          caption="从工单创建到首次处理的时长"
        />
        <MetricCard
          label="满意度均分"
          :value="formatScore(ticketSlaOverview?.averageSatisfactionScore)"
          :caption="`已收集 ${ticketSlaOverview?.satisfactionCount ?? 0} 次评分`"
        />
      </div>

      <div class="sla-insight__summary-grid">
        <div class="sla-insight__block">
          <p class="sla-insight__eyebrow">风险提示</p>
          <p
            v-for="note in riskNotes"
            :key="note"
            class="sla-insight__note"
          >
            {{ note }}
          </p>
        </div>

        <div class="sla-insight__block">
          <p class="sla-insight__eyebrow">证据链</p>
          <p
            v-for="item in evidenceItems"
            :key="`${item.label}-${item.detail}`"
            class="sla-insight__note"
          >
            <strong>{{ item.label }}：</strong>{{ item.detail }}
          </p>
          <p v-if="!evidenceItems.length" class="sla-insight__note">
            选择一个工单及其关联会话后，这里会展示对应证据链。
          </p>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.sla-insight,
.sla-insight__summary-grid,
.sla-insight__block {
  display: grid;
  gap: 0.9rem;
}

.sla-insight__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

.sla-insight__summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.sla-insight__block {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.sla-insight__eyebrow,
.sla-insight__note {
  margin: 0;
}

.sla-insight__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.sla-insight__note {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

@media (max-width: 1080px) {
  .sla-insight__metrics,
  .sla-insight__summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
