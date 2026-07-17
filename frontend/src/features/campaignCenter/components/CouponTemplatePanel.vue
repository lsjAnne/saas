<script setup lang="ts">
import { reactive, shallowRef, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  CampaignActivity,
  CouponTemplate,
  CreateCouponTemplatePayload,
  Store
} from '@/services/apiTypes';
import { formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  templates: CouponTemplate[];
  stores: Store[];
  activeStoreId: string;
  selectedCampaign: CampaignActivity | null;
  isSubmitting: boolean;
  errorMessage?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  errorMessage: null
});

const emit = defineEmits<{
  submitTemplate: [payload: CreateCouponTemplatePayload];
}>();

const localError = shallowRef<string | null>(null);

const form = reactive({
  storeId: '',
  templateName: '',
  discountType: 'amount',
  discountValue: '20',
  thresholdAmount: '99',
  status: 'enabled'
});

function resolveTone(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase() === 'enabled' ? 'success' : 'warn';
}

function resolveDefaultStoreId() {
  if (props.selectedCampaign?.storeId) {
    return props.selectedCampaign.storeId;
  }

  if (props.activeStoreId !== 'all') {
    return props.activeStoreId;
  }

  return props.stores[0]?.storeId ?? '';
}

function resetForm() {
  form.storeId = resolveDefaultStoreId();
  form.templateName = '';
  form.discountType = 'amount';
  form.discountValue = '20';
  form.thresholdAmount = '99';
  form.status = 'enabled';
}

watch(
  () => [props.activeStoreId, props.selectedCampaign?.campaignId, props.stores.length],
  () => {
    localError.value = null;
    resetForm();
  },
  { immediate: true }
);

function toPositiveNumber(value: string, fieldName: string) {
  const parsed = Number(value);

  if (Number.isNaN(parsed) || parsed <= 0) {
    throw new Error(`${fieldName} 必须大于 0`);
  }

  return parsed;
}

function toOptionalNumber(value: string) {
  if (!value.trim()) {
    return undefined;
  }

  const parsed = Number(value);

  if (Number.isNaN(parsed) || parsed < 0) {
    throw new Error('门槛金额必须大于等于 0');
  }

  return parsed;
}

function submitTemplate() {
  try {
    localError.value = null;

    if (!form.storeId.trim()) {
      throw new Error('请选择店铺');
    }

    if (!form.templateName.trim()) {
      throw new Error('请输入模板名称');
    }

    emit('submitTemplate', {
      storeId: form.storeId.trim(),
      templateName: form.templateName.trim(),
      discountType: form.discountType,
      discountValue: toPositiveNumber(form.discountValue, '优惠值'),
      thresholdAmount: toOptionalNumber(form.thresholdAmount),
      status: form.status
    });
  } catch (error) {
    localError.value = error instanceof Error ? error.message : '模板表单校验失败';
  }
}

function resolveStoreName(storeId: string) {
  return props.stores.find((store) => store.storeId === storeId)?.shopName ?? storeId;
}

function formatTemplateValue(template: CouponTemplate) {
  if (template.discountType === 'percentage') {
    return `${(template.discountValue * 10).toFixed(1)} 折`;
  }

  return formatCurrency(template.discountValue);
}
</script>

