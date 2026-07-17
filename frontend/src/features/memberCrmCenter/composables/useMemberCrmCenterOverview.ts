import { computed, shallowRef } from 'vue';

import { getCampaigns } from '@/features/campaignCenter/services/campaignCenterService';
import {
  addMemberTag,
  createMemberSegmentRule,
  createMemberTouchTask,
  exportMemberGroup,
  executeMemberSegmentRule,
  getMember,
  getMemberCrmAnalysis,
  getMemberCrmLinkage,
  getMembers,
  getMemberTags,
  getMemberTouchTasks,
  removeMemberTag,
  saveMemberCrmProfile
} from '@/services/memberCrmCenterService';
import { getStores } from '@/services/storeChannelService';
import type {
  CampaignActivity,
  ExportMemberGroupPayload,
  MemberCrmAnalysisView,
  MemberCrmLinkageView,
  MemberCrmProfileView,
  MemberDetailView,
  MemberGroupExportView,
  MemberPageResult,
  MemberSegmentExecutionView,
  MemberSegmentRuleView,
  MemberTagSummaryView,
  MemberTouchTaskView,
  MemberView,
  SaveMemberCrmProfilePayload,
  SaveMemberSegmentRulePayload,
  SaveMemberTagPayload,
  SaveMemberTouchTaskPayload,
  Store
} from '@/services/apiTypes';

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function memberPriority(member: MemberView) {
  const lifecycle = normalizeText(member.lifecycleStage);
  const level = normalizeText(member.levelCode);

  if (lifecycle === 'dormant' && ['vip', 'svip'].includes(level)) {
    return 5;
  }

  if (member.totalPaidAmount >= 5000) {
    return 4;
  }

  if (lifecycle === 'silent') {
    return 3;
  }

  if (member.totalOrderCount >= 2) {
    return 2;
  }

  return 1;
}

