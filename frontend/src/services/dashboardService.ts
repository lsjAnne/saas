import { apiClient } from '@/services/http/apiClient';
import type {
  DashboardCampaignAnalysis,
  DashboardMemberAnalysis,
  DashboardRisk,
  DashboardSummary,
  DashboardTrend
} from '@/services/apiTypes';

export function getDashboardSummary(token: string) {
  return apiClient.get<DashboardSummary>('/api/dashboard/summary', token);
}

export function getDashboardTrends(token: string) {
  return apiClient.get<DashboardTrend[]>('/api/dashboard/trends', token);
}

export function getDashboardRisks(token: string) {
  return apiClient.get<DashboardRisk[]>('/api/dashboard/risks', token);
}

export function getDashboardCampaignAnalysis(token: string) {
  return apiClient.get<DashboardCampaignAnalysis>('/api/dashboard/campaign-analysis', token);
}

export function getDashboardMemberAnalysis(token: string) {
  return apiClient.get<DashboardMemberAnalysis>('/api/dashboard/member-analysis', token);
}
