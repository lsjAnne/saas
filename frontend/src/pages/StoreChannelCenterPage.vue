<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import ChannelAccountPanel from '@/features/storeChannel/components/ChannelAccountPanel.vue';
import StoreChannelHeroPanel from '@/features/storeChannel/components/StoreChannelHeroPanel.vue';
import StoreConnectionComposer from '@/features/storeChannel/components/StoreConnectionComposer.vue';
import StoreDirectoryPanel from '@/features/storeChannel/components/StoreDirectoryPanel.vue';
import { useStoreChannelOverview } from '@/features/storeChannel/composables/useStoreChannelOverview';
import type {
  ConnectStorePayload,
  CreateChannelAccountPayload,
  UpdateStoreSettingsPayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const storeChannel = useStoreChannelOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await storeChannel.load(authStore.token);
}

async function handleStoreConnection(payload: ConnectStorePayload) {
  if (!authStore.token) {
    return;
  }

  await storeChannel.submitStoreConnection(payload, authStore.token);
}

async function handleChannelAccount(payload: CreateChannelAccountPayload) {
  if (!authStore.token) {
    return;
  }

  await storeChannel.submitChannelAccount(payload, authStore.token);
}

async function handleStoreSettings(payload: UpdateStoreSettingsPayload) {
  if (!authStore.token) {
    return;
  }

  await storeChannel.submitSelectedStoreSettings(payload, authStore.token);
}

async function handleRefreshAuth(channelAccountId: string) {
  if (!authStore.token) {
    return;
  }

  await storeChannel.refreshChannelAuth(channelAccountId, authStore.token);
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="store-channel-page">
    <header class="store-channel-page__header">
      <p class="store-channel-page__eyebrow">渠道接入</p>
      <h2 class="store-channel-page__title">店铺与渠道</h2>
      <p class="store-channel-page__subtitle">
        管理店铺接入、渠道账号授权与店铺策略配置。
      </p>
    </header>

    <InlineErrorCard
      v-if="storeChannel.errorMessage"
      :message="storeChannel.errorMessage"
      @retry="bootstrapPage"
    />

    <StoreChannelHeroPanel
      :selected-store="storeChannel.selectedStore"
      :summary="storeChannel.summary"
      :platform-breakdown="storeChannel.platformBreakdown"
      :action-feedback="storeChannel.actionFeedback"
      @open-store="storeChannel.openStoreConnector"
      @open-channel="storeChannel.openChannelConnector"
    />

    <div class="store-channel-page__grid">
      <StoreDirectoryPanel
        :stores="storeChannel.stores"
        :selected-store-id="storeChannel.selectedStoreId"
        :selected-store="storeChannel.selectedStore"
        @select="storeChannel.selectStore"
        @edit="storeChannel.openStoreSettingsEditor"
      />

      <div class="store-channel-page__stack">
        <StoreConnectionComposer
          :mode="storeChannel.composerMode"
          :selected-store="storeChannel.selectedStore"
          :default-organization-id="authStore.organizationId"
          :default-operator-id="authStore.user?.id ?? ''"
          :is-submitting="storeChannel.isRunningAction"
          :error-message="storeChannel.actionError"
          @close="storeChannel.closeComposer"
          @open-store="storeChannel.openStoreConnector"
          @open-channel="storeChannel.openChannelConnector"
          @open-settings="storeChannel.openStoreSettingsEditor"
          @connect-store="handleStoreConnection"
          @create-channel-account="handleChannelAccount"
          @update-store-settings="handleStoreSettings"
        />

        <ChannelAccountPanel
          :channel-accounts="storeChannel.channelAccounts"
          :selected-store="storeChannel.selectedStore"
          :refreshing-channel-account-id="storeChannel.refreshingChannelAccountId"
          @open-create="storeChannel.openChannelConnector"
          @refresh="handleRefreshAuth"
        />
      </div>
    </div>

    <p v-if="storeChannel.isLoading" class="store-channel-page__footer-note">
      正在回读店铺、渠道账号与授权时效数据...
    </p>
  </section>
</template>

<style scoped>
.store-channel-page {
  display: grid;
  gap: 1.2rem;
}

.store-channel-page__header {
  display: grid;
  gap: 0.75rem;
}

.store-channel-page__eyebrow,
.store-channel-page__title,
.store-channel-page__subtitle,
.store-channel-page__footer-note {
  margin: 0;
}

.store-channel-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.store-channel-page__title {
  color: var(--color-ink-strong);
  font-size: 1.55rem;
  font-weight: 600;
}

.store-channel-page__subtitle,
.store-channel-page__footer-note {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.store-channel-page__grid {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  gap: 1rem;
  align-items: start;
}

.store-channel-page__stack {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1180px) {
  .store-channel-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
