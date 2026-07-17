import { apiClient } from '@/services/http/apiClient';
import type {
  ChannelAccount,
  ConnectStorePayload,
  CreateChannelAccountPayload,
  Store,
  UpdateStoreSettingsPayload
} from '@/services/apiTypes';

export function getStores(token: string) {
  return apiClient.get<Store[]>('/api/stores', token);
}

export function getStore(storeId: string, token: string) {
  return apiClient.get<Store>(`/api/stores/${storeId}`, token);
}

export function connectStore(payload: ConnectStorePayload, token: string) {
  return apiClient.post<Store>('/api/stores/connect', payload, token);
}

export function updateStoreSettings(
  storeId: string,
  payload: UpdateStoreSettingsPayload,
  token: string
) {
  return apiClient.put<Store>(`/api/stores/${storeId}/settings`, payload, token);
}

export function getChannelAccounts(token: string) {
  return apiClient.get<ChannelAccount[]>('/api/channel-accounts', token);
}

export function createChannelAccount(
  payload: CreateChannelAccountPayload,
  token: string
) {
  return apiClient.post<ChannelAccount>('/api/channel-accounts', payload, token);
}

export function refreshChannelAccountAuth(
  channelAccountId: string,
  token: string
) {
  return apiClient.post<ChannelAccount>(
    `/api/channel-accounts/${channelAccountId}/refresh-auth`,
    null,
    token
  );
}
