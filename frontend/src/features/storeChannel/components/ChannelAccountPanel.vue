<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ChannelAccount, Store } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  channelAccounts: ChannelAccount[];
  selectedStore: Store | null;
  refreshingChannelAccountId: string | null;
}

defineProps<Props>();

defineEmits<{
  openCreate: [];
  refresh: [channelAccountId: string];
}>();
</script>

<template>
  <PanelCard
    eyebrow="Authorization"
    title="渠道账号授权矩阵"
    description="渠道账号按当前聚焦店铺的组织维度过滤，避免把不相关账号混进操作视野。授权刷新后统一回读真实到期时间和状态。"
  >
    <div class="channel-panel">
      <div class="channel-panel__header">
        <div class="channel-panel__header-copy">
          <p class="channel-panel__scope-label">当前组织范围</p>
          <strong class="channel-panel__scope-value">
            {{ selectedStore?.organizationId ?? '全部组织' }}
          </strong>
        </div>
        <button type="button" class="channel-panel__header-action" @click="$emit('openCreate')">
          新增账号
        </button>
      </div>

      <article
        v-for="account in channelAccounts"
        :key="account.channelAccountId"
        class="channel-panel__item"
      >
        <div class="channel-panel__item-copy">
          <div class="channel-panel__item-top">
            <strong class="channel-panel__item-title">{{ account.accountName }}</strong>
            <StatusPill
              :label="account.authStatus"
              :tone="account.authStatus === 'connected' ? 'success' : 'warn'"
            />
          </div>
          <p class="channel-panel__item-meta">
            {{ account.channelType }} / {{ account.organizationId }}
          </p>
          <p class="channel-panel__item-meta">
            到期时间：{{ formatDateTime(account.expiresAt) }}
          </p>
        </div>

        <button
          type="button"
          class="channel-panel__item-action"
          :disabled="refreshingChannelAccountId === account.channelAccountId"
          @click="$emit('refresh', account.channelAccountId)"
        >
          {{
            refreshingChannelAccountId === account.channelAccountId
              ? '刷新中...'
              : '刷新授权'
          }}
        </button>
      </article>

      <p v-if="!channelAccounts.length" class="channel-panel__empty">
        当前范围下还没有渠道账号。可以直接新增一个账号，把授权回读链路一起打通。
      </p>
    </div>
  </PanelCard>
</template>

<style scoped>
.channel-panel {
  display: grid;
  gap: 0.85rem;
}

.channel-panel__header,
.channel-panel__item,
.channel-panel__item-top {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
}

.channel-panel__header,
.channel-panel__item {
  align-items: center;
}

.channel-panel__header-copy,
.channel-panel__item-copy {
  display: grid;
  gap: 0.3rem;
}

.channel-panel__scope-label,
.channel-panel__scope-value,
.channel-panel__item-title,
.channel-panel__item-meta,
.channel-panel__empty {
  margin: 0;
}

.channel-panel__scope-label,
.channel-panel__item-meta,
.channel-panel__empty {
  color: var(--color-ink-soft);
}

.channel-panel__scope-label {
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.channel-panel__scope-value,
.channel-panel__item-title {
  color: var(--color-ink-strong);
}

.channel-panel__header-action,
.channel-panel__item-action {
  min-height: 2.8rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.channel-panel__header-action {
  border: 0;
  background: linear-gradient(135deg, #2e251d, #856243);
  color: #fff8f0;
}

.channel-panel__item {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.78);
}

.channel-panel__item-action {
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.66);
  color: var(--color-ink);
}

.channel-panel__item-action:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.channel-panel__empty {
  line-height: 1.7;
}

@media (max-width: 720px) {
  .channel-panel__header,
  .channel-panel__item,
  .channel-panel__item-top {
    flex-direction: column;
    align-items: start;
  }
}
</style>
