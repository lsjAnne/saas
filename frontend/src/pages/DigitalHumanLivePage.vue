<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import AvatarLibraryPanel from '@/features/digitalHuman/components/AvatarLibraryPanel.vue';
import BarrageSourcePanel from '@/features/digitalHuman/components/BarrageSourcePanel.vue';
import DigitalHumanHeroPanel from '@/features/digitalHuman/components/DigitalHumanHeroPanel.vue';
import FaqLibraryPanel from '@/features/digitalHuman/components/FaqLibraryPanel.vue';
import InteractionPanel from '@/features/digitalHuman/components/InteractionPanel.vue';
import LearningMemoryPanel from '@/features/digitalHuman/components/LearningMemoryPanel.vue';
import ScheduleBoardPanel from '@/features/digitalHuman/components/ScheduleBoardPanel.vue';
import ScriptLibraryPanel from '@/features/digitalHuman/components/ScriptLibraryPanel.vue';
import SessionPanel from '@/features/digitalHuman/components/SessionPanel.vue';
import { useDigitalHumanLive } from '@/features/digitalHuman/composables/useDigitalHumanLive';
import type { DigitalHumanAvatar, DigitalHumanFaq, DigitalHumanScript } from '@/features/digitalHuman/types';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const digitalHuman = useDigitalHumanLive();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.load(authStore.token);
}

async function handleCreateAvatar(payload: {
  organizationId: string;
  avatarName: string;
  avatarType: string;
  voiceName: string;
  previewUrl: string;
}) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.createAvatar(payload, authStore.token);
}

async function handleToggleAvatar(avatar: DigitalHumanAvatar) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.toggleAvatar(avatar, authStore.token);
}

async function handleDeleteAvatar(avatarId: string) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.deleteAvatar(avatarId, authStore.token);
}

async function handleCreateScript(payload: {
  organizationId: string;
  scriptName: string;
  sceneType: string;
  contentText: string;
}) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.createScript(
    {
      organizationId: payload.organizationId,
      scriptName: payload.scriptName,
      sceneType: payload.sceneType,
      contentText: payload.contentText,
      version: 'v1',
      status: 'draft',
    },
    authStore.token,
  );
}

async function handlePublishScript(script: DigitalHumanScript) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.publishScript(script, authStore.token);
}

async function handleCreateSchedule(payload: {
  organizationId: string;
  scheduleName: string;
  avatarId: string;
  scriptId: string;
  platformType: string;
  repeatType: string;
  startAt: string;
}) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.createSchedule(
    {
      organizationId: payload.organizationId,
      scheduleName: payload.scheduleName,
      avatarId: payload.avatarId,
      scriptId: payload.scriptId,
      platformType: payload.platformType,
      repeatType: payload.repeatType,
      startAt: new Date(payload.startAt).toISOString(),
    },
    authStore.token,
  );
}

async function handleStartSchedule(scheduleId: string) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.startSchedule(scheduleId, authStore.token);
}

async function handleStopSchedule(scheduleId: string) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.stopSchedule(scheduleId, authStore.token);
}

async function handleHeartbeat(sessionId: string) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.heartbeatSession(sessionId, authStore.token);
}

async function handleCreateFaq(payload: Record<string, unknown>) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.createFaq(
    {
      organizationId: String(payload.organizationId),
      productId: payload.productId ? String(payload.productId) : undefined,
      productName: payload.productName ? String(payload.productName) : undefined,
      questionType: String(payload.questionType),
      keywords: payload.keywords ? String(payload.keywords) : undefined,
      question: String(payload.question),
      answer: String(payload.answer),
      enabled: true,
    },
    authStore.token,
  );
}

async function handleToggleFaq(faq: DigitalHumanFaq) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.updateFaq(
    faq.faqId,
    {
      organizationId: faq.organizationId ? faq.organizationId : '',
      productId: faq.productId,
      productName: faq.productName,
      questionType: faq.questionType,
      keywords: faq.keywords,
      question: faq.question,
      answer: faq.answer,
      enabled: !faq.enabled,
    },
    authStore.token,
  );
}

async function handleDeleteFaq(faqId: string) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.deleteFaq(faqId, authStore.token);
}

async function handleSendBarrage(payload: Record<string, unknown>) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.sendBarrage(
    {
      organizationId: String(payload.organizationId),
      sessionId: payload.sessionId ? String(payload.sessionId) : undefined,
      platformType: payload.platformType ? String(payload.platformType) : 'mock',
      userNick: payload.userNick ? String(payload.userNick) : '演示观众',
      content: String(payload.content),
    },
    authStore.token,
  );
}