export function useMemberCrmCenterOverview() {
  const stores = shallowRef<Store[]>([]);
  const members = shallowRef<MemberPageResult | null>(null);
  const memberTags = shallowRef<MemberTagSummaryView[]>([]);
  const crmAnalysis = shallowRef<MemberCrmAnalysisView | null>(null);
  const touchTasks = shallowRef<MemberTouchTaskView[]>([]);
  const campaigns = shallowRef<CampaignActivity[]>([]);
  const selectedMemberId = shallowRef<string | null>(null);
  const selectedMemberDetail = shallowRef<MemberDetailView | null>(null);
  const selectedCrmLinkage = shallowRef<MemberCrmLinkageView | null>(null);
  const crmProfilesByMemberId = shallowRef<Record<string, MemberCrmProfileView>>({});
  const sessionSegmentRules = shallowRef<MemberSegmentRuleView[]>([]);
  const sessionSegmentExecutions = shallowRef<MemberSegmentExecutionView[]>([]);
  const latestExport = shallowRef<MemberGroupExportView | null>(null);
  const activeStoreId = shallowRef('all');
  const levelFilter = shallowRef('all');
  const lifecycleFilter = shallowRef('all');
  const tagFilter = shallowRef('all');
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

  const sortedMembers = computed(() => {
    const source = members.value?.list ?? [];

    return [...source].sort((left, right) => {
      const priorityGap = memberPriority(right) - memberPriority(left);
      if (priorityGap !== 0) {
        return priorityGap;
      }

      return toTimestamp(right.lastOrderAt) - toTimestamp(left.lastOrderAt);
    });
  });

  const selectedMember = computed(() => {
    const detailMember = selectedMemberDetail.value?.member ?? null;

    if (detailMember && detailMember.memberId === selectedMemberId.value) {
      return detailMember;
    }

    return (
      sortedMembers.value.find((member) => member.memberId === selectedMemberId.value) ?? null
    );
  });

  const selectedCrmProfile = computed(
    () =>
      (selectedMemberId.value
        ? crmProfilesByMemberId.value[selectedMemberId.value]
        : null) ?? null
  );

  const selectedStoreName = computed(() => {
    const storeId =
      selectedMember.value?.storeId ??
      (activeStoreId.value !== 'all' ? activeStoreId.value : '');

    if (!storeId) {
      return '全部店铺';
    }

    return sortedStores.value.find((store) => store.storeId === storeId)?.shopName ?? storeId;
  });

  const availableLevels = computed(() =>
    [...new Set(sortedMembers.value.map((member) => member.levelCode).filter(Boolean))]
  );

  const availableLifecycleStages = computed(() =>
    [...new Set(sortedMembers.value.map((member) => member.lifecycleStage).filter(Boolean))]
  );

  const activeStoreScopedTagOptions = computed(() =>
    memberTags.value.filter((tag) =>
      tagFilter.value === 'all' ? true : true
    )
  );

  const selectedMemberTouchTasks = computed(() => {
    if (!selectedMemberId.value) {
      return touchTasks.value.slice(0, 8);
    }

    return touchTasks.value.filter((task) => task.memberId === selectedMemberId.value);
  });

  const publishedCampaignOptions = computed(() => {
    const scopedStoreId =
      selectedMember.value?.storeId ??
      (activeStoreId.value !== 'all' ? activeStoreId.value : '');

    return campaigns.value.filter((campaign) => {
      const storeMatched = scopedStoreId ? campaign.storeId === scopedStoreId : true;
      return storeMatched && normalizeText(campaign.status) === 'published';
    });
  });

  const summary = computed(() => ({
    totalMembers: crmAnalysis.value?.totalMembers ?? members.value?.total ?? 0,
    strategicMembers: crmAnalysis.value?.strategicMembers ?? 0,
    highValueMembers: crmAnalysis.value?.highValueMembers ?? 0,
    dormantMembers: crmAnalysis.value?.dormantMembers ?? 0,
    autoTaggedMembers: crmAnalysis.value?.autoTaggedMembers ?? 0,
    activeRecallTaskCount: crmAnalysis.value?.activeRecallTaskCount ?? 0
  }));

  function currentStoreId() {
    return activeStoreId.value === 'all' ? undefined : activeStoreId.value;
  }

  function currentTagCode() {
    return tagFilter.value === 'all' ? undefined : tagFilter.value;
  }

  function currentLevelCode() {
    return levelFilter.value === 'all' ? undefined : levelFilter.value;
  }

  function currentLifecycleStage() {
    return lifecycleFilter.value === 'all' ? undefined : lifecycleFilter.value;
  }

  function applySelection(preferredMemberId?: string | null) {
    const nextMemberId =
      preferredMemberId &&
      sortedMembers.value.some((member) => member.memberId === preferredMemberId)
        ? preferredMemberId
        : sortedMembers.value[0]?.memberId ?? null;

    selectedMemberId.value = nextMemberId;
    selectedMemberDetail.value = null;
    selectedCrmLinkage.value = null;
  }

  async function refreshCollection(token: string, preferredMemberId?: string | null) {
    const storeId = currentStoreId();
    const [membersPayload, memberTagsPayload, crmAnalysisPayload, touchTasksPayload, campaignsPayload] =
      await Promise.all([
        getMembers(
          {
            storeId,
            levelCode: currentLevelCode(),
            tagCode: currentTagCode(),
            lifecycleStage: currentLifecycleStage(),
            page: 1,
            pageSize: 100
          },
          token
        ),
        getMemberTags(storeId, token),
        getMemberCrmAnalysis(storeId, token),
        getMemberTouchTasks({ storeId }, token),
        getCampaigns(token)
      ]);

    members.value = membersPayload;
    memberTags.value = memberTagsPayload;
    crmAnalysis.value = crmAnalysisPayload;
    touchTasks.value = touchTasksPayload;
    campaigns.value = campaignsPayload;
    applySelection(preferredMemberId ?? selectedMemberId.value);
  }

  async function refreshSelectedContext(token: string) {
    if (!selectedMemberId.value) {
      selectedMemberDetail.value = null;
      selectedCrmLinkage.value = null;
      return;
    }

    const [memberDetailPayload, crmLinkagePayload] = await Promise.all([
      getMember(selectedMemberId.value, token),
      getMemberCrmLinkage(selectedMemberId.value, token)
    ]);

    selectedMemberDetail.value = memberDetailPayload;
    selectedCrmLinkage.value = crmLinkagePayload;
  }

  async function load(token: string, preferredMemberId?: string | null) {
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

      await refreshCollection(token, preferredMemberId);
      await refreshSelectedContext(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '会员与 CRM 中心加载失败，请稍后重试';
    } finally {
      isLoading.value = false;
    }
  }

  async function reloadWithCurrentFilters(token: string, preferredMemberId?: string | null) {
    await refreshCollection(token, preferredMemberId);
    await refreshSelectedContext(token);
  }

  async function selectMember(memberId: string, token: string) {
    selectedMemberId.value = memberId;
    selectedMemberDetail.value = null;
    selectedCrmLinkage.value = null;
    await refreshSelectedContext(token);
  }

  async function selectStore(storeId: string, token: string) {
    activeStoreId.value = storeId;
    await reloadWithCurrentFilters(token);
  }

  async function selectLevel(levelCode: string, token: string) {
    levelFilter.value = levelCode;
    await reloadWithCurrentFilters(token);
  }

  async function selectLifecycleStage(stage: string, token: string) {
    lifecycleFilter.value = stage;
    await reloadWithCurrentFilters(token);
  }

  async function selectTag(tagCode: string, token: string) {
    tagFilter.value = tagCode;
    await reloadWithCurrentFilters(token);
  }

  async function runAction(callback: () => Promise<void>) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      await callback();
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '会员 CRM 操作失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitCrmProfile(payload: SaveMemberCrmProfilePayload, token: string) {
    if (!selectedMemberId.value) {
      return;
    }

    await runAction(async () => {
      const profile = await saveMemberCrmProfile(selectedMemberId.value!, payload, token);
      crmProfilesByMemberId.value = {
        ...crmProfilesByMemberId.value,
        [profile.memberId]: profile
      };
      actionFeedback.value = `已更新会员 ${profile.customerName} 的 CRM 档案。`;
      await refreshSelectedContext(token);
    });
  }

  async function submitAddTag(payload: SaveMemberTagPayload, token: string) {
    if (!selectedMemberId.value) {
      return;
    }

    await runAction(async () => {
      await addMemberTag(selectedMemberId.value!, payload, token);
      actionFeedback.value = `已为当前会员打上标签 ${payload.tagName}。`;
      await reloadWithCurrentFilters(token, selectedMemberId.value);
    });
  }

  async function submitRemoveTag(memberTagId: string, token: string) {
    if (!selectedMemberId.value) {
      return;
    }

    await runAction(async () => {
      await removeMemberTag(selectedMemberId.value!, memberTagId, token);
      actionFeedback.value = '已移除当前会员标签。';
      await reloadWithCurrentFilters(token, selectedMemberId.value);
    });
  }

  async function submitSegmentRule(payload: SaveMemberSegmentRulePayload, token: string) {
    await runAction(async () => {
      const created = await createMemberSegmentRule(payload, token);
      sessionSegmentRules.value = [
        created,
        ...sessionSegmentRules.value.filter((rule) => rule.ruleId !== created.ruleId)
      ];
      actionFeedback.value = `已创建分群规则 ${created.ruleName}。`;
    });
  }

  async function runSegmentRule(ruleId: string, token: string) {
    await runAction(async () => {
      const execution = await executeMemberSegmentRule(ruleId, token);
      sessionSegmentExecutions.value = [
        execution,
        ...sessionSegmentExecutions.value.filter((item) => item.ruleId !== execution.ruleId)
      ];
      actionFeedback.value = `分群规则 ${execution.ruleName} 已执行，命中 ${execution.matchedMemberCount} 人。`;
      await reloadWithCurrentFilters(token, execution.matchedMemberIds[0] ?? selectedMemberId.value);
    });
  }

  async function submitTouchTask(payload: SaveMemberTouchTaskPayload, token: string) {
    await runAction(async () => {
      const created = await createMemberTouchTask(payload, token);
      actionFeedback.value = `已创建 ${created.taskType} 触达任务。`;
      await reloadWithCurrentFilters(token, created.memberId);
    });
  }

  async function submitExportGroup(token: string) {
    await runAction(async () => {
      const payload: ExportMemberGroupPayload = {
        storeId: currentStoreId(),
        levelCode: currentLevelCode(),
        tagCode: currentTagCode(),
        lifecycleStage: currentLifecycleStage()
      };

      latestExport.value = await exportMemberGroup(payload, token);
      actionFeedback.value = `已导出会员群组，共 ${latestExport.value.totalMembers} 人。`;
    });
  }

  return {
    stores: sortedStores,
    members,
    memberList: sortedMembers,
    memberTags,
    activeStoreScopedTagOptions,
    crmAnalysis,
    touchTasks,
    selectedMemberId,
    selectedMember,
    selectedMemberDetail,
    selectedCrmProfile,
    selectedCrmLinkage,
    selectedMemberTouchTasks,
    selectedStoreName,
    availableLevels,
    availableLifecycleStages,
    publishedCampaignOptions,
    sessionSegmentRules,
    sessionSegmentExecutions,
    latestExport,
    summary,
    activeStoreId,
    levelFilter,
    lifecycleFilter,
    tagFilter,
    isLoading,
    isRunningAction,
    errorMessage,
    actionError,
    actionFeedback,
    load,
    selectMember,
    selectStore,
    selectLevel,
    selectLifecycleStage,
    selectTag,
    submitCrmProfile,
    submitAddTag,
    submitRemoveTag,
    submitSegmentRule,
    runSegmentRule,
    submitTouchTask,
    submitExportGroup
  };
}
