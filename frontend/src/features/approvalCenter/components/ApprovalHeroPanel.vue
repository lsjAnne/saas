<script setup lang="ts">
import { computed } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import type { ApprovalInstance } from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface Props {
  summary: {
    pendingApprovalCount: number;
    processedApprovalCount: number;
    enabledTemplateCount: number;
    governanceOpenCount: number;
  };
  selectedApproval: ApprovalInstance | null;
}

const props = defineProps<Props>();

const metricItems = computed(() => [
  {
    label: '待审批',
    value: formatCount(props.summary.pendingApprovalCount, ' 项'),
    caption: '当前仍在队列中的审批事项'
  },
  {
    label: '已处理',
    value: formatCount(props.summary.processedApprovalCount, ' 项'),
    caption: '已审批或已驳回的事项'
  },
  {
    label: '启用模板',
    value: formatCount(props.summary.enabledTemplateCount, ' 套'),
    caption: '当前租户可用的审批模板'
  },
  {
    label: '治理请求',
    value: formatCount(props.summary.governanceOpenCount, ' 单'),
    caption: '仍在流转中的治理请求'
  }
]);

const headline = computed(() => {
  if (props.selectedApproval) {
    return `当前聚焦 ${props.selectedApproval.approvalType} / ${props.selectedApproval.relatedType}，处理人 ${props.selectedApproval.currentHandlerId || '--'}。`;
  }

  if (props.summary.pendingApprovalCount > 0) {
    return `当前仍有 ${formatCount(props.summary.pendingApprovalCount)} 项待处理审批，需要尽快归拢责任人。`;
  }

  return '当前审批盘面没有明显积压，可以把注意力转向模板治理和协作规范。';
});
</script>

<template>
  <PanelCard
    title="审批协作总览"
    description="待审批、已处理、启用模板与治理请求一览。"
  >
    <div class="approval-hero">
      <div class="approval-hero__lead">
        <p class="approval-hero__badge">审批总览</p>
        <h4 class="approval-hero__headline">
          {{ headline }}
        </h4>
        <p class="approval-hero__subline">
          责任流转一屏看清谁在处理、哪些模板在跑。
        </p>
      </div>

      <div class="approval-hero__metrics">
        <MetricCard
          v-for="metric in metricItems"
          :key="metric.label"
          :label="metric.label"
          :value="metric.value"
          :caption="metric.caption"
        />
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.approval-hero {
  display: grid;
  gap: 1rem;
}

.approval-hero__lead {
  display: grid;
  gap: 0.85rem;
}

.approval-hero__badge,
.approval-hero__headline,
.approval-hero__subline {
  margin: 0;
}

.approval-hero__badge {
  width: fit-content;
  padding: 0.42rem 0.78rem;
  border-radius: var(--radius-pill);
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.approval-hero__headline {
  max-width: 22ch;
  color: var(--color-ink-strong);
  font-size: clamp(1.35rem, 2.2vw, 1.6rem);
  line-height: 1.25;
}

.approval-hero__subline {
  max-width: 60rem;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.approval-hero__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

@media (max-width: 980px) {
  .approval-hero__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .approval-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
