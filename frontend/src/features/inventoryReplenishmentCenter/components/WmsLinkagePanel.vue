<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  PurchaseReceipt,
  PurchaseReceiptDiscrepancy,
  WmsLinkageView
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  linkage: WmsLinkageView | null;
  receipts: PurchaseReceipt[];
  discrepancies: PurchaseReceiptDiscrepancy[];
  selectedStoreName: string;
}

defineProps<Props>();
</script>

<template>
  <PanelCard
    eyebrow="WMS Linkage"
    title="WMS 联动与收货差异"
    description="这里不做复杂库位图，先把仓库、波次、锁批次、盘点和逆向入库的关键状态集中呈现。"
  >
    <div class="wms-linkage">
      <div class="wms-linkage__summary">
        <article class="wms-linkage__metric">
          <p class="wms-linkage__metric-label">仓库数</p>
          <h4 class="wms-linkage__metric-value">{{ linkage?.warehouseCount ?? 0 }}</h4>
        </article>
        <article class="wms-linkage__metric">
          <p class="wms-linkage__metric-label">活跃波次</p>
          <h4 class="wms-linkage__metric-value">{{ linkage?.activeWaveCount ?? 0 }}</h4>
        </article>
        <article class="wms-linkage__metric">
          <p class="wms-linkage__metric-label">锁批次数</p>
          <h4 class="wms-linkage__metric-value">{{ linkage?.lockedBatchCount ?? 0 }}</h4>
        </article>
        <article class="wms-linkage__metric">
          <p class="wms-linkage__metric-label">盘点任务</p>
          <h4 class="wms-linkage__metric-value">{{ linkage?.cycleCountTaskCount ?? 0 }}</h4>
        </article>
      </div>

      <div class="wms-linkage__status">
        <StatusPill :label="`OMS ${linkage?.omsSyncStatus || 'unknown'}`" tone="success" />
        <StatusPill :label="`ERP ${linkage?.erpSyncStatus || 'unknown'}`" tone="neutral" />
        <StatusPill :label="`TMS ${linkage?.tmsHandoverStatus || 'unknown'}`" tone="warn" />
        <p class="wms-linkage__status-copy">当前焦点店铺：{{ selectedStoreName }}</p>
      </div>

      <div class="wms-linkage__grid">
        <div class="wms-linkage__block">
          <p class="wms-linkage__block-title">最近收货</p>
          <div v-if="receipts.length" class="wms-linkage__list">
            <article v-for="receipt in receipts.slice(0, 4)" :key="receipt.purchaseReceiptId" class="wms-linkage__row">
              <strong>{{ receipt.receiptNo }}</strong>
              <span>{{ receipt.inboundStatus }}</span>
              <span>{{ formatDateTime(receipt.receivedAt) }}</span>
            </article>
          </div>
          <p v-else class="wms-linkage__empty">当前范围下还没有收货记录。</p>
        </div>

        <div class="wms-linkage__block">
          <p class="wms-linkage__block-title">收货差异</p>
          <div v-if="discrepancies.length" class="wms-linkage__list">
            <article
              v-for="item in discrepancies.slice(0, 4)"
              :key="item.discrepancyId"
              class="wms-linkage__row"
            >
              <strong>{{ item.discrepancyNo }}</strong>
              <span>{{ item.discrepancyType }}</span>
              <span>差异 {{ item.discrepancyQty }}</span>
            </article>
          </div>
          <p v-else class="wms-linkage__empty">当前范围下没有收货差异。</p>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.wms-linkage,
.wms-linkage__status,
.wms-linkage__list {
  display: grid;
  gap: 0.9rem;
}

.wms-linkage__summary,
.wms-linkage__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

.wms-linkage__grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.wms-linkage__metric,
.wms-linkage__block,
.wms-linkage__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.7);
}

.wms-linkage__metric-label,
.wms-linkage__metric-value,
.wms-linkage__status-copy,
.wms-linkage__block-title,
.wms-linkage__empty {
  margin: 0;
}

.wms-linkage__metric-label,
.wms-linkage__status-copy,
.wms-linkage__empty {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.wms-linkage__metric-value,
.wms-linkage__block-title {
  color: var(--color-ink-strong);
}

.wms-linkage__status {
  grid-template-columns: repeat(3, auto) 1fr;
  align-items: center;
}

.wms-linkage__row {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr 1fr;
  gap: 0.6rem;
  color: var(--color-ink-soft);
}

@media (max-width: 1080px) {
  .wms-linkage__summary,
  .wms-linkage__grid,
  .wms-linkage__status {
    grid-template-columns: 1fr;
  }
}
</style>
