<script setup lang="ts">
import { reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { AdminTenantOverview, FeatureToggleUpdatePayload } from '@/services/apiTypes';
import { formatCount, formatDate, formatPercent } from '@/utils/formatters';

type StatusTone = 'neutral' | 'warn' | 'success';

interface FeaturePreset {
  key: string;
  label: string;
  description: string;
  defaultValue: boolean;
}

interface ChurnWarningItem {
  code: string;
  label: string;
  count: number;
  detail: string;
  tone: StatusTone;
}

interface CustomerSuccessFollowUpItem {
  tenantId: string;
  tenantName: string;
  planName: string;
  reason: string;
  nextAction: string;
  tone: StatusTone;
  quotaUsageRate: number;
  seatCount: number;
}

interface Props {
  tenants: AdminTenantOverview[];
  selectedTenantId: string | null;
  selectedTenant: AdminTenantOverview | null;
  featurePresets: readonly FeaturePreset[];
  resolvedFeatureFlags: Record<string, boolean>;
  customerSuccessFollowUps: CustomerSuccessFollowUpItem[];
  churnWarnings: ChurnWarningItem[];
  isSubmitting: boolean;
  actionError?: string | null;
}

interface Emits {
  selectTenant: [tenantId: string];
  updateFeatureToggles: [payload: FeatureToggleUpdatePayload];
  suspendTenant: [];
  resumeTenant: [];
  runAutomation: [];
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<Emits>();

const toggleForm = reactive<Record<string, boolean>>({});

const labelMap: Record<string, string> = {
  'Auto charge': '自动续费',
  'Allow subscription automation to charge renewals automatically.': '允许订阅自动化自动扣费续费。',
  'ERP required': '需接入 ERP',
  'Treat ERP integration as a mandatory delivery gate.': '将 ERP 接入作为强制交付门禁。',
  'WMS required': '需接入 WMS',
  'Treat WMS readiness as a mandatory delivery gate.': '将 WMS 就绪作为强制交付门禁。',
  'Tax required': '需接入税务',
  'Treat tax and invoice connectivity as a mandatory gate.': '将税务与开票连通作为强制门禁。',
  'Messaging required': '需接入消息',
  'Require messaging gateway and callback worker readiness.': '要求消息网关与回调 worker 就绪。',
  'BI required': '需接入 BI',
  'Require BI and reporting connectivity before release.': '发布前要求 BI 与报表连通。',
  'Routing required': '需接入路由',
  'Require external routing integration as a delivery gate.': '将外部路由接入作为交付门禁。',
  'Payment overdue': '账单逾期',
  'Tenants in overdue or past-due states need payment recovery.': '逾期或欠费租户需要进行回款处理。',
  'Trial ending soon': '试用即将到期',
  'Trials ending within 7 days need a conversion follow-up.': '7 天内到期的试用需要转化跟进。',
  'Suspended tenants': '已停用租户',
  'Suspended tenants need a recovery or offboarding decision.': '已停用租户需要决定恢复或退出。',
  'Capacity hot spots': '容量紧张',
  'Quota usage over 85% is a likely upsell or risk signal.': '配额使用超过 85% 是增购或风险信号。',
  'Auto charge failed': '自动扣费失败',
  'Latest automation run found tenants with failed renewal charges.': '最近一次自动化发现续费扣费失败的租户。',
  'Payment is overdue.': '账单已逾期。',
  'Recover payment and confirm renewal status.': '追回款项并确认续费状态。',
  'Tenant is suspended.': '租户已停用。',
  'Confirm whether to resume service or close the account.': '确认恢复服务还是关闭账户。',
  'Trial is expiring within 7 days.': '试用将在 7 天内到期。',
  'Run conversion outreach and confirm package fit.': '开展转化触达并确认套餐匹配。',
  'Quota usage is above 85%.': '配额使用已超过 85%。',
  'Discuss seat or package expansion.': '沟通席位或套餐扩容。',
  'New paying tenant is still in the onboarding window.': '新付费租户仍在开通窗口期。',
  'Validate onboarding completion and adoption.': '确认开通完成与使用情况。'
};

function tenantStatusLabel(status?: string | null) {
  const map: Record<string, string> = {
    active: '正常',
    suspended: '已停用',
    trial: '试用',
    expiring: '临期',
    overdue: '逾期'
  };
  return (status && map[status]) || '未知';
}

function subscriptionStatusLabel(status?: string | null) {
  const map: Record<string, string> = {
    active: '生效',
    unsubscribed: '未订阅',
    trial: '试用',
    past_due: '已逾期'
  };
  return (status && map[status]) || '待命中';
}

function toneLabel(tone: string) {
  const map: Record<string, string> = { success: '正常', warn: '需关注', neutral: '一般' };
  return map[tone] ?? tone;
}

watch(
  () => props.resolvedFeatureFlags,
  (next) => {
    for (const preset of props.featurePresets) {
      toggleForm[preset.key] = next[preset.key] ?? preset.defaultValue;
    }
  },
  { immediate: true, deep: true }
);

function quotaUsageRate(tenant: AdminTenantOverview) {
  return tenant.totalUsedAmount / Math.max(tenant.totalQuotaLimit, 1);
}

function submitFeatureToggles() {
  emit('updateFeatureToggles', {
    featureFlags: Object.fromEntries(
      props.featurePresets.map((item) => [item.key, toggleForm[item.key] ?? item.defaultValue])
    )
  });
}
</script>

<template>
  <PanelCard
    title="客户成功跟进与租户下钻"
    description="租户选择、跟进优先级、流失预警与平台操作集中在一屏。"
  >
    <div class="tenant-matrix">
      <div class="tenant-matrix__list">
        <button
          v-for="tenant in tenants"
          :key="tenant.tenantId"
          type="button"
          :class="[
            'tenant-matrix__tenant-card',
            { 'tenant-matrix__tenant-card--active': tenant.tenantId === selectedTenantId }
          ]"
          @click="emit('selectTenant', tenant.tenantId)"
        >
          <div class="tenant-matrix__row">
            <strong>{{ tenant.tenantName }}</strong>
            <StatusPill
              :label="tenantStatusLabel(tenant.tenantStatus)"
              :tone="tenant.tenantStatus === 'active' ? 'success' : 'warn'"
            />
          </div>
          <p>{{ tenant.planName || '未订阅' }} / {{ subscriptionStatusLabel(tenant.subscriptionStatus) }}</p>
          <p>席位 {{ tenant.seatCount }} / 配额 {{ tenant.totalUsedAmount }} / {{ tenant.totalQuotaLimit }}</p>
          <p>创建于 {{ formatDate(tenant.createdAt) }} / 试用到期 {{ formatDate(tenant.trialEndAt) }}</p>
        </button>
      </div>

      <div class="tenant-matrix__detail">
        <div class="tenant-matrix__detail-card">
          <div class="tenant-matrix__row">
            <div>
              <p class="tenant-matrix__eyebrow">已选租户</p>
              <h4>{{ selectedTenant?.tenantName || '请选择租户' }}</h4>
            </div>
            <StatusPill
              :label="subscriptionStatusLabel(selectedTenant?.subscriptionStatus)"
              :tone="selectedTenant?.subscriptionStatus === 'active' ? 'success' : 'warn'"
            />
          </div>
          <dl v-if="selectedTenant" class="tenant-matrix__detail-grid">
            <div>
              <dt>负责人</dt>
              <dd>{{ selectedTenant.ownerName }} / {{ selectedTenant.mobile }}</dd>
            </div>
            <div>
              <dt>套餐</dt>
              <dd>{{ selectedTenant.planName || '--' }}</dd>
            </div>
            <div>
              <dt>席位</dt>
              <dd>{{ formatCount(selectedTenant.seatCount) }}</dd>
            </div>
            <div>
              <dt>配额使用</dt>
              <dd>{{ formatPercent(quotaUsageRate(selectedTenant), 0) }}</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ formatDate(selectedTenant.createdAt) }}</dd>
            </div>
            <div>
              <dt>试用到期</dt>
              <dd>{{ formatDate(selectedTenant.trialEndAt) }}</dd>
            </div>
          </dl>

          <div class="tenant-matrix__action-row">
            <button class="tenant-matrix__primary" type="button" :disabled="isSubmitting" @click="emit('runAutomation')">
              运行订阅自动化
            </button>
            <button class="tenant-matrix__secondary" type="button" :disabled="!selectedTenant || isSubmitting" @click="emit('suspendTenant')">
              停用租户
            </button>
            <button class="tenant-matrix__secondary" type="button" :disabled="!selectedTenant || isSubmitting" @click="emit('resumeTenant')">
              恢复租户
            </button>
          </div>
        </div>

        <div class="tenant-matrix__detail-card">
          <div>
            <p class="tenant-matrix__eyebrow">客户成功队列</p>
            <h4>待跟进租户</h4>
          </div>
          <div class="tenant-matrix__follow-up-list">
            <button
              v-for="item in customerSuccessFollowUps"
              :key="item.tenantId"
              type="button"
              class="tenant-matrix__follow-up-card"
              @click="emit('selectTenant', item.tenantId)"
            >
              <div class="tenant-matrix__row">
                <strong>{{ item.tenantName }}</strong>
                <StatusPill :label="toneLabel(item.tone)" :tone="item.tone" />
              </div>
              <p>{{ item.planName }} / {{ item.seatCount }} 席 / {{ formatPercent(item.quotaUsageRate, 0) }}</p>
              <p>{{ labelMap[item.reason] ?? item.reason }}</p>
              <p>{{ labelMap[item.nextAction] ?? item.nextAction }}</p>
            </button>
            <p v-if="customerSuccessFollowUps.length === 0" class="tenant-matrix__empty">
              当前没有需要立即跟进的客户成功事项。
            </p>
          </div>
        </div>

        <div class="tenant-matrix__detail-card">
          <div>
            <p class="tenant-matrix__eyebrow">流失预警</p>
            <h4>组合风险监视</h4>
          </div>
          <div class="tenant-matrix__warning-list">
            <article v-for="item in churnWarnings" :key="item.code" class="tenant-matrix__warning-card">
              <div class="tenant-matrix__row">
                <strong>{{ labelMap[item.label] ?? item.label }}</strong>
                <StatusPill :label="String(item.count)" :tone="item.tone" />
              </div>
              <p>{{ labelMap[item.detail] ?? item.detail }}</p>
            </article>
            <p v-if="churnWarnings.length === 0" class="tenant-matrix__empty">
              当前没有检测到流失预警。
            </p>
          </div>
        </div>

        <div class="tenant-matrix__detail-card">
          <div>
            <p class="tenant-matrix__eyebrow">功能门禁</p>
            <h4>外部系统与计费控制</h4>
          </div>
          <div class="tenant-matrix__toggle-list">
            <label v-for="item in featurePresets" :key="item.key" class="tenant-matrix__toggle-card">
              <div>
                <strong>{{ labelMap[item.label] ?? item.label }}</strong>
                <p>{{ labelMap[item.description] ?? item.description }}</p>
              </div>
              <input v-model="toggleForm[item.key]" type="checkbox" />
            </label>
          </div>
          <button class="tenant-matrix__primary" type="button" :disabled="!selectedTenant || isSubmitting" @click="submitFeatureToggles">
            保存功能开关
          </button>
          <div v-if="actionError" class="tenant-matrix__error">{{ actionError }}</div>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.tenant-matrix {
  display: grid;
  grid-template-columns: 0.92fr 1.08fr;
  gap: 1rem;
}

