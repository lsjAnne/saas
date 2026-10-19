<script setup lang="ts">
import { computed } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import type {
  LiveConcurrencyOverviewView,
  LiveSessionStatusView,
  LiveSpecialAnalysisView
} from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface Props {
  specialAnalysis: LiveSpecialAnalysisView | null;
  concurrencyOverview: LiveConcurrencyOverviewView | null;
  selectedSessionStatus: LiveSessionStatusView | null;
}

const props = defineProps<Props>();

function formatReadyFlag(value: boolean | null | undefined) {
  return value ? '已就绪' : '未就绪';
}

const metricItems = computed(() => [
  {
    label: '在播场次',
    value: formatCount(props.concurrencyOverview?.runningSessionCount, ' 场'),
    caption: '当前租户正在运行的直播会话数'
  },
  {
    label: '剩余并发',
    value: formatCount(props.concurrencyOverview?.remainingQuota, ' 场'),
    caption: '按当前租户配额还能继续启动的场次'
  },
  {
    label: '人工接管',
    value: formatCount(props.specialAnalysis?.manualTakeoverSessionCount, ' 场'),
    caption: '已经切到人工兜底的数字人会话'
  },
  {
    label: '强控会话',
    value: formatCount(props.specialAnalysis?.strongControlSessionCount, ' 场'),
    caption: '仍处于强控制模式的直播会话'
  },
  {
    label: '承诺驳回',
    value: formatCount(props.specialAnalysis?.promiseRejectedSessionCount, ' 场'),
    caption: '承诺审核未放行，仍需人工收口的会话'
  },
  {
    label: '风险事件',
    value: formatCount(props.specialAnalysis?.openRiskEventCount, ' 条'),
    caption: '当前仍未关闭的直播风险事件'
  }
]);

const headline = computed(() => {
  if (props.selectedSessionStatus) {
    return `当前聚焦会话 ${props.selectedSessionStatus.liveSessionId}，场景 ${props.selectedSessionStatus.currentScene || '--'}，控制模式 ${props.selectedSessionStatus.controlMode || '--'}。`;
  }

  if ((props.specialAnalysis?.failedSessionCount ?? 0) > 0) {
    return `当前已有 ${formatCount(props.specialAnalysis?.failedSessionCount)} 场失败会话，需要优先排查恢复动作。`;
  }

  return '直播运营中心聚焦数字人互动、并发占用、风险命中和人工接管，而不是只看静态排期。';
});

const runtimeSummary = computed(() => {
  const selectedSession = props.selectedSessionStatus;
  const specialAnalysis = props.specialAnalysis;
  const provider = selectedSession?.runtimeProvider || specialAnalysis?.runtimeProvider || '--';
  const runtimeReady = selectedSession?.runtimeReady ?? specialAnalysis?.runtimeReady ?? false;
  const callbackReady = selectedSession?.callbackReady ?? specialAnalysis?.callbackReady ?? false;
  const blockedQueueCount = specialAnalysis?.runtimeGateBlockedQueueCount ?? 0;
  const roomId = selectedSession?.roomId || null;
  const mockFallback = roomId?.startsWith('mock-room-') ?? false;

  return {
    provider,
    runtimeReady,
    callbackReady,
    blockedQueueCount,
    roomLabel: roomId || '当前未选中会话',
    roomMode: roomId ? (mockFallback ? '模拟托底房间' : '真实运行房间') : '等待选择会话',
    roomCaption: roomId
      ? mockFallback
        ? '当前会话仍在 mock fallback 运行，说明 runtime 尚未真正接通。'
        : '当前会话已拿到真实房间号，可以继续跟踪在播治理。'
      : '先选择会话，再看当前房间是真实 runtime 还是 mock 托底。'
  };
});
</script>

