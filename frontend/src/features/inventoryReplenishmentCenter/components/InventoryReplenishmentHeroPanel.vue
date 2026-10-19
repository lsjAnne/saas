<script setup lang="ts">
import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  InventorySnapshot,
  PurchaseOrder,
  ReplenishmentTask,
  WmsLinkageView
} from '@/services/apiTypes';
import { formatCurrency, formatDateTime } from '@/utils/formatters';

interface Summary {
  totalSnapshots: number;
  lowStockCount: number;
  pendingApprovalTaskCount: number;
  pendingApprovalOrderCount: number;
  discrepancyCount: number;
  payableAmount: number;
}

interface Props {
  selectedStoreName: string;
  selectedSnapshot: InventorySnapshot | null;
  selectedTask: ReplenishmentTask | null;
  selectedPurchaseOrder: PurchaseOrder | null;
  wmsLinkage: WmsLinkageView | null;
  summary: Summary;
  actionFeedback?: string | null;
}

withDefaults(defineProps<Props>(), {
  actionFeedback: null
});
</script>

<template>
  <PanelCard
    title="库存总览"
    description="低库存识别、补货审批、采购推进与仓内联动的主控制台。"
  >
    <div class="inventory-hero">
      <div class="inventory-hero__lead">
        <p class="inventory-hero__eyebrow">当前焦点范围</p>
        <h4 class="inventory-hero__title">{{ selectedStoreName }}</h4>
        <p class="inventory-hero__copy">
          当前范围下有 {{ summary.lowStockCount }} 个低库存快照、{{ summary.pendingApprovalTaskCount }}
          个待审批补货任务、{{ summary.pendingApprovalOrderCount }} 个待审批采购单。
        </p>
        <p v-if="actionFeedback" class="inventory-hero__feedback">{{ actionFeedback }}</p>
      </div>

      <div class="inventory-hero__focus">
        <div class="inventory-hero__focus-item">
          <p class="inventory-hero__focus-label">当前 SKU</p>
          <h5 class="inventory-hero__focus-value">{{ selectedSnapshot?.skuId || '--' }}</h5>
          <p class="inventory-hero__focus-copy">
            可用 {{ selectedSnapshot?.availableStock ?? '--' }} / 安全
            {{ selectedSnapshot?.safetyStock ?? '--' }}
          </p>
        </div>
        <div class="inventory-hero__focus-item">
          <p class="inventory-hero__focus-label">补货任务</p>
          <h5 class="inventory-hero__focus-value">{{ selectedTask?.replenishmentTaskId || '--' }}</h5>
          <p class="inventory-hero__focus-copy">
            {{ selectedTask?.taskStatus || '待选择' }} / {{ selectedTask?.approvalStatus || '待选择' }}
          </p>
        </div>
        <div class="inventory-hero__focus-item">
          <p class="inventory-hero__focus-label">采购单</p>
          <h5 class="inventory-hero__focus-value">{{ selectedPurchaseOrder?.orderNo || '--' }}</h5>
          <p class="inventory-hero__focus-copy">
            {{ selectedPurchaseOrder?.orderStatus || '待选择' }} / {{ selectedPurchaseOrder?.dispatchStatus || '待选择' }}
          </p>
        </div>
      </div>
    </div>

    <div class="inventory-hero__metrics">
      <MetricCard
        label="库存快照"
        :value="`${summary.totalSnapshots}`"
        caption="当前筛选范围的库存样本数"
      />
      <MetricCard
        label="待处理差异"
        :value="`${summary.discrepancyCount}`"
        caption="收货差异与仓内异常压力"
      />
      <MetricCard
        label="应付净额"
        :value="formatCurrency(summary.payableAmount)"
        caption="基于应付台账聚合"
      />
      <MetricCard
        label="WMS 仓库"
        :value="`${wmsLinkage?.warehouseCount ?? 0}`"
        :caption="`活跃波次 ${wmsLinkage?.activeWaveCount ?? 0}`"
      />
    </div>

    <div class="inventory-hero__tail">
      <StatusPill :label="`OMS ${wmsLinkage?.omsSyncStatus || '未知'}`" tone="success" />
      <StatusPill :label="`ERP ${wmsLinkage?.erpSyncStatus || '未知'}`" tone="neutral" />
      <StatusPill :label="`TMS ${wmsLinkage?.tmsHandoverStatus || '未知'}`" tone="warn" />
      <p class="inventory-hero__tail-copy">
        最近快照时间 {{ formatDateTime(selectedSnapshot?.snapshotAt) }}，当前仓内联动重点是锁批次
        {{ wmsLinkage?.lockedBatchCount ?? 0 }}、盘点 {{ wmsLinkage?.cycleCountTaskCount ?? 0 }}、逆向入库
        {{ wmsLinkage?.reverseInboundCount ?? 0 }}。
      </p>
    </div>
  </PanelCard>
</template>

<style scoped>
.inventory-hero,
.inventory-hero__lead,
.inventory-hero__focus,
.inventory-hero__tail {
  display: grid;
  gap: 0.9rem;
}

.inventory-hero {
  grid-template-columns: 1.06fr 0.94fr;
}

.inventory-hero__lead,
.inventory-hero__focus-item {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.inventory-hero__eyebrow,
.inventory-hero__title,
.inventory-hero__copy,
.inventory-hero__feedback,
.inventory-hero__focus-label,
.inventory-hero__focus-value,
.inventory-hero__focus-copy,
.inventory-hero__tail-copy {
  margin: 0;
}

.inventory-hero__eyebrow,
.inventory-hero__focus-label {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.inventory-hero__title,
.inventory-hero__focus-value {
  color: var(--color-ink-strong);
}

.inventory-hero__title {
  font-size: 1.5rem;
  font-weight: 600;
}

.inventory-hero__copy,
.inventory-hero__feedback,
.inventory-hero__focus-copy,
.inventory-hero__tail-copy {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.inventory-hero__feedback {
  color: var(--color-success);
}

.inventory-hero__focus {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.inventory-hero__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
  margin-top: 1rem;
}

.inventory-hero__tail {
  margin-top: 1rem;
  grid-template-columns: repeat(3, auto) 1fr;
  align-items: center;
}

@media (max-width: 1180px) {
  .inventory-hero,
  .inventory-hero__focus,
  .inventory-hero__metrics,
  .inventory-hero__tail {
    grid-template-columns: 1fr;
  }
}
</style>
