<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  ConversationMessage,
  CustomerConversation,
  SupportSession
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  conversation: CustomerConversation | null;
  messages: ConversationMessage[];
  activeSupportSession: SupportSession | null;
  canViewControlledDetail: boolean;
  gateMessage: string | null;
  isLoadingMessages: boolean;
  replyDraft: string;
  manualReason: string;
  isSendingMessage: boolean;
  isTransferringManual: boolean;
}

defineProps<Props>();

defineEmits<{
  'update:replyDraft': [value: string];
  'update:manualReason': [value: string];
  send: [];
  transfer: [];
}>();

function toneFromMessage(message: ConversationMessage) {
  if (message.riskFlag) {
    return 'warn';
  }

  return message.aiGeneratedFlag ? 'neutral' : 'success';
}
</script>

<template>
  <PanelCard
    eyebrow="Conversation"
    title="消息流与人工接管"
    description="先读会话上下文，再决定直接回复还是升级人工接管，避免在高风险问题上误发。"
  >
    <div v-if="conversation" class="conversation-detail">
      <div class="conversation-detail__summary">
        <div>
          <h4 class="conversation-detail__title">{{ conversation.customerId }}</h4>
          <p class="conversation-detail__meta">
            {{ conversation.platformType }} · {{ conversation.storeId }} ·
            {{ conversation.platformConversationId }}
          </p>
        </div>
        <StatusPill
          :label="conversation.riskFlag ? 'risk' : conversation.conversationStatus"
          :tone="conversation.riskFlag ? 'warn' : 'success'"
        />
      </div>

      <div v-if="activeSupportSession" class="conversation-detail__gate conversation-detail__gate--success">
        支持会话已生效：{{ activeSupportSession.requesterId }} · 到期
        {{ formatDateTime(activeSupportSession.expiresAt) }}
      </div>
      <div
        v-else-if="gateMessage"
        class="conversation-detail__gate conversation-detail__gate--warn"
      >
        {{ gateMessage }}
      </div>

      <div class="conversation-detail__stream">
        <article
          v-for="message in messages"
          :key="message.messageId"
          class="conversation-detail__message"
        >
          <div class="conversation-detail__message-head">
            <p class="conversation-detail__sender">{{ message.senderType }}</p>
            <StatusPill
              :label="message.messageType"
              :tone="toneFromMessage(message)"
            />
          </div>
          <p class="conversation-detail__content">{{ message.contentText }}</p>
          <p class="conversation-detail__time">{{ formatDateTime(message.createdAt) }}</p>
        </article>

        <p v-if="isLoadingMessages" class="conversation-detail__hint">
          正在加载会话消息...
        </p>
        <p v-else-if="!messages.length" class="conversation-detail__hint">
          当前会话还没有消息。
        </p>
      </div>

      <div class="conversation-detail__actions">
        <label class="conversation-detail__field">
          <span>直接回复</span>
          <textarea
            :value="replyDraft"
            rows="4"
            :disabled="!canViewControlledDetail"
            placeholder="输入准备发送给客户的回复内容"
            @input="$emit('update:replyDraft', ($event.target as HTMLTextAreaElement).value)"
          />
        </label>

        <div class="conversation-detail__buttons">
          <button
            class="conversation-detail__button conversation-detail__button--primary"
            type="button"
            :disabled="!canViewControlledDetail || isSendingMessage"
            @click="$emit('send')"
          >
            {{ isSendingMessage ? '发送中...' : '发送回复' }}
          </button>
        </div>
      </div>

      <div class="conversation-detail__actions conversation-detail__actions--manual">
        <label class="conversation-detail__field">
          <span>人工接管原因</span>
          <textarea
            :value="manualReason"
            rows="3"
            :disabled="!canViewControlledDetail"
            placeholder="说明为什么需要人工接管"
            @input="$emit('update:manualReason', ($event.target as HTMLTextAreaElement).value)"
          />
        </label>

        <div class="conversation-detail__buttons">
          <button
            class="conversation-detail__button conversation-detail__button--secondary"
            type="button"
            :disabled="!canViewControlledDetail || isTransferringManual"
            @click="$emit('transfer')"
          >
            {{ isTransferringManual ? '转人工中...' : '转人工接管' }}
          </button>
        </div>
      </div>
    </div>

    <p v-else class="conversation-detail__hint">
      先从左侧选择一条会话。
    </p>
  </PanelCard>
</template>

<style scoped>
.conversation-detail {
  display: grid;
  gap: 1rem;
}

.conversation-detail__summary,
.conversation-detail__message-head,
.conversation-detail__buttons {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.conversation-detail__title,
.conversation-detail__meta,
.conversation-detail__sender,
.conversation-detail__content,
.conversation-detail__time,
.conversation-detail__hint {
  margin: 0;
}

.conversation-detail__title {
  color: var(--color-ink-strong);
}

.conversation-detail__meta,
.conversation-detail__time,
.conversation-detail__hint {
  color: var(--color-ink-soft);
}

.conversation-detail__gate {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  font-size: 0.92rem;
}

.conversation-detail__gate--success {
  color: var(--color-success);
  background: rgba(47, 109, 83, 0.12);
}

.conversation-detail__gate--warn {
  color: var(--color-warning);
  background: rgba(183, 117, 47, 0.12);
}

.conversation-detail__stream {
  display: grid;
  gap: 0.8rem;
}

.conversation-detail__message {
  display: grid;
  gap: 0.55rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.conversation-detail__sender {
  color: var(--color-ink);
  font-weight: 600;
}

.conversation-detail__content {
  color: var(--color-ink-strong);
  line-height: 1.7;
}

.conversation-detail__actions {
  display: grid;
  gap: 0.8rem;
}

.conversation-detail__field {
  display: grid;
  gap: 0.5rem;
}

.conversation-detail__field span {
  color: var(--color-ink-soft);
  font-size: 0.88rem;
}

.conversation-detail__field textarea {
  width: 100%;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 253, 250, 0.9);
  resize: vertical;
}

.conversation-detail__field textarea:focus {
  outline: none;
  border-color: rgba(133, 98, 67, 0.32);
  box-shadow: 0 0 0 4px rgba(133, 98, 67, 0.1);
}

.conversation-detail__button {
  min-height: 2.9rem;
  padding: 0 1.1rem;
  border-radius: var(--radius-pill);
  border: 0;
}

.conversation-detail__button--primary {
  background: linear-gradient(135deg, #2d241c, #856243);
  color: #fff8f0;
}

.conversation-detail__button--secondary {
  background: rgba(183, 117, 47, 0.14);
  color: var(--color-warning);
}
</style>
