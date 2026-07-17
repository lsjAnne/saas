import { apiClient } from '@/services/http/apiClient';
import type {
  AuditLogRecord,
  Organization,
  OrganizationMember
} from '@/services/apiTypes';

export interface InviteOrganizationMemberPayload {
  userId: string;
  userName: string;
  mobile?: string;
  roleCode: string;
}

export interface UpdateOrganizationMemberRolePayload {
  roleCode: string;
}

export interface RolePermissionMatrixView {
  roleCode: string;
  permissionCodes: string[];
}

export function getOrganizations(token: string) {
  return apiClient.get<Organization[]>('/api/organizations', token);
}

export function getOrganizationMembers(organizationId: string, token: string) {
  return apiClient.get<OrganizationMember[]>(`/api/organizations/${organizationId}/members`, token);
}

export function inviteOrganizationMember(
  organizationId: string,
  payload: InviteOrganizationMemberPayload,
  token: string
) {
  return apiClient.post<OrganizationMember>(
    `/api/organizations/${organizationId}/members/invite`,
    payload,
    token
  );
}

export function updateOrganizationMemberRole(
  organizationId: string,
  memberId: string,
  payload: UpdateOrganizationMemberRolePayload,
  token: string
) {
  return apiClient.put<OrganizationMember>(
    `/api/organizations/${organizationId}/members/${memberId}/role`,
    payload,
    token
  );
}

export function getTenantRolePermissionMatrix(token: string) {
  return apiClient.get<RolePermissionMatrixView[]>('/api/auth/rbac/roles', token);
}

export function getTenantAuditLogs(token: string) {
  return apiClient.get<AuditLogRecord[]>('/api/tenant/audit-logs', token);
}
