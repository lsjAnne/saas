<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import CostControlPanel from '@/features/inventoryReplenishmentCenter/components/CostControlPanel.vue';
import InventoryReplenishmentHeroPanel from '@/features/inventoryReplenishmentCenter/components/InventoryReplenishmentHeroPanel.vue';
import InventorySnapshotPanel from '@/features/inventoryReplenishmentCenter/components/InventorySnapshotPanel.vue';
import ReplenishmentPipelinePanel from '@/features/inventoryReplenishmentCenter/components/ReplenishmentPipelinePanel.vue';
import WmsLinkagePanel from '@/features/inventoryReplenishmentCenter/components/WmsLinkagePanel.vue';
import { useInventoryReplenishmentCenterOverview } from '@/features/inventoryReplenishmentCenter/composables/useInventoryReplenishmentCenterOverview';
import type {
  CreatePurchaseOrderPayload,
  CreateReplenishmentTaskPayload,
  UpdateInventorySafetyStockPayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const inventoryCenter = useInventoryReplenishmentCenterOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await inventoryCenter.load(authStore.token);
}

async function withToken<T>(callback: (token: string) => Promise<T>) {
  if (!authStore.token) {
    return;
  }

  await callback(authStore.token);
}

async function handleSelectStore(storeId: string) {
  await withToken((token) => inventoryCenter.selectStore(storeId, token));
}

async function handleSelectSnapshot(snapshotId: string) {
  await withToken((token) => inventoryCenter.selectSnapshot(snapshotId, token));
}

async function handleSelectPurchaseRequest(purchaseRequestId: string) {
  await withToken((token) => inventoryCenter.selectPurchaseRequest(purchaseRequestId, token));
}

async function handleSelectPurchaseOrder(purchaseOrderId: string) {
  await withToken((token) => inventoryCenter.selectPurchaseOrder(purchaseOrderId, token));
}

async function handleUpdateSafetyStock(payload: UpdateInventorySafetyStockPayload) {
  await withToken((token) => inventoryCenter.submitSafetyStock(payload, token));
}

async function handleCreateTask(payload: CreateReplenishmentTaskPayload) {
  await withToken((token) => inventoryCenter.submitCreateReplenishmentTask(payload, token));
}

async function handleSubmitTaskApproval() {
  await withToken((token) => inventoryCenter.submitReplenishmentTaskApproval(token));
}

async function handleCreatePurchaseOrder(payload: CreatePurchaseOrderPayload) {
  await withToken((token) => inventoryCenter.submitCreatePurchaseOrder(payload, token));
}

async function handleSubmitPurchaseOrderApproval() {
  await withToken((token) => inventoryCenter.submitPurchaseOrderReview(token));
}

async function handleDispatchPurchaseOrder() {
  await withToken((token) => inventoryCenter.submitPurchaseOrderDispatch(token));
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="inventory-center-page">
    <header class="inventory-center-page__header">
      <p class="inventory-center-page__eyebrow">履约前链</p>
      <h2 class="inventory-center-page__title">库存补货</h2>
      <p class="inventory-center-page__subtitle">
        库存水位、补货计划与成本控制
      </p>
    </header>

    <InlineErrorCard
      v-if="inventoryCenter.errorMessage"
      :message="inventoryCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <InventoryReplenishmentHeroPanel
      :selected-store-name="inventoryCenter.selectedStoreName"
      :selected-snapshot="inventoryCenter.selectedSnapshot"
      :selected-task="inventoryCenter.selectedTask"
      :selected-purchase-order="inventoryCenter.selectedPurchaseOrder"
      :wms-linkage="inventoryCenter.wmsLinkage"
      :summary="inventoryCenter.summary"
      :action-feedback="inventoryCenter.actionFeedback"
    />

    <div class="inventory-center-page__grid">
      <InventorySnapshotPanel
        :stores="inventoryCenter.stores"
        :snapshots="inventoryCenter.filteredSnapshots"
        :selected-snapshot-id="inventoryCenter.selectedSnapshotId"
        :selected-snapshot="inventoryCenter.selectedSnapshot"
        :active-store-id="inventoryCenter.activeStoreId"
        :is-submitting="inventoryCenter.isRunningAction"
        :action-error="inventoryCenter.actionError"
        @select-store="handleSelectStore"
        @select-snapshot="handleSelectSnapshot"
        @update-safety-stock="handleUpdateSafetyStock"
        @create-task="handleCreateTask"
      />

      <div class="inventory-center-page__stack">
        <ReplenishmentPipelinePanel
          :tasks="inventoryCenter.filteredTasks"
          :purchase-requests="inventoryCenter.filteredPurchaseRequests"
          :purchase-orders="inventoryCenter.filteredPurchaseOrders"
          :selected-task-id="inventoryCenter.selectedTaskId"
          :selected-purchase-request-id="inventoryCenter.selectedPurchaseRequestId"
          :selected-purchase-order-id="inventoryCenter.selectedPurchaseOrderId"
          :selected-task="inventoryCenter.selectedTask"
          :selected-purchase-request="inventoryCenter.selectedPurchaseRequest"
          :selected-purchase-order="inventoryCenter.selectedPurchaseOrder"
          :is-submitting="inventoryCenter.isRunningAction"
          :action-error="inventoryCenter.actionError"
          @select-task="inventoryCenter.selectTask"
          @select-purchase-request="handleSelectPurchaseRequest"
          @select-purchase-order="handleSelectPurchaseOrder"
          @submit-task-approval="handleSubmitTaskApproval"
          @create-purchase-order="handleCreatePurchaseOrder"
          @submit-purchase-order-approval="handleSubmitPurchaseOrderApproval"
          @dispatch-purchase-order="handleDispatchPurchaseOrder"
        />

        <WmsLinkagePanel
          :linkage="inventoryCenter.wmsLinkage"
          :receipts="inventoryCenter.filteredPurchaseReceipts"
          :discrepancies="inventoryCenter.filteredPurchaseDiscrepancies"
          :selected-store-name="inventoryCenter.selectedStoreName"
        />

        <CostControlPanel
          :payable-ledgers="inventoryCenter.filteredPayableLedgers"
          :reconciliations="inventoryCenter.filteredSupplierReconciliations"
          :cost-collections="inventoryCenter.filteredPurchaseCostCollections"
        />
      </div>
    </div>

    <p v-if="inventoryCenter.isLoading" class="inventory-center-page__footer-note">
      正在回读库存快照、补货任务、采购链路、WMS 摘要和成本对账...
    </p>
  </section>
</template>

<style scoped>
.inventory-center-page {
  display: grid;
  gap: 1.2rem;
}

.inventory-center-page__header {
  display: grid;
  gap: 0.75rem;
}

.inventory-center-page__eyebrow,
.inventory-center-page__title,
.inventory-center-page__subtitle,
.inventory-center-page__footer-note {
  margin: 0;
}

.inventory-center-page__eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.inventory-center-page__title {
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
  line-height: 1.2;
}

.inventory-center-page__subtitle,
.inventory-center-page__footer-note {
  max-width: 64rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.inventory-center-page__grid {
  display: grid;
  grid-template-columns: 0.94fr 1.06fr;
  gap: 1rem;
  align-items: start;
}

.inventory-center-page__stack {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1180px) {
  .inventory-center-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
