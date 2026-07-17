<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  CustomerConversation,
  CustomerServiceTicket,
  SaveTicketSatisfactionPayload,
  TicketSatisfactionView
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface GeneratedAfterSaleDraft {
  reasonText: string;
  evidenceBlob: string;
}

interface Props {
  ticket: CustomerServiceTicket | null;
  linkedConversationCount: number;
  selectedConversation: CustomerConversation | null;
  generatedAfterSaleDraft: GeneratedAfterSaleDraft | null;
  satisfaction: TicketSatisfactionView | null;
  isSubmitting: boolean;
  actionError?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<{
  generateSuggestion: [];
  saveSatisfaction: [payload: SaveTicketSatisfactionPayload];
}>();

const satisfactionForm = reactive({
  score: '5',
  comment: ''
});

const activeChannelLabel = computed(() => {
  if (!props.selectedConversation) {
    return 'No linked conversation yet';
  }

  return `${props.selectedConversation.platformType} / ${props.selectedConversation.platformConversationId}`;
});

watch(
  () => [props.ticket?.ticketId, props.satisfaction?.ratedAt],
  () => {
    satisfactionForm.score = String(props.satisfaction?.score ?? 5);
    satisfactionForm.comment = props.satisfaction?.comment ?? '';
  },
  { immediate: true }
);

function resolveTone(): 'neutral' | 'warn' | 'success' {
  if (props.ticket?.riskFlag) {
    return 'warn';
  }

  if ((props.ticket?.ticketStatus ?? '').toLowerCase() === 'processing') {
    return 'success';
  }

  return 'neutral';
}

function submitSatisfaction() {
  emit('saveSatisfaction', {
    score: Number(satisfactionForm.score),
    comment: satisfactionForm.comment.trim() || undefined
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Ticket Detail"
    title="Customer context, reply readiness, and satisfaction"
    description="Keep ticket facts, linked customer context, and the closure score in one place before moving into conversation or after-sale actions."
  >
    <div v-if="ticket" class="ticket-detail">
      <div class="ticket-detail__summary">
        <div class="ticket-detail__summary-copy">
          <p class="ticket-detail__eyebrow">Current Ticket</p>
          <h4 class="ticket-detail__title">{{ ticket.ticketId }}</h4>
          <p class="ticket-detail__meta">
            Order {{ ticket.orderId }} / Customer {{ ticket.customerId }}
          </p>
          <p class="ticket-detail__meta">Created {{ formatDateTime(ticket.createdAt) }}</p>
        </div>
        <div class="ticket-detail__summary-tags">
          <StatusPill :label="ticket.ticketStatus" :tone="resolveTone()" />
          <StatusPill v-if="ticket.riskFlag" label="risk" tone="warn" />
        </div>
      </div>

      <div class="ticket-detail__context-grid">
        <article class="ticket-detail__context-card">
          <p class="ticket-detail__section-eyebrow">Linked Conversations</p>
          <p class="ticket-detail__context-value">{{ linkedConversationCount }}</p>
          <p class="ticket-detail__section-copy">
            Active channel: {{ activeChannelLabel }}
          </p>
        </article>

        <article class="ticket-detail__context-card">
          <p class="ticket-detail__section-eyebrow">Conversation Status</p>
          <p class="ticket-detail__context-value">
            {{ selectedConversation?.conversationStatus || 'pending' }}
          </p>
          <p class="ticket-detail__section-copy">
            Risk flag: {{ selectedConversation?.riskFlag ? 'manual escalation required' : 'not flagged' }}
          </p>
        </article>

        <article class="ticket-detail__context-card ticket-detail__context-card--wide">
          <p class="ticket-detail__section-eyebrow">After-Sale Draft Preview</p>
          <p class="ticket-detail__section-copy">
            {{
              generatedAfterSaleDraft?.reasonText ||
              'Generate a conversation suggestion below, then use the after-sale panel to build the final request note.'
            }}
          </p>
        </article>
      </div>

      <div class="ticket-detail__suggestion">
        <div class="ticket-detail__section-head">
          <div>
            <p class="ticket-detail__section-eyebrow">Ticket Reply Suggestion</p>
            <p class="ticket-detail__section-copy">
              Ticket-level suggestion keeps the service queue moving even before the agent sends a final conversation reply.
            </p>
          </div>
          <button
            type="button"
            class="ticket-detail__primary"
            :disabled="isSubmitting"
            @click="emit('generateSuggestion')"
          >
            Generate suggestion
          </button>
        </div>
        <div class="ticket-detail__suggestion-box">
          {{
            ticket.aiReplySuggestion ||
            'No ticket suggestion yet. Use the button to create a first handling draft for this service case.'
          }}
        </div>
      </div>

      <form class="ticket-detail__satisfaction" @submit.prevent="submitSatisfaction">
        <div class="ticket-detail__section-head">
          <div>
            <p class="ticket-detail__section-eyebrow">Closure Satisfaction</p>
            <p class="ticket-detail__section-copy">
              Record the final customer outcome after the conversation and after-sale steps have been handled.
            </p>
          </div>
          <StatusPill
            v-if="satisfaction"
            :label="`${satisfaction.score}/5`"
            tone="success"
          />
        </div>

        <div class="ticket-detail__form-grid">
          <label class="ticket-detail__field">
            <span>Score</span>
            <select v-model="satisfactionForm.score" :disabled="isSubmitting">
              <option value="5">5</option>
              <option value="4">4</option>
              <option value="3">3</option>
              <option value="2">2</option>
              <option value="1">1</option>
            </select>
          </label>
          <label class="ticket-detail__field ticket-detail__field--wide">
            <span>Comment</span>
            <textarea
              v-model="satisfactionForm.comment"
              rows="4"
              :disabled="isSubmitting"
              placeholder="Capture the customer reaction, the final promise, or anything the team should revisit later."
            />
          </label>
        </div>

        <p v-if="satisfaction" class="ticket-detail__history">
          Last updated {{ formatDateTime(satisfaction.ratedAt) }}.
          {{ satisfaction.comment || 'No comment recorded.' }}
        </p>
        <p v-if="actionError" class="ticket-detail__error">{{ actionError }}</p>

        <button class="ticket-detail__ghost" type="submit" :disabled="isSubmitting">
          Save satisfaction
        </button>
      </form>
    </div>

    <div v-else class="ticket-detail__empty">
      Select a ticket from the queue first, then review the linked customer context and closure status.
    </div>
  </PanelCard>
</template>

<style scoped>
.ticket-detail,
.ticket-detail__summary,
.ticket-detail__summary-copy,
.ticket-detail__summary-tags,
.ticket-detail__context-grid,
.ticket-detail__suggestion,
.ticket-detail__satisfaction {
  display: grid;
  gap: 0.9rem;
}

.ticket-detail__summary {
  grid-template-columns: 1fr auto;
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.72);
}

.ticket-detail__eyebrow,
.ticket-detail__title,
.ticket-detail__meta,
.ticket-detail__section-eyebrow,
.ticket-detail__section-copy,
.ticket-detail__history,
.ticket-detail__error,
.ticket-detail__empty,
.ticket-detail__context-value {
  margin: 0;
}

.ticket-detail__eyebrow,
.ticket-detail__section-eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.ticket-detail__title,
.ticket-detail__context-value {
  color: var(--color-ink-strong);
}

.ticket-detail__title {
  font-size: 1.2rem;
}

.ticket-detail__meta,
.ticket-detail__section-copy,
.ticket-detail__history,
.ticket-detail__empty {
  color: var(--color-ink-soft);
  line-height: 1.65;
}

.ticket-detail__context-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.ticket-detail__context-card,
.ticket-detail__suggestion-box,
.ticket-detail__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.66);
}

.ticket-detail__context-card {
  display: grid;
  gap: 0.55rem;
}

.ticket-detail__context-card--wide {
  grid-column: span 2;
}

.ticket-detail__context-value {
  font-size: 1.05rem;
  font-weight: 600;
}

.ticket-detail__section-head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.ticket-detail__suggestion-box {
  color: var(--color-ink);
  line-height: 1.75;
  white-space: pre-wrap;
}

.ticket-detail__form-grid {
  display: grid;
  grid-template-columns: 12rem 1fr;
  gap: 0.85rem;
}

.ticket-detail__field {
  display: grid;
  gap: 0.45rem;
}

.ticket-detail__field--wide {
  grid-column: span 2;
}

.ticket-detail__field span {
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.ticket-detail__field select,
.ticket-detail__field textarea {
  min-height: 2.8rem;
  padding: 0.75rem 0.9rem;
  border-radius: 1rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-ink-strong);
  font: inherit;
}

.ticket-detail__primary,
.ticket-detail__ghost {
  min-height: 2.7rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.ticket-detail__primary {
  border: 0;
  background: linear-gradient(135deg, #82493b, #2d1815);
  color: #fff8f0;
}

.ticket-detail__ghost {
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.66);
  color: var(--color-ink);
}

.ticket-detail__error {
  color: var(--color-danger);
}

@media (max-width: 960px) {
  .ticket-detail__summary,
  .ticket-detail__context-grid,
  .ticket-detail__form-grid {
    grid-template-columns: 1fr;
  }

  .ticket-detail__context-card--wide,
  .ticket-detail__field--wide {
    grid-column: span 1;
  }
}
</style>
