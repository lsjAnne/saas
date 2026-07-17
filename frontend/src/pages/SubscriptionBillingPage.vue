<script setup lang="ts">
import { onMounted, shallowRef } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import BillingTimelineCard from '@/features/subscription/components/BillingTimelineCard.vue';
import SubscriptionHeroCard from '@/features/subscription/components/SubscriptionHeroCard.vue';
import { useSubscriptionOverview } from '@/features/subscription/composables/useSubscriptionOverview';
import type { InvoiceRequestPayload } from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const subscriptionOverview = useSubscriptionOverview();
const isBootstrapping = shallowRef(false);

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token || !authStore.tenantId) {
    return;
  }

  isBootstrapping.value = true;

  try {
    await subscriptionOverview.load(authStore.tenantId, authStore.token);
  } finally {
    isBootstrapping.value = false;
  }
}

async function withTenantToken(callback: (tenantId: string, token: string) => Promise<void>) {
  if (!authStore.token || !authStore.tenantId) {
    return;
  }

  await callback(authStore.tenantId, authStore.token);
}

async function handleChangePlan(planCode: string) {
  await withTenantToken((tenantId, token) =>
    subscriptionOverview.changePlan(planCode, tenantId, token)
  );
}

async function handleRenew(months: number) {
  await withTenantToken((tenantId, token) =>
    subscriptionOverview.renewSubscription(months, tenantId, token)
  );
}

async function handlePurchaseSeats(seatCount: number) {
  await withTenantToken((tenantId, token) =>
    subscriptionOverview.purchaseSeats(seatCount, tenantId, token)
  );
}

async function handleSettleOrder(billingOrderId: string) {
  await withTenantToken((tenantId, token) =>
    subscriptionOverview.settleOrder(billingOrderId, tenantId, token)
  );
}

async function handleRequestInvoice(payload: InvoiceRequestPayload) {
  await withTenantToken((tenantId, token) =>
    subscriptionOverview.requestInvoice(payload, tenantId, token)
  );
}

function handleDownloadOrder(billingOrderId: string) {
  subscriptionOverview.downloadBillingOrder(billingOrderId);
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="subscription-page">
    <header class="subscription-page__header">
      <p class="subscription-page__eyebrow">Subscription & Billing Center</p>
      <h2 class="subscription-page__title">Operate plans, seats, bills, and invoices from one place</h2>
      <p class="subscription-page__subtitle">
        This page turns the billing prototype into an executable workspace so tenant admins can upgrade plans, renew subscriptions, add seats, export bills, and submit invoice requests without leaving the page.
      </p>
    </header>

    <InlineErrorCard
      v-if="subscriptionOverview.overviewError"
      :message="subscriptionOverview.overviewError"
      @retry="bootstrapPage"
    />

    <div class="subscription-page__grid">
      <SubscriptionHeroCard
        :subscription="subscriptionOverview.subscription"
        :plans="subscriptionOverview.orderedPlans"
        :quotas="subscriptionOverview.keyQuotas"
        :current-bill="subscriptionOverview.currentBill"
        :is-loading="subscriptionOverview.isLoading"
        :is-submitting="subscriptionOverview.isRunningAction"
        @change-plan="handleChangePlan"
        @renew="handleRenew"
        @purchase-seats="handlePurchaseSeats"
      />

      <BillingTimelineCard
        :billing-orders="subscriptionOverview.billingOrders"
        :last-invoice-request="subscriptionOverview.lastInvoiceRequest"
        :is-submitting="subscriptionOverview.isRunningAction"
        @settle-order="handleSettleOrder"
        @request-invoice="handleRequestInvoice"
        @download-order="handleDownloadOrder"
      />
    </div>

    <p
      v-if="subscriptionOverview.actionFeedback"
      class="subscription-page__footer-note subscription-page__footer-note--success"
    >
      {{ subscriptionOverview.actionFeedback }}
    </p>
    <p
      v-if="subscriptionOverview.actionError"
      class="subscription-page__footer-note subscription-page__footer-note--error"
    >
      {{ subscriptionOverview.actionError }}
    </p>
    <p v-if="isBootstrapping" class="subscription-page__footer-note">
      Loading subscription, quota, bill, and payment status...
    </p>
  </section>
</template>

<style scoped>
.subscription-page {
  display: grid;
  gap: 1.25rem;
}

.subscription-page__header {
  display: grid;
  gap: 0.8rem;
  padding: 0.2rem 0.25rem;
}

.subscription-page__eyebrow {
  margin: 0;
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.subscription-page__title {
  margin: 0;
  max-width: 18ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 3.6rem);
  line-height: 1.02;
}

.subscription-page__subtitle {
  margin: 0;
  max-width: 62rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
  font-size: 1rem;
}

.subscription-page__grid {
  display: grid;
  grid-template-columns: 1.18fr 0.82fr;
  gap: 1rem;
  align-items: start;
}

.subscription-page__footer-note {
  margin: 0;
  color: var(--color-ink-soft);
}

.subscription-page__footer-note--success {
  color: var(--color-success);
}

.subscription-page__footer-note--error {
  color: var(--color-danger);
}

@media (max-width: 1080px) {
  .subscription-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
