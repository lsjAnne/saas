<script setup lang="ts">
import type { BarrageConnectionStatus } from '@/features/digitalHuman/types';

defineProps<{
  connections: BarrageConnectionStatus[];
  isRunningAction: boolean;
}>();

const emit = defineEmits<{
  start: [platformType: string];
  stop: [platformType: string];
}>();

const PLATFORM_META: Record<string, { label: string; hint: string }> = {
  douyin: { label: '抖音直播', hint: '主播授权 + 互动弹幕 WebSocket' },
  kuaishou: { label: '快手直播', hint: '主播授权 + 评论推送订阅' },
  wechat_channels: { label: '视频号直播', hint: '商家主体授权 + 场次评论事件' },
};
</script>

<template>
  <section class="dh-panel barrage-source-panel">
    <header class="dh-panel__head">
      <h3 class="dh-panel__title">弹幕源接入</h3>
      <p class="dh-panel__subline">
        接入真实直播平台弹幕（抖音 / 快手 / 视频号），弹幕到达后由数字人按商品与问题类型实时作答。
      </p>
    </header>

    <div class="barrage-source__hint">
      <span class="barrage-source__hint-dot" />
      当前默认 <b>mock 模式</b>：启动后模拟观众弹幕持续进入问答闭环，无需真实平台凭证即可体验；填入
      app-key / app-secret / room-id 并关闭 mock 即切换真实平台拉流。
    </div>

    <ul class="barrage-source__list">
      <li
        v-for="connection in connections"
        :key="connection.platformType"
        class="barrage-source__item"
      >
        <div class="barrage-source__info">
          <div class="barrage-source__row">
            <span class="barrage-source__name">
              {{ PLATFORM_META[connection.platformType]?.label ?? connection.platformType }}
            </span>
            <span
              class="dh-status"
              :class="connection.running ? 'dh-status--live' : 'dh-status--off'"
            >
              {{ connection.running ? '拉流中' : '未连接' }}
            </span>
            <span class="dh-tag" :class="connection.mode === 'mock' ? 'dh-tag--mock' : ''">
              {{ connection.mode === 'mock' ? '模拟模式' : '真实模式' }}
            </span>
          </div>
          <p class="barrage-source__hint-text">
            {{ PLATFORM_META[connection.platformType]?.hint }}
            <template v-if="connection.errorMessage"> · {{ connection.errorMessage }}</template>
          </p>
        </div>
        <div class="barrage-source__actions">
          <button
            v-if="!connection.running"
            class="dh-btn dh-btn--primary"
            :disabled="isRunningAction"
            @click="emit('start', connection.platformType)"
          >
            启动
          </button>
          <button
            v-else
            class="dh-btn dh-btn--ghost"
            :disabled="isRunningAction"
            @click="emit('stop', connection.platformType)"
          >
            停止
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.barrage-source-panel {
  border-left: 3px solid var(--color-accent);
}
.barrage-source__hint {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 0.8125rem;
  line-height: 1.55;
  color: var(--color-ink-muted);
  background: var(--color-panel-strong);
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  margin-bottom: 14px;
}
.barrage-source__hint-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-accent);
  flex: none;
  margin-top: 5px;
}
.barrage-source__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.barrage-source__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  background: var(--color-panel-strong);
  padding: 12px 14px;
}
.barrage-source__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.barrage-source__row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.barrage-source__name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-ink-strong);
}
.barrage-source__hint-text {
  margin: 0;
  font-size: 0.75rem;
  color: var(--color-ink-soft);
}
.barrage-source__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
