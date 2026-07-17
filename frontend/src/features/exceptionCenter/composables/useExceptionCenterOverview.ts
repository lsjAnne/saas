import { computed, shallowRef } from 'vue';

import {
  escalateException,
  getException,
  getExceptions,
  ignoreException,
  processException
} from '@/services/exceptionCenterService';
import type { ExceptionTask } from '@/services/apiTypes';

type ExceptionCategoryView = {
  code: string;
  label: string;
  count: number;
  openCount: number;
};

type ExceptionActionChoice = 'apply_sop' | 'manual_followup' | 'owner_callback';

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function severityWeight(severity: string | null | undefined) {
  const normalized = normalizeText(severity);

  if (normalized === 'critical') {
    return 4;
  }

  if (normalized === 'high') {
    return 3;
  }

  if (normalized === 'medium') {
    return 2;
  }

  return 1;
}

function statusWeight(status: string | null | undefined) {
  const normalized = normalizeText(status);

  if (normalized === 'new') {
    return 5;
  }

  if (normalized === 'todo') {
    return 4;
  }

  if (normalized === 'processing') {
    return 3;
  }

  if (normalized === 'escalated') {
    return 2;
  }

  if (normalized === 'resolved') {
    return 1;
  }

  return 0;
}

function isOpenStatus(status: string | null | undefined) {
  const normalized = normalizeText(status);
  return normalized !== 'resolved' && normalized !== 'ignored';
}

function formatCategoryLabel(code: string) {
  if (!code.trim()) {
    return '未分类';
  }

  return code
    .split(/[_-]+/)
    .filter(Boolean)
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(' ');
}

function buildSuggestedSop(task: ExceptionTask | null) {
  if (!task) {
    return [];
  }

  const normalizedType = normalizeText(task.exceptionType);
  const normalizedRelatedType = normalizeText(task.relatedType);

  if (normalizedType.includes('fulfillment') || normalizedRelatedType.includes('fulfillment')) {
    return [
      '先确认当前履约任务是否仍可重试，再判断是否需要人工改配货源。',
      '核对最近一次失败说明与关联订单状态，避免重复推进已失效任务。',
      '若同类异常连续出现，升级给履约负责人并回写 SOP 备注。'
    ];
  }

  if (normalizedType.includes('inventory') || normalizedRelatedType.includes('inventory')) {
    return [
      '优先核对库存快照与安全库存阈值，确认是否为真实缺货。',
      '若风险仍存在，回到补货中心创建补货或采购动作。',
      '必要时同步标记备用货源或暂缓相关自动化执行。'
    ];
  }

  if (normalizedType.includes('order') || normalizedRelatedType.includes('order')) {
    return [
      '先核对订单主状态、物流状态与当前责任人是否一致。',
      '确认是否需要重新审单、改配策略或转售后处理。',
      '将最终处理结果回写到异常备注，方便后续审计。'
    ];
  }

  return [
    '先核对关联业务对象与当前状态，确认这不是重复告警。',
    '参考当前建议文本执行标准动作，必要时补充人工备注。',
    '如果无法在本页收口，升级给对应域负责人并保留处理痕迹。'
  ];
}

