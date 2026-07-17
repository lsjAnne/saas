<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ExceptionTask } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  selectedException: ExceptionTask | null;
  suggestedSop: string[];
  actionChoice: string;
  actionRemark: string;
  actionFeedback: string | null;
  isRunningAction: boolean;
  canProcessSelected: boolean;
  canIgnoreSelected: boolean;
  canEscalateSelected: boolean;
}

interface Emits {
  updateActionChoice: [value: string];
  updateActionRemark: [value: string];
  process: [];
  ignore: [];
  escalate: [];
}

defineProps<Props>();
const emit = defineEmits<Emits>();

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toneFromSeverity(severity: string) {
  const normalized = normalizeText(severity);

  if (normalized === 'critical' || normalized === 'high') {
    return 'warn';
  }

  return 'neutral';
}

function toneFromStatus(status: string) {
  const normalized = normalizeText(status);

  if (normalized === 'resolved' || normalized === 'ignored') {
    return 'success';
  }

  if (normalized === 'escalated') {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Exception Detail"
    title="异常详情与处理动作"
    description="在一个详情面板里看清关联对象、建议文本、推荐 SOP 和本次处理动作。"
  >
    <div v-if="selectedException" class="exception-detail">
      <div class="exception-detail__header">
        <div>
          <p class="exception-detail__title">{{ selectedException.exceptionType }}</p>
          <p class="exception-detail__meta">
            {{ selectedException.relatedType }} · {{ selectedException.relatedId }} · {{ selectedException.storeId }}
          </p>
        </div>
        <div class="exception-detail__pill-stack">
          <StatusPill
            :label="selectedException.severity"
            :tone="toneFromSeverity(selectedException.severity)"
          />
          <StatusPill
            :label="selectedException.status"
            :tone="toneFromStatus(selectedException.status)"
          />
        </div>
      </div>

      <dl class="exception-detail__facts">
        <div class="exception-detail__fact">
          <dt>责任人</dt>
          <dd>{{ selectedException.ownerUserId || '当前未分配' }}</dd>
        </div>
        <div class="exception-detail__fact">
          <dt>创建时间</dt>
          <dd>{{ formatDateTime(selectedException.createdAt) }}</dd>
        </div>
        <div class="exception-detail__fact exception-detail__fact--wide">
          <dt>建议文本</dt>
          <dd>{{ selectedException.suggestionText || '当前没有建议文本。' }}</dd>
        </div>
      </dl>

      <section class="exception-detail__section">
        <p class="exception-detail__section-eyebrow">推荐 SOP</p>
        <ol class="exception-detail__sop-list">
          <li v-for="item in suggestedSop" :key="item" class="exception-detail__sop-item">
            {{ item }}
          </li>
        </ol>
      </section>

      <section class="exception-detail__section">
        <p class="exception-detail__section-eyebrow">处理动作区</p>

        <label class="exception-detail__field">
          <span class="exception-detail__field-label">处理动作类型</span>
          <select
            class="exception-detail__select"
            :value="actionChoice"
            @change="emit('updateActionChoice', ($event.target as HTMLSelectElement).value)"
          >
            <option value="apply_sop">apply_sop</option>
            <option value="manual_followup">manual_followup</option>
            <option value="owner_callback">owner_callback</option>
          </select>
        </label>

        <label class="exception-detail__field">
          <span class="exception-detail__field-label">处理备注</span>
          <textarea
            class="exception-detail__textarea"
            rows="5"
            :value="actionRemark"
            placeholder="补充当前判断、人工处理结果或升级原因"
            @input="emit('updateActionRemark', ($event.target as HTMLTextAreaElement).value)"
          />
        </label>

        <p v-if="actionFeedback" class="exception-detail__feedback">{{ actionFeedback }}</p>

        <div class="exception-detail__actions">
          <button
            type="button"
            class="exception-detail__action exception-detail__action--primary"
            :disabled="!canProcessSelected || isRunningAction"
            @click="emit('process')"
          >
            同意建议
          </button>
          <button
            type="button"
            class="exception-detail__action"
            :disabled="!canIgnoreSelected || isRunningAction"
            @click="emit('ignore')"
          >
            忽略
          </button>
          <button
            type="button"
            class="exception-detail__action exception-detail__action--warn"
            :disabled="!canEscalateSelected || isRunningAction"
            @click="emit('escalate')"
          >
            升级
          </button>
        </div>
      </section>
    </div>

    <p v-else class="exception-detail__empty">
      先从左侧选择一条异常，再查看详情和执行动作。
    </p>
  </PanelCard>
</template>

<style scoped>
.exception-detail,
.exception-detail__section {
  display: grid;
  gap: 0.85rem;
}

.exception-detail__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.exception-detail__pill-stack {
  display: grid;
  gap: 0.45rem;
  justify-items: end;
}

.exception-detail__title,
.exception-detail__meta,
.exception-detail__section-eyebrow,
.exception-detail__feedback,
.exception-detail__empty,
.exception-detail__field-label {
  margin: 0;
}

.exception-detail__title {
  color: var(--color-ink-strong);
  font-size: 1.08rem;
}

.exception-detail__meta,
.exception-detail__feedback,
.exception-detail__empty {
  color: var(--color-ink-soft);
  line-height: 1.65;
}

.exception-detail__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0;
}

.exception-detail__fact {
  display: grid;
  gap: 0.3rem;
  padding: 0.85rem 0.95rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 249, 243, 0.72);
}

.exception-detail__fact--wide {
  grid-column: 1 / -1;
}

.exception-detail__fact dt,
.exception-detail__section-eyebrow,
.exception-detail__field-label {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.exception-detail__fact dd {
  margin: 0;
  color: var(--color-ink-strong);
  line-height: 1.65;
}

.exception-detail__sop-list {
  display: grid;
  gap: 0.65rem;
  padding-left: 1.2rem;
  margin: 0;
  color: var(--color-ink-strong);
  line-height: 1.7;
}

.exception-detail__field {
  display: grid;
  gap: 0.5rem;
}

.exception-detail__select,
.exception-detail__textarea {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.88);
  color: var(--color-ink-strong);
  padding: 0.85rem 1rem;
  font: inherit;
}

.exception-detail__textarea {
  resize: vertical;
  min-height: 8rem;
}

.exception-detail__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.exception-detail__action {
  min-height: 2.9rem;
  padding: 0 1.15rem;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.86);
  color: var(--color-ink-strong);
  font: inherit;
  font-weight: 600;
}

.exception-detail__action--primary {
  border: 0;
  background: linear-gradient(135deg, #3b2a20, #8f6240);
  color: #fff8f0;
}

.exception-detail__action--warn {
  border: 0;
  background: linear-gradient(135deg, #8b4d2e, #5b2214);
  color: #fff8f0;
}

@media (max-width: 820px) {
  .exception-detail__facts {
    grid-template-columns: 1fr;
  }

  .exception-detail__fact--wide {
    grid-column: auto;
  }
}
</style>
