import { computed, shallowRef } from 'vue';

import {
  archiveContentAssetVersion,
  copyContentAssetReference,
  generateContentAsset,
  getContentAssetDetail,
  getContentAssets,
  publishContentAsset,
  uploadContentAsset
} from '@/features/contentAssetCenter/services/contentAssetCenterService';
import type {
  ArchiveContentAssetVersionPayload,
  ContentAsset,
  ContentAssetDetailView,
  ContentAssetReference,
  ContentAssetReferenceCopyView,
  ContentAssetVersion,
  GenerateContentAssetPayload,
  PublishContentAssetPayload,
  Store,
  UploadContentAssetPayload
} from '@/services/apiTypes';
import { getStores } from '@/services/storeChannelService';

export type ContentAssetCategoryKey = 'all' | 'product' | 'live' | 'service';
type Tone = 'neutral' | 'warn' | 'success';

export interface ContentAssetCategoryCard {
  key: ContentAssetCategoryKey;
  label: string;
  description: string;
  count: number;
  tone: Tone;
}

export interface ContentAssetLibraryItem extends ContentAsset {
  storeName: string;
  statusLabel: string;
  statusTone: Tone;
  typeLabel: string;
  categoryLabel: string;
}

const CATEGORY_META: Record<ContentAssetCategoryKey, { label: string; description: string }> = {
  all: {
    label: '全部资产',
    description: '统一查看商品、直播和客服素材，优先处理可直接复用或继续加工的资产。'
  },
  product: {
    label: '商品素材',
    description: '沉淀商品主图、封面、短视频成片和可继续上架复用的视觉资产。'
  },
  live: {
    label: '直播素材',
    description: '收口开场脚本、讲解卡片、切片画面和直播编排所需的执行物料。'
  },
  service: {
    label: '客服素材',
    description: '管理 FAQ、快捷回复、售后说明和服务 SOP 的标准化内容资产。'
  }
};

const TYPE_LABELS: Record<string, string> = {
  image: '图片',
  video: '视频',
  script: '脚本',
  faq: 'FAQ',
  copy: '文案',
  cover: '封面'
};

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function isPublishedVideo(asset: ContentAsset) {
  return asset.assetType === 'video' && normalizeText(asset.previewText).includes('published to');
}

function sortAssets(items: ContentAsset[]) {
  return [...items].sort((left, right) => {
    const leftPublished = isPublishedVideo(left);
    const rightPublished = isPublishedVideo(right);

    if (leftPublished !== rightPublished) {
      return Number(rightPublished) - Number(leftPublished);
    }

    if (left.assetStatus !== right.assetStatus) {
      return left.assetStatus === 'active' ? -1 : 1;
    }

    if (left.generated !== right.generated) {
      return Number(right.generated) - Number(left.generated);
    }

    return toTimestamp(right.updatedAt) - toTimestamp(left.updatedAt);
  });
}

function resolveStatus(asset: ContentAsset): { label: string; tone: Tone } {
  if (asset.assetStatus === 'archived') {
    return {
      label: '已归档',
      tone: 'neutral'
    };
  }

  if (isPublishedVideo(asset)) {
    return {
      label: '已发布',
      tone: 'success'
    };
  }

  if (asset.generated) {
    return {
      label: 'AI 生成',
      tone: 'success'
    };
  }

  return {
    label: '已上传',
    tone: 'warn'
  };
}

function resolveCategoryKey(value: string | null | undefined): ContentAssetCategoryKey {
  const normalized = normalizeText(value);
  return normalized === 'product' || normalized === 'live' || normalized === 'service'
    ? normalized
    : 'all';
}

