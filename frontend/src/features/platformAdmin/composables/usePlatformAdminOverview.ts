import { computed, reactive, shallowRef } from 'vue';

import { getSubscriptionPlans } from '@/services/onboardingService';
import {
  getAdminComplianceAcceptances,
  getAdminComplianceDocuments,
  getAdminTenants,
  getDeliveryReadiness,
  getObservabilityReadiness,
  getReleaseReadiness,
  publishAdminComplianceDocument,
  reconcileSubscriptionAutomation,
  resumeAdminTenant,
  suspendAdminTenant,
  updateAdminTenantFeatureToggles
} from '@/services/platformAdminService';
import type {
  AdminComplianceAcceptanceView,
  AdminComplianceDocumentView,
  AdminTenantOverview,
  ComplianceDocumentPublishPayload,
  DeliveryReadinessView,
  FeatureToggleUpdatePayload,
  ObservabilityReadinessView,
  ReleaseReadinessView,
  SubscriptionAutomationSummary,
  SubscriptionPlan,
  TenantProfile
} from '@/services/apiTypes';

type StatusTone = 'neutral' | 'warn' | 'success';

interface GrowthTrendPoint {
  label: string;
  newTenantCount: number;
  payingTenantCount: number;
}

interface TrialConversionSummary {
  trialTenantCount: number;
  convertedTenantCount: number;
  expiringSoonCount: number;
  conversionRate: number;
}

interface ChurnWarningItem {
  code: string;
  label: string;
  count: number;
  detail: string;
  tone: StatusTone;
}

