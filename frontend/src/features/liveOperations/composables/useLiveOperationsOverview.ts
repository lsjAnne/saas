import { computed, shallowRef } from 'vue';

import { ApiError } from '@/services/http/apiClient';
import type {
  ConfigureCommitmentWhitelistPayload,
  EnableControlModePayload,
  InteractionReplyPayload,
  LiveAccountGovernanceView,
  LiveCommitmentWhitelistConfigView,
  LiveConcurrencyCheckView,
  LiveConcurrencyOverviewView,
  LiveConcurrencyQueueView,
  LiveInteractionReplyView,
  LivePlan,
  LiveRiskEventView,
  LiveRiskRecoveryPlanView,
  LiveSession,
  LiveSessionStatusView,
  LiveSpecialAnalysisDrilldownView,
  LiveSpecialAnalysisView,
  ManualTakeoverPayload,
  PromiseAuditPayload,
  SimulateLiveCallbackPayload,
  SkipCurrentProductResultView,
  StrongControlPayload,
  SwitchLiveScenePayload
} from '@/services/apiTypes';
import {
  configureLiveCommitmentWhitelist,
  enableLiveControlMode,
  getLiveAccountGovernance,
  getLiveConcurrencyOverview,
  getLiveConcurrencyQueues,
  getLivePlans,
  getLiveRiskEvents,
  getLiveRiskRecoveryPlans,
  getLiveSessionRiskEvents,
  getLiveSessionStatus,
  getLiveSessions,
  getLiveSpecialAnalysis,
  getLiveSpecialAnalysisDrilldown,
  manualTakeoverLiveSession,
  pauseLivePlan,
  promiseAuditLiveSession,
  replyLiveInteraction,
  resumeLivePlan,
  resumeLiveSystemMode,
  runDueLivePlans,
  simulateLiveCallback,
  skipCurrentLiveProduct,
  startLivePlan,
  stopLivePlan,
  strongControlLiveSession,
  switchLiveScene,
  validateLiveConcurrency
} from '@/features/liveOperations/services/liveOperationsService';

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function isObjectRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function sessionPriority(session: LiveSession) {
  const status = normalizeText(session.sessionStatus);

  if (status.includes('running') || status.includes('live')) {
    return 3;
  }

  if (status.includes('pause')) {
    return 2;
  }

  if (status.includes('fail') || status.includes('error')) {
    return 1;
  }

  return 0;
}

function planPriority(plan: LivePlan) {
  const status = normalizeText(plan.planStatus);

  if (status.includes('running') || status.includes('live')) {
    return 3;
  }

  if (status.includes('scheduled') || status.includes('published') || status.includes('ready')) {
    return 2;
  }

  if (status.includes('draft')) {
    return 1;
  }

  return 0;
}

function formatReplyRiskMessage(error: ApiError) {
  const data = isObjectRecord(error.data) ? error.data : {};
  const commitmentType = typeof data.commitmentType === 'string' ? data.commitmentType : 'unknown';

  if (error.code === '7107') {
    const approvalId = typeof data.approvalId === 'string' ? data.approvalId : '--';
    return `互动需人工确认，承诺类型 ${commitmentType}，审批单 ${approvalId}。`;
  }

  if (error.code === '7106') {
    const reason = typeof data.reason === 'string' ? data.reason : error.message;
    return `互动已拦截，承诺类型 ${commitmentType}，原因 ${reason}。`;
  }

  return error.message;
}

