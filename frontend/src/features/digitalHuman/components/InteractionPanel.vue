<script setup lang="ts">
import { computed, reactive } from 'vue';
import type { DigitalHumanInteraction, InteractionOverview } from '@/features/digitalHuman/types';

const props = defineProps<{
  interactions: DigitalHumanInteraction[];
  liveChat: DigitalHumanInteraction[];
  overview: InteractionOverview | null;
  organizationId: string;
  isRunningAction: boolean;
}>();

const emit = defineEmits<{
  (e: 'send', payload: Record<string, unknown>): void;
  (e: 'simulate', payload: Record<string, unknown>): void;
}>();

const form = reactive({
  content: '',
});

const chat = computed(() => props.liveChat);

function handleSend() {
  const content = form.content.trim();
  if (!content || props.isRunningAction) {
    return;
  }
  emit('send', {
    organizationId: props.organizationId,
    sessionId: null,
    platformType: 'mock',
    userNick: '演示观众',
    content,
  });
  form.content = '';
}

function handleSimulate() {
  emit('simulate', {
    organizationId: props.organizationId,
    sessionId: null,
    platformType: 'douyin',
    count: 5,
  });
}

const timeText = (value?: string) => {
  if (!value) {
    return '';
  }
  const d = new Date(value);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};
</script>

<template>
  <section class="dh-panel">
    <div class="dh-panel__head">
      <h2 class="dh-panel__title">直播间互动</h2>
      <p class="dh-panel__subline">
        模拟观众发弹幕，数字人按商品与问题类型即时作答；未命中知识库自动兜底。
      </p>
    </div>

    <div class="dh-interaction__stats">
      <div class="dh-interaction__stat">
        <b>{{ props.overview?.faqCount ?? 0 }}</b>
        <span>知识库条目</span>
      </div>
      <div class="dh-interaction__stat">
        <b>{{ props.overview?.barrageCount ?? 0 }}</b>
        <span>收到弹幕</span>
      </div>
      <div class="dh-interaction__stat">
        <b>{{ props.overview?.repliedCount ?? 0 }}</b>
        <span>知识库命中</span>
      </div>
      <div class="dh-interaction__stat">
        <b>{{ props.overview?.hitRate ?? 0 }}%</b>
        <span>命中率</span>
      </div>
    </div>

    <div v-if="props.overview && props.overview.hotQuestions.length" class="dh-interaction__hot">
      <span class="dh-interaction__hot-label">高频问题：</span>
      <span
        v-for="q in props.overview.hotQuestions"
        :key="q.question"
        class="dh-tag"
      >
        {{ q.productName ? `${q.productName}·` : '' }}{{ q.question }}（{{ q.hitCount }}）
      </span>
    </div>

    <div class="dh-interaction__chat">
      <div v-if="chat.length === 0" class="dh-empty">
        还没有弹幕。下方输入一句观众提问，或一键模拟 5 条弹幕，看看数字人如何回答。
      </div>
      <div
        v-for="item in chat"
        :key="item.interactionId"
        class="dh-interaction__row"
        :class="{ 'dh-interaction__row--fallback': item.hitType !== 'faq' }"
      >
        <div class="dh-interaction__bubble dh-interaction__bubble--viewer">
          <span class="dh-interaction__nick">观众</span>
          {{ item.question }}
          <span class="dh-interaction__time">{{ timeText(item.createdAt) }}</span>
        </div>
        <div
          class="dh-interaction__bubble dh-interaction__bubble--bot"
          :class="{ 'dh-interaction__bubble--fallback': item.hitType !== 'faq' }"
        >
          <span class="dh-interaction__nick dh-interaction__nick--bot">数字人小美</span>
          {{ item.answer }}
          <span class="dh-tag dh-interaction__hit">
            {{ item.hitType === 'faq' ? '知识库' : '兜底' }}
          </span>
        </div>
      </div>
    </div>

    <div class="dh-interaction__send">
      <input
        v-model="form.content"
        class="dh-input"
        placeholder="模拟观众提问，如：这个多少钱？包邮吗？"
        @keyup.enter="handleSend"
      />
      <button class="dh-btn dh-btn--primary" :disabled="props.isRunningAction" @click="handleSend">
        发送弹幕
      </button>
      <button
        class="dh-btn dh-btn--ghost"
        :disabled="props.isRunningAction"
        @click="handleSimulate"
      >
        模拟 5 条弹幕
      </button>
    </div>
  </section>
</template>

<style>
.dh-interaction__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}
.dh-interaction__stat {
  background: var(--color-panel-strong);
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.dh-interaction__stat b {
  font-size: 1.1rem;
  color: var(--color-accent);
}
.dh-interaction__stat span {
  font-size: 0.75rem;
  color: var(--color-ink-soft);
}
.dh-interaction__hot {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  font-size: 0.8125rem;
}
.dh-interaction__hot-label {
  color: var(--color-ink-muted);
}
.dh-interaction__chat {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 420px;
  overflow-y: auto;
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  padding: 14px;
  background: var(--color-panel-strong);
  margin-bottom: 12px;
}
.dh-interaction__row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.dh-interaction__bubble {
  font-size: 0.875rem;
  line-height: 1.5;
  border-radius: var(--radius-md);
  padding: 8px 12px;
  max-width: 92%;
}
.dh-interaction__bubble--viewer {
  align-self: flex-start;
  background: var(--color-surface);
  border: 1px solid var(--color-line);
  color: var(--color-ink-strong);
}
.dh-interaction__bubble--bot {
  align-self: flex-end;
  background: #e8f0fe;
  color: #1e3a8a;
}
.dh-interaction__bubble--fallback {
  background: #f3f4f6;
  color: var(--color-ink-muted);
}
.dh-interaction__nick {
  font-size: 0.75rem;
  color: var(--color-ink-soft);
  margin-right: 6px;
}
.dh-interaction__nick--bot {
  color: var(--color-accent);
  font-weight: 600;
}
.dh-interaction__time {
  font-size: 0.6875rem;
  color: var(--color-ink-soft);
  margin-left: 6px;
}
.dh-interaction__hit {
  margin-left: 6px;
}
.dh-interaction__send {
  display: flex;
  gap: 8px;
}
@media (max-width: 1100px) {
  .dh-interaction__stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
