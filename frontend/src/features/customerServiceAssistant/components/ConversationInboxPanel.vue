<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { CustomerConversation } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  conversations: CustomerConversation[];
  selectedConversationId: string | null;
}

defineProps<Props>();

defineEmits<{
  select: [conversationId: string];
}>();

function toneFromConversation(conversation: CustomerConversation) {
  if (conversation.riskFlag || conversation.conversationStatus === 'waiting_manual') {
    return 'warn';
  }

  if (conversation.conversationStatus === 'closed') {
    return 'neutral';
  }

  return 'success';
}

const statusLabelMap: Record<string, string> = {
  waiting_manual: '待人工接管',
  closed: '已结束',
  pending: '待处理'
};

function labelOf(value: string) {
  return statusLabelMap[value] ?? value;
}
</script>

<template>
  <PanelCard
    title="会话列表"
    description="按平台、客户、风险和当前状态聚合待处理会话，优先把高风险和待人工接管项排到前面。"
  >
    <div class="conversation-inbox">
      <button
        v-for="conversation in conversations"
        :key="conversation.conversationId"
        type="button"
        :class="[
          'conversation-inbox__item',
          {
            'conversation-inbox__item--active':
              selectedConversationId === conversation.conversationId
          }
        ]"
        @click="$emit('select', conversation.conversationId)"
      >
        <div class="conversation-inbox__head">
          <div>
            <h4 class="conversation-inbox__title">{{ conversation.customerId }}</h4>
            <p class="conversation-inbox__meta">
              {{ conversation.platformType }} · {{ conversation.storeId }}
            </p>
          </div>
          <StatusPill
            :label="labelOf(conversation.conversationStatus)"
            :tone="toneFromConversation(conversation)"
          />
        </div>
        <p class="conversation-inbox__foot">
          会话 {{ conversation.platformConversationId }} · 最近消息
          {{ formatDateTime(conversation.lastMessageAt) }}
        </p>
      </button>

      <p v-if="!conversations.length" class="conversation-inbox__empty">
        当前租户还没有客户会话。
      </p>
    </div>
  </PanelCard>
</template>

<style scoped>
.conversation-inbox {
  display: grid;
  gap: 0.9rem;
}

.conversation-inbox__item {
  display: grid;
  gap: 0.7rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
  text-align: left;
  transition:
    transform var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.conversation-inbox__item:hover {
  transform: translateY(-1px);
  border-color: var(--color-accent);
}

.conversation-inbox__item--active {
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
}

.conversation-inbox__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.conversation-inbox__title,
.conversation-inbox__meta,
.conversation-inbox__foot,
.conversation-inbox__empty {
  margin: 0;
}

.conversation-inbox__title {
  color: var(--color-ink-strong);
}

.conversation-inbox__meta,
.conversation-inbox__foot,
.conversation-inbox__empty {
  color: var(--color-ink-soft);
}

.conversation-inbox__meta {
  margin-top: 0.3rem;
}
</style>
