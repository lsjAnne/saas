import { computed, shallowRef } from 'vue';

import {
  getNotificationGatewayConfig,
  getNotificationGatewayOverview,
  getNotificationTemplates,
  getNotifications,
  replayDeadLetterNotificationTask,
  retryNotificationTask,
  updateNotificationGatewayConfig,
  updateNotificationTemplate
} from '@/services/supportWorkbenchService';
import type {
  NotificationChannelGatewayStatView,
  NotificationGatewayConfigView,
  NotificationGatewayOverviewView,
  NotificationGatewayProviderConfigView,
  NotificationGatewayProviderView,
  NotificationTask,
  NotificationTemplateView,
  ReplayNotificationTaskPayload,
  UpdateNotificationGatewayChannelBindingPayload,
  UpdateNotificationGatewayProviderPayload,
  UpdateNotificationTemplatePayload
} from '@/services/apiTypes';

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function isEscalatedTask(task: NotificationTask) {
  const status = normalizeText(task.sendStatus);
  const priority = normalizeText(task.priority);

  return (
    priority === 'urgent' ||
    status === 'failed' ||
    status === 'dead_letter' ||
    status === 'dead-letter'
  );
}

function isRecoverableTask(task: NotificationTask) {
  const status = normalizeText(task.sendStatus);
  return status === 'failed' || status === 'dead_letter' || status === 'dead-letter';
}

function replaceTask(tasks: NotificationTask[], updated: NotificationTask) {
  const nextTasks = tasks.map((task) =>
    task.notificationTaskId === updated.notificationTaskId ? updated : task
  );

  if (nextTasks.some((task) => task.notificationTaskId === updated.notificationTaskId)) {
    return nextTasks;
  }

  return [updated, ...tasks];
}

function replaceTemplate(templates: NotificationTemplateView[], updated: NotificationTemplateView) {
  return templates.map((template) =>
    template.notificationTemplateId === updated.notificationTemplateId ? updated : template
  );
}

