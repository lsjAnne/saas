<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import AuditTrailPanel from '@/features/auditCenter/components/AuditTrailPanel.vue';
import ExportGovernancePanel from '@/features/auditCenter/components/ExportGovernancePanel.vue';
import { useAuditCenterOverview } from '@/features/auditCenter/composables/useAuditCenterOverview';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const auditCenter = useAuditCenterOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await auditCenter.load(authStore.token);
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="audit-page">
    <header class="audit-page__header">
      <p class="audit-page__eyebrow">合规留痕</p>
      <h2 class="audit-page__title">审计导出</h2>
      <p class="audit-page__subtitle">
        导出申请、审批与审计追踪。
      </p>
    </header>

    <InlineErrorCard
      v-if="auditCenter.errorMessage"
      :message="auditCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <div class="audit-page__grid">
      <AuditTrailPanel :audit-logs="auditCenter.auditLogs" />
      <ExportGovernancePanel :export-tasks="auditCenter.exportTasks" />
    </div>
  </section>
</template>

<style scoped>
.audit-page {
  display: grid;
  gap: 1.2rem;
}

.audit-page__header {
  display: grid;
  gap: 0.75rem;
}

.audit-page__eyebrow,
.audit-page__title,
.audit-page__subtitle {
  margin: 0;
}

.audit-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.audit-page__title {
  max-width: 18ch;
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
  line-height: 1.2;
}

.audit-page__subtitle {
  max-width: 54rem;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.audit-page__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

@media (max-width: 1080px) {
  .audit-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
