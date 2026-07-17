<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import SupplierCenterHeroPanel from '@/features/supplierCenter/components/SupplierCenterHeroPanel.vue';
import SupplierDirectoryPanel from '@/features/supplierCenter/components/SupplierDirectoryPanel.vue';
import SupplierExecutionPanel from '@/features/supplierCenter/components/SupplierExecutionPanel.vue';
import SupplierProfilePanel from '@/features/supplierCenter/components/SupplierProfilePanel.vue';
import SupplierSettlementPanel from '@/features/supplierCenter/components/SupplierSettlementPanel.vue';
import { useSupplierCenterOverview } from '@/features/supplierCenter/composables/useSupplierCenterOverview';
import type {
  CreateSupplierAdmissionReviewPayload,
  CreateSupplierDeliveryAppointmentPayload,
  CreateSupplierPayload,
  CreateSupplierRiskEventPayload,
  CreateSupplierScorecardPayload,
  CreateSupplierSettlementStatementPayload,
  UpdateSupplierPayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const supplierCenter = useSupplierCenterOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await supplierCenter.load(authStore.token);
}

async function handleSelectStore(storeId: string) {
  if (!authStore.token) {
    return;
  }

  await supplierCenter.selectStore(storeId, authStore.token);
}

async function handleSelectSupplier(supplierId: string) {
  if (!authStore.token) {
    return;
  }

  await supplierCenter.selectSupplier(supplierId, authStore.token);
}

async function handleCreateSupplier(payload: CreateSupplierPayload) {
  if (!authStore.token) {
    return;
  }

  await supplierCenter.submitCreateSupplier(payload, authStore.token);
}

async function handleUpdateSupplier(payload: UpdateSupplierPayload) {
  if (!authStore.token) {
    return;
  }

  await supplierCenter.submitUpdateSupplier(payload, authStore.token);
}

async function handleSetPrimary() {
  if (!authStore.token) {
    return;
  }

  await supplierCenter.markPrimarySupplier(authStore.token);
}

async function handleSetBackup() {
  if (!authStore.token) {
    return;
  }

  await supplierCenter.markBackupSupplier(authStore.token);
}

async function handleAdmissionReview(payload: CreateSupplierAdmissionReviewPayload) {
  if (!authStore.token) {
    return;
  }

  await supplierCenter.submitAdmissionReview(payload, authStore.token);
}

async function handleScorecard(payload: CreateSupplierScorecardPayload) {
  if (!authStore.token) {
    return;
  }

  await supplierCenter.submitScorecard(payload, authStore.token);
}

async function handleDeliveryAppointment(
  payload: CreateSupplierDeliveryAppointmentPayload
) {
  if (!authStore.token) {
    return;
  }

  await supplierCenter.submitDeliveryAppointment(payload, authStore.token);
}

async function handleRiskEvent(payload: CreateSupplierRiskEventPayload) {
  if (!authStore.token) {
    return;
  }

  await supplierCenter.submitRiskEvent(payload, authStore.token);
}

async function handleSettlement(payload: CreateSupplierSettlementStatementPayload) {
  if (!authStore.token) {
    return;
  }

  await supplierCenter.submitSettlementStatement(payload, authStore.token);
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="supplier-center-page">
    <header class="supplier-center-page__header">
      <p class="supplier-center-page__eyebrow">Supplier Center</p>
      <h2 class="supplier-center-page__title">
        让供应商主档、准入与结算在一张高密度 SRM 页面里闭环
      </h2>
      <p class="supplier-center-page__subtitle">
        这不是纯展示页。当前版本已经把主档、主备切换、准入评审、评分卡、到货预约、风险事件和结算单接进真实接口，后续再逐步扩询价和趋势分析。
      </p>
    </header>

    <InlineErrorCard
      v-if="supplierCenter.errorMessage"
      :message="supplierCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <SupplierCenterHeroPanel
      :selected-supplier="supplierCenter.selectedSupplier"
      :selected-store-name="supplierCenter.selectedStoreName"
      :summary="supplierCenter.summary"
      :srm-linkage="supplierCenter.srmLinkage"
      :action-feedback="supplierCenter.actionFeedback"
      @open-create="supplierCenter.openCreateProfile"
      @open-edit="supplierCenter.openEditProfile"
    />

    <div class="supplier-center-page__grid">
      <SupplierDirectoryPanel
        :stores="supplierCenter.stores"
        :suppliers="supplierCenter.filteredSuppliers"
        :selected-supplier-id="supplierCenter.selectedSupplierId"
        :active-store-id="supplierCenter.activeStoreId"
        @select-store="handleSelectStore"
        @select-supplier="handleSelectSupplier"
      />

      <div class="supplier-center-page__stack">
        <SupplierProfilePanel
          :mode="supplierCenter.profileMode"
          :stores="supplierCenter.stores"
          :active-store-id="supplierCenter.activeStoreId"
          :supplier="supplierCenter.selectedSupplier"
          :supplier-detail="supplierCenter.selectedSupplierDetail"
          :is-submitting="supplierCenter.isRunningAction"
          :action-error="supplierCenter.actionError"
          @open-create="supplierCenter.openCreateProfile"
          @open-edit="supplierCenter.openEditProfile"
          @submit-create="handleCreateSupplier"
          @submit-update="handleUpdateSupplier"
          @set-primary="handleSetPrimary"
          @set-backup="handleSetBackup"
        />

        <SupplierExecutionPanel
          :selected-supplier="supplierCenter.selectedSupplier"
          :srm-linkage="supplierCenter.srmLinkage"
          :latest-admission-review="supplierCenter.latestAdmissionReview"
          :latest-scorecard="supplierCenter.latestScorecard"
          :latest-delivery-appointment="supplierCenter.latestDeliveryAppointment"
          :latest-risk-event="supplierCenter.latestRiskEvent"
          :is-submitting="supplierCenter.isRunningAction"
          :action-error="supplierCenter.actionError"
          @submit-admission-review="handleAdmissionReview"
          @submit-scorecard="handleScorecard"
          @submit-delivery-appointment="handleDeliveryAppointment"
          @submit-risk-event="handleRiskEvent"
        />

        <SupplierSettlementPanel
          :selected-supplier="supplierCenter.selectedSupplier"
          :settlement-statements="supplierCenter.settlementStatements"
          :is-submitting="supplierCenter.isRunningAction"
          :action-error="supplierCenter.actionError"
          @submit-settlement="handleSettlement"
        />
      </div>
    </div>

    <p v-if="supplierCenter.isLoading" class="supplier-center-page__footer-note">
      正在回读供应商主档、SRM 联动摘要和结算单上下文...
    </p>
  </section>
</template>

<style scoped>
.supplier-center-page {
  display: grid;
  gap: 1.2rem;
}

.supplier-center-page__header {
  display: grid;
  gap: 0.75rem;
}

.supplier-center-page__eyebrow,
.supplier-center-page__title,
.supplier-center-page__subtitle,
.supplier-center-page__footer-note {
  margin: 0;
}

.supplier-center-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.supplier-center-page__title {
  max-width: 15ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 3.8rem);
  line-height: 1.04;
}

.supplier-center-page__subtitle,
.supplier-center-page__footer-note {
  max-width: 64rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.supplier-center-page__grid {
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  gap: 1rem;
  align-items: start;
}

.supplier-center-page__stack {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1180px) {
  .supplier-center-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
