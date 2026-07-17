<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  DashboardInventoryWarning
} from '@/features/dashboard/composables/useDashboardOverview';
import type { DashboardRisk } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  risks: DashboardRisk[];
  inventoryWarnings: DashboardInventoryWarning[];
}

defineProps<Props>();

function toneFromLevel(level: string) {
  const normalized = level.trim().toLowerCase();
  if (normalized === 'high' || normalized === 'critical') {
    return 'warn';
  }

  if (normalized === 'low') {
    return 'success';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Risk & Inventory"
    title="风险提醒区与库存预警区"
    description="风险项要高亮解释，库存预警要直接指出触线 SKU，避免首页只剩概念层总览。"
  >
    <div class="risk-panel">
      <section class="risk-panel__section">
        <div class="risk-panel__section-head">
          <h4 class="risk-panel__section-title">风险提醒</h4>
          <p class="risk-panel__section-copy">先看为什么要优先处理，再决定进入哪个执行页。</p>
        </div>

        <article v-for="risk in risks" :key="risk.riskCode" class="risk-panel__item">
          <div class="risk-panel__header">
            <div>
              <h5 class="risk-panel__title">{{ risk.riskName }}</h5>
              <p class="risk-panel__detail">{{ risk.suggestion }}</p>
            </div>
            <StatusPill :label="`${risk.riskCount} 项`" :tone="toneFromLevel(risk.riskLevel)" />
          </div>
        </article>

        <p v-if="risks.length === 0" class="risk-panel__empty">
          当前没有显著风险提醒，经营首页可以更多承担日常执行入口。
        </p>
      </section>

      <section class="risk-panel__section">
        <div class="risk-panel__section-head">
          <h4 class="risk-panel__section-title">库存预警</h4>
          <p class="risk-panel__section-copy">低库存会直接压缩活动、履约和直播节奏，这里只展示最先要补的 SKU。</p>
        </div>

        <article
          v-for="warning in inventoryWarnings"
          :key="warning.id"
          class="risk-panel__item"
        >
          <div class="risk-panel__header">
            <div>
              <h5 class="risk-panel__title">{{ warning.productId }} / {{ warning.skuId }}</h5>
              <p class="risk-panel__detail">
                可用库存 {{ warning.availableStock }}，安全库存 {{ warning.safetyStock }}，缺口
                {{ warning.gap }}。
              </p>
              <p class="risk-panel__meta">快照时间 {{ formatDateTime(warning.snapshotAt) }}</p>
            </div>
            <StatusPill
              :label="warning.hasPendingApproval ? '待审批补货' : '需补货'"
              :tone="warning.hasPendingApproval ? 'neutral' : 'warn'"
            />
          </div>
        </article>

        <p v-if="inventoryWarnings.length === 0" class="risk-panel__empty">
          当前没有 SKU 压到安全库存线下方。
        </p>
      </section>
    </div>
  </PanelCard>
</template>

<style scoped>
.risk-panel,
.risk-panel__section {
  display: grid;
  gap: 0.9rem;
}

.risk-panel__section + .risk-panel__section {
  padding-top: 0.3rem;
  border-top: 1px solid rgba(91, 72, 54, 0.08);
}

.risk-panel__section-title,
.risk-panel__section-copy,
.risk-panel__title,
.risk-panel__detail,
.risk-panel__meta,
.risk-panel__empty {
  margin: 0;
}

.risk-panel__section-title,
.risk-panel__title {
  color: var(--color-ink-strong);
}

.risk-panel__section-copy,
.risk-panel__detail,
.risk-panel__meta,
.risk-panel__empty {
  color: var(--color-ink-soft);
  line-height: 1.65;
}

.risk-panel__meta {
  margin-top: 0.35rem;
}

.risk-panel__item {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.risk-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: flex-start;
}

@media (max-width: 760px) {
  .risk-panel__header {
    flex-direction: column;
  }
}
</style>
