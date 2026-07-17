import { computed, shallowRef } from 'vue';

import {
  createCandidateProduct,
  generateProductDraft,
  getCandidateProducts,
  getProductDrafts,
  publishProductDraft,
  updateCandidateProduct,
  updateCandidateProductStatus,
  updateProductDraft
} from '@/services/productCenterService';
import { getStores } from '@/services/storeChannelService';
import type {
  CandidateProduct,
  CreateCandidateProductPayload,
  GenerateProductDraftPayload,
  Product,
  ProductDraft,
  Store,
  UpdateCandidateProductPayload,
  UpdateProductDraftPayload
} from '@/services/apiTypes';

export type ProductComposerMode =
  | 'create-candidate'
  | 'edit-candidate'
  | 'generate-draft'
  | 'edit-draft'
  | 'publish-draft';

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function candidatePriority(candidate: CandidateProduct) {
  const status = normalizeText(candidate.status);
  const risk = normalizeText(candidate.riskLevel);

  if (status.includes('pending')) {
    return 4;
  }

  if (risk.includes('high')) {
    return 3;
  }

  if (status.includes('testable') || status.includes('pool')) {
    return 2;
  }

  return 1;
}

function draftPriority(draft: ProductDraft) {
  const status = normalizeText(draft.status);

  if (status.includes('pending_publish')) {
    return 4;
  }

  if (status.includes('drafting')) {
    return 3;
  }

  if (status.includes('rejected')) {
    return 2;
  }

  if (status.includes('published')) {
    return 1;
  }

  return 0;
}

