<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { BusinessAssistantResponse } from '@/features/businessAssistant/composables/useBusinessAssistantCopilot';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  response: BusinessAssistantResponse | null;
  isLoading: boolean;
  selectedStoreName: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  openRoute: [route: string];
  pickFollowUp: [question: string];
}>();

const intentLabel = computed(() => {
  if (!props.response) {
    return 'waiting';
  }

  return props.response.intentLabel;
});

const intentTone = computed(() => {
  if (!props.response) {
    return 'neutral';
  }

  if (props.response.intent === 'risk' || props.response.intent === 'fulfillment') {
    return 'warn';
  }

  return 'success';
});

const confidenceTone = computed(() => {
  if (!props.response) {
    return 'neutral';
  }

  if (props.response.confidence === 'high') {
    return 'success';
  }

  if (props.response.confidence === 'medium') {
    return 'neutral';
  }

  return 'warn';
});
</script>

<template>
  <PanelCard
    eyebrow="Assistant Response"
    title="建议输出与下一步动作"
    :description="
      response
        ? `以下回答基于 ${response.storeName} 的真实业务数据生成，不是预设文案。`
        : `先对 ${selectedStoreName} 发起一个问题，助手会在这里汇总证据、洞察、追问和动作。`
    "
  >
    <div class="assistant-response">
      <template v-if="response">
        <div class="assistant-response__meta">
          <StatusPill :label="intentLabel" :tone="intentTone" />
          <StatusPill :label="response.confidenceLabel" :tone="confidenceTone" />
          <p class="assistant-response__timestamp">{{ formatDateTime(response.createdAt) }}</p>
        </div>

        <div class="assistant-response__question">
          <p class="assistant-response__question-label">你的问题</p>
          <p class="assistant-response__question-copy">{{ response.question }}</p>
        </div>

        <div class="assistant-response__summary-card">
          <p class="assistant-response__title">{{ response.title }}</p>
          <p class="assistant-response__summary">{{ response.summary }}</p>
          <p class="assistant-response__confidence">{{ response.confidenceReason }}</p>
        </div>

        <div class="assistant-response__labels">
          <StatusPill
            v-for="label in response.coverageLabels"
            :key="label"
            :label="label"
            tone="neutral"
          />
        </div>

        <div v-if="response.matchedSignals.length" class="assistant-response__signals">
          <p class="assistant-response__column-title">命中信号</p>
          <div class="assistant-response__signal-list">
            <StatusPill
              v-for="signal in response.matchedSignals"
              :key="signal"
              :label="signal"
              tone="neutral"
            />
          </div>
        </div>

        <div class="assistant-response__grid">
          <article class="assistant-response__column">
            <p class="assistant-response__column-title">证据</p>
            <ul class="assistant-response__list">
              <li v-for="item in response.evidence" :key="item" class="assistant-response__list-item">
                {{ item }}
              </li>
            </ul>
          </article>

          <article class="assistant-response__column">
            <p class="assistant-response__column-title">洞察</p>
            <ul class="assistant-response__list">
              <li v-for="item in response.insights" :key="item" class="assistant-response__list-item">
                {{ item }}
              </li>
            </ul>
          </article>
        </div>

        <div v-if="response.followUpQuestions.length" class="assistant-response__follow-up">
          <p class="assistant-response__column-title">建议追问</p>
          <div class="assistant-response__follow-up-list">
            <button
              v-for="question in response.followUpQuestions"
              :key="question"
              type="button"
              class="assistant-response__follow-up-chip"
              @click="emit('pickFollowUp', question)"
            >
              {{ question }}
            </button>
          </div>
        </div>

        <div class="assistant-response__labels">
          <StatusPill
            v-for="label in response.sourceLabels"
            :key="label"
            :label="label"
            tone="neutral"
          />
        </div>

        <div class="assistant-response__actions">
          <button
            v-for="action in response.actions"
            :key="action.label"
            type="button"
            class="assistant-response__action"
            @click="emit('openRoute', action.route)"
          >
            <span>{{ action.label }}</span>
            <strong>{{ action.description }}</strong>
          </button>
        </div>
      </template>

      <div v-else class="assistant-response__empty">
        <p class="assistant-response__empty-title">
          {{ isLoading ? '正在同步经营上下文...' : '先问一个经营问题' }}
        </p>
        <p class="assistant-response__empty-copy">
          {{
            isLoading
              ? '会先回读推荐、店铺、会员、订单和库存等数据，完成后就可以直接提问。'
              : '推荐先问“今天卖什么最稳妥”或“哪些订单今天必须人工处理”，这页会生成证据、洞察、追问和可执行动作。'
          }}
        </p>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.assistant-response {
  display: grid;
  gap: 1rem;
}

.assistant-response__meta,
.assistant-response__labels,
.assistant-response__signal-list,
.assistant-response__follow-up-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  align-items: center;
}

.assistant-response__timestamp,
.assistant-response__question-label,
.assistant-response__question-copy,
.assistant-response__title,
.assistant-response__summary,
.assistant-response__confidence,
.assistant-response__column-title,
.assistant-response__empty-title,
.assistant-response__empty-copy {
  margin: 0;
}

.assistant-response__timestamp,
.assistant-response__question-label,
.assistant-response__column-title {
  color: var(--color-ink-faint);
  font-size: 0.82rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.assistant-response__question,
.assistant-response__summary-card,
.assistant-response__column,
.assistant-response__signals,
.assistant-response__follow-up,
.assistant-response__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: rgba(132, 108, 85, 0.06);
}

.assistant-response__question-copy,
.assistant-response__summary,
.assistant-response__confidence,
.assistant-response__list-item,
.assistant-response__empty-copy {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.assistant-response__confidence {
  margin-top: 0.7rem;
  color: var(--color-ink-faint);
}

.assistant-response__title,
.assistant-response__empty-title {
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: 1.45rem;
}

.assistant-response__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.assistant-response__list {
  margin: 0.85rem 0 0;
  padding-left: 1.1rem;
  display: grid;
  gap: 0.7rem;
}

.assistant-response__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.assistant-response__follow-up-chip {
  min-height: 2.75rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-line);
  background: transparent;
  color: var(--color-ink);
  font: inherit;
  text-align: left;
}

.assistant-response__action {
  min-height: 5rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(249, 242, 235, 0.96));
  display: grid;
  gap: 0.35rem;
  text-align: left;
}

.assistant-response__action span {
  color: var(--color-ink-faint);
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.assistant-response__action strong {
  color: var(--color-ink-strong);
  line-height: 1.6;
}

@media (max-width: 980px) {
  .assistant-response__grid,
  .assistant-response__actions {
    grid-template-columns: 1fr;
  }
}
</style>