export function useContentAssetCenterOverview() {
  const stores = shallowRef<Store[]>([]);
  const assets = shallowRef<ContentAsset[]>([]);
  const selectedCategory = shallowRef<ContentAssetCategoryKey>('all');
  const selectedAssetId = shallowRef<string | null>(null);
  const selectedDetail = shallowRef<ContentAssetDetailView | null>(null);
  const copyResult = shallowRef<ContentAssetReferenceCopyView | null>(null);
  const isLoading = shallowRef(false);
  const isDetailLoading = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);
  const sessionToken = shallowRef<string | null>(null);

  const storeMap = computed(() => new Map(stores.value.map((store) => [store.storeId, store])));

  const filteredAssets = computed(() => {
    if (selectedCategory.value === 'all') {
      return sortAssets(assets.value);
    }

    return sortAssets(assets.value.filter((asset) => asset.assetCategory === selectedCategory.value));
  });

  const libraryAssets = computed<ContentAssetLibraryItem[]>(() =>
    filteredAssets.value.map((asset) => {
      const status = resolveStatus(asset);
      return {
        ...asset,
        storeName: storeMap.value.get(asset.storeId)?.shopName ?? asset.storeId,
        statusLabel: status.label,
        statusTone: status.tone,
        typeLabel: TYPE_LABELS[asset.assetType] ?? asset.assetType,
        categoryLabel: CATEGORY_META[resolveCategoryKey(asset.assetCategory)].label
      };
    })
  );

  const selectedAsset = computed(() => {
    if (selectedDetail.value && selectedDetail.value.asset.assetId === selectedAssetId.value) {
      return selectedDetail.value.asset;
    }

    return assets.value.find((asset) => asset.assetId === selectedAssetId.value) ?? null;
  });

  const versions = computed<ContentAssetVersion[]>(() => selectedDetail.value?.versions ?? []);
  const references = computed<ContentAssetReference[]>(() => selectedDetail.value?.references ?? []);

  const selectedStoreName = computed(() => {
    const selectedStoreId = selectedAsset.value?.storeId ?? defaultStoreId.value ?? null;

    return selectedStoreId
      ? storeMap.value.get(selectedStoreId)?.shopName ?? selectedStoreId
      : '未选择店铺';
  });

  const defaultStoreId = computed(() => selectedAsset.value?.storeId ?? stores.value[0]?.storeId ?? null);

  const storeOptions = computed(() =>
    stores.value.map((store) => ({
      value: store.storeId,
      label: store.shopName
    }))
  );

  const categoryCards = computed<ContentAssetCategoryCard[]>(() => {
    const total = assets.value.length;
    const productCount = assets.value.filter((asset) => asset.assetCategory === 'product').length;
    const liveCount = assets.value.filter((asset) => asset.assetCategory === 'live').length;
    const serviceCount = assets.value.filter((asset) => asset.assetCategory === 'service').length;

    return [
      {
        key: 'all',
        ...CATEGORY_META.all,
        count: total,
        tone: total ? 'success' : 'neutral'
      },
      {
        key: 'product',
        ...CATEGORY_META.product,
        count: productCount,
        tone: productCount ? 'success' : 'neutral'
      },
      {
        key: 'live',
        ...CATEGORY_META.live,
        count: liveCount,
        tone: liveCount ? 'warn' : 'neutral'
      },
      {
        key: 'service',
        ...CATEGORY_META.service,
        count: serviceCount,
        tone: serviceCount ? 'success' : 'neutral'
      }
    ];
  });

  const summary = computed(() => ({
    totalAssets: assets.value.length,
    generatedAssets: assets.value.filter((asset) => asset.generated).length,
    videoAssets: assets.value.filter((asset) => asset.assetType === 'video').length,
    publishedVideoAssets: assets.value.filter(isPublishedVideo).length,
    archivedAssets: assets.value.filter((asset) => asset.assetStatus === 'archived').length
  }));

  function requireSessionToken() {
    if (!sessionToken.value) {
      throw new Error('当前登录状态已失效，请重新登录后重试。');
    }

    return sessionToken.value;
  }

  function clearSelection() {
    selectedAssetId.value = null;
    selectedDetail.value = null;
    copyResult.value = null;
  }

  function resolveNextAssetId(preferredAssetId?: string | null) {
    if (preferredAssetId && filteredAssets.value.some((asset) => asset.assetId === preferredAssetId)) {
      return preferredAssetId;
    }

    if (selectedAssetId.value && filteredAssets.value.some((asset) => asset.assetId === selectedAssetId.value)) {
      return selectedAssetId.value;
    }

    return filteredAssets.value[0]?.assetId ?? null;
  }

  function mergeDetail(detail: ContentAssetDetailView) {
    assets.value = sortAssets([
      detail.asset,
      ...assets.value.filter((asset) => asset.assetId !== detail.asset.assetId)
    ]);
    selectedCategory.value = resolveCategoryKey(detail.asset.assetCategory);
    selectedAssetId.value = detail.asset.assetId;
    selectedDetail.value = detail;
    copyResult.value = null;
  }

  async function loadDetail(assetId: string) {
    const token = requireSessionToken();
    isDetailLoading.value = true;

    try {
      selectedDetail.value = await getContentAssetDetail(assetId, token);
      selectedAssetId.value = assetId;
    } finally {
      isDetailLoading.value = false;
    }
  }

  async function load(token: string, preferredAssetId?: string | null) {
    sessionToken.value = token;
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [storesPayload, assetsPayload] = await Promise.all([getStores(token), getContentAssets(token)]);

      stores.value = storesPayload;
      assets.value = sortAssets(assetsPayload);

      const nextAssetId = resolveNextAssetId(preferredAssetId);
      if (!nextAssetId) {
        clearSelection();
        return;
      }

      await loadDetail(nextAssetId);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '内容素材中心加载失败，请稍后重试。';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  async function selectCategory(category: ContentAssetCategoryKey) {
    selectedCategory.value = category;
    const nextAssetId = resolveNextAssetId(selectedAssetId.value);

    if (!nextAssetId) {
      clearSelection();
      return;
    }

    if (nextAssetId === selectedAssetId.value && selectedDetail.value) {
      return;
    }

    await loadDetail(nextAssetId);
  }

  async function selectAsset(assetId: string) {
    if (assetId === selectedAssetId.value && selectedDetail.value) {
      return;
    }

    actionError.value = null;
    await loadDetail(assetId);
  }

  async function runAction<T>(executor: () => Promise<T>, fallbackMessage: string) {
    isRunningAction.value = true;
    actionError.value = null;
    actionFeedback.value = null;

    try {
      return await executor();
    } catch (error) {
      actionError.value = error instanceof Error ? error.message : fallbackMessage;
      throw error;
    } finally {
      isRunningAction.value = false;
    }
  }

  async function uploadAsset(payload: UploadContentAssetPayload) {
    const token = requireSessionToken();
    const detail = await runAction(
      () => uploadContentAsset(payload, token),
      '上传素材失败，请稍后重试。'
    );
    mergeDetail(detail);
    actionFeedback.value = `已上传素材《${detail.asset.assetName}》，可继续预览、归档或复用。`;
  }

  async function generateAssetFromPrompt(payload: GenerateContentAssetPayload) {
    const token = requireSessionToken();
    const detail = await runAction(
      () => generateContentAsset(payload, token),
      '生成素材失败，请稍后重试。'
    );
    mergeDetail(detail);

    actionFeedback.value =
      payload.assetType === 'video'
        ? `已生成视频成片《${detail.asset.assetName}》，并补齐编排与剪辑版本。`
        : `已生成素材《${detail.asset.assetName}》，可继续查看版本与引用来源。`;
  }

  async function archiveLatestVersion(payload: ArchiveContentAssetVersionPayload) {
    const token = requireSessionToken();

    if (!selectedAssetId.value) {
      throw new Error('请先选择需要归档的素材。');
    }

    const detail = await runAction(
      () => archiveContentAssetVersion(selectedAssetId.value!, payload, token),
      '归档版本失败，请稍后重试。'
    );
    mergeDetail(detail);
    actionFeedback.value = `已归档素材《${detail.asset.assetName}》的当前版本。`;
  }

  async function copyReferenceText() {
    const token = requireSessionToken();

    if (!selectedAssetId.value) {
      throw new Error('请先选择需要复制引用的素材。');
    }

    const result = await runAction(
      () => copyContentAssetReference(selectedAssetId.value!, token),
      '复制引用失败，请稍后重试。'
    );

    copyResult.value = result;

    try {
      if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(result.referenceText);
        actionFeedback.value = `已复制 ${result.referenceCount} 条引用来源的复用文本。`;
        return;
      }
    } catch {
      // Keep the generated text in UI when clipboard is unavailable.
    }

    actionFeedback.value = '引用文本已生成，请在右侧引用来源区直接复制。';
  }

  async function publishVideoAsset(payload: PublishContentAssetPayload) {
    const token = requireSessionToken();

    if (!selectedAssetId.value || selectedAsset.value?.assetType !== 'video') {
      throw new Error('请先选择一个可发布的视频素材。');
    }

    const detail = await runAction(
      () => publishContentAsset(selectedAssetId.value!, payload, token),
      '发布视频失败，请稍后重试。'
    );
    mergeDetail(detail);
    actionFeedback.value = `视频《${detail.asset.assetName}》已发布到 ${payload.platformCode}。`;
  }

  return {
    selectedCategory,
    selectedAssetId,
    selectedAsset,
    selectedDetail,
    selectedStoreName,
    defaultStoreId,
    storeOptions,
    categoryCards,
    libraryAssets,
    versions,
    references,
    summary,
    copyResult,
    isLoading,
    isDetailLoading,
    isRunningAction,
    errorMessage,
    actionError,
    actionFeedback,
    load,
    selectCategory,
    selectAsset,
    uploadAsset,
    generateAssetFromPrompt,
    archiveLatestVersion,
    copyReferenceText,
    publishVideoAsset
  };
}
