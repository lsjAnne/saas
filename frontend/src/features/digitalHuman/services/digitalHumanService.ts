import { apiClient } from '@/services/http/apiClient';
import type {
  CreateDigitalHumanAvatarPayload,
  CreateDigitalHumanFaqPayload,
  CreateDigitalHumanSchedulePayload,
  CreateDigitalHumanScriptPayload,
  DigitalHumanAvatar,
  DigitalHumanBarrage,
  DigitalHumanFaq,
  DigitalHumanInteraction,
  DigitalHumanLearningItem,
  DigitalHumanOverview,
  DigitalHumanSchedule,
  DigitalHumanScript,
  DigitalHumanSession,
  InteractionOverview,
  InteractionReplyView,
  LearningGrowth,
  SimulateDigitalHumanBarragePayload,
  SubmitDigitalHumanBarragePayload,
  BarrageConnectionStatus,
} from '@/features/digitalHuman/types';

export function getDigitalHumanOverview(token: string) {
  return apiClient.get<DigitalHumanOverview>('/api/digital-humans/overview', token);
}

export function listDigitalHumanAvatars(token: string) {
  return apiClient.get<DigitalHumanAvatar[]>('/api/digital-humans/avatars', token);
}

export function createDigitalHumanAvatar(payload: CreateDigitalHumanAvatarPayload, token: string) {
  return apiClient.post<DigitalHumanAvatar>('/api/digital-humans/avatars', payload, token);
}

export function updateDigitalHumanAvatar(
  avatarId: string,
  payload: CreateDigitalHumanAvatarPayload,
  token: string,
) {
  return apiClient.put<DigitalHumanAvatar>(`/api/digital-humans/avatars/${avatarId}`, payload, token);
}

export function deleteDigitalHumanAvatar(avatarId: string, token: string) {
  return apiClient.delete(`/api/digital-humans/avatars/${avatarId}`, token);
}

export function listDigitalHumanScripts(token: string) {
  return apiClient.get<DigitalHumanScript[]>('/api/digital-humans/scripts', token);
}

export function createDigitalHumanScript(payload: CreateDigitalHumanScriptPayload, token: string) {
  return apiClient.post<DigitalHumanScript>('/api/digital-humans/scripts', payload, token);
}

export function updateDigitalHumanScript(
  scriptId: string,
  payload: CreateDigitalHumanScriptPayload,
  token: string,
) {
  return apiClient.put<DigitalHumanScript>(`/api/digital-humans/scripts/${scriptId}`, payload, token);
}

export function listDigitalHumanSchedules(token: string) {
  return apiClient.get<DigitalHumanSchedule[]>('/api/digital-humans/schedules', token);
}

export function createDigitalHumanSchedule(payload: CreateDigitalHumanSchedulePayload, token: string) {
  return apiClient.post<DigitalHumanSchedule>('/api/digital-humans/schedules', payload, token);
}

export function updateDigitalHumanSchedule(
  scheduleId: string,
  payload: CreateDigitalHumanSchedulePayload,
  token: string,
) {
  return apiClient.put<DigitalHumanSchedule>(`/api/digital-humans/schedules/${scheduleId}`, payload, token);
}

export function startDigitalHumanSchedule(scheduleId: string, token: string) {
  return apiClient.post<DigitalHumanSession>(`/api/digital-humans/schedules/${scheduleId}/start`, null, token);
}

export function stopDigitalHumanSchedule(scheduleId: string, token: string) {
  return apiClient.post<DigitalHumanSession>(`/api/digital-humans/schedules/${scheduleId}/stop`, null, token);
}

export function listDigitalHumanSessions(token: string) {
  return apiClient.get<DigitalHumanSession[]>('/api/digital-humans/sessions', token);
}

export function heartbeatDigitalHumanSession(sessionId: string, token: string) {
  return apiClient.post<DigitalHumanSession>(`/api/digital-humans/sessions/${sessionId}/heartbeat`, null, token);
}

export function listDigitalHumanFaqs(token: string) {
  return apiClient.get<DigitalHumanFaq[]>('/api/digital-humans/faqs', token);
}

export function createDigitalHumanFaq(payload: CreateDigitalHumanFaqPayload, token: string) {
  return apiClient.post<DigitalHumanFaq>('/api/digital-humans/faqs', payload, token);
}

export function updateDigitalHumanFaq(faqId: string, payload: CreateDigitalHumanFaqPayload, token: string) {
  return apiClient.put<DigitalHumanFaq>(`/api/digital-humans/faqs/${faqId}`, payload, token);
}

export function deleteDigitalHumanFaq(faqId: string, token: string) {
  return apiClient.delete(`/api/digital-humans/faqs/${faqId}`, token);
}

export function listDigitalHumanBarrages(token: string, sessionId?: string) {
  const query = sessionId ? `?sessionId=${encodeURIComponent(sessionId)}` : '';
  return apiClient.get<DigitalHumanBarrage[]>(`/api/digital-humans/barrages${query}`, token);
}

export function submitDigitalHumanBarrage(payload: SubmitDigitalHumanBarragePayload, token: string) {
  return apiClient.post<InteractionReplyView>('/api/digital-humans/barrages', payload, token);
}

export function simulateDigitalHumanBarrages(payload: SimulateDigitalHumanBarragePayload, token: string) {
  return apiClient.post<InteractionReplyView[]>('/api/digital-humans/barrages/simulate', payload, token);
}

export function listDigitalHumanInteractions(sessionId: string, token: string) {
  return apiClient.get<DigitalHumanInteraction[]>(`/api/digital-humans/sessions/${sessionId}/interactions`, token);
}

export function listRecentDigitalHumanInteractions(limit: number, token: string) {
  return apiClient.get<DigitalHumanInteraction[]>(
    `/api/digital-humans/interactions/recent?limit=${limit}`,
    token,
  );
}

export function getDigitalHumanInteractionOverview(token: string) {
  return apiClient.get<InteractionOverview>('/api/digital-humans/interaction-overview', token);
}

export function listDigitalHumanLearningItems(token: string) {
  return apiClient.get<DigitalHumanLearningItem[]>('/api/digital-humans/learning/items', token);
}

export function confirmDigitalHumanLearningItem(itemId: string, token: string) {
  return apiClient.post<DigitalHumanLearningItem>(`/api/digital-humans/learning/items/${itemId}/confirm`, null, token);
}

export function ignoreDigitalHumanLearningItem(itemId: string, token: string) {
  return apiClient.post<DigitalHumanLearningItem>(`/api/digital-humans/learning/items/${itemId}/ignore`, null, token);
}

export function getDigitalHumanLearningGrowth(token: string) {
  return apiClient.get<LearningGrowth>('/api/digital-humans/learning/growth', token);
}

// ---------- 弹幕源接入（真实直播平台弹幕拉流） ----------

export function listBarrageConnections(token: string) {
  return apiClient.get<BarrageConnectionStatus[]>('/api/live/barrage-connections', token);
}

export function startBarrageConnection(platformType: string, token: string) {
  return apiClient.post<BarrageConnectionStatus[]>(
    `/api/live/barrage-connections/${platformType}/start`,
    null,
    token,
  );
}

export function stopBarrageConnection(platformType: string, token: string) {
  return apiClient.post<BarrageConnectionStatus[]>(
    `/api/live/barrage-connections/${platformType}/stop`,
    null,
    token,
  );
}
