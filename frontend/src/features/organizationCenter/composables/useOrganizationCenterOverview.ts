import { computed, shallowRef } from 'vue';

import type { AuditLogRecord, Organization, OrganizationMember, Store } from '@/services/apiTypes';
import { ApiError } from '@/services/http/apiClient';
import {
  getOrganizationMembers,
  getOrganizations,
  getTenantAuditLogs,
  getTenantRolePermissionMatrix,
  inviteOrganizationMember,
  type InviteOrganizationMemberPayload,
  type RolePermissionMatrixView,
  updateOrganizationMemberRole
} from '@/services/organizationCenterService';
import { getStores } from '@/services/storeChannelService';

const ROLE_ORDER = ['owner', 'admin', 'operator', 'service'] as const;
const GOVERNANCE_LOG_TARGETS = new Set(['organization', 'organization_member']);

function roleRank(roleCode: string) {
  const index = ROLE_ORDER.indexOf(roleCode as (typeof ROLE_ORDER)[number]);
  return index === -1 ? ROLE_ORDER.length : index;
}

function normalizeRole(roleCode: string | null | undefined) {
  if (!roleCode) {
    return 'operator';
  }

  if (roleCode === 'tenant_admin') {
    return 'admin';
  }

  if (roleCode === 'customer_service') {
    return 'service';
  }

  return roleCode;
}

function isOptionalAccessError(error: unknown) {
  return error instanceof ApiError && (error.status === 403 || error.status === 404);
}

export function useOrganizationCenterOverview() {
  const organizations = shallowRef<Organization[]>([]);
  const selectedOrganizationId = shallowRef<string | null>(null);
  const organizationMembers = shallowRef<OrganizationMember[]>([]);
  const roleMatrix = shallowRef<RolePermissionMatrixView[]>([]);
  const stores = shallowRef<Store[]>([]);
  const auditLogs = shallowRef<AuditLogRecord[]>([]);
  const isLoading = shallowRef(false);
  const isSubmittingInvite = shallowRef(false);
  const updatingMemberId = shallowRef<string | null>(null);
  const errorMessage = shallowRef<string | null>(null);
  const optionalNotices = shallowRef<string[]>([]);

  const selectedOrganization = computed(
    () =>
      organizations.value.find((organization) => organization.id === selectedOrganizationId.value) ?? null
  );

  const selectedOrganizationStores = computed(() =>
    stores.value.filter((item) => item.organizationId === selectedOrganizationId.value)
  );

  const governanceAuditLogs = computed(() =>
    auditLogs.value.filter(
      (item) =>
        GOVERNANCE_LOG_TARGETS.has(item.targetType) ||
        item.actionType.includes('ORGANIZATION') ||
        item.actionType.includes('MEMBER_ROLE')
    )
  );

  const sortedRoleMatrix = computed(() =>
    [...roleMatrix.value].sort((left, right) => roleRank(left.roleCode) - roleRank(right.roleCode))
  );

  async function loadOptionalFeeds(token: string) {
    optionalNotices.value = [];

    const [storesResult, auditResult] = await Promise.allSettled([
      getStores(token),
      getTenantAuditLogs(token)
    ]);

    if (storesResult.status === 'fulfilled') {
      stores.value = storesResult.value;
    } else {
      stores.value = [];
      if (isOptionalAccessError(storesResult.reason)) {
        optionalNotices.value = [...optionalNotices.value, '当前角色暂无店铺范围查看权限，数据范围区仅展示组织级说明。'];
      } else {
        throw storesResult.reason;
      }
    }

    if (auditResult.status === 'fulfilled') {
      auditLogs.value = auditResult.value;
    } else {
      auditLogs.value = [];
      if (isOptionalAccessError(auditResult.reason)) {
        optionalNotices.value = [...optionalNotices.value, '当前角色暂无审计日志读取权限，审计入口区仅保留跳转提示。'];
      } else {
        throw auditResult.reason;
      }
    }
  }

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [organizationPayload, roleMatrixPayload] = await Promise.all([
        getOrganizations(token),
        getTenantRolePermissionMatrix(token)
      ]);

      organizations.value = organizationPayload;
      roleMatrix.value = roleMatrixPayload.map((item) => ({
        ...item,
        roleCode: normalizeRole(item.roleCode)
      }));

      const firstOrganizationId = organizationPayload[0]?.id ?? null;
      selectedOrganizationId.value = firstOrganizationId;
      organizationMembers.value = firstOrganizationId
        ? await getOrganizationMembers(firstOrganizationId, token)
        : [];

      await loadOptionalFeeds(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '组织与权限中心数据加载失败，请稍后重试';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  async function selectOrganization(organizationId: string, token: string) {
    selectedOrganizationId.value = organizationId;
    organizationMembers.value = await getOrganizationMembers(organizationId, token);
  }

  async function submitInvite(
    payload: InviteOrganizationMemberPayload,
    token: string
  ) {
    if (!selectedOrganizationId.value) {
      return;
    }

    isSubmittingInvite.value = true;
    errorMessage.value = null;

    try {
      const created = await inviteOrganizationMember(selectedOrganizationId.value, payload, token);
      organizationMembers.value = [...organizationMembers.value, created].sort(
        (left, right) => roleRank(left.roleCode) - roleRank(right.roleCode)
      );
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '邀请成员失败，请稍后重试';
      throw error;
    } finally {
      isSubmittingInvite.value = false;
    }
  }

  async function updateMemberRole(memberId: string, roleCode: string, token: string) {
    if (!selectedOrganizationId.value) {
      return;
    }

    updatingMemberId.value = memberId;
    errorMessage.value = null;

    try {
      const updated = await updateOrganizationMemberRole(
        selectedOrganizationId.value,
        memberId,
        { roleCode },
        token
      );

      organizationMembers.value = organizationMembers.value
        .map((item) => (item.memberId === memberId ? updated : item))
        .sort((left, right) => roleRank(left.roleCode) - roleRank(right.roleCode));
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '成员角色更新失败，请稍后重试';
      throw error;
    } finally {
      updatingMemberId.value = null;
    }
  }

  return {
    organizations,
    selectedOrganizationId,
    selectedOrganization,
    organizationMembers,
    roleMatrix: sortedRoleMatrix,
    stores,
    selectedOrganizationStores,
    governanceAuditLogs,
    optionalNotices,
    isLoading,
    isSubmittingInvite,
    updatingMemberId,
    errorMessage,
    load,
    selectOrganization,
    submitInvite,
    updateMemberRole
  };
}
