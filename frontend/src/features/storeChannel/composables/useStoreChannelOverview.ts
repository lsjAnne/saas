import { computed, shallowRef } from 'vue';

import {
  connectStore,
  createChannelAccount,
  getChannelAccounts,
  getStores,
  refreshChannelAccountAuth,
  updateStoreSettings
} from '@/services/storeChannelService';
import type {
  ChannelAccount,
  ConnectStorePayload,
  CreateChannelAccountPayload,
  Store,
  UpdateStoreSettingsPayload
} from '@/services/apiTypes';

export type StoreChannelComposerMode =
  | 'connect-store'
  | 'create-channel-account'
  | 'edit-store-settings';

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function isConnected(status: string | null | undefined) {
  return normalizeText(status).includes('connect');
}

function isExpiringSoon(expiresAt: string | null | undefined) {
  if (!expiresAt) {
    return false;
  }

  const expiresAtMs = new Date(expiresAt).getTime();
  const threshold = Date.now() + 1000 * 60 * 60 * 24 * 7;
  return expiresAtMs <= threshold;
}

export function useStoreChannelOverview() {
  const stores = shallowRef<Store[]>([]);
  const channelAccounts = shallowRef<ChannelAccount[]>([]);
  const selectedStoreId = shallowRef<string | null>(null);
  const composerMode = shallowRef<StoreChannelComposerMode | null>(null);
  const refreshingChannelAccountId = shallowRef<string | null>(null);
  const isLoading = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const sortedStores = computed(() =>
    [...stores.value].sort(
      (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const selectedStore = computed(
    () =>
      sortedStores.value.find((store) => store.storeId === selectedStoreId.value) ?? null
  );

  const visibleChannelAccounts = computed(() => {
    if (!selectedStore.value) {
      return [...channelAccounts.value].sort(
        (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
      );
    }

    return channelAccounts.value
      .filter(
        (account) =>
          account.organizationId === selectedStore.value?.organizationId
      )
      .sort((left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt));
  });

  const platformBreakdown = computed(() => {
    const counts = new Map<string, number>();

    for (const store of stores.value) {
      const key = store.platformType || 'unknown';
      counts.set(key, (counts.get(key) ?? 0) + 1);
    }

    return [...counts.entries()]
      .map(([platformType, count]) => ({
        platformType,
        count
      }))
      .sort((left, right) => right.count - left.count);
  });

  const summary = computed(() => ({
    connectedStoreCount: stores.value.filter((store) => isConnected(store.authStatus)).length,
    activePlatformCount: platformBreakdown.value.length,
    configuredShipSchemeCount: stores.value.filter(
      (store) =>
        !!store.defaultShipConfig && Object.keys(store.defaultShipConfig).length > 0
    ).length,
    expiringAccountCount: channelAccounts.value.filter((account) =>
      isExpiringSoon(account.expiresAt)
    ).length
  }));

  function clearActionState() {
    actionError.value = null;
    actionFeedback.value = null;
  }

  function applySelection(preferredStoreId?: string | null) {
    const nextStoreId =
      preferredStoreId && stores.value.some((store) => store.storeId === preferredStoreId)
        ? preferredStoreId
        : sortedStores.value[0]?.storeId ?? null;

    selectedStoreId.value = nextStoreId;
  }

  async function load(token: string, preferredStoreId?: string | null) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [storesPayload, channelAccountsPayload] = await Promise.all([
        getStores(token),
        getChannelAccounts(token)
      ]);

      stores.value = storesPayload;
      channelAccounts.value = channelAccountsPayload;
      applySelection(preferredStoreId ?? selectedStoreId.value);
    } catch (error) {
      errorMessage.value =
        error instanceof Error
          ? error.message
          : '店铺与渠道中心加载失败，请稍后重试';
    } finally {
      isLoading.value = false;
    }
  }

  function selectStore(storeId: string) {
    selectedStoreId.value = storeId;
  }

  function openStoreConnector() {
    composerMode.value = 'connect-store';
    actionError.value = null;
  }

  function openChannelConnector() {
    composerMode.value = 'create-channel-account';
    actionError.value = null;
  }

  function openStoreSettingsEditor() {
    if (!selectedStore.value) {
      return;
    }

    composerMode.value = 'edit-store-settings';
    actionError.value = null;
  }

  function closeComposer() {
    composerMode.value = null;
    actionError.value = null;
  }

  async function submitStoreConnection(
    payload: ConnectStorePayload,
    token: string
  ) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      const store = await connectStore(payload, token);
      composerMode.value = null;
      actionFeedback.value = `已接入店铺 ${store.shopName}，正在回读最新门店与渠道状态。`;
      await load(token, store.storeId);
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '店铺接入失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitChannelAccount(
    payload: CreateChannelAccountPayload,
    token: string
  ) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      const account = await createChannelAccount(payload, token);
      composerMode.value = null;
      actionFeedback.value = `已新增渠道账号 ${account.accountName}，正在刷新授权矩阵。`;
      await load(token, selectedStoreId.value);
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '渠道账号创建失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitSelectedStoreSettings(
    payload: UpdateStoreSettingsPayload,
    token: string
  ) {
    if (!selectedStore.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      await updateStoreSettings(selectedStore.value.storeId, payload, token);
      composerMode.value = null;
      actionFeedback.value = `已更新 ${selectedStore.value.shopName} 的利润与风险阈值。`;
      await load(token, selectedStore.value.storeId);
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '店铺阈值更新失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function refreshChannelAuth(channelAccountId: string, token: string) {
    refreshingChannelAccountId.value = channelAccountId;
    actionError.value = null;

    try {
      const account = await refreshChannelAccountAuth(channelAccountId, token);
      actionFeedback.value = `已刷新 ${account.accountName} 的授权状态，正在回读真实过期时间。`;
      await load(token, selectedStoreId.value);
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '渠道授权刷新失败，请稍后重试';
    } finally {
      refreshingChannelAccountId.value = null;
    }
  }

  return {
    stores: sortedStores,
    channelAccounts: visibleChannelAccounts,
    selectedStoreId,
    selectedStore,
    composerMode,
    refreshingChannelAccountId,
    isLoading,
    isRunningAction,
    errorMessage,
    actionError,
    actionFeedback,
    summary,
    platformBreakdown,
    clearActionState,
    load,
    selectStore,
    openStoreConnector,
    openChannelConnector,
    openStoreSettingsEditor,
    closeComposer,
    submitStoreConnection,
    submitChannelAccount,
    submitSelectedStoreSettings,
    refreshChannelAuth
  };
}
