<script setup lang="ts">
import { onMounted, ref } from 'vue';

import AutomationActionConfigPanel from '@/features/automationCenter/components/AutomationActionConfigPanel.vue';
import AutomationApprovalQueue from '@/features/automationCenter/components/AutomationApprovalQueue.vue';
import AutomationExecutionTable from '@/features/automationCenter/components/AutomationExecutionTable.vue';
import AutomationOverviewPanel from '@/features/automationCenter/components/AutomationOverviewPanel.vue';
import { useAutomationCenter } from '@/features/automationCenter/composables/useAutomationCenter';
import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import type {
  AutomationActionType,
  AutomationExecutionStatus,
  AutomationTriggerSource,
  CreateAutomationActionPayload
} from '@/services/automationTypes';
import { useAuthStore } from '@/stores/authStore';

type ActiveTab = 'rules' | 'executions' | 'approvals';

const authStore = useAuthStore();
const automation = useAutomationCenter();

authStore.hydrate();

const activeTab = ref<ActiveTab>('rules');

const TAB_OPTIONS: Array<{ key: ActiveTab; label: string }> = [
  { key: 'rules', label: '规则动作配置' },
  { key: 'executions', label: '执行记录' },
  { key: 'approvals', label: '人工确认队列' }
];

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }
  await automation.bootstrap(authStore.token);
}

async function handleSelectStore(storeId: string) {
  if (!authStore.token) {
    return;
  }
  await automation.selectStore(storeId, authStore.token);
}

async function handleTrigger() {
  if (!authStore.token) {
    return;
  }
  await automation.triggerOrderSync(authStore.token);
  activeTab.value = 'executions';
}

async function handleCreateAction(payload: CreateAutomationActionPayload) {
  if (!authStore.token) {
    return;
  }
  await automation.createAction(payload, authStore.token);
}

async function handleUpdateAction(
  actionId: string,
  payload: {
    actionType: AutomationActionType;
    actionConfig: string;
    priority: number;
    enabled: boolean;
  }
) {
  if (!authStore.token) {
    return;
  }
  await automation.updateAction(actionId, payload, authStore.token);
}

async function handleToggleAction(action: { actionId: string; enabled: boolean; actionType: AutomationActionType; actionConfig: string; priority: number }) {
  if (!authStore.token) {
    return;
  }
  await automation.updateAction(
    action.actionId,
    {
      actionType: action.actionType,
      actionConfig: action.actionConfig,
      priority: action.priority,
      enabled: !action.enabled
    },
    authStore.token
  );
}

async function handleRemoveAction(actionId: string) {
  if (!authStore.token) {
    return;
  }
  await automation.removeAction(actionId, authStore.token);
}

async function handleApplyFilter(patch: {
  status: AutomationExecutionStatus | '';
  actionType: AutomationActionType | '';
  triggerSource: AutomationTriggerSource | '';
}) {
  automation.executionFilter.status = patch.status;
  automation.executionFilter.actionType = patch.actionType;
  automation.executionFilter.triggerSource = patch.triggerSource;
  if (!authStore.token) {
    return;
  }
  await automation.applyExecutionFilter(authStore.token);
}

async function handleExecutionPage(page: number) {
  if (!authStore.token) {
    return;
  }
  await automation.goExecutionPage(page, authStore.token);
}

async function handleApprovalPage(page: number) {
  if (!authStore.token) {
    return;
  }
  await automation.goApprovalPage(page, authStore.token);
}

async function handleActionPage(page: number) {
  if (!authStore.token) {
    return;
  }
  await automation.goActionPage(page, authStore.token);
}

async function handleApprove(taskId: string, comment: string | undefined) {
  if (!authStore.token) {
    return;
  }
  await automation.approveTask(taskId, comment, authStore.token);
}

