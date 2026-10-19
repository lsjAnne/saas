<script setup lang="ts">
import MetricCard from '@/components/cards/MetricCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { Supplier, SupplierSrmLinkage } from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface Summary {
  totalSuppliers: number;
  primarySuppliers: number;
  backupSuppliers: number;
  highRiskSuppliers: number;
  dropshipSuppliers: number;
  blacklistedSuppliers: number;
}

interface Props {
  selectedSupplier: Supplier | null;
  selectedStoreName: string;
  summary: Summary;
  srmLinkage: SupplierSrmLinkage | null;
  actionFeedback?: string | null;
}

withDefaults(defineProps<Props>(), {
  actionFeedback: null
});

defineEmits<{
  openCreate: [];
  openEdit: [];
}>();

const riskLevelLabelMap: Record<string, string> = {
  normal: '正常',
  low: '低风险',
  medium: '中风险',
  high: '高风险',
  critical: '紧急'
};

function resolveRiskLevelLabel(level: string | null | undefined) {
  return riskLevelLabelMap[(level ?? '').toLowerCase()] ?? level ?? '正常';
}
</script>

<template>
  <section class="supplier-hero">
    <div class="supplier-hero__copy">
      <p class="supplier-hero__eyebrow">供应商总控</p>
      <h3 class="supplier-hero__title">
        供应商主档、准入与结算
      </h3>
      <p class="supplier-hero__subtitle">
        当前聚焦店铺：{{ selectedStoreName }}。主档与关键动作稳定可用，逐步做深询价与绩效趋势。
      </p>

      <div class="supplier-hero__actions">
        <button class="supplier-hero__button supplier-hero__button--solid" type="button" @click="$emit('openCreate')">
          新增供应商
        </button>
        <button class="supplier-hero__button" type="button" @click="$emit('openEdit')">
          编辑当前供应商
        </button>
      </div>

      <p v-if="actionFeedback" class="supplier-hero__feedback">{{ actionFeedback }}</p>
    </div>

    <div class="supplier-hero__focus-card">
      <div class="supplier-hero__focus-head">
        <div>
          <p class="supplier-hero__focus-label">当前聚焦供应商</p>
          <h4 class="supplier-hero__focus-title">
            {{ selectedSupplier?.supplierName || '请选择供应商' }}
          </h4>
        </div>
        <StatusPill
          v-if="selectedSupplier"
          :label="resolveRiskLevelLabel(selectedSupplier.riskLevel)"
          :tone="selectedSupplier.blacklistFlag ? 'warn' : 'neutral'"
        />
      </div>

      <div v-if="selectedSupplier" class="supplier-hero__focus-meta">
        <span>{{ selectedSupplier.supplierPlatformType }}</span>
        <span>{{ selectedSupplier.supplierPlatformId }}</span>
        <span v-if="selectedSupplier.primary">主供应商</span>
        <span v-if="selectedSupplier.backup">备选供应商</span>
        <span v-if="selectedSupplier.dropshipSupportFlag">支持一件代发</span>
      </div>

      <div class="supplier-hero__focus-stats">
        <article class="supplier-hero__focus-item">
          <p class="supplier-hero__focus-item-label">询价次数</p>
          <p class="supplier-hero__focus-item-value">
            {{ formatCount(srmLinkage?.inquiryCount) }}
          </p>
        </article>
        <article class="supplier-hero__focus-item">
          <p class="supplier-hero__focus-item-label">预约记录</p>
          <p class="supplier-hero__focus-item-value">
            {{ formatCount(srmLinkage?.deliveryAppointmentCount) }}
          </p>
        </article>
        <article class="supplier-hero__focus-item">
          <p class="supplier-hero__focus-item-label">结算单</p>
          <p class="supplier-hero__focus-item-value">
            {{ formatCount(srmLinkage?.settlementStatementCount) }}
          </p>
        </article>
      </div>
    </div>
  </section>

  <section class="supplier-hero__metrics">
    <MetricCard label="供应商总量" :value="formatCount(summary.totalSuppliers)" caption="当前租户下的供应商主档数量" />
    <MetricCard label="主备供应商" :value="`${formatCount(summary.primarySuppliers)} / ${formatCount(summary.backupSuppliers)}`" caption="主供应商与备选供应商配置情况" />
    <MetricCard label="高风险供应商" :value="formatCount(summary.highRiskSuppliers)" caption="需要优先关注风险与替补的供应商" />
    <MetricCard label="支持代发" :value="formatCount(summary.dropshipSuppliers)" caption="支持一件代发的供应商数量" />
  </section>
