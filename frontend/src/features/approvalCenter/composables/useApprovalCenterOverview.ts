import { computed, shallowRef } from 'vue';

import {
  approveApproval,
  getApprovals,
  getApprovalTemplates,
  getGovernanceApprovalRequests,
  rejectApproval,
  transferApproval
} from '@/services/approvalCenterService';
import type {
  ApprovalInstance,
  ApprovalTemplateView,
  GovernanceApprovalRequestView
} from '@/services/apiTypes';

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function isPendingStatus(status: string | null | undefined) {
  const normalized = normalizeText(status);
  return (
    normalized.includes('pending') ||
    normalized.includes('submitted') ||
    normalized.includes('review') ||
    normalized.includes('processing')
  );
}

export function useApprovalCenterOverview() {
  const approvals = shallowRef<ApprovalInstance[]>([]);
  const templates = shallowRef<ApprovalTemplateView[]>([]);
  const governanceRequests = shallowRef<GovernanceApprovalRequestView[]>([]);
  const selectedApprovalId = shallowRef<string | null>(null);
  const actionRemark = shallowRef('');
  const transferHandlerId = shallowRef('');
  const isLoading = shallowRef(false);
  const isSubmittingAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const sortedApprovals = computed(() =>
    [...approvals.value].sort((left, right) => {
      const pendingWeight = Number(isPendingStatus(right.status)) - Number(isPendingStatus(left.status));
      if (pendingWeight !== 0) {
        return pendingWeight;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const selectedApproval = computed(
    () =>
      sortedApprovals.value.find((approval) => approval.approvalId === selectedApprovalId.value) ?? null
  );

  const summary = computed(() => ({
    pendingApprovalCount: approvals.value.filter((approval) => isPendingStatus(approval.status)).length,
    processedApprovalCount: approvals.value.filter((approval) => !isPendingStatus(approval.status)).length,
    enabledTemplateCount: templates.value.filter((template) => template.enabled).length,
    governanceOpenCount: governanceRequests.value.filter((request) =>
      isPendingStatus(request.requestStatus)
    ).length
  }));

  const sortedTemplates = computed(() =>
    [...templates.value].sort((left, right) => {
      if (left.enabled !== right.enabled) {
        return Number(right.enabled) - Number(left.enabled);
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const sortedGovernanceRequests = computed(() =>
    [...governanceRequests.value].sort((left, right) => {
      const pendingWeight =
        Number(isPendingStatus(right.requestStatus)) - Number(isPendingStatus(left.requestStatus));
      if (pendingWeight !== 0) {
        return pendingWeight;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  function selectApproval(approvalId: string) {
    selectedApprovalId.value = approvalId;

    const selected = approvals.value.find((approval) => approval.approvalId === approvalId);
    transferHandlerId.value = selected?.currentHandlerId ?? '';
    actionRemark.value = '';
    actionFeedback.value = null;
  }

  function syncRelatedGovernanceRequest(updatedApproval: ApprovalInstance) {
    governanceRequests.value = governanceRequests.value.map((request) =>
      request.requestId === updatedApproval.relatedId
        ? {
            ...request,
            requestStatus: updatedApproval.status,
            currentHandlerId: updatedApproval.currentHandlerId,
            remark: updatedApproval.resultRemark ?? request.remark
          }
        : request
    );
  }

  function replaceApproval(updatedApproval: ApprovalInstance) {
    approvals.value = approvals.value.map((approval) =>
      approval.approvalId === updatedApproval.approvalId ? updatedApproval : approval
    );
    syncRelatedGovernanceRequest(updatedApproval);
  }

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;
    actionFeedback.value = null;

    try {
      const [approvalsPayload, templatesPayload, governancePayload] = await Promise.all([
        getApprovals(token),
        getApprovalTemplates(token),
        getGovernanceApprovalRequests(token)
      ]);

      approvals.value = approvalsPayload;
      templates.value = templatesPayload;
      governanceRequests.value = governancePayload;

      if (
        selectedApprovalId.value &&
        approvalsPayload.some((approval) => approval.approvalId === selectedApprovalId.value)
      ) {
        selectApproval(selectedApprovalId.value);
      } else if (approvalsPayload.length) {
        selectApproval(approvalsPayload[0].approvalId);
      } else {
        selectedApprovalId.value = null;
        transferHandlerId.value = '';
        actionRemark.value = '';
      }
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '审批与协作中心加载失败，请稍后重试';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  async function approveSelected(token: string) {
    if (!selectedApproval.value) {
      return;
    }

    isSubmittingAction.value = true;
    actionFeedback.value = null;

    try {
      const updated = await approveApproval(
        selectedApproval.value.approvalId,
        token,
        actionRemark.value || undefined
      );
      replaceApproval(updated);
      actionFeedback.value = '该审批项已通过。';
      actionRemark.value = '';
    } finally {
      isSubmittingAction.value = false;
    }
  }

  async function rejectSelected(token: string) {
    if (!selectedApproval.value) {
      return;
    }

    isSubmittingAction.value = true;
    actionFeedback.value = null;

    try {
      const updated = await rejectApproval(
        selectedApproval.value.approvalId,
        token,
        actionRemark.value || undefined
      );
      replaceApproval(updated);
      actionFeedback.value = '该审批项已驳回。';
      actionRemark.value = '';
    } finally {
      isSubmittingAction.value = false;
    }
  }

  async function transferSelected(token: string) {
    if (!selectedApproval.value) {
      return;
    }

    if (!transferHandlerId.value.trim()) {
      actionFeedback.value = '请输入接收该审批的处理人 ID。';
      return;
    }

    isSubmittingAction.value = true;
    actionFeedback.value = null;

    try {
      const updated = await transferApproval(selectedApproval.value.approvalId, token, {
        currentHandlerId: transferHandlerId.value.trim(),
        remark: actionRemark.value || undefined
      });
      replaceApproval(updated);
      actionFeedback.value = '审批项已完成转交。';
      actionRemark.value = '';
      transferHandlerId.value = updated.currentHandlerId ?? '';
    } finally {
      isSubmittingAction.value = false;
    }
  }

  return {
    approvals: sortedApprovals,
    templates: sortedTemplates,
    governanceRequests: sortedGovernanceRequests,
    selectedApprovalId,
    selectedApproval,
    actionRemark,
    transferHandlerId,
    summary,
    isLoading,
    isSubmittingAction,
    errorMessage,
    actionFeedback,
    load,
    selectApproval,
    approveSelected,
    rejectSelected,
    transferSelected
  };
}
