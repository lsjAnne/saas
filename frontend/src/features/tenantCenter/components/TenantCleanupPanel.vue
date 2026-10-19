<script setup lang="ts">
import { reactive } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { AdminTenantOverview, RegisterTenantRequest } from '@/services/apiTypes';
import { formatDate } from '@/utils/formatters';

interface Props {
  selectedTenant: AdminTenantOverview | null;
  expiringTenants: AdminTenantOverview[];
  isSubmitting: boolean;
  actionError?: string | null;
}

interface Emits {
  createTenant: [payload: RegisterTenantRequest];
  startTrial: [];
  suspendTenant: [];
  resumeTenant: [];
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});
const emit = defineEmits<Emits>();

const createForm = reactive<RegisterTenantRequest>({
  tenantName: '',
  ownerName: '',
  mobile: ''
});

function submitCreateTenant() {
  emit('createTenant', {
    tenantName: createForm.tenantName.trim(),
    ownerName: createForm.ownerName.trim(),
    mobile: createForm.mobile.trim()
  });
}

function tenantStatusLabel(status?: string | null) {
  const map: Record<string, string> = {
    active: '正常',
    suspended: '已停用',
    trial: '试用',
    expiring: '临期'
  };
  return (status && map[status]) || '未知';
}
</script>

<template>
  <PanelCard
    title="试用到期与停复服操作区"
    description="右侧统一处理开通租户、启动试用、停服和恢复服务，同时把试用临期租户直接拉到面前。"
  >
    <div class="tenant-lifecycle">
      <div class="tenant-lifecycle__alerts">
        <div class="tenant-lifecycle__section-head">
          <div>
            <p class="tenant-lifecycle__eyebrow">试用到期</p>
            <h4 class="tenant-lifecycle__title">试用到期区</h4>
          </div>
        </div>

        <div v-if="expiringTenants.length" class="tenant-lifecycle__alert-list">
          <article
            v-for="tenant in expiringTenants"
            :key="tenant.tenantId"
            class="tenant-lifecycle__alert-item"
          >
            <div class="tenant-lifecycle__row">
              <strong>{{ tenant.tenantName }}</strong>
              <StatusPill
                :label="tenantStatusLabel(tenant.tenantStatus)"
                :tone="tenant.tenantStatus === 'trial' ? 'warn' : 'neutral'"
              />
            </div>
            <p class="tenant-lifecycle__alert-meta">
              试用到期 {{ formatDate(tenant.trialEndAt) }} · 当前套餐 {{ tenant.planName || '未订阅' }}
            </p>
          </article>
        </div>
        <p v-else class="tenant-lifecycle__empty">当前没有试用临期租户。</p>
      </div>

      <div class="tenant-lifecycle__actions">
        <div class="tenant-lifecycle__card">
          <div>
            <p class="tenant-lifecycle__eyebrow">开通租户</p>
            <h4 class="tenant-lifecycle__title">开通租户</h4>
          </div>

          <label class="tenant-lifecycle__field">
            <span class="tenant-lifecycle__field-label">租户名称</span>
            <input v-model="createForm.tenantName" class="tenant-lifecycle__input" type="text" />
          </label>
          <label class="tenant-lifecycle__field">
            <span class="tenant-lifecycle__field-label">负责人</span>
            <input v-model="createForm.ownerName" class="tenant-lifecycle__input" type="text" />
          </label>
          <label class="tenant-lifecycle__field">
            <span class="tenant-lifecycle__field-label">手机号</span>
            <input v-model="createForm.mobile" class="tenant-lifecycle__input" type="text" />
          </label>

          <button class="tenant-lifecycle__primary" type="button" :disabled="isSubmitting" @click="submitCreateTenant">
            {{ isSubmitting ? '提交中...' : '开通并启动试用' }}
          </button>
        </div>

        <div class="tenant-lifecycle__card">
          <div>
            <p class="tenant-lifecycle__eyebrow">租户状态</p>
            <h4 class="tenant-lifecycle__title">
              {{ selectedTenant?.tenantName || '等待选择租户' }}
            </h4>
          </div>

          <dl v-if="selectedTenant" class="tenant-lifecycle__detail">
            <div>
              <dt>当前状态</dt>
              <dd>{{ tenantStatusLabel(selectedTenant.tenantStatus) }}</dd>
            </div>
            <div>
              <dt>试用到期</dt>
              <dd>{{ formatDate(selectedTenant.trialEndAt) }}</dd>
            </div>
          </dl>

          <div class="tenant-lifecycle__buttons">
            <button
              class="tenant-lifecycle__secondary"
              type="button"
              :disabled="!selectedTenant || isSubmitting"
              @click="emit('startTrial')"
            >
              启动试用
            </button>
            <button
              class="tenant-lifecycle__secondary"
              type="button"
              :disabled="!selectedTenant || isSubmitting"
              @click="emit('suspendTenant')"
            >
              停服
            </button>
            <button
              class="tenant-lifecycle__secondary"
              type="button"
              :disabled="!selectedTenant || isSubmitting"
              @click="emit('resumeTenant')"
            >
              恢复服务
            </button>
          </div>

          <p v-if="actionError" class="tenant-lifecycle__error">{{ actionError }}</p>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.tenant-lifecycle {
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  gap: 1rem;
}

.tenant-lifecycle__alerts,
.tenant-lifecycle__actions,
.tenant-lifecycle__card,
.tenant-lifecycle__alert-list {
  display: grid;
  gap: 0.8rem;
}

.tenant-lifecycle__card,
.tenant-lifecycle__alert-item {
  padding: 0.95rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.tenant-lifecycle__section-head,
.tenant-lifecycle__row,
.tenant-lifecycle__buttons {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: flex-start;
}

.tenant-lifecycle__eyebrow,
.tenant-lifecycle__title,
.tenant-lifecycle__alert-meta,
.tenant-lifecycle__field-label,
.tenant-lifecycle__empty,
.tenant-lifecycle__error {
  margin: 0;
}

.tenant-lifecycle__eyebrow,
.tenant-lifecycle__field-label,
.tenant-lifecycle__detail dt {
  color: var(--color-ink-soft);
  font-size: 0.78rem;
  font-weight: 600;
}

.tenant-lifecycle__alert-meta,
.tenant-lifecycle__empty,
.tenant-lifecycle__error,
.tenant-lifecycle__detail dd {
  color: var(--color-ink-soft);
}

.tenant-lifecycle__field {
  display: grid;
  gap: 0.45rem;
}

.tenant-lifecycle__input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--color-line);
  border-radius: calc(var(--radius-lg) - 0.25rem);
  padding: 0.72rem 0.8rem;
  background: #fff;
  color: var(--color-ink-strong);
  font: inherit;
}

.tenant-lifecycle__detail {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0;
}

.tenant-lifecycle__detail div {
  padding: 0.8rem;
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.tenant-lifecycle__detail dd {
  margin: 0.35rem 0 0;
}

.tenant-lifecycle__buttons {
  flex-wrap: wrap;
}

.tenant-lifecycle__primary,
.tenant-lifecycle__secondary {
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

.tenant-lifecycle__primary {
  border: 0;
  background: var(--color-accent);
  color: #ffffff;
}

.tenant-lifecycle__primary:hover {
  background: var(--color-accent-hover);
  color: #ffffff;
}

.tenant-lifecycle__secondary {
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
}

.tenant-lifecycle__secondary:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.tenant-lifecycle__error {
  line-height: 1.6;
}

@media (max-width: 1080px) {
  .tenant-lifecycle,
  .tenant-lifecycle__detail {
    grid-template-columns: 1fr;
  }
}
</style>