</template>

<style scoped>
.supplier-hero,
.supplier-hero__metrics {
  display: grid;
  gap: 1rem;
}

.supplier-hero {
  grid-template-columns: 1.12fr 0.88fr;
}

.supplier-hero__copy,
.supplier-hero__focus-card {
  padding: 1.4rem;
  border-radius: var(--radius-shell);
  box-shadow: var(--shadow-card);
}

.supplier-hero__copy {
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.supplier-hero__focus-card {
  display: grid;
  gap: 1rem;
  background: var(--color-surface);
  border: 1px solid var(--color-line);
  color: var(--color-ink);
}

.supplier-hero__eyebrow,
.supplier-hero__title,
.supplier-hero__subtitle,
.supplier-hero__feedback,
.supplier-hero__focus-label,
.supplier-hero__focus-title,
.supplier-hero__focus-item-label,
.supplier-hero__focus-item-value {
  margin: 0;
}

.supplier-hero__eyebrow,
.supplier-hero__focus-label {
  font-size: 0.78rem;
  font-weight: 600;
}

.supplier-hero__eyebrow {
  color: var(--color-accent);
}

.supplier-hero__title {
  margin-top: 0.55rem;
  max-width: 24ch;
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
  line-height: 1.2;
}

.supplier-hero__subtitle,
.supplier-hero__feedback {
  margin-top: 0.8rem;
  max-width: 54rem;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.supplier-hero__feedback {
  color: var(--color-success);
}

.supplier-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  margin-top: 1.1rem;
}

.supplier-hero__button {
  min-height: 2.8rem;
  padding: 0 1.1rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
}

.supplier-hero__button--solid {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: #ffffff;
}

.supplier-hero__button--solid:hover:not(:disabled) {
  background: var(--color-accent-hover);
  color: #ffffff;
}

.supplier-hero__focus-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.supplier-hero__focus-label {
  color: var(--color-accent);
}

.supplier-hero__focus-title {
  margin-top: 0.45rem;
  color: var(--color-ink-strong);
  font-size: 1.2rem;
  font-weight: 600;
}

.supplier-hero__focus-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  color: var(--color-ink-soft);
  font-size: 0.86rem;
}

.supplier-hero__focus-meta span {
  display: inline-flex;
  min-height: 2rem;
  align-items: center;
  padding: 0 0.8rem;
  border-radius: var(--radius-pill);
  background: var(--color-panel-strong);
  border: 1px solid var(--color-line);
}

.supplier-hero__focus-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.supplier-hero__focus-item {
  padding: 0.9rem;
  border-radius: var(--radius-lg);
  background: var(--color-panel-strong);
  border: 1px solid var(--color-line);
}

.supplier-hero__focus-item-label {
  color: var(--color-ink-faint);
  font-size: 0.78rem;
}

.supplier-hero__focus-item-value {
  margin-top: 0.55rem;
  color: var(--color-ink-strong);
  font-size: 1.45rem;
  font-weight: 600;
}

.supplier-hero__metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

@media (max-width: 1180px) {
  .supplier-hero,
  .supplier-hero__metrics,
  .supplier-hero__focus-stats {
    grid-template-columns: 1fr;
  }
}
</style>
