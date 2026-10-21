<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import type {
  AutomationExecution,
  AutomationExecutionQuery,
  AutomationPage
} from '@/services/automationTypes';
import {
  AUTOMATION_ACTION_TYPE_LABELS,
  AUTOMATION_ACTION_TYPE_OPTIONS
} from '@/services/automationTypes';
import type { AutomationExecutionFilter } from '@/features/automationCenter/composables/useAutomationCenter';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  page: AutomationPage<AutomationExecution> | null;
  filter: AutomationExecutionFilter;
}

interface Emits {
  applyFilter: [patch: AutomationExecutionFilter];
  page: [page: number];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const status = ref<AutomationExecutionFilter['status']>(props.filter.status);
const actionType = ref<AutomationExecutionFilter['actionType']>(props.filter.actionType);
const triggerSource = ref<AutomationExecutionFilter['triggerSource']>(props.filter.triggerSource);

watch(
  () => props.filter,
  (next) => {
    status.value = next.status;
    actionType.value = next.actionType;
    triggerSource.value = next.triggerSource;
  }
);

function applyFilter() {
  emit('applyFilter', {
    status: status.value,
    actionType: actionType.value,
    triggerSource: triggerSource.value
  });
}

const total = computed(() => props.page?.total ?? 0);
const currentPage = computed(() => props.page?.page ?? 1);
const maxPage = computed(() => Math.max(1, Math.ceil(total.value / (props.page?.pageSize ?? 10))));

function statusToneClass(execution: AutomationExecution) {
  switch (execution.status) {
    case 'SUCCESS':
      return 'auto-exec__status--success';
    case 'FAILED':
      return 'auto-exec__status--failed';
    case 'PENDING_REVIEW':
      return 'auto-exec__status--review';
    default:
      return 'auto-exec__status--skipped';
  }
}

const STATUS_OPTIONS: AutomationExecutionQuery['status'][] = [
  '',
  'SUCCESS',
  'FAILED',
  'PENDING_REVIEW',
  'SKIPPED'
];

const TRIGGER_OPTIONS: AutomationExecutionFilter['triggerSource'][] = [
  '',
  'SCHEDULE',
  'MANUAL',
  'EVENT'
];
</script>

<template>
  <PanelCard
    eyebrow="Executions"
    title="执行记录"
    description="全链路可审计的执行流水，按状态、动作类型与触发来源筛选，失败与挂起记录可下钻。"
  >
    <div class="auto-exec__filters">
      <label class="auto-exec__field">
        <span>状态</span>
        <select v-model="status">
          <option value="">全部</option>
          <option v-for="option in STATUS_OPTIONS.slice(1)" :key="option" :value="option">
            {{ option }}
          </option>
        </select>
      </label>

      <label class="auto-exec__field">
        <span>动作类型</span>
        <select v-model="actionType">
          <option value="">全部</option>
          <option v-for="option in AUTOMATION_ACTION_TYPE_OPTIONS" :key="option" :value="option">
            {{ AUTOMATION_ACTION_TYPE_LABELS[option] }}（{{ option }}）
          </option>
        </select>
      </label>

      <label class="auto-exec__field">
        <span>触发来源</span>
        <select v-model="triggerSource">
          <option value="">全部</option>
          <option v-for="option in TRIGGER_OPTIONS.slice(1)" :key="option" :value="option">
            {{ option }}
          </option>
        </select>
      </label>

      <button type="button" class="auto-exec__submit auto-exec__submit--primary" @click="applyFilter">
        应用筛选
      </button>
    </div>

