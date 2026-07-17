<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { NotificationGatewayOverviewView, NotificationTask } from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface Props {
  overview: NotificationGatewayOverviewView | null;
  highlightedTasks: NotificationTask[];
}

const props = defineProps<Props>();

const failurePressure = computed(
  () => (props.overview?.failedTaskCount ?? 0) + (props.overview?.deadLetterTaskCount ?? 0)
);

const deliveryRate = computed(() => {
  const total = props.overview?.totalTaskCount ?? 0;
  const delivered = props.overview?.deliveredTaskCount ?? 0;

  if (!total) {
    return '--';
  }

  return `${Math.round((delivered / total) * 100)}%`;
});

const metricItems = computed(() => [
  {
    label: '任务池',
    value: formatCount(props.overview?.totalTaskCount, ' 条'),
    caption: '全渠道通知任务总量'
  },
  {
    label: '送达率',
    value: deliveryRate.value,
    caption: '按 delivered / total 粗略计算'
  },
  {
    label: '异常积压',
    value: formatCount(failurePressure.value, ' 条'),
    caption: '失败与死信任务需优先处理'
  },
  {
    label: '待回执',
    value: formatCount(props.overview?.receiptPendingTaskCount, ' 条'),
    caption: '仍需追踪回执的通知'
  }
]);

const commandHeadline = computed(() => {
  if (!props.overview) {
    return '通知链路数据尚未加载，请先确认当前租户具备 notification.manage 权限。';
  }

  if (failurePressure.value > 0) {
    return `当前还有 ${formatCount(failurePressure.value)} 条失败或死信任务，需要尽快恢复投递闭环。`;
  }

  if ((props.overview.scheduledTaskCount ?? 0) > 0) {
    return `仍有 ${formatCount(props.overview.scheduledTaskCount)} 条计划通知待执行，建议继续盯住调度窗口。`;
  }

  return '通知链路当前没有明显积压，可以把重点转到通道路由配置和模板治理。';
});

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toneFromStatus(status: string) {
  const normalized = normalizeText(status);

  if (normalized === 'delivered' || normalized === 'sent') {
    return 'success';
  }

  if (normalized === 'failed' || normalized === 'dead_letter' || normalized === 'dead-letter') {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Notifications Command Deck"
    title="先看通知压力，再决定是调渠道、改模板还是补回放"
    :description="commandHeadline"
    dark
  >
    <div class="notification-hero">
      <div class="notification-hero__lead">
        <p class="notification-hero__badge">Gateway Posture</p>
        <h4 class="notification-hero__headline">
          已启用 {{ formatCount(overview?.enabledGatewayCount) }} / {{ formatCount(overview?.configuredGatewayCount) }}
          个网关，Mock {{ formatCount(overview?.mockGatewayCount) }} 个。
        </h4>
        <p class="notification-hero__subline">
          这里明确区分“已配置”“已启用”“仍待回执”和“已经失败”，避免把监控总量误当成链路可操作状态。
        </p>

        <div class="notification-hero__signals">
          <article
            v-for="metric in metricItems"
            :key="metric.label"
            class="notification-hero__signal-card"
          >
            <p class="notification-hero__signal-label">{{ metric.label }}</p>
            <h5 class="notification-hero__signal-value">{{ metric.value }}</h5>
            <p class="notification-hero__signal-caption">{{ metric.caption }}</p>
          </article>
        </div>
      </div>

      <div class="notification-hero__focus">
        <div class="notification-hero__focus-header">
          <p class="notification-hero__focus-eyebrow">Immediate Attention</p>
          <h5 class="notification-hero__focus-title">优先盯住这些任务</h5>
        </div>

        <div v-if="highlightedTasks.length" class="notification-hero__focus-list">
          <article
            v-for="task in highlightedTasks"
            :key="task.notificationTaskId"
            class="notification-hero__focus-item"
          >
            <div class="notification-hero__focus-main">
              <div>
                <p class="notification-hero__focus-template">{{ task.templateCode }}</p>
                <p class="notification-hero__focus-meta">
                  {{ task.notifyType }} · {{ task.targetReceiver }}
                </p>
              </div>
              <StatusPill :label="task.sendStatus" :tone="toneFromStatus(task.sendStatus)" />
            </div>
            <p class="notification-hero__focus-note">
              优先级 {{ task.priority || '--' }} · 重试 {{ formatCount(task.retryCount, ' 次') }}
            </p>
          </article>
        </div>

        <p v-else class="notification-hero__empty">
          当前没有 urgent / failed / dead-letter 任务，通知盘面相对平稳。
        </p>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.notification-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(18rem, 0.88fr);
  gap: 1rem;
}

.notification-hero__lead,
.notification-hero__focus {
  display: grid;
  gap: 0.9rem;
}

.notification-hero__badge,
.notification-hero__headline,
.notification-hero__subline,
.notification-hero__focus-eyebrow,
.notification-hero__focus-title,
.notification-hero__empty,
.notification-hero__focus-template,
.notification-hero__focus-meta,
.notification-hero__focus-note,
.notification-hero__signal-label,
.notification-hero__signal-value,
.notification-hero__signal-caption {
  margin: 0;
}

.notification-hero__badge,
.notification-hero__focus-eyebrow {
  width: fit-content;
  padding: 0.42rem 0.78rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 244, 236, 0.76);
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.notification-hero__headline,
.notification-hero__focus-title {
  color: #fff8f0;
}

.notification-hero__headline {
  max-width: 20ch;
  font-size: clamp(1.8rem, 3vw, 2.7rem);
  line-height: 1.08;
}

.notification-hero__subline,
.notification-hero__empty,
.notification-hero__focus-meta,
.notification-hero__focus-note,
.notification-hero__signal-caption {
  color: rgba(255, 244, 236, 0.72);
  line-height: 1.7;
}

.notification-hero__signals {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.notification-hero__signal-card,
.notification-hero__focus {
  border-radius: calc(var(--radius-xl) - 0.2rem);
  border: 1px solid rgba(255, 255, 255, 0.1);
  background:
    linear-gradient(165deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.03)),
    rgba(255, 255, 255, 0.04);
}

.notification-hero__signal-card {
  padding: 1rem;
}

.notification-hero__signal-label {
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(255, 244, 236, 0.58);
}

.notification-hero__signal-value {
  margin-top: 0.45rem;
  font-size: 1.9rem;
  color: #fffaf4;
}

.notification-hero__signal-caption {
  margin-top: 0.45rem;
}

.notification-hero__focus {
  align-content: start;
  padding: 1rem;
}

.notification-hero__focus-header {
  display: grid;
  gap: 0.65rem;
}

.notification-hero__focus-list {
  display: grid;
  gap: 0.75rem;
}

.notification-hero__focus-item {
  display: grid;
  gap: 0.45rem;
  padding: 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(17, 10, 6, 0.28);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.notification-hero__focus-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
}

.notification-hero__focus-template {
  color: #fff8f0;
  font-size: 1rem;
}

@media (max-width: 1080px) {
  .notification-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .notification-hero__signals {
    grid-template-columns: 1fr;
  }
}
</style>
