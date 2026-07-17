import { computed, shallowRef } from 'vue';

import {
  getCampaignRecommendations,
  getMemberRecommendations,
  getProductRecommendations,
  getRecommendationOverview
} from '@/services/recommendationService';
import { getStores } from '@/services/storeChannelService';
import type {
  CampaignRecommendationView,
  MemberRecommendationView,
  ProductRecommendationView,
  RecommendationOverviewView,
  Store
} from '@/services/apiTypes';

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

export function useBusinessAssistantOverview() {
  const stores = shallowRef<Store[]>([]);
  const overview = shallowRef<RecommendationOverviewView | null>(null);
  const productRecommendations = shallowRef<ProductRecommendationView[]>([]);
  const memberRecommendations = shallowRef<MemberRecommendationView[]>([]);
  const campaignRecommendations = shallowRef<CampaignRecommendationView[]>([]);
  const activeStoreId = shallowRef('all');
  const isLoading = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);

  const sortedStores = computed(() =>
    [...stores.value].sort(
      (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const selectedStoreName = computed(() => {
    if (activeStoreId.value === 'all') {
      return '全部店铺';
    }

    return (
      sortedStores.value.find((store) => store.storeId === activeStoreId.value)?.shopName ??
      activeStoreId.value
    );
  });

  const topProduct = computed(() => productRecommendations.value[0] ?? null);
  const topMember = computed(() => memberRecommendations.value[0] ?? null);
  const topCampaign = computed(() => campaignRecommendations.value[0] ?? null);

  const summary = computed(() => ({
    totalRecommendationCount:
      (overview.value?.productRecommendationCount ?? 0) +
      (overview.value?.memberRecommendationCount ?? 0) +
      (overview.value?.campaignRecommendationCount ?? 0),
    highRiskProductCount: productRecommendations.value.filter(
      (item) => (item.riskLevel ?? '').toLowerCase() === 'high'
    ).length,
    reviveVipCount: memberRecommendations.value.filter(
      (item) => item.segmentCode === 'revive_vip'
    ).length,
    comboCampaignCount: campaignRecommendations.value.filter(
      (item) => item.recommendedCandidateProductIds.length > 0
    ).length
  }));

  function currentStoreId() {
    return activeStoreId.value === 'all' ? undefined : activeStoreId.value;
  }

  async function refreshRecommendations(token: string) {
    const storeId = currentStoreId();
    const [overviewPayload, productsPayload, membersPayload, campaignsPayload] =
      await Promise.all([
        getRecommendationOverview(storeId, token),
        getProductRecommendations({ storeId, limit: 6 }, token),
        getMemberRecommendations({ storeId, limit: 6 }, token),
        getCampaignRecommendations({ storeId, limit: 6 }, token)
      ]);

    overview.value = overviewPayload;
    productRecommendations.value = productsPayload;
    memberRecommendations.value = membersPayload;
    campaignRecommendations.value = campaignsPayload;
  }

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const storesPayload = await getStores(token);
      stores.value = storesPayload;

      if (
        activeStoreId.value !== 'all' &&
        !storesPayload.some((store) => store.storeId === activeStoreId.value)
      ) {
        activeStoreId.value = storesPayload[0]?.storeId ?? 'all';
      }

      if (activeStoreId.value === 'all' && storesPayload.length === 1) {
        activeStoreId.value = storesPayload[0].storeId;
      }

      await refreshRecommendations(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '经营助手加载失败，请稍后重试';
    } finally {
      isLoading.value = false;
    }
  }

  async function selectStore(storeId: string, token: string) {
    activeStoreId.value = storeId;
    await refreshRecommendations(token);
  }

  return {
    stores: sortedStores,
    overview,
    productRecommendations,
    memberRecommendations,
    campaignRecommendations,
    topProduct,
    topMember,
    topCampaign,
    summary,
    selectedStoreName,
    activeStoreId,
    isLoading,
    errorMessage,
    load,
    selectStore
  };
}
