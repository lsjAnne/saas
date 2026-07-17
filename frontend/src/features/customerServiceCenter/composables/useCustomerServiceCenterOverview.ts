import { computed, shallowRef } from 'vue';

import {
  generateConversationReplySuggestion,
  getConversationMessages,
  getConversations,
  sendConversationMessage,
  transferConversationManual
} from '@/features/customerServiceAssistant/services/customerServiceAssistantService';
import {
  createAfterSale,
  generateTicketReplySuggestion,
  getAfterSale,
  getTicket,
  getTicketSlaOverview,
  getTickets,
  saveTicketSatisfaction,
  submitAfterSaleApproval as submitAfterSaleApprovalRequest
} from '@/features/customerServiceCenter/services/customerServiceCenterService';
import { getStores } from '@/services/storeChannelService';
import type {
  AfterSaleRecord,
  ConversationMessage,
  CreateAfterSalePayload,
  CustomerConversation,
  CustomerServiceTicket,
  ReplySuggestionView,
  SaveTicketSatisfactionPayload,
  Store,
  TicketSatisfactionView,
  TicketSlaOverviewView
} from '@/services/apiTypes';

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function ticketPriority(ticket: CustomerServiceTicket) {
  const status = normalizeText(ticket.ticketStatus);

  if (ticket.riskFlag) {
    return 5;
  }

  if (status === 'processing') {
    return 4;
  }

  if (ticket.aiReplySuggestion) {
    return 3;
  }

  if (status === 'created') {
    return 2;
  }

  return 1;
}

