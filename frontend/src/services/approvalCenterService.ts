import { apiClient } from '@/services/http/apiClient';
import type {
  ApprovalInstance,
  ApprovalTemplateView,
  GovernanceApprovalRequestView
} from '@/services/apiTypes';

export function getApprovals(token: string) {
  return apiClient.get<ApprovalInstance[]>('/api/approvals', token);
}

export function approveApproval(approvalId: string, token: string, remark?: string) {
  return apiClient.post<ApprovalInstance>(
    `/api/approvals/${approvalId}/approve`,
    {
      remark
    },
    token
  );
}

export function rejectApproval(approvalId: string, token: string, remark?: string) {
  return apiClient.post<ApprovalInstance>(
    `/api/approvals/${approvalId}/reject`,
    {
      remark
    },
    token
  );
}

export function transferApproval(
  approvalId: string,
  token: string,
  payload: { currentHandlerId: string; remark?: string }
) {
  return apiClient.post<ApprovalInstance>(
    `/api/approvals/${approvalId}/transfer`,
    payload,
    token
  );
}

export function getApprovalTemplates(token: string) {
  return apiClient.get<ApprovalTemplateView[]>('/api/approval-templates', token);
}

export function getGovernanceApprovalRequests(token: string) {
  return apiClient.get<GovernanceApprovalRequestView[]>(
    '/api/governance-approval-requests',
    token
  );
}
