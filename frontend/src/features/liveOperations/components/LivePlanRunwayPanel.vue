<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { LiveConcurrencyCheckView, LivePlan } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  livePlans: LivePlan[];
  selectedPlanId: string | null;
  selectedPlan: LivePlan | null;
  concurrencyCheck: LiveConcurrencyCheckView | null;
  actionFeedback: string | null;
  isRunningAction: boolean;
}

interface Emits {
  select: [livePlanId: string];
  validate: [];
  start: [];
  runDue: [];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

function toneFromStatus(status: string) {
  const normalized = status.toLowerCase();

  if (normalized.includes('running') || normalized.includes('live')) {
    return 'success';
  }

  if (normalized.includes('cancel') || normalized.includes('fail')) {
    return 'warn';
  }

  return 'neutral';
}

function formatReadyFlag(value: boolean) {
  return value ? '已就绪' : '未就绪';
}

const concurrencyGateSummary = computed(() => {
  const check = props.concurrencyCheck;
  if (!check) {
    return null;
  }

  return {
    title: check.allowed ? '并发校验通过' : '并发校验未通过',
    detail: check.allowed
      ? '计划可以继续开播，当前并发、runtime 和 callback 均满足要求。'
      : '计划被拦截，需要先确认是配额、账号占用还是运行态未就绪。'
  };
});
</script>

<template>
  <PanelCard
    eyebrow="Plan Runway"
    title="直播计划跑道"
    description="先看排期、主账号和占用，再做并发校验、到点开播或批量运行。"
  >
    <div class="live-plan-runway">
      <div class="live-plan-runway__list">
        <button
          v-for="plan in livePlans"
          :key="plan.livePlanId"
          type="button"
          :class="[
            'live-plan-runway__item',
            { 'live-plan-runway__item--active': plan.livePlanId === selectedPlanId }
          ]"
          @click="emit('select', plan.livePlanId)"
        >
          <div class="live-plan-runway__item-head">
            <div>
              <p class="live-plan-runway__item-title">{{ plan.planName }}</p>
              <p class="live-plan-runway__item-meta">
                店铺 {{ plan.storeId }} / 主播 {{ plan.anchorProfileName || '--' }}
              </p>
            </div>
            <StatusPill :label="plan.planStatus" :tone="toneFromStatus(plan.planStatus)" />
          </div>
          <p class="live-plan-runway__item-time">
            {{ formatDateTime(plan.scheduledStartAt) }} - {{ formatDateTime(plan.scheduledEndAt) }}
          </p>
        </button>
      </div>

      <div class="live-plan-runway__detail-card">
        <div v-if="selectedPlan" class="live-plan-runway__detail">
          <div class="live-plan-runway__detail-head">
            <div>
              <p class="live-plan-runway__detail-title">{{ selectedPlan.planName }}</p>
              <p class="live-plan-runway__detail-meta">
                账号 {{ selectedPlan.liveAccountId || '--' }} / 创建于 {{ formatDateTime(selectedPlan.createdAt) }}
              </p>
            </div>
            <StatusPill :label="selectedPlan.planStatus" :tone="toneFromStatus(selectedPlan.planStatus)" />
          </div>

          <dl class="live-plan-runway__facts">
            <div class="live-plan-runway__fact">
              <dt>计划开始</dt>
              <dd>{{ formatDateTime(selectedPlan.scheduledStartAt) }}</dd>
            </div>
            <div class="live-plan-runway__fact">
              <dt>计划结束</dt>
              <dd>{{ formatDateTime(selectedPlan.scheduledEndAt) }}</dd>
            </div>
            <div class="live-plan-runway__fact">
              <dt>主播画像</dt>
              <dd>{{ selectedPlan.anchorProfileName || '当前未配置主播画像。' }}</dd>
            </div>
          </dl>

          <div v-if="concurrencyCheck && concurrencyGateSummary" class="live-plan-runway__check-card">
            <p class="live-plan-runway__check-title">{{ concurrencyGateSummary.title }}</p>
            <p class="live-plan-runway__check-detail">{{ concurrencyGateSummary.detail }}</p>

            <dl class="live-plan-runway__check-grid">
              <div class="live-plan-runway__check-fact">
                <dt>Runtime Provider</dt>
                <dd>{{ concurrencyCheck.runtimeProvider || '--' }}</dd>
              </div>
              <div class="live-plan-runway__check-fact">
                <dt>Runtime 就绪</dt>
                <dd>{{ formatReadyFlag(concurrencyCheck.runtimeReady) }}</dd>
              </div>
              <div class="live-plan-runway__check-fact">
                <dt>Callback 就绪</dt>
                <dd>{{ formatReadyFlag(concurrencyCheck.callbackReady) }}</dd>
              </div>
              <div class="live-plan-runway__check-fact">
                <dt>账号占用</dt>
                <dd>{{ concurrencyCheck.accountOccupied ? '已占用' : '空闲' }}</dd>
              </div>
              <div class="live-plan-runway__check-fact">
                <dt>租户并发</dt>
                <dd>{{ concurrencyCheck.tenantRunningCount }} / {{ concurrencyCheck.tenantQuotaLimit }}</dd>
              </div>
              <div class="live-plan-runway__check-fact">
                <dt>配额状态</dt>
                <dd>{{ concurrencyCheck.tenantQuotaExceeded ? '超限' : '可用' }}</dd>
              </div>
            </dl>

            <p class="live-plan-runway__check-reason">
              {{ concurrencyCheck.reason || '当前没有额外阻塞原因。' }}
            </p>
          </div>

          <p v-if="actionFeedback" class="live-plan-runway__feedback">{{ actionFeedback }}</p>

          <div class="live-plan-runway__actions">
            <button
              type="button"
              class="live-plan-runway__action"
              :disabled="isRunningAction"
              @click="emit('validate')"
            >
              并发校验
            </button>
            <button
              type="button"
              class="live-plan-runway__action live-plan-runway__action--primary"
              :disabled="isRunningAction"
              @click="emit('start')"
            >
              立即开播
            </button>
            <button
              type="button"
              class="live-plan-runway__action"
              :disabled="isRunningAction"
              @click="emit('runDue')"
            >
              运行到点计划
            </button>
          </div>
        </div>

        <p v-else class="live-plan-runway__empty">当前没有直播计划可展示。</p>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.live-plan-runway {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(20rem, 1.05fr);
  gap: 1rem;
}

