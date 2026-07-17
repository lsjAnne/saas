<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ExceptionTask } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  exceptions: ExceptionTask[];
  selectedExceptionId: string | null;
}

interface Emits {
  select: [exceptionTaskId: string];
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
    eyebrow="Exception Queue"
    title="异常主队列"
    description="队列优先按状态和风险排序，先看仍开放的高风险项。"
  >
    <div class="exception-queue">
      <button
        v-for="item in exceptions"
        :key="item.exceptionTaskId"
        type="button"
        :class="[
          'exception-queue__item',
          { 'exception-queue__item--active': item.exceptionTaskId === selectedExceptionId }
        ]"
        @click="emit('select', item.exceptionTaskId)"
      >
        <div class="exception-queue__item-head">
          <div>
            <p class="exception-queue__title">{{ item.exceptionType }}</p>
            <p class="exception-queue__meta">
              {{ item.relatedType }} · {{ item.relatedId }}
            </p>
          </div>
          <div class="exception-queue__pill-stack">
            <StatusPill :label="item.severity" :tone="toneFromSeverity(item.severity)" />
            <StatusPill :label="item.status" :tone="toneFromStatus(item.status)" />
          </div>
        </div>
        <p class="exception-queue__detail">
          店铺 {{ item.storeId }} · {{ formatDateTime(item.createdAt) }}
        </p>
      </button>

      <p v-if="exceptions.length === 0" class="exception-queue__empty">
        当前筛选范围下没有异常项。
      </p>
    </div>
  </PanelCard>
</template>

<style scoped>
.exception-queue {
  display: grid;
  gap: 0.8rem;
}

.exception-queue__item {
  width: 100%;
  display: grid;
  gap: 0.5rem;
  padding: 0.95rem 1rem;
  text-align: left;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.74);
  transition:
    transform var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.exception-queue__item:hover {
  transform: translateY(-1px);
  border-color: rgba(156, 113, 71, 0.24);
}

.exception-queue__item--active {
  border-color: rgba(156, 113, 71, 0.34);
  box-shadow: 0 14px 30px rgba(112, 84, 55, 0.12);
}

.exception-queue__item-head {
  display: flex;
  justify-content: space-between;
  gap: 0.9rem;
  align-items: flex-start;
}

.exception-queue__pill-stack {
  display: grid;
  gap: 0.45rem;
  justify-items: end;
}

.exception-queue__title,
.exception-queue__meta,
.exception-queue__detail,
.exception-queue__empty {
  margin: 0;
}

.exception-queue__title {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.exception-queue__meta,
.exception-queue__detail,
.exception-queue__empty {
  color: var(--color-ink-soft);
  line-height: 1.58;
}
</style>
