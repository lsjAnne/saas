<script setup lang="ts">
import { computed, shallowRef, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { NotificationTask, ReplayNotificationTaskPayload } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  tasks: NotificationTask[];
  runningTaskId: string | null;
}

interface Emits {
  retryTask: [notificationTaskId: string];
  replayTask: [
    payload: {
      notificationTaskId: string;
      replay: ReplayNotificationTaskPayload;
    }
  ];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const replayDrafts = shallowRef<Record<string, ReplayNotificationTaskPayload>>({});

watch(
  () => props.tasks,
  (tasks) => {
    replayDrafts.value = tasks.reduce<Record<string, ReplayNotificationTaskPayload>>(
      (result, task) => {
        result[task.notificationTaskId] = {
          targetReceiver: task.targetReceiver,
          payloadJson: task.payloadJson,
          scheduledAt: task.scheduledAt ? task.scheduledAt.slice(0, 16) : null
        };
        return result;
      },
      {}
    );
  },
  { immediate: true, deep: true }
);

const taskRows = computed(() =>
  props.tasks.map((task) => ({
    task,
    replay: replayDrafts.value[task.notificationTaskId] ?? {
      targetReceiver: task.targetReceiver,
      payloadJson: task.payloadJson,
      scheduledAt: null
    }
  }))
);

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toneFromStatus(status: string) {
  const normalized = normalizeText(status);

  if (normalized === 'dead_letter' || normalized === 'dead-letter') {
    return 'warn';
  }

  if (normalized === 'failed') {
    return 'neutral';
  }

  return 'neutral';
}

function isDeadLetter(status: string) {
  const normalized = normalizeText(status);
  return normalized === 'dead_letter' || normalized === 'dead-letter';
}

function toScheduledAt(value: string | null) {
  if (!value) {
    return null;
  }

  return new Date(value).toISOString();
}

function handleReplay(notificationTaskId: string) {
  const draft = replayDrafts.value[notificationTaskId];
  if (!draft) {
    return;
  }

  emit('replayTask', {
    notificationTaskId,
    replay: {
      targetReceiver: draft.targetReceiver?.trim() || null,
      payloadJson: draft.payloadJson?.trim() || null,
      scheduledAt: toScheduledAt(draft.scheduledAt)
    }
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Failure Recovery"
    title="失败与死信恢复"
    description="失败任务可直接重试，死信任务允许改接收人、改 payload 或重新排期后再回放。"
  >
    <div v-if="taskRows.length" class="failure-recovery">
      <article
        v-for="{ task, replay } in taskRows"
        :key="task.notificationTaskId"
        class="failure-recovery__item"
      >
        <div class="failure-recovery__head">
          <div>
            <p class="failure-recovery__title">{{ task.templateCode }}</p>
            <p class="failure-recovery__meta">
              {{ task.notifyType }} · {{ task.targetReceiver }} · 创建于
              {{ formatDateTime(task.createdAt) }}
            </p>
          </div>
          <StatusPill :label="task.sendStatus" :tone="toneFromStatus(task.sendStatus)" />
        </div>

        <p class="failure-recovery__reason">
          {{ task.deadLetterReason || '当前没有写入死信原因，通常需要检查 provider 回执与目标地址。' }}
        </p>

        <div v-if="isDeadLetter(task.sendStatus)" class="failure-recovery__editor">
          <label class="failure-recovery__field">
            <span class="failure-recovery__field-label">回放接收人</span>
            <input v-model="replay.targetReceiver" class="failure-recovery__input" type="text" />
          </label>

          <label class="failure-recovery__field">
            <span class="failure-recovery__field-label">回放时间</span>
            <input
              v-model="replay.scheduledAt"
              class="failure-recovery__input"
              type="datetime-local"
            />
          </label>

          <label class="failure-recovery__field failure-recovery__field--full">
            <span class="failure-recovery__field-label">回放 Payload</span>
            <textarea
              v-model="replay.payloadJson"
              class="failure-recovery__textarea"
              rows="5"
            />
          </label>
        </div>

        <div class="failure-recovery__actions">
          <button
            v-if="!isDeadLetter(task.sendStatus)"
            class="failure-recovery__button"
            type="button"
            :disabled="runningTaskId === task.notificationTaskId"
            @click="emit('retryTask', task.notificationTaskId)"
          >
            {{ runningTaskId === task.notificationTaskId ? '重试中...' : '重新投递' }}
          </button>

          <button
            v-else
            class="failure-recovery__button"
            type="button"
            :disabled="runningTaskId === task.notificationTaskId"
            @click="handleReplay(task.notificationTaskId)"
          >
            {{ runningTaskId === task.notificationTaskId ? '回放中...' : '回放死信任务' }}
          </button>
        </div>
      </article>
    </div>

    <p v-else class="failure-recovery__empty">当前没有失败或死信任务。</p>
  </PanelCard>
</template>

<style scoped>
.failure-recovery {
  display: grid;
  gap: 0.85rem;
}

.failure-recovery__item {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.failure-recovery__head,
.failure-recovery__actions {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: flex-start;
}

.failure-recovery__title,
.failure-recovery__meta,
.failure-recovery__reason,
.failure-recovery__field-label,
.failure-recovery__empty {
  margin: 0;
}

.failure-recovery__title {
  color: var(--color-ink-strong);
  font-size: 1.02rem;
}

.failure-recovery__meta,
.failure-recovery__reason,
.failure-recovery__field-label,
.failure-recovery__empty {
  color: var(--color-ink-soft);
}

.failure-recovery__reason {
  line-height: 1.65;
}

.failure-recovery__editor {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.failure-recovery__field {
  display: grid;
  gap: 0.4rem;
}

.failure-recovery__field--full {
  grid-column: 1 / -1;
}

.failure-recovery__field-label {
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.failure-recovery__input,
.failure-recovery__textarea {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: calc(var(--radius-lg) - 0.25rem);
  padding: 0.7rem 0.8rem;
  background: rgba(255, 251, 247, 0.92);
  color: var(--color-ink-strong);
  font: inherit;
  box-sizing: border-box;
}

.failure-recovery__textarea {
  resize: vertical;
}

.failure-recovery__button {
  min-width: 9rem;
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.72rem 1.15rem;
  background: #8f5f3a;
  color: #fff8f0;
  font: inherit;
  cursor: pointer;
}

.failure-recovery__button:disabled {
  cursor: wait;
  opacity: 0.7;
}

@media (max-width: 820px) {
  .failure-recovery__editor {
    grid-template-columns: 1fr;
  }

  .failure-recovery__head,
  .failure-recovery__actions {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
