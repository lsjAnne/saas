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
      <p class="customer-service-assistant-page__eyebrow">客服辅助</p>
      <h2 class="customer-service-assistant-page__title">客户服务辅助</h2>
      <p class="customer-service-assistant-page__subtitle">
        会话洞察与回复建议，辅助客服高效接待。
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
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.customer-service-assistant-page__title {
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
  line-height: 1.2;
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