async function handleReject(taskId: string, comment: string | undefined) {
  if (!authStore.token) {
    return;
  }
  await automation.rejectTask(taskId, comment, authStore.token);
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="automation-page">
    <header class="automation-page__header">
      <p class="automation-page__eyebrow">运营自动化</p>
      <h2 class="automation-page__title">自动化控制台</h2>
      <p class="automation-page__subtitle">
        串起渠道拉单、规则匹配、自动发货/对账/单据生成与人工确认挂起，全链路写入可审计执行记录。
      </p>
    </header>

    <div class="automation-page__toolbar">
      <label class="automation-page__store-field">
        <span>当前店铺</span>
        <select
          :value="automation.selectedStoreId"
          @change="handleSelectStore(($event.target as HTMLSelectElement).value)"
        >
          <option v-for="store in automation.stores" :key="store.storeId" :value="store.storeId">
            {{ store.shopName }} / {{ store.storeId }}
          </option>
        </select>
      </label>

      <button
        type="button"
        class="automation-page__trigger"
        :disabled="automation.isTriggering || !automation.selectedStoreId"
        @click="handleTrigger"
      >
        {{ automation.isTriggering ? '正在拉单...' : '手动触发拉单' }}
      </button>
    </div>

    <p v-if="automation.actionFeedback" class="automation-page__feedback">
      {{ automation.actionFeedback }}
    </p>

    <InlineErrorCard
      v-if="automation.errorMessage"
      :message="automation.errorMessage"
      @retry="bootstrapPage"
    />
    <InlineErrorCard
      v-if="automation.actionError"
      title="操作未完成"
      :message="automation.actionError"
      retry-label="我知道了"
      @retry="() => undefined"
    />

    <AutomationOverviewPanel :stats="automation.stats" />

    <nav class="automation-page__tabs">
      <button
        v-for="tab in TAB_OPTIONS"
        :key="tab.key"
        type="button"
        :class="['automation-page__tab', { 'automation-page__tab--active': activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </nav>

    <AutomationActionConfigPanel
      v-show="activeTab === 'rules'"
      :rules="automation.rules"
      :actions-page="automation.actionsPage"
      :store-id="automation.selectedStoreId"
      :is-submitting="automation.isMutating"
      @create="handleCreateAction"
      @update="handleUpdateAction"
      @toggle="handleToggleAction"
      @remove="handleRemoveAction"
      @page="handleActionPage"
    />

    <AutomationExecutionTable
      v-show="activeTab === 'executions'"
      :page="automation.executionsPage"
      :filter="automation.executionFilter"
      @apply-filter="handleApplyFilter"
      @page="handleExecutionPage"
    />

    <AutomationApprovalQueue
      v-show="activeTab === 'approvals'"
      :page="automation.approvalsPage"
      :is-submitting="automation.isMutating"
      @approve="handleApprove"
      @reject="handleReject"
      @page="handleApprovalPage"
    />

    <p v-if="automation.isLoading" class="automation-page__footer-note">
      正在回读自动化统计、规则动作、执行记录与人工确认队列...
    </p>
  </section>
</template>

<style scoped>
.automation-page {
  display: grid;
  gap: 1.1rem;
}

.automation-page__header {
  display: grid;
  gap: 0.8rem;
}

.automation-page__eyebrow,
.automation-page__title,
.automation-page__subtitle,
.automation-page__footer-note,
.automation-page__feedback {
  margin: 0;
}

.automation-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.automation-page__title {
  color: var(--color-ink-strong);
  font-size: 1.55rem;
  font-weight: 600;
}

.automation-page__subtitle {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.automation-page__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
  flex-wrap: wrap;
}

.automation-page__store-field {
  display: grid;
  gap: 0.4rem;
  min-width: 260px;
}

.automation-page__store-field span {
  color: var(--color-ink-soft);
  font-size: 0.88rem;
}

.automation-page__store-field select {
  min-height: 2.9rem;
  padding: 0.75rem 0.9rem;
  border-radius: 1rem;
  border: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.88);
  color: var(--color-ink-strong);
  font: inherit;
}

.automation-page__trigger {
  min-height: 2.9rem;
  padding: 0 1.4rem;
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-accent);
  color: #ffffff;
  font: inherit;
  font-weight: 600;
}

.automation-page__feedback {
  padding: 0.7rem 1rem;
  border-radius: var(--radius-lg);
  background: rgba(47, 125, 92, 0.1);
  color: var(--color-success);
  line-height: 1.6;
}

.automation-page__tabs {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  border-bottom: 1px solid var(--color-line);
}

.automation-page__tab {
  min-height: 2.6rem;
  padding: 0 1.1rem;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--color-ink-soft);
  font: inherit;
  font-weight: 600;
}

.automation-page__tab--active {
  color: var(--color-accent);
  border-bottom-color: var(--color-accent);
}

.automation-page__footer-note {
  color: var(--color-ink-soft);
  line-height: 1.6;
}
</style>
