<script setup lang="ts">
import type { DigitalHumanSession } from '@/features/digitalHuman/types';

defineProps<{
  sessions: DigitalHumanSession[];
  isLoading: boolean;
  isRunningAction: boolean;
}>();

const emit = defineEmits<{
  heartbeat: [sessionId: string];
}>();

const statusLabel: Record<string, string> = {
  scheduled: '待开播',
  live: '直播中',
  ended: '已结束',
  failed: '失败',
};
</script>

<template>
  <section class="dh-panel dh-library">
    <div class="dh-panel__head">
      <div>
        <h3 class="dh-panel__title">开播会话</h3>
        <p class="dh-panel__subline">无人直播的运行态与回读数据，心跳刷新观众、订单与成交。</p>
      </div>
    </div>

    <div v-if="!sessions.length" class="dh-empty">
      {{ isLoading ? '加载中…' : '还没有开播会话。先创建排班并点击开播。' }}
    </div>

    <ul v-else class="dh-list">
      <li v-for="session in sessions" :key="session.sessionId" class="dh-list__item dh-list__item--stack">
        <div class="dh-list__main">
          <span class="dh-list__name">{{ session.platformRoomId || session.sessionId }}</span>
          <span
            class="dh-status"
            :class="session.liveStatus === 'live' ? 'dh-status--live' : session.liveStatus === 'ended' ? 'dh-status--off' : 'dh-status--on'"
          >
            {{ statusLabel[session.liveStatus] ?? session.liveStatus }}
          </span>
          <span v-if="session.mockFlag" class="dh-tag dh-tag--mock">mock</span>
        </div>
        <div class="dh-session__stats">
          <span>峰值观众 <b>{{ session.viewerPeak }}</b></span>
          <span>订单 <b>{{ session.orderCount }}</b></span>
          <span>成交 <b>¥{{ session.gmvAmount }}</b></span>
          <span v-if="session.startedAt" class="dh-list__sub">
            {{ new Date(session.startedAt).toLocaleString('zh-CN', { hour12: false }) }}
          </span>
        </div>
        <div class="dh-list__actions">
          <button
            v-if="session.liveStatus === 'live'"
            class="dh-btn dh-btn--ghost"
            :disabled="isRunningAction"
            @click="emit('heartbeat', session.sessionId)"
          >
            心跳刷新
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>
