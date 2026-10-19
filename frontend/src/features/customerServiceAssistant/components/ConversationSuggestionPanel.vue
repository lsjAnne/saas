<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { CustomerConversation, ReplySuggestionView, SupportSession } from '@/services/apiTypes';

interface Props {
  conversation: CustomerConversation | null;
  suggestion: ReplySuggestionView | null;
  activeSupportSession: SupportSession | null;
  isGeneratingSuggestion: boolean;
  canViewControlledDetail: boolean;
}

defineProps<Props>();

defineEmits<{
  generate: [];
  applySuggestion: [suggestedReply: string];
}>();

const sourceLabelMap: Record<string, string> = {
  pending: '待生成',
  faq: 'FAQ 命中',
  policy: '政策规则',
  general: '通用回复'
};

function labelOf(value: string) {
  return sourceLabelMap[value] ?? value;
}
</script>

<template>
  <PanelCard
    title="回复建议与风险说明"
    description="展示 AI 回复建议、FAQ 命中、置信度和人工接管线索，避免把建议误当成已执行结果。"
  >
    <div class="conversation-suggestion">
      <div class="conversation-suggestion__head">
        <StatusPill
          :label="labelOf(suggestion?.sourceType || 'pending')"
          :tone="suggestion ? 'success' : 'neutral'"
        />
        <button
          class="conversation-suggestion__button"
          type="button"
          :disabled="!conversation || !canViewControlledDetail || isGeneratingSuggestion"
          @click="$emit('generate')"
        >
          {{ isGeneratingSuggestion ? '生成中...' : '生成 AI 建议' }}
        </button>
      </div>

      <div v-if="suggestion" class="conversation-suggestion__body">
        <p class="conversation-suggestion__copy">{{ suggestion.suggestedReply }}</p>
        <p class="conversation-suggestion__meta">
          {{
            suggestion.matchedQuestion
              ? `命中 FAQ：${suggestion.matchedQuestion}`
              : '未命中 FAQ，当前建议来自意图识别与通用回复链路。'
          }}
        </p>
        <p class="conversation-suggestion__meta">
          {{ `置信度：${suggestion.confidenceLevel}；建议动作：${suggestion.recommendedAction}` }}
        </p>
        <p class="conversation-suggestion__meta">
          {{ `知识来源：${suggestion.knowledgeSourceSummary}` }}
        </p>
        <p class="conversation-suggestion__meta">
          {{
            suggestion.autoSendEligible
              ? '当前建议满足自动发送条件，但仍建议客服确认后再发送。'
              : '当前建议仅供参考，需要人工确认或接管。'
          }}
        </p>
        <p v-if="suggestion.handoffReason" class="conversation-suggestion__meta">
          {{ `人工接管原因：${suggestion.handoffReason}` }}
        </p>
        <div v-if="suggestion.riskLabels.length" class="conversation-suggestion__risk-list">
          <StatusPill
            v-for="label in suggestion.riskLabels"
            :key="label"
            :label="label"
            tone="warn"
          />
        </div>
        <p v-if="activeSupportSession" class="conversation-suggestion__meta">
          当前支持会话已生效，可继续查看受控详情与接管动作。
        </p>
        <button
          class="conversation-suggestion__button conversation-suggestion__button--secondary"
          type="button"
          @click="$emit('applySuggestion', suggestion.suggestedReply)"
        >
          采用建议并发送
        </button>
      </div>

      <p v-else class="conversation-suggestion__placeholder">
        先选择会话，再生成 AI 建议回复。
      </p>
    </div>
  </PanelCard>
</template>

<style scoped>
.conversation-suggestion {
  display: grid;
  gap: 1rem;
}

.conversation-suggestion__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.conversation-suggestion__body {
  display: grid;
  gap: 0.85rem;
}

.conversation-suggestion__risk-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.conversation-suggestion__copy,
.conversation-suggestion__meta,
.conversation-suggestion__placeholder {
  margin: 0;
}

.conversation-suggestion__copy {
  color: var(--color-ink-strong);
  line-height: 1.8;
  font-size: 1rem;
}

.conversation-suggestion__meta,
.conversation-suggestion__placeholder {
  color: var(--color-ink-soft);
  line-height: 1.65;
}

.conversation-suggestion__button {
  min-height: 2.85rem;
  padding: 0 1.05rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.conversation-suggestion__button:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.conversation-suggestion__button--secondary {
  background: var(--color-accent);
  color: #ffffff;
  border: 1px solid var(--color-accent);
}

.conversation-suggestion__button--secondary:hover {
  background: var(--color-accent-hover);
  color: #ffffff;
  border-color: var(--color-accent-hover);
}
</style>
