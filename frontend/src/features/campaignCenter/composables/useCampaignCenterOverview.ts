import { computed, shallowRef } from 'vue';

import {
  createCampaign,
  createCouponTemplate,
  getCampaign,
  getCampaigns,
  getCouponTemplates,
  publishCampaign,
  submitCampaignApproval,
  updateCampaign
} from '@/features/campaignCenter/services/campaignCenterService';
import { getStores } from '@/services/storeChannelService';
import type {
  CampaignActivity,
  CouponTemplate,
  CreateCampaignPayload,
  CreateCouponTemplatePayload,
  Store,
  UpdateCampaignPayload
} from '@/services/apiTypes';

export type CampaignComposerMode = 'create' | 'edit';

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function campaignPriority(campaign: CampaignActivity) {
  const status = normalizeText(campaign.status);

  if (status.includes('published')) {
    return 5;
  }

  if (status === 'approved') {
    return 4;
  }

  if (status.includes('pending')) {
    return 3;
  }

  if (status.includes('rejected')) {
    return 2;
  }

  return 1;
}

export function useCampaignCenterOverview() {
  const stores = shallowRef<Store[]>([]);
  const campaigns = shallowRef<CampaignActivity[]>([]);
  const couponTemplates = shallowRef<CouponTemplate[]>([]);
  const activeStoreId = shallowRef('all');
  const selectedCampaignId = shallowRef<string | null>(null);
  const selectedCampaignDetail = shallowRef<CampaignActivity | null>(null);
  const composerMode = shallowRef<CampaignComposerMode>('edit');
  const isLoading = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const sortedStores = computed(() =>
    [...stores.value].sort(
      (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const sortedCampaigns = computed(() =>
    [...campaigns.value].sort((left, right) => {
      const priorityGap = campaignPriority(right) - campaignPriority(left);
      if (priorityGap !== 0) {
        return priorityGap;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const sortedCouponTemplates = computed(() =>
    [...couponTemplates.value].sort((left, right) => {
      const enabledGap =
        Number(normalizeText(right.status) === 'enabled') -
        Number(normalizeText(left.status) === 'enabled');

      if (enabledGap !== 0) {
        return enabledGap;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const filteredCampaigns = computed(() => {
    if (activeStoreId.value === 'all') {
      return sortedCampaigns.value;
    }

    return sortedCampaigns.value.filter(
      (campaign) => campaign.storeId === activeStoreId.value
    );
  });

  const selectedCampaign = computed(() => {
    const source = selectedCampaignDetail.value ?? null;

    if (source && source.campaignId === selectedCampaignId.value) {
      return source;
    }

    return (
      filteredCampaigns.value.find(
        (campaign) => campaign.campaignId === selectedCampaignId.value
      ) ??
      sortedCampaigns.value.find(
        (campaign) => campaign.campaignId === selectedCampaignId.value
      ) ??
      null
    );
  });

  const selectedCouponTemplate = computed(() => {
    const couponTemplateId = selectedCampaign.value?.couponTemplateId;

    if (!couponTemplateId) {
      return null;
    }

    return (
      sortedCouponTemplates.value.find(
        (template) => template.couponTemplateId === couponTemplateId
      ) ?? null
    );
  });

  const selectedStoreName = computed(() => {
    const storeId =
      selectedCampaign.value?.storeId ??
      (activeStoreId.value !== 'all' ? activeStoreId.value : '');

    if (!storeId) {
      return '全部店铺';
    }

    return sortedStores.value.find((store) => store.storeId === storeId)?.shopName ?? storeId;
  });

  const availableCouponTemplates = computed(() => {
    const scopedStoreId =
      selectedCampaign.value?.storeId ??
      (activeStoreId.value !== 'all' ? activeStoreId.value : '');

    if (!scopedStoreId) {
      return sortedCouponTemplates.value;
    }

    return sortedCouponTemplates.value.filter(
      (template) => template.storeId === scopedStoreId
    );
  });

  const summary = computed(() => ({
    totalCampaigns: campaigns.value.length,
    pendingApprovalCount: campaigns.value.filter((campaign) =>
      normalizeText(campaign.status).includes('pending')
    ).length,
    approvedCount: campaigns.value.filter(
      (campaign) => normalizeText(campaign.status) === 'approved'
    ).length,
    publishedCount: campaigns.value.filter(
      (campaign) => normalizeText(campaign.status) === 'published'
    ).length,
    couponBoundCount: campaigns.value.filter(
      (campaign) => Boolean(campaign.couponTemplateId)
    ).length,
    enabledTemplateCount: couponTemplates.value.filter(
      (template) => normalizeText(template.status) === 'enabled'
    ).length
  }));

  function resetSelectedDetail() {
    selectedCampaignDetail.value = null;
  }

  function applySelection(preferredCampaignId?: string | null) {
    const nextCampaignId =
      preferredCampaignId &&
      campaigns.value.some((campaign) => campaign.campaignId === preferredCampaignId)
        ? preferredCampaignId
        : filteredCampaigns.value[0]?.campaignId ??
          sortedCampaigns.value[0]?.campaignId ??
          null;

    selectedCampaignId.value = nextCampaignId;
    resetSelectedDetail();
  }

  async function refreshSelectedContext(token: string) {
    if (!selectedCampaignId.value) {
      resetSelectedDetail();
      return;
    }

    selectedCampaignDetail.value = await getCampaign(selectedCampaignId.value, token);
  }

  async function load(token: string, preferredCampaignId?: string | null) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [storesPayload, campaignsPayload, couponTemplatesPayload] = await Promise.all([
        getStores(token),
        getCampaigns(token),
        getCouponTemplates(token)
      ]);

      stores.value = storesPayload;
      campaigns.value = campaignsPayload;
      couponTemplates.value = couponTemplatesPayload;

      if (
        activeStoreId.value !== 'all' &&
        !storesPayload.some((store) => store.storeId === activeStoreId.value)
      ) {
        activeStoreId.value = storesPayload[0]?.storeId ?? 'all';
      }

      if (activeStoreId.value === 'all' && storesPayload.length === 1) {
        activeStoreId.value = storesPayload[0].storeId;
      }

      applySelection(preferredCampaignId ?? selectedCampaignId.value);
      await refreshSelectedContext(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '营销活动中心加载失败，请稍后重试';
    } finally {
      isLoading.value = false;
    }
  }

  async function selectStore(storeId: string, token: string) {
    activeStoreId.value = storeId;
    applySelection(selectedCampaignId.value);
    await refreshSelectedContext(token);
  }

  async function selectCampaign(campaignId: string, token: string) {
    selectedCampaignId.value = campaignId;
    resetSelectedDetail();
    await refreshSelectedContext(token);
  }

  function openCreateCampaign() {
    composerMode.value = 'create';
    actionError.value = null;
  }

  function openEditCampaign() {
    if (!selectedCampaign.value) {
      return;
    }

    composerMode.value = 'edit';
    actionError.value = null;
  }

  async function runAction(callback: () => Promise<void>) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      await callback();
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '营销活动操作失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitCreateCampaign(payload: CreateCampaignPayload, token: string) {
    await runAction(async () => {
      const created = await createCampaign(payload, token);
      actionFeedback.value = `已创建活动 ${created.activityName}，正在回读活动看板。`;
      composerMode.value = 'edit';

      if (activeStoreId.value === 'all' && payload.storeId) {
        activeStoreId.value = payload.storeId;
      }

      await load(token, created.campaignId);
    });
  }

  async function submitUpdateCampaign(payload: UpdateCampaignPayload, token: string) {
    if (!selectedCampaignId.value) {
      return;
    }

    await runAction(async () => {
      const updated = await updateCampaign(selectedCampaignId.value!, payload, token);
      actionFeedback.value = `已更新活动 ${updated.activityName} 的编排内容。`;
      await load(token, updated.campaignId);
    });
  }

  async function submitApproval(token: string) {
    if (!selectedCampaignId.value) {
      return;
    }

    await runAction(async () => {
      const updated = await submitCampaignApproval(selectedCampaignId.value!, token);
      actionFeedback.value = `已提交活动 ${updated.activityName} 的审批流。`;
      await load(token, updated.campaignId);
    });
  }

  async function submitPublish(token: string) {
    if (!selectedCampaignId.value) {
      return;
    }

    await runAction(async () => {
      const updated = await publishCampaign(selectedCampaignId.value!, token);
      actionFeedback.value = `活动 ${updated.activityName} 已发布到执行轨道。`;
      await load(token, updated.campaignId);
    });
  }

  async function submitCouponTemplate(
    payload: CreateCouponTemplatePayload,
    token: string
  ) {
    await runAction(async () => {
      const created = await createCouponTemplate(payload, token);
      actionFeedback.value = `已新增优惠券模板 ${created.templateName}。`;

      if (activeStoreId.value === 'all' && payload.storeId) {
        activeStoreId.value = payload.storeId;
      }

      await load(token, selectedCampaignId.value);
    });
  }

  return {
    stores: sortedStores,
    campaigns: sortedCampaigns,
    filteredCampaigns,
    couponTemplates: sortedCouponTemplates,
    availableCouponTemplates,
    activeStoreId,
    selectedCampaignId,
    selectedCampaign,
    selectedCouponTemplate,
    selectedStoreName,
    summary,
    composerMode,
    isLoading,
    isRunningAction,
    errorMessage,
    actionError,
    actionFeedback,
    load,
    selectStore,
    selectCampaign,
    openCreateCampaign,
    openEditCampaign,
    submitCreateCampaign,
    submitUpdateCampaign,
    submitApproval,
    submitPublish,
    submitCouponTemplate
  };
}
