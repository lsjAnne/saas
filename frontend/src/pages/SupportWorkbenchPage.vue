<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import NotificationOpsPanel from '@/features/supportWorkbench/components/NotificationOpsPanel.vue';
import SupportSlaPanel from '@/features/supportWorkbench/components/SupportSlaPanel.vue';
import TicketQueuePanel from '@/features/supportWorkbench/components/TicketQueuePanel.vue';
import { useSupportWorkbenchOverview } from '@/features/supportWorkbench/composables/useSupportWorkbenchOverview';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const supportWorkbench = useSupportWorkbenchOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await supportWorkbench.load(authStore.token);
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="support-workbench-page">
    <header class="support-workbench-page__header">
      <p class="support-workbench-page__eyebrow">Support & Tickets</p>
      <h2 class="support-workbench-page__title">把工单压力、支持会话和通知送达问题收进一个处置工作台</h2>
      <p class="support-workbench-page__subtitle">
        支持与工单页强调“先知道压力在哪，再决定要不要升级支持介入”。
      </p>
    </header>

    <InlineErrorCard
      v-if="supportWorkbench.errorMessage"
      :message="supportWorkbench.errorMessage"
      @retry="bootstrapPage"
    />

    <SupportSlaPanel
      :ticket-sla-overview="supportWorkbench.ticketSlaOverview"
      :support-sessions="supportWorkbench.supportSessions"
    />

    <div class="support-workbench-page__grid">
      <TicketQueuePanel
        :tickets="supportWorkbench.tickets"
        :support-sessions="supportWorkbench.supportSessions"
      />
      <NotificationOpsPanel
        :notifications="supportWorkbench.notifications"
        :gateway-overview="supportWorkbench.gatewayOverview"
      />
    </div>
  </section>
</template>

<style scoped>
.support-workbench-page {
  display: grid;
  gap: 1.2rem;
}

.support-workbench-page__header {
  display: grid;
  gap: 0.75rem;
}

.support-workbench-page__eyebrow,
.support-workbench-page__title,
.support-workbench-page__subtitle {
  margin: 0;
}

.support-workbench-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.support-workbench-page__title {
  max-width: 18ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.1rem, 3.8vw, 3.6rem);
  line-height: 1.05;
}

.support-workbench-page__subtitle {
  max-width: 52rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.support-workbench-page__grid {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 1rem;
}

@media (max-width: 1080px) {
  .support-workbench-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
