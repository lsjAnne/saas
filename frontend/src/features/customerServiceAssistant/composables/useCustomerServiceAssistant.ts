import { computed, shallowRef } from 'vue';

import {
  generateConversationReplySuggestion,
  getConversationMessages,
  getConversations,
  sendConversationMessage,
  transferConversationManual
} from '@/features/customerServiceAssistant/services/customerServiceAssistantService';
import { getSupportSessions } from '@/services/supportWorkbenchService';
import type {
  ConversationMessage,
  CustomerConversation,
  ReplySuggestionView,
  SupportSession
} from '@/services/apiTypes';

export function useCustomerServiceAssistant() {
  const conversations = shallowRef<CustomerConversation[]>([]);
  const supportSessions = shallowRef<SupportSession[]>([]);
  const messages = shallowRef<ConversationMessage[]>([]);
  const selectedConversationId = shallowRef<string | null>(null);
  const suggestion = shallowRef<ReplySuggestionView | null>(null);
  const isLoading = shallowRef(false);
  const isLoadingMessages = shallowRef(false);
  const isGeneratingSuggestion = shallowRef(false);
  const isSendingMessage = shallowRef(false);
  const isTransferringManual = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const gateMessage = shallowRef<string | null>(null);

  const selectedConversation = computed(
    () =>
      conversations.value.find(
        (conversation) => conversation.conversationId === selectedConversationId.value
      ) ?? null
  );

  const activeSupportSession = computed(
    () =>
      supportSessions.value.find(
        (session) => session.active && session.status === 'approved'
      ) ?? null
  );

  function requiresSupportGate(operatorType: string) {
    return operatorType.startsWith('platform-');
  }

  function hasControlledDetailAccess(operatorType: string) {
    return !requiresSupportGate(operatorType) || Boolean(activeSupportSession.value);
  }

  async function load(token: string, operatorType: string) {
    isLoading.value = true;
    errorMessage.value = null;
    gateMessage.value = null;

    try {
      conversations.value = await getConversations(token);

      try {
        supportSessions.value = await getSupportSessions(token);
      } catch {
        supportSessions.value = [];
      }

      const firstConversation = conversations.value[0];

      if (firstConversation && hasControlledDetailAccess(operatorType)) {
        await selectConversation(firstConversation.conversationId, token, operatorType);
      } else if (firstConversation && requiresSupportGate(operatorType)) {
        selectedConversationId.value = firstConversation.conversationId;
        gateMessage.value = '当前账号需要有效支持会话后才可进入受控会话详情。';
      }
    } catch (error) {
      errorMessage.value =
        error instanceof Error
          ? error.message
          : '客户服务辅助中心数据加载失败，请稍后重试';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  async function refreshConversations(token: string) {
    conversations.value = await getConversations(token);
  }

  async function selectConversation(
    conversationId: string,
    token: string,
    operatorType: string
  ) {
    selectedConversationId.value = conversationId;
    suggestion.value = null;
    gateMessage.value = null;

    if (!hasControlledDetailAccess(operatorType)) {
      messages.value = [];
      gateMessage.value = '当前账号需要有效支持会话后才可查看会话消息与执行接管动作。';
      return;
    }

    isLoadingMessages.value = true;

    try {
      messages.value = await getConversationMessages(conversationId, token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error
          ? error.message
          : '会话消息加载失败，请稍后重试';
      throw error;
    } finally {
      isLoadingMessages.value = false;
    }
  }

  async function generateSuggestion(token: string, operatorType: string) {
    if (!selectedConversationId.value || !hasControlledDetailAccess(operatorType)) {
      return null;
    }

    isGeneratingSuggestion.value = true;
    errorMessage.value = null;

    try {
      suggestion.value = await generateConversationReplySuggestion(
        selectedConversationId.value,
        token
      );
      await Promise.all([
        selectConversation(selectedConversationId.value, token, operatorType),
        refreshConversations(token)
      ]);
      return suggestion.value;
    } catch (error) {
      errorMessage.value =
        error instanceof Error
          ? error.message
          : 'AI 建议生成失败，请稍后重试';
      throw error;
    } finally {
      isGeneratingSuggestion.value = false;
    }
  }

  async function sendReply(
    token: string,
    operatorType: string,
    contentText: string
  ) {
    if (!selectedConversationId.value || !contentText.trim() || !hasControlledDetailAccess(operatorType)) {
      return;
    }

    isSendingMessage.value = true;
    errorMessage.value = null;

    try {
      await sendConversationMessage(selectedConversationId.value, contentText.trim(), token);
      suggestion.value = null;
      await Promise.all([
        selectConversation(selectedConversationId.value, token, operatorType),
        refreshConversations(token)
      ]);
    } catch (error) {
      errorMessage.value =
        error instanceof Error
          ? error.message
          : '发送回复失败，请稍后重试';
      throw error;
    } finally {
      isSendingMessage.value = false;
    }
  }

  async function transferManual(
    token: string,
    operatorType: string,
    reason: string
  ) {
    if (!selectedConversationId.value || !reason.trim() || !hasControlledDetailAccess(operatorType)) {
      return;
    }

    isTransferringManual.value = true;
    errorMessage.value = null;

    try {
      await transferConversationManual(selectedConversationId.value, reason.trim(), token);
      suggestion.value = null;
      await Promise.all([
        selectConversation(selectedConversationId.value, token, operatorType),
        refreshConversations(token)
      ]);
    } catch (error) {
      errorMessage.value =
        error instanceof Error
          ? error.message
          : '转人工失败，请稍后重试';
      throw error;
    } finally {
      isTransferringManual.value = false;
    }
  }

  return {
    conversations,
    supportSessions,
    messages,
    selectedConversationId,
    selectedConversation,
    suggestion,
    activeSupportSession,
    isLoading,
    isLoadingMessages,
    isGeneratingSuggestion,
    isSendingMessage,
    isTransferringManual,
    errorMessage,
    gateMessage,
    requiresSupportGate,
    hasControlledDetailAccess,
    load,
    selectConversation,
    generateSuggestion,
    sendReply,
    transferManual
  };
}
