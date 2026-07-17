<script setup lang="ts">
import { computed, reactive, shallowRef, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import type {
  CampaignActivity,
  CouponTemplate,
  CreateCampaignPayload,
  Store,
  UpdateCampaignPayload
} from '@/services/apiTypes';

interface Props {
  mode: 'create' | 'edit';
  stores: Store[];
  couponTemplates: CouponTemplate[];
  activeStoreId: string;
  selectedCampaign: CampaignActivity | null;
  isSubmitting: boolean;
  errorMessage?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  errorMessage: null
});

const emit = defineEmits<{
  openCreate: [];
  openEdit: [];
  submitCreate: [payload: CreateCampaignPayload];
  submitUpdate: [payload: UpdateCampaignPayload];
}>();

const localError = shallowRef<string | null>(null);

const form = reactive({
  storeId: '',
  activityType: 'full_reduction',
  activityName: '',
  startAt: '',
  endAt: '',
  productIdsText: '',
  couponTemplateId: '',
  thresholdAmount: '99',
  discountAmount: '20',
  discountRate: '0.9',
  note: ''
});

const filteredTemplates = computed(() =>
  props.couponTemplates.filter((template) => template.storeId === form.storeId)
);

function pad(value: number) {
  return String(value).padStart(2, '0');
}

function createDefaultDateValue(offsetDays = 0) {
  const date = new Date();
  date.setMinutes(0, 0, 0);
  date.setHours(date.getHours() + 1);
  date.setDate(date.getDate() + offsetDays);

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}`;
}

function toLocalDateTimeValue(value: string | null | undefined) {
  if (!value) {
    return '';
  }

  const date = new Date(value);

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}`;
}

