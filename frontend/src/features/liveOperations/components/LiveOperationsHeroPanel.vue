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
    eyebrow="Live Operations Deck"
    title="把开播计划、数字人互动、强控接管和风险闭环压进同一个直播总控台"
    :description="headline"
    dark
  >
    <div class="live-hero">
      <div class="live-hero__lead">
        <p class="live-hero__badge">Broadcast Control</p>
        <h4 class="live-hero__headline">
          这一页不是普通数据看板，而是给直播运营负责人直接下判断和下动作的指挥页。
        </h4>
        <p class="live-hero__subline">
          页面同时展示运行态、数字人治理状态、并发占用、承诺审核和风险压力，避免在多个模块之间来回跳转确认直播是否真正可控。
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
            <dt>Runtime Provider</dt>
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
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 244, 236, 0.76);
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.live-hero__headline {
  max-width: 22ch;
  color: #fff8f0;
  font-size: clamp(1.8rem, 3vw, 2.8rem);
  line-height: 1.08;
}

.live-hero__subline {
  max-width: 60rem;
  color: rgba(255, 244, 236, 0.72);
  line-height: 1.7;
}

.live-hero__runtime-card {
  display: grid;
  gap: 0.85rem;
  padding: 1rem;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.live-hero__runtime-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.live-hero__runtime-title {
  color: #fff8f0;
  font-size: 1rem;
  font-weight: 600;
}

.live-hero__runtime-copy,
.live-hero__runtime-note,
.live-hero__runtime-fact dt,
.live-hero__room-pill-label {
  color: rgba(255, 244, 236, 0.72);
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
  background: rgba(255, 255, 255, 0.08);
}

.live-hero__room-pill-label {
  font-size: 0.76rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.live-hero__room-pill-value,
.live-hero__runtime-fact dd {
  color: #fffdf9;
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
  letter-spacing: 0.12em;
  text-transform: uppercase;
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