export function useProductCenterOverview() {
  const stores = shallowRef<Store[]>([]);
  const candidates = shallowRef<CandidateProduct[]>([]);
  const drafts = shallowRef<ProductDraft[]>([]);
  const sessionPublishedProducts = shallowRef<Product[]>([]);
  const selectedCandidateId = shallowRef<string | null>(null);
  const selectedDraftId = shallowRef<string | null>(null);
  const composerMode = shallowRef<ProductComposerMode | null>(null);
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

  const sortedCandidates = computed(() =>
    [...candidates.value].sort((left, right) => {
      const priorityGap = candidatePriority(right) - candidatePriority(left);
      if (priorityGap !== 0) {
        return priorityGap;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const sortedDrafts = computed(() =>
    [...drafts.value].sort((left, right) => {
      const priorityGap = draftPriority(right) - draftPriority(left);
      if (priorityGap !== 0) {
        return priorityGap;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const selectedCandidate = computed(
    () =>
      sortedCandidates.value.find(
        (candidate) => candidate.candidateProductId === selectedCandidateId.value
      ) ?? null
  );

  const selectedDraft = computed(
    () =>
      sortedDrafts.value.find((draft) => draft.productDraftId === selectedDraftId.value) ??
      null
  );

  const selectedStore = computed(() => {
    const storeId = selectedCandidate.value?.storeId ?? selectedDraft.value?.storeId;

    return sortedStores.value.find((store) => store.storeId === storeId) ?? null;
  });

  const relatedDrafts = computed(() => {
    if (!selectedCandidate.value) {
      return sortedDrafts.value.slice(0, 4);
    }

    const targetCandidateId = selectedCandidate.value.candidateProductId;
    const matches = sortedDrafts.value.filter(
      (draft) => draft.candidateProductId === targetCandidateId
    );

    return matches.length ? matches : sortedDrafts.value.slice(0, 4);
  });

  const summary = computed(() => ({
    candidateCount: candidates.value.length,
    pendingReviewCount: candidates.value.filter((candidate) =>
      normalizeText(candidate.status).includes('pending')
    ).length,
    pendingPublishCount: drafts.value.filter((draft) =>
      normalizeText(draft.status).includes('pending_publish')
    ).length,
    publishedDraftCount: drafts.value.filter((draft) =>
      normalizeText(draft.status).includes('published')
    ).length,
    sessionPublishedCount: sessionPublishedProducts.value.length,
    highRiskCount: candidates.value.filter((candidate) =>
      normalizeText(candidate.riskLevel).includes('high')
    ).length
  }));

  function applySelection(
    preferredCandidateId?: string | null,
    preferredDraftId?: string | null
  ) {
    const nextCandidateId =
      preferredCandidateId &&
      candidates.value.some(
        (candidate) => candidate.candidateProductId === preferredCandidateId
      )
        ? preferredCandidateId
        : sortedCandidates.value[0]?.candidateProductId ?? null;

    selectedCandidateId.value = nextCandidateId;

    const nextRelatedDrafts = nextCandidateId
      ? sortedDrafts.value.filter(
          (draft) => draft.candidateProductId === nextCandidateId
        )
      : [];

    const nextDraftId =
      preferredDraftId &&
      drafts.value.some((draft) => draft.productDraftId === preferredDraftId)
        ? preferredDraftId
        : nextRelatedDrafts[0]?.productDraftId ??
          sortedDrafts.value[0]?.productDraftId ??
          null;

    selectedDraftId.value = nextDraftId;
  }

  async function load(
    token: string,
    preferredCandidateId?: string | null,
    preferredDraftId?: string | null
  ) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [storesPayload, candidatesPayload, draftsPayload] = await Promise.all([
        getStores(token),
        getCandidateProducts(token),
        getProductDrafts(token)
      ]);

      stores.value = storesPayload;
      candidates.value = candidatesPayload;
      drafts.value = draftsPayload;
      applySelection(
        preferredCandidateId ?? selectedCandidateId.value,
        preferredDraftId ?? selectedDraftId.value
      );
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '商品中心加载失败，请稍后重试';
    } finally {
      isLoading.value = false;
    }
  }

  function selectCandidate(candidateProductId: string) {
    selectedCandidateId.value = candidateProductId;
    const matchedDraft =
      sortedDrafts.value.find(
        (draft) => draft.candidateProductId === candidateProductId
      ) ?? null;

    selectedDraftId.value = matchedDraft?.productDraftId ?? null;
  }

  function selectDraft(productDraftId: string) {
    selectedDraftId.value = productDraftId;
    const draft =
      sortedDrafts.value.find((item) => item.productDraftId === productDraftId) ?? null;

    if (draft) {
      selectedCandidateId.value = draft.candidateProductId;
    }
  }

  function openCreateCandidate() {
    composerMode.value = 'create-candidate';
    actionError.value = null;
  }

  function openEditCandidate() {
    if (!selectedCandidate.value) {
      return;
    }

    composerMode.value = 'edit-candidate';
    actionError.value = null;
  }

  function openGenerateDraft() {
    if (!selectedCandidate.value) {
      return;
    }

    composerMode.value = 'generate-draft';
    actionError.value = null;
  }

  function openEditDraft() {
    if (!selectedDraft.value) {
      return;
    }

    composerMode.value = 'edit-draft';
    actionError.value = null;
  }

  function openPublishDraft() {
    if (!selectedDraft.value) {
      return;
    }

    composerMode.value = 'publish-draft';
    actionError.value = null;
  }

  function closeComposer() {
    composerMode.value = null;
    actionError.value = null;
  }

  async function submitCandidate(
    payload: CreateCandidateProductPayload,
    token: string
  ) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      const candidate = await createCandidateProduct(payload, token);
      composerMode.value = null;
      actionFeedback.value = `已新增候选商品 ${candidate.title}，正在回读候选池。`;
      await load(token, candidate.candidateProductId, selectedDraftId.value);
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '新增候选商品失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitCandidateUpdate(
    payload: UpdateCandidateProductPayload,
    token: string
  ) {
    if (!selectedCandidate.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      const candidate = await updateCandidateProduct(
        selectedCandidate.value.candidateProductId,
        payload,
        token
      );
      composerMode.value = null;
      actionFeedback.value = `已更新候选商品 ${candidate.title}，正在回读候选池。`;
      await load(token, candidate.candidateProductId, selectedDraftId.value);
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '更新候选商品失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function changeCandidateStatus(status: string, token: string) {
    if (!selectedCandidate.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      const candidate = await updateCandidateProductStatus(
        selectedCandidate.value.candidateProductId,
        { status },
        token
      );
      actionFeedback.value = `候选商品 ${candidate.title} 已切换为 ${candidate.status}。`;
      await load(token, candidate.candidateProductId, selectedDraftId.value);
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '更新候选状态失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitDraftGeneration(
    payload: GenerateProductDraftPayload,
    token: string
  ) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      const draft = await generateProductDraft(payload, token);
      composerMode.value = null;
      actionFeedback.value = `已生成草稿 ${draft.title}，正在回读草稿工位。`;
      await load(token, draft.candidateProductId, draft.productDraftId);
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '生成商品草稿失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitDraftUpdate(
    payload: UpdateProductDraftPayload,
    token: string
  ) {
    if (!selectedDraft.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      const draft = await updateProductDraft(
        selectedDraft.value.productDraftId,
        payload,
        token
      );
      composerMode.value = null;
      actionFeedback.value = `已更新草稿 ${draft.title}，正在回读草稿工位。`;
      await load(token, draft.candidateProductId, draft.productDraftId);
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '更新商品草稿失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitDraftPublish(platformProductId: string | undefined, token: string) {
    if (!selectedDraft.value) {
      return;
    }

    isRunningAction.value = true;
    actionError.value = null;

    try {
      const product = await publishProductDraft(
        selectedDraft.value.productDraftId,
        { platformProductId },
        token
      );
      sessionPublishedProducts.value = [
        product,
        ...sessionPublishedProducts.value.filter(
          (item) => item.productId !== product.productId
        )
      ].slice(0, 6);
      composerMode.value = null;
      actionFeedback.value = `草稿 ${selectedDraft.value.title} 已发布，平台商品号 ${product.platformProductId}。`;
      await load(token, selectedCandidateId.value, selectedDraft.value.productDraftId);
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '发布商品草稿失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  return {
    stores: sortedStores,
    candidates: sortedCandidates,
    drafts: sortedDrafts,
    relatedDrafts,
    sessionPublishedProducts,
    selectedStore,
    selectedCandidateId,
    selectedCandidate,
    selectedDraftId,
    selectedDraft,
    composerMode,
    summary,
    isLoading,
    isRunningAction,
    errorMessage,
    actionError,
    actionFeedback,
    load,
    selectCandidate,
    selectDraft,
    openCreateCandidate,
    openEditCandidate,
    openGenerateDraft,
    openEditDraft,
    openPublishDraft,
    closeComposer,
    submitCandidate,
    submitCandidateUpdate,
    changeCandidateStatus,
    submitDraftGeneration,
    submitDraftUpdate,
    submitDraftPublish
  };
}
