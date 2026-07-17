<script setup lang="ts">
import { computed, reactive } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  IntegrationCredentialView,
  IssuedIntegrationCredential,
  PluginApp,
  WebhookSubscription
} from '@/services/apiTypes';
import type {
  CreateOpenPlatformAppPayload,
  CreateOpenPlatformWebhookPayload
} from '@/features/openPlatform/services/openPlatformConsoleService';
import { formatDate } from '@/utils/formatters';

interface Props {
  organizationId: string;
  apps: PluginApp[];
  credentialsByApp: Record<string, IntegrationCredentialView[]>;
  issuedCredentialByApp: Record<string, IssuedIntegrationCredential | undefined>;
  webhooks: WebhookSubscription[];
  isRunningAction: boolean;
  activeAppId: string | null;
  activeWebhookId: string | null;
  actionFeedback: string | null;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  createApp: [payload: CreateOpenPlatformAppPayload];
  toggleApp: [payload: { appId: string; enable: boolean }];
  refreshCredential: [appId: string];
  revokeCredential: [appId: string];
  createWebhook: [payload: CreateOpenPlatformWebhookPayload];
  toggleWebhook: [payload: { subscriptionId: string; enable: boolean }];
  rotateWebhookSecret: [subscriptionId: string];
}>();

const appTypeOptions = [
  { label: 'ERP', value: 'erp' },
  { label: 'Plugin', value: 'plugin' },
  { label: 'WMS', value: 'wms' },
  { label: 'TMS', value: 'tms' },
  { label: 'BI', value: 'bi' },
  { label: 'Messaging', value: 'messaging' }
];

const permissionOptions = [
  { label: 'WMS 联动读取', value: 'wms.linkage.read' },
  { label: 'ERP 主数据读取', value: 'erp.master_data.read' },
  { label: 'ERP 科目映射读取', value: 'erp.account_mapping.read' },
  { label: 'ERP 集成基线', value: 'erp.integration_baseline.read' },
  { label: 'ERP OFBiz 基线', value: 'erp.ofbiz_baseline.read' },
  { label: 'WMS OpenBoxes 基线', value: 'wms.openboxes_baseline.read' },
  { label: 'TMS 控制塔读取', value: 'tms.control_tower.read' },
  { label: '消息基线读取', value: 'messaging.rabbitmq_baseline.read' },
  { label: '回调桥读取', value: 'messaging.callback_bridge.read' },
  { label: 'BI 总览读取', value: 'bi.superset_overview.read' },
  { label: '可观测性只读', value: 'system.observability_readiness.read' },
  { label: '交付 Readiness 只读', value: 'delivery.readiness.read' }
];

const appForm = reactive({
  appName: '',
  appType: 'plugin',
  permissionScope: ['erp.master_data.read']
});

const webhookForm = reactive({
  eventCode: 'order.paid',
  callbackUrl: ''
});

const resolvedOrganizationId = computed(
  () => props.organizationId || props.apps[0]?.organizationId || props.webhooks[0]?.organizationId || ''
);

function toneFromStatus(status: string) {
  const normalized = status.trim().toLowerCase();
  return normalized === 'active' || normalized === 'enabled' ? 'success' : 'neutral';
}

function togglePermissionScope(scope: string) {
  const exists = appForm.permissionScope.includes(scope);
  appForm.permissionScope = exists
    ? appForm.permissionScope.filter((item) => item !== scope)
    : [...appForm.permissionScope, scope];
}

function handleCreateApp() {
  emit('createApp', {
    organizationId: resolvedOrganizationId.value,
    appName: appForm.appName.trim(),
    appType: appForm.appType,
    permissionScope: appForm.permissionScope
  });
}

function handleCreateWebhook() {
  emit('createWebhook', {
    organizationId: resolvedOrganizationId.value,
    eventCode: webhookForm.eventCode.trim(),
    callbackUrl: webhookForm.callbackUrl.trim()
  });
}

function currentCredential(appId: string) {
  return props.credentialsByApp[appId]?.[0] ?? null;
}

function latestIssuedCredential(appId: string) {
  return props.issuedCredentialByApp[appId] ?? null;
}
</script>