export function useExceptionCenterOverview() {
  const exceptions = shallowRef<ExceptionTask[]>([]);
  const selectedExceptionId = shallowRef<string | null>(null);
  const activeCategoryCode = shallowRef('all');
  const actionChoice = shallowRef<ExceptionActionChoice>('apply_sop');
  const actionRemark = shallowRef('');
  const isLoading = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const categories = computed<ExceptionCategoryView[]>(() => {
    const bucket = new Map<string, ExceptionCategoryView>();

    for (const item of exceptions.value) {
      const code = item.exceptionType || 'uncategorized';
      const existing = bucket.get(code) ?? {
        code,
        label: formatCategoryLabel(code),
        count: 0,
        openCount: 0
      };
      existing.count += 1;
      existing.openCount += Number(isOpenStatus(item.status));
      bucket.set(code, existing);
    }

    const derived = [...bucket.values()].sort((left, right) => right.openCount - left.openCount);
    return [
      {
        code: 'all',
        label: '全部异常',
        count: exceptions.value.length,
        openCount: exceptions.value.filter((item) => isOpenStatus(item.status)).length
      }
    ].concat(derived);
  });

  const filteredExceptions = computed(() =>
    exceptions.value
      .filter((item) =>
        activeCategoryCode.value === 'all' ? true : item.exceptionType === activeCategoryCode.value
      )
      .sort((left, right) => {
        const statusDelta = statusWeight(right.status) - statusWeight(left.status);
        if (statusDelta !== 0) {
          return statusDelta;
        }

        const severityDelta = severityWeight(right.severity) - severityWeight(left.severity);
        if (severityDelta !== 0) {
          return severityDelta;
        }

        return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
      })
  );

  const selectedException = computed(
    () =>
      filteredExceptions.value.find((item) => item.exceptionTaskId === selectedExceptionId.value) ??
      exceptions.value.find((item) => item.exceptionTaskId === selectedExceptionId.value) ??
      null
  );

  const suggestedSop = computed(() => buildSuggestedSop(selectedException.value));

  const summary = computed(() => ({
    totalCount: exceptions.value.length,
    openCount: exceptions.value.filter((item) => isOpenStatus(item.status)).length,
    escalatedCount: exceptions.value.filter(
      (item) => normalizeText(item.status) === 'escalated'
    ).length,
    resolvedCount: exceptions.value.filter(
      (item) => normalizeText(item.status) === 'resolved'
    ).length,
    highSeverityCount: exceptions.value.filter((item) => severityWeight(item.severity) >= 3).length
  }));

  const canProcessSelected = computed(() => {
    const status = normalizeText(selectedException.value?.status);
    return ['new', 'todo', 'processing', 'escalated'].includes(status);
  });

  const canIgnoreSelected = computed(() => {
    const status = normalizeText(selectedException.value?.status);
    return ['new', 'todo', 'processing'].includes(status);
  });

  const canEscalateSelected = computed(() => {
    const status = normalizeText(selectedException.value?.status);
    return ['new', 'todo', 'processing'].includes(status);
  });

  function replaceException(updated: ExceptionTask) {
    exceptions.value = exceptions.value.map((item) =>
      item.exceptionTaskId === updated.exceptionTaskId ? updated : item
    );
  }

  async function selectException(exceptionTaskId: string, token: string) {
    selectedExceptionId.value = exceptionTaskId;
    actionFeedback.value = null;

    const detail = await getException(exceptionTaskId, token);
    replaceException(detail);
  }

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;
    actionFeedback.value = null;

    try {
      const payload = await getExceptions(token);
      exceptions.value = payload;

      const firstVisibleId =
        payload.find((item) =>
          activeCategoryCode.value === 'all' ? true : item.exceptionType === activeCategoryCode.value
        )?.exceptionTaskId ?? payload[0]?.exceptionTaskId ?? null;

      selectedExceptionId.value = firstVisibleId;

      if (firstVisibleId) {
        await selectException(firstVisibleId, token);
      }
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '异常中心加载失败，请稍后重试';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  function selectCategory(code: string) {
    activeCategoryCode.value = code;
    const firstVisible = filteredExceptions.value[0];
    selectedExceptionId.value = firstVisible?.exceptionTaskId ?? null;
    actionFeedback.value = null;
  }

  async function processSelected(token: string, operatorId?: string) {
    if (!selectedException.value || !canProcessSelected.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      const updated = await processException(selectedException.value.exceptionTaskId, token, {
        action: actionChoice.value,
        operatorId,
        remark: actionRemark.value || undefined
      });
      replaceException(updated);
      actionFeedback.value = '异常项已按当前 SOP 处理并回写结果。';
      actionRemark.value = '';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function ignoreSelected(token: string, operatorId?: string) {
    if (!selectedException.value || !canIgnoreSelected.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      const updated = await ignoreException(selectedException.value.exceptionTaskId, token, {
        operatorId,
        remark: actionRemark.value || undefined
      });
      replaceException(updated);
      actionFeedback.value = '异常项已被忽略，并保留本次处理备注。';
      actionRemark.value = '';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function escalateSelected(token: string, operatorId?: string) {
    if (!selectedException.value || !canEscalateSelected.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      const updated = await escalateException(selectedException.value.exceptionTaskId, token, {
        operatorId,
        remark: actionRemark.value || undefined
      });
      replaceException(updated);
      actionFeedback.value = '异常项已升级，建议继续跟进责任人。';
      actionRemark.value = '';
    } finally {
      isRunningAction.value = false;
    }
  }

  return {
    exceptions: filteredExceptions,
    categories,
    selectedExceptionId,
    selectedException,
    activeCategoryCode,
    actionChoice,
    actionRemark,
    suggestedSop,
    summary,
    canProcessSelected,
    canIgnoreSelected,
    canEscalateSelected,
    isLoading,
    isRunningAction,
    errorMessage,
    actionFeedback,
    load,
    selectCategory,
    selectException,
    processSelected,
    ignoreSelected,
    escalateSelected
  };
}
