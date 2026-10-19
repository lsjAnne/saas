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
    return '暂未关联会话';
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

const ticketStatusMap: Record<string, string> = {
  pending: '待处理',
  processing: '处理中',
  closed: '已关闭',
  resolved: '已解决'
};

function labelOf(value: string) {
  return ticketStatusMap[value] ?? value;
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
    title="工单详情与结案满意度"
    description="集中查看工单事实、关联客户上下文与结案评分，再进入会话或售后动作。"
  >
    <div v-if="ticket" class="ticket-detail">
      <div class="ticket-detail__summary">
        <div class="ticket-detail__summary-copy">
          <p class="ticket-detail__eyebrow">当前工单</p>
          <h4 class="ticket-detail__title">{{ ticket.ticketId }}</h4>
          <p class="ticket-detail__meta">
            订单 {{ ticket.orderId }} / 客户 {{ ticket.customerId }}
          </p>
          <p class="ticket-detail__meta">创建于 {{ formatDateTime(ticket.createdAt) }}</p>
        </div>
        <div class="ticket-detail__summary-tags">
          <StatusPill :label="labelOf(ticket.ticketStatus)" :tone="resolveTone()" />
          <StatusPill v-if="ticket.riskFlag" label="风险" tone="warn" />
        </div>
      </div>

      <div class="ticket-detail__context-grid">
        <article class="ticket-detail__context-card">
          <p class="ticket-detail__section-eyebrow">关联会话</p>
          <p class="ticket-detail__context-value">{{ linkedConversationCount }}</p>
          <p class="ticket-detail__section-copy">
            当前会话通道：{{ activeChannelLabel }}
          </p>
        </article>

        <article class="ticket-detail__context-card">
          <p class="ticket-detail__section-eyebrow">会话状态</p>
          <p class="ticket-detail__context-value">
            {{ labelOf(selectedConversation?.conversationStatus || 'pending') }}
          </p>
          <p class="ticket-detail__section-copy">
            风险标记：{{ selectedConversation?.riskFlag ? '需要人工升级处理' : '无风险标记' }}
          </p>
        </article>

        <article class="ticket-detail__context-card ticket-detail__context-card--wide">
          <p class="ticket-detail__section-eyebrow">售后草稿预览</p>
          <p class="ticket-detail__section-copy">
            {{
              generatedAfterSaleDraft?.reasonText ||
              '先在下方生成会话建议，再用售后面板整理最终的售后申请说明。'
            }}
          </p>
        </article>
      </div>

      <div class="ticket-detail__suggestion">
        <div class="ticket-detail__section-head">
          <div>
            <p class="ticket-detail__section-eyebrow">工单回复建议</p>
            <p class="ticket-detail__section-copy">
              在客服发出最终会话回复前，先用工单级建议保持队列流转。
            </p>
          </div>
          <button
            type="button"
            class="ticket-detail__primary"
            :disabled="isSubmitting"
            @click="emit('generateSuggestion')"
          >
            生成建议
          </button>
        </div>
        <div class="ticket-detail__suggestion-box">
          {{
            ticket.aiReplySuggestion ||
            '当前还没有工单建议，点击按钮为该服务工单生成首条处理草稿。'
          }}
        </div>
      </div>

      <form class="ticket-detail__satisfaction" @submit.prevent="submitSatisfaction">
        <div class="ticket-detail__section-head">
          <div>
            <p class="ticket-detail__section-eyebrow">结案满意度</p>
            <p class="ticket-detail__section-copy">
              在会话与售后步骤处理完成后，记录客户的最终反馈。
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
            <span>评分</span>
            <select v-model="satisfactionForm.score" :disabled="isSubmitting">
              <option value="5">5</option>
              <option value="4">4</option>
              <option value="3">3</option>
              <option value="2">2</option>
              <option value="1">1</option>
            </select>
          </label>
          <label class="ticket-detail__field ticket-detail__field--wide">
            <span>备注</span>
            <textarea
              v-model="satisfactionForm.comment"
              rows="4"
              :disabled="isSubmitting"
              placeholder="记录客户反馈、最终承诺或后续需要复盘的内容。"
            />
          </label>
        </div>

        <p v-if="satisfaction" class="ticket-detail__history">
          最近更新于 {{ formatDateTime(satisfaction.ratedAt) }}。
          {{ satisfaction.comment || '未填写备注。' }}
        </p>
        <p v-if="actionError" class="ticket-detail__error">{{ actionError }}</p>

        <button class="ticket-detail__ghost" type="submit" :disabled="isSubmitting">
          保存满意度
        </button>
      </form>
    </div>

    <div v-else class="ticket-detail__empty">
      先从队列中选择一个工单，再查看关联的客户上下文与结案状态。
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
  background: var(--color-surface);
  border: 1px solid var(--color-line);
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
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
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
  background: var(--color-surface);
  border: 1px solid var(--color-line);
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
  border: 1px solid var(--color-line);
  background: #ffffff;
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
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #ffffff;
}

.ticket-detail__primary:hover {
  background: var(--color-accent-hover);
}

.ticket-detail__ghost {
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.ticket-detail__ghost:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
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