<template>
  <PanelCard
    eyebrow="Control Desk"
    title="统一管理应用、凭证、Scope 与 Webhook"
    description="把开放平台真正需要操作的事项留在同一个控制面板里，不再只停在 apps 和 webhooks 的静态展示。"
  >
    <div class="open-platform-assets">
      <p v-if="actionFeedback" class="open-platform-assets__feedback">{{ actionFeedback }}</p>

      <section class="open-platform-assets__block">
        <div class="open-platform-assets__section-head">
          <div>
            <p class="open-platform-assets__label">Create App</p>
            <h4 class="open-platform-assets__section-title">创建应用并声明 Scope</h4>
            <p class="open-platform-assets__section-copy">
              新应用创建后即可继续下发凭证，前端这里同步暴露和后端一致的读取类 Scope。
            </p>
          </div>
          <button
            type="button"
            class="open-platform-assets__primary"
            :disabled="
              isRunningAction ||
              !resolvedOrganizationId ||
              !appForm.appName.trim() ||
              !appForm.permissionScope.length
            "
            @click="handleCreateApp"
          >
            {{ isRunningAction ? '提交中..' : '创建应用' }}
          </button>
        </div>

        <div class="open-platform-assets__form-grid">
          <label class="open-platform-assets__field">
            <span>应用名称</span>
            <input v-model="appForm.appName" type="text" placeholder="例如：erp-connector" />
          </label>
          <label class="open-platform-assets__field">
            <span>应用类型</span>
            <select v-model="appForm.appType">
              <option v-for="option in appTypeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
        </div>

        <div class="open-platform-assets__scope-box">
          <p class="open-platform-assets__label">Permission Scope</p>
          <div class="open-platform-assets__chips">
            <button
              v-for="option in permissionOptions"
              :key="option.value"
              type="button"
              :class="[
                'open-platform-assets__chip',
                {
                  'open-platform-assets__chip--active': appForm.permissionScope.includes(option.value)
                }
              ]"
              @click="togglePermissionScope(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </section>

      <section class="open-platform-assets__block">
        <p class="open-platform-assets__label">Apps</p>
        <article v-for="app in apps" :key="app.appId" class="open-platform-assets__item">
          <div class="open-platform-assets__head">
            <div>
              <h4 class="open-platform-assets__title">{{ app.appName }}</h4>
              <p class="open-platform-assets__meta">{{ app.appType }} · {{ formatDate(app.createdAt) }}</p>
            </div>
            <div class="open-platform-assets__head-actions">
              <StatusPill :label="app.status" :tone="toneFromStatus(app.status)" />
              <button
                type="button"
                class="open-platform-assets__secondary"
                :disabled="isRunningAction && activeAppId === app.appId"
                @click="emit('toggleApp', { appId: app.appId, enable: app.status !== 'active' })"
              >
                {{
                  isRunningAction && activeAppId === app.appId
                    ? '处理中..'
                    : app.status === 'active'
                      ? '禁用应用'
                      : '启用应用'
                }}
              </button>
            </div>
          </div>

          <div class="open-platform-assets__facts">
            <p class="open-platform-assets__fact">
              <strong>AccessKey</strong>
              <span>{{ app.accessKey }}</span>
            </p>
            <p class="open-platform-assets__fact">
              <strong>Secret</strong>
              <span>{{ app.secretMasked }}</span>
            </p>
          </div>

          <div class="open-platform-assets__facts">
            <p class="open-platform-assets__fact">
              <strong>Credential Status</strong>
              <span>
                {{
                  currentCredential(app.appId)
                    ? currentCredential(app.appId)?.active
                      ? 'active'
                      : 'revoked'
                    : 'not issued'
                }}
              </span>
            </p>
            <p class="open-platform-assets__fact">
              <strong>Expires At</strong>
              <span>{{ currentCredential(app.appId)?.expiresAt || '--' }}</span>
            </p>
          </div>

          <div v-if="latestIssuedCredential(app.appId)" class="open-platform-assets__facts">
            <p class="open-platform-assets__fact">
              <strong>Latest Secret</strong>
              <span>{{ latestIssuedCredential(app.appId)?.secret }}</span>
            </p>
            <p class="open-platform-assets__fact">
              <strong>Masked</strong>
              <span>{{ latestIssuedCredential(app.appId)?.secretMasked }}</span>
            </p>
          </div>

          <div class="open-platform-assets__chips">
            <span
              v-for="scope in app.permissionScope"
              :key="scope"
              class="open-platform-assets__chip open-platform-assets__chip--readonly"
            >
              {{ scope }}
            </span>
          </div>

          <div class="open-platform-assets__actions">
            <button
              type="button"
              class="open-platform-assets__secondary"
              :disabled="isRunningAction && activeAppId === app.appId"
              @click="emit('refreshCredential', app.appId)"
            >
              {{ isRunningAction && activeAppId === app.appId ? '处理中..' : '刷新凭证' }}
            </button>
            <button
              type="button"
              class="open-platform-assets__secondary"
              :disabled="isRunningAction && activeAppId === app.appId"
              @click="emit('revokeCredential', app.appId)"
            >
              {{ isRunningAction && activeAppId === app.appId ? '处理中..' : '吊销凭证' }}
            </button>
          </div>
        </article>

        <p v-if="apps.length === 0" class="open-platform-assets__empty">
          当前还没有应用，先创建一个接入应用再继续配置凭证和调用范围。
        </p>
      </section>

      <section class="open-platform-assets__block">
        <div class="open-platform-assets__section-head">
          <div>
            <p class="open-platform-assets__label">Webhook</p>
            <h4 class="open-platform-assets__section-title">新增回调订阅</h4>
            <p class="open-platform-assets__section-copy">
              新订阅会立即生成 Secret Token，后续可直接轮换密钥并观察回调编排结果。
            </p>
          </div>
          <button
            type="button"
            class="open-platform-assets__primary"
            :disabled="
              isRunningAction ||
              !resolvedOrganizationId ||
              !webhookForm.eventCode.trim() ||
              !webhookForm.callbackUrl.trim()
            "
            @click="handleCreateWebhook"
          >
            {{ isRunningAction ? '提交中..' : '新增订阅' }}
          </button>
        </div>

        <div class="open-platform-assets__form-grid">
          <label class="open-platform-assets__field">
            <span>事件编码</span>
            <input v-model="webhookForm.eventCode" type="text" placeholder="例如：order.paid" />
          </label>
          <label class="open-platform-assets__field">
            <span>回调地址</span>
            <input
              v-model="webhookForm.callbackUrl"
              type="text"
              placeholder="https://example.com/webhooks/order-paid"
            />
          </label>
        </div>
      </section>

      <section class="open-platform-assets__block">
        <p class="open-platform-assets__label">Webhook Subscriptions</p>
        <article
          v-for="webhook in webhooks"
          :key="webhook.subscriptionId"
          class="open-platform-assets__item"
        >
          <div class="open-platform-assets__head">
            <div>
              <h4 class="open-platform-assets__title">{{ webhook.eventCode }}</h4>
              <p class="open-platform-assets__meta">{{ webhook.callbackUrl }}</p>
            </div>
            <div class="open-platform-assets__head-actions">
              <StatusPill :label="webhook.status" :tone="toneFromStatus(webhook.status)" />
              <button
                type="button"
                class="open-platform-assets__secondary"
                :disabled="isRunningAction && activeWebhookId === webhook.subscriptionId"
                @click="
                  emit('toggleWebhook', {
                    subscriptionId: webhook.subscriptionId,
                    enable: webhook.status !== 'enabled'
                  })
                "
              >
                {{
                  isRunningAction && activeWebhookId === webhook.subscriptionId
                    ? '处理中..'
                    : webhook.status === 'enabled'
                      ? '停用回调'
                      : '启用回调'
                }}
              </button>
            </div>
          </div>

          <div class="open-platform-assets__facts">
            <p class="open-platform-assets__fact">
              <strong>Secret Token</strong>
              <span>{{ webhook.secretToken }}</span>
            </p>
            <p class="open-platform-assets__fact">
              <strong>Created At</strong>
              <span>{{ formatDate(webhook.createdAt) }}</span>
            </p>
          </div>

          <div class="open-platform-assets__actions">
            <button
              type="button"
              class="open-platform-assets__secondary"
              :disabled="isRunningAction && activeWebhookId === webhook.subscriptionId"
              @click="emit('rotateWebhookSecret', webhook.subscriptionId)"
            >
              {{ isRunningAction && activeWebhookId === webhook.subscriptionId ? '处理中..' : '轮换 Secret' }}
            </button>
          </div>
        </article>

        <p v-if="webhooks.length === 0" class="open-platform-assets__empty">
          当前还没有回调订阅，先增加一条地址再观察编排和调用表现。
        </p>
      </section>
    </div>
  </PanelCard>