export function useLiveOperationsOverview() {
  const livePlans = shallowRef<LivePlan[]>([]);
  const liveSessions = shallowRef<LiveSession[]>([]);
  const concurrencyOverview = shallowRef<LiveConcurrencyOverviewView | null>(null);
  const concurrencyQueues = shallowRef<LiveConcurrencyQueueView[]>([]);
  const riskEvents = shallowRef<LiveRiskEventView[]>([]);
  const specialAnalysis = shallowRef<LiveSpecialAnalysisView | null>(null);
  const specialAnalysisDrilldown = shallowRef<LiveSpecialAnalysisDrilldownView | null>(null);
  const riskRecoveryPlans = shallowRef<LiveRiskRecoveryPlanView[]>([]);
  const accountGovernance = shallowRef<LiveAccountGovernanceView[]>([]);
  const latestSkipResult = shallowRef<SkipCurrentProductResultView | null>(null);
  const latestInteractionReply = shallowRef<LiveInteractionReplyView | null>(null);
  const latestCommitmentWhitelistConfig = shallowRef<LiveCommitmentWhitelistConfigView | null>(null);
  const selectedPlanId = shallowRef<string | null>(null);
  const selectedSessionId = shallowRef<string | null>(null);
  const selectedSessionStatus = shallowRef<LiveSessionStatusView | null>(null);
  const selectedSessionRiskEvents = shallowRef<LiveRiskEventView[]>([]);
  const concurrencyCheck = shallowRef<LiveConcurrencyCheckView | null>(null);
  const actionFeedback = shallowRef<string | null>(null);
  const isLoading = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);

  const sortedPlans = computed(() =>
    [...livePlans.value].sort((left, right) => {
      const priorityGap = planPriority(right) - planPriority(left);
      if (priorityGap !== 0) {
        return priorityGap;
      }

      return toTimestamp(right.scheduledStartAt) - toTimestamp(left.scheduledStartAt);
    })
  );

  const sortedSessions = computed(() =>
    [...liveSessions.value].sort((left, right) => {
      const priorityGap = sessionPriority(right) - sessionPriority(left);
      if (priorityGap !== 0) {
        return priorityGap;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const selectedPlan = computed(
    () => sortedPlans.value.find((plan) => plan.livePlanId === selectedPlanId.value) ?? null
  );

  const selectedSession = computed(
    () => sortedSessions.value.find((session) => session.liveSessionId === selectedSessionId.value) ?? null
  );

  const riskSummary = computed(() => ({
    blockedQueueCount: concurrencyQueues.value.length,
    highSeverityCount: riskEvents.value.filter((event) => {
      const severity = normalizeText(event.severity);
      return severity.includes('high') || severity.includes('critical');
    }).length,
    manualReviewCount: riskRecoveryPlans.value.filter((plan) => plan.requiresManualReview).length,
    occupiedAccountCount: accountGovernance.value.filter((account) => account.occupied || account.expiringSoon)
      .length
  }));

  function applySelections() {
    if (!selectedPlanId.value || !livePlans.value.some((plan) => plan.livePlanId === selectedPlanId.value)) {
      selectedPlanId.value = sortedPlans.value[0]?.livePlanId ?? null;
    }

    if (
      !selectedSessionId.value ||
      !liveSessions.value.some((session) => session.liveSessionId === selectedSessionId.value)
    ) {
      selectedSessionId.value = sortedSessions.value[0]?.liveSessionId ?? null;
    }
  }

  async function refreshSelectedSessionStatus(token: string) {
    if (!selectedSessionId.value) {
      selectedSessionStatus.value = null;
      selectedSessionRiskEvents.value = [];
      return;
    }

    const [statusPayload, sessionRiskEventsPayload] = await Promise.all([
      getLiveSessionStatus(selectedSessionId.value, token),
      getLiveSessionRiskEvents(selectedSessionId.value, token)
    ]);
    selectedSessionStatus.value = statusPayload;
    selectedSessionRiskEvents.value = sessionRiskEventsPayload;
  }

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [
        plansPayload,
        sessionsPayload,
        concurrencyPayload,
        queuesPayload,
        risksPayload,
        specialPayload,
        drilldownPayload,
        recoveryPayload,
        governancePayload
      ] = await Promise.all([
        getLivePlans(token),
        getLiveSessions(token),
        getLiveConcurrencyOverview(token),
        getLiveConcurrencyQueues(token),
        getLiveRiskEvents(token),
        getLiveSpecialAnalysis(token),
        getLiveSpecialAnalysisDrilldown(token),
        getLiveRiskRecoveryPlans(token),
        getLiveAccountGovernance(token)
      ]);

      livePlans.value = plansPayload;
      liveSessions.value = sessionsPayload;
      concurrencyOverview.value = concurrencyPayload;
      concurrencyQueues.value = queuesPayload;
      riskEvents.value = risksPayload;
      specialAnalysis.value = specialPayload;
      specialAnalysisDrilldown.value = drilldownPayload;
      riskRecoveryPlans.value = recoveryPayload;
      accountGovernance.value = governancePayload;

      applySelections();
      await refreshSelectedSessionStatus(token);
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '直播运营中心加载失败，请稍后重试';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  async function selectSession(sessionId: string, token: string) {
    selectedSessionId.value = sessionId;
    latestSkipResult.value = null;
    latestInteractionReply.value = null;
    await refreshSelectedSessionStatus(token);
  }

  function selectPlan(planId: string) {
    selectedPlanId.value = planId;
    concurrencyCheck.value = null;
    actionFeedback.value = null;
    latestCommitmentWhitelistConfig.value = null;
  }

  async function validateSelectedPlan(token: string) {
    if (!selectedPlan.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      concurrencyCheck.value = await validateLiveConcurrency(selectedPlan.value.livePlanId, token);
      actionFeedback.value = concurrencyCheck.value.allowed
        ? '并发校验通过，可以进入开播。'
        : concurrencyCheck.value.reason || '并发校验未通过。';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function startSelectedPlan(token: string) {
    if (!selectedPlan.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      const started = await startLivePlan(selectedPlan.value.livePlanId, token);
      selectedSessionId.value = started.liveSessionId;
      latestSkipResult.value = null;
      actionFeedback.value = '直播已开始，正在刷新会话状态。';
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  async function runDue(token: string) {
    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      const result = await runDueLivePlans(token);
      actionFeedback.value = `已扫描 ${result.scannedCount} 个计划，启动 ${result.startedCount} 个，失败 ${result.failedCount} 个。`;
      if (result.startedSessions[0]) {
        selectedSessionId.value = result.startedSessions[0].liveSessionId;
      }
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  async function pauseSelectedSession(token: string) {
    if (!selectedSession.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      await pauseLivePlan(selectedSession.value.livePlanId, token);
      latestSkipResult.value = null;
      actionFeedback.value = '直播会话已暂停。';
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  async function resumeSelectedSession(token: string) {
    if (!selectedSession.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      await resumeLivePlan(selectedSession.value.livePlanId, token);
      latestSkipResult.value = null;
      actionFeedback.value = '直播会话已恢复。';
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  async function stopSelectedSession(token: string) {
    if (!selectedSession.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      await stopLivePlan(selectedSession.value.livePlanId, token);
      latestSkipResult.value = null;
      actionFeedback.value = '直播会话已停止。';
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  async function skipSelectedSessionProduct(token: string) {
    if (!selectedSessionId.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      latestSkipResult.value = await skipCurrentLiveProduct(selectedSessionId.value, token);
      actionFeedback.value = `已跳过当前商品，脚本已切到 ${latestSkipResult.value.activeScriptVersion}。`;
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  async function switchSelectedSessionScene(payload: SwitchLiveScenePayload, token: string) {
    if (!selectedSessionId.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      await switchLiveScene(selectedSessionId.value, payload, token);
      actionFeedback.value = `当前会话已切换到场景 ${payload.targetScene}。`;
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  async function strongControlSelectedSession(payload: StrongControlPayload, token: string) {
    if (!selectedSessionId.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      await strongControlLiveSession(selectedSessionId.value, payload, token);
      actionFeedback.value = `强控模式已更新为 ${payload.controlMode}。`;
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  async function enableSelectedSessionControlMode(payload: EnableControlModePayload, token: string) {
    if (!selectedSessionId.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      const result = await enableLiveControlMode(selectedSessionId.value, payload, token);
      actionFeedback.value = `会话控场模式已切换为 ${result.controlMode}。`;
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  async function manualTakeoverSelectedSession(payload: ManualTakeoverPayload, token: string) {
    if (!selectedSessionId.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      await manualTakeoverLiveSession(selectedSessionId.value, payload, token);
      actionFeedback.value = `已切换为人工接管，接管人 ${payload.takeoverOperator}。`;
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  async function promiseAuditSelectedSession(payload: PromiseAuditPayload, token: string) {
    if (!selectedSessionId.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      await promiseAuditLiveSession(selectedSessionId.value, payload, token);
      actionFeedback.value = `承诺审核已提交，当前结论 ${payload.decision}。`;
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  async function resumeSelectedSessionSystemMode(token: string) {
    if (!selectedSessionId.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      const result = await resumeLiveSystemMode(selectedSessionId.value, token);
      actionFeedback.value = `会话已恢复系统模式，当前控场模式 ${result.controlMode}。`;
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  async function configureSelectedPlanCommitmentWhitelist(
    payload: ConfigureCommitmentWhitelistPayload,
    token: string
  ) {
    if (!selectedPlan.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      latestCommitmentWhitelistConfig.value = await configureLiveCommitmentWhitelist(
        selectedPlan.value.livePlanId,
        payload,
        token
      );
      actionFeedback.value =
        `承诺白名单已更新：白名单 ${latestCommitmentWhitelistConfig.value.whitelistPhrases.length} 条，` +
        `人工确认 ${latestCommitmentWhitelistConfig.value.manualConfirmPhrases.length} 条，` +
        `禁止承诺 ${latestCommitmentWhitelistConfig.value.prohibitedPhrases.length} 条。`;
    } finally {
      isRunningAction.value = false;
    }
  }

  async function replySelectedSession(payload: InteractionReplyPayload, token: string) {
    if (!selectedSessionId.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      latestInteractionReply.value = await replyLiveInteraction(selectedSessionId.value, payload, token);
      actionFeedback.value = `互动 ${latestInteractionReply.value.interactionEventId} 已自动回复。`;
      await load(token);
    } catch (error) {
      if (error instanceof ApiError && ['7106', '7107'].includes(error.code ?? '')) {
        latestInteractionReply.value = null;
        actionFeedback.value = formatReplyRiskMessage(error);
        await load(token);
        return;
      }

      throw error;
    } finally {
      isRunningAction.value = false;
    }
  }

  async function simulateSelectedSessionCallback(payload: SimulateLiveCallbackPayload, token: string) {
    if (!selectedSessionId.value) {
      return;
    }

    isRunningAction.value = true;
    actionFeedback.value = null;

    try {
      await simulateLiveCallback(selectedSessionId.value, payload, token);
      actionFeedback.value = `已模拟回调事件 ${payload.eventType}。`;
      await load(token);
    } finally {
      isRunningAction.value = false;
    }
  }

  return {
    livePlans: sortedPlans,
    liveSessions: sortedSessions,
    concurrencyOverview,
    concurrencyQueues,
    riskEvents,
    specialAnalysis,
    specialAnalysisDrilldown,
    riskRecoveryPlans,
    accountGovernance,
    latestSkipResult,
    latestInteractionReply,
    latestCommitmentWhitelistConfig,
    selectedPlanId,
    selectedPlan,
    selectedSessionId,
    selectedSession,
    selectedSessionStatus,
    selectedSessionRiskEvents,
    concurrencyCheck,
    riskSummary,
    actionFeedback,
    isLoading,
    isRunningAction,
    errorMessage,
    load,
    selectPlan,
    selectSession,
    validateSelectedPlan,
    startSelectedPlan,
    runDue,
    pauseSelectedSession,
    resumeSelectedSession,
    stopSelectedSession,
    skipSelectedSessionProduct,
    switchSelectedSessionScene,
    strongControlSelectedSession,
    enableSelectedSessionControlMode,
    manualTakeoverSelectedSession,
    promiseAuditSelectedSession,
    resumeSelectedSessionSystemMode,
    configureSelectedPlanCommitmentWhitelist,
    replySelectedSession,
    simulateSelectedSessionCallback
  };
}
