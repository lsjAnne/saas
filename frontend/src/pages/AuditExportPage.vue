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
      <p class="audit-page__eyebrow">Audit & Exports</p>
      <h2 class="audit-page__title">把审计轨迹和导出治理收成一个可信留痕中心</h2>
      <p class="audit-page__subtitle">
        这页服务于合规与追溯，不强调操作入口，而强调链路、范围和结果状态。
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
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.audit-page__title {
  max-width: 18ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.1rem, 3.8vw, 3.6rem);
  line-height: 1.05;
}

.audit-page__subtitle {
  max-width: 54rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
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
