<script setup lang="ts">
import { computed, reactive, shallowRef, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ConnectStorePayload, Store } from '@/services/apiTypes';

interface Props {
  stores: Store[];
  selectedStoreId: string | null;
  defaultOrganizationId: string;
  currentOperatorId: string;
  isSubmitting: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  selectStore: [storeId: string];
  connectStore: [payload: ConnectStorePayload];
}>();

const localError = shallowRef<string | null>(null);

const connectForm = reactive({
  organizationId: '',
  ownerUserId: '',
  platformType: 'douyin',
  platformShopId: '',
  shopName: '',
  profitThreshold: '18.8',
  riskThreshold: '8.8',
  defaultShipConfigText: '{\n  "warehouseCode": "WH-001",\n  "shippingStrategy": "balanced"\n}'
});

const selectedStore = computed(
  () => props.stores.find((store) => store.storeId === props.selectedStoreId) ?? null
);

watch(
  () => [props.defaultOrganizationId, props.currentOperatorId],
  () => {
    connectForm.organizationId = props.defaultOrganizationId;
    connectForm.ownerUserId = props.currentOperatorId;
  },
  { immediate: true }
);

function statusTone(authStatus: string) {
  return authStatus.toLowerCase().includes('author') ? 'success' : 'warn';
}

function formatConfig(config: Record<string, unknown> | null) {
  return JSON.stringify(config ?? {}, null, 2);
}

function toNumber(value: string, label: string) {
  const parsed = Number(value);

  if (Number.isNaN(parsed) || parsed < 0) {
    throw new Error(`${label} 必须是大于等于 0 的数字`);
  }

  return parsed;
}

function parseConfig(text: string) {
  const normalized = text.trim();

  if (!normalized) {
    return {};
  }

  const parsed = JSON.parse(normalized);

  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('默认发货配置必须是 JSON 对象');
  }

  return parsed as Record<string, unknown>;
}

function submitConnection() {
  localError.value = null;

  try {
    emit('connectStore', {
      organizationId: connectForm.organizationId.trim(),
      ownerUserId: connectForm.ownerUserId.trim(),
      platformType: connectForm.platformType.trim(),
      platformShopId: connectForm.platformShopId.trim(),
      shopName: connectForm.shopName.trim(),
      profitThreshold: toNumber(connectForm.profitThreshold, '利润阈值'),
      riskThreshold: toNumber(connectForm.riskThreshold, '风险阈值'),
      defaultShipConfig: parseConfig(connectForm.defaultShipConfigText)
    });
  } catch (error) {
    localError.value =
      error instanceof Error ? error.message : '店铺接入配置解析失败，请检查输入内容';
  }
}
</script>