    <div v-if="page && page.list.length" class="auto-exec__table-wrap">
      <table class="auto-exec__table">
        <thead>
          <tr>
            <th>执行 ID</th>
            <th>规则</th>
            <th>动作</th>
            <th>触发</th>
            <th>目标</th>
            <th>状态</th>
            <th>重试</th>
            <th>耗时</th>
            <th>执行时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in page.list" :key="item.executionId">
            <td class="auto-exec__cell--mono auto-exec__cell--strong">{{ item.executionId }}</td>
            <td>{{ item.ruleName || item.ruleId }}</td>
            <td><code class="auto-exec__code">{{ item.actionType }}</code></td>
            <td>{{ item.triggerSource }}</td>
            <td class="auto-exec__cell--mono">
              {{ item.targetType }}:{{ item.targetId }}
              <div v-if="item.errorMessage" class="auto-exec__error">{{ item.errorMessage }}</div>
            </td>
            <td>
              <span :class="['auto-exec__status', statusToneClass(item)]">{{ item.status }}</span>
            </td>
            <td>{{ item.retryCount }}</td>
            <td>{{ item.costMs }} ms</td>
            <td class="auto-exec__cell--time">{{ formatDateTime(item.executedAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-else class="auto-exec__empty">当前筛选条件下没有执行记录。</p>

    <div v-if="total > 0" class="auto-exec__pager">
      <button
        type="button"
        class="auto-exec__ghost"
        :disabled="currentPage <= 1"
        @click="emit('page', currentPage - 1)"
      >
        上一页
      </button>
      <span class="auto-exec__pager-meta">
        第 {{ currentPage }} / {{ maxPage }} 页 · 共 {{ total }} 条
      </span>
      <button
        type="button"
        class="auto-exec__ghost"
        :disabled="currentPage >= maxPage"
        @click="emit('page', currentPage + 1)"
      >
        下一页
      </button>
    </div>
  </PanelCard>
</template>

<style scoped>
.auto-exec__filters {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
  gap: 0.75rem;
  align-items: end;
  margin-bottom: 1rem;
}

.auto-exec__field {
  display: grid;
  gap: 0.4rem;
}

.auto-exec__field span {
  color: var(--color-ink-soft);
  font-size: 0.88rem;
}

.auto-exec__field select {
  width: 100%;
  min-height: 2.9rem;
  padding: 0.75rem 0.9rem;
  border-radius: 1rem;
  border: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.88);
  color: var(--color-ink-strong);
  font: inherit;
  box-sizing: border-box;
}

.auto-exec__table-wrap {
  overflow-x: auto;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
}

.auto-exec__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.auto-exec__table th,
.auto-exec__table td {
  padding: 0.65rem 0.8rem;
  text-align: left;
  border-bottom: 1px solid var(--color-line);
  color: var(--color-ink);
  vertical-align: top;
}

.auto-exec__table th {
  background: var(--color-surface-2);
  color: var(--color-ink-strong);
  font-weight: 600;
  white-space: nowrap;
}

.auto-exec__table tbody tr:last-child td {
  border-bottom: 0;
}

.auto-exec__cell--mono {
  font-family: ui-monospace, "Cascadia Code", Consolas, monospace;
  font-size: 0.8rem;
}

.auto-exec__cell--strong {
  color: var(--color-ink-strong);
  font-weight: 600;
}

.auto-exec__cell--time {
  white-space: nowrap;
  color: var(--color-ink-soft);
}

.auto-exec__error {
  margin-top: 0.3rem;
  color: var(--color-danger);
  font-size: 0.78rem;
  line-height: 1.5;
}

.auto-exec__code {
  padding: 0.12rem 0.5rem;
  border-radius: var(--radius-pill);
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 0.76rem;
  font-weight: 600;
}

.auto-exec__status {
  display: inline-flex;
  align-items: center;
  padding: 0.28rem 0.7rem;
  border-radius: var(--radius-pill);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.auto-exec__status--success {
  background: rgba(47, 125, 92, 0.14);
  color: var(--color-success);
}

.auto-exec__status--failed {
  background: rgba(210, 84, 74, 0.14);
  color: var(--color-danger);
}

.auto-exec__status--review {
  background: rgba(192, 122, 44, 0.16);
  color: var(--color-warning);
}

.auto-exec__status--skipped {
  background: var(--color-surface-2);
  color: var(--color-ink-soft);
}

.auto-exec__submit,
.auto-exec__ghost {
  min-height: 2.9rem;
  padding: 0 1.1rem;
  border-radius: var(--radius-pill);
  font: inherit;
  font-weight: 600;
}

.auto-exec__submit--primary {
  border: 0;
  background: var(--color-accent);
  color: #ffffff;
}

.auto-exec__ghost {
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink-strong);
}

.auto-exec__pager {
  margin-top: 0.9rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.auto-exec__pager-meta {
  color: var(--color-ink-soft);
  font-size: 0.88rem;
}

.auto-exec__empty {
  margin: 0;
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px dashed var(--color-line);
  background: var(--color-surface);
  color: var(--color-ink-soft);
  line-height: 1.7;
}

@media (max-width: 900px) {
  .auto-exec__filters {
    grid-template-columns: 1fr;
  }
}
</style>
