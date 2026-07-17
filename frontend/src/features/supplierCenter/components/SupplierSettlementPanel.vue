<script setup lang="ts">
import { reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  CreateSupplierSettlementStatementPayload,
  Supplier,
  SupplierSettlementStatement
} from '@/services/apiTypes';
import { formatCurrency, formatDate, formatDateTime } from '@/utils/formatters';

interface Props {
  selectedSupplier: Supplier | null;
  settlementStatements: SupplierSettlementStatement[];
  isSubmitting: boolean;
  actionError?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<{
  submitSettlement: [payload: CreateSupplierSettlementStatementPayload];
}>();

const form = reactive({
  statementPeriod: '',
  accountPeriodDays: '30',
  payableAmount: '0',
  dueDate: ''
});

watch(
  () => props.selectedSupplier?.supplierId,
  () => {
    const now = new Date();
    const month = `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}`;
    form.statementPeriod = `${month}`;
    form.accountPeriodDays = '30';
    form.payableAmount = '0';
    form.dueDate = '';
  },
  { immediate: true }
);

function toneFromStatus(status: string) {
  const normalized = status.toLowerCase();
  if (normalized.includes('pending') || normalized.includes('draft')) {
    return 'warn';
  }
  if (normalized.includes('settled')) {
    return 'success';
  }
  return 'neutral';
}

function handleSubmit() {
  if (!props.selectedSupplier) {
    return;
  }

  emit('submitSettlement', {
    storeId: props.selectedSupplier.storeId,
    supplierId: props.selectedSupplier.supplierId,
    statementPeriod: form.statementPeriod,
    accountPeriodDays: Number(form.accountPeriodDays),
    payableAmount: Number(form.payableAmount),
    dueDate: form.dueDate
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Settlement & Summary"
    title="结算单与对账摘要"
    description="先把结算单主链打通，让采购与财务至少能在供应商页看到真实对账状态。"
  >
    <div class="supplier-settlement">
      <form class="supplier-settlement__composer" @submit.prevent="handleSubmit">
        <label class="supplier-settlement__field">
          <span>账期月份</span>
          <input v-model="form.statementPeriod" :disabled="!selectedSupplier || isSubmitting" placeholder="2026-06" />
        </label>
        <label class="supplier-settlement__field">
          <span>账期天数</span>
          <input v-model="form.accountPeriodDays" :disabled="!selectedSupplier || isSubmitting" type="number" min="1" />
        </label>
        <label class="supplier-settlement__field">
          <span>应付金额</span>
          <input v-model="form.payableAmount" :disabled="!selectedSupplier || isSubmitting" type="number" min="0" />
        </label>
        <label class="supplier-settlement__field">
          <span>到期日</span>
          <input v-model="form.dueDate" :disabled="!selectedSupplier || isSubmitting" type="date" />
        </label>
        <button class="supplier-settlement__submit" type="submit" :disabled="!selectedSupplier || isSubmitting">
          生成结算单
        </button>
      </form>

      <p v-if="actionError" class="supplier-settlement__error">{{ actionError }}</p>

      <div v-if="settlementStatements.length" class="supplier-settlement__list">
        <article
          v-for="statement in settlementStatements"
          :key="statement.statementId"
          class="supplier-settlement__item"
        >
          <div class="supplier-settlement__item-head">
            <div>
              <h4 class="supplier-settlement__item-title">{{ statement.statementPeriod }}</h4>
              <p class="supplier-settlement__item-meta">
                到期 {{ formatDate(statement.dueDate) }} · 创建 {{ formatDateTime(statement.createdAt) }}
              </p>
            </div>
            <StatusPill :label="statement.settlementStatus" :tone="toneFromStatus(statement.settlementStatus)" />
          </div>
          <p class="supplier-settlement__item-amount">
            {{ formatCurrency(statement.payableAmount) }}
          </p>
        </article>
      </div>

      <div v-else class="supplier-settlement__empty">
        当前供应商还没有结算单记录，先生成第一张对账结算单。
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.supplier-settlement,
.supplier-settlement__composer,
.supplier-settlement__list {
  display: grid;
  gap: 0.9rem;
}

.supplier-settlement__composer {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: end;
}

.supplier-settlement__field {
  display: grid;
  gap: 0.45rem;
}

.supplier-settlement__field span {
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.supplier-settlement__field input {
  min-height: 2.75rem;
  padding: 0 0.9rem;
  border-radius: 1rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.78);
  color: var(--color-ink-strong);
}

.supplier-settlement__submit {
  min-height: 2.75rem;
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-success);
  color: #fff8f0;
}

.supplier-settlement__item,
.supplier-settlement__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.7);
}

.supplier-settlement__item-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.supplier-settlement__item-title,
.supplier-settlement__item-meta,
.supplier-settlement__item-amount,
.supplier-settlement__empty,
.supplier-settlement__error {
  margin: 0;
}

.supplier-settlement__item-title {
  color: var(--color-ink-strong);
}

.supplier-settlement__item-meta,
.supplier-settlement__empty {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.supplier-settlement__item-amount {
  margin-top: 0.8rem;
  color: var(--color-ink-strong);
  font-size: 1.5rem;
  font-weight: 600;
}

.supplier-settlement__error {
  color: var(--color-danger);
}

@media (max-width: 1180px) {
  .supplier-settlement__composer {
    grid-template-columns: 1fr;
  }
}
</style>
