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
      label: 'Ticket',
      detail: `${props.selectedTicket.ticketId} / ${props.selectedTicket.orderId} / ${formatDateTime(props.selectedTicket.createdAt)}`
    });
  }

  if (props.selectedConversation) {
    items.push({
      label: 'Conversation',
      detail: `${props.selectedConversation.platformType} / ${props.selectedConversation.platformConversationId} / ${props.selectedConversation.conversationStatus}`
    });
  }

  props.conversationMessages.slice(-3).forEach((message) => {
    items.push({
      label: `${message.senderType} / ${message.messageType}`,
      detail: message.contentText
    });
  });

  if (props.afterSaleRecord) {
    items.push({
      label: 'After-Sale',
      detail: `${props.afterSaleRecord.afterSaleId} / ${props.afterSaleRecord.status}`
    });
  }

  return items;
});

const riskNotes = computed(() => {
  const notes: string[] = [];

  if (props.selectedTicket?.riskFlag) {
    notes.push('The selected ticket is already marked as risk.');
  }

  if (props.selectedConversation?.riskFlag) {
    notes.push('The linked conversation already requires manual handling.');
  }

  if ((props.ticketSlaOverview?.overdueTicketCount ?? 0) > 0) {
    notes.push(
      `${props.ticketSlaOverview?.overdueTicketCount ?? 0} tickets are overdue in the current scope.`
    );
  }

  if (!props.afterSaleRecord && props.selectedTicket) {
    notes.push('No after-sale record is linked yet for the selected ticket.');
  }

  if (!notes.length) {
    notes.push('No obvious risk signal is active in the current selection.');
  }

  return notes;
});

function formatMinutes(value: number | null | undefined) {
  if (value == null || value <= 0) {
    return '--';
  }

  return `${Number(value).toFixed(1)} min`;
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
    eyebrow="SLA / Risk / Evidence"
    title="Operational health plus the evidence chain"
    description="Use this panel to keep the SLA snapshot, risk notes, and the latest customer evidence visible while closing the case."
  >
    <div class="sla-insight">
      <div class="sla-insight__metrics">
        <MetricCard
          label="Scoped Tickets"
          :value="`${filteredTicketCount}`"
          :caption="selectedStoreName"
        />
        <MetricCard
          label="SLA Rate"
          :value="formatPercent(withinSlaRate, 0)"
          caption="Higher means the queue is more stable"
        />
        <MetricCard
          label="Avg Response"
          :value="formatMinutes(ticketSlaOverview?.averageFirstResponseMinutes)"
          caption="Measured from ticket creation to first handling"
        />
        <MetricCard
          label="Avg Satisfaction"
          :value="formatScore(ticketSlaOverview?.averageSatisfactionScore)"
          :caption="`Rated ${ticketSlaOverview?.satisfactionCount ?? 0} times`"
        />
      </div>

      <div class="sla-insight__summary-grid">
        <div class="sla-insight__block">
          <p class="sla-insight__eyebrow">Risk Notes</p>
          <p
            v-for="note in riskNotes"
            :key="note"
            class="sla-insight__note"
          >
            {{ note }}
          </p>
        </div>

        <div class="sla-insight__block">
          <p class="sla-insight__eyebrow">Evidence Chain</p>
          <p
            v-for="item in evidenceItems"
            :key="`${item.label}-${item.detail}`"
            class="sla-insight__note"
          >
            <strong>{{ item.label }}:</strong> {{ item.detail }}
          </p>
          <p v-if="!evidenceItems.length" class="sla-insight__note">
            Select a ticket and linked conversation to populate the evidence chain.
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
  background: rgba(255, 255, 255, 0.68);
}

.sla-insight__eyebrow,
.sla-insight__note {
  margin: 0;
}

.sla-insight__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
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