<template>
  <PanelCard
    eyebrow="Store Access"
    title="平台选择与店铺授权状态"
    description="先明确当前租户准备接入哪一家平台，再把授权状态、利润阈值和默认发货策略锁到真实店铺上。"
  >
    <div class="onboarding-store">
      <div class="onboarding-store__list">
        <button
          v-for="store in stores"
          :key="store.storeId"
          type="button"
          :class="[
            'onboarding-store__item',
            { 'onboarding-store__item--active': store.storeId === selectedStoreId }
          ]"
          @click="emit('selectStore', store.storeId)"
        >
          <div class="onboarding-store__item-head">
            <div>
              <p class="onboarding-store__item-title">{{ store.shopName }}</p>
              <p class="onboarding-store__item-meta">
                {{ store.platformType }} · {{ store.platformShopId }}
              </p>
            </div>
            <StatusPill
              :label="store.authStatus"
              :tone="statusTone(store.authStatus)"
            />
          </div>
        </button>

        <p v-if="!stores.length" class="onboarding-store__empty">
          还没有接入任何店铺，先在右侧完成第一家平台店铺授权。
        </p>
      </div>

      <div class="onboarding-store__detail">
        <div v-if="selectedStore" class="onboarding-store__snapshot">
          <div class="onboarding-store__snapshot-head">
            <strong>{{ selectedStore.shopName }}</strong>
            <StatusPill
              :label="selectedStore.authStatus"
              :tone="statusTone(selectedStore.authStatus)"
            />
          </div>
          <dl class="onboarding-store__facts">
            <div>
              <dt>组织</dt>
              <dd>{{ selectedStore.organizationId }}</dd>
            </div>
            <div>
              <dt>平台</dt>
              <dd>{{ selectedStore.platformType }}</dd>
            </div>
            <div>
              <dt>利润阈值</dt>
              <dd>{{ selectedStore.profitThreshold }}</dd>
            </div>
            <div>
              <dt>风险阈值</dt>
              <dd>{{ selectedStore.riskThreshold }}</dd>
            </div>
          </dl>
          <pre class="onboarding-store__config">{{ formatConfig(selectedStore.defaultShipConfig) }}</pre>
        </div>

        <p v-if="localError" class="onboarding-store__error">{{ localError }}</p>

        <form class="onboarding-store__form" @submit.prevent="submitConnection">
          <label class="onboarding-store__field">
            <span>组织 ID</span>
            <input v-model="connectForm.organizationId" type="text" required />
          </label>
          <label class="onboarding-store__field">
            <span>店主用户 ID</span>
            <input v-model="connectForm.ownerUserId" type="text" required />
          </label>
          <label class="onboarding-store__field">
            <span>平台类型</span>
            <select v-model="connectForm.platformType">
              <option value="douyin">抖音</option>
              <option value="xiaohongshu">小红书</option>
              <option value="kuaishou">快手</option>
              <option value="taobao">淘宝</option>
            </select>
          </label>
          <label class="onboarding-store__field">
            <span>平台店铺 ID</span>
            <input v-model="connectForm.platformShopId" type="text" required />
          </label>
          <label class="onboarding-store__field onboarding-store__field--full">
            <span>店铺名称</span>
            <input v-model="connectForm.shopName" type="text" required />
          </label>
          <label class="onboarding-store__field">
            <span>利润阈值</span>
            <input v-model="connectForm.profitThreshold" type="number" min="0" step="0.01" required />
          </label>
          <label class="onboarding-store__field">
            <span>风险阈值</span>
            <input v-model="connectForm.riskThreshold" type="number" min="0" step="0.01" required />
          </label>
          <label class="onboarding-store__field onboarding-store__field--full">
            <span>默认发货配置 JSON</span>
            <textarea v-model="connectForm.defaultShipConfigText" rows="6" />
          </label>
          <button class="onboarding-store__primary" type="submit" :disabled="isSubmitting">
            {{ isSubmitting ? '接入中...' : '接入店铺并写入默认配置' }}
          </button>
        </form>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.onboarding-store {
  display: grid;
  grid-template-columns: 0.94fr 1.06fr;
  gap: 1rem;
}

.onboarding-store__list,
.onboarding-store__detail,
.onboarding-store__form,
.onboarding-store__field {
  display: grid;
  gap: 0.8rem;
}

.onboarding-store__item,
.onboarding-store__snapshot {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.72);
}

.onboarding-store__item {
  padding: 0.95rem 1rem;
  text-align: left;
}

.onboarding-store__item--active {
  border-color: rgba(133, 98, 67, 0.42);
  box-shadow: 0 14px 28px rgba(33, 24, 16, 0.08);
}

.onboarding-store__item-head,
.onboarding-store__snapshot-head {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  justify-content: space-between;
}

.onboarding-store__item-title,
.onboarding-store__item-meta,
.onboarding-store__empty,
.onboarding-store__config,
.onboarding-store__error {
  margin: 0;
}

.onboarding-store__item-title {
  color: var(--color-ink-strong);
  font-weight: 700;
}

.onboarding-store__item-meta,
.onboarding-store__empty,
.onboarding-store__field span,
.onboarding-store__facts dt {
  color: var(--color-ink-soft);
  font-size: 0.84rem;
}

.onboarding-store__snapshot {
  padding: 1rem;
}

.onboarding-store__error {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  background: rgba(183, 117, 47, 0.12);
  color: var(--color-warning);
}

.onboarding-store__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 1rem 0;
}

.onboarding-store__facts dt,
.onboarding-store__facts dd {
  margin: 0;
}

.onboarding-store__facts dd {
  color: var(--color-ink-strong);
  font-weight: 600;
}

.onboarding-store__config {
  overflow: auto;
  padding: 0.85rem;
  border-radius: var(--radius-lg);
  background: rgba(45, 36, 28, 0.92);
  color: #fff8f0;
  font-family: 'Cascadia Code', 'SFMono-Regular', Consolas, monospace;
  font-size: 0.79rem;
  line-height: 1.6;
}

.onboarding-store__field input,
.onboarding-store__field select,
.onboarding-store__field textarea {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.14);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.82);
  padding: 0.9rem 1rem;
  color: var(--color-ink-strong);
  font: inherit;
}

.onboarding-store__field--full {
  grid-column: 1 / -1;
}

.onboarding-store__form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.onboarding-store__primary {
  min-height: 3rem;
  grid-column: 1 / -1;
  border: 0;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, #2d241c, #856243);
  color: #fff8f0;
  font-weight: 700;
}

.onboarding-store__primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 1180px) {
  .onboarding-store,
  .onboarding-store__form,
  .onboarding-store__facts {
    grid-template-columns: 1fr;
  }
}
</style>