async function handleSimulateBarrages(payload: Record<string, unknown>) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.simulateBarrages(
    {
      organizationId: String(payload.organizationId),
      sessionId: payload.sessionId ? String(payload.sessionId) : undefined,
      platformType: payload.platformType ? String(payload.platformType) : 'douyin',
      count: payload.count ? Number(payload.count) : 5,
    },
    authStore.token,
  );
}

async function handleConfirmLearningItem(itemId: string) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.confirmLearningItem(itemId, authStore.token);
}

async function handleIgnoreLearningItem(itemId: string) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.ignoreLearningItem(itemId, authStore.token);
}

async function handleStartBarrage(platformType: string) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.startBarrage(platformType, authStore.token);
}

async function handleStopBarrage(platformType: string) {
  if (!authStore.token) {
    return;
  }
  await digitalHuman.stopBarrage(platformType, authStore.token);
}

onMounted(() => {
  bootstrapPage();
  // 弹幕源接入后轮询最新问答上屏（每 3 秒一次，轻量单请求）
  liveChatTimer = window.setInterval(() => {
    if (authStore.token && digitalHuman.barrageConnections.some((c) => c.running)) {
      digitalHuman.refreshRecentChat(authStore.token);
    }
  }, 3000);
});

onBeforeUnmount(() => {
  if (liveChatTimer !== null) {
    window.clearInterval(liveChatTimer);
  }
});

let liveChatTimer: number | null = null;
</script>

<template>
  <div class="digital-human-page">
    <header class="digital-human-page__header">
      <h1 class="digital-human-page__title">数字人无人直播</h1>
      <p class="digital-human-page__subtitle">
        数字人形象、话术剧本与 7×24 无人排班一屏编排，自动开播、心跳回读成交数据。
      </p>
    </header>

    <div v-if="digitalHuman.errorMessage" class="digital-human-page__error">
      <InlineErrorCard :message="digitalHuman.errorMessage" />
    </div>

    <div v-if="digitalHuman.actionFeedback" class="digital-human-page__feedback">
      {{ digitalHuman.actionFeedback }}
    </div>

    <DigitalHumanHeroPanel :overview="digitalHuman.overview" />

    <div class="digital-human-page__grid">
      <div class="digital-human-page__column">
        <AvatarLibraryPanel
          :avatars="digitalHuman.avatars"
          :organization-id="authStore.organizationId"
          :is-loading="digitalHuman.isLoading"
          :is-running-action="digitalHuman.isRunningAction"
          @create="handleCreateAvatar"
          @toggle="handleToggleAvatar"
          @remove="handleDeleteAvatar"
        />
        <ScriptLibraryPanel
          :scripts="digitalHuman.scripts"
          :organization-id="authStore.organizationId"
          :is-loading="digitalHuman.isLoading"
          :is-running-action="digitalHuman.isRunningAction"
          @create="handleCreateScript"
          @publish="handlePublishScript"
        />
        <FaqLibraryPanel
          :faqs="digitalHuman.faqs"
          :organization-id="authStore.organizationId"
          :is-loading="digitalHuman.isLoading"
          :is-running-action="digitalHuman.isRunningAction"
          @create="handleCreateFaq"
          @toggle="handleToggleFaq"
          @remove="handleDeleteFaq"
        />
      </div>
      <div class="digital-human-page__column">
        <ScheduleBoardPanel
          :schedules="digitalHuman.schedules"
          :avatars="digitalHuman.avatars"
          :scripts="digitalHuman.scripts"
          :organization-id="authStore.organizationId"
          :is-loading="digitalHuman.isLoading"
          :is-running-action="digitalHuman.isRunningAction"
          @create="handleCreateSchedule"
          @start="handleStartSchedule"
          @stop="handleStopSchedule"
        />
        <SessionPanel
          :sessions="digitalHuman.sessions"
          :is-loading="digitalHuman.isLoading"
          :is-running-action="digitalHuman.isRunningAction"
          @heartbeat="handleHeartbeat"
        />
        <BarrageSourcePanel
          :connections="digitalHuman.barrageConnections"
          :is-running-action="digitalHuman.isRunningAction"
          @start="handleStartBarrage"
          @stop="handleStopBarrage"
        />
        <InteractionPanel
          :interactions="digitalHuman.interactions"
          :live-chat="digitalHuman.liveChat"
          :overview="digitalHuman.interactionOverview"
          :organization-id="authStore.organizationId"
          :is-running-action="digitalHuman.isRunningAction"
          @send="handleSendBarrage"
          @simulate="handleSimulateBarrages"
        />
        <LearningMemoryPanel
          :growth="digitalHuman.learningGrowth"
          :items="digitalHuman.learningItems"
          :is-loading="digitalHuman.isLoading"
          :is-running-action="digitalHuman.isRunningAction"
          @confirm="handleConfirmLearningItem"
          @ignore="handleIgnoreLearningItem"
        />
      </div>
    </div>
  </div>
