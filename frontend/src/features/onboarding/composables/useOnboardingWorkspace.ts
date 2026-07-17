import { computed, shallowRef } from 'vue';

import { login, type LoginRequest } from '@/services/authService';
import {
  downgradeTenantPlan,
  getSubscriptionPlans,
  registerTenant,
  subscribeTenant,
  upgradeTenantPlan
} from '@/services/onboardingService';
import type {
  ConnectStorePayload,
  RegisterTenantRequest,
  RegisterTenantResponse,
  Store,
  SubscriptionPlan,
  TenantContextView,
  TenantSubscription,
  UpdateStoreSettingsPayload,
  UsageQuota
} from '@/services/apiTypes';
import { connectStore, getStores, updateStoreSettings } from '@/services/storeChannelService';
import { getTenantContext } from '@/services/tenantContextService';
import { getTenantQuotas, getTenantSubscription } from '@/services/tenantObservabilityService';
import { useAuthStore } from '@/stores/authStore';

const DEFAULT_BOOTSTRAP_PASSWORD = '123456';
const PLAN_PRIORITY: Record<string, number> = {
  trial: 0,
  basic: 1,
  pro: 2
};
const KEY_QUOTA_CODES = ['store_count', 'seat_count', 'notification_count'];

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function planPriority(planCode: string | null | undefined) {
  return PLAN_PRIORITY[normalizeText(planCode)] ?? 99;
}

function sortPlans(plans: SubscriptionPlan[]) {
  return [...plans].sort(
    (left, right) => planPriority(left.planCode) - planPriority(right.planCode)
  );
}

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

