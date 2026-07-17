<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  AfterSaleRecord,
  ConversationMessage,
  CreateAfterSalePayload,
  CustomerConversation,
  CustomerServiceTicket
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface GeneratedAfterSaleDraft {
  reasonText: string;
  evidenceBlob: string;
}

interface Props {
  ticket: CustomerServiceTicket | null;
  conversation: CustomerConversation | null;
  messages: ConversationMessage[];
  generatedAfterSaleDraft: GeneratedAfterSaleDraft | null;
  afterSaleRecord: AfterSaleRecord | null;
  activeAfterSaleId: string;
  isSubmitting: boolean;
  actionError?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<{
  createAfterSale: [payload: CreateAfterSalePayload];
  openAfterSale: [afterSaleId: string];
  submitApproval: [];
}>();

const createForm = reactive({
  orderId: '',
  afterSaleType: 'refund_only',
  reasonText: '',
  evidenceBlob: ''
});

const lookupForm = reactive({
  afterSaleId: ''
});

const canSubmitApproval = computed(
  () => (props.afterSaleRecord?.status ?? '').toLowerCase() === 'created'
);

const conversationSummary = computed(() => {
  if (!props.conversation) {
    return 'No linked conversation';
  }

  return `${props.conversation.platformType} / ${props.conversation.platformConversationId} / ${props.conversation.conversationStatus}`;
});

const recentEvidencePreview = computed(() =>
  props.messages.slice(-2).map((message) => `${message.senderType}: ${message.contentText}`)
);

watch(
  () => [props.ticket?.orderId, props.activeAfterSaleId],
  () => {
    createForm.orderId = props.ticket?.orderId ?? createForm.orderId;
    lookupForm.afterSaleId = props.activeAfterSaleId || lookupForm.afterSaleId;
  },
  { immediate: true }
);

function resolveTone(
  status: string | null | undefined
): 'neutral' | 'warn' | 'success' {
  const normalized = (status ?? '').toLowerCase();

  if (normalized === 'reviewing') {
    return 'warn';
  }

  if (normalized === 'created') {
    return 'success';
  }

  return 'neutral';
}

function applyGeneratedDraft() {
  if (!props.generatedAfterSaleDraft) {
    return;
  }

  createForm.reasonText = props.generatedAfterSaleDraft.reasonText;
  createForm.evidenceBlob = props.generatedAfterSaleDraft.evidenceBlob;
}

function submitCreate() {
  emit('createAfterSale', {
    orderId: createForm.orderId.trim(),
    afterSaleType: createForm.afterSaleType,
    reasonText: createForm.reasonText.trim() || undefined,
    evidenceBlob: createForm.evidenceBlob.trim() || undefined
  });
}

function submitLookup() {
  const afterSaleId = lookupForm.afterSaleId.trim();
  if (!afterSaleId) {
    return;
  }

  emit('openAfterSale', afterSaleId);
}
</script>

<template>
  <PanelCard
    eyebrow="After-Sale Workflow"
    title="After-sale request, evidence, and approval"
    description="Turn the current ticket and conversation into a structured after-sale request without leaving the service desk."
  >
    <div class="after-sale-workflow">
      <div class="after-sale-workflow__context">
        <div>
          <p class="after-sale-workflow__eyebrow">Current Order Anchor</p>
          <h4 class="after-sale-workflow__title">{{ ticket?.orderId || createForm.orderId || '--' }}</h4>
          <p class="after-sale-workflow__meta">
            Ticket {{ ticket?.ticketId || '--' }} / Customer {{ ticket?.customerId || '--' }}
          </p>
          <p class="after-sale-workflow__meta">Conversation {{ conversationSummary }}</p>
        </div>
        <StatusPill
          :label="afterSaleRecord?.status || 'ready'"
          :tone="resolveTone(afterSaleRecord?.status)"
        />
      </div>

      <div class="after-sale-workflow__draft">
        <div class="after-sale-workflow__draft-head">
          <div>
            <p class="after-sale-workflow__eyebrow">Generated Request Draft</p>
            <p class="after-sale-workflow__meta">
              Pull the latest customer request, handling note, and risk signals into the form before creating the record.
            </p>
          </div>
          <button
            class="after-sale-workflow__ghost"
            type="button"
            :disabled="!generatedAfterSaleDraft || isSubmitting"
            @click="applyGeneratedDraft"
          >
            Apply draft
          </button>
        </div>
        <p class="after-sale-workflow__draft-copy">
          {{
            generatedAfterSaleDraft?.reasonText ||
            'No generated after-sale draft yet. Create or refresh a conversation suggestion first.'
          }}
        </p>
        <p v-if="recentEvidencePreview.length" class="after-sale-workflow__draft-copy">
          Recent evidence: {{ recentEvidencePreview.join(' | ') }}
        </p>
      </div>