<template>
  <PanelCard
    eyebrow="Coupon Pool"
    title="优惠券模板池"
    description="活动页直接维护模板池，保证活动创建时可以立刻绑定，不需要再跳去别的模块。"
  >
    <div class="coupon-panel">
      <div class="coupon-panel__list">
        <article v-for="template in templates" :key="template.couponTemplateId" class="coupon-panel__item">
          <div class="coupon-panel__item-head">
            <div>
              <p class="coupon-panel__item-store">{{ resolveStoreName(template.storeId) }}</p>
              <h4 class="coupon-panel__item-title">{{ template.templateName }}</h4>
            </div>
            <StatusPill :label="template.status" :tone="resolveTone(template.status)" />
          </div>
          <dl class="coupon-panel__facts">
            <div class="coupon-panel__fact">
              <dt>优惠方式</dt>
              <dd>{{ template.discountType }}</dd>
            </div>
            <div class="coupon-panel__fact">
              <dt>优惠值</dt>
              <dd>{{ formatTemplateValue(template) }}</dd>
            </div>
            <div class="coupon-panel__fact">
              <dt>门槛</dt>
              <dd>{{ formatCurrency(template.thresholdAmount) }}</dd>
            </div>
            <div class="coupon-panel__fact">
              <dt>创建时间</dt>
              <dd>{{ formatDateTime(template.createdAt) }}</dd>
            </div>
          </dl>
        </article>

        <p v-if="!templates.length" class="coupon-panel__empty">
          还没有优惠券模板。先在右侧创建一个，活动表单就能直接绑定。
        </p>
      </div>

      <form class="coupon-panel__form" @submit.prevent="submitTemplate">
        <p v-if="localError || errorMessage" class="coupon-panel__error">
          {{ localError || errorMessage }}
        </p>

        <label class="coupon-panel__field">
          <span>店铺</span>
          <select v-model="form.storeId">
            <option v-for="store in stores" :key="store.storeId" :value="store.storeId">
              {{ store.shopName }}
            </option>
          </select>
        </label>

        <label class="coupon-panel__field">
          <span>模板名称</span>
          <input v-model="form.templateName" type="text" required />
        </label>

        <label class="coupon-panel__field">
          <span>优惠方式</span>
          <select v-model="form.discountType">
            <option value="amount">金额减免</option>
            <option value="percentage">百分比折扣</option>
          </select>
        </label>

        <label class="coupon-panel__field">
          <span>优惠值</span>
          <input
            v-model="form.discountValue"
            type="number"
            min="0"
            step="0.01"
            :placeholder="form.discountType === 'percentage' ? '例如 0.85' : '例如 20'"
          />
        </label>

        <label class="coupon-panel__field">
          <span>门槛金额</span>
          <input v-model="form.thresholdAmount" type="number" min="0" step="0.01" />
        </label>

        <label class="coupon-panel__field">
          <span>状态</span>
          <select v-model="form.status">
            <option value="enabled">enabled</option>
            <option value="disabled">disabled</option>
          </select>
        </label>

        <div class="coupon-panel__submit-row">
          <button type="submit" class="coupon-panel__primary" :disabled="isSubmitting">
            {{ isSubmitting ? '创建中...' : '创建模板' }}
          </button>
        </div>
      </form>
    </div>
  </PanelCard>
</template>

<style scoped>
.coupon-panel {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 1rem;
}

.coupon-panel__list,
.coupon-panel__form {
  display: grid;
  gap: 0.85rem;
}

.coupon-panel__item {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.72);
}

.coupon-panel__item-head,
.coupon-panel__submit-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  justify-content: space-between;
}

.coupon-panel__item-store,
.coupon-panel__item-title,
.coupon-panel__empty,
.coupon-panel__error {
  margin: 0;
}

.coupon-panel__item-store,
.coupon-panel__empty {
  color: var(--color-ink-soft);
}

.coupon-panel__item-store {
  font-size: 0.8rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.coupon-panel__item-title {
  margin-top: 0.3rem;
  color: var(--color-ink-strong);
  font-size: 1.1rem;
}

.coupon-panel__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
  margin: 0;
}

.coupon-panel__fact {
  display: grid;
  gap: 0.35rem;
  padding: 0.8rem 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(247, 241, 236, 0.92);
}

.coupon-panel__fact dt {
  color: var(--color-ink-soft);
  font-size: 0.8rem;
}

.coupon-panel__fact dd {
  margin: 0;
  color: var(--color-ink-strong);
}

.coupon-panel__empty {
  line-height: 1.7;
}

.coupon-panel__error {
  padding: 0.85rem 0.95rem;
  border-radius: var(--radius-lg);
  background: rgba(183, 117, 47, 0.1);
  color: var(--color-warning);
}

.coupon-panel__field {
  display: grid;
  gap: 0.55rem;
}

.coupon-panel__field span {
  color: var(--color-ink-soft);
  font-size: 0.83rem;
}

.coupon-panel__field input,
.coupon-panel__field select {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.14);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.78);
  padding: 0.9rem 1rem;
  color: var(--color-ink-strong);
  font: inherit;
}

.coupon-panel__primary {
  min-height: 2.9rem;
  padding: 0 1rem;
  border: 0;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, #5b241d, #a1543d);
  color: #fff8f0;
}

.coupon-panel__primary:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

@media (max-width: 1120px) {
  .coupon-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .coupon-panel__facts {
    grid-template-columns: 1fr;
  }
}
</style>
