<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  LiveAccountGovernanceView,
  LiveConcurrencyQueueView,
  LiveRiskEventView,
  LiveRiskRecoveryPlanView,
  LiveSpecialAnalysisDrilldownView
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  riskEvents: LiveRiskEventView[];
  concurrencyQueues: LiveConcurrencyQueueView[];
  riskRecoveryPlans: LiveRiskRecoveryPlanView[];
  accountGovernance: LiveAccountGovernanceView[];
  specialAnalysisDrilldown: LiveSpecialAnalysisDrilldownView | null;
}

defineProps<Props>();

function toneFromSeverity(severity: string) {
  const normalized = severity.toLowerCase();

  if (normalized.includes('critical') || normalized.includes('high')) {
    return 'warn';
  }

  if (normalized.includes('normal') || normalized.includes('low')) {
    return 'neutral';
  }

  return 'neutral';
}

function toneFromGovernanceLevel(level: string) {
  const normalized = level.toLowerCase();

  if (normalized.includes('high') || normalized.includes('critical')) {
    return 'warn';
  }

  if (normalized.includes('low') || normalized.includes('healthy')) {
    return 'success';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Risk Radar"
    title="风险雷达与互动闭环"
    description="把风险事件、阻塞队列、恢复动作、账号治理和互动闭环快照堆到同一屏上，方便直播负责人一次性判断。"
  >
    <div class="live-risk-radar">
      <section class="live-risk-radar__section">
        <div class="live-risk-radar__section-head">
          <h4 class="live-risk-radar__section-title">互动闭环快照</h4>
          <span class="live-risk-radar__section-count">
            {{ specialAnalysisDrilldown?.riskEventCodes.length ?? 0 }}
          </span>
        </div>
        <div class="live-risk-radar__snapshot-grid">
          <article class="live-risk-radar__snapshot-card">
            <p class="live-risk-radar__snapshot-value">{{ specialAnalysisDrilldown?.highRiskSessionIds.length ?? 0 }}</p>
            <p class="live-risk-radar__snapshot-label">高风险会话</p>
          </article>
          <article class="live-risk-radar__snapshot-card">
            <p class="live-risk-radar__snapshot-value">{{ specialAnalysisDrilldown?.manualTakeoverSessionIds.length ?? 0 }}</p>
            <p class="live-risk-radar__snapshot-label">人工接管</p>
          </article>
          <article class="live-risk-radar__snapshot-card">
            <p class="live-risk-radar__snapshot-value">{{ specialAnalysisDrilldown?.promiseRejectedSessionIds.length ?? 0 }}</p>
            <p class="live-risk-radar__snapshot-label">承诺驳回</p>
          </article>
          <article class="live-risk-radar__snapshot-card">
            <p class="live-risk-radar__snapshot-value">{{ specialAnalysisDrilldown?.queuedPlanIds.length ?? 0 }}</p>
            <p class="live-risk-radar__snapshot-label">排队计划</p>
          </article>
        </div>
        <div class="live-risk-radar__chips">
          <StatusPill
            v-for="blockedType in specialAnalysisDrilldown?.blockedTypes ?? []"
            :key="blockedType"
            :label="blockedType"
            tone="neutral"
          />
          <StatusPill
            v-for="riskCode in specialAnalysisDrilldown?.riskEventCodes ?? []"
            :key="riskCode"
            :label="riskCode"
            tone="warn"
          />
        </div>
      </section>

      <section class="live-risk-radar__section">
        <div class="live-risk-radar__section-head">
          <h4 class="live-risk-radar__section-title">风险事件</h4>
          <span class="live-risk-radar__section-count">{{ riskEvents.length }}</span>
        </div>
        <div v-if="riskEvents.length" class="live-risk-radar__list">
          <article
            v-for="event in riskEvents.slice(0, 4)"
            :key="`${event.eventCode}-${event.occurredAt}`"
            class="live-risk-radar__item"
          >
            <div class="live-risk-radar__item-head">
              <div>
                <p class="live-risk-radar__item-title">{{ event.eventCode }}</p>
                <p class="live-risk-radar__item-meta">
                  会话 {{ event.liveSessionId || '--' }} / 计划 {{ event.livePlanId || '--' }}
                </p>
              </div>
              <StatusPill :label="event.severity" :tone="toneFromSeverity(event.severity)" />
            </div>
            <p class="live-risk-radar__item-detail">{{ event.detail }}</p>
            <p class="live-risk-radar__item-time">{{ formatDateTime(event.occurredAt) }}</p>
          </article>
        </div>
        <p v-else class="live-risk-radar__empty">当前没有风险事件。</p>
      </section>

      <section class="live-risk-radar__section">
        <div class="live-risk-radar__section-head">
          <h4 class="live-risk-radar__section-title">阻塞队列</h4>
          <span class="live-risk-radar__section-count">{{ concurrencyQueues.length }}</span>
        </div>
        <div v-if="concurrencyQueues.length" class="live-risk-radar__list">
          <article
            v-for="queue in concurrencyQueues.slice(0, 4)"
            :key="`${queue.livePlanId}-${queue.blockedType}`"
            class="live-risk-radar__item"
          >
            <p class="live-risk-radar__item-title">{{ queue.planName }}</p>
            <p class="live-risk-radar__item-meta">
              {{ queue.blockedType }} / 账号 {{ queue.liveAccountId || '--' }}
            </p>
            <p class="live-risk-radar__item-detail">{{ queue.blockedReason }}</p>
            <p class="live-risk-radar__item-time">{{ formatDateTime(queue.scheduledStartAt) }}</p>
          </article>
        </div>
        <p v-else class="live-risk-radar__empty">当前没有阻塞队列。</p>
      </section>

      <section class="live-risk-radar__section">
        <div class="live-risk-radar__section-head">
          <h4 class="live-risk-radar__section-title">恢复方案</h4>
          <span class="live-risk-radar__section-count">{{ riskRecoveryPlans.length }}</span>
        </div>
        <div v-if="riskRecoveryPlans.length" class="live-risk-radar__list">
          <article
            v-for="plan in riskRecoveryPlans.slice(0, 4)"
            :key="`${plan.entityType}-${plan.entityId}-${plan.riskCode}`"
            class="live-risk-radar__item"
          >
            <div class="live-risk-radar__item-head">
              <div>
                <p class="live-risk-radar__item-title">{{ plan.riskCode }}</p>
                <p class="live-risk-radar__item-meta">
                  {{ plan.entityType }} / {{ plan.entityId }}
                </p>
              </div>
              <StatusPill
                :label="plan.requiresManualReview ? '人工复核' : '可自动恢复'"
                :tone="plan.requiresManualReview ? 'warn' : 'success'"
              />
            </div>
            <p class="live-risk-radar__item-detail">{{ plan.recoveryAction }}</p>
            <p class="live-risk-radar__item-time">{{ plan.blockingReason || '当前没有额外阻塞说明。' }}</p>
          </article>
        </div>
        <p v-else class="live-risk-radar__empty">当前没有恢复方案数据。</p>
      </section>

      <section class="live-risk-radar__section">
        <div class="live-risk-radar__section-head">
          <h4 class="live-risk-radar__section-title">账号治理</h4>
          <span class="live-risk-radar__section-count">{{ accountGovernance.length }}</span>
        </div>
        <div v-if="accountGovernance.length" class="live-risk-radar__list">
          <article
            v-for="account in accountGovernance.slice(0, 4)"
            :key="account.liveAccountId"
            class="live-risk-radar__item"
          >
            <div class="live-risk-radar__item-head">
              <div>
                <p class="live-risk-radar__item-title">{{ account.accountName }}</p>
                <p class="live-risk-radar__item-meta">
                  {{ account.liveAccountId }} / 组织 {{ account.organizationId }}
                </p>
              </div>
              <StatusPill
                :label="account.governanceRiskLevel"
                :tone="toneFromGovernanceLevel(account.governanceRiskLevel)"
              />
            </div>
            <p class="live-risk-radar__item-detail">
              在播 {{ account.runningSessionCount }} 场 / 排队 {{ account.queuedPlanCount }} 个 /
              {{ account.occupied ? '当前已占用' : '当前未占用' }}
            </p>
            <p class="live-risk-radar__item-time">{{ formatDateTime(account.expiresAt) }}</p>
          </article>
        </div>
        <p v-else class="live-risk-radar__empty">当前没有账号治理数据。</p>
      </section>
    </div>
  </PanelCard>
</template>

<style scoped>
.live-risk-radar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.live-risk-radar__section {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.live-risk-radar__section-head,
.live-risk-radar__item-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.live-risk-radar__section-title,
.live-risk-radar__section-count,
.live-risk-radar__item-title,
.live-risk-radar__item-meta,
.live-risk-radar__item-detail,
.live-risk-radar__item-time,
.live-risk-radar__empty,
.live-risk-radar__snapshot-value,
.live-risk-radar__snapshot-label {
  margin: 0;
}

.live-risk-radar__section-title,
.live-risk-radar__item-title,
.live-risk-radar__snapshot-value {
  color: var(--color-ink-strong);
}

.live-risk-radar__section-count {
  min-width: 2rem;
  min-height: 2rem;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(91, 72, 54, 0.08);
  color: var(--color-ink-strong);
}

.live-risk-radar__list {
  display: grid;
  gap: 0.75rem;
}

.live-risk-radar__item,
.live-risk-radar__snapshot-card {
  display: grid;
  gap: 0.45rem;
  padding: 0.85rem 0.9rem;
  border-radius: calc(var(--radius-lg) - 0.25rem);
  background: rgba(250, 245, 239, 0.82);
}

.live-risk-radar__item-meta,
.live-risk-radar__item-detail,
.live-risk-radar__item-time,
.live-risk-radar__empty,
.live-risk-radar__snapshot-label {
  color: var(--color-ink-soft);
}

.live-risk-radar__snapshot-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.live-risk-radar__snapshot-value {
  font-size: 1.4rem;
  font-weight: 700;
}

.live-risk-radar__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

@media (max-width: 1080px) {
  .live-risk-radar {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .live-risk-radar__snapshot-grid {
    grid-template-columns: 1fr;
  }
}
</style>
