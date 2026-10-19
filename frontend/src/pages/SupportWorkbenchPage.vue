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
      <p class="support-workbench-page__eyebrow">服务台</p>
      <h2 class="support-workbench-page__title">支持工单</h2>
      <p class="support-workbench-page__subtitle">
        工单 SLA、队列与通知运维。
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
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.support-workbench-page__title {
  max-width: 18ch;
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
  line-height: 1.2;
}

.support-workbench-page__subtitle {
  max-width: 52rem;
  color: var(--color-ink-soft);
  line-height: 1.6;
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
