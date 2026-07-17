<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import CandidatePoolPanel from '@/features/productCenter/components/CandidatePoolPanel.vue';
import DraftWorkbenchPanel from '@/features/productCenter/components/DraftWorkbenchPanel.vue';
import ProductCenterHeroPanel from '@/features/productCenter/components/ProductCenterHeroPanel.vue';
import ProductComposerPanel from '@/features/productCenter/components/ProductComposerPanel.vue';
import { useProductCenterOverview } from '@/features/productCenter/composables/useProductCenterOverview';
import type {
  CreateCandidateProductPayload,
  GenerateProductDraftPayload,
  UpdateCandidateProductPayload,
  UpdateProductDraftPayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const productCenter = useProductCenterOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await productCenter.load(authStore.token);
}

async function handleCreateCandidate(payload: CreateCandidateProductPayload) {
  if (!authStore.token) {
    return;
  }

  await productCenter.submitCandidate(payload, authStore.token);
}

async function handleUpdateCandidate(payload: UpdateCandidateProductPayload) {
  if (!authStore.token) {
    return;
  }

  await productCenter.submitCandidateUpdate(payload, authStore.token);
}

async function handleGenerateDraft(payload: GenerateProductDraftPayload) {
  if (!authStore.token) {
    return;
  }

  await productCenter.submitDraftGeneration(payload, authStore.token);
}

async function handleUpdateDraft(payload: UpdateProductDraftPayload) {
  if (!authStore.token) {
    return;
  }

  await productCenter.submitDraftUpdate(payload, authStore.token);
}

async function handlePublishDraft(platformProductId?: string) {
  if (!authStore.token) {
    return;
  }

  await productCenter.submitDraftPublish(platformProductId, authStore.token);
}

async function handleChangeCandidateStatus(status: string) {
  if (!authStore.token) {
    return;
  }

  await productCenter.changeCandidateStatus(status, authStore.token);
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="product-center-page">
    <header class="product-center-page__header">
      <p class="product-center-page__eyebrow">Product Incubation Center</p>
      <h2 class="product-center-page__title">
        把选品判断、草稿生成与上架出栈压进同一张商品孵化控制台
      </h2>
      <p class="product-center-page__subtitle">
        这张页故意不去伪装成传统 ERP 商品总表，而是把真正影响上架效率的前半程做深：先看候选池，再看草稿工位，最后把发布动作拉到可控边界内。
      </p>
    </header>

    <InlineErrorCard
      v-if="productCenter.errorMessage"
      :message="productCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <ProductCenterHeroPanel
      :selected-store="productCenter.selectedStore"
      :selected-candidate="productCenter.selectedCandidate"
      :selected-draft="productCenter.selectedDraft"
      :session-published-products="productCenter.sessionPublishedProducts"
      :action-feedback="productCenter.actionFeedback"
      :summary="productCenter.summary"
      @open-create="productCenter.openCreateCandidate"
      @open-generate="productCenter.openGenerateDraft"
      @open-publish="productCenter.openPublishDraft"
    />

    <div class="product-center-page__grid">
      <CandidatePoolPanel
        :candidates="productCenter.candidates"
        :selected-candidate="productCenter.selectedCandidate"
        :selected-candidate-id="productCenter.selectedCandidateId"
        :is-running-action="productCenter.isRunningAction"
        @select="productCenter.selectCandidate"
        @open-create="productCenter.openCreateCandidate"
        @open-edit="productCenter.openEditCandidate"
        @open-generate="productCenter.openGenerateDraft"
        @change-status="handleChangeCandidateStatus"
      />

      <div class="product-center-page__stack">
        <ProductComposerPanel
          :mode="productCenter.composerMode"
          :stores="productCenter.stores"
          :selected-candidate="productCenter.selectedCandidate"
          :selected-draft="productCenter.selectedDraft"
          :is-submitting="productCenter.isRunningAction"
          :error-message="productCenter.actionError"
          @close="productCenter.closeComposer"
          @open-create="productCenter.openCreateCandidate"
          @open-edit-candidate="productCenter.openEditCandidate"
          @open-generate="productCenter.openGenerateDraft"
          @open-edit-draft="productCenter.openEditDraft"
          @open-publish="productCenter.openPublishDraft"
          @create-candidate="handleCreateCandidate"
          @update-candidate="handleUpdateCandidate"
          @generate-draft="handleGenerateDraft"
          @update-draft="handleUpdateDraft"
          @publish-draft="handlePublishDraft"
        />

        <DraftWorkbenchPanel
          :drafts="productCenter.drafts"
          :related-drafts="productCenter.relatedDrafts"
          :selected-draft="productCenter.selectedDraft"
          :selected-draft-id="productCenter.selectedDraftId"
          :session-published-products="productCenter.sessionPublishedProducts"
          @select="productCenter.selectDraft"
          @open-edit="productCenter.openEditDraft"
          @open-publish="productCenter.openPublishDraft"
        />
      </div>
    </div>

    <p v-if="productCenter.isLoading" class="product-center-page__footer-note">
      正在回读候选池、草稿工位与店铺上下文...
    </p>
  </section>
</template>

<style scoped>
.product-center-page {
  display: grid;
  gap: 1.2rem;
}

.product-center-page__header {
  display: grid;
  gap: 0.75rem;
}

.product-center-page__eyebrow,
.product-center-page__title,
.product-center-page__subtitle,
.product-center-page__footer-note {
  margin: 0;
}

.product-center-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.product-center-page__title {
  max-width: 16ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 3.8rem);
  line-height: 1.04;
}

.product-center-page__subtitle,
.product-center-page__footer-note {
  max-width: 62rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.product-center-page__grid {
  display: grid;
  grid-template-columns: 1.02fr 0.98fr;
  gap: 1rem;
  align-items: start;
}

.product-center-page__stack {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1180px) {
  .product-center-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
