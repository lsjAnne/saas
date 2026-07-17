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
    eyebrow="Portfolio Queue"
    title="Customer success follow-up and tenant drill-down"
    description="The main operating grid keeps tenant selection, follow-up priorities, churn warnings, and platform actions in one place."
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
              :label="tenant.tenantStatus"
              :tone="tenant.tenantStatus === 'active' ? 'success' : 'warn'"
            />
          </div>
          <p>{{ tenant.planName || 'No plan' }} / {{ tenant.subscriptionStatus || '--' }}</p>
          <p>Seats {{ tenant.seatCount }} / Quota {{ tenant.totalUsedAmount }} / {{ tenant.totalQuotaLimit }}</p>
          <p>Created {{ formatDate(tenant.createdAt) }} / Trial end {{ formatDate(tenant.trialEndAt) }}</p>
        </button>
      </div>

      <div class="tenant-matrix__detail">
        <div class="tenant-matrix__detail-card">
          <div class="tenant-matrix__row">
            <div>
              <p class="tenant-matrix__eyebrow">Selected tenant</p>
              <h4>{{ selectedTenant?.tenantName || 'Select a tenant' }}</h4>
            </div>
            <StatusPill
              :label="selectedTenant?.subscriptionStatus || 'idle'"
              :tone="selectedTenant?.subscriptionStatus === 'active' ? 'success' : 'warn'"
            />
          </div>
          <dl v-if="selectedTenant" class="tenant-matrix__detail-grid">
            <div>
              <dt>Owner</dt>
              <dd>{{ selectedTenant.ownerName }} / {{ selectedTenant.mobile }}</dd>
            </div>
            <div>
              <dt>Plan</dt>
              <dd>{{ selectedTenant.planName || '--' }}</dd>
            </div>
            <div>
              <dt>Seats</dt>
              <dd>{{ formatCount(selectedTenant.seatCount) }}</dd>
            </div>
            <div>
              <dt>Quota usage</dt>
              <dd>{{ formatPercent(quotaUsageRate(selectedTenant), 0) }}</dd>
            </div>
            <div>
              <dt>Created</dt>
              <dd>{{ formatDate(selectedTenant.createdAt) }}</dd>
            </div>
            <div>
              <dt>Trial end</dt>
              <dd>{{ formatDate(selectedTenant.trialEndAt) }}</dd>
            </div>
          </dl>

          <div class="tenant-matrix__action-row">
            <button class="tenant-matrix__primary" type="button" :disabled="isSubmitting" @click="emit('runAutomation')">
              Run subscription automation
            </button>
            <button class="tenant-matrix__secondary" type="button" :disabled="!selectedTenant || isSubmitting" @click="emit('suspendTenant')">
              Suspend tenant
            </button>
            <button class="tenant-matrix__secondary" type="button" :disabled="!selectedTenant || isSubmitting" @click="emit('resumeTenant')">
              Resume tenant
            </button>
          </div>
        </div>

        <div class="tenant-matrix__detail-card">
          <div>
            <p class="tenant-matrix__eyebrow">Customer success queue</p>
            <h4>Next follow-ups</h4>
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
                <StatusPill :label="item.tone" :tone="item.tone" />
              </div>
              <p>{{ item.planName }} / {{ item.seatCount }} seats / {{ formatPercent(item.quotaUsageRate, 0) }}</p>
              <p>{{ item.reason }}</p>
              <p>{{ item.nextAction }}</p>
            </button>
            <p v-if="customerSuccessFollowUps.length === 0" class="tenant-matrix__empty">
              No immediate customer-success follow-ups are required.
            </p>
          </div>
        </div>

        <div class="tenant-matrix__detail-card">
          <div>
            <p class="tenant-matrix__eyebrow">Churn warnings</p>
            <h4>Portfolio risk watch</h4>
          </div>
          <div class="tenant-matrix__warning-list">
            <article v-for="item in churnWarnings" :key="item.code" class="tenant-matrix__warning-card">
              <div class="tenant-matrix__row">
                <strong>{{ item.label }}</strong>
                <StatusPill :label="String(item.count)" :tone="item.tone" />
              </div>
              <p>{{ item.detail }}</p>
            </article>
            <p v-if="churnWarnings.length === 0" class="tenant-matrix__empty">
              No immediate churn warnings detected.
            </p>
          </div>
        </div>

        <div class="tenant-matrix__detail-card">
          <div>
            <p class="tenant-matrix__eyebrow">Feature gates</p>
            <h4>External systems and billing controls</h4>
          </div>
          <div class="tenant-matrix__toggle-list">
            <label v-for="item in featurePresets" :key="item.key" class="tenant-matrix__toggle-card">
              <div>
                <strong>{{ item.label }}</strong>
                <p>{{ item.description }}</p>
              </div>
              <input v-model="toggleForm[item.key]" type="checkbox" />
            </label>
          </div>
          <button class="tenant-matrix__primary" type="button" :disabled="!selectedTenant || isSubmitting" @click="submitFeatureToggles">
            Save feature flags
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
  border: 1px solid rgba(91, 72, 54, 0.1);
  background: rgba(255, 255, 255, 0.74);
}

.tenant-matrix__tenant-card,
.tenant-matrix__follow-up-card {
  display: grid;
  gap: 0.35rem;
  text-align: left;
  cursor: pointer;
}

.tenant-matrix__tenant-card--active {
  border-color: rgba(113, 87, 62, 0.44);
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
  color: var(--color-ink-faint);
  font-size: 0.75rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
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
  background: rgba(255, 248, 242, 0.72);
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
  min-height: 2.8rem;
  border-radius: var(--radius-md);
  border: 0;
  padding: 0 1rem;
  font-weight: 600;
  cursor: pointer;
}

.tenant-matrix__primary {
  background: linear-gradient(135deg, #3f5f55, #1f2c28);
  color: #fffaf4;
}

.tenant-matrix__secondary {
  background: rgba(61, 46, 36, 0.08);
  color: var(--color-ink-strong);
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
