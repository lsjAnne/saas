import { computed, shallowRef } from 'vue';

import { getSubscriptionPlans, registerTenant } from '@/services/onboardingService';
import {
  getAdminTenants,
  purchaseAdminTenantSeats,
  resumeAdminTenant,
  startAdminTenantTrial,
  subscribeAdminTenant,
  suspendAdminTenant,
  upgradeAdminTenantPlan,
  downgradeAdminTenantPlan
} from '@/services/platformAdminService';
import type {
  AdminTenantOverview,
  PlanChangePayload,
  RegisterTenantRequest,
  SeatPurchasePayload,
  SubscriptionActionPayload,
  SubscriptionPlan
} from '@/services/apiTypes';

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function isSuspended(status: string | null | undefined) {
  return normalizeText(status).includes('suspend');
}

function isTrial(status: string | null | undefined) {
  return normalizeText(status) === 'trial';
}

function isExpiringSoon(trialEndAt: string | null | undefined) {
  if (!trialEndAt) {
    return false;
  }

  const diff = toTimestamp(trialEndAt) - Date.now();
  return diff >= 0 && diff <= 7 * 24 * 60 * 60 * 1000;
}

export function useTenantCenterOverview() {
  const tenants = shallowRef<AdminTenantOverview[]>([]);
  const plans = shallowRef<SubscriptionPlan[]>([]);
  const selectedTenantId = shallowRef<string | null>(null);
  const isLoading = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const sortedTenants = computed(() =>
    [...tenants.value].sort((left, right) => {
      if (isSuspended(left.tenantStatus) !== isSuspended(right.tenantStatus)) {
        return Number(isSuspended(left.tenantStatus)) - Number(isSuspended(right.tenantStatus));
      }

      if (isExpiringSoon(left.trialEndAt) !== isExpiringSoon(right.trialEndAt)) {
        return Number(isExpiringSoon(right.trialEndAt)) - Number(isExpiringSoon(left.trialEndAt));
      }

      return toTimestamp(left.trialEndAt) - toTimestamp(right.trialEndAt);
    })
  );

  const selectedTenant = computed(
    () => sortedTenants.value.find((tenant) => tenant.tenantId === selectedTenantId.value) ?? null
  );

  const selectedPlan = computed(
    () =>
      plans.value.find((plan) => plan.planCode === selectedTenant.value?.planCode) ?? null
  );

  const summary = computed(() => ({
    totalTenantCount: tenants.value.length,
    activeTenantCount: tenants.value.filter((tenant) => normalizeText(tenant.tenantStatus) === 'active').length,
    trialTenantCount: tenants.value.filter((tenant) => isTrial(tenant.tenantStatus)).length,
    suspendedTenantCount: tenants.value.filter((tenant) => isSuspended(tenant.tenantStatus)).length,
    expiringSoonCount: tenants.value.filter((tenant) => isExpiringSoon(tenant.trialEndAt)).length,
    totalSeatCount: tenants.value.reduce((sum, tenant) => sum + tenant.seatCount, 0),
    totalQuotaUsageRate:
      tenants.value.reduce((sum, tenant) => sum + tenant.totalUsedAmount, 0) /
      Math.max(tenants.value.reduce((sum, tenant) => sum + tenant.totalQuotaLimit, 0), 1)
  }));

  const expiringTenants = computed(() =>
    sortedTenants.value.filter(
      (tenant) =>
        tenant.trialEndAt &&
        (isTrial(tenant.tenantStatus) || normalizeText(tenant.subscriptionStatus) === 'trial')
    )
  );

  function clearActionState() {
    actionError.value = null;
    actionFeedback.value = null;
  }

  function applySelection(preferredTenantId?: string | null) {
    selectedTenantId.value =
      sortedTenants.value.find((tenant) => tenant.tenantId === preferredTenantId)?.tenantId ??
      sortedTenants.value[0]?.tenantId ??
      null;
  }

  async function refreshTenants(token: string, preferredTenantId?: string | null) {
    tenants.value = await getAdminTenants(token);
    applySelection(preferredTenantId ?? selectedTenantId.value);
  }

  function planLevel(planCode: string | null | undefined) {
    return plans.value.findIndex((plan) => plan.planCode === planCode);
  }

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;
    clearActionState();

    try {
      const [tenantPayload, planPayload] = await Promise.all([
        getAdminTenants(token),
        getSubscriptionPlans()
      ]);

      tenants.value = tenantPayload;
      plans.value = planPayload;
      applySelection(selectedTenantId.value);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '租户中心数据加载失败，请稍后重试';
    } finally {
      isLoading.value = false;
    }
  }

  function selectTenant(tenantId: string) {
    selectedTenantId.value = tenantId;
  }

  async function createTenant(payload: RegisterTenantRequest, token: string) {
    isRunningAction.value = true;
    clearActionState();

    try {
      const created = await registerTenant(payload);
      await startAdminTenantTrial(created.tenantId, token);
      await refreshTenants(token, created.tenantId);
      actionFeedback.value = `租户 ${created.tenantName} 已开通并进入试用期。`;
    } catch (error) {
      actionError.value = error instanceof Error ? error.message : '租户开通失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function startTrial(token: string) {
    if (!selectedTenantId.value) {
      return;
    }

    isRunningAction.value = true;
    clearActionState();

    try {
      await startAdminTenantTrial(selectedTenantId.value, token);
      await refreshTenants(token, selectedTenantId.value);
      actionFeedback.value = '已为当前租户开启试用期。';
    } catch (error) {
      actionError.value = error instanceof Error ? error.message : '开启试用失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function suspendTenant(token: string) {
    if (!selectedTenantId.value) {
      return;
    }

    isRunningAction.value = true;
    clearActionState();

    try {
      await suspendAdminTenant(selectedTenantId.value, token);
      await refreshTenants(token, selectedTenantId.value);
      actionFeedback.value = '当前租户已停服。';
    } catch (error) {
      actionError.value = error instanceof Error ? error.message : '停服失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function resumeTenant(token: string) {
    if (!selectedTenantId.value) {
      return;
    }

    isRunningAction.value = true;
    clearActionState();

    try {
      await resumeAdminTenant(selectedTenantId.value, token);
      await refreshTenants(token, selectedTenantId.value);
      actionFeedback.value = '当前租户已恢复服务。';
    } catch (error) {
      actionError.value = error instanceof Error ? error.message : '恢复服务失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitPlanChange(
    payload: SubscriptionActionPayload | PlanChangePayload,
    token: string
  ) {
    if (!selectedTenantId.value || !selectedTenant.value) {
      return;
    }

    isRunningAction.value = true;
    clearActionState();

    try {
      if ('seatCount' in payload) {
        await subscribeAdminTenant(selectedTenantId.value, payload, token);
        actionFeedback.value = '租户订阅已开通。';
      } else {
        const currentLevel = planLevel(selectedTenant.value.planCode);
        const targetLevel = planLevel(payload.planCode);

        if (targetLevel === -1) {
          throw new Error('目标套餐不存在');
        }

        if (currentLevel === -1) {
          throw new Error('当前租户尚未绑定可升级套餐，请先执行订阅开通');
        }

        if (targetLevel > currentLevel) {
          await upgradeAdminTenantPlan(selectedTenantId.value, payload, token);
          actionFeedback.value = '租户套餐已升级。';
        } else if (targetLevel < currentLevel) {
          await downgradeAdminTenantPlan(selectedTenantId.value, payload, token);
          actionFeedback.value = '租户套餐已降级。';
        } else {
          actionFeedback.value = '目标套餐与当前套餐一致，无需变更。';
          isRunningAction.value = false;
          return;
        }
      }

      await refreshTenants(token, selectedTenantId.value);
    } catch (error) {
      actionError.value = error instanceof Error ? error.message : '套餐调整失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function purchaseSeats(payload: SeatPurchasePayload, token: string) {
    if (!selectedTenantId.value) {
      return;
    }

    isRunningAction.value = true;
    clearActionState();

    try {
      await purchaseAdminTenantSeats(selectedTenantId.value, payload, token);
      await refreshTenants(token, selectedTenantId.value);
      actionFeedback.value = '租户席位已增加。';
    } catch (error) {
      actionError.value = error instanceof Error ? error.message : '席位调整失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  return {
    tenants: sortedTenants,
    plans,
    selectedTenantId,
    selectedTenant,
    selectedPlan,
    summary,
    expiringTenants,
    isLoading,
    isRunningAction,
    errorMessage,
    actionError,
    actionFeedback,
    load,
    selectTenant,
    createTenant,
    startTrial,
    suspendTenant,
    resumeTenant,
    submitPlanChange,
    purchaseSeats
  };
}
