<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { CustomerServiceTicket, SupportSession } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  tickets: CustomerServiceTicket[];
  supportSessions: SupportSession[];
}

defineProps<Props>();

function ticketTone(ticket: CustomerServiceTicket) {
  if (ticket.riskFlag) {
    return 'warn';
  }

  return ticket.ticketStatus === 'closed' ? 'neutral' : 'success';
}

function sessionTone(status: string) {
  if (status === 'approved') {
    return 'success';
  }

  if (status === 'rejected' || status === 'closed') {
    return 'neutral';
  }

  return 'warn';
}
</script>

<template>
  <PanelCard
    eyebrow="Queue"
    title="工单与支持会话队列"
    description="把一线工单与平台支持会话放进同一工作台，便于判断何时需要升级到支持介入。"
  >
    <div class="ticket-queue">
      <section class="ticket-queue__block">
        <p class="ticket-queue__label">Tickets</p>
        <article v-for="ticket in tickets.slice(0, 4)" :key="ticket.ticketId" class="ticket-queue__item">
          <div class="ticket-queue__head">
            <div>
              <h4 class="ticket-queue__title">{{ ticket.orderId }} · {{ ticket.customerId }}</h4>
              <p class="ticket-queue__meta">{{ ticket.aiReplySuggestion || '等待建议回复' }}</p>
            </div>
            <StatusPill :label="ticket.ticketStatus" :tone="ticketTone(ticket)" />
          </div>
          <p class="ticket-queue__time">{{ formatDateTime(ticket.createdAt) }}</p>
        </article>
      </section>

      <section class="ticket-queue__block">
        <p class="ticket-queue__label">Support Sessions</p>
        <article v-for="session in supportSessions.slice(0, 4)" :key="session.id" class="ticket-queue__item">
          <div class="ticket-queue__head">
            <div>
              <h4 class="ticket-queue__title">{{ session.requesterId }}</h4>
              <p class="ticket-queue__meta">{{ session.reason }}</p>
            </div>
            <StatusPill :label="session.status" :tone="sessionTone(session.status)" />
          </div>
          <p class="ticket-queue__time">{{ formatDateTime(session.createdAt) }}</p>
        </article>
      </section>
    </div>
  </PanelCard>
</template>

<style scoped>
.ticket-queue {
  display: grid;
  gap: 1rem;
}

.ticket-queue__block {
  display: grid;
  gap: 0.8rem;
}

.ticket-queue__label,
.ticket-queue__title,
.ticket-queue__meta,
.ticket-queue__time {
  margin: 0;
}

.ticket-queue__label {
  color: var(--color-ink-faint);
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.ticket-queue__item {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.74);
}

.ticket-queue__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.ticket-queue__title {
  color: var(--color-ink-strong);
}

.ticket-queue__meta,
.ticket-queue__time {
  margin-top: 0.35rem;
  color: var(--color-ink-soft);
}
</style>