export function useOnboardingWorkspace() {
  const authStore = useAuthStore();

  const plans = shallowRef<SubscriptionPlan[]>([]);
  const selectedPlanCode = shallowRef('trial');
  const registration = shallowRef<RegisterTenantResponse | null>(null);
  const context = shallowRef<TenantContextView | null>(null);
  const subscription = shallowRef<TenantSubscription | null>(null);
  const quotas = shallowRef<UsageQuota[]>([]);
  const stores = shallowRef<Store[]>([]);
  const selectedStoreId = shallowRef<string | null>(null);

  const isBootstrapping = shallowRef(false);
  const isRefreshingWorkspace = shallowRef(false);
  const isLoggingIn = shallowRef(false);
  const isRegistering = shallowRef(false);
  const isUpdatingPlan = shallowRef(false);
  const isConnectingStore = shallowRef(false);
  const isSavingSettings = shallowRef(false);

  const accessError = shallowRef<string | null>(null);
  const accessFeedback = shallowRef<string | null>(null);
  const workspaceError = shallowRef<string | null>(null);
  const workspaceFeedback = shallowRef<string | null>(null);

  const orderedPlans = computed(() => sortPlans(plans.value));
  const paidPlans = computed(() =>
    orderedPlans.value.filter((plan) => normalizeText(plan.planCode) !== 'trial')
  );
  const orderedStores = computed(() =>
    [...stores.value].sort(
      (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );
  const selectedStore = computed(
    () => stores.value.find((store) => store.storeId === selectedStoreId.value) ?? null
  );
  const keyQuotas = computed(() =>
    quotas.value.filter((quota) => KEY_QUOTA_CODES.includes(quota.quotaCode))
  );
  const defaultOrganizationId = computed(
    () =>
      context.value?.defaultOrganizationId ||
      registration.value?.defaultOrganizationId ||
      authStore.organizationId ||
      ''
  );
  const currentOperatorId = computed(() => authStore.user?.id ?? '');

  function clearAccessState() {
    accessError.value = null;
    accessFeedback.value = null;
  }

  function clearWorkspaceState() {
    workspaceError.value = null;
    workspaceFeedback.value = null;
  }

  function applySelectedStore(nextStores: Store[], preferredStoreId?: string | null) {
    const nextStoreId =
      preferredStoreId && nextStores.some((store) => store.storeId === preferredStoreId)
        ? preferredStoreId
        : nextStores[0]?.storeId ?? null;

    selectedStoreId.value = nextStoreId;
  }

  async function loadPlans() {
    const payload = await getSubscriptionPlans();
    plans.value = sortPlans(payload);

    if (!plans.value.some((plan) => plan.planCode === selectedPlanCode.value)) {
      selectedPlanCode.value = plans.value[0]?.planCode ?? 'trial';
    }
  }

  async function loadWorkspace() {
    if (!authStore.token || !authStore.tenantId) {
      return;
    }

    isRefreshingWorkspace.value = true;
    workspaceError.value = null;

    try {
      const [contextPayload, subscriptionPayload, quotaPayload, storePayload] =
        await Promise.all([
          getTenantContext(authStore.token),
          getTenantSubscription(authStore.tenantId, authStore.token),
          getTenantQuotas(authStore.tenantId, authStore.token),
          getStores(authStore.token)
        ]);

      context.value = contextPayload;
      subscription.value = subscriptionPayload;
      quotas.value = quotaPayload;
      stores.value = storePayload;
      applySelectedStore(storePayload, selectedStoreId.value);
    } catch (error) {
      workspaceError.value =
        error instanceof Error ? error.message : '加载 onboarding 工作台失败，请稍后重试';
      throw error;
    } finally {
      isRefreshingWorkspace.value = false;
    }
  }

  async function refreshStoreList(preferredStoreId?: string | null) {
    if (!authStore.token) {
      return;
    }

    const payload = await getStores(authStore.token);
    stores.value = payload;
    applySelectedStore(payload, preferredStoreId ?? selectedStoreId.value);
  }

  async function refreshSubscriptionState() {
    if (!authStore.token || !authStore.tenantId) {
      return;
    }

    const [subscriptionPayload, quotaPayload] = await Promise.all([
      getTenantSubscription(authStore.tenantId, authStore.token),
      getTenantQuotas(authStore.tenantId, authStore.token)
    ]);

    subscription.value = subscriptionPayload;
    quotas.value = quotaPayload;
  }

  async function bootstrap() {
    authStore.hydrate();
    isBootstrapping.value = true;

    try {
      await loadPlans();

      if (authStore.isAuthenticated) {
        await loadWorkspace();
      }
    } finally {
      isBootstrapping.value = false;
    }
  }

  function selectPlan(planCode: string) {
    selectedPlanCode.value = planCode;
  }

  function selectStore(storeId: string) {
    selectedStoreId.value = storeId;
  }

  async function signIn(request: LoginRequest) {
    isLoggingIn.value = true;
    clearAccessState();

    try {
      const payload = await login(request);
      authStore.persistSession(payload);
      await loadWorkspace();
      accessFeedback.value = '已登录，继续完成套餐和店铺初始化。';
    } catch (error) {
      accessError.value =
        error instanceof Error ? error.message : '登录失败，请检查账号和密码';
      throw error;
    } finally {
      isLoggingIn.value = false;
    }
  }

  async function changePlan(
    planCode: string,
    options: {
      seatCount?: number;
      autoRenew?: boolean;
      silentFeedback?: boolean;
    } = {}
  ) {
    if (!authStore.token || !authStore.tenantId) {
      return;
    }

    const targetPlanCode = planCode.trim();
    const currentPlanCode = normalizeText(subscription.value?.planCode);
    const nextPlanCode = normalizeText(targetPlanCode);

    if (!nextPlanCode) {
      return;
    }

    if (currentPlanCode === nextPlanCode) {
      if (!options.silentFeedback) {
        workspaceFeedback.value = '当前已经是该套餐，无需重复切换。';
      }
      return;
    }

    isUpdatingPlan.value = true;
    workspaceError.value = null;

    try {
      const seatCount = options.seatCount ?? subscription.value?.seatCount ?? 1;
      const autoRenew = options.autoRenew ?? true;
      const currentStatus = normalizeText(subscription.value?.subscriptionStatus);
      const shouldSubscribe =
        !currentPlanCode || currentPlanCode === 'trial' || currentStatus === 'trialing';

      if (shouldSubscribe) {
        await subscribeTenant(
          authStore.tenantId,
          {
            planCode: targetPlanCode,
            seatCount,
            autoRenew
          },
          authStore.token
        );
      } else if (planPriority(targetPlanCode) > planPriority(subscription.value?.planCode)) {
        await upgradeTenantPlan(
          authStore.tenantId,
          { planCode: targetPlanCode },
          authStore.token
        );
      } else {
        await downgradeTenantPlan(
          authStore.tenantId,
          { planCode: targetPlanCode },
          authStore.token
        );
      }

      await refreshSubscriptionState();

      if (!options.silentFeedback) {
        workspaceFeedback.value = `套餐已切换为 ${subscription.value?.planName ?? targetPlanCode}。`;
      }
    } catch (error) {
      workspaceError.value =
        error instanceof Error ? error.message : '套餐切换失败，请稍后重试';
      throw error;
    } finally {
      isUpdatingPlan.value = false;
    }
  }

  async function registerAndStart(request: RegisterTenantRequest) {
    isRegistering.value = true;
    clearAccessState();

    try {
      const payload = await registerTenant(request);
      registration.value = payload;

      const session = await login({
        username: request.mobile,
        password: DEFAULT_BOOTSTRAP_PASSWORD
      });

      authStore.persistSession(session);
      await loadWorkspace();

      if (normalizeText(selectedPlanCode.value) !== 'trial') {
        await changePlan(selectedPlanCode.value, {
          seatCount: 1,
          autoRenew: true,
          silentFeedback: true
        });
      }

      accessFeedback.value = `已开通 ${payload.tenantName}，默认组织 ${payload.defaultOrganizationId} 已建立，初始密码 ${DEFAULT_BOOTSTRAP_PASSWORD}。`;
    } catch (error) {
      accessError.value =
        error instanceof Error ? error.message : '试用开通失败，请稍后重试';
      throw error;
    } finally {
      isRegistering.value = false;
    }
  }

  async function submitStoreConnection(payload: ConnectStorePayload) {
    if (!authStore.token) {
      return;
    }

    isConnectingStore.value = true;
    clearWorkspaceState();

    try {
      const store = await connectStore(payload, authStore.token);
      await refreshStoreList(store.storeId);
      workspaceFeedback.value = `店铺 ${store.shopName} 已接入，下一步请校准阈值与发货配置。`;
    } catch (error) {
      workspaceError.value =
        error instanceof Error ? error.message : '店铺接入失败，请稍后重试';
      throw error;
    } finally {
      isConnectingStore.value = false;
    }
  }

  async function submitStoreSettings(payload: UpdateStoreSettingsPayload) {
    if (!authStore.token || !selectedStore.value) {
      return;
    }

    isSavingSettings.value = true;
    clearWorkspaceState();

    try {
      await updateStoreSettings(selectedStore.value.storeId, payload, authStore.token);
      await refreshStoreList(selectedStore.value.storeId);
      workspaceFeedback.value = `已保存 ${selectedStore.value.shopName} 的初始化阈值与默认发货配置。`;
    } catch (error) {
      workspaceError.value =
        error instanceof Error ? error.message : '初始化设置保存失败，请稍后重试';
      throw error;
    } finally {
      isSavingSettings.value = false;
    }
  }

  function signOut() {
    authStore.clearSession();
    context.value = null;
    subscription.value = null;
    quotas.value = [];
    stores.value = [];
    selectedStoreId.value = null;
    clearWorkspaceState();
    accessFeedback.value = '已退出当前租户会话。';
  }

  return {
    plans: orderedPlans,
    paidPlans,
    selectedPlanCode,
    registration,
    context,
    subscription,
    quotas: keyQuotas,
    stores: orderedStores,
    selectedStoreId,
    selectedStore,
    defaultOrganizationId,
    currentOperatorId,
    defaultBootstrapPassword: DEFAULT_BOOTSTRAP_PASSWORD,
    isBootstrapping,
    isAuthenticated: computed(() => authStore.isAuthenticated),
    isRefreshingWorkspace,
    isLoggingIn,
    isRegistering,
    isUpdatingPlan,
    isConnectingStore,
    isSavingSettings,
    accessError,
    accessFeedback,
    workspaceError,
    workspaceFeedback,
    bootstrap,
    loadWorkspace,
    selectPlan,
    selectStore,
    signIn,
    registerAndStart,
    changePlan,
    submitStoreConnection,
    submitStoreSettings,
    signOut
  };
}
