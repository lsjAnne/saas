<script setup lang="ts">
import { computed } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import type { ExceptionTask } from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface Props {
  summary: {
    totalCount: number;
    openCount: number;
    escalatedCount: number;
    resolvedCount: number;
    highSeverityCount: number;
  };
  selectedException: ExceptionTask | null;
  actionFeedback: string | null;
}

const props = defineProps<Props>();

const metricItems = computed(() => [
  {
    label: '开放异常',
    value: formatCount(props.summary.openCount, ' 项'),
    caption: '仍需要处理或人工判断的异常'
  },
  {
    label: '高风险',
    value: formatCount(props.summary.highSeverityCount, ' 项'),
    caption: '优先级最高，应先收口'
  },
  {
    label: '已升级',
    value: formatCount(props.summary.escalatedCount, ' 项'),
    caption: '已经进入跨角色跟进状态'
  },
  {
    label: '已解决',
    value: formatCount(props.summary.resolvedCount, ' 项'),
    caption: '已完成处理并回写结果'
  }
]);

const headline = computed(() => {
  if (props.actionFeedback) {
    return props.actionFeedback;
  }

  if (props.selectedException) {
    return `当前聚焦 ${props.selectedException.exceptionType}，关联 ${props.selectedException.relatedType} / ${props.selectedException.relatedId}。`;
  }

  return `当前异常池共 ${formatCount(props.summary.totalCount, ' 项')}，优先聚焦高风险与未收口事项。`;
});
</script>

<template>
  <PanelCard
    eyebrow="Exception Command"
    title="把异常分类、责任动作和推荐 SOP 压进同一张风险操作台"
    :description="headline"
    dark
  >
    <div class="exception-hero">
      <div class="exception-hero__lead">
        <p class="exception-hero__badge">Risk Operations</p>
        <h4 class="exception-hero__headline">
          异常中心的重点不是展示告警，而是让团队在一屏内判断、执行、留痕。
        </h4>
        <p class="exception-hero__subline">
          当前版本先围绕异常分类导航、详情回读、推荐 SOP 和处理动作形成真实操作入口，避免异常问题继续散落在订单、库存和履约页面中。
        </p>
      </div>

      <div class="exception-hero__metrics">
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
.exception-hero {
  display: grid;
  gap: 1rem;
}

.exception-hero__lead {
  display: grid;
  gap: 0.85rem;
}

.exception-hero__badge,
.exception-hero__headline,
.exception-hero__subline {
  margin: 0;
}

.exception-hero__badge {
  width: fit-content;
  padding: 0.42rem 0.78rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 244, 236, 0.76);
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.exception-hero__headline {
  max-width: 20ch;
  color: #fff8f0;
  font-size: clamp(1.9rem, 3vw, 2.8rem);
  line-height: 1.08;
}

.exception-hero__subline {
  max-width: 62rem;
  color: rgba(255, 244, 236, 0.72);
  line-height: 1.72;
}

.exception-hero__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

@media (max-width: 980px) {
  .exception-hero__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .exception-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
