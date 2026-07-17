<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  CreateSupplierPayload,
  Store,
  Supplier,
  UpdateSupplierPayload
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  mode: 'create' | 'edit';
  stores: Store[];
  activeStoreId: string;
  supplier: Supplier | null;
  supplierDetail: Supplier | null;
  isSubmitting: boolean;
  actionError?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<{
  openCreate: [];
  openEdit: [];
  submitCreate: [payload: CreateSupplierPayload];
  submitUpdate: [payload: UpdateSupplierPayload];
  setPrimary: [];
  setBackup: [];
}>();

const form = reactive({
  storeId: '',
  supplierPlatformType: '1688',
  supplierPlatformId: '',
  supplierName: '',
  sourceUrl: '',
  priceScore: '80',
  deliveryScore: '80',
  stabilityScore: '80',
  riskLevel: 'medium',
  dropshipSupportFlag: false,
  blacklistFlag: false
});

const isCreateMode = computed(() => props.mode === 'create');

const visibleSupplier = computed(() => props.supplierDetail ?? props.supplier);

const availableStoreId = computed(() => {
  if (props.activeStoreId !== 'all') {
    return props.activeStoreId;
  }

  return props.stores[0]?.storeId ?? '';
});

watch(
  () => [props.mode, visibleSupplier.value?.supplierId, availableStoreId.value],
  () => {
    const supplier = visibleSupplier.value;

    if (isCreateMode.value) {
      form.storeId = availableStoreId.value;
      form.supplierPlatformType = '1688';
      form.supplierPlatformId = '';
      form.supplierName = '';
      form.sourceUrl = '';
      form.priceScore = '80';
      form.deliveryScore = '80';
      form.stabilityScore = '80';
      form.riskLevel = 'medium';
      form.dropshipSupportFlag = false;
      form.blacklistFlag = false;
      return;
    }

    if (!supplier) {
      form.storeId = availableStoreId.value;
      form.supplierPlatformType = '1688';
      form.supplierPlatformId = '';
      form.supplierName = '';
      form.sourceUrl = '';
      form.priceScore = '80';
      form.deliveryScore = '80';
      form.stabilityScore = '80';
      form.riskLevel = 'medium';
      form.dropshipSupportFlag = false;
      form.blacklistFlag = false;
      return;
    }

    form.storeId = supplier.storeId;
    form.supplierPlatformType = supplier.supplierPlatformType;
    form.supplierPlatformId = supplier.supplierPlatformId;
    form.supplierName = supplier.supplierName;
    form.sourceUrl = supplier.sourceUrl ?? '';
    form.priceScore = `${supplier.priceScore ?? 80}`;
    form.deliveryScore = `${supplier.deliveryScore ?? 80}`;
    form.stabilityScore = `${supplier.stabilityScore ?? 80}`;
    form.riskLevel = supplier.riskLevel ?? 'medium';
    form.dropshipSupportFlag = supplier.dropshipSupportFlag;
    form.blacklistFlag = supplier.blacklistFlag;
  },
  { immediate: true }
);

function toOptionalNumber(value: string) {
  const normalized = value.trim();

  if (!normalized) {
    return null;
  }

  return Number(normalized);
}

