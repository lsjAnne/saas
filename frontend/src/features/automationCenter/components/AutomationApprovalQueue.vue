<script setup lang="ts">
import { computed, ref } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import type { AutomationApprovalTask, AutomationPage } from '@/services/automationTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  page: AutomationPage<AutomationApprovalTask> | null;
  isSubmitting: boolean;
}

interface Emits {
  approve: [taskId: string, comment: string | undefined];
  reject: [taskId: string, comment: string | undefined];
  page: [page: number];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const selectedTaskId = ref<string | null>(null);
const comment = ref('');
const rejectWarning = ref('');

const selectedTask = computed<AutomationApprovalTask | null>(
  () =>
    props.page?.list.find((task) => task.taskId === selectedTaskId.value) ??
    props.page?.list[0] ??
    null
);

function selectTask(taskId: string) {
  selectedTaskId.value = taskId;
  comment.value = '';
  rejectWarning.value = '';
}

function handleApprove() {
  if (!selectedTask.value) {
    return;
  }
  emit('approve', selectedTask.value.taskId, comment.value.trim() || undefined);
  comment.value = '';
}

function handleReject() {
  if (!selectedTask.value) {
    return;
  }
  if (!comment.value.trim()) {
    rejectWarning.value = '拒绝人工确认时必须填写备注说明原因。';
    return;
  }
  emit('reject', selectedTask.value.taskId, comment.value.trim());
  comment.value = '';
  rejectWarning.value = '';
}

const total = computed(() => props.page?.total ?? 0);
const currentPage = computed(() => props.page?.page ?? 1);
const maxPage = computed(() => Math.max(1, Math.ceil(total.value / (props.page?.pageSize ?? 10))));
</script>

<template>
  <PanelCard
    eyebrow="Approvals"
    title="人工确认队列"
    description="命中 PAUSE_REVIEW 的动作在此挂起等待 L2 人工确认；通过后继续后续动作，拒绝则终止该链路。"
  >
    <div class="auto-approval">
      <div class="auto-approval__list">
        <button
          v-for="task in page?.list ?? []"
          :key="task.taskId"
          type="button"
          :class="['auto-approval__item', { 'auto-approval__item--active': task.taskId === selectedTask?.taskId }]"
          @click="selectTask(task.taskId)"
        >
          <div class="auto-approval__item-head">
            <span class="auto-approval__item-title">{{ task.targetType }}:{{ task.targetId }}</span>
            <span class="auto-approval__status">{{ task.status }}</span>
          </div>
          <p class="auto-approval__item-reason">{{ task.reason || '无挂起原因说明' }}</p>
          <p class="auto-approval__item-meta">
            任务 {{ task.taskId }} · 执行 {{ task.executionId }} · 创建 {{ formatDateTime(task.createdAt) }}
          </p>
        </button>

        <p v-if="!page || page.list.length === 0" class="auto-approval__empty">
          当前没有待人工确认的任务，所有挂起项都已处理完毕。
        </p>
      </div>

      <div class="auto-approval__detail">
        <template v-if="selectedTask">
          <p class="auto-approval__detail-title">确认详情</p>
          <dl class="auto-approval__detail-grid">
            <dt>目标类型</dt>
            <dd>{{ selectedTask.targetType }}</dd>
            <dt>目标 ID</dt>
            <dd class="auto-approval__mono">{{ selectedTask.targetId }}</dd>
            <dt>挂起原因</dt>
            <dd>{{ selectedTask.reason || '--' }}</dd>
            <dt>关联执行</dt>
            <dd class="auto-approval__mono">{{ selectedTask.executionId }}</dd>
          </dl>

          <label class="auto-approval__field">
            <span>处理备注（拒绝时必填）</span>
            <textarea v-model="comment" rows="3" placeholder="例如：金额异常，需复核收货地址后放行" />
          </label>

          <p v-if="rejectWarning" class="auto-approval__warning">{{ rejectWarning }}</p>