</template>

<style>
.digital-human-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.digital-human-page__header {
  margin-bottom: 2px;
}
.digital-human-page__title {
  font-size: 1.55rem;
  font-weight: 600;
  color: var(--color-ink-strong);
  margin: 0 0 6px;
}
.digital-human-page__subtitle {
  font-size: 0.875rem;
  color: var(--color-ink-muted);
  margin: 0;
}
.digital-human-page__error {
  margin: 0;
}
.digital-human-page__feedback {
  background: var(--color-accent-soft);
  color: var(--color-accent);
  border: 1px solid var(--color-accent-soft);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  font-size: 0.875rem;
}
.digital-human-page__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  align-items: start;
}
.digital-human-page__column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
@media (max-width: 1100px) {
  .digital-human-page__grid {
    grid-template-columns: 1fr;
  }
}

/* 共享面板样式（dh- 前缀） */
.dh-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-line);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
}
.dh-panel__head {
  margin-bottom: 14px;
}
.dh-panel__title {
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--color-ink-strong);
  margin: 0 0 4px;
}
.dh-panel__subline {
  font-size: 0.8125rem;
  color: var(--color-ink-muted);
  margin: 0;
}
.dh-library__form {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.dh-library__form--stack {
  flex-direction: column;
  align-items: stretch;
}
.dh-library__row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.dh-input,
.dh-select {
  flex: 1 1 160px;
  min-height: 38px;
  border: 1px solid var(--color-line-strong);
  border-radius: var(--radius-md);
  padding: 0 12px;
  font-size: 0.875rem;
  color: var(--color-ink-strong);
  background: var(--color-surface);
  outline: none;
}
.dh-input:focus,
.dh-select:focus {
  border-color: var(--color-accent);
}
.dh-textarea {
  min-height: 76px;
  border: 1px solid var(--color-line-strong);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: 0.875rem;
  color: var(--color-ink-strong);
  background: var(--color-surface);
  outline: none;
  resize: vertical;
}
.dh-textarea:focus {
  border-color: var(--color-accent);
}
.dh-btn {
  min-height: 38px;
  border-radius: var(--radius-md);
  padding: 0 14px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  transition: opacity 0.15s;
}
.dh-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.dh-btn--primary {
  background: var(--color-accent);
  color: #fff;
}
.dh-btn--ghost {
  background: var(--color-surface);
  color: var(--color-accent);
  border-color: var(--color-accent);
}
.dh-btn--danger {
  background: var(--color-surface);
  color: #d93a3a;
  border-color: #f0c4c4;
}
.dh-empty {
  border: 1px dashed var(--color-line-strong);
  border-radius: var(--radius-md);
  padding: 22px;
  text-align: center;
  font-size: 0.875rem;
  color: var(--color-ink-soft);
}
.dh-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.dh-list__item {
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  background: var(--color-panel-strong);
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.dh-list__item--stack {
  flex-direction: column;
  align-items: stretch;
}
.dh-list__main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.dh-list__name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-ink-strong);
}
.dh-list__sub {
  font-size: 0.75rem;
  color: var(--color-ink-soft);
}
.dh-list__desc {
  margin: 8px 0 0;
  font-size: 0.8125rem;
  color: var(--color-ink-muted);
  line-height: 1.5;
}
.dh-list__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.dh-tag {
  font-size: 0.75rem;
  color: var(--color-ink-muted);
  background: var(--color-panel-strong);
  border: 1px solid var(--color-line);
  border-radius: 999px;
  padding: 2px 10px;
}
.dh-tag--mock {
  color: #b45309;
  background: #fef3c7;
  border-color: #fde68a;
}
.dh-status {
  font-size: 0.75rem;
  font-weight: 500;
  border-radius: 999px;
  padding: 2px 10px;
}
.dh-status--on {
  color: #2563eb;
  background: #dbeafe;
}
.dh-status--live {
  color: #16a34a;
  background: #dcfce7;
}
.dh-status--off {
  color: var(--color-ink-soft);
  background: var(--color-panel-strong);
  border: 1px solid var(--color-line);
}
.dh-session__stats {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  font-size: 0.8125rem;
  color: var(--color-ink-muted);
  margin-top: 8px;
}
.dh-session__stats b {
  color: var(--color-ink-strong);
}
</style>
