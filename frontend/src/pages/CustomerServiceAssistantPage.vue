<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import ConversationDetailPanel from '@/features/customerServiceAssistant/components/ConversationDetailPanel.vue';
import ConversationInboxPanel from '@/features/customerServiceAssistant/components/ConversationInboxPanel.vue';
import ConversationSuggestionPanel from '@/features/customerServiceAssistant/components/ConversationSuggestionPanel.vue';
import { useCustomerServiceAssistant } from '@/features/customerServiceAssistant/composables/useCustomerServiceAssistant';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const customerServiceAssistant = useCustomerServiceAssistant();
const replyDraft = shallowRef('');
const manualReason = shallowRef('');

authStore.hydrate();

const canViewControlledDetail = computed(() =>
  customerServiceAssistant.hasControlledDetailAccess(
    authStore.user?.operatorType || ''
  )
);

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await customerServiceAssistant.load(
    authStore.token,
    authStore.user?.operatorType || ''
  );
}

async function handleSelectConversation(conversationId: string) {
  if (!authStore.token) {
    return;
  }

  replyDraft.value = '';
  manualReason.value = '';

  await customerServiceAssistant.selectConversation(
    conversationId,
    authStore.token,
    authStore.user?.operatorType || ''
  );
}

async function handleGenerateSuggestion() {
  if (!authStore.token) {
    return;
  }

  const suggestion = await customerServiceAssistant.generateSuggestion(
    authStore.token,
    authStore.user?.operatorType || ''
  );

  if (suggestion) {
    replyDraft.value = suggestion.suggestedReply;
  }
}

async function handleSendReply(preferredReply?: string) {
  if (!authStore.token) {
    return;
  }

  const content = preferredReply ?? replyDraft.value;
  await customerServiceAssistant.sendReply(
    authStore.token,
    authStore.user?.operatorType || '',
    content
  );
  replyDraft.value = '';
}

async function handleTransferManual() {
  if (!authStore.token) {
    return;
  }

  await customerServiceAssistant.transferManual(
    authStore.token,
    authStore.user?.operatorType || '',
    manualReason.value
  );
  manualReason.value = '';
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="customer-service-assistant-page">
    <header class="customer-service-assistant-page__header">
      <p class="customer-service-assistant-page__eyebrow">Customer Service Assistant</p>
      <h2 class="customer-service-assistant-page__title">
        把会话队列、消息流和 AI 建议放进同一条客服辅助链路
      </h2>
      <p class="customer-service-assistant-page__subtitle">
        这一页强调“先识别风险，再决定直接回复还是人工接管”，并把支持会话 gate 明确暴露在界面上。
      </p>
    </header>

    <InlineErrorCard
      v-if="customerServiceAssistant.errorMessage"
      :message="customerServiceAssistant.errorMessage"
      @retry="bootstrapPage"
    />

    <div class="customer-service-assistant-page__grid">
      <ConversationInboxPanel
        :conversations="customerServiceAssistant.conversations"
        :selected-conversation-id="customerServiceAssistant.selectedConversationId"
        @select="handleSelectConversation"
      />

      <ConversationDetailPanel
        :conversation="customerServiceAssistant.selectedConversation"
        :messages="customerServiceAssistant.messages"
        :active-support-session="customerServiceAssistant.activeSupportSession"
        :can-view-controlled-detail="canViewControlledDetail"
        :gate-message="customerServiceAssistant.gateMessage"
        :is-loading-messages="customerServiceAssistant.isLoadingMessages"
        :reply-draft="replyDraft"
        :manual-reason="manualReason"
        :is-sending-message="customerServiceAssistant.isSendingMessage"
        :is-transferring-manual="customerServiceAssistant.isTransferringManual"
        @update:reply-draft="replyDraft = $event"
        @update:manual-reason="manualReason = $event"
        @send="handleSendReply()"
        @transfer="handleTransferManual"
      />
    </div>

    <ConversationSuggestionPanel
      :conversation="customerServiceAssistant.selectedConversation"
      :suggestion="customerServiceAssistant.suggestion"
      :active-support-session="customerServiceAssistant.activeSupportSession"
      :is-generating-suggestion="customerServiceAssistant.isGeneratingSuggestion"
      :can-view-controlled-detail="canViewControlledDetail"
      @generate="handleGenerateSuggestion"
      @apply-suggestion="handleSendReply"
    />
  </section>
</template>

<style scoped>
.customer-service-assistant-page {
  display: grid;
  gap: 1.2rem;
}

.customer-service-assistant-page__header {
  display: grid;
  gap: 0.75rem;
}

.customer-service-assistant-page__eyebrow,
.customer-service-assistant-page__title,
.customer-service-assistant-page__subtitle {
  margin: 0;
}

.customer-service-assistant-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.customer-service-assistant-page__title {
  max-width: 16ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.1rem, 3.8vw, 3.6rem);
  line-height: 1.05;
}

.customer-service-assistant-page__subtitle {
  max-width: 60rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.customer-service-assistant-page__grid {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 1rem;
}

@media (max-width: 1080px) {
  .customer-service-assistant-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
