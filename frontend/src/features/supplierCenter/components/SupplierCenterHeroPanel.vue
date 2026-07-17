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
</script>

<template>
  <section class="supplier-hero">
    <div class="supplier-hero__copy">
      <p class="supplier-hero__eyebrow">Supplier Control Tower</p>
      <h3 class="supplier-hero__title">
        把供应商主档、准入、预约和结算链路收进一张真正可执行的 SRM 前台
      </h3>
      <p class="supplier-hero__subtitle">
        当前聚焦店铺：{{ selectedStoreName }}。先让主档与关键动作稳定可用，再逐步把询价和绩效趋势做深。
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
          :label="selectedSupplier.riskLevel || 'normal'"
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
  background:
    radial-gradient(circle at top right, rgba(217, 192, 163, 0.22), transparent 36%),
    linear-gradient(150deg, rgba(255, 251, 245, 0.96), rgba(242, 233, 223, 0.94));
  border: 1px solid rgba(91, 72, 54, 0.1);
}

.supplier-hero__focus-card {
  display: grid;
  gap: 1rem;
  background: linear-gradient(145deg, #334035, #222920);
  color: #fff8f0;
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
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.supplier-hero__eyebrow {
  color: var(--color-ink-faint);
}

.supplier-hero__title {
  margin-top: 0.55rem;
  max-width: 14ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3.6rem);
  line-height: 1.05;
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
  border: 1px solid rgba(91, 72, 54, 0.16);
  background: rgba(255, 255, 255, 0.6);
  color: var(--color-ink-strong);
}

.supplier-hero__button--solid {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: #fff8f0;
}

.supplier-hero__focus-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.supplier-hero__focus-label {
  color: rgba(255, 244, 236, 0.64);
}

.supplier-hero__focus-title {
  margin-top: 0.45rem;
  font-family: var(--font-display);
  font-size: 1.55rem;
}

.supplier-hero__focus-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  color: rgba(255, 244, 236, 0.8);
  font-size: 0.86rem;
}

.supplier-hero__focus-meta span {
  display: inline-flex;
  min-height: 2rem;
  align-items: center;
  padding: 0 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.08);
}

.supplier-hero__focus-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.supplier-hero__focus-item {
  padding: 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.08);
}

.supplier-hero__focus-item-label {
  color: rgba(255, 244, 236, 0.64);
  font-size: 0.78rem;
}

.supplier-hero__focus-item-value {
  margin-top: 0.55rem;
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