interface CustomerSuccessFollowUpItem {
  tenantId: string;
  tenantName: string;
  planName: string;
  reason: string;
  nextAction: string;
  tone: StatusTone;
  quotaUsageRate: number;
  seatCount: number;
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function isSuspendedStatus(status: string | null | undefined) {
  return normalizeText(status).includes('suspend');
}

function isOverdueTenant(tenant: AdminTenantOverview) {
  return (
    normalizeText(tenant.tenantStatus).includes('overdue') ||
    normalizeText(tenant.subscriptionStatus).includes('past_due')
  );
}

function isTrialTenant(tenant: AdminTenantOverview) {
  return (
    !tenant.subscriptionId ||
    normalizeText(tenant.planCode) === 'trial' ||
    normalizeText(tenant.subscriptionStatus) === 'trial' ||
    normalizeText(tenant.tenantStatus) === 'trial'
  );
}

function isExpiringSoon(value: string | null | undefined, days = 7) {
  if (!value) {
    return false;
  }

  const diff = toTimestamp(value) - Date.now();
  return diff >= 0 && diff <= days * 24 * 60 * 60 * 1000;
}

function createdWithinDays(value: string | null | undefined, days: number) {
  if (!value) {
    return false;
  }

  const diff = Date.now() - toTimestamp(value);
  return diff >= 0 && diff <= days * 24 * 60 * 60 * 1000;
}

function quotaUsageRate(tenant: AdminTenantOverview) {
  return tenant.totalUsedAmount / Math.max(tenant.totalQuotaLimit, 1);
}

function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function monthKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

function monthLabel(date: Date) {
  return date.toLocaleString('en-US', { month: 'short' });
}

export const platformFeatureTogglePresets = [
  {
    key: 'billing_auto_charge_authorized',
    label: 'Auto charge',
    description: 'Allow subscription automation to charge renewals automatically.',
    defaultValue: true
  },
  {
    key: 'external_integration_erp_required',
    label: 'ERP required',
    description: 'Treat ERP integration as a mandatory delivery gate.',
    defaultValue: true
  },
  {
    key: 'external_integration_wms_required',
    label: 'WMS required',
    description: 'Treat WMS readiness as a mandatory delivery gate.',
    defaultValue: true
  },
  {
    key: 'external_integration_tax_required',
    label: 'Tax required',
    description: 'Treat tax and invoice connectivity as a mandatory gate.',
    defaultValue: true
  },
  {
    key: 'external_integration_messaging_required',
    label: 'Messaging required',
    description: 'Require messaging gateway and callback worker readiness.',
    defaultValue: true
  },
  {
    key: 'external_integration_bi_required',
    label: 'BI required',
    description: 'Require BI and reporting connectivity before release.',
    defaultValue: true
  },
  {
    key: 'external_integration_routing_required',
    label: 'Routing required',
    description: 'Require external routing integration as a delivery gate.',
    defaultValue: true
  }
] as const;

export function usePlatformAdminOverview() {
  const tenants = shallowRef<AdminTenantOverview[]>([]);
  const plans = shallowRef<SubscriptionPlan[]>([]);
  const tenantProfilesById = shallowRef<Record<string, TenantProfile>>({});
  const complianceDocuments = shallowRef<AdminComplianceDocumentView[]>([]);
  const complianceAcceptances = shallowRef<AdminComplianceAcceptanceView[]>([]);
  const selectedTenantId = shallowRef<string | null>(null);
  const releaseReadiness = shallowRef<ReleaseReadinessView | null>(null);
  const deliveryReadiness = shallowRef<DeliveryReadinessView | null>(null);
  const observabilityReadiness = shallowRef<ObservabilityReadinessView | null>(null);
  const subscriptionAutomationSummary = shallowRef<SubscriptionAutomationSummary | null>(null);
  const acceptanceFilters = reactive({
    tenantId: '',
    documentCode: '',
    acceptanceStatus: ''
  });
  const isLoading = shallowRef(false);
  const isRefreshingReadiness = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const sortedTenants = computed(() =>
    [...tenants.value].sort((left, right) => {
      if (isOverdueTenant(left) !== isOverdueTenant(right)) {
        return Number(isOverdueTenant(right)) - Number(isOverdueTenant(left));
      }

      if (isExpiringSoon(left.trialEndAt) !== isExpiringSoon(right.trialEndAt)) {
        return Number(isExpiringSoon(right.trialEndAt)) - Number(isExpiringSoon(left.trialEndAt));
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const selectedTenant = computed(
    () => sortedTenants.value.find((item) => item.tenantId === selectedTenantId.value) ?? null
  );

  const selectedTenantProfile = computed(() =>
    selectedTenantId.value ? tenantProfilesById.value[selectedTenantId.value] ?? null : null
  );

  const resolvedFeatureFlags = computed<Record<string, boolean>>(() => {
    const base = Object.fromEntries(
      platformFeatureTogglePresets.map((item) => [item.key, item.defaultValue])
    ) as Record<string, boolean>;

    return {
      ...base,
      ...(selectedTenantProfile.value?.featureFlags ?? {})
    };
  });

  const planPriceMap = computed(
    () => new Map(plans.value.map((plan) => [plan.planCode, plan]))
  );

  const summary = computed(() => {
    const payingTenants = tenants.value.filter(
      (tenant) => tenant.subscriptionId && normalizeText(tenant.planCode) !== 'trial'
    );
    const estimatedMonthlyRevenue = payingTenants.reduce((sum, tenant) => {
      const plan = tenant.planCode ? planPriceMap.value.get(tenant.planCode) : undefined;
      return sum + (plan?.monthlyPrice ?? 0);
    }, 0);

    return {
      totalTenantCount: tenants.value.length,
      activeTenantCount: tenants.value.filter((tenant) => normalizeText(tenant.tenantStatus) === 'active').length,
      payingTenantCount: payingTenants.length,
      trialTenantCount: tenants.value.filter(isTrialTenant).length,
      suspendedTenantCount: tenants.value.filter((tenant) => isSuspendedStatus(tenant.tenantStatus)).length,
      overdueTenantCount: tenants.value.filter(isOverdueTenant).length,
      newTenantCount30d: tenants.value.filter((tenant) => createdWithinDays(tenant.createdAt, 30)).length,
      estimatedMonthlyRevenue,
      atRiskTenantCount: tenants.value.filter(
        (tenant) =>
          isOverdueTenant(tenant) ||
          isSuspendedStatus(tenant.tenantStatus) ||
          isExpiringSoon(tenant.trialEndAt) ||
          quotaUsageRate(tenant) >= 0.85
      ).length,
      totalSeatCount: tenants.value.reduce((sum, tenant) => sum + tenant.seatCount, 0),
      quotaUsageRate:
        tenants.value.reduce((sum, tenant) => sum + tenant.totalUsedAmount, 0) /
        Math.max(tenants.value.reduce((sum, tenant) => sum + tenant.totalQuotaLimit, 0), 1)
    };
  });

  const growthTrend = computed<GrowthTrendPoint[]>(() => {
    const now = new Date();

    return Array.from({ length: 6 }, (_, offset) => {
      const monthDate = new Date(now.getFullYear(), now.getMonth() - (5 - offset), 1);
      const currentKey = monthKey(monthDate);
      const monthTenants = tenants.value.filter((tenant) => {
        if (!tenant.createdAt) {
          return false;
        }

        return monthKey(startOfMonth(new Date(tenant.createdAt))) === currentKey;
      });

      return {
        label: monthLabel(monthDate),
        newTenantCount: monthTenants.length,
        payingTenantCount: monthTenants.filter(
          (tenant) => tenant.subscriptionId && normalizeText(tenant.planCode) !== 'trial'
        ).length
      };
    });
  });

  const trialConversion = computed<TrialConversionSummary>(() => {
    const trialTenantCount = tenants.value.filter(isTrialTenant).length;
    const convertedTenantCount = tenants.value.filter(
      (tenant) =>
        tenant.subscriptionId &&
        normalizeText(tenant.planCode) !== 'trial' &&
        normalizeText(tenant.subscriptionStatus) === 'active'
    ).length;
    const expiringSoonCount = tenants.value.filter(
      (tenant) => isTrialTenant(tenant) && isExpiringSoon(tenant.trialEndAt)
    ).length;
    const conversionRate =
      convertedTenantCount / Math.max(convertedTenantCount + trialTenantCount, 1);

    return {
      trialTenantCount,
      convertedTenantCount,
      expiringSoonCount,
      conversionRate
    };
  });

  const churnWarnings = computed<ChurnWarningItem[]>(() => {
    const warnings: ChurnWarningItem[] = [
      {
        code: 'payment-overdue',
        label: 'Payment overdue',
        count: tenants.value.filter(isOverdueTenant).length,
        detail: 'Tenants in overdue or past-due states need payment recovery.',
        tone: 'warn'
      },
      {
        code: 'trial-expiring',
        label: 'Trial ending soon',
        count: tenants.value.filter((tenant) => isTrialTenant(tenant) && isExpiringSoon(tenant.trialEndAt)).length,
        detail: 'Trials ending within 7 days need a conversion follow-up.',
        tone: 'warn'
      },
      {
        code: 'suspended',
        label: 'Suspended tenants',
        count: tenants.value.filter((tenant) => isSuspendedStatus(tenant.tenantStatus)).length,
        detail: 'Suspended tenants need a recovery or offboarding decision.',
        tone: 'warn'
      },
      {
        code: 'capacity-hot',
        label: 'Capacity hot spots',
        count: tenants.value.filter((tenant) => quotaUsageRate(tenant) >= 0.85).length,
        detail: 'Quota usage over 85% is a likely upsell or risk signal.',
        tone: 'neutral'
      }
    ];

    if (subscriptionAutomationSummary.value?.autoChargeFailedCount) {
      warnings.unshift({
        code: 'auto-charge-failed',
        label: 'Auto charge failed',
        count: subscriptionAutomationSummary.value.autoChargeFailedCount,
        detail: 'Latest automation run found tenants with failed renewal charges.',
        tone: 'warn'
      });
    }

    return warnings.filter((item) => item.count > 0);
  });

  const customerSuccessFollowUps = computed<CustomerSuccessFollowUpItem[]>(() =>
    tenants.value
      .map((tenant) => {
        const usageRate = quotaUsageRate(tenant);
        let score = 0;
        let reason = '';
        let nextAction = '';
        let tone: StatusTone = 'neutral';

        if (isOverdueTenant(tenant)) {
          score = 100;
          reason = 'Payment is overdue.';
          nextAction = 'Recover payment and confirm renewal status.';
          tone = 'warn';
        } else if (isSuspendedStatus(tenant.tenantStatus)) {
          score = 92;
          reason = 'Tenant is suspended.';
          nextAction = 'Confirm whether to resume service or close the account.';
          tone = 'warn';
        } else if (isTrialTenant(tenant) && isExpiringSoon(tenant.trialEndAt)) {
          score = 84;
          reason = 'Trial is expiring within 7 days.';
          nextAction = 'Run conversion outreach and confirm package fit.';
          tone = 'warn';
        } else if (usageRate >= 0.85) {
          score = 76;
          reason = 'Quota usage is above 85%.';
          nextAction = 'Discuss seat or package expansion.';
          tone = 'neutral';
        } else if (tenant.subscriptionId && normalizeText(tenant.planCode) !== 'trial' && createdWithinDays(tenant.createdAt, 30)) {
          score = 62;
          reason = 'New paying tenant is still in the onboarding window.';
          nextAction = 'Validate onboarding completion and adoption.';
          tone = 'success';
        }

        return {
          tenantId: tenant.tenantId,
          tenantName: tenant.tenantName,
          planName: tenant.planName ?? '--',
          reason,
          nextAction,
          tone,
          quotaUsageRate: usageRate,
          seatCount: tenant.seatCount,
          score
        };
      })
      .filter((item) => item.score > 0)
      .sort((left, right) => right.score - left.score)
      .slice(0, 6)
      .map(({ score: _score, ...item }) => item)
  );

  function applySelection() {
    selectedTenantId.value =
      sortedTenants.value.find((item) => item.tenantId === selectedTenantId.value)?.tenantId ??
      sortedTenants.value[0]?.tenantId ??
      null;
  }

  async function refreshTenants(token: string) {
    tenants.value = await getAdminTenants(token);
    applySelection();
  }

  async function refreshComplianceAcceptances(token: string) {
    complianceAcceptances.value = await getAdminComplianceAcceptances(
      {
        tenantId: acceptanceFilters.tenantId || undefined,
        documentCode: acceptanceFilters.documentCode || undefined,
        acceptanceStatus: acceptanceFilters.acceptanceStatus || undefined
      },
      token
    );
  }

  async function refreshReadiness(token: string) {
    if (!selectedTenantId.value) {
      releaseReadiness.value = null;
      deliveryReadiness.value = null;
      observabilityReadiness.value = null;
      return;
    }

    isRefreshingReadiness.value = true;

    try {
      const [releasePayload, deliveryPayload, observabilityPayload] = await Promise.all([
        getReleaseReadiness(selectedTenantId.value, token),
        getDeliveryReadiness(selectedTenantId.value, token),
        getObservabilityReadiness(selectedTenantId.value, token)
      ]);

      releaseReadiness.value = releasePayload;
      deliveryReadiness.value = deliveryPayload;
      observabilityReadiness.value = observabilityPayload;
    } finally {
      isRefreshingReadiness.value = false;
    }
  }

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [tenantsPayload, documentsPayload, plansPayload] = await Promise.all([
        getAdminTenants(token),
        getAdminComplianceDocuments(token),
        getSubscriptionPlans()
      ]);

      tenants.value = tenantsPayload;
      complianceDocuments.value = documentsPayload;
      plans.value = plansPayload;
      applySelection();
      acceptanceFilters.tenantId = selectedTenantId.value ?? '';
      await Promise.all([refreshComplianceAcceptances(token), refreshReadiness(token)]);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : 'Failed to load platform operations workspace.';
    } finally {
      isLoading.value = false;
    }
  }

  async function selectTenant(tenantId: string, token: string) {
    selectedTenantId.value = tenantId;
    acceptanceFilters.tenantId = tenantId;
    await Promise.all([refreshComplianceAcceptances(token), refreshReadiness(token)]);
  }

  async function updateAcceptanceFilters(
    filters: {
      tenantId?: string;
      documentCode?: string;
      acceptanceStatus?: string;
    },
    token: string
  ) {
    acceptanceFilters.tenantId = filters.tenantId ?? '';
    acceptanceFilters.documentCode = filters.documentCode ?? '';
    acceptanceFilters.acceptanceStatus = filters.acceptanceStatus ?? '';
    await refreshComplianceAcceptances(token);
  }

  async function runAction(callback: () => Promise<void>) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      await callback();
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : 'Failed to complete the platform action.';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitFeatureToggles(payload: FeatureToggleUpdatePayload, token: string) {
    if (!selectedTenantId.value) {
      return;
    }

    await runAction(async () => {
      const profile = await updateAdminTenantFeatureToggles(selectedTenantId.value!, payload, token);
      tenantProfilesById.value = {
        ...tenantProfilesById.value,
        [profile.tenantId]: profile
      };
      actionFeedback.value = `Feature flags updated for ${profile.tenantName}.`;
      await refreshReadiness(token);
    });
  }

  async function submitSuspendTenant(token: string) {
    if (!selectedTenantId.value) {
      return;
    }

    await runAction(async () => {
      const profile = await suspendAdminTenant(selectedTenantId.value!, token);
      tenantProfilesById.value = {
        ...tenantProfilesById.value,
        [profile.tenantId]: profile
      };
      actionFeedback.value = `${profile.tenantName} has been suspended.`;
      await Promise.all([refreshTenants(token), refreshReadiness(token), refreshComplianceAcceptances(token)]);
    });
  }

  async function submitResumeTenant(token: string) {
    if (!selectedTenantId.value) {
      return;
    }

    await runAction(async () => {
      const profile = await resumeAdminTenant(selectedTenantId.value!, token);
      tenantProfilesById.value = {
        ...tenantProfilesById.value,
        [profile.tenantId]: profile
      };
      actionFeedback.value = `${profile.tenantName} has been resumed.`;
      await Promise.all([refreshTenants(token), refreshReadiness(token), refreshComplianceAcceptances(token)]);
    });
  }

  async function submitSubscriptionAutomation(token: string) {
    await runAction(async () => {
      subscriptionAutomationSummary.value = await reconcileSubscriptionAutomation(token);
      actionFeedback.value = 'Subscription automation reconciliation completed.';
      await refreshTenants(token);
    });
  }

  async function submitPublishComplianceDocument(
    documentCode: string,
    payload: ComplianceDocumentPublishPayload,
    token: string
  ) {
    await runAction(async () => {
      const published = await publishAdminComplianceDocument(documentCode, payload, token);
      actionFeedback.value = `Published compliance document ${published.documentCode} v${published.version}.`;
      complianceDocuments.value = await getAdminComplianceDocuments(token);
      await refreshComplianceAcceptances(token);
    });
  }

  return {
    tenants: sortedTenants,
    plans,
    selectedTenantId,
    selectedTenant,
    selectedTenantProfile,
    resolvedFeatureFlags,
    releaseReadiness,
    deliveryReadiness,
    observabilityReadiness,
    complianceDocuments,
    complianceAcceptances,
    subscriptionAutomationSummary,
    acceptanceFilters,
    summary,
    growthTrend,
    trialConversion,
    churnWarnings,
    customerSuccessFollowUps,
    isLoading,
    isRefreshingReadiness,
    isRunningAction,
    errorMessage,
    actionError,
    actionFeedback,
    load,
    selectTenant,
    updateAcceptanceFilters,
    submitFeatureToggles,
    submitSuspendTenant,
    submitResumeTenant,
    submitSubscriptionAutomation,
    submitPublishComplianceDocument
  };
}
