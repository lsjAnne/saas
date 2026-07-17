<script setup lang="ts">
import { reactive, shallowRef, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import type {
  ConnectStorePayload,
  CreateChannelAccountPayload,
  Store,
  UpdateStoreSettingsPayload
} from '@/services/apiTypes';

interface Props {
  mode: 'connect-store' | 'create-channel-account' | 'edit-store-settings' | null;
  selectedStore: Store | null;
  defaultOrganizationId: string;
  defaultOperatorId: string;
  isSubmitting: boolean;
  errorMessage?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  errorMessage: null
});

const emit = defineEmits<{
  close: [];
  openStore: [];
  openChannel: [];
  openSettings: [];
  connectStore: [payload: ConnectStorePayload];
  createChannelAccount: [payload: CreateChannelAccountPayload];
  updateStoreSettings: [payload: UpdateStoreSettingsPayload];
}>();

const localError = shallowRef<string | null>(null);

const storeForm = reactive({
  organizationId: '',
  ownerUserId: '',
  platformType: 'douyin',
  platformShopId: '',
  shopName: '',
  profitThreshold: '0',
  riskThreshold: '0',
  defaultShipConfigText: '{\n  "warehouseCode": "WH-001",\n  "shippingStrategy": "balanced"\n}'
});

const channelForm = reactive({
  organizationId: '',
  channelType: 'douyin',
  accountName: '',
  extraConfigText: '{\n  "region": "cn-east-1"\n}'
});

const settingsForm = reactive({
  profitThreshold: '0',
  riskThreshold: '0',
  defaultShipConfigText: '{}'
});

function stringifyMap(value: Record<string, unknown> | null | undefined) {
  return JSON.stringify(value ?? {}, null, 2);
}

function resetStoreForm() {
  storeForm.organizationId = props.defaultOrganizationId;
  storeForm.ownerUserId = props.defaultOperatorId;
  storeForm.platformType = 'douyin';
  storeForm.platformShopId = '';
  storeForm.shopName = '';
  storeForm.profitThreshold = '0';
  storeForm.riskThreshold = '0';
  storeForm.defaultShipConfigText =
    '{\n  "warehouseCode": "WH-001",\n  "shippingStrategy": "balanced"\n}';
}

function resetChannelForm() {
  channelForm.organizationId =
    props.selectedStore?.organizationId || props.defaultOrganizationId;
  channelForm.channelType = props.selectedStore?.platformType || 'douyin';
  channelForm.accountName = '';
  channelForm.extraConfigText = '{\n  "region": "cn-east-1"\n}';
}

function resetSettingsForm() {
  settingsForm.profitThreshold = String(props.selectedStore?.profitThreshold ?? 0);
  settingsForm.riskThreshold = String(props.selectedStore?.riskThreshold ?? 0);
  settingsForm.defaultShipConfigText = stringifyMap(
    props.selectedStore?.defaultShipConfig
  );
}

watch(
  () => [
    props.mode,
    props.selectedStore?.storeId,
    props.defaultOrganizationId,
    props.defaultOperatorId
  ],
  () => {
    localError.value = null;

    if (props.mode === 'connect-store') {
      resetStoreForm();
    }

    if (props.mode === 'create-channel-account') {
      resetChannelForm();
    }

    if (props.mode === 'edit-store-settings') {
      resetSettingsForm();
    }
  },
  { immediate: true }
);

function parseMap(text: string) {
  const normalized = text.trim();
  if (!normalized) {
    return {};
  }

  const parsed = JSON.parse(normalized);
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('配置 JSON 必须是对象，例如 {"warehouseCode":"WH-001"}');
  }

  return parsed as Record<string, unknown>;
}

function toNumber(value: string, fieldName: string) {
  const parsed = Number(value);

  if (Number.isNaN(parsed) || parsed < 0) {
    throw new Error(`${fieldName} 必须是大于等于 0 的数字`);
  }

  return parsed;
}

