import { apiClient } from '@/services/http/apiClient';
import type {
  ExportMemberGroupPayload,
  MemberCrmAnalysisView,
  MemberCrmLinkageView,
  MemberCrmProfileView,
  MemberDetailView,
  MemberGroupExportView,
  MemberPageResult,
  MemberSegmentExecutionView,
  MemberSegmentRuleView,
  MemberTag,
  MemberTagSummaryView,
  MemberTouchTaskView,
  SaveMemberCrmProfilePayload,
  SaveMemberSegmentRulePayload,
  SaveMemberTagPayload,
  SaveMemberTouchTaskPayload
} from '@/services/apiTypes';

interface MemberQuery {
  storeId?: string;
  levelCode?: string;
  tagCode?: string;
  lifecycleStage?: string;
  page?: number;
  pageSize?: number;
}

interface TouchTaskQuery {
  storeId?: string;
  memberId?: string;
  taskType?: string;
  status?: string;
}

function buildQuery(
  query: Record<string, string | number | undefined | null>
): string {
  const searchParams = new URLSearchParams();

  Object.entries(query).forEach(([key, value]) => {
    if (value == null || value === '') {
      return;
    }

    searchParams.set(key, String(value));
  });

  const serialized = searchParams.toString();
  return serialized ? `?${serialized}` : '';
}

export function getMembers(query: MemberQuery, token: string) {
  return apiClient.get<MemberPageResult>(`/api/members${buildQuery(query)}`, token);
}

export function getMember(memberId: string, token: string) {
  return apiClient.get<MemberDetailView>(`/api/members/${memberId}`, token);
}

export function saveMemberCrmProfile(
  memberId: string,
  payload: SaveMemberCrmProfilePayload,
  token: string
) {
  return apiClient.post<MemberCrmProfileView>(
    `/api/members/${memberId}/crm-profile`,
    payload,
    token
  );
}

export function getMemberCrmLinkage(memberId: string, token: string) {
  return apiClient.get<MemberCrmLinkageView>(`/api/members/${memberId}/crm-linkage`, token);
}

export function addMemberTag(
  memberId: string,
  payload: SaveMemberTagPayload,
  token: string
) {
  return apiClient.post<MemberTag>(`/api/members/${memberId}/tags`, payload, token);
}

export function removeMemberTag(memberId: string, memberTagId: string, token: string) {
  return apiClient.delete<MemberTag>(`/api/members/${memberId}/tags/${memberTagId}`, token);
}

export function getMemberTags(storeId: string | undefined, token: string) {
  const query = storeId ? `?storeId=${encodeURIComponent(storeId)}` : '';
  return apiClient.get<MemberTagSummaryView[]>(`/api/member-tags${query}`, token);
}

export function createMemberSegmentRule(
  payload: SaveMemberSegmentRulePayload,
  token: string
) {
  return apiClient.post<MemberSegmentRuleView>('/api/member-segment-rules', payload, token);
}

export function executeMemberSegmentRule(ruleId: string, token: string) {
  return apiClient.post<MemberSegmentExecutionView>(
    `/api/member-segment-rules/${ruleId}/execute`,
    null,
    token
  );
}

export function getMemberCrmAnalysis(storeId: string | undefined, token: string) {
  const query = storeId ? `?storeId=${encodeURIComponent(storeId)}` : '';
  return apiClient.get<MemberCrmAnalysisView>(`/api/member-crm-analysis${query}`, token);
}

export function createMemberTouchTask(
  payload: SaveMemberTouchTaskPayload,
  token: string
) {
  return apiClient.post<MemberTouchTaskView>('/api/member-touch-tasks', payload, token);
}

export function getMemberTouchTasks(query: TouchTaskQuery, token: string) {
  return apiClient.get<MemberTouchTaskView[]>(
    `/api/member-touch-tasks${buildQuery(query)}`,
    token
  );
}

export function exportMemberGroup(payload: ExportMemberGroupPayload, token: string) {
  return apiClient.post<MemberGroupExportView>('/api/member-groups/export', payload, token);
}