export function useNotificationsCenterOverview() {
  const notifications = shallowRef<NotificationTask[]>([]);
  const gatewayOverview = shallowRef<NotificationGatewayOverviewView | null>(null);
  const gatewayConfig = shallowRef<NotificationGatewayConfigView | null>(null);
  const templates = shallowRef<NotificationTemplateView[]>([]);
  const isLoading = shallowRef(false);
  const isSavingBindings = shallowRef(false);
  const isSavingProviders = shallowRef(false);
  const savingTemplateId = shallowRef<string | null>(null);
  const runningTaskId = shallowRef<string | null>(null);
  const errorMessage = shallowRef<string | null>(null);
  const actionMessage = shallowRef<string | null>(null);
  const actionErrorMessage = shallowRef<string | null>(null);

  const recentTasks = computed(() =>
    [...notifications.value]
      .sort((left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt))
      .slice(0, 8)
  );

  const highlightedTasks = computed(() =>
    recentTasks.value.filter((task) => isEscalatedTask(task)).slice(0, 3)
  );

  const failedTasks = computed(() =>
    [...notifications.value]
      .filter((task) => isRecoverableTask(task))
      .sort((left, right) => {
        const priorityGap = Number(isEscalatedTask(right)) - Number(isEscalatedTask(left));
        if (priorityGap !== 0) {
          return priorityGap;
        }

        return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
      })
      .slice(0, 8)
  );

  const providerStats = computed<NotificationGatewayProviderView[]>(() =>
    [...(gatewayOverview.value?.providerStats ?? [])].sort((left, right) => {
      if (right.failedTaskCount !== left.failedTaskCount) {
        return right.failedTaskCount - left.failedTaskCount;
      }

      return right.routedTaskCount - left.routedTaskCount;
    })
  );

  const channelStats = computed<NotificationChannelGatewayStatView[]>(() =>
    [...(gatewayOverview.value?.channelStats ?? [])].sort(
      (left, right) => right.totalTaskCount - left.totalTaskCount
    )
  );

  const gatewayProviders = computed<NotificationGatewayProviderConfigView[]>(() =>
    [...(gatewayConfig.value?.providers ?? [])].sort((left, right) => {
      if (left.notifyType !== right.notifyType) {
        return left.notifyType.localeCompare(right.notifyType);
      }

      return left.gatewayCode.localeCompare(right.gatewayCode);
    })
  );

  const sortedTemplates = computed<NotificationTemplateView[]>(() =>
    [...templates.value].sort((left, right) => {
      if (left.enabled !== right.enabled) {
        return Number(right.enabled) - Number(left.enabled);
      }

      if (left.notifyType !== right.notifyType) {
        return left.notifyType.localeCompare(right.notifyType);
      }

      return left.templateCode.localeCompare(right.templateCode);
    })
  );

  async function refreshOverview(token: string) {
    const [nextNotifications, nextOverview] = await Promise.all([
      getNotifications(token),
      getNotificationGatewayOverview(token)
    ]);

    notifications.value = nextNotifications;
    gatewayOverview.value = nextOverview;
  }

  function resetActionFeedback() {
    actionMessage.value = null;
    actionErrorMessage.value = null;
  }

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;
    resetActionFeedback();

    try {
      const [notificationsPayload, gatewayPayload, gatewayConfigPayload, templatesPayload] =
        await Promise.all([
          getNotifications(token),
          getNotificationGatewayOverview(token),
          getNotificationGatewayConfig(token),
          getNotificationTemplates(token)
        ]);

      notifications.value = notificationsPayload;
      gatewayOverview.value = gatewayPayload;
      gatewayConfig.value = gatewayConfigPayload;
      templates.value = templatesPayload;
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '消息通知中心加载失败，请稍后重试';
    } finally {
      isLoading.value = false;
    }
  }

  async function saveChannelBindings(
    token: string,
    payload: {
      requireConfiguredGateway: boolean;
      channelBindings: UpdateNotificationGatewayChannelBindingPayload[];
    }
  ) {
    isSavingBindings.value = true;
    resetActionFeedback();

    try {
      gatewayConfig.value = await updateNotificationGatewayConfig(
        {
          requireConfiguredGateway: payload.requireConfiguredGateway,
          channelBindings: payload.channelBindings,
          providers: []
        },
        token
      );
      gatewayOverview.value = await getNotificationGatewayOverview(token);
      actionMessage.value = '通知渠道路由已更新，当前修改按运行时配置立即生效。';
    } catch (error) {
      actionErrorMessage.value =
        error instanceof Error ? error.message : '通知渠道配置保存失败，请稍后重试';
    } finally {
      isSavingBindings.value = false;
    }
  }

  async function saveGatewayProviders(
    token: string,
    providers: UpdateNotificationGatewayProviderPayload[]
  ) {
    isSavingProviders.value = true;
    resetActionFeedback();

    try {
      gatewayConfig.value = await updateNotificationGatewayConfig(
        {
          requireConfiguredGateway: gatewayConfig.value?.requireConfiguredGateway ?? false,
          channelBindings: [],
          providers
        },
        token
      );
      gatewayOverview.value = await getNotificationGatewayOverview(token);
      actionMessage.value = '通知网关配置已更新，后续通知会按照新的网关状态派发。';
    } catch (error) {
      actionErrorMessage.value =
        error instanceof Error ? error.message : '通知网关配置保存失败，请稍后重试';
    } finally {
      isSavingProviders.value = false;
    }
  }

  async function saveTemplate(
    token: string,
    notificationTemplateId: string,
    payload: UpdateNotificationTemplatePayload
  ) {
    savingTemplateId.value = notificationTemplateId;
    resetActionFeedback();

    try {
      const updatedTemplate = await updateNotificationTemplate(
        notificationTemplateId,
        payload,
        token
      );

      templates.value = replaceTemplate(templates.value, updatedTemplate);
      actionMessage.value = `模板 ${updatedTemplate.templateCode} 已更新。`;
    } catch (error) {
      actionErrorMessage.value =
        error instanceof Error ? error.message : '通知模板保存失败，请稍后重试';
    } finally {
      savingTemplateId.value = null;
    }
  }

  async function retryFailedTask(token: string, notificationTaskId: string) {
    runningTaskId.value = notificationTaskId;
    resetActionFeedback();

    try {
      const updatedTask = await retryNotificationTask(notificationTaskId, token);
      notifications.value = replaceTask(notifications.value, updatedTask);
      gatewayOverview.value = await getNotificationGatewayOverview(token);
      actionMessage.value = `任务 ${updatedTask.notificationTaskId} 已重新投递。`;
    } catch (error) {
      actionErrorMessage.value =
        error instanceof Error ? error.message : '通知任务重试失败，请稍后重试';
    } finally {
      runningTaskId.value = null;
    }
  }

  async function replayDeadLetterTask(
    token: string,
    notificationTaskId: string,
    payload: ReplayNotificationTaskPayload
  ) {
    runningTaskId.value = notificationTaskId;
    resetActionFeedback();

    try {
      const updatedTask = await replayDeadLetterNotificationTask(
        notificationTaskId,
        payload,
        token
      );
      notifications.value = replaceTask(notifications.value, updatedTask);
      gatewayOverview.value = await getNotificationGatewayOverview(token);
      actionMessage.value = `死信任务 ${updatedTask.notificationTaskId} 已重新入列。`;
    } catch (error) {
      actionErrorMessage.value =
        error instanceof Error ? error.message : '死信任务回放失败，请稍后重试';
    } finally {
      runningTaskId.value = null;
    }
  }

  async function refreshAll(token: string) {
    resetActionFeedback();

    try {
      await refreshOverview(token);
      gatewayConfig.value = await getNotificationGatewayConfig(token);
      templates.value = await getNotificationTemplates(token);
      actionMessage.value = '通知中心数据已刷新。';
    } catch (error) {
      actionErrorMessage.value =
        error instanceof Error ? error.message : '通知中心刷新失败，请稍后重试';
    }
  }

  return {
    gatewayConfig,
    gatewayOverview,
    providerStats,
    gatewayProviders,
    channelStats,
    templates: sortedTemplates,
    notifications,
    recentTasks,
    highlightedTasks,
    failedTasks,
    isLoading,
    isSavingBindings,
    isSavingProviders,
    savingTemplateId,
    runningTaskId,
    errorMessage,
    actionMessage,
    actionErrorMessage,
    load,
    refreshAll,
    saveChannelBindings,
    saveGatewayProviders,
    saveTemplate,
    retryFailedTask,
    replayDeadLetterTask
  };
}
