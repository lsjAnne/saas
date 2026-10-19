<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import ConversationDetailPanel from '@/features/customerServiceAssistant/components/ConversationDetailPanel.vue';
import ConversationInboxPanel from '@/features/customerServiceAssistant/components/ConversationInboxPanel.vue';
import ConversationSuggestionPanel from '@/features/customerServiceAssistant/components/ConversationSuggestionPanel.vue';
import AfterSaleWorkflowPanel from '@/features/customerServiceCenter/components/AfterSaleWorkflowPanel.vue';
import CustomerServiceCenterHeroPanel from '@/features/customerServiceCenter/components/CustomerServiceCenterHeroPanel.vue';
import SlaInsightPanel from '@/features/customerServiceCenter/components/SlaInsightPanel.vue';
import TicketDetailPanel from '@/features/customerServiceCenter/components/TicketDetailPanel.vue';
import TicketQueuePanel from '@/features/customerServiceCenter/components/TicketQueuePanel.vue';
import { useCustomerServiceCenterOverview } from '@/features/customerServiceCenter/composables/useCustomerServiceCenterOverview';
import type { CreateAfterSalePayload, SaveTicketSatisfactionPayload } from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const customerServiceCenter = useCustomerServiceCenterOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await customerServiceCenter.load(authStore.token);
}

async function withToken<T>(callback: (token: string) => Promise<T>) {
  if (!authStore.token) {
    return;
  }

  await callback(authStore.token);
}

async function handleSelectStore(storeId: string) {
  await withToken((token) => customerServiceCenter.selectStore(storeId, token));
}

async function handleSelectStatus(status: string) {
  await withToken((token) => customerServiceCenter.selectStatus(status, token));
}

async function handleSelectTicket(ticketId: string) {
  await withToken((token) => customerServiceCenter.selectTicket(ticketId, token));
}

async function handleSelectConversation(conversationId: string) {
  await withToken((token) => customerServiceCenter.selectConversation(conversationId, token));
}

async function handleGenerateTicketSuggestion() {
  await withToken((token) => customerServiceCenter.submitReplySuggestion(token));
}

async function handleGenerateConversationSuggestion() {
  await withToken((token) => customerServiceCenter.submitConversationSuggestion(token));
}

async function handleSendReply(preferredReply?: string) {
  await withToken((token) =>
    customerServiceCenter.submitConversationReply(token, preferredReply)
  );
}

async function handleTransferManual() {
  await withToken((token) => customerServiceCenter.submitConversationManualTransfer(token));
}

async function handleSaveSatisfaction(payload: SaveTicketSatisfactionPayload) {
  await withToken((token) => customerServiceCenter.submitSatisfaction(payload, token));
}

async function handleCreateAfterSale(payload: CreateAfterSalePayload) {
  await withToken((token) => customerServiceCenter.submitCreateAfterSale(payload, token));
}

async function handleOpenAfterSale(afterSaleId: string) {
  await withToken((token) => customerServiceCenter.openAfterSale(afterSaleId, token));
}

async function handleSubmitApproval() {
  await withToken((token) => customerServiceCenter.submitAfterSaleApproval(token));
}

function handleReplyDraftUpdate(value: string) {
  customerServiceCenter.replyDraft = value;
}

