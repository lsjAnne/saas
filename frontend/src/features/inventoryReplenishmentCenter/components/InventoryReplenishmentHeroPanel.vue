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
    eyebrow="Inventory & Replenishment"
    title="把库存快照、补货决策、采购推进和 WMS 联动压进同一条履约前链"
    description="这一页不是静态库存总表，而是让运营先发现低库存，再决定补货、采购和仓内联动动作的主控制台。"
    dark
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
      <StatusPill :label="`OMS ${wmsLinkage?.omsSyncStatus || 'unknown'}`" tone="success" />
      <StatusPill :label="`ERP ${wmsLinkage?.erpSyncStatus || 'unknown'}`" tone="neutral" />
      <StatusPill :label="`TMS ${wmsLinkage?.tmsHandoverStatus || 'unknown'}`" tone="warn" />
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
  background: rgba(255, 248, 240, 0.08);
  border: 1px solid rgba(255, 244, 236, 0.08);
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
  color: rgba(255, 244, 236, 0.72);
  font-size: 0.76rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.inventory-hero__title,
.inventory-hero__focus-value {
  color: #fff8f0;
}

.inventory-hero__title {
  font-family: var(--font-display);
  font-size: clamp(1.9rem, 3vw, 2.7rem);
}

.inventory-hero__copy,
.inventory-hero__feedback,
.inventory-hero__focus-copy,
.inventory-hero__tail-copy {
  color: rgba(255, 244, 236, 0.78);
  line-height: 1.7;
}

.inventory-hero__feedback {
  color: #f6d8b5;
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
