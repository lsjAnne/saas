import { apiClient } from '@/services/http/apiClient';
import type {
  ConversationMessage,
  CustomerConversation,
  ReplySuggestionView
} from '@/services/apiTypes';

export function getConversations(token: string) {
  return apiClient.get<CustomerConversation[]>('/api/conversations', token);
}

export function getConversationMessages(conversationId: string, token: string) {
  return apiClient.get<ConversationMessage[]>(`/api/conversations/${conversationId}/messages`, token);
}

export function generateConversationReplySuggestion(conversationId: string, token: string) {
  return apiClient.post<ReplySuggestionView>(
    `/api/conversations/${conversationId}/reply-suggestion`,
    undefined,
    token
  );
}

export function sendConversationMessage(
  conversationId: string,
  contentText: string,
  token: string
) {
  return apiClient.post<ConversationMessage>(
    `/api/conversations/${conversationId}/send`,
    {
      contentText
    },
    token
  );
}

export function transferConversationManual(
  conversationId: string,
  reason: string,
  token: string
) {
  return apiClient.post<CustomerConversation>(
    `/api/conversations/${conversationId}/transfer-manual`,
    {
      reason
    },
    token
  );
}
