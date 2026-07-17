<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import CampaignBoardPanel from '@/features/campaignCenter/components/CampaignBoardPanel.vue';
import CampaignCenterHeroPanel from '@/features/campaignCenter/components/CampaignCenterHeroPanel.vue';
import CampaignComposerPanel from '@/features/campaignCenter/components/CampaignComposerPanel.vue';
import CouponTemplatePanel from '@/features/campaignCenter/components/CouponTemplatePanel.vue';
import { useCampaignCenterOverview } from '@/features/campaignCenter/composables/useCampaignCenterOverview';
import type {
  CreateCampaignPayload,
  CreateCouponTemplatePayload,
  UpdateCampaignPayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const campaignCenter = useCampaignCenterOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await campaignCenter.load(authStore.token);
}

async function handleSelectStore(storeId: string) {
  if (!authStore.token) {
    return;
  }

  await campaignCenter.selectStore(storeId, authStore.token);
}

async function handleSelectCampaign(campaignId: string) {
  if (!authStore.token) {
    return;
  }

  await campaignCenter.selectCampaign(campaignId, authStore.token);
}

async function handleCreateCampaign(payload: CreateCampaignPayload) {
  if (!authStore.token) {
    return;
  }

  await campaignCenter.submitCreateCampaign(payload, authStore.token);
}

async function handleUpdateCampaign(payload: UpdateCampaignPayload) {
  if (!authStore.token) {
    return;
  }

  await campaignCenter.submitUpdateCampaign(payload, authStore.token);
}

async function handleSubmitApproval() {
  if (!authStore.token) {
    return;
  }

  await campaignCenter.submitApproval(authStore.token);
}

async function handlePublishCampaign() {
  if (!authStore.token) {
    return;
  }

  await campaignCenter.submitPublish(authStore.token);
}

async function handleCreateCouponTemplate(payload: CreateCouponTemplatePayload) {
  if (!authStore.token) {
    return;
  }

  await campaignCenter.submitCouponTemplate(payload, authStore.token);
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="campaign-center-page">
    <header class="campaign-center-page__header">
      <p class="campaign-center-page__eyebrow">Campaign Center</p>
      <h2 class="campaign-center-page__title">
        把营销活动、优惠券和审批发布轨道压进同一张高密度营销控制台
      </h2>
      <p class="campaign-center-page__subtitle">
        当前版本不是展示壳。活动列表、新建与编辑、优惠券模板池、提交审批和发布动作都已经接到真实接口，先让营销域从规划页进入可执行前端。
      </p>
    </header>

    <InlineErrorCard
      v-if="campaignCenter.errorMessage"
      :message="campaignCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <CampaignCenterHeroPanel
      :selected-campaign="campaignCenter.selectedCampaign"
      :selected-store-name="campaignCenter.selectedStoreName"
      :selected-coupon-template="campaignCenter.selectedCouponTemplate"
      :summary="campaignCenter.summary"
      :action-feedback="campaignCenter.actionFeedback"
      @open-create="campaignCenter.openCreateCampaign"
      @open-edit="campaignCenter.openEditCampaign"
    />

    <div class="campaign-center-page__grid">
      <CampaignBoardPanel
        :stores="campaignCenter.stores"
        :campaigns="campaignCenter.filteredCampaigns"
        :active-store-id="campaignCenter.activeStoreId"
        :selected-campaign-id="campaignCenter.selectedCampaignId"
        :selected-campaign="campaignCenter.selectedCampaign"
        :selected-coupon-template="campaignCenter.selectedCouponTemplate"
        :is-running-action="campaignCenter.isRunningAction"
        @select-store="handleSelectStore"
        @select-campaign="handleSelectCampaign"
        @open-create="campaignCenter.openCreateCampaign"
        @open-edit="campaignCenter.openEditCampaign"
        @submit-approval="handleSubmitApproval"
        @publish="handlePublishCampaign"
      />

      <div class="campaign-center-page__stack">
        <CampaignComposerPanel
          :mode="campaignCenter.composerMode"
          :stores="campaignCenter.stores"
          :coupon-templates="campaignCenter.couponTemplates"
          :active-store-id="campaignCenter.activeStoreId"
          :selected-campaign="campaignCenter.selectedCampaign"
          :is-submitting="campaignCenter.isRunningAction"
          :error-message="campaignCenter.actionError"
          @open-create="campaignCenter.openCreateCampaign"
          @open-edit="campaignCenter.openEditCampaign"
          @submit-create="handleCreateCampaign"
          @submit-update="handleUpdateCampaign"
        />

        <CouponTemplatePanel
          :templates="campaignCenter.availableCouponTemplates"
          :stores="campaignCenter.stores"
          :active-store-id="campaignCenter.activeStoreId"
          :selected-campaign="campaignCenter.selectedCampaign"
          :is-submitting="campaignCenter.isRunningAction"
          :error-message="campaignCenter.actionError"
          @submit-template="handleCreateCouponTemplate"
        />
      </div>
    </div>

    <p v-if="campaignCenter.isLoading" class="campaign-center-page__footer-note">
      正在回读活动排期、优惠券模板池和当前店铺上下文...
    </p>
  </section>
</template>

<style scoped>
.campaign-center-page {
  display: grid;
  gap: 1.2rem;
}

.campaign-center-page__header {
  display: grid;
  gap: 0.75rem;
}

.campaign-center-page__eyebrow,
.campaign-center-page__title,
.campaign-center-page__subtitle,
.campaign-center-page__footer-note {
  margin: 0;
}

.campaign-center-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.campaign-center-page__title {
  max-width: 14ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 3.8rem);
  line-height: 1.04;
}

.campaign-center-page__subtitle,
.campaign-center-page__footer-note {
  max-width: 64rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.campaign-center-page__grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}

.campaign-center-page__stack {
  display: grid;
  gap: 1rem;
}
</style>
