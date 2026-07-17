import { apiClient } from '@/services/http/apiClient';
import type {
  ConfigureCommitmentWhitelistPayload,
  EnableControlModePayload,
  LiveAccountGovernanceView,
  LiveCommitmentWhitelistConfigView,
  LiveConcurrencyCheckView,
  LiveConcurrencyOverviewView,
  LiveConcurrencyQueueView,
  LiveControlModeOperationView,
  LiveInteractionReplyView,
  LivePlan,
  LiveRiskEventView,
  LiveRiskRecoveryPlanView,
  LiveSession,
  LiveSessionStatusView,
  InteractionReplyPayload,
  LiveSpecialAnalysisDrilldownView,
  LiveSpecialAnalysisView,
  ManualTakeoverPayload,
  PromiseAuditPayload,
  RunDueLivePlansResultView,
  SimulateLiveCallbackPayload,
  SkipCurrentProductResultView,
  StrongControlPayload,
  SwitchLiveScenePayload
} from '@/services/apiTypes';

export function getLivePlans(token: string) {
  return apiClient.get<LivePlan[]>('/api/live-plans', token);
}

export function getLiveSessions(token: string) {
  return apiClient.get<LiveSession[]>('/api/live-sessions', token);
}

export function getLiveSessionStatus(liveSessionId: string, token: string) {
  return apiClient.get<LiveSessionStatusView>(`/api/live-sessions/${liveSessionId}/status`, token);
}

export function getLiveConcurrencyOverview(token: string) {
  return apiClient.get<LiveConcurrencyOverviewView>('/api/live-sessions/concurrency-overview', token);
}

export function getLiveConcurrencyQueues(token: string) {
  return apiClient.get<LiveConcurrencyQueueView[]>('/api/live-concurrency-queues', token);
}

export function getLiveRiskEvents(token: string) {
  return apiClient.get<LiveRiskEventView[]>('/api/live-risk-events', token);
}

export function getLiveSessionRiskEvents(liveSessionId: string, token: string) {
  return apiClient.get<LiveRiskEventView[]>(`/api/live-sessions/${liveSessionId}/risk-events`, token);
}

export function getLiveSpecialAnalysis(token: string) {
  return apiClient.get<LiveSpecialAnalysisView>('/api/live-special-analysis', token);
}

export function getLiveSpecialAnalysisDrilldown(token: string) {
  return apiClient.get<LiveSpecialAnalysisDrilldownView>('/api/live-special-analysis/drilldown', token);
}

export function getLiveRiskRecoveryPlans(token: string) {
  return apiClient.get<LiveRiskRecoveryPlanView[]>('/api/live-risk-recovery-plans', token);
}

export function getLiveAccountGovernance(token: string) {
  return apiClient.get<LiveAccountGovernanceView[]>('/api/live-accounts/governance', token);
}

export function validateLiveConcurrency(livePlanId: string, token: string) {
  return apiClient.post<LiveConcurrencyCheckView>(`/api/live-plans/${livePlanId}/validate-concurrency`, null, token);
}

export function startLivePlan(livePlanId: string, token: string) {
  return apiClient.post<LiveSession>(`/api/live-plans/${livePlanId}/start`, null, token);
}

export function pauseLivePlan(livePlanId: string, token: string) {
  return apiClient.post<LiveSession>(`/api/live-plans/${livePlanId}/pause`, null, token);
}

export function resumeLivePlan(livePlanId: string, token: string) {
  return apiClient.post<LiveSession>(`/api/live-plans/${livePlanId}/resume`, null, token);
}

export function stopLivePlan(livePlanId: string, token: string) {
  return apiClient.post<LiveSession>(`/api/live-plans/${livePlanId}/stop`, null, token);
}

export function runDueLivePlans(token: string) {
  return apiClient.post<RunDueLivePlansResultView>('/api/live-plans/run-due', null, token);
}

export function skipCurrentLiveProduct(liveSessionId: string, token: string) {
  return apiClient.post<SkipCurrentProductResultView>(
    `/api/live-sessions/${liveSessionId}/skip-current-product`,
    null,
    token
  );
}

export function simulateLiveCallback(
  liveSessionId: string,
  payload: SimulateLiveCallbackPayload,
  token: string
) {
  return apiClient.post<LiveSession>(`/api/live-sessions/${liveSessionId}/simulate-callback`, payload, token);
}

export function strongControlLiveSession(
  liveSessionId: string,
  payload: StrongControlPayload,
  token: string
) {
  return apiClient.post<LiveSession>(`/api/live-sessions/${liveSessionId}/strong-control`, payload, token);
}

export function enableLiveControlMode(
  liveSessionId: string,
  payload: EnableControlModePayload,
  token: string
) {
  return apiClient.post<LiveControlModeOperationView>(
    `/api/live-sessions/${liveSessionId}/enable-control-mode`,
    payload,
    token
  );
}

export function switchLiveScene(
  liveSessionId: string,
  payload: SwitchLiveScenePayload,
  token: string
) {
  return apiClient.post<LiveSession>(`/api/live-sessions/${liveSessionId}/switch-scene`, payload, token);
}

export function manualTakeoverLiveSession(
  liveSessionId: string,
  payload: ManualTakeoverPayload,
  token: string
) {
  return apiClient.post<LiveSession>(`/api/live-sessions/${liveSessionId}/manual-takeover`, payload, token);
}

export function resumeLiveSystemMode(liveSessionId: string, token: string) {
  return apiClient.post<LiveControlModeOperationView>(
    `/api/live-sessions/${liveSessionId}/resume-system-mode`,
    null,
    token
  );
}

export function promiseAuditLiveSession(
  liveSessionId: string,
  payload: PromiseAuditPayload,
  token: string
) {
  return apiClient.post<LiveSession>(`/api/live-sessions/${liveSessionId}/promise-audit`, payload, token);
}

export function configureLiveCommitmentWhitelist(
  livePlanId: string,
  payload: ConfigureCommitmentWhitelistPayload,
  token: string
) {
  return apiClient.post<LiveCommitmentWhitelistConfigView>(
    `/api/live-plans/${livePlanId}/configure-commitment-whitelist`,
    payload,
    token
  );
}

export function replyLiveInteraction(
  liveSessionId: string,
  payload: InteractionReplyPayload,
  token: string
) {
  return apiClient.post<LiveInteractionReplyView>(
    `/api/live-sessions/${liveSessionId}/interaction/reply`,
    payload,
    token
  );
}
