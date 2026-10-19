<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import ApprovalHeroPanel from '@/features/approvalCenter/components/ApprovalHeroPanel.vue';
import ApprovalQueuePanel from '@/features/approvalCenter/components/ApprovalQueuePanel.vue';
import ApprovalTemplateMatrixPanel from '@/features/approvalCenter/components/ApprovalTemplateMatrixPanel.vue';
import GovernanceApprovalRequestPanel from '@/features/approvalCenter/components/GovernanceApprovalRequestPanel.vue';
import { useApprovalCenterOverview } from '@/features/approvalCenter/composables/useApprovalCenterOverview';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const approvalCenter = useApprovalCenterOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await approvalCenter.load(authStore.token);
}

async function handleApprove() {
  if (!authStore.token) {
    return;
  }

  await approvalCenter.approveSelected(authStore.token);
}

async function handleReject() {
  if (!authStore.token) {
    return;
  }

  await approvalCenter.rejectSelected(authStore.token);
}

async function handleTransfer() {
  if (!authStore.token) {
    return;
  }

  await approvalCenter.transferSelected(authStore.token);
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="approval-center-page">
    <header class="approval-center-page__header">
      <p class="approval-center-page__eyebrow">协作中心</p>
      <h2 class="approval-center-page__title">审批协作</h2>
      <p class="approval-center-page__subtitle">
        审批队列、模板与治理请求。
      </p>
    </header>

    <InlineErrorCard
      v-if="approvalCenter.errorMessage"
      :message="approvalCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <ApprovalHeroPanel
      :summary="approvalCenter.summary"
      :selected-approval="approvalCenter.selectedApproval"
    />

    <ApprovalQueuePanel
      :approvals="approvalCenter.approvals"
      :selected-approval-id="approvalCenter.selectedApprovalId"
      :selected-approval="approvalCenter.selectedApproval"
      :action-remark="approvalCenter.actionRemark"
      :transfer-handler-id="approvalCenter.transferHandlerId"
      :action-feedback="approvalCenter.actionFeedback"
      :is-submitting-action="approvalCenter.isSubmittingAction"
      @select="approvalCenter.selectApproval"
      @update-action-remark="approvalCenter.actionRemark = $event"
      @update-transfer-handler-id="approvalCenter.transferHandlerId = $event"
      @approve="handleApprove"
      @reject="handleReject"
      @transfer="handleTransfer"
    />

    <div class="approval-center-page__grid">
      <ApprovalTemplateMatrixPanel :templates="approvalCenter.templates" />
      <GovernanceApprovalRequestPanel :requests="approvalCenter.governanceRequests" />
    </div>

    <p v-if="approvalCenter.isLoading" class="approval-center-page__footer-note">
      正在刷新审批队列、模板矩阵与治理请求...
    </p>
  </section>
</template>

<style scoped>
.approval-center-page {
  display: grid;
  gap: 1.2rem;
}

.approval-center-page__header {
  display: grid;
  gap: 0.8rem;
}

.approval-center-page__eyebrow,
.approval-center-page__title,
.approval-center-page__subtitle,
.approval-center-page__footer-note {
  margin: 0;
}

.approval-center-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.approval-center-page__title {
  max-width: 16ch;
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
  line-height: 1.2;
}

.approval-center-page__subtitle,
.approval-center-page__footer-note {
  max-width: 64rem;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.approval-center-page__grid {
  display: grid;
  grid-template-columns: 0.96fr 1.04fr;
  gap: 1rem;
  align-items: start;
}

@media (max-width: 1080px) {
  .approval-center-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
