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
      <p class="approval-center-page__eyebrow">Approval Center</p>
      <h2 class="approval-center-page__title">
        把审批流转、模板治理和协作责任放进同一个审批与协作中心
      </h2>
      <p class="approval-center-page__subtitle">
        这个页面不只是看审批结果，而是把待处理队列、处理人流转、模板阶段设计和治理申请统一摆在一个入口里，方便真正执行协作闭环。
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
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.approval-center-page__title {
  max-width: 16ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.15rem, 3.8vw, 3.7rem);
  line-height: 1.05;
}

.approval-center-page__subtitle,
.approval-center-page__footer-note {
  max-width: 64rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
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
