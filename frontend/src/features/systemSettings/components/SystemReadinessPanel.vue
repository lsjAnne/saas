<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ObservabilityOverview } from '@/services/apiTypes';

interface Props {
  overview: ObservabilityOverview | null;
}

const props = defineProps<Props>();

const readinessRows = computed(() => {
  if (!props.overview) {
    return [];
  }

  return [
    {
      label: '可观测栈',
      ready: props.overview.observabilityStackReady,
      detail: `${props.overview.enabledGatewayCount}/${props.overview.configuredGatewayCount} 网关启用`
    },
    {
      label: '交付流水线',
      ready: props.overview.deliveryPipelineReady,
      detail: props.overview.deliveryRepository || '尚未声明交付仓库'
    },
    {
      label: '双交付验收',
      ready: props.overview.dualDeliveryAcceptanceReady,
      detail: `${props.overview.readyExternalSystemCount}/${props.overview.requiredExternalSystemCount} 外部系统已就绪`
    }
  ];
});

const stackRows = computed(() => {
  if (!props.overview) {
    return [];
  }

  return [
    { label: '日志聚合', endpoint: props.overview.observabilityStack.logAggregation },
    { label: '链路追踪', endpoint: props.overview.observabilityStack.trace },
    { label: '告警路由', endpoint: props.overview.observabilityStack.alertRouter },
    { label: '看板', endpoint: props.overview.observabilityStack.dashboard }
  ];
});
</script>

<template>
  <PanelCard
    eyebrow="Readiness"
    title="系统设置与交付门禁摘要"
    description="系统设置页把 readiness 讲清楚：哪些是已配置、哪些是可联通、哪些只是已被纳入交付范围。"
  >
    <div class="readiness-panel">
      <div class="readiness-panel__summary">
        <article v-for="row in readinessRows" :key="row.label" class="readiness-panel__summary-item">
          <div class="readiness-panel__summary-head">
            <h4 class="readiness-panel__summary-title">{{ row.label }}</h4>
            <StatusPill :label="row.ready ? '已就绪' : '待处理'" :tone="row.ready ? 'success' : 'warn'" />
          </div>
          <p class="readiness-panel__summary-detail">{{ row.detail }}</p>
        </article>
      </div>

      <div class="readiness-panel__stack">
        <article v-for="row in stackRows" :key="row.label" class="readiness-panel__stack-item">
          <div class="readiness-panel__summary-head">
            <h4 class="readiness-panel__summary-title">{{ row.label }}</h4>
            <StatusPill
              :label="row.endpoint.probeReachable ? '可联通' : '未联通'"
              :tone="row.endpoint.probeReachable ? 'success' : 'warn'"
            />
          </div>
          <p class="readiness-panel__summary-detail">
            {{ row.endpoint.maskedEndpoint || row.endpoint.host || '未配置 endpoint' }}
          </p>
        </article>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.readiness-panel {
  display: grid;
  gap: 1rem;
}

.readiness-panel__summary,
.readiness-panel__stack {
  display: grid;
  gap: 0.85rem;
}

.readiness-panel__summary-item,
.readiness-panel__stack-item {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.readiness-panel__summary-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.readiness-panel__summary-title,
.readiness-panel__summary-detail {
  margin: 0;
}

.readiness-panel__summary-title {
  color: var(--color-ink-strong);
}

.readiness-panel__summary-detail {
  margin-top: 0.35rem;
  color: var(--color-ink-soft);
  line-height: 1.6;
}
</style>
