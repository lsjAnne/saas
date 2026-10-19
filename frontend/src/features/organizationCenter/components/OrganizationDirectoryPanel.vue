<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { Organization } from '@/services/apiTypes';
import { formatDate } from '@/utils/formatters';

interface Props {
  organizations: Organization[];
  selectedOrganizationId: string | null;
}

defineProps<Props>();

defineEmits<{
  select: [organizationId: string];
}>();

function toneFromStatus(status: string) {
  return status === 'active' ? 'success' : 'neutral';
}

function statusLabel(status: string) {
  return status === 'active' ? '正常' : '停用';
}
</script>

<template>
  <PanelCard
    title="组织目录"
    description="组织结构、成员入口与当前操作上下文一屏掌握。"
  >
    <div class="organization-directory">
      <button
        v-for="organization in organizations"
        :key="organization.id"
        type="button"
        :class="[
          'organization-directory__item',
          { 'organization-directory__item--active': selectedOrganizationId === organization.id }
        ]"
        @click="$emit('select', organization.id)"
      >
        <div class="organization-directory__head">
          <div>
            <h4 class="organization-directory__title">{{ organization.organizationName }}</h4>
            <p class="organization-directory__meta">
              创建于 {{ formatDate(organization.createdAt) }}
            </p>
          </div>
          <StatusPill :label="statusLabel(organization.status)" :tone="toneFromStatus(organization.status)" />
        </div>
      </button>

      <p v-if="organizations.length === 0" class="organization-directory__empty">
        当前租户还没有组织记录。
      </p>
    </div>
  </PanelCard>
</template>

<style scoped>
.organization-directory {
  display: grid;
  gap: 0.85rem;
}

.organization-directory__item {
  width: 100%;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
  text-align: left;
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.organization-directory__item--active {
  border-color: var(--color-accent);
  box-shadow: var(--shadow-card);
}

.organization-directory__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.organization-directory__title,
.organization-directory__meta,
.organization-directory__empty {
  margin: 0;
}

.organization-directory__title {
  color: var(--color-ink-strong);
}

.organization-directory__meta,
.organization-directory__empty {
  margin-top: 0.35rem;
  color: var(--color-ink-soft);
}
</style>
