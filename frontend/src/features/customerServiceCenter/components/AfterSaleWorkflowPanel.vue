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
    return '暂未关联会话';
  }

  return `${props.conversation.platformType} / ${props.conversation.platformConversationId} / ${labelOf(props.conversation.conversationStatus)}`;
});

const recentEvidencePreview = computed(() =>
  props.messages.slice(-2).map((message) => `${labelOf(message.senderType)}：${message.contentText}`)
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

const labelMap: Record<string, string> = {
  ready: '就绪',
  created: '已创建',
  reviewing: '审核中',
  approved: '已通过',
  rejected: '已驳回',
  refund_only: '仅退款',
  return_refund: '退货退款',
  exchange: '换货',
  repair: '维修',
  waiting_manual: '待人工接管',
  closed: '已结束',
  pending: '待处理',
  customer: '客户',
  agent: '客服',
  ai: '智能助手',
  system: '系统'
};

function labelOf(value: string) {
  return labelMap[value] ?? value;
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
    title="售后申请、证据与审批"
    description="在客服工作台内把当前工单与会话整理成结构化的售后申请。"
  >
    <div class="after-sale-workflow">
      <div class="after-sale-workflow__context">
        <div>
          <p class="after-sale-workflow__eyebrow">当前订单</p>
          <h4 class="after-sale-workflow__title">{{ ticket?.orderId || createForm.orderId || '--' }}</h4>
          <p class="after-sale-workflow__meta">
            工单 {{ ticket?.ticketId || '--' }} / 客户 {{ ticket?.customerId || '--' }}
          </p>
          <p class="after-sale-workflow__meta">会话 {{ conversationSummary }}</p>
        </div>
        <StatusPill
          :label="labelOf(afterSaleRecord?.status || 'ready')"
          :tone="resolveTone(afterSaleRecord?.status)"
        />
      </div>

      <div class="after-sale-workflow__draft">
        <div class="after-sale-workflow__draft-head">
          <div>
            <p class="after-sale-workflow__eyebrow">生成的售后草稿</p>
            <p class="after-sale-workflow__meta">
              在创建记录前，把最新客户诉求、处理备注与风险信号带入表单。
            </p>
          </div>
          <button
            class="after-sale-workflow__ghost"
            type="button"
            :disabled="!generatedAfterSaleDraft || isSubmitting"
            @click="applyGeneratedDraft"
          >
            应用草稿
          </button>
        </div>
        <p class="after-sale-workflow__draft-copy">
          {{
            generatedAfterSaleDraft?.reasonText ||
            '当前还没有生成售后草稿，请先生成或刷新会话建议。'
          }}
        </p>
        <p v-if="recentEvidencePreview.length" class="after-sale-workflow__draft-copy">
          最近证据：{{ recentEvidencePreview.join(' | ') }}
        </p>
      </div>

      <form class="after-sale-workflow__create" @submit.prevent="submitCreate">
        <label class="after-sale-workflow__field">
          <span>订单号</span>
          <input v-model="createForm.orderId" :disabled="isSubmitting" />
        </label>
        <label class="after-sale-workflow__field">
          <span>售后类型</span>
          <select v-model="createForm.afterSaleType" :disabled="isSubmitting">
            <option value="refund_only">仅退款</option>
            <option value="return_refund">退货退款</option>
            <option value="exchange">换货</option>
            <option value="repair">维修</option>
          </select>
        </label>
        <label class="after-sale-workflow__field after-sale-workflow__field--wide">
          <span>售后原因</span>
          <textarea
            v-model="createForm.reasonText"
            rows="3"
            :disabled="isSubmitting"
            placeholder="说明该订单需要售后处理的原因。"
          />
        </label>
        <label class="after-sale-workflow__field after-sale-workflow__field--wide">
          <span>证据材料</span>
          <textarea
            v-model="createForm.evidenceBlob"
            rows="5"
            :disabled="isSubmitting"
            placeholder="记录截图、会话片段、物流节点或内部备注等。"
          />
        </label>
        <button class="after-sale-workflow__primary" type="submit" :disabled="isSubmitting">
          创建售后单
        </button>
      </form>

      <form class="after-sale-workflow__lookup" @submit.prevent="submitLookup">
        <label class="after-sale-workflow__field">
          <span>读取已有售后单号</span>
          <input
            v-model="lookupForm.afterSaleId"
            :disabled="isSubmitting"
            placeholder="输入售后单号"
          />
        </label>
        <button class="after-sale-workflow__ghost" type="submit" :disabled="isSubmitting">
          读取记录
        </button>
      </form>

      <div v-if="afterSaleRecord" class="after-sale-workflow__record">
        <div class="after-sale-workflow__record-top">
          <div>
            <p class="after-sale-workflow__record-title">{{ afterSaleRecord.afterSaleId }}</p>
            <p class="after-sale-workflow__record-meta">
              {{ labelOf(afterSaleRecord.afterSaleType) }} / 订单 {{ afterSaleRecord.orderId }}
            </p>
          </div>
          <StatusPill
            :label="labelOf(afterSaleRecord.status)"
            :tone="resolveTone(afterSaleRecord.status)"
          />
        </div>
        <p class="after-sale-workflow__record-copy">
          原因：{{ afterSaleRecord.reasonText || '无' }}
        </p>
        <p class="after-sale-workflow__record-copy">
          证据：{{ afterSaleRecord.evidenceBlob || '无' }}
        </p>
        <p class="after-sale-workflow__record-copy">
          创建于：{{ formatDateTime(afterSaleRecord.createdAt) }}
        </p>
        <button
          class="after-sale-workflow__ghost"
          type="button"
          :disabled="isSubmitting || !canSubmitApproval"
          @click="emit('submitApproval')"
        >
          提交审批
        </button>
      </div>

      <div v-else class="after-sale-workflow__empty">
        先基于当前工单整理草稿，再创建新售后单或读取已有记录进行审批跟进。
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
  background: var(--color-surface);
  border: 1px solid var(--color-line);
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
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
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
  background: var(--color-surface);
  border: 1px solid var(--color-line);
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
  border: 1px solid var(--color-line);
  background: #ffffff;
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
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #ffffff;
}

.after-sale-workflow__primary:hover {
  background: var(--color-accent-hover);
}

.after-sale-workflow__ghost {
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.after-sale-workflow__ghost:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
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
