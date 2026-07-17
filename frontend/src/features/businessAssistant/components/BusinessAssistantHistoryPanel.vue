<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { BusinessAssistantResponse } from '@/features/businessAssistant/composables/useBusinessAssistantCopilot';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  history: BusinessAssistantResponse[];
}

defineProps<Props>();

const emit = defineEmits<{
  replayQuestion: [question: string];
}>();
</script>

<template>
  <PanelCard
    eyebrow="Conversation History"
    title="历史记录"
    description="保留最近的问题与结构化回答，方便重新分析或沿着上一轮动作继续追问。"
  >
    <div class="assistant-history">
      <div v-if="history.length" class="assistant-history__list">
        <article
          v-for="item in history"
          :key="item.id"
          class="assistant-history__card"
        >
          <div class="assistant-history__card-head">
            <p class="assistant-history__question">{{ item.question }}</p>
            <p class="assistant-history__timestamp">{{ formatDateTime(item.createdAt) }}</p>
          </div>
          <div class="assistant-history__labels">
            <StatusPill :label="item.intentLabel" tone="neutral" />
            <StatusPill
              :label="item.confidenceLabel"
              :tone="item.confidence === 'low' ? 'warn' : item.confidence === 'high' ? 'success' : 'neutral'"
            />
            <StatusPill
              v-for="label in item.coverageLabels.slice(0, 2)"
              :key="`${item.id}-${label}`"
              :label="label"
              tone="neutral"
            />
          </div>
          <p class="assistant-history__title">{{ item.title }}</p>
          <p class="assistant-history__summary">{{ item.summary }}</p>
          <button
            type="button"
            class="assistant-history__button"
            @click="emit('replayQuestion', item.question)"
          >
            再次分析
          </button>
        </article>
      </div>

      <div v-else class="assistant-history__empty">
        <p class="assistant-history__empty-title">还没有历史记录</p>
        <p class="assistant-history__empty-copy">
          首次提问后，这里会保留最近 10 次建议，方便你快速回到之前的问题和追问链路。
        </p>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.assistant-history,
.assistant-history__list {
  display: grid;
  gap: 0.9rem;
}

.assistant-history__card,
.assistant-history__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: rgba(132, 108, 85, 0.06);
}

.assistant-history__card-head {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: baseline;
}

.assistant-history__labels {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  margin-top: 0.7rem;
}

.assistant-history__question,
.assistant-history__timestamp,
.assistant-history__title,
.assistant-history__summary,
.assistant-history__empty-title,
.assistant-history__empty-copy {
  margin: 0;
}

.assistant-history__question,
.assistant-history__title,
.assistant-history__empty-title {
  color: var(--color-ink-strong);
}

.assistant-history__question {
  font-weight: 600;
  line-height: 1.6;
}

.assistant-history__timestamp {
  color: var(--color-ink-faint);
  font-size: 0.8rem;
  white-space: nowrap;
}

.assistant-history__title {
  margin-top: 0.6rem;
  font-family: var(--font-display);
  font-size: 1.1rem;
}

.assistant-history__summary,
.assistant-history__empty-copy {
  margin-top: 0.45rem;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.assistant-history__button {
  min-height: 2.6rem;
  margin-top: 0.8rem;
  padding: 0 0.95rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-line);
  background: transparent;
  color: var(--color-ink);
  font: inherit;
}

@media (max-width: 720px) {
  .assistant-history__card-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