function submitStore() {
  localError.value = null;

  try {
    emit('connectStore', {
      organizationId: storeForm.organizationId.trim(),
      ownerUserId: storeForm.ownerUserId.trim(),
      platformType: storeForm.platformType.trim(),
      platformShopId: storeForm.platformShopId.trim(),
      shopName: storeForm.shopName.trim(),
      profitThreshold: toNumber(storeForm.profitThreshold, '利润阈值'),
      riskThreshold: toNumber(storeForm.riskThreshold, '风险阈值'),
      defaultShipConfig: parseMap(storeForm.defaultShipConfigText)
    });
  } catch (error) {
    localError.value =
      error instanceof Error ? error.message : '店铺接入表单校验失败';
  }
}

function submitChannel() {
  localError.value = null;

  try {
    emit('createChannelAccount', {
      organizationId: channelForm.organizationId.trim(),
      channelType: channelForm.channelType.trim(),
      accountName: channelForm.accountName.trim(),
      extraConfig: parseMap(channelForm.extraConfigText)
    });
  } catch (error) {
    localError.value =
      error instanceof Error ? error.message : '渠道账号表单校验失败';
  }
}

function submitSettings() {
  localError.value = null;

  try {
    emit('updateStoreSettings', {
      profitThreshold: toNumber(settingsForm.profitThreshold, '利润阈值'),
      riskThreshold: toNumber(settingsForm.riskThreshold, '风险阈值'),
      defaultShipConfig: parseMap(settingsForm.defaultShipConfigText)
    });
  } catch (error) {
    localError.value =
      error instanceof Error ? error.message : '店铺设置表单校验失败';
  }
}
</script>

<template>
  <PanelCard
    eyebrow="Action Station"
    title="接入与配置操作台"
    description="这里承接三类动作：新增店铺、新增渠道账号、更新当前店铺阈值。为了避免误配，所有 JSON 配置都要求显式可见。"
  >
    <div class="composer">
      <div v-if="!mode" class="composer__idle">
        <p class="composer__idle-copy">
          先从三类动作里选一个开始。当前如果已经选中店铺，也可以直接进入阈值编辑。
        </p>
        <div class="composer__idle-actions">
          <button type="button" class="composer__primary" @click="emit('openStore')">
            接入新店铺
          </button>
          <button type="button" class="composer__ghost" @click="emit('openChannel')">
            新增渠道账号
          </button>
          <button
            type="button"
            class="composer__ghost"
            :disabled="!selectedStore"
            @click="emit('openSettings')"
          >
            编辑当前店铺阈值
          </button>
        </div>
      </div>

      <template v-else>
        <div class="composer__mode-bar">
          <strong class="composer__mode-title">
            {{
              mode === 'connect-store'
                ? '新增店铺接入'
                : mode === 'create-channel-account'
                  ? '新增渠道账号'
                  : `编辑 ${selectedStore?.shopName ?? '当前店铺'} 阈值`
            }}
          </strong>
          <button type="button" class="composer__mode-close" @click="$emit('close')">
            收起
          </button>
        </div>

        <p v-if="errorMessage || localError" class="composer__error">
          {{ localError || errorMessage }}
        </p>

        <form
          v-if="mode === 'connect-store'"
          class="composer__form"
          @submit.prevent="submitStore"
        >
          <label class="composer__field">
            <span>组织 ID</span>
            <input v-model="storeForm.organizationId" type="text" required />
          </label>
          <label class="composer__field">
            <span>店主用户 ID</span>
            <input v-model="storeForm.ownerUserId" type="text" required />
          </label>
          <label class="composer__field">
            <span>平台类型</span>
            <input v-model="storeForm.platformType" type="text" required />
          </label>
          <label class="composer__field">
            <span>平台店铺 ID</span>
            <input v-model="storeForm.platformShopId" type="text" required />
          </label>
          <label class="composer__field composer__field--full">
            <span>店铺名称</span>
            <input v-model="storeForm.shopName" type="text" required />
          </label>
          <label class="composer__field">
            <span>利润阈值</span>
            <input v-model="storeForm.profitThreshold" type="number" min="0" step="0.01" required />
          </label>
          <label class="composer__field">
            <span>风险阈值</span>
            <input v-model="storeForm.riskThreshold" type="number" min="0" step="0.01" required />
          </label>
          <label class="composer__field composer__field--full">
            <span>默认发货配置 JSON</span>
            <textarea v-model="storeForm.defaultShipConfigText" rows="7" />
          </label>

          <div class="composer__submit-row">
            <button type="submit" class="composer__primary" :disabled="isSubmitting">
              {{ isSubmitting ? '提交中...' : '确认接入店铺' }}
            </button>
          </div>
        </form>

        <form
          v-else-if="mode === 'create-channel-account'"
          class="composer__form"
          @submit.prevent="submitChannel"
        >
          <label class="composer__field">
            <span>组织 ID</span>
            <input v-model="channelForm.organizationId" type="text" required />
          </label>
          <label class="composer__field">
            <span>渠道类型</span>
            <input v-model="channelForm.channelType" type="text" required />
          </label>
          <label class="composer__field composer__field--full">
            <span>账号名称</span>
            <input v-model="channelForm.accountName" type="text" required />
          </label>
          <label class="composer__field composer__field--full">
            <span>额外配置 JSON</span>
            <textarea v-model="channelForm.extraConfigText" rows="6" />
          </label>

          <div class="composer__submit-row">
            <button type="submit" class="composer__primary" :disabled="isSubmitting">
              {{ isSubmitting ? '提交中...' : '确认新增账号' }}
            </button>
          </div>
        </form>

        <form v-else class="composer__form" @submit.prevent="submitSettings">
          <label class="composer__field">
            <span>利润阈值</span>
            <input v-model="settingsForm.profitThreshold" type="number" min="0" step="0.01" required />
          </label>
          <label class="composer__field">
            <span>风险阈值</span>
            <input v-model="settingsForm.riskThreshold" type="number" min="0" step="0.01" required />
          </label>
          <label class="composer__field composer__field--full">
            <span>默认发货配置 JSON</span>
            <textarea v-model="settingsForm.defaultShipConfigText" rows="7" />
          </label>

          <div class="composer__submit-row">
            <button type="submit" class="composer__primary" :disabled="isSubmitting">
              {{ isSubmitting ? '保存中...' : '保存店铺阈值' }}
            </button>
          </div>
        </form>
      </template>
    </div>
  </PanelCard>
