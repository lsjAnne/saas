<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import type {
  AutomationAction,
  AutomationActionType,
  AutomationPage,
  AutomationRule,
  CreateAutomationActionPayload
} from '@/services/automationTypes';
import {
  AUTOMATION_ACTION_TYPE_LABELS,
  AUTOMATION_ACTION_TYPE_OPTIONS
} from '@/services/automationTypes';

interface Props {
  rules: AutomationRule[];
  actionsPage: AutomationPage<AutomationAction> | null;
  storeId: string;
  isSubmitting: boolean;
}

interface Emits {
  create: [payload: CreateAutomationActionPayload];
  update: [actionId: string, payload: {
    actionType: AutomationActionType;
    actionConfig: string;
    priority: number;
    enabled: boolean;
  }];
  toggle: [action: AutomationAction];
  remove: [actionId: string];
  page: [page: number];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

interface ActionFormState {
  ruleId: string;
  actionType: AutomationActionType;
  actionConfig: string;
  priority: number;
  enabled: boolean;
}

const editingActionId = ref<string | null>(null);
const form = reactive<ActionFormState>({
  ruleId: '',
  actionType: 'AUTO_SHIP',
  actionConfig: '',
  priority: 10,
  enabled: true
});

watch(
  () => props.rules,
  (rules) => {
    if (!form.ruleId && rules.length > 0) {
      form.ruleId = rules[0].ruleId;
    }
  },
  { immediate: true }
);

function startCreate() {
  editingActionId.value = null;
  form.ruleId = props.rules[0]?.ruleId ?? '';
  form.actionType = 'AUTO_SHIP';
  form.actionConfig = '';
  form.priority = 10;
  form.enabled = true;
}

function startEdit(action: AutomationAction) {
  editingActionId.value = action.actionId;
  form.ruleId = action.ruleId;
  form.actionType = action.actionType;
  form.actionConfig = action.actionConfig;
  form.priority = action.priority;
  form.enabled = action.enabled;
}

function submitForm() {
  if (!form.ruleId) {
    return;
  }

  const payload = {
    actionType: form.actionType,
    actionConfig: form.actionConfig,
    priority: Number(form.priority),
    enabled: form.enabled
  };

  if (editingActionId.value) {
    emit('update', editingActionId.value, payload);
  } else {
    emit('create', {
      storeId: props.storeId,
      ruleId: form.ruleId,
      ...payload
    });
  }

  startCreate();
}

function ruleNameOf(action: AutomationAction) {
  return action.ruleName || props.rules.find((rule) => rule.ruleId === action.ruleId)?.ruleName || action.ruleId;
}

const total = computed(() => props.actionsPage?.total ?? 0);
const currentPage = computed(() => props.actionsPage?.page ?? 1);
const maxPage = computed(() => Math.max(1, Math.ceil(total.value / (props.actionsPage?.pageSize ?? 10))));
</script>

<template>
  <div class="auto-action-config">
    <PanelCard
      eyebrow="Rules"
      title="规则列表"
      description="已启用的规则定义，动作绑定挂载在规则之上；规则表达式用于匹配订单或财务单据。"
    >
      <div v-if="rules.length" class="auto-action-config__table-wrap">
        <table class="auto-action-config__table">
          <thead>
            <tr>
              <th>规则名称</th>
              <th>类型</th>
              <th>规则表达式</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in rules" :key="rule.ruleId">
              <td class="auto-action-config__cell--strong">{{ rule.ruleName }}</td>
              <td><code class="auto-action-config__code">{{ rule.ruleType }}</code></td>
              <td class="auto-action-config__cell--mono">{{ rule.ruleExpression }}</td>
              <td>
                <span :class="['auto-action-config__status', rule.enabled ? 'auto-action-config__status--on' : 'auto-action-config__status--off']">
                  {{ rule.enabled ? '已启用' : '已停用' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="auto-action-config__empty">当前没有可用规则，请先在规则治理中创建并启用规则。</p>
    </PanelCard>

    <PanelCard
      eyebrow="Actions"
      title="动作绑定配置"
      description="为规则绑定自动动作，支持新增、编辑、启停与删除；按 priority 依次执行。"
    >
      <form class="auto-action-config__form" @submit.prevent="submitForm">
        <div class="auto-action-config__form-head">
          <p class="auto-action-config__block-title">
            {{ editingActionId ? `编辑动作 ${editingActionId}` : '新增动作绑定' }}
          </p>
          <button type="button" class="auto-action-config__ghost" @click="startCreate">
            清空表单
          </button>
        </div>

        <div class="auto-action-config__grid auto-action-config__grid--three">
          <label class="auto-action-config__field">
            <span>挂载规则</span>
            <select v-model="form.ruleId" :disabled="Boolean(editingActionId)">
              <option v-for="rule in rules" :key="rule.ruleId" :value="rule.ruleId">
                {{ rule.ruleName }}
              </option>
            </select>
          </label>

          <label class="auto-action-config__field">
            <span>动作类型</span>
            <select v-model="form.actionType">
              <option
                v-for="option in AUTOMATION_ACTION_TYPE_OPTIONS"
                :key="option"
                :value="option"
              >
                {{ AUTOMATION_ACTION_TYPE_LABELS[option] }}（{{ option }}）
              </option>
            </select>
          </label>

          <label class="auto-action-config__field">
            <span>优先级</span>
            <input v-model.number="form.priority" type="number" min="1" />
          </label>
        </div>

        <label class="auto-action-config__field">
          <span>动作配置（JSON 文本）</span>
          <textarea
            v-model="form.actionConfig"
            rows="3"
            placeholder='{"threshold":1000,"targetStatus":"shipped"}'
          />
        </label>

        <label class="auto-action-config__field auto-action-config__field--inline">
          <input v-model="form.enabled" type="checkbox" />
          <span>启用该动作</span>
        </label>

        <button
          class="auto-action-config__submit auto-action-config__submit--primary"
          type="submit"
          :disabled="isSubmitting || !form.ruleId"
        >
          {{ editingActionId ? '保存修改' : '新增绑定' }}
        </button>
      </form>

      <div v-if="actionsPage && actionsPage.list.length" class="auto-action-config__table-wrap">
        <table class="auto-action-config__table">
          <thead>
            <tr>
              <th>规则</th>
              <th>动作类型</th>
              <th>配置</th>
              <th>优先级</th>
              <th>启用</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="action in actionsPage.list" :key="action.actionId">
              <td class="auto-action-config__cell--strong">{{ ruleNameOf(action) }}</td>
              <td>
                <code class="auto-action-config__code">{{ action.actionType }}</code>
              </td>
              <td class="auto-action-config__cell--mono auto-action-config__cell--truncate">
                {{ action.actionConfig || '--' }}
              </td>
              <td>{{ action.priority }}</td>
              <td>
                <button
                  type="button"
                  :class="['auto-action-config__status', action.enabled ? 'auto-action-config__status--on' : 'auto-action-config__status--off']"
                  @click="emit('toggle', action)"
                >
                  {{ action.enabled ? '启用' : '停用' }}
                </button>
              </td>
              <td class="auto-action-config__cell-actions">
                <button type="button" class="auto-action-config__ghost" @click="startEdit(action)">
                  编辑
                </button>
                <button
                  type="button"
                  class="auto-action-config__ghost auto-action-config__ghost--danger"
                  @click="emit('remove', action.actionId)"
                >
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="auto-action-config__empty">当前还没有动作绑定，先在上方新增一条。</p>

      <div v-if="total > 0" class="auto-action-config__pager">
        <button
          type="button"
          class="auto-action-config__ghost"
          :disabled="currentPage <= 1"
          @click="emit('page', currentPage - 1)"
        >
          上一页
        </button>
        <span class="auto-action-config__pager-meta">第 {{ currentPage }} / {{ maxPage }} 页 · 共 {{ total }} 条</span>
        <button
          type="button"
          class="auto-action-config__ghost"
          :disabled="currentPage >= maxPage"
          @click="emit('page', currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </PanelCard>
  </div>
</template>

<style scoped>
.auto-action-config {
  display: grid;
  gap: 1rem;
}

.auto-action-config__form {
  display: grid;
  gap: 0.85rem;
  padding: 1rem;
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
  margin-bottom: 1.1rem;
}

.auto-action-config__form-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.auto-action-config__block-title {
  margin: 0;
  color: var(--color-ink-strong);
  font-weight: 700;
}

.auto-action-config__grid {
  display: grid;
  gap: 0.75rem;
}

.auto-action-config__grid--three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.auto-action-config__field {
  display: grid;
  gap: 0.4rem;
}

.auto-action-config__field--inline {
  grid-template-columns: auto 1fr;
  align-items: center;
}

.auto-action-config__field span {
  color: var(--color-ink-soft);
  font-size: 0.88rem;
}

.auto-action-config__field input,
.auto-action-config__field select,
.auto-action-config__field textarea {
  width: 100%;
  min-height: 2.9rem;
  padding: 0.75rem 0.9rem;
  border-radius: 1rem;
  border: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.88);
  color: var(--color-ink-strong);
  font: inherit;
  resize: vertical;
  box-sizing: border-box;
}

.auto-action-config__field textarea {
  min-height: 5.4rem;
  font-family: var(--font-body);
}

.auto-action-config__table-wrap {
  overflow-x: auto;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
}

.auto-action-config__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.auto-action-config__table th,
.auto-action-config__table td {
  padding: 0.7rem 0.85rem;
  text-align: left;
  border-bottom: 1px solid var(--color-line);
  color: var(--color-ink);
  vertical-align: middle;
}

.auto-action-config__table th {
  background: var(--color-surface-2);
  color: var(--color-ink-strong);
  font-weight: 600;
  white-space: nowrap;
}

.auto-action-config__table tbody tr:last-child td {
  border-bottom: 0;
}

.auto-action-config__cell--strong {
  color: var(--color-ink-strong);
  font-weight: 600;
}

.auto-action-config__cell--mono {
  font-family: ui-monospace, "Cascadia Code", Consolas, monospace;
  font-size: 0.82rem;
}

.auto-action-config__cell--truncate {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auto-action-config__cell-actions {
  display: flex;
  gap: 0.45rem;
  flex-wrap: wrap;
}

.auto-action-config__code {
  padding: 0.12rem 0.5rem;
  border-radius: var(--radius-pill);
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 0.76rem;
  font-weight: 600;
}

.auto-action-config__status {
  display: inline-flex;
  align-items: center;
  padding: 0.28rem 0.7rem;
  border-radius: var(--radius-pill);
  font-size: 0.78rem;
  font-weight: 600;
  border: 0;
}

.auto-action-config__status--on {
  background: rgba(47, 125, 92, 0.13);
  color: var(--color-success);
}

.auto-action-config__status--off {
  background: var(--color-surface-2);
  color: var(--color-ink-soft);
}

.auto-action-config__submit,
.auto-action-config__ghost {
  min-height: 2.6rem;
  padding: 0 1.05rem;
  border-radius: var(--radius-pill);
  font: inherit;
  font-weight: 600;
}

.auto-action-config__submit {
  width: fit-content;
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink-strong);
}

.auto-action-config__submit--primary {
  border: 0;
  background: var(--color-accent);
  color: #ffffff;
}

.auto-action-config__ghost {
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink-strong);
}

.auto-action-config__ghost--danger {
  color: var(--color-danger);
}

.auto-action-config__pager {
  margin-top: 0.9rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.auto-action-config__pager-meta {
  color: var(--color-ink-soft);
  font-size: 0.88rem;
}

.auto-action-config__empty {
  margin: 0;
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px dashed var(--color-line);
  background: var(--color-surface);
  color: var(--color-ink-soft);
  line-height: 1.7;
}

@media (max-width: 900px) {
  .auto-action-config__grid--three {
    grid-template-columns: 1fr;
  }
}
</style>