</template>

<style scoped>
.open-platform-assets,
.open-platform-assets__block {
  display: grid;
  gap: 0.9rem;
}

.open-platform-assets__block {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.open-platform-assets__section-head,
.open-platform-assets__head,
.open-platform-assets__head-actions,
.open-platform-assets__actions {
  display: flex;
  gap: 0.75rem;
}

.open-platform-assets__section-head,
.open-platform-assets__head {
  justify-content: space-between;
  align-items: flex-start;
}

.open-platform-assets__head-actions,
.open-platform-assets__actions {
  flex-wrap: wrap;
}

.open-platform-assets__feedback,
.open-platform-assets__label,
.open-platform-assets__section-title,
.open-platform-assets__section-copy,
.open-platform-assets__title,
.open-platform-assets__meta,
.open-platform-assets__empty,
.open-platform-assets__fact strong,
.open-platform-assets__fact span {
  margin: 0;
}

.open-platform-assets__label {
  color: var(--color-ink-faint);
  font-size: 0.74rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.open-platform-assets__section-title,
.open-platform-assets__title {
  color: var(--color-ink-strong);
}

.open-platform-assets__section-copy,
.open-platform-assets__meta,
.open-platform-assets__empty,
.open-platform-assets__feedback,
.open-platform-assets__fact span {
  color: var(--color-ink-soft);
}

.open-platform-assets__item {
  display: grid;
  gap: 0.85rem;
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 252, 248, 0.88);
}

.open-platform-assets__form-grid,
.open-platform-assets__facts {
  display: grid;
  gap: 0.8rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.open-platform-assets__field {
  display: grid;
  gap: 0.4rem;
  color: var(--color-ink-soft);
}

.open-platform-assets__field input,
.open-platform-assets__field select {
  width: 100%;
  padding: 0.7rem 0.85rem;
  border-radius: 0.9rem;
  border: 1px solid rgba(91, 72, 54, 0.16);
  background: rgba(255, 255, 255, 0.92);
  color: var(--color-ink-strong);
}

.open-platform-assets__scope-box,
.open-platform-assets__chips {
  display: grid;
  gap: 0.6rem;
}

.open-platform-assets__chips {
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
}

.open-platform-assets__chip,
.open-platform-assets__primary,
.open-platform-assets__secondary {
  border: none;
  cursor: pointer;
  transition: transform 120ms ease, opacity 120ms ease;
}

.open-platform-assets__chip {
  padding: 0.6rem 0.75rem;
  border-radius: 999px;
  background: rgba(193, 162, 126, 0.14);
  color: var(--color-ink-strong);
}

.open-platform-assets__chip--active {
  background: rgba(162, 95, 47, 0.16);
  box-shadow: inset 0 0 0 1px rgba(162, 95, 47, 0.24);
}

.open-platform-assets__chip--readonly {
  cursor: default;
}

.open-platform-assets__primary,
.open-platform-assets__secondary {
  padding: 0.65rem 0.95rem;
  border-radius: 999px;
}

.open-platform-assets__primary {
  background: var(--color-accent);
  color: #fff;
}

.open-platform-assets__secondary {
  background: rgba(91, 72, 54, 0.08);
  color: var(--color-ink-strong);
}

.open-platform-assets__primary:disabled,
.open-platform-assets__secondary:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

@media (max-width: 960px) {
  .open-platform-assets__section-head,
  .open-platform-assets__head {
    flex-direction: column;
  }

  .open-platform-assets__form-grid,
  .open-platform-assets__facts {
    grid-template-columns: 1fr;
  }
}
</style>
