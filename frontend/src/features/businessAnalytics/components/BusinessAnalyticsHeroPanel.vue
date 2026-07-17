<script setup lang="ts">
import { reactive, watch } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import { formatCount, formatCurrency, formatPercent } from '@/utils/formatters';

interface StoreOption {
  value: string;
  label: string;
}

interface Props {
  storeOptions: StoreOption[];
  activeStoreId: string;
  selectedStoreName: string;
  analysisPeriodStart: string;
  analysisPeriodEnd: string;
  salesIncomeAmount: number;
  netProfitAmount: number;
  profitMarginRate: number;
  outstandingReceivableAmount: number;
  productCount: number;
  exceptionCount: number;
  riskHeadline: string;
  isRefreshing: boolean;
}

interface Emits {
  selectStore: [storeId: string];
  updatePeriod: [payload: { periodStart: string; periodEnd: string }];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const periodForm = reactive({
  periodStart: '',
  periodEnd: ''
});

watch(
  () => [props.analysisPeriodStart, props.analysisPeriodEnd],
  () => {
    periodForm.periodStart = props.analysisPeriodStart;
    periodForm.periodEnd = props.analysisPeriodEnd;
  },
  { immediate: true }
);

function handleStoreChange(event: Event) {
  emit('selectStore', (event.target as HTMLSelectElement).value);
}

function submitPeriod() {
  emit('updatePeriod', {
    periodStart: periodForm.periodStart,
    periodEnd: periodForm.periodEnd
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Business Analytics"
    title="把趋势、利润、异常和动作信号压成一页经营诊断台"
    description="先回答今天的经营质量，再决定要补货、修异常、调活动，还是先追回款。"
  >
    <div class="analytics-hero">
      <section class="analytics-hero__copy">
        <p class="analytics-hero__eyebrow">经营窗口</p>
        <h4 class="analytics-hero__title">{{ selectedStoreName }}</h4>
        <p class="analytics-hero__text">
          当前窗口 {{ analysisPeriodStart }} 至 {{ analysisPeriodEnd }}。
          风险提示：{{ riskHeadline }}
        </p>
      </section>

      <section class="analytics-hero__controls">
        <label class="analytics-hero__field">
          <span>店铺范围</span>
          <select
            class="analytics-hero__control"
            :value="activeStoreId"
            :disabled="isRefreshing"
            @change="handleStoreChange"
          >
            <option v-for="option in storeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="analytics-hero__field">
          <span>开始日期</span>
          <input
            v-model="periodForm.periodStart"
            class="analytics-hero__control"
            type="date"
            :disabled="isRefreshing"
          />
        </label>

        <label class="analytics-hero__field">
          <span>结束日期</span>
          <input
            v-model="periodForm.periodEnd"
            class="analytics-hero__control"
            type="date"
            :disabled="isRefreshing"
          />
        </label>

        <button
          class="analytics-hero__primary"
          type="button"
          :disabled="isRefreshing"
          @click="submitPeriod"
        >
          {{ isRefreshing ? '刷新中...' : '刷新分析窗口' }}
        </button>
      </section>
    </div>

    <div class="analytics-hero__metrics">
      <MetricCard
        label="销售收入"
        :value="formatCurrency(salesIncomeAmount)"
        caption="当前窗口内已纳入经营分析的销售额"
      />
      <MetricCard
        label="净利润"
        :value="formatCurrency(netProfitAmount)"
        caption="结合费用分摊后的真实利润结果"
      />
      <MetricCard
        label="利润率"
        :value="formatPercent(profitMarginRate, 1)"
        :caption="`待跟进异常 ${formatCount(exceptionCount, ' 个')}`"
      />
      <MetricCard
        label="待回款"
        :value="formatCurrency(outstandingReceivableAmount)"
        :caption="`纳入排行商品 ${formatCount(productCount, ' 个')}`"
      />
    </div>
  </PanelCard>
</template>

<style scoped>
.analytics-hero,
.analytics-hero__copy,
.analytics-hero__controls {
  display: grid;
  gap: 0.9rem;
}

.analytics-hero {
  grid-template-columns: 1.08fr 0.92fr;
  align-items: start;
}

.analytics-hero__copy,
.analytics-hero__controls {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.72);
}

.analytics-hero__eyebrow,
.analytics-hero__title,
.analytics-hero__text,
.analytics-hero__field span {
  margin: 0;
}

.analytics-hero__eyebrow,
.analytics-hero__field span {
  color: var(--color-ink-faint);
  font-size: 0.74rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.analytics-hero__text {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.analytics-hero__controls {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.analytics-hero__field {
  display: grid;
  gap: 0.45rem;
}

.analytics-hero__control,
.analytics-hero__primary {
  min-height: 2.8rem;
  border-radius: var(--radius-md);
}

.analytics-hero__control {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.16);
  background: rgba(255, 255, 255, 0.92);
  padding: 0 0.85rem;
}

.analytics-hero__primary {
  grid-column: span 2;
  border: 0;
  background: linear-gradient(135deg, #314839, #17221b);
  color: #fffaf4;
  font-weight: 600;
  cursor: pointer;
}

.analytics-hero__primary:disabled {
  opacity: 0.68;
  cursor: not-allowed;
}

.analytics-hero__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

@media (max-width: 1180px) {
  .analytics-hero,
  .analytics-hero__controls,
  .analytics-hero__metrics {
    grid-template-columns: 1fr;
  }

  .analytics-hero__primary {
    grid-column: span 1;
  }
}
</style>