.tenant-matrix__list,
.tenant-matrix__detail,
.tenant-matrix__detail-card,
.tenant-matrix__toggle-list,
.tenant-matrix__follow-up-list,
.tenant-matrix__warning-list {
  display: grid;
  gap: 0.8rem;
}

.tenant-matrix__tenant-card,
.tenant-matrix__detail-card,
.tenant-matrix__toggle-card,
.tenant-matrix__follow-up-card,
.tenant-matrix__warning-card {
  padding: 0.95rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.tenant-matrix__tenant-card,
.tenant-matrix__follow-up-card {
  display: grid;
  gap: 0.35rem;
  text-align: left;
  cursor: pointer;
}

.tenant-matrix__tenant-card--active {
  border-color: var(--color-accent);
}

.tenant-matrix__row,
.tenant-matrix__action-row,
.tenant-matrix__toggle-card {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.tenant-matrix__tenant-card p,
.tenant-matrix__eyebrow,
.tenant-matrix__detail-card h4,
.tenant-matrix__toggle-card p,
.tenant-matrix__follow-up-card p,
.tenant-matrix__warning-card p,
.tenant-matrix__error,
.tenant-matrix__empty {
  margin: 0;
}

.tenant-matrix__eyebrow,
.tenant-matrix__detail-grid dt {
  color: var(--color-ink-soft);
  font-size: 0.78rem;
  font-weight: 600;
}

.tenant-matrix__detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0;
}

.tenant-matrix__detail-grid div {
  padding: 0.8rem;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.tenant-matrix__detail-grid dd {
  margin: 0.35rem 0 0;
}

.tenant-matrix__toggle-card input {
  width: 1rem;
  height: 1rem;
  margin-top: 0.25rem;
}

.tenant-matrix__primary,
.tenant-matrix__secondary {
  min-height: 2.5rem;
  border-radius: var(--radius-md);
  padding: 0 0.95rem;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast),
    background var(--transition-fast);
}

.tenant-matrix__primary {
  border: 0;
  background: var(--color-accent);
  color: #ffffff;
}

.tenant-matrix__primary:hover {
  background: var(--color-accent-hover);
  color: #ffffff;
}

.tenant-matrix__secondary {
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
}

.tenant-matrix__secondary:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.tenant-matrix__error {
  color: #b55d2f;
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .tenant-matrix,
  .tenant-matrix__detail-grid {
    grid-template-columns: 1fr;
  }

  .tenant-matrix__action-row {
    flex-direction: column;
  }
}
</style>
