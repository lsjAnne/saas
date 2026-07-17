<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import type {
  PayableLedgerEntry,
  PurchaseCostCollectionView,
  SupplierReconciliationView
} from '@/services/apiTypes';
import { formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  payableLedgers: PayableLedgerEntry[];
  reconciliations: SupplierReconciliationView[];
  costCollections: PurchaseCostCollectionView[];
}

defineProps<Props>();
</script>

<template>
  <PanelCard
    eyebrow="Cost & Reconciliation"
    title="成本归集与对账摘要"
    description="先把应付台账、供应商对账和采购成本归集做成一个高密度摘要面，不把财务面提前拆得太散。"
  >
    <div class="cost-control">
      <div class="cost-control__grid">
        <div class="cost-control__block">
          <p class="cost-control__block-title">应付台账</p>
          <div v-if="payableLedgers.length" class="cost-control__list">
            <article
              v-for="entry in payableLedgers.slice(0, 4)"
              :key="entry.ledgerEntryId"
              class="cost-control__row"
            >
              <strong>{{ entry.ledgerNo }}</strong>
              <span>{{ entry.bizType }}</span>
              <span>{{ formatCurrency(entry.entryAmount) }}</span>
              <span>{{ formatDateTime(entry.occurredAt) }}</span>
            </article>
          </div>
          <p v-else class="cost-control__empty">当前范围下没有应付台账。</p>
        </div>

        <div class="cost-control__block">
          <p class="cost-control__block-title">供应商对账</p>
          <div v-if="reconciliations.length" class="cost-control__list">
            <article
              v-for="item in reconciliations.slice(0, 4)"
              :key="`${item.storeId}-${item.supplierId}`"
              class="cost-control__row"
            >
              <strong>{{ item.supplierName }}</strong>
              <span>{{ item.reconciliationStatus }}</span>
              <span>{{ formatCurrency(item.netPayableAmount) }}</span>
              <span>差异 {{ item.pendingDiscrepancyCount }}</span>
            </article>
          </div>
          <p v-else class="cost-control__empty">当前范围下没有供应商对账记录。</p>
        </div>
      </div>

      <div class="cost-control__block">
        <p class="cost-control__block-title">采购成本归集</p>
        <div v-if="costCollections.length" class="cost-control__list">
          <article
            v-for="item in costCollections.slice(0, 4)"
            :key="item.purchaseOrderId"
            class="cost-control__row cost-control__row--wide"
          >
            <strong>{{ item.purchaseOrderNo }}</strong>
            <span>基础 {{ formatCurrency(item.basePurchaseCost) }}</span>
            <span>费用 {{ formatCurrency(item.allocatedExpense) }}</span>
            <span>总成本 {{ formatCurrency(item.totalCollectedCost) }}</span>
            <span>单位 {{ formatCurrency(item.unitCollectedCost) }}</span>
          </article>
        </div>
        <p v-else class="cost-control__empty">当前范围下没有成本归集记录。</p>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.cost-control,
.cost-control__list {
  display: grid;
  gap: 0.9rem;
}

.cost-control__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.cost-control__block,
.cost-control__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.7);
}

.cost-control__block-title,
.cost-control__empty {
  margin: 0;
}

.cost-control__block-title {
  color: var(--color-ink-strong);
}

.cost-control__empty {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.cost-control__row {
  display: grid;
  grid-template-columns: 1fr 0.8fr 0.8fr 1fr;
  gap: 0.6rem;
  color: var(--color-ink-soft);
}

.cost-control__row--wide {
  grid-template-columns: 1fr repeat(4, 0.8fr);
}

@media (max-width: 1080px) {
  .cost-control__grid,
  .cost-control__row,
  .cost-control__row--wide {
    grid-template-columns: 1fr;
  }
}
</style>
