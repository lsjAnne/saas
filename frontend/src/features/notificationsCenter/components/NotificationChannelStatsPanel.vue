<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  NotificationChannelGatewayStatView,
  NotificationGatewayChannelBindingView,
  NotificationGatewayProviderConfigView,
  UpdateNotificationGatewayChannelBindingPayload
} from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface Props {
  requireConfiguredGateway: boolean;
  channelBindings: NotificationGatewayChannelBindingView[];
  channelStats: NotificationChannelGatewayStatView[];
  providers: NotificationGatewayProviderConfigView[];
  isSubmitting: boolean;
}

interface Emits {
  saveBindings: [
    payload: {
      requireConfiguredGateway: boolean;
      channelBindings: UpdateNotificationGatewayChannelBindingPayload[];
    }
  ];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const draft = reactive<{
  requireConfiguredGateway: boolean;
  bindings: Record<string, string>;
}>({
  requireConfiguredGateway: props.requireConfiguredGateway,
  bindings: {}
});

watch(
  () => [props.requireConfiguredGateway, props.channelBindings],
  () => {
    draft.requireConfiguredGateway = props.requireConfiguredGateway;
    draft.bindings = props.channelBindings.reduce<Record<string, string>>((result, channel) => {
      result[channel.notifyType] = channel.gatewayCode;
      return result;
    }, {});
  },
  { immediate: true, deep: true }
);

const channelRows = computed(() =>
  props.channelBindings.map((channel) => {
    const stats = props.channelStats.find((item) => item.notifyType === channel.notifyType);
    const availableProviders = props.providers.filter(
      (provider) => provider.notifyType === channel.notifyType
    );
    const deliveredTaskCount = stats?.deliveredTaskCount ?? 0;
    const totalTaskCount = stats?.totalTaskCount ?? 0;
    const successRate = totalTaskCount ? Math.round((deliveredTaskCount / totalTaskCount) * 100) : 0;
    const failedTaskCount = stats?.failedTaskCount ?? 0;
    const deadLetterTaskCount = stats?.deadLetterTaskCount ?? 0;

    return {
      ...channel,
      totalTaskCount,
      sentTaskCount: stats?.sentTaskCount ?? 0,
      scheduledTaskCount: stats?.scheduledTaskCount ?? 0,
      failedTaskCount,
      deadLetterTaskCount,
      successRate,
      availableProviders
    };
  })
);

function toneFromChannel(failedTaskCount: number, deadLetterTaskCount: number, successRate: number) {
  if (failedTaskCount + deadLetterTaskCount > 0) {
    return 'warn';
  }

  if (successRate >= 90) {
    return 'success';
  }

  return 'neutral';
}

function handleSave() {
  emit('saveBindings', {
    requireConfiguredGateway: draft.requireConfiguredGateway,
    channelBindings: channelRows.value.map((channel) => ({
      notifyType: channel.notifyType,
      gatewayCode: draft.bindings[channel.notifyType] ?? channel.gatewayCode
    }))
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Channel Routing"
    title="通知渠道配置"
    description="直接切换每条通知通道当前绑定的 provider。这里保存的是运行时路由，适合原型验证与联调收口。"
  >
    <div class="channel-stats__topbar">
      <label class="channel-stats__toggle">
        <input v-model="draft.requireConfiguredGateway" type="checkbox" />
        <span>要求显式配置网关，缺失绑定时不再自动放行 Mock 路由</span>
      </label>

      <button
        class="channel-stats__save"
        type="button"
        :disabled="isSubmitting"
        @click="handleSave"
      >
        {{ isSubmitting ? '保存中...' : '保存通道路由' }}
      </button>
    </div>

    <div v-if="channelRows.length" class="channel-stats">
      <article
        v-for="channel in channelRows"
        :key="channel.notifyType"
        class="channel-stats__row"
      >
        <div class="channel-stats__head">
          <div>
            <p class="channel-stats__title">{{ channel.notifyType }}</p>
            <p class="channel-stats__summary">
              当前路由 {{ draft.bindings[channel.notifyType] || channel.gatewayCode }} · 成功率
              {{ channel.successRate }}%
            </p>
          </div>
          <StatusPill
            :label="channel.failedTaskCount + channel.deadLetterTaskCount > 0 ? '需处理' : '稳定'"
            :tone="
              toneFromChannel(
                channel.failedTaskCount,
                channel.deadLetterTaskCount,
                channel.successRate
              )
            "
          />
        </div>

        <label class="channel-stats__field">
          <span class="channel-stats__field-label">绑定 provider</span>
          <select v-model="draft.bindings[channel.notifyType]" class="channel-stats__select">
            <option
              v-for="provider in channel.availableProviders"
              :key="provider.gatewayCode"
              :value="provider.gatewayCode"
            >
              {{ provider.gatewayCode }}
            </option>
          </select>
        </label>

        <div class="channel-stats__facts">
          <span>投递总量 {{ formatCount(channel.totalTaskCount, ' 条') }}</span>
          <span>已发送 {{ formatCount(channel.sentTaskCount, ' 条') }}</span>
          <span>待发送 {{ formatCount(channel.scheduledTaskCount, ' 条') }}</span>
          <span>失败 / 死信 {{ formatCount(channel.failedTaskCount + channel.deadLetterTaskCount, ' 条') }}</span>
        </div>

        <p class="channel-stats__note">
          {{ channel.mockMode ? '当前为 Mock 路由' : '当前为真实路由' }} ·
          {{ channel.receiptSupported ? '支持回执闭环' : '不支持回执闭环' }} ·
          {{ channel.endpoint || '--' }}
        </p>
      </article>
    </div>

    <p v-else class="channel-stats__empty">当前没有可配置的通知通道。</p>
  </PanelCard>
</template>

<style scoped>
.channel-stats__topbar,
.channel-stats,
.channel-stats__row {
  display: grid;
}

.channel-stats__topbar {
  gap: 0.9rem;
  margin-bottom: 1rem;
}

.channel-stats__toggle {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.channel-stats__toggle input {
  margin-top: 0.2rem;
}

.channel-stats__save {
  justify-self: start;
  min-width: 10rem;
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.72rem 1.15rem;
  background: #8f5f3a;
  color: #fff8f0;
  font: inherit;
  cursor: pointer;
}

.channel-stats__save:disabled {
  cursor: wait;
  opacity: 0.7;
}

.channel-stats {
  gap: 0.85rem;
}

.channel-stats__row {
  gap: 0.75rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.channel-stats__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.channel-stats__title,
.channel-stats__summary,
.channel-stats__note,
.channel-stats__empty {
  margin: 0;
}

.channel-stats__title {
  color: var(--color-ink-strong);
  font-size: 1.04rem;
}

.channel-stats__summary,
.channel-stats__facts,
.channel-stats__note,
.channel-stats__empty,
.channel-stats__field-label {
  color: var(--color-ink-soft);
}

.channel-stats__summary {
  margin-top: 0.35rem;
}

.channel-stats__field {
  display: grid;
  gap: 0.4rem;
}

.channel-stats__field-label {
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.channel-stats__select {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: calc(var(--radius-lg) - 0.25rem);
  padding: 0.7rem 0.8rem;
  background: rgba(255, 251, 247, 0.92);
  color: var(--color-ink-strong);
  font: inherit;
}

.channel-stats__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.45rem 0.8rem;
  font-size: 0.92rem;
}

.channel-stats__note {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 0.8rem;
  word-break: break-all;
}

@media (max-width: 720px) {
  .channel-stats__head,
  .channel-stats__facts {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