<template>
  <PanelCard
    title="直播总控"
    :description="headline"
  >
    <div class="live-hero">
      <div class="live-hero__lead">
        <p class="live-hero__badge">直播指挥</p>
        <h4 class="live-hero__headline">
          给直播运营负责人直接下判断、下动作的指挥页
        </h4>
        <p class="live-hero__subline">
          运行态、数字人治理、并发占用、承诺审核与风险压力一屏联动，避免多模块来回跳转。
        </p>
      </div>

      <div class="live-hero__runtime-card">
        <div class="live-hero__runtime-head">
          <div>
            <p class="live-hero__runtime-title">运行态总览</p>
            <p class="live-hero__runtime-copy">
              当前 provider、runtime/callback 就绪度、阻塞计划数和选中会话房间状态都在这里一次看清。
            </p>
          </div>
          <div class="live-hero__room-pill">
            <span class="live-hero__room-pill-label">{{ runtimeSummary.roomMode }}</span>
            <strong class="live-hero__room-pill-value">{{ runtimeSummary.roomLabel }}</strong>
          </div>
        </div>

        <dl class="live-hero__runtime-grid">
          <div class="live-hero__runtime-fact">
            <dt>Runtime 服务商</dt>
            <dd>{{ runtimeSummary.provider }}</dd>
          </div>
          <div class="live-hero__runtime-fact">
            <dt>Runtime 就绪</dt>
            <dd>{{ formatReadyFlag(runtimeSummary.runtimeReady) }}</dd>
          </div>
          <div class="live-hero__runtime-fact">
            <dt>Callback 就绪</dt>
            <dd>{{ formatReadyFlag(runtimeSummary.callbackReady) }}</dd>
          </div>
          <div class="live-hero__runtime-fact">
            <dt>阻塞计划数</dt>
            <dd>{{ formatCount(runtimeSummary.blockedQueueCount, ' 个') }}</dd>
          </div>
        </dl>

        <p class="live-hero__runtime-note">{{ runtimeSummary.roomCaption }}</p>
      </div>

      <div class="live-hero__metrics">
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
.live-hero {
  display: grid;
  gap: 1rem;
}

.live-hero__lead {
  display: grid;
  gap: 0.85rem;
}

.live-hero__badge,
.live-hero__headline,
.live-hero__subline,
.live-hero__runtime-title,
.live-hero__runtime-copy,
.live-hero__runtime-note {
  margin: 0;
}

.live-hero__badge {
  width: fit-content;
  padding: 0.42rem 0.78rem;
  border-radius: var(--radius-md);
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.live-hero__headline {
  max-width: 40ch;
  color: var(--color-ink-strong);
  font-size: 1.45rem;
  font-weight: 600;
  line-height: 1.3;
}

.live-hero__subline {
  max-width: 56rem;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.live-hero__runtime-card {
  display: grid;
  gap: 0.85rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.live-hero__runtime-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.live-hero__runtime-title {
  color: var(--color-ink-strong);
  font-size: 1rem;
  font-weight: 600;
}

.live-hero__runtime-copy,
.live-hero__runtime-note,
.live-hero__runtime-fact dt,
.live-hero__room-pill-label {
  color: var(--color-ink-soft);
}

.live-hero__runtime-copy,
.live-hero__runtime-note {
  line-height: 1.65;
}

.live-hero__room-pill {
  min-width: 13rem;
  display: grid;
  gap: 0.2rem;
  padding: 0.8rem 0.9rem;
  border-radius: var(--radius-lg);
  background: var(--color-panel-strong);
  border: 1px solid var(--color-line);
}

.live-hero__room-pill-label {
  font-size: 0.76rem;
}

.live-hero__room-pill-value,
.live-hero__runtime-fact dd {
  color: var(--color-ink-strong);
}

.live-hero__room-pill-value {
  font-size: 0.92rem;
  word-break: break-word;
}

.live-hero__runtime-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0;
}

.live-hero__runtime-fact {
  display: grid;
  gap: 0.22rem;
}

.live-hero__runtime-fact dt {
  font-size: 0.76rem;
}

.live-hero__runtime-fact dd {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
}

.live-hero__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

@media (max-width: 980px) {
  .live-hero__runtime-head {
    flex-direction: column;
  }

  .live-hero__runtime-grid,
  .live-hero__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .live-hero__room-pill {
    min-width: 0;
    width: 100%;
  }
}

@media (max-width: 640px) {
  .live-hero__runtime-grid,
  .live-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