          <div class="auto-approval__actions">
            <button
              type="button"
              class="auto-approval__submit auto-approval__submit--primary"
              :disabled="isSubmitting"
              @click="handleApprove"
            >
              通过
            </button>
            <button
              type="button"
              class="auto-approval__ghost auto-approval__ghost--danger"
              :disabled="isSubmitting"
              @click="handleReject"
            >
              拒绝
            </button>
          </div>
        </template>
        <p v-else class="auto-approval__empty">选择左侧一条挂起任务后，在这里查看详情并通过或拒绝。</p>
      </div>
    </div>

    <div v-if="total > 0" class="auto-approval__pager">
      <button
        type="button"
        class="auto-approval__ghost"
        :disabled="currentPage <= 1"
        @click="emit('page', currentPage - 1)"
      >
        上一页
      </button>
      <span class="auto-approval__pager-meta">
        第 {{ currentPage }} / {{ maxPage }} 页 · 共 {{ total }} 条
      </span>
      <button
        type="button"
        class="auto-approval__ghost"
        :disabled="currentPage >= maxPage"
        @click="emit('page', currentPage + 1)"
      >
        下一页
      </button>
    </div>
  </PanelCard>
</template>

<style scoped>
.auto-approval {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 1rem;
  align-items: start;
}

.auto-approval__list {
  display: grid;
  gap: 0.7rem;
}

.auto-approval__item {
  display: grid;
  gap: 0.45rem;
  width: 100%;
  padding: 0.9rem 1rem;
  text-align: left;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.auto-approval__item--active {
  border-color: var(--color-accent);
}

.auto-approval__item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.8rem;
}

.auto-approval__item-title {
  margin: 0;
  color: var(--color-ink-strong);
  font-weight: 700;
}

.auto-approval__item-reason,
.auto-approval__item-meta,
.auto-approval__empty {
  margin: 0;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.auto-approval__item-meta {
  font-size: 0.82rem;
}

.auto-approval__status {
  padding: 0.22rem 0.65rem;
  border-radius: var(--radius-pill);
  background: rgba(192, 122, 44, 0.16);
  color: var(--color-warning);
  font-size: 0.74rem;
  font-weight: 700;
}

.auto-approval__detail {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.auto-approval__detail-title {
  margin: 0;
  color: var(--color-ink-strong);
  font-weight: 700;
}

.auto-approval__detail-grid {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.45rem 1rem;
  margin: 0;
}

.auto-approval__detail-grid dt {
  color: var(--color-ink-soft);
}

.auto-approval__detail-grid dd {
  margin: 0;
  color: var(--color-ink-strong);
}

.auto-approval__mono {
  font-family: ui-monospace, "Cascadia Code", Consolas, monospace;
  font-size: 0.82rem;
}

.auto-approval__field {
  display: grid;
  gap: 0.4rem;
}

.auto-approval__field span {
  color: var(--color-ink-soft);
  font-size: 0.88rem;
}

.auto-approval__field textarea {
  width: 100%;
  padding: 0.75rem 0.9rem;
  border-radius: 1rem;
  border: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.88);
  color: var(--color-ink-strong);
  font: inherit;
  resize: vertical;
  box-sizing: border-box;
}

.auto-approval__warning {
  margin: 0;
  color: var(--color-danger);
  font-size: 0.85rem;
}

.auto-approval__actions {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.auto-approval__submit,
.auto-approval__ghost {
  min-height: 2.7rem;
  padding: 0 1.15rem;
  border-radius: var(--radius-pill);
  font: inherit;
  font-weight: 600;
}

.auto-approval__submit--primary {
  border: 0;
  background: var(--color-accent);
  color: #ffffff;
}

.auto-approval__ghost {
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink-strong);
}

.auto-approval__ghost--danger {
  color: var(--color-danger);
}

.auto-approval__pager {
  margin-top: 0.9rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.auto-approval__pager-meta {
  color: var(--color-ink-soft);
  font-size: 0.88rem;
}

.auto-approval__empty {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px dashed var(--color-line);
  background: var(--color-surface);
  line-height: 1.7;
}

@media (max-width: 980px) {
  .auto-approval {
    grid-template-columns: 1fr;
  }
}
</style>
