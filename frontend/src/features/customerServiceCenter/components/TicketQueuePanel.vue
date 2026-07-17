<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { CustomerServiceTicket, Store } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  stores: Store[];
  tickets: CustomerServiceTicket[];
  selectedTicketId: string | null;
  activeStoreId: string;
  statusFilter: string;
  availableStatuses: string[];
  isLoading: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  selectStore: [storeId: string];
  selectStatus: [status: string];
  selectTicket: [ticketId: string];
}>();

function resolveTone(ticket: CustomerServiceTicket): 'neutral' | 'warn' | 'success' {
  if (ticket.riskFlag) {
    return 'warn';
  }

  if ((ticket.ticketStatus ?? '').toLowerCase() === 'processing') {
    return 'success';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Ticket Queue"
    title="工单队列与筛选"
    description="先按店铺和状态压缩视野，再从高风险和待处理工单中挑出当前要落动作的一张。"
  >
    <div class="ticket-queue">
      <div class="ticket-queue__filters">
        <div class="ticket-queue__filter-group">
          <button
            type="button"
            :class="['ticket-queue__filter', { 'ticket-queue__filter--active': activeStoreId === 'all' }]"
            @click="emit('selectStore', 'all')"
          >
            全部店铺
          </button>
          <button
            v-for="store in stores"
            :key="store.storeId"
            type="button"
            :class="[
              'ticket-queue__filter',
              { 'ticket-queue__filter--active': activeStoreId === store.storeId }
            ]"
            @click="emit('selectStore', store.storeId)"
          >
            {{ store.shopName }}
          </button>
        </div>

        <div class="ticket-queue__filter-group">
          <button
            type="button"
            :class="['ticket-queue__filter', { 'ticket-queue__filter--active': statusFilter === 'all' }]"
            @click="emit('selectStatus', 'all')"
          >
            全部状态
          </button>
          <button
            v-for="status in availableStatuses"
            :key="status"
            type="button"
            :class="[
              'ticket-queue__filter',
              { 'ticket-queue__filter--active': statusFilter === status }
            ]"
            @click="emit('selectStatus', status)"
          >
            {{ status }}
          </button>
        </div>
      </div>

      <div v-if="tickets.length" class="ticket-queue__list">
        <button
          v-for="ticket in tickets"
          :key="ticket.ticketId"
          type="button"
          :class="[
            'ticket-queue__item',
            { 'ticket-queue__item--active': selectedTicketId === ticket.ticketId }
          ]"
          @click="emit('selectTicket', ticket.ticketId)"
        >
          <div class="ticket-queue__item-top">
            <div>
              <p class="ticket-queue__item-title">{{ ticket.ticketId }}</p>
              <p class="ticket-queue__item-meta">
                订单 {{ ticket.orderId }} / 客户 {{ ticket.customerId }}
              </p>
            </div>
            <StatusPill :label="ticket.ticketStatus" :tone="resolveTone(ticket)" />
          </div>
          <div class="ticket-queue__item-tags">
            <span class="ticket-queue__tag">{{ ticket.storeId }}</span>
            <span class="ticket-queue__tag">
              {{ ticket.aiReplySuggestion ? '已有 AI 建议' : '待生成 AI 建议' }}
            </span>
            <span v-if="ticket.riskFlag" class="ticket-queue__tag ticket-queue__tag--warn">
              风险工单
            </span>
          </div>
          <p class="ticket-queue__item-time">{{ formatDateTime(ticket.createdAt) }}</p>
        </button>
      </div>

      <div v-else class="ticket-queue__empty">
        {{ isLoading ? '正在回读工单队列...' : '当前筛选下没有工单。' }}
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.ticket-queue,
.ticket-queue__filters,
.ticket-queue__filter-group,
.ticket-queue__list {
  display: grid;
  gap: 0.85rem;
}

.ticket-queue__filter-group {
  grid-template-columns: repeat(auto-fit, minmax(8rem, 1fr));
}

.ticket-queue__filter,
.ticket-queue__item {
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.72);
}

.ticket-queue__filter {
  min-height: 2.6rem;
  padding: 0 0.95rem;
  border-radius: var(--radius-pill);
  color: var(--color-ink);
  font: inherit;
}

.ticket-queue__filter--active {
  background: linear-gradient(135deg, rgba(130, 73, 59, 0.94), rgba(53, 31, 26, 0.94));
  border-color: transparent;
  color: #fff8f0;
}

.ticket-queue__list {
  max-height: 42rem;
  overflow: auto;
}

.ticket-queue__item {
  padding: 1rem;
  border-radius: var(--radius-lg);
  text-align: left;
  display: grid;
  gap: 0.75rem;
}

.ticket-queue__item--active {
  border-color: rgba(130, 73, 59, 0.38);
  box-shadow: 0 18px 36px rgba(81, 52, 44, 0.12);
}

.ticket-queue__item-top {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.ticket-queue__item-title,
.ticket-queue__item-meta,
.ticket-queue__item-time,
.ticket-queue__empty {
  margin: 0;
}

.ticket-queue__item-title {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.ticket-queue__item-meta,
.ticket-queue__item-time,
.ticket-queue__empty {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.ticket-queue__item-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.ticket-queue__tag {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0 0.75rem;
  border-radius: var(--radius-pill);
  background: rgba(132, 108, 85, 0.1);
  color: var(--color-ink);
  font-size: 0.78rem;
}

.ticket-queue__tag--warn {
  background: rgba(183, 117, 47, 0.14);
  color: var(--color-warning);
}

.ticket-queue__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.64);
}
</style>