function handleSubmit() {
  if (isCreateMode.value) {
    emit('submitCreate', {
      storeId: form.storeId,
      supplierPlatformType: form.supplierPlatformType,
      supplierPlatformId: form.supplierPlatformId,
      supplierName: form.supplierName,
      sourceUrl: form.sourceUrl,
      priceScore: toOptionalNumber(form.priceScore),
      deliveryScore: toOptionalNumber(form.deliveryScore),
      stabilityScore: toOptionalNumber(form.stabilityScore),
      riskLevel: form.riskLevel,
      dropshipSupportFlag: form.dropshipSupportFlag
    });
    return;
  }

  emit('submitUpdate', {
    supplierName: form.supplierName,
    sourceUrl: form.sourceUrl,
    priceScore: toOptionalNumber(form.priceScore),
    deliveryScore: toOptionalNumber(form.deliveryScore),
    stabilityScore: toOptionalNumber(form.stabilityScore),
    riskLevel: form.riskLevel,
    dropshipSupportFlag: form.dropshipSupportFlag,
    blacklistFlag: form.blacklistFlag
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Profile & Governance"
    title="供应商主档与治理动作"
    description="左侧选主档，右侧在同一个面板里完成创建、编辑和主备切换，避免切页打断采购节奏。"
  >
    <div class="supplier-profile">
      <div class="supplier-profile__toolbar">
        <div class="supplier-profile__toolbar-actions">
          <button
            type="button"
            :class="['supplier-profile__ghost', { 'supplier-profile__ghost--active': isCreateMode }]"
            @click="emit('openCreate')"
          >
            新增模式
          </button>
          <button
            type="button"
            :class="['supplier-profile__ghost', { 'supplier-profile__ghost--active': !isCreateMode }]"
            @click="emit('openEdit')"
          >
            编辑模式
          </button>
        </div>

        <div v-if="visibleSupplier && !isCreateMode" class="supplier-profile__toolbar-actions">
          <button class="supplier-profile__chip" type="button" @click="emit('setPrimary')">
            设为主供应商
          </button>
          <button class="supplier-profile__chip" type="button" @click="emit('setBackup')">
            设为备选
          </button>
        </div>
      </div>

      <div v-if="visibleSupplier && !isCreateMode" class="supplier-profile__summary">
        <div class="supplier-profile__summary-copy">
          <p class="supplier-profile__summary-eyebrow">当前主档</p>
          <h4 class="supplier-profile__summary-title">{{ visibleSupplier.supplierName }}</h4>
          <p class="supplier-profile__summary-meta">
            {{ visibleSupplier.supplierPlatformType }} · {{ visibleSupplier.supplierPlatformId }}
          </p>
          <p class="supplier-profile__summary-time">
            创建时间 {{ formatDateTime(visibleSupplier.createdAt) }}
          </p>
        </div>

        <div class="supplier-profile__summary-tags">
          <StatusPill :label="visibleSupplier.riskLevel || 'normal'" :tone="visibleSupplier.blacklistFlag ? 'warn' : 'neutral'" />
          <StatusPill v-if="visibleSupplier.primary" label="primary" tone="success" />
          <StatusPill v-if="visibleSupplier.backup" label="backup" tone="neutral" />
        </div>
      </div>

      <form class="supplier-profile__form" @submit.prevent="handleSubmit">
        <label class="supplier-profile__field">
          <span>所属店铺</span>
          <select v-model="form.storeId" :disabled="!isCreateMode || isSubmitting">
            <option v-for="store in stores" :key="store.storeId" :value="store.storeId">
              {{ store.shopName }}
            </option>
          </select>
        </label>

        <label class="supplier-profile__field">
          <span>平台类型</span>
          <input v-model="form.supplierPlatformType" :disabled="!isCreateMode || isSubmitting" />
        </label>

        <label class="supplier-profile__field">
          <span>平台供应商 ID</span>
          <input v-model="form.supplierPlatformId" :disabled="!isCreateMode || isSubmitting" />
        </label>

        <label class="supplier-profile__field supplier-profile__field--wide">
          <span>供应商名称</span>
          <input v-model="form.supplierName" :disabled="isSubmitting" />
        </label>

        <label class="supplier-profile__field supplier-profile__field--wide">
          <span>来源链接</span>
          <input v-model="form.sourceUrl" :disabled="isSubmitting" placeholder="https://..." />
        </label>

        <label class="supplier-profile__field">
          <span>价格分</span>
          <input v-model="form.priceScore" :disabled="isSubmitting" type="number" min="0" max="100" />
        </label>

        <label class="supplier-profile__field">
          <span>交付分</span>
          <input v-model="form.deliveryScore" :disabled="isSubmitting" type="number" min="0" max="100" />
        </label>

        <label class="supplier-profile__field">
          <span>稳定分</span>
          <input v-model="form.stabilityScore" :disabled="isSubmitting" type="number" min="0" max="100" />
        </label>

        <label class="supplier-profile__field">
          <span>风险等级</span>
          <select v-model="form.riskLevel" :disabled="isSubmitting">
            <option value="low">low</option>
            <option value="medium">medium</option>
            <option value="high">high</option>
          </select>
        </label>

        <label class="supplier-profile__switch">
          <input v-model="form.dropshipSupportFlag" :disabled="isSubmitting" type="checkbox" />
          <span>支持一件代发</span>
        </label>

        <label class="supplier-profile__switch" v-if="!isCreateMode">
          <input v-model="form.blacklistFlag" :disabled="isSubmitting" type="checkbox" />
          <span>标记为黑名单</span>
        </label>

        <p v-if="actionError" class="supplier-profile__error">{{ actionError }}</p>

        <button class="supplier-profile__submit" type="submit" :disabled="isSubmitting">
          {{ isCreateMode ? '创建供应商' : '保存主档' }}
        </button>
      </form>
    </div>
  </PanelCard>
</template>

<style scoped>
.supplier-profile,
.supplier-profile__summary,
.supplier-profile__summary-copy,
.supplier-profile__summary-tags {
  display: grid;
  gap: 0.9rem;
}

.supplier-profile__toolbar,
.supplier-profile__toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
  justify-content: space-between;
}

.supplier-profile__ghost,
.supplier-profile__chip,
.supplier-profile__submit {
  min-height: 2.7rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.supplier-profile__ghost,
.supplier-profile__chip {
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.66);
  color: var(--color-ink);
}

.supplier-profile__ghost--active {
  background: rgba(133, 98, 67, 0.14);
  color: var(--color-ink-strong);
}

.supplier-profile__summary {
  grid-template-columns: 1fr auto;
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.68);
}

.supplier-profile__summary-eyebrow,
.supplier-profile__summary-title,
.supplier-profile__summary-meta,
.supplier-profile__summary-time,
.supplier-profile__error {
  margin: 0;
}

.supplier-profile__summary-eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.supplier-profile__summary-title {
  color: var(--color-ink-strong);
  font-size: 1.2rem;
}

.supplier-profile__summary-meta,
.supplier-profile__summary-time {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.supplier-profile__form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

.supplier-profile__field,
.supplier-profile__switch {
  display: grid;
  gap: 0.45rem;
}

.supplier-profile__field--wide {
  grid-column: span 3;
}

.supplier-profile__field span,
.supplier-profile__switch span {
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.supplier-profile__field input,
.supplier-profile__field select {
  min-height: 2.8rem;
  padding: 0 0.9rem;
  border-radius: 1rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-ink-strong);
}

.supplier-profile__switch {
  grid-column: span 3;
  grid-template-columns: auto 1fr;
  align-items: center;
}

.supplier-profile__error {
  grid-column: span 3;
  color: var(--color-danger);
}

.supplier-profile__submit {
  grid-column: span 3;
  border: 0;
  background: var(--color-accent);
  color: #fff8f0;
}

@media (max-width: 1080px) {
  .supplier-profile__summary,
  .supplier-profile__form {
    grid-template-columns: 1fr;
  }

  .supplier-profile__field--wide,
  .supplier-profile__switch,
  .supplier-profile__error,
  .supplier-profile__submit {
    grid-column: span 1;
  }
}
</style>
