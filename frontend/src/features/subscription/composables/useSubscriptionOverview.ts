import { computed, shallowRef } from 'vue';

import {
  downgradeTenantPlan,
  getSubscriptionPlans,
  purchaseTenantSeats,
  renewTenantSubscription,
  subscribeTenant,
  upgradeTenantPlan
} from '@/services/onboardingService';
import {
  createTenantInvoiceRequest,
  getTenantBillingOrders,
  getTenantQuotas,
  getTenantSubscription,
  settleTenantBillingOrder
} from '@/services/tenantObservabilityService';
import type {
  BillingOrder,
  InvoiceRequest,
  InvoiceRequestPayload,
  SubscriptionPlan,
  TenantSubscription,
  UsageQuota
} from '@/services/apiTypes';

const KEY_QUOTA_CODES = ['store_count', 'seat_count', 'notification_count'];

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

export function useSubscriptionOverview() {
  const plans = shallowRef<SubscriptionPlan[]>([]);
  const subscription = shallowRef<TenantSubscription | null>(null);
  const quotas = shallowRef<UsageQuota[]>([]);
  const billingOrders = shallowRef<BillingOrder[]>([]);
  const lastInvoiceRequest = shallowRef<InvoiceRequest | null>(null);
  const isLoading = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const overviewError = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const currentBill = computed(() => billingOrders.value[0] ?? null);
  const selectedPlan = computed(
    () => plans.value.find((plan) => plan.planCode === subscription.value?.planCode) ?? null
  );
  const orderedPlans = computed(() =>
    [...plans.value].sort((left, right) => {
      if (left.monthlyPrice !== right.monthlyPrice) {
        return left.monthlyPrice - right.monthlyPrice;
      }

      return left.yearlyPrice - right.yearlyPrice;
    })
  );
  const keyQuotas = computed(() =>
    quotas.value
      .filter((quota) => KEY_QUOTA_CODES.includes(quota.quotaCode))
      .slice(0, 3)
  );

  function clearActionState() {
    actionError.value = null;
    actionFeedback.value = null;
  }

  function planLevel(planCode: string | null | undefined) {
    return orderedPlans.value.findIndex((plan) => plan.planCode === planCode);
  }

  async function refreshOverview(tenantId: string, token: string) {
    const [subscriptionPayload, quotaPayload, billingPayload] = await Promise.all([
      getTenantSubscription(tenantId, token),
      getTenantQuotas(tenantId, token),
      getTenantBillingOrders(tenantId, token)
    ]);

    subscription.value = subscriptionPayload;
    quotas.value = quotaPayload;
    billingOrders.value = billingPayload;
  }

  async function load(tenantId: string, token: string) {
    isLoading.value = true;
    overviewError.value = null;
    clearActionState();

    try {
      const [planPayload] = await Promise.all([
        getSubscriptionPlans(),
        refreshOverview(tenantId, token)
      ]);
      plans.value = planPayload;
    } catch (error) {
      overviewError.value =
        error instanceof Error ? error.message : 'Failed to load subscription and billing data.';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  async function changePlan(planCode: string, tenantId: string, token: string) {
    const targetPlanCode = planCode.trim();
    if (!targetPlanCode) {
      return;
    }

    isRunningAction.value = true;
    clearActionState();

    try {
      const currentPlanCode = normalizeText(subscription.value?.planCode);
      const nextPlanCode = normalizeText(targetPlanCode);

      if (currentPlanCode === nextPlanCode) {
        actionFeedback.value = 'Current plan already matches the selected plan.';
        return;
      }

      const currentStatus = normalizeText(subscription.value?.subscriptionStatus);
      const shouldSubscribe =
        !subscription.value ||
        !currentPlanCode ||
        currentPlanCode === 'trial' ||
        currentStatus === 'trial' ||
        currentStatus === 'trialing';

      if (shouldSubscribe) {
        await subscribeTenant(
          tenantId,
          {
            planCode: targetPlanCode,
            seatCount: subscription.value?.seatCount ?? 1,
            autoRenew: subscription.value?.autoRenew ?? true
          },
          token
        );
      } else {
        const currentLevel = planLevel(subscription.value?.planCode);
        const targetLevel = planLevel(targetPlanCode);

        if (currentLevel === -1 || targetLevel === -1) {
          throw new Error('Plan level is unavailable. Refresh plans and try again.');
        }

        if (targetLevel > currentLevel) {
          await upgradeTenantPlan(tenantId, { planCode: targetPlanCode }, token);
        } else {
          await downgradeTenantPlan(tenantId, { planCode: targetPlanCode }, token);
        }
      }

      await refreshOverview(tenantId, token);
      actionFeedback.value = `Plan updated to ${subscription.value?.planName ?? targetPlanCode}.`;
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : 'Failed to change the plan.';
      throw error;
    } finally {
      isRunningAction.value = false;
    }
  }

  async function renewSubscription(months: number, tenantId: string, token: string) {
    isRunningAction.value = true;
    clearActionState();

    try {
      await renewTenantSubscription(tenantId, { months }, token);
      await refreshOverview(tenantId, token);
      actionFeedback.value = `Subscription renewed for ${months} month(s).`;
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : 'Failed to renew the subscription.';
      throw error;
    } finally {
      isRunningAction.value = false;
    }
  }

  async function purchaseSeats(seatCount: number, tenantId: string, token: string) {
    isRunningAction.value = true;
    clearActionState();

    try {
      await purchaseTenantSeats(tenantId, { seatCount }, token);
      await refreshOverview(tenantId, token);
      actionFeedback.value = `Added ${seatCount} additional seat(s).`;
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : 'Failed to purchase seats.';
      throw error;
    } finally {
      isRunningAction.value = false;
    }
  }

  async function settleOrder(billingOrderId: string, tenantId: string, token: string) {
    isRunningAction.value = true;
    clearActionState();

    try {
      await settleTenantBillingOrder(tenantId, billingOrderId, token);
      await refreshOverview(tenantId, token);
      actionFeedback.value = `Billing order ${billingOrderId} has been settled.`;
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : 'Failed to settle the billing order.';
      throw error;
    } finally {
      isRunningAction.value = false;
    }
  }

  async function requestInvoice(
    payload: InvoiceRequestPayload,
    tenantId: string,
    token: string
  ) {
    isRunningAction.value = true;
    clearActionState();

    try {
      lastInvoiceRequest.value = await createTenantInvoiceRequest(tenantId, payload, token);
      actionFeedback.value = `Invoice request ${lastInvoiceRequest.value.invoiceRequestId} submitted.`;
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : 'Failed to submit the invoice request.';
      throw error;
    } finally {
      isRunningAction.value = false;
    }
  }

  function downloadBillingOrder(billingOrderId: string) {
    const order = billingOrders.value.find((item) => item.billingOrderId === billingOrderId);
    if (!order) {
      actionError.value = 'Billing order not found.';
      return;
    }

    clearActionState();

    const content = [
      'Ark Commerce Subscription Bill',
      `Bill ID: ${order.billingOrderId}`,
      `Plan Name: ${order.planName}`,
      `Order Type: ${order.orderType}`,
      `Payment Status: ${order.paymentStatus}`,
      `Payable Amount: ${order.payableAmount}`,
      `External Order No: ${order.externalOrderNo ?? '--'}`,
      `Created At: ${order.createdAt}`,
      `Paid At: ${order.paidAt ?? '--'}`,
      `Current Subscription: ${subscription.value?.planName ?? '--'}`,
      `Current Seats: ${subscription.value?.seatCount ?? '--'}`
    ].join('\n');

    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const objectUrl = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = objectUrl;
    anchor.download = `${billingOrderId}.txt`;
    anchor.click();
    window.URL.revokeObjectURL(objectUrl);

    actionFeedback.value = `Billing order ${billingOrderId} exported.`;
  }

  return {
    plans,
    subscription,
    quotas,
    billingOrders,
    selectedPlan,
    orderedPlans,
    lastInvoiceRequest,
    currentBill,
    keyQuotas,
    isLoading,
    isRunningAction,
    overviewError,
    actionError,
    actionFeedback,
    load,
    changePlan,
    renewSubscription,
    purchaseSeats,
    settleOrder,
    requestInvoice,
    downloadBillingOrder
  };
}
