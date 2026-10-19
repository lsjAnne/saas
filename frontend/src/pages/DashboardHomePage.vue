<script setup lang="ts">
import { computed, onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import DashboardHeroCard from '@/features/dashboard/components/DashboardHeroCard.vue';
import DashboardInsightPanel from '@/features/dashboard/components/DashboardInsightPanel.vue';
import DashboardRiskPanel from '@/features/dashboard/components/DashboardRiskPanel.vue';
import DashboardTrendPanel from '@/features/dashboard/components/DashboardTrendPanel.vue';
import type {
  DashboardActionItem,
  DashboardPendingItem,
  DashboardQuickEntry
} from '@/features/dashboard/composables/useDashboardOverview';
import { useDashboardOverview } from '@/features/dashboard/composables/useDashboardOverview';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const dashboardOverview = useDashboardOverview();

authStore.hydrate();

function canAccessItem(item: {
  requiredPermissions: string[];
  permissionMode?: 'any' | 'all';
}) {
  return item.permissionMode === 'any'
    ? authStore.hasAnyPermission(item.requiredPermissions)
    : authStore.hasAllPermissions(item.requiredPermissions);
}

const visiblePendingItems = computed<DashboardPendingItem[]>(() =>
  dashboardOverview.pendingItems.filter(canAccessItem)
);

const visibleSuggestedActions = computed<DashboardActionItem[]>(() =>
  dashboardOverview.suggestedActions.filter(canAccessItem)
);

const visibleQuickEntries = computed<DashboardQuickEntry[]>(() =>
  dashboardOverview.quickEntries.filter(canAccessItem)
);

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  try {
    await dashboardOverview.load(authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="dashboard-page">
    <header class="dashboard-page__header">
      <p class="dashboard-page__eyebrow">经营总览</p>
      <h2 class="dashboard-page__title">经营首页</h2>
      <p class="dashboard-page__subtitle">
        今日经营结果、待处理事项与关键风险一屏掌握。
      </p>
    </header>

    <InlineErrorCard
      v-if="dashboardOverview.errorMessage"
      :message="dashboardOverview.errorMessage"
      @retry="bootstrapPage"
    />

    <DashboardHeroCard :summary="dashboardOverview.summary" />

    <div class="dashboard-page__grid">
      <DashboardTrendPanel :pending-items="visiblePendingItems" />
      <DashboardRiskPanel
        :risks="dashboardOverview.risks"
        :inventory-warnings="dashboardOverview.inventoryWarnings"
      />
    </div>

    <DashboardInsightPanel
      :suggested-actions="visibleSuggestedActions"
      :quick-entries="visibleQuickEntries"
    />
  </section>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 1rem;
}

.dashboard-page__header {
  display: grid;
  gap: 0.35rem;
}

.dashboard-page__eyebrow,
.dashboard-page__title,
.dashboard-page__subtitle {
  margin: 0;
}

.dashboard-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.dashboard-page__title {
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
}

.dashboard-page__subtitle {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.dashboard-page__grid {
  display: grid;
  grid-template-columns: 1.02fr 0.98fr;
  gap: 1rem;
}

@media (max-width: 1080px) {
  .dashboard-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
