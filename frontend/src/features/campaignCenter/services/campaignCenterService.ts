import { apiClient } from '@/services/http/apiClient';
import type {
  CampaignActivity,
  CouponTemplate,
  CreateCampaignPayload,
  CreateCouponTemplatePayload,
  UpdateCampaignPayload
} from '@/services/apiTypes';

export function getCampaigns(token: string) {
  return apiClient.get<CampaignActivity[]>('/api/campaigns', token);
}

export function getCampaign(campaignId: string, token: string) {
  return apiClient.get<CampaignActivity>(`/api/campaigns/${campaignId}`, token);
}

export function createCampaign(payload: CreateCampaignPayload, token: string) {
  return apiClient.post<CampaignActivity>('/api/campaigns', payload, token);
}

export function updateCampaign(
  campaignId: string,
  payload: UpdateCampaignPayload,
  token: string
) {
  return apiClient.put<CampaignActivity>(`/api/campaigns/${campaignId}`, payload, token);
}

export function submitCampaignApproval(campaignId: string, token: string) {
  return apiClient.post<CampaignActivity>(
    `/api/campaigns/${campaignId}/submit-approval`,
    null,
    token
  );
}

export function publishCampaign(campaignId: string, token: string) {
  return apiClient.post<CampaignActivity>(`/api/campaigns/${campaignId}/publish`, null, token);
}

export function getCouponTemplates(token: string) {
  return apiClient.get<CouponTemplate[]>('/api/coupon-templates', token);
}

export function createCouponTemplate(
  payload: CreateCouponTemplatePayload,
  token: string
) {
  return apiClient.post<CouponTemplate>('/api/coupon-templates', payload, token);
}
