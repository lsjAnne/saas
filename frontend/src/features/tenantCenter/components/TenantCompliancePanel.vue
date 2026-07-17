<script setup lang="ts">
import { computed, shallowRef } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { AdminTenantOverview } from '@/services/apiTypes';
import { formatDate, formatPercent } from '@/utils/formatters';

interface Props {
  tenants: AdminTenantOverview[];
  selectedTenantId: string | null;
}

interface Emits {
  selectTenant: [tenantId: string];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const query = shallowRef('');

const visibleTenants = computed(() => {
  const keyword = query.value.trim().toLowerCase();

  if (!keyword) {
    return props.tenants;
  }

  return props.tenants.filter((tenant) =>
    [tenant.tenantName, tenant.tenantCode, tenant.ownerName, tenant.mobile]
      .join(' ')
      .toLowerCase()
      .includes(keyword)
  );
});

function usageRate(tenant: AdminTenantOverview) {
  return tenant.totalUsedAmount / Math.max(tenant.totalQuotaLimit, 1);
}
</script>

<template>
  <PanelCard
    eyebrow="Tenant List"
    title="租户列表区"
    description="按租户状态、套餐和试用到期快速定位当前需要运营动作的租户。"
  >
    <label class="tenant-list__search">
      <span class="tenant-list__search-label">搜索租户</span>
      <input v-model="query" class="tenant-list__search-input" type="text" placeholder="名称 / 编码 / 负责人 / 手机号" />
    </label>

    <div v-if="visibleTenants.length" class="tenant-list">
      <button
        v-for="tenant in visibleTenants"
        :key="tenant.tenantId"
        type="button"
        :class="[
          'tenant-list__item',
          { 'tenant-list__item--active': tenant.tenantId === selectedTenantId }
        ]"
        @click="emit('selectTenant', tenant.tenantId)"
      >
        <div class="tenant-list__head">
          <div>
            <p class="tenant-list__name">{{ tenant.tenantName }}</p>
            <p class="tenant-list__code">{{ tenant.tenantCode }}</p>
          </div>
          <StatusPill
            :label="tenant.tenantStatus"
            :tone="tenant.tenantStatus === 'active' ? 'success' : 'warn'"
          />
        </div>

        <div class="tenant-list__facts">
          <span>{{ tenant.planName || '未订阅' }} / {{ tenant.subscriptionStatus || '--' }}</span>
          <span>席位 {{ tenant.seatCount }}</span>
          <span>试用到期 {{ formatDate(tenant.trialEndAt) }}</span>
          <span>配额使用 {{ formatPercent(usageRate(tenant), 0) }}</span>
        </div>
      </button>
    </div>

    <p v-else class="tenant-list__empty">当前没有匹配的租户。</p>
  </PanelCard>
</template>

<style scoped>
.tenant-list__search,
.tenant-list {
  display: grid;
  gap: 0.85rem;
}

.tenant-list__search {
  margin-bottom: 1rem;
}

.tenant-list__search-label,
.tenant-list__name,
.tenant-list__code,
.tenant-list__empty {
  margin: 0;
}

.tenant-list__search-label,
.tenant-list__code,
.tenant-list__facts,
.tenant-list__empty {
  color: var(--color-ink-soft);
}

.tenant-list__search-label {
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.tenant-list__search-input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: calc(var(--radius-lg) - 0.25rem);
  padding: 0.72rem 0.8rem;
  background: rgba(255, 251, 247, 0.92);
  color: var(--color-ink-strong);
  font: inherit;
}

.tenant-list__item {
  display: grid;
  gap: 0.7rem;
  width: 100%;
  padding: 0.95rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.1);
  background: rgba(255, 255, 255, 0.74);
  text-align: left;
  cursor: pointer;
}

.tenant-list__item--active {
  border-color: rgba(113, 87, 62, 0.44);
  background: rgba(252, 246, 239, 0.96);
}

.tenant-list__head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: flex-start;
}

.tenant-list__name {
  color: var(--color-ink-strong);
  font-weight: 600;
}

.tenant-list__code {
  margin-top: 0.3rem;
  font-size: 0.84rem;
}

.tenant-list__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.45rem 0.8rem;
  font-size: 0.92rem;
}

@media (max-width: 720px) {
  .tenant-list__facts,
  .tenant-list__head {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