      <form class="after-sale-workflow__create" @submit.prevent="submitCreate">
        <label class="after-sale-workflow__field">
          <span>Order ID</span>
          <input v-model="createForm.orderId" :disabled="isSubmitting" />
        </label>
        <label class="after-sale-workflow__field">
          <span>After-Sale Type</span>
          <select v-model="createForm.afterSaleType" :disabled="isSubmitting">
            <option value="refund_only">refund_only</option>
            <option value="return_refund">return_refund</option>
            <option value="exchange">exchange</option>
            <option value="repair">repair</option>
          </select>
        </label>
        <label class="after-sale-workflow__field after-sale-workflow__field--wide">
          <span>Reason</span>
          <textarea
            v-model="createForm.reasonText"
            rows="3"
            :disabled="isSubmitting"
            placeholder="Describe why this order needs an after-sale action."
          />
        </label>
        <label class="after-sale-workflow__field after-sale-workflow__field--wide">
          <span>Evidence</span>
          <textarea
            v-model="createForm.evidenceBlob"
            rows="5"
            :disabled="isSubmitting"
            placeholder="Capture screenshots, conversation snippets, logistics checkpoints, or any internal notes."
          />
        </label>
        <button class="after-sale-workflow__primary" type="submit" :disabled="isSubmitting">
          Create after-sale record
        </button>
      </form>

      <form class="after-sale-workflow__lookup" @submit.prevent="submitLookup">
        <label class="after-sale-workflow__field">
          <span>Load Existing After-Sale ID</span>
          <input
            v-model="lookupForm.afterSaleId"
            :disabled="isSubmitting"
            placeholder="Enter afterSaleId"
          />
        </label>
        <button class="after-sale-workflow__ghost" type="submit" :disabled="isSubmitting">
          Load record
        </button>
      </form>

      <div v-if="afterSaleRecord" class="after-sale-workflow__record">
        <div class="after-sale-workflow__record-top">
          <div>
            <p class="after-sale-workflow__record-title">{{ afterSaleRecord.afterSaleId }}</p>
            <p class="after-sale-workflow__record-meta">
              {{ afterSaleRecord.afterSaleType }} / Order {{ afterSaleRecord.orderId }}
            </p>
          </div>
          <StatusPill
            :label="afterSaleRecord.status"
            :tone="resolveTone(afterSaleRecord.status)"
          />
        </div>
        <p class="after-sale-workflow__record-copy">
          Reason: {{ afterSaleRecord.reasonText || 'None' }}
        </p>
        <p class="after-sale-workflow__record-copy">
          Evidence: {{ afterSaleRecord.evidenceBlob || 'None' }}
        </p>
        <p class="after-sale-workflow__record-copy">
          Created: {{ formatDateTime(afterSaleRecord.createdAt) }}
        </p>
        <button
          class="after-sale-workflow__ghost"
          type="button"
          :disabled="isSubmitting || !canSubmitApproval"
          @click="emit('submitApproval')"
        >
          Submit approval
        </button>
      </div>

      <div v-else class="after-sale-workflow__empty">
        Build a draft from the current ticket, then create a new after-sale record or load an existing one for approval follow-up.
      </div>

      <p v-if="actionError" class="after-sale-workflow__error">{{ actionError }}</p>
    </div>
  </PanelCard>
</template>

<style scoped>
.after-sale-workflow,
.after-sale-workflow__create,
.after-sale-workflow__lookup,
.after-sale-workflow__record,
.after-sale-workflow__draft {
  display: grid;
  gap: 0.9rem;
}

.after-sale-workflow__context,
.after-sale-workflow__record-top,
.after-sale-workflow__draft-head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.after-sale-workflow__context {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.72);
}

.after-sale-workflow__eyebrow,
.after-sale-workflow__title,
.after-sale-workflow__meta,
.after-sale-workflow__record-title,
.after-sale-workflow__record-meta,
.after-sale-workflow__record-copy,
.after-sale-workflow__draft-copy,
.after-sale-workflow__empty,
.after-sale-workflow__error {
  margin: 0;
}

.after-sale-workflow__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.after-sale-workflow__title,
.after-sale-workflow__record-title {
  color: var(--color-ink-strong);
  font-size: 1.15rem;
}

.after-sale-workflow__meta,
.after-sale-workflow__record-meta,
.after-sale-workflow__record-copy,
.after-sale-workflow__draft-copy,
.after-sale-workflow__empty {
  color: var(--color-ink-soft);
  line-height: 1.65;
}

.after-sale-workflow__draft,
.after-sale-workflow__record,
.after-sale-workflow__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.66);
}

.after-sale-workflow__create {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.after-sale-workflow__lookup {
  grid-template-columns: 1fr auto;
  align-items: end;
}

.after-sale-workflow__field {
  display: grid;
  gap: 0.45rem;
}

.after-sale-workflow__field--wide,
.after-sale-workflow__primary {
  grid-column: span 2;
}

.after-sale-workflow__field span {
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.after-sale-workflow__field input,
.after-sale-workflow__field select,
.after-sale-workflow__field textarea {
  min-height: 2.8rem;
  padding: 0.75rem 0.9rem;
  border-radius: 1rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-ink-strong);
  font: inherit;
}

.after-sale-workflow__primary,
.after-sale-workflow__ghost {
  min-height: 2.7rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.after-sale-workflow__primary {
  border: 0;
  background: linear-gradient(135deg, #30525a, #6b8d8b);
  color: #fff8f0;
}

.after-sale-workflow__ghost {
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.66);
  color: var(--color-ink);
}

.after-sale-workflow__error {
  color: var(--color-danger);
}

@media (max-width: 960px) {
  .after-sale-workflow__create,
  .after-sale-workflow__lookup {
    grid-template-columns: 1fr;
  }

  .after-sale-workflow__field--wide,
  .after-sale-workflow__primary {
    grid-column: span 1;
  }

  .after-sale-workflow__record-top,
  .after-sale-workflow__draft-head {
    flex-direction: column;
  }
}
</style>
