<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import LiveOperationsHeroPanel from '@/features/liveOperations/components/LiveOperationsHeroPanel.vue';
import LivePlanRunwayPanel from '@/features/liveOperations/components/LivePlanRunwayPanel.vue';
import LiveRiskRadarPanel from '@/features/liveOperations/components/LiveRiskRadarPanel.vue';
import LiveSessionCommandPanel from '@/features/liveOperations/components/LiveSessionCommandPanel.vue';
import { useLiveOperationsOverview } from '@/features/liveOperations/composables/useLiveOperationsOverview';
import type {
  ConfigureCommitmentWhitelistPayload,
  EnableControlModePayload,
  InteractionReplyPayload,
  ManualTakeoverPayload,
  PromiseAuditPayload,
  SimulateLiveCallbackPayload,
  StrongControlPayload,
  SwitchLiveScenePayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const liveOperations = useLiveOperationsOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await liveOperations.load(authStore.token);
}

async function handleSelectSession(liveSessionId: string) {
  if (!authStore.token) {
    return;
  }

  await liveOperations.selectSession(liveSessionId, authStore.token);
}

async function handleValidatePlan() {
  if (!authStore.token) {
    return;
  }

  await liveOperations.validateSelectedPlan(authStore.token);
}

async function handleStartPlan() {
  if (!authStore.token) {
    return;
  }

  await liveOperations.startSelectedPlan(authStore.token);
}

async function handleRunDue() {
  if (!authStore.token) {
    return;
  }

  await liveOperations.runDue(authStore.token);
}

async function handlePauseSession() {
  if (!authStore.token) {
    return;
  }

  await liveOperations.pauseSelectedSession(authStore.token);
}

async function handleResumeSession() {
  if (!authStore.token) {
    return;
  }

  await liveOperations.resumeSelectedSession(authStore.token);
}

async function handleStopSession() {
  if (!authStore.token) {
    return;
  }

  await liveOperations.stopSelectedSession(authStore.token);
}

async function handleSkipCurrentProduct() {
  if (!authStore.token) {
    return;
  }

  await liveOperations.skipSelectedSessionProduct(authStore.token);
}

async function handleSwitchScene(payload: SwitchLiveScenePayload) {
  if (!authStore.token) {
    return;
  }

  await liveOperations.switchSelectedSessionScene(payload, authStore.token);
}

async function handleStrongControl(payload: StrongControlPayload) {
  if (!authStore.token) {
    return;
  }

  await liveOperations.strongControlSelectedSession(payload, authStore.token);
}

async function handleEnableControlMode(payload: EnableControlModePayload) {
  if (!authStore.token) {
    return;
  }

  await liveOperations.enableSelectedSessionControlMode(payload, authStore.token);
}

async function handleManualTakeover(payload: ManualTakeoverPayload) {
  if (!authStore.token) {
    return;
  }

  await liveOperations.manualTakeoverSelectedSession(payload, authStore.token);
}

async function handlePromiseAudit(payload: PromiseAuditPayload) {
  if (!authStore.token) {
    return;
  }

  await liveOperations.promiseAuditSelectedSession(payload, authStore.token);
}

async function handleResumeSystemMode() {
  if (!authStore.token) {
    return;
  }

  await liveOperations.resumeSelectedSessionSystemMode(authStore.token);
}

async function handleConfigureCommitmentWhitelist(payload: ConfigureCommitmentWhitelistPayload) {
  if (!authStore.token) {
    return;
  }

  await liveOperations.configureSelectedPlanCommitmentWhitelist(payload, authStore.token);
}

async function handleReplyInteraction(payload: InteractionReplyPayload) {
  if (!authStore.token) {
    return;
  }

  await liveOperations.replySelectedSession(payload, authStore.token);
}

async function handleSimulateCallback(payload: SimulateLiveCallbackPayload) {
  if (!authStore.token) {
    return;
  }

  await liveOperations.simulateSelectedSessionCallback(payload, authStore.token);
}

onMounted(() => {
  void bootstrapPage();
});
</script>

