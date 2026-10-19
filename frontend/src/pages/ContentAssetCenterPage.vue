<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import ContentAssetCategoryPanel from '@/features/contentAssetCenter/components/ContentAssetCategoryPanel.vue';
import ContentAssetCenterHeroPanel from '@/features/contentAssetCenter/components/ContentAssetCenterHeroPanel.vue';
import ContentAssetLibraryPanel from '@/features/contentAssetCenter/components/ContentAssetLibraryPanel.vue';
import ContentAssetPreviewPanel from '@/features/contentAssetCenter/components/ContentAssetPreviewPanel.vue';
import ContentAssetReferencePanel from '@/features/contentAssetCenter/components/ContentAssetReferencePanel.vue';
import ContentAssetVersionPanel from '@/features/contentAssetCenter/components/ContentAssetVersionPanel.vue';
import type { ContentAssetCategoryKey } from '@/features/contentAssetCenter/composables/useContentAssetCenterOverview';
import { useContentAssetCenterOverview } from '@/features/contentAssetCenter/composables/useContentAssetCenterOverview';
import type {
  GenerateContentAssetPayload,
  PublishContentAssetPayload,
  UploadContentAssetPayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const assetCenter = useContentAssetCenterOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await assetCenter.load(authStore.token);
}

async function handleSelectCategory(category: ContentAssetCategoryKey) {
  await assetCenter.selectCategory(category);
}

async function handleSelectAsset(assetId: string) {
  await assetCenter.selectAsset(assetId);
}

async function handleUpload(payload: UploadContentAssetPayload) {
  await assetCenter.uploadAsset(payload);
}

async function handleGenerate(payload: GenerateContentAssetPayload) {
  await assetCenter.generateAssetFromPrompt(payload);
}

async function handleArchive(remark: string) {
  await assetCenter.archiveLatestVersion({
    remark: remark || undefined
  });
}

async function handleCopyReference() {
  await assetCenter.copyReferenceText();
}

async function handlePublishVideo(payload: PublishContentAssetPayload) {
  await assetCenter.publishVideoAsset(payload);
}

onMounted(() => {
  void bootstrapPage();
});
</script>

<template>
  <section class="content-asset-page">
    <header class="content-asset-page__header">
      <p class="content-asset-page__eyebrow">内容资产</p>
      <h2 class="content-asset-page__title">内容素材</h2>
      <p class="content-asset-page__subtitle">
        素材库、版本与发布流转的统一管理。
      </p>
    </header>

    <InlineErrorCard
      v-if="assetCenter.errorMessage"
      :message="assetCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <ContentAssetCenterHeroPanel
      :selected-asset="assetCenter.selectedAsset"
      :selected-store-name="assetCenter.selectedStoreName"
      :default-store-id="assetCenter.defaultStoreId"
      :store-options="assetCenter.storeOptions"
      :summary="assetCenter.summary"
      :is-submitting="assetCenter.isRunningAction"
      :action-feedback="assetCenter.actionFeedback"
      :action-error="assetCenter.actionError"
      @upload="handleUpload"
      @generate="handleGenerate"
    />

    <div class="content-asset-page__grid">
      <div class="content-asset-page__stack">
        <ContentAssetCategoryPanel
          :items="assetCenter.categoryCards"
          :selected-category="assetCenter.selectedCategory"
          @select="handleSelectCategory"
        />

        <ContentAssetLibraryPanel
          :assets="assetCenter.libraryAssets"
          :selected-asset-id="assetCenter.selectedAssetId"
          :is-loading="assetCenter.isLoading"
          @select="handleSelectAsset"
        />
      </div>

      <ContentAssetPreviewPanel
        :detail="assetCenter.selectedDetail"
        :store-name="assetCenter.selectedStoreName"
        :is-loading="assetCenter.isDetailLoading"
      />
    </div>

    <div class="content-asset-page__bottom-grid">
      <ContentAssetVersionPanel
        :versions="assetCenter.versions"
        :asset-status="assetCenter.selectedAsset?.assetStatus ?? null"
        :is-submitting="assetCenter.isRunningAction"
        :action-error="assetCenter.actionError"
        @archive="handleArchive"
      />

      <ContentAssetReferencePanel
        :references="assetCenter.references"
        :copy-result="assetCenter.copyResult"
        :selected-asset="assetCenter.selectedAsset"
        :is-submitting="assetCenter.isRunningAction"
        :action-error="assetCenter.actionError"
        @copy="handleCopyReference"
        @publish="handlePublishVideo"
      />
    </div>

    <p v-if="assetCenter.isLoading" class="content-asset-page__footer-note">
      正在回读素材池、店铺归属和当前素材详情...
    </p>
  </section>
</template>

<style scoped>
.content-asset-page {
  display: grid;
  gap: 1.2rem;
}

.content-asset-page__header {
  display: grid;
  gap: 0.8rem;
}

.content-asset-page__eyebrow,
.content-asset-page__title,
.content-asset-page__subtitle,
.content-asset-page__footer-note {
  margin: 0;
}

.content-asset-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.content-asset-page__title {
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
  line-height: 1.2;
}

.content-asset-page__subtitle,
.content-asset-page__footer-note {
  max-width: 68rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.content-asset-page__grid,
.content-asset-page__bottom-grid {
  display: grid;
  gap: 1rem;
  align-items: start;
}

.content-asset-page__grid {
  grid-template-columns: 0.92fr 1.08fr;
}

.content-asset-page__bottom-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.content-asset-page__stack {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1260px) {
  .content-asset-page__grid,
  .content-asset-page__bottom-grid {
    grid-template-columns: 1fr;
  }
}
</style>