function toOffsetDateTime(value: string) {
  const date = new Date(value);
  const offsetMinutes = -date.getTimezoneOffset();
  const sign = offsetMinutes >= 0 ? '+' : '-';
  const absoluteMinutes = Math.abs(offsetMinutes);
  const offsetHours = pad(Math.floor(absoluteMinutes / 60));
  const offsetRemainder = pad(absoluteMinutes % 60);

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:00${sign}${offsetHours}:${offsetRemainder}`;
}

function resolveCreateStoreId() {
  if (props.activeStoreId !== 'all') {
    return props.activeStoreId;
  }

  return props.selectedCampaign?.storeId ?? props.stores[0]?.storeId ?? '';
}

function resetCreateForm() {
  form.storeId = resolveCreateStoreId();
  form.activityType = 'full_reduction';
  form.activityName = '';
  form.startAt = createDefaultDateValue();
  form.endAt = createDefaultDateValue(2);
  form.productIdsText = '';
  form.couponTemplateId = '';
  form.thresholdAmount = '99';
  form.discountAmount = '20';
  form.discountRate = '0.9';
  form.note = '';
}

function applyCampaign(campaign: CampaignActivity) {
  form.storeId = campaign.storeId;
  form.activityType = campaign.activityType;
  form.activityName = campaign.activityName;
  form.startAt = toLocalDateTimeValue(campaign.startAt);
  form.endAt = toLocalDateTimeValue(campaign.endAt);
  form.productIdsText = campaign.productIds.join('\n');
  form.couponTemplateId = campaign.couponTemplateId ?? '';
  form.thresholdAmount = String(campaign.rule.thresholdAmount ?? '99');
  form.discountAmount = String(campaign.rule.discountAmount ?? '20');
  form.discountRate = String(campaign.rule.discountRate ?? '0.9');
  form.note = String(campaign.rule.note ?? '');
}

watch(
  () => [props.mode, props.selectedCampaign?.campaignId, props.activeStoreId, props.stores.length],
  () => {
    localError.value = null;

    if (props.mode === 'create') {
      resetCreateForm();
      return;
    }

    if (props.selectedCampaign) {
      applyCampaign(props.selectedCampaign);
    } else {
      resetCreateForm();
    }
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

function parseProductIds() {
  return form.productIdsText
    .split(/[\n,，\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .filter((item, index, source) => source.indexOf(item) === index);
}

function buildRule() {
  if (form.activityType === 'full_reduction') {
    const thresholdAmount = toPositiveNumber(form.thresholdAmount, '满减门槛');
    const discountAmount = toPositiveNumber(form.discountAmount, '优惠金额');

    if (discountAmount >= thresholdAmount) {
      throw new Error('优惠金额必须小于满减门槛');
    }

    return {
      thresholdAmount,
      discountAmount
    };
  }

  if (form.activityType === 'discount') {
    const discountRate = Number(form.discountRate);

    if (Number.isNaN(discountRate) || discountRate <= 0 || discountRate > 1) {
      throw new Error('折扣比例必须在 0 到 1 之间');
    }

    return {
      discountRate
    };
  }

  if (!form.note.trim()) {
    throw new Error('当前活动类型至少需要一条规则说明');
  }

  return {
    note: form.note.trim()
  };
}

function buildPayload(): CreateCampaignPayload {
  if (!form.storeId.trim()) {
    throw new Error('请选择店铺');
  }

  if (!form.activityName.trim()) {
    throw new Error('请输入活动名称');
  }

  if (!form.startAt || !form.endAt) {
    throw new Error('请填写活动开始和结束时间');
  }

  return {
    storeId: form.storeId.trim(),
    activityType: form.activityType.trim(),
    activityName: form.activityName.trim(),
    startAt: toOffsetDateTime(form.startAt),
    endAt: toOffsetDateTime(form.endAt),
    productIds: parseProductIds(),
    rule: buildRule(),
    couponTemplateId: form.couponTemplateId.trim() || undefined
  };
}

function submitCreate() {
  try {
    localError.value = null;
    emit('submitCreate', buildPayload());
  } catch (error) {
    localError.value = error instanceof Error ? error.message : '活动表单校验失败';
  }
}

function submitUpdate() {
  try {
    localError.value = null;
    emit('submitUpdate', buildPayload());
  } catch (error) {
    localError.value = error instanceof Error ? error.message : '活动表单校验失败';
  }
}
</script>

<template>
  <PanelCard
    eyebrow="Composer"
    title="活动编排台"
    description="活动创建和更新都在这里完成。首轮直接围绕后端已支持的字段，不伪装复杂拖拽编排器。"
  >
    <div class="campaign-composer">
      <div class="campaign-composer__toolbar">
        <button
          type="button"
          :class="[
            'campaign-composer__switch',
            { 'campaign-composer__switch--active': mode === 'create' }
          ]"
          @click="$emit('openCreate')"
        >
          新建模式
        </button>
        <button
          type="button"
          :class="[
            'campaign-composer__switch',
            { 'campaign-composer__switch--active': mode === 'edit' }
          ]"
          :disabled="!selectedCampaign"
          @click="$emit('openEdit')"
        >
          编辑模式
        </button>
      </div>

      <p class="campaign-composer__hint">
        活动类型严格跟随后端：`full_reduction`、`discount`、`gift`、`bundle`、`new_customer`。
      </p>

      <p v-if="localError || errorMessage" class="campaign-composer__error">
        {{ localError || errorMessage }}
      </p>

      <form
        class="campaign-composer__form"
        @submit.prevent="mode === 'create' ? submitCreate() : submitUpdate()"
      >
        <label class="campaign-composer__field">
          <span>店铺</span>
          <select v-model="form.storeId" :disabled="mode === 'edit'">
            <option v-for="store in stores" :key="store.storeId" :value="store.storeId">
              {{ store.shopName }}
            </option>
          </select>
        </label>

        <label class="campaign-composer__field">
          <span>活动类型</span>
          <select v-model="form.activityType">
            <option value="full_reduction">满减</option>
            <option value="discount">折扣</option>
            <option value="gift">赠品</option>
            <option value="bundle">组合</option>
            <option value="new_customer">新客</option>
          </select>
        </label>

        <label class="campaign-composer__field campaign-composer__field--full">
          <span>活动名称</span>
          <input v-model="form.activityName" type="text" required />
        </label>

        <label class="campaign-composer__field">
          <span>开始时间</span>
          <input v-model="form.startAt" type="datetime-local" required />
        </label>

        <label class="campaign-composer__field">
          <span>结束时间</span>
          <input v-model="form.endAt" type="datetime-local" required />
        </label>

        <label class="campaign-composer__field campaign-composer__field--full">
          <span>商品 ID</span>
          <textarea
            v-model="form.productIdsText"
            rows="4"
            placeholder="每行一个，或使用逗号分隔"
          />
        </label>

        <label class="campaign-composer__field campaign-composer__field--full">
          <span>绑定优惠券模板</span>
          <select v-model="form.couponTemplateId">
            <option value="">不绑定模板</option>
            <option
              v-for="template in filteredTemplates"
              :key="template.couponTemplateId"
              :value="template.couponTemplateId"
            >
              {{ template.templateName }} / {{ template.status }}
            </option>
          </select>
        </label>

        <template v-if="form.activityType === 'full_reduction'">
          <label class="campaign-composer__field">
            <span>满减门槛</span>
            <input v-model="form.thresholdAmount" type="number" min="0" step="0.01" />
          </label>
          <label class="campaign-composer__field">
            <span>优惠金额</span>
            <input v-model="form.discountAmount" type="number" min="0" step="0.01" />
          </label>
        </template>

        <label
          v-else-if="form.activityType === 'discount'"
          class="campaign-composer__field campaign-composer__field--full"
        >
          <span>折扣比例</span>
          <input
            v-model="form.discountRate"
            type="number"
            min="0.01"
            max="1"
            step="0.01"
            placeholder="例如 0.85"
          />
        </label>

        <label v-else class="campaign-composer__field campaign-composer__field--full">
          <span>规则说明</span>
          <textarea
            v-model="form.note"
            rows="4"
            placeholder="例如：新客首单赠品、组合加价购说明"
          />
        </label>

        <div class="campaign-composer__submit-row">
          <button type="submit" class="campaign-composer__primary" :disabled="isSubmitting">
            {{
              isSubmitting
                ? '提交中...'
                : mode === 'create'
                  ? '创建活动'
                  : '保存活动'
            }}
          </button>
        </div>
      </form>
    </div>
  </PanelCard>
</template>

<style scoped>
.campaign-composer {
  display: grid;
  gap: 0.9rem;
}

.campaign-composer__toolbar,
.campaign-composer__submit-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.campaign-composer__hint,
.campaign-composer__error {
  margin: 0;
}

.campaign-composer__hint {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.campaign-composer__error {
  padding: 0.85rem 0.95rem;
  border-radius: var(--radius-lg);
  background: rgba(183, 117, 47, 0.1);
  color: var(--color-warning);
}

.campaign-composer__switch,
.campaign-composer__primary {
  min-height: 2.9rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.campaign-composer__switch {
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.74);
  color: var(--color-ink);
}

.campaign-composer__switch--active {
  border-color: rgba(161, 84, 61, 0.32);
  background: rgba(249, 236, 232, 0.94);
  color: #7a4032;
}

.campaign-composer__primary {
  border: 0;
  background: linear-gradient(135deg, #5b241d, #a1543d);
  color: #fff8f0;
}

.campaign-composer__switch:disabled,
.campaign-composer__primary:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.campaign-composer__form,
.campaign-composer__field {
  display: grid;
  gap: 0.8rem;
}

.campaign-composer__form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.campaign-composer__field span {
  color: var(--color-ink-soft);
  font-size: 0.83rem;
}

.campaign-composer__field input,
.campaign-composer__field textarea,
.campaign-composer__field select {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.14);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.78);
  padding: 0.9rem 1rem;
  color: var(--color-ink-strong);
  font: inherit;
}

.campaign-composer__field--full,
.campaign-composer__submit-row {
  grid-column: 1 / -1;
}

@media (max-width: 720px) {
  .campaign-composer__form {
    grid-template-columns: 1fr;
  }
}
</style>