.live-plan-runway__list,
.live-plan-runway__detail {
  display: grid;
  gap: 0.8rem;
}

.live-plan-runway__item {
  display: grid;
  gap: 0.5rem;
  width: 100%;
  padding: 0.95rem 1rem;
  text-align: left;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.74);
}

.live-plan-runway__item--active {
  border-color: rgba(156, 113, 71, 0.3);
  box-shadow: 0 14px 30px rgba(112, 84, 55, 0.12);
}

.live-plan-runway__item-head,
.live-plan-runway__detail-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.live-plan-runway__item-title,
.live-plan-runway__item-meta,
.live-plan-runway__item-time,
.live-plan-runway__detail-title,
.live-plan-runway__detail-meta,
.live-plan-runway__feedback,
.live-plan-runway__empty,
.live-plan-runway__check-title,
.live-plan-runway__check-detail,
.live-plan-runway__check-reason {
  margin: 0;
}

.live-plan-runway__item-title,
.live-plan-runway__detail-title,
.live-plan-runway__check-title,
.live-plan-runway__check-fact dd {
  color: var(--color-ink-strong);
}

.live-plan-runway__item-meta,
.live-plan-runway__item-time,
.live-plan-runway__detail-meta,
.live-plan-runway__feedback,
.live-plan-runway__empty,
.live-plan-runway__check-detail,
.live-plan-runway__check-reason,
.live-plan-runway__fact dt,
.live-plan-runway__check-fact dt {
  color: var(--color-ink-soft);
}

.live-plan-runway__detail-card {
  padding: 1rem;
  border-radius: var(--radius-xl);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background:
    radial-gradient(circle at top right, rgba(198, 172, 140, 0.12), transparent 36%),
    rgba(255, 252, 248, 0.92);
}

.live-plan-runway__facts,
.live-plan-runway__check-grid {
  display: grid;
  gap: 0.7rem;
  margin: 0;
}

.live-plan-runway__check-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.live-plan-runway__fact,
.live-plan-runway__check-fact {
  display: grid;
  gap: 0.28rem;
}

.live-plan-runway__fact dt,
.live-plan-runway__check-fact dt {
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.live-plan-runway__fact dd,
.live-plan-runway__check-fact dd {
  margin: 0;
  line-height: 1.65;
}

.live-plan-runway__check-card {
  display: grid;
  gap: 0.55rem;
  padding: 0.85rem 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(250, 245, 239, 0.82);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.live-plan-runway__check-title {
  font-weight: 600;
}

.live-plan-runway__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.live-plan-runway__action {
  min-height: 2.9rem;
  padding: 0 1.1rem;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.86);
  color: var(--color-ink-strong);
  font: inherit;
  font-weight: 600;
}

.live-plan-runway__action--primary {
  border: 0;
  background: linear-gradient(135deg, rgba(131, 79, 36, 0.98), rgba(186, 125, 74, 0.92));
  color: #fffaf4;
}

@media (max-width: 1080px) {
  .live-plan-runway {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .live-plan-runway__check-grid {
    grid-template-columns: 1fr;
  }
}
</style>