export function useCustomerServiceCenterOverview() {
  const stores = shallowRef<Store[]>([]);
  const tickets = shallowRef<CustomerServiceTicket[]>([]);
  const conversations = shallowRef<CustomerConversation[]>([]);
  const conversationMessages = shallowRef<ConversationMessage[]>([]);
  const ticketSlaOverview = shallowRef<TicketSlaOverviewView | null>(null);
  const ticketSatisfactions = shallowRef<Record<string, TicketSatisfactionView>>({});
  const selectedTicketId = shallowRef<string | null>(null);
  const selectedConversationId = shallowRef<string | null>(null);
  const selectedTicketDetail = shallowRef<CustomerServiceTicket | null>(null);
  const selectedAfterSaleRecord = shallowRef<AfterSaleRecord | null>(null);
  const conversationSuggestion = shallowRef<ReplySuggestionView | null>(null);
  const activeAfterSaleId = shallowRef('');
  const activeStoreId = shallowRef('all');
  const statusFilter = shallowRef('all');
  const replyDraft = shallowRef('');
  const manualReason = shallowRef('');
  const isLoading = shallowRef(false);
  const isLoadingConversationMessages = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const isGeneratingConversationSuggestion = shallowRef(false);
  const isSendingConversationReply = shallowRef(false);
  const isTransferringConversationManual = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const sortedStores = computed(() =>
    [...stores.value].sort(
      (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const sortedTickets = computed(() =>
    [...tickets.value].sort((left, right) => {
      const priorityGap = ticketPriority(right) - ticketPriority(left);
      if (priorityGap !== 0) {
        return priorityGap;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const filteredTickets = computed(() =>
    sortedTickets.value.filter((ticket) => {
      const storeMatched =
        activeStoreId.value === 'all' ? true : ticket.storeId === activeStoreId.value;
      const statusMatched =
        statusFilter.value === 'all' ? true : ticket.ticketStatus === statusFilter.value;

      return storeMatched && statusMatched;
    })
  );

  const selectedTicket = computed(() => {
    const source = selectedTicketDetail.value;

    if (source && source.ticketId === selectedTicketId.value) {
      return source;
    }

    return (
      filteredTickets.value.find((ticket) => ticket.ticketId === selectedTicketId.value) ??
      sortedTickets.value.find((ticket) => ticket.ticketId === selectedTicketId.value) ??
      null
    );
  });

  const linkedConversations = computed(() => {
    const ticket = selectedTicket.value;

    if (!ticket) {
      return [];
    }

    return [...conversations.value]
      .filter(
        (conversation) =>
          conversation.storeId === ticket.storeId &&
          conversation.customerId === ticket.customerId
      )
      .sort(
        (left, right) => toTimestamp(right.lastMessageAt) - toTimestamp(left.lastMessageAt)
      );
  });

  const selectedConversation = computed(
    () =>
      linkedConversations.value.find(
        (conversation) => conversation.conversationId === selectedConversationId.value
      ) ?? linkedConversations.value[0] ?? null
  );

  const currentTicketSatisfaction = computed(() =>
    selectedTicketId.value ? ticketSatisfactions.value[selectedTicketId.value] ?? null : null
  );

  const selectedStoreName = computed(() => {
    const storeId =
      selectedTicket.value?.storeId ?? (activeStoreId.value !== 'all' ? activeStoreId.value : '');

    if (!storeId) {
      return 'All Stores';
    }

    return sortedStores.value.find((store) => store.storeId === storeId)?.shopName ?? storeId;
  });

  const availableStatuses = computed(() =>
    [...new Set(sortedTickets.value.map((ticket) => ticket.ticketStatus).filter(Boolean))]
  );

  const summary = computed(() => {
    const scopedTickets = filteredTickets.value;
    const totalTicketCount = ticketSlaOverview.value?.totalTicketCount ?? scopedTickets.length;
    const withinSlaCount = ticketSlaOverview.value?.withinSlaCount ?? 0;

    return {
      totalTicketCount,
      riskTicketCount: scopedTickets.filter((ticket) => ticket.riskFlag).length,
      processingTicketCount: scopedTickets.filter(
        (ticket) => normalizeText(ticket.ticketStatus) === 'processing'
      ).length,
      overdueTicketCount: ticketSlaOverview.value?.overdueTicketCount ?? 0,
      withinSlaRate: totalTicketCount > 0 ? withinSlaCount / totalTicketCount : 0,
      averageFirstResponseMinutes: Number(
        ticketSlaOverview.value?.averageFirstResponseMinutes ?? 0
      ),
      satisfactionCount: ticketSlaOverview.value?.satisfactionCount ?? 0,
      averageSatisfactionScore: Number(
        ticketSlaOverview.value?.averageSatisfactionScore ?? 0
      )
    };
  });

  const generatedAfterSaleDraft = computed(() => {
    const ticket = selectedTicket.value;

    if (!ticket) {
      return null;
    }

    const conversation = selectedConversation.value;
    const recentMessages = [...conversationMessages.value].slice(-3);
    const latestCustomerMessage = [...conversationMessages.value]
      .reverse()
      .find((message) => message.senderType === 'customer');
    const latestHandledMessage = [...conversationMessages.value]
      .reverse()
      .find((message) => message.senderType !== 'customer');
    const riskSignals = [
      ticket.riskFlag ? 'ticket marked risk' : '',
      conversation?.riskFlag ? 'conversation in manual risk state' : '',
      normalizeText(ticket.ticketStatus) === 'processing' ? 'ticket already processing' : '',
      currentTicketSatisfaction.value
        ? `latest satisfaction ${currentTicketSatisfaction.value.score}/5`
        : ''
    ].filter(Boolean);

    const reasonText = [
      `Order ${ticket.orderId} needs after-sale follow-up.`,
      latestCustomerMessage
        ? `Customer request: ${latestCustomerMessage.contentText}`
        : `Customer ${ticket.customerId} opened a service ticket.`,
      conversationSuggestion.value?.suggestedReply
        ? `Suggested reply: ${conversationSuggestion.value.suggestedReply}`
        : '',
      riskSignals.length ? `Risk signals: ${riskSignals.join('; ')}` : ''
    ]
      .filter(Boolean)
      .join(' ');

    const evidenceBlob = [
      `ticketId=${ticket.ticketId}`,
      `orderId=${ticket.orderId}`,
      `customerId=${ticket.customerId}`,
      `ticketStatus=${ticket.ticketStatus}`,
      `ticketRisk=${ticket.riskFlag}`,
      conversation
        ? `conversation=${conversation.platformType}/${conversation.platformConversationId}/${conversation.conversationStatus}`
        : 'conversation=none',
      latestHandledMessage
        ? `latestHandledMessage=${latestHandledMessage.senderType}:${latestHandledMessage.contentText}`
        : 'latestHandledMessage=none',
      ...recentMessages.map(
        (message) =>
          `message=${message.senderType}/${message.messageType}/${message.contentText}`
      )
    ].join('\n');

    return {
      reasonText,
      evidenceBlob
    };
  });

  function currentStoreId() {
    return activeStoreId.value === 'all' ? undefined : activeStoreId.value;
  }

  function clearAfterSaleContext() {
    selectedAfterSaleRecord.value = null;
    activeAfterSaleId.value = '';
  }

  function clearConversationContext() {
    selectedConversationId.value = null;
    conversationMessages.value = [];
    conversationSuggestion.value = null;
    replyDraft.value = '';
    manualReason.value = '';
  }

  function applySelection(preferredTicketId?: string | null) {
    const previousTicketId = selectedTicketId.value;
    const nextTicketId =
      preferredTicketId &&
      filteredTickets.value.some((ticket) => ticket.ticketId === preferredTicketId)
        ? preferredTicketId
        : filteredTickets.value[0]?.ticketId ?? sortedTickets.value[0]?.ticketId ?? null;

    selectedTicketId.value = nextTicketId;
    selectedTicketDetail.value = null;

    if (previousTicketId !== nextTicketId) {
      clearAfterSaleContext();
      clearConversationContext();
    }
  }

  function applyConversationSelection(preferredConversationId?: string | null) {
    const nextConversationId =
      preferredConversationId &&
      linkedConversations.value.some(
        (conversation) => conversation.conversationId === preferredConversationId
      )
        ? preferredConversationId
        : linkedConversations.value[0]?.conversationId ?? null;

    if (selectedConversationId.value !== nextConversationId) {
      conversationSuggestion.value = null;
      replyDraft.value = '';
      manualReason.value = '';
    }

    selectedConversationId.value = nextConversationId;
  }

  async function refreshTicketBoard(token: string, preferredTicketId?: string | null) {
    const [ticketsPayload, slaPayload] = await Promise.all([
      getTickets(token),
      getTicketSlaOverview(currentStoreId(), token)
    ]);

    tickets.value = ticketsPayload;
    ticketSlaOverview.value = slaPayload;
    applySelection(preferredTicketId ?? selectedTicketId.value);
  }

  async function refreshConversationBoard(
    token: string,
    preferredConversationId?: string | null
  ) {
    conversations.value = await getConversations(token);
    applyConversationSelection(preferredConversationId ?? selectedConversationId.value);
  }

  async function refreshSelectedContext(token: string) {
    if (!selectedTicketId.value) {
      selectedTicketDetail.value = null;
      return;
    }

    selectedTicketDetail.value = await getTicket(selectedTicketId.value, token);
  }

  async function refreshSelectedConversationMessages(token: string) {
    if (!selectedConversationId.value) {
      conversationMessages.value = [];
      return;
    }

    isLoadingConversationMessages.value = true;

    try {
      conversationMessages.value = await getConversationMessages(
        selectedConversationId.value,
        token
      );
    } finally {
      isLoadingConversationMessages.value = false;
    }
  }

  async function load(token: string, preferredTicketId?: string | null) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const storesPayload = await getStores(token);
      stores.value = storesPayload;

      if (
        activeStoreId.value !== 'all' &&
        !storesPayload.some((store) => store.storeId === activeStoreId.value)
      ) {
        activeStoreId.value = storesPayload[0]?.storeId ?? 'all';
      }

      if (activeStoreId.value === 'all' && storesPayload.length === 1) {
        activeStoreId.value = storesPayload[0].storeId;
      }

      await refreshTicketBoard(token, preferredTicketId);
      await refreshConversationBoard(token);
      await Promise.all([
        refreshSelectedContext(token),
        refreshSelectedConversationMessages(token)
      ]);
    } catch (error) {
      errorMessage.value =
        error instanceof Error
          ? error.message
          : 'Customer service center load failed. Please retry later.';
    } finally {
      isLoading.value = false;
    }
  }

  async function selectStore(storeId: string, token: string) {
    activeStoreId.value = storeId;
    await refreshTicketBoard(token);
    await refreshConversationBoard(token);
    await Promise.all([
      refreshSelectedContext(token),
      refreshSelectedConversationMessages(token)
    ]);
  }

  async function selectStatus(status: string, token: string) {
    statusFilter.value = status;
    applySelection(selectedTicketId.value);
    applyConversationSelection(selectedConversationId.value);
    await Promise.all([
      refreshSelectedContext(token),
      refreshSelectedConversationMessages(token)
    ]);
  }

  async function selectTicket(ticketId: string, token: string) {
    if (selectedTicketId.value !== ticketId) {
      clearAfterSaleContext();
      clearConversationContext();
    }

    selectedTicketId.value = ticketId;
    selectedTicketDetail.value = null;
    applyConversationSelection();
    await Promise.all([
      refreshSelectedContext(token),
      refreshSelectedConversationMessages(token)
    ]);
  }

  async function selectConversation(conversationId: string, token: string) {
    selectedConversationId.value = conversationId;
    conversationSuggestion.value = null;
    replyDraft.value = '';
    manualReason.value = '';
    await refreshSelectedConversationMessages(token);
  }

  async function runAction(callback: () => Promise<void>) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      await callback();
    } catch (error) {
      actionError.value =
        error instanceof Error
          ? error.message
          : 'Customer service action failed. Please retry later.';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitReplySuggestion(token: string) {
    if (!selectedTicketId.value) {
      return;
    }

    await runAction(async () => {
      const updated = await generateTicketReplySuggestion(selectedTicketId.value, token);
      actionFeedback.value = `Generated ticket reply suggestion for ${updated.ticketId}.`;
      await refreshTicketBoard(token, updated.ticketId);
      await refreshSelectedContext(token);
    });
  }

  async function submitConversationSuggestion(token: string) {
    if (!selectedConversationId.value) {
      return;
    }

    isGeneratingConversationSuggestion.value = true;
    actionError.value = null;

    try {
      const suggestion = await generateConversationReplySuggestion(
        selectedConversationId.value,
        token
      );
      conversationSuggestion.value = suggestion;
      replyDraft.value = suggestion.suggestedReply;
      actionFeedback.value = `Generated conversation suggestion for ${suggestion.conversationId}.`;
      await refreshConversationBoard(token, suggestion.conversationId);
      await refreshSelectedConversationMessages(token);
    } catch (error) {
      actionError.value =
        error instanceof Error
          ? error.message
          : 'Conversation suggestion generation failed. Please retry later.';
    } finally {
      isGeneratingConversationSuggestion.value = false;
    }
  }

  async function submitConversationReply(token: string, preferredReply?: string) {
    if (!selectedConversationId.value) {
      return;
    }

    const contentText = (preferredReply ?? replyDraft.value).trim();

    if (!contentText) {
      return;
    }

    isSendingConversationReply.value = true;
    actionError.value = null;

    try {
      await sendConversationMessage(selectedConversationId.value, contentText, token);
      replyDraft.value = '';
      conversationSuggestion.value = null;
      actionFeedback.value = `Sent reply to conversation ${selectedConversationId.value}.`;
      await refreshConversationBoard(token, selectedConversationId.value);
      await refreshSelectedConversationMessages(token);
    } catch (error) {
      actionError.value =
        error instanceof Error
          ? error.message
          : 'Conversation reply send failed. Please retry later.';
    } finally {
      isSendingConversationReply.value = false;
    }
  }

  async function submitConversationManualTransfer(token: string, preferredReason?: string) {
    if (!selectedConversationId.value) {
      return;
    }

    const reasonText = (preferredReason ?? manualReason.value).trim();

    if (!reasonText) {
      return;
    }

    isTransferringConversationManual.value = true;
    actionError.value = null;

    try {
      await transferConversationManual(selectedConversationId.value, reasonText, token);
      manualReason.value = '';
      conversationSuggestion.value = null;
      actionFeedback.value = `Transferred conversation ${selectedConversationId.value} to manual handling.`;
      await refreshConversationBoard(token, selectedConversationId.value);
      await refreshSelectedConversationMessages(token);
    } catch (error) {
      actionError.value =
        error instanceof Error
          ? error.message
          : 'Conversation manual transfer failed. Please retry later.';
    } finally {
      isTransferringConversationManual.value = false;
    }
  }

  async function submitSatisfaction(
    payload: SaveTicketSatisfactionPayload,
    token: string
  ) {
    if (!selectedTicketId.value) {
      return;
    }

    await runAction(async () => {
      const saved = await saveTicketSatisfaction(selectedTicketId.value, payload, token);
      ticketSatisfactions.value = {
        ...ticketSatisfactions.value,
        [saved.ticketId]: saved
      };
      actionFeedback.value = `Saved satisfaction ${saved.score}/5 for ${saved.ticketId}.`;
    });
  }

  async function submitCreateAfterSale(payload: CreateAfterSalePayload, token: string) {
    await runAction(async () => {
      const created = await createAfterSale(payload, token);
      selectedAfterSaleRecord.value = created;
      activeAfterSaleId.value = created.afterSaleId;
      actionFeedback.value = `Created after-sale record ${created.afterSaleId}.`;
    });
  }

  async function openAfterSale(afterSaleId: string, token: string) {
    await runAction(async () => {
      const record = await getAfterSale(afterSaleId, token);
      selectedAfterSaleRecord.value = record;
      activeAfterSaleId.value = record.afterSaleId;
      actionFeedback.value = `Loaded after-sale record ${record.afterSaleId}.`;
    });
  }

  async function submitAfterSaleApproval(token: string) {
    if (!activeAfterSaleId.value) {
      return;
    }

    await runAction(async () => {
      const updated = await submitAfterSaleApprovalRequest(activeAfterSaleId.value, token);
      selectedAfterSaleRecord.value = updated;
      actionFeedback.value = `Submitted after-sale approval for ${updated.afterSaleId}.`;
    });
  }

  return {
    stores: sortedStores,
    tickets: sortedTickets,
    linkedConversations,
    selectedConversation,
    conversationMessages,
    conversationSuggestion,
    filteredTickets,
    ticketSlaOverview,
    currentTicketSatisfaction,
    selectedTicketId,
    selectedTicket,
    selectedStoreName,
    selectedAfterSaleRecord,
    activeAfterSaleId,
    availableStatuses,
    summary,
    generatedAfterSaleDraft,
    activeStoreId,
    statusFilter,
    replyDraft,
    manualReason,
    isLoading,
    isLoadingConversationMessages,
    isRunningAction,
    isGeneratingConversationSuggestion,
    isSendingConversationReply,
    isTransferringConversationManual,
    errorMessage,
    actionError,
    actionFeedback,
    load,
    selectStore,
    selectStatus,
    selectTicket,
    selectConversation,
    submitReplySuggestion,
    submitConversationSuggestion,
    submitConversationReply,
    submitConversationManualTransfer,
    submitSatisfaction,
    submitCreateAfterSale,
    openAfterSale,
    submitAfterSaleApproval
  };
}