function handleManualReasonUpdate(value: string) {
  customerServiceCenter.manualReason = value;
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="customer-service-center-page">
    <header class="customer-service-center-page__header">
      <p class="customer-service-center-page__eyebrow">工单售后</p>
      <h2 class="customer-service-center-page__title">客服售后</h2>
      <p class="customer-service-center-page__subtitle">
        工单队列、SLA 与售后流程管理。
      </p>
    </header>

    <InlineErrorCard
      v-if="customerServiceCenter.errorMessage"
      :message="customerServiceCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <CustomerServiceCenterHeroPanel
      :selected-ticket="customerServiceCenter.selectedTicket"
      :selected-store-name="customerServiceCenter.selectedStoreName"
      :ticket-sla-overview="customerServiceCenter.ticketSlaOverview"
      :summary="customerServiceCenter.summary"
      :action-feedback="customerServiceCenter.actionFeedback"
    />

    <div class="customer-service-center-page__grid">
      <TicketQueuePanel
        :stores="customerServiceCenter.stores"
        :tickets="customerServiceCenter.filteredTickets"
        :selected-ticket-id="customerServiceCenter.selectedTicketId"
        :active-store-id="customerServiceCenter.activeStoreId"
        :status-filter="customerServiceCenter.statusFilter"
        :available-statuses="customerServiceCenter.availableStatuses"
        :is-loading="customerServiceCenter.isLoading"
        @select-store="handleSelectStore"
        @select-status="handleSelectStatus"
        @select-ticket="handleSelectTicket"
      />

      <div class="customer-service-center-page__stack">
        <TicketDetailPanel
          :ticket="customerServiceCenter.selectedTicket"
          :linked-conversation-count="customerServiceCenter.linkedConversations.length"
          :selected-conversation="customerServiceCenter.selectedConversation"
          :generated-after-sale-draft="customerServiceCenter.generatedAfterSaleDraft"
          :satisfaction="customerServiceCenter.currentTicketSatisfaction"
          :is-submitting="customerServiceCenter.isRunningAction"
          :action-error="customerServiceCenter.actionError"
          @generate-suggestion="handleGenerateTicketSuggestion"
          @save-satisfaction="handleSaveSatisfaction"
        />

        <ConversationInboxPanel
          :conversations="customerServiceCenter.linkedConversations"
          :selected-conversation-id="customerServiceCenter.selectedConversation?.conversationId ?? null"
          @select="handleSelectConversation"
        />

        <ConversationDetailPanel
          :conversation="customerServiceCenter.selectedConversation"
          :messages="customerServiceCenter.conversationMessages"
          :active-support-session="null"
          :can-view-controlled-detail="true"
          :gate-message="null"
          :is-loading-messages="customerServiceCenter.isLoadingConversationMessages"
          :reply-draft="customerServiceCenter.replyDraft"
          :manual-reason="customerServiceCenter.manualReason"
          :is-sending-message="customerServiceCenter.isSendingConversationReply"
          :is-transferring-manual="customerServiceCenter.isTransferringConversationManual"
          @update:reply-draft="handleReplyDraftUpdate"
          @update:manual-reason="handleManualReasonUpdate"
          @send="handleSendReply()"
          @transfer="handleTransferManual"
        />

        <ConversationSuggestionPanel
          :conversation="customerServiceCenter.selectedConversation"
          :suggestion="customerServiceCenter.conversationSuggestion"
          :active-support-session="null"
          :is-generating-suggestion="customerServiceCenter.isGeneratingConversationSuggestion"
          :can-view-controlled-detail="true"
          @generate="handleGenerateConversationSuggestion"
          @apply-suggestion="handleSendReply"
        />

        <AfterSaleWorkflowPanel
          :ticket="customerServiceCenter.selectedTicket"
          :conversation="customerServiceCenter.selectedConversation"
          :messages="customerServiceCenter.conversationMessages"
          :generated-after-sale-draft="customerServiceCenter.generatedAfterSaleDraft"
          :after-sale-record="customerServiceCenter.selectedAfterSaleRecord"
          :active-after-sale-id="customerServiceCenter.activeAfterSaleId"
          :is-submitting="customerServiceCenter.isRunningAction"
          :action-error="customerServiceCenter.actionError"
          @create-after-sale="handleCreateAfterSale"
          @open-after-sale="handleOpenAfterSale"
          @submit-approval="handleSubmitApproval"
        />

        <SlaInsightPanel
          :ticket-sla-overview="customerServiceCenter.ticketSlaOverview"
          :filtered-ticket-count="customerServiceCenter.filteredTickets.length"
          :selected-store-name="customerServiceCenter.selectedStoreName"
          :selected-ticket="customerServiceCenter.selectedTicket"
          :selected-conversation="customerServiceCenter.selectedConversation"
          :conversation-messages="customerServiceCenter.conversationMessages"
          :after-sale-record="customerServiceCenter.selectedAfterSaleRecord"
        />
      </div>
    </div>

    <p v-if="customerServiceCenter.isLoading" class="customer-service-center-page__footer-note">
      正在回读工单、关联会话和售后上下文...
    </p>
  </section>
</template>

<style scoped>
.customer-service-center-page {
  display: grid;
  gap: 1.2rem;
}

.customer-service-center-page__header {
  display: grid;
  gap: 0.75rem;
}

.customer-service-center-page__eyebrow,
.customer-service-center-page__title,
.customer-service-center-page__subtitle,
.customer-service-center-page__footer-note {
  margin: 0;
}

.customer-service-center-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.customer-service-center-page__title {
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
  line-height: 1.2;
}

.customer-service-center-page__subtitle,
.customer-service-center-page__footer-note {
  max-width: 64rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.customer-service-center-page__grid {
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  gap: 1rem;
  align-items: start;
}

.customer-service-center-page__stack {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1180px) {
  .customer-service-center-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
