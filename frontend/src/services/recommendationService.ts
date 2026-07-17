import { apiClient } from '@/services/http/apiClient';
import type {
  BusinessAssistantResponse,
  CampaignRecommendationView,
  MemberRecommendationView,
  ProductRecommendationView,
  RecommendationOverviewView
} from '@/services/apiTypes';

interface RecommendationQuery {
  storeId?: string;
  limit?: number;
}

function buildQuery(query: RecommendationQuery): string {
  const searchParams = new URLSearchParams();

  Object.entries(query).forEach(([key, value]) => {
    if (value == null || value === '') {
      return;
    }

    searchParams.set(key, String(value));
  });

  const serialized = searchParams.toString();
  return serialized ? `?${serialized}` : '';
}

export function getRecommendationOverview(storeId: string | undefined, token: string) {
  return apiClient.get<RecommendationOverviewView>(
    `/api/recommendations/overview${buildQuery({ storeId })}`,
    token
  );
}

export function getProductRecommendations(query: RecommendationQuery, token: string) {
  return apiClient.get<ProductRecommendationView[]>(
    `/api/recommendations/products${buildQuery(query)}`,
    token
  );
}

export function getMemberRecommendations(query: RecommendationQuery, token: string) {
  return apiClient.get<MemberRecommendationView[]>(
    `/api/recommendations/members${buildQuery(query)}`,
    token
  );
}

export function getCampaignRecommendations(query: RecommendationQuery, token: string) {
  return apiClient.get<CampaignRecommendationView[]>(
    `/api/recommendations/campaigns${buildQuery(query)}`,
    token
  );
}

export function askBusinessAssistant(
  payload: { storeId?: string; question: string },
  token: string
) {
  return apiClient.post<BusinessAssistantResponse>(
    '/api/recommendations/assistant/ask',
    payload,
    token
  );
}
