<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  AdminTenantOverview,
  DeliveryReadinessView,
  ObservabilityReadinessView,
  ReleaseReadinessView
} from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface Props {
  selectedTenant: AdminTenantOverview | null;
  releaseReadiness: ReleaseReadinessView | null;
  deliveryReadiness: DeliveryReadinessView | null;
  observabilityReadiness: ObservabilityReadinessView | null;
  isRefreshing: boolean;
}

defineProps<Props>();

function toneByBoolean(value: boolean | null | undefined) {
  return value ? 'success' : 'warn';
}

const readinessLabelMap: Record<string, string> = {
  ready: '就绪',
  passed: '通过',
  blocked: '阻塞',
  idle: '待命中',
  failed: '失败'
};

function readinessLabel(status?: string | null) {
  return (status && readinessLabelMap[status]) || '待命中';
}
</script>

<template>
  <PanelCard
    title="发布、交付与就绪度"
    description="平台治理把发布、交付和可观测性放在同一视图，降低风险切换成本。"
  >
    <div class="readiness-panel">
      <div class="readiness-panel__card">
        <div class="readiness-panel__row">
          <div>
            <p class="readiness-panel__eyebrow">发布</p>
            <h4>{{ selectedTenant?.tenantName || '等待选择租户' }}</h4>
          </div>
          <StatusPill
            :label="readinessLabel(releaseReadiness?.conclusion)"
            :tone="toneByBoolean((releaseReadiness?.blockingReasons?.length ?? 1) === 0)"
          />
        </div>
        <p class="readiness-panel__text">
          阻塞项 {{ formatCount(releaseReadiness?.blockingReasons?.length ?? 0) }}，
          清单 {{ formatCount(releaseReadiness?.checklistItems?.length ?? 0) }}。
        </p>
        <div v-if="releaseReadiness?.checklistItems?.length" class="readiness-panel__list">
          <article
            v-for="item in releaseReadiness.checklistItems.slice(0, 5)"
            :key="item.itemCode"
            class="readiness-panel__item"
          >
            <div class="readiness-panel__row">
              <strong>{{ item.itemCode }}</strong>
              <StatusPill
                :label="readinessLabel(item.status)"
                :tone="toneByBoolean(item.status === 'ready' || item.status === 'passed')"
              />
            </div>
            <p>{{ item.detail }}</p>
          </article>
        </div>
      </div>

      <div class="readiness-panel__card">
        <div class="readiness-panel__row">
          <div>
            <p class="readiness-panel__eyebrow">交付</p>
            <h4>{{ deliveryReadiness?.pipeline.repository || '等待回读' }}</h4>
          </div>
          <StatusPill
            :label="readinessLabel(deliveryReadiness?.pipeline.ready ? 'ready' : 'blocked')"
            :tone="toneByBoolean(deliveryReadiness?.pipeline.ready)"
          />
        </div>
        <p class="readiness-panel__text">
          外部集成 {{ deliveryReadiness?.externalIntegrations.reachableCount ?? 0 }} /
          {{ deliveryReadiness?.externalIntegrations.configuredCount ?? 0 }} 可达，双形态验收
          {{ deliveryReadiness?.acceptance.ready ? '已就绪' : '未就绪' }}。
        </p>
        <div class="readiness-panel__list">
          <article class="readiness-panel__item">
            <strong>标准 SaaS</strong>
            <p>{{ deliveryReadiness?.acceptance.standardSaas.detail || '--' }}</p>
          </article>
          <article class="readiness-panel__item">
            <strong>私有部署</strong>
            <p>{{ deliveryReadiness?.acceptance.privateDeployment.detail || '--' }}</p>
          </article>
        </div>
      </div>

      <div class="readiness-panel__card">
        <div class="readiness-panel__row">
          <div>
            <p class="readiness-panel__eyebrow">可观测</p>
            <h4>{{ observabilityReadiness?.tenantId || selectedTenant?.tenantId || '等待回读' }}</h4>
          </div>
          <StatusPill
            :label="readinessLabel(observabilityReadiness?.stack.ready ? 'ready' : 'blocked')"
            :tone="toneByBoolean(observabilityReadiness?.stack.ready)"
          />
        </div>
        <p class="readiness-panel__text">
          栈可达 {{ observabilityReadiness?.stack.reachableCount ?? 0 }} /
          {{ observabilityReadiness?.stack.configuredCount ?? 0 }}，trace 审计链
          {{ formatCount(observabilityReadiness?.auditTraceability.traceableAuditLogCount ?? 0) }}。
        </p>
        <div class="readiness-panel__list">
          <article
            v-for="reason in observabilityReadiness?.blockingReasons ?? []"
            :key="reason"
            class="readiness-panel__item"
          >
            <strong>阻塞原因</strong>
            <p>{{ reason }}</p>
          </article>
          <p v-if="!observabilityReadiness?.blockingReasons?.length" class="readiness-panel__empty">
            {{ isRefreshing ? '正在刷新 observability readiness...' : '当前没有额外阻塞原因。' }}
          </p>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.readiness-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.readiness-panel__card,
.readiness-panel__list {
  display: grid;
  gap: 0.8rem;
}

.readiness-panel__card {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.readiness-panel__row {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.readiness-panel__eyebrow,
.readiness-panel__card h4,
.readiness-panel__text,
.readiness-panel__item p,
.readiness-panel__empty {
  margin: 0;
}

.readiness-panel__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.readiness-panel__text {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.readiness-panel__item {
  display: grid;
  gap: 0.35rem;
  padding: 0.8rem;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

@media (max-width: 1180px) {
  .readiness-panel {
    grid-template-columns: 1fr;
  }
}
</style>
