import { computed, reactive, shallowRef } from 'vue';

import {
  approveAutomationTask,
  createAutomationAction,
  deleteAutomationAction,
  getAutomationActions,
  getAutomationApprovals,
  getAutomationExecutions,
  getAutomationRules,
  getAutomationStats,
  rejectAutomationTask,
  triggerAutomationOrderSync,
  updateAutomationAction
} from '@/services/automationCenterService';
import { getStores } from '@/services/storeChannelService';
import type {
  AutomationAction,
  AutomationApprovalTask,
  AutomationExecution,
  AutomationOrderSyncTriggerResult,
  AutomationPage,
  AutomationRule,
  AutomationStats,
  AutomationActionType,
  AutomationExecutionStatus,
  AutomationTriggerSource,
  CreateAutomationActionPayload,
  UpdateAutomationActionPayload
} from '@/services/automationTypes';
import type { Store } from '@/services/apiTypes';

const PAGE_SIZE = 10;

export interface AutomationExecutionFilter {
  status: AutomationExecutionStatus | '';
  actionType: AutomationActionType | '';
  triggerSource: AutomationTriggerSource | '';
}

export function useAutomationCenter() {
  const stores = shallowRef<Store[]>([]);
  const selectedStoreId = shallowRef('');
  const stats = shallowRef<AutomationStats | null>(null);
  const rules = shallowRef<AutomationRule[]>([]);
  const actionsPage = shallowRef<AutomationPage<AutomationAction> | null>(null);
  const executionsPage = shallowRef<AutomationPage<AutomationExecution> | null>(null);
  const approvalsPage = shallowRef<AutomationPage<AutomationApprovalTask> | null>(null);

  const actionPage = shallowRef(1);
  const executionPage = shallowRef(1);
  const approvalPage = shallowRef(1);

  const executionFilter = reactive<AutomationExecutionFilter>({
    status: '',
    actionType: '',
    triggerSource: ''
  });

  const isLoading = shallowRef(false);
  const isMutating = shallowRef(false);
  const isTriggering = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const selectedStore = computed(
    () =>
      stores.value.find((store) => store.storeId === selectedStoreId.value) ??
      stores.value[0] ??
      null
  );

  function toErrorMessage(error: unknown, fallback: string) {
    return error instanceof Error ? error.message : fallback;
  }

  async function reloadActions(token: string) {
    const payload = await getAutomationActions(
      {
        storeId: selectedStoreId.value || undefined,
        page: actionPage.value,
        size: PAGE_SIZE
      },
      token
    );
    actionsPage.value = payload;
  }

  async function reloadExecutions(token: string) {
    const payload = await getAutomationExecutions(
      {
        storeId: selectedStoreId.value || undefined,
        status: executionFilter.status || undefined,
        actionType: executionFilter.actionType || undefined,
        triggerSource: executionFilter.triggerSource || undefined,
        page: executionPage.value,
        size: PAGE_SIZE
      },
      token
    );
    executionsPage.value = payload;
  }

  async function reloadApprovals(token: string) {
    const payload = await getAutomationApprovals(
      {
        storeId: selectedStoreId.value || undefined,
        page: approvalPage.value,
        size: PAGE_SIZE
      },
      token
    );
    approvalsPage.value = payload;
  }

  async function reloadStats(token: string) {
    const payload = await getAutomationStats(
      selectedStoreId.value || undefined,
      token
    );
    stats.value = payload;
  }

  async function reloadRules(token: string) {
    const payload = await getAutomationRules(token);
    rules.value = payload;
  }

  async function reloadAll(token: string) {
    await Promise.all([
      reloadStats(token),
      reloadRules(token),
      reloadActions(token),
      reloadExecutions(token),
      reloadApprovals(token)
    ]);
  }

  async function bootstrap(token: string) {
    isLoading.value = true;
    errorMessage.value = null;
    actionFeedback.value = null;

    try {
      const storeList = await getStores(token);
      stores.value = storeList;

      if (!selectedStoreId.value && storeList.length > 0) {
        selectedStoreId.value = storeList[0].storeId;
      }

      await reloadAll(token);
    } catch (error) {
      errorMessage.value = toErrorMessage(error, '自动化控制台加载失败，请稍后重试');
    } finally {
      isLoading.value = false;
    }
  }

  async function selectStore(storeId: string, token: string) {
    selectedStoreId.value = storeId;
    actionPage.value = 1;
    executionPage.value = 1;
    approvalPage.value = 1;
    actionFeedback.value = null;
    actionError.value = null;

    try {
      await reloadAll(token);
    } catch (error) {
      actionError.value = toErrorMessage(error, '切换店铺后加载自动化数据失败');
    }
  }

  async function applyExecutionFilter(token: string) {
    executionPage.value = 1;
    try {
      await reloadExecutions(token);
    } catch (error) {
      actionError.value = toErrorMessage(error, '刷新执行记录失败');
    }
  }

  async function goExecutionPage(nextPage: number, token: string) {
    const total = executionsPage.value?.total ?? 0;
    const maxPage = Math.max(1, Math.ceil(total / PAGE_SIZE));
    if (nextPage < 1 || nextPage > maxPage) {
      return;
    }
    executionPage.value = nextPage;
    try {
      await reloadExecutions(token);
    } catch (error) {
      actionError.value = toErrorMessage(error, '翻页失败');
    }
  }

  async function goApprovalPage(nextPage: number, token: string) {
    const total = approvalsPage.value?.total ?? 0;
    const maxPage = Math.max(1, Math.ceil(total / PAGE_SIZE));
    if (nextPage < 1 || nextPage > maxPage) {
      return;
    }
    approvalPage.value = nextPage;
    try {
      await reloadApprovals(token);
    } catch (error) {
      actionError.value = toErrorMessage(error, '翻页失败');
    }
  }

  async function goActionPage(nextPage: number, token: string) {
    const total = actionsPage.value?.total ?? 0;
    const maxPage = Math.max(1, Math.ceil(total / PAGE_SIZE));
    if (nextPage < 1 || nextPage > maxPage) {
      return;
    }
    actionPage.value = nextPage;
    try {
      await reloadActions(token);
    } catch (error) {
      actionError.value = toErrorMessage(error, '翻页失败');
    }
  }

  async function createAction(payload: CreateAutomationActionPayload, token: string) {
    isMutating.value = true;
    actionError.value = null;
    try {
      const created = await createAutomationAction(payload, token);
      actionFeedback.value = `动作 ${created.actionId} 已绑定到规则 ${created.ruleName}。`;
      await Promise.all([reloadActions(token), reloadStats(token)]);
    } catch (error) {
      actionError.value = toErrorMessage(error, '新增动作绑定失败');
    } finally {
      isMutating.value = false;
    }
  }

  async function updateAction(
    actionId: string,
    payload: UpdateAutomationActionPayload,
    token: string
  ) {
    isMutating.value = true;
    actionError.value = null;
    try {
      const updated = await updateAutomationAction(actionId, payload, token);
      actionFeedback.value = `动作 ${updated.actionId} 已更新。`;
      await Promise.all([reloadActions(token), reloadStats(token)]);
    } catch (error) {
      actionError.value = toErrorMessage(error, '更新动作失败');
    } finally {
      isMutating.value = false;
    }
  }

  async function toggleActionEnabled(
    action: AutomationAction,
    token: string
  ) {
    await updateAction(
      action.actionId,
      {
        actionType: action.actionType,
        actionConfig: action.actionConfig,
        priority: action.priority,
        enabled: !action.enabled
      },
      token
    );
  }

  async function removeAction(actionId: string, token: string) {
    isMutating.value = true;
    actionError.value = null;
    try {
      await deleteAutomationAction(actionId, token);
      actionFeedback.value = `动作 ${actionId} 已删除。`;
      await Promise.all([reloadActions(token), reloadStats(token)]);
    } catch (error) {
      actionError.value = toErrorMessage(error, '删除动作失败');
    } finally {
      isMutating.value = false;
    }
  }

  async function approveTask(taskId: string, comment: string | undefined, token: string) {
    isMutating.value = true;
    actionError.value = null;
    try {
      await approveAutomationTask(taskId, comment, token);
      actionFeedback.value = `人工确认 ${taskId} 已通过。`;
      await Promise.all([
        reloadApprovals(token),
        reloadExecutions(token),
        reloadStats(token)
      ]);
    } catch (error) {
      actionError.value = toErrorMessage(error, '通过人工确认失败');
    } finally {
      isMutating.value = false;
    }
  }

  async function rejectTask(
    taskId: string,
    comment: string | undefined,
    token: string
  ) {
    isMutating.value = true;
    actionError.value = null;
    try {
      await rejectAutomationTask(taskId, comment, token);
      actionFeedback.value = `人工确认 ${taskId} 已拒绝。`;
      await Promise.all([
        reloadApprovals(token),
        reloadExecutions(token),
        reloadStats(token)
      ]);
    } catch (error) {
      actionError.value = toErrorMessage(error, '拒绝人工确认失败');
    } finally {
      isMutating.value = false;
    }
  }

  async function triggerOrderSync(token: string): Promise<AutomationOrderSyncTriggerResult | null> {
    if (!selectedStoreId.value) {
      actionError.value = '请先选择一个店铺再手动拉单。';
      return null;
    }

    isTriggering.value = true;
    actionError.value = null;
    actionFeedback.value = null;

    try {
      const result = await triggerAutomationOrderSync(selectedStoreId.value, token);
      actionFeedback.value =
        `已拉单 ${result.pulledOrderCount} 笔，命中规则 ${result.matchedRuleCount} 条，` +
        `产生执行 ${result.executionCount} 条。`;
      await Promise.all([
        reloadExecutions(token),
        reloadApprovals(token),
        reloadStats(token)
      ]);
      return result;
    } catch (error) {
      actionError.value = toErrorMessage(error, '手动拉单触发失败');
      return null;
    } finally {
      isTriggering.value = false;
    }
  }

  return reactive({
    stores,
    selectedStoreId,
    selectedStore,
    stats,
    rules,
    actionsPage,
    executionsPage,
    approvalsPage,
    actionPage,
    executionPage,
    approvalPage,
    executionFilter,
    isLoading,
    isMutating,
    isTriggering,
    errorMessage,
    actionError,
    actionFeedback,
    bootstrap,
    selectStore,
    applyExecutionFilter,
    goExecutionPage,
    goApprovalPage,
    goActionPage,
    reloadAll,
    createAction,
    updateAction,
    toggleActionEnabled,
    removeAction,
    approveTask,
    rejectTask,
    triggerOrderSync
  });
}