</template>

<style scoped>
.composer {
  display: grid;
  gap: 0.9rem;
}

.composer__idle,
.composer__form,
.composer__field {
  display: grid;
  gap: 0.8rem;
}

.composer__idle-copy,
.composer__mode-title,
.composer__error {
  margin: 0;
}

.composer__idle-copy {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.composer__idle-actions,
.composer__mode-bar,
.composer__submit-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  justify-content: space-between;
}

.composer__mode-title {
  color: var(--color-ink-strong);
  font-size: 1.05rem;
}

.composer__mode-close,
.composer__primary,
.composer__ghost {
  min-height: 2.9rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.composer__mode-close,
.composer__ghost {
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.74);
  color: var(--color-ink);
}

.composer__primary {
  border: 0;
  background: linear-gradient(135deg, #2e251d, #856243);
  color: #fff8f0;
}

.composer__primary:disabled,
.composer__ghost:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.composer__error {
  padding: 0.85rem 0.95rem;
  border-radius: var(--radius-lg);
  background: rgba(183, 117, 47, 0.1);
  color: var(--color-warning);
}

.composer__form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.composer__field span {
  color: var(--color-ink-soft);
  font-size: 0.83rem;
}

.composer__field input,
.composer__field textarea {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.14);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.78);
  padding: 0.9rem 1rem;
  color: var(--color-ink-strong);
  font: inherit;
}

.composer__field--full,
.composer__submit-row {
  grid-column: 1 / -1;
}

@media (max-width: 720px) {
  .composer__form {
    grid-template-columns: 1fr;
  }
}
</style>