<template>
  <section class="live-operations-page">
    <header class="live-operations-page__header">
      <p class="live-operations-page__eyebrow">直播指挥</p>
      <h2 class="live-operations-page__title">直播运营</h2>
      <p class="live-operations-page__subtitle">
        直播排期、风险雷达与现场指挥一屏管理
      </p>
    </header>

    <InlineErrorCard
      v-if="liveOperations.errorMessage"
      :message="liveOperations.errorMessage"
      @retry="bootstrapPage"
    />

    <LiveOperationsHeroPanel
      :special-analysis="liveOperations.specialAnalysis"
      :concurrency-overview="liveOperations.concurrencyOverview"
      :selected-session-status="liveOperations.selectedSessionStatus"
    />

    <div class="live-operations-page__grid">
      <LivePlanRunwayPanel
        :live-plans="liveOperations.livePlans"
        :selected-plan-id="liveOperations.selectedPlanId"
        :selected-plan="liveOperations.selectedPlan"
        :concurrency-check="liveOperations.concurrencyCheck"
        :action-feedback="liveOperations.actionFeedback"
        :is-running-action="liveOperations.isRunningAction"
        @select="liveOperations.selectPlan"
        @validate="handleValidatePlan"
        @start="handleStartPlan"
        @run-due="handleRunDue"
      />

      <LiveSessionCommandPanel
        :live-sessions="liveOperations.liveSessions"
        :selected-plan-id="liveOperations.selectedPlanId"
        :selected-session-id="liveOperations.selectedSessionId"
        :selected-session-status="liveOperations.selectedSessionStatus"
        :selected-session-risk-events="liveOperations.selectedSessionRiskEvents"
        :latest-skip-result="liveOperations.latestSkipResult"
        :latest-interaction-reply="liveOperations.latestInteractionReply"
        :latest-commitment-whitelist-config="liveOperations.latestCommitmentWhitelistConfig"
        :action-feedback="liveOperations.actionFeedback"
        :is-running-action="liveOperations.isRunningAction"
        @select="handleSelectSession"
        @pause="handlePauseSession"
        @resume="handleResumeSession"
        @stop="handleStopSession"
        @skip-current-product="handleSkipCurrentProduct"
        @switch-scene="handleSwitchScene"
        @strong-control="handleStrongControl"
        @enable-control-mode="handleEnableControlMode"
        @resume-system-mode="handleResumeSystemMode"
        @manual-takeover="handleManualTakeover"
        @promise-audit="handlePromiseAudit"
        @configure-commitment-whitelist="handleConfigureCommitmentWhitelist"
        @reply-interaction="handleReplyInteraction"
        @simulate-callback="handleSimulateCallback"
      />
    </div>

    <LiveRiskRadarPanel
      :risk-events="liveOperations.riskEvents"
      :concurrency-queues="liveOperations.concurrencyQueues"
      :risk-recovery-plans="liveOperations.riskRecoveryPlans"
      :account-governance="liveOperations.accountGovernance"
      :special-analysis-drilldown="liveOperations.specialAnalysisDrilldown"
    />

    <p v-if="liveOperations.isLoading" class="live-operations-page__footer-note">
      正在刷新直播计划、运行会话、互动状态和风险闭环数据...
    </p>
  </section>
</template>

<style scoped>
.live-operations-page {
  display: grid;
  gap: 1.2rem;
}

.live-operations-page__header {
  display: grid;
  gap: 0.8rem;
}

.live-operations-page__eyebrow,
.live-operations-page__title,
.live-operations-page__subtitle,
.live-operations-page__footer-note {
  margin: 0;
}

.live-operations-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.live-operations-page__title {
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
  line-height: 1.2;
}

.live-operations-page__subtitle,
.live-operations-page__footer-note {
  max-width: 64rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.live-operations-page__grid {
  display: grid;
  grid-template-columns: 1.02fr 0.98fr;
  gap: 1rem;
  align-items: start;
}

@media (max-width: 1180px) {
  .live-operations-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
