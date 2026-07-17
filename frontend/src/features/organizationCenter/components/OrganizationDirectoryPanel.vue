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
</script>

<template>
  <PanelCard
    eyebrow="Organizations"
    title="组织目录"
    description="把组织结构、成员入口和当前操作上下文放在同一屏，避免团队协作配置分散。"
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
          <StatusPill :label="organization.status" :tone="toneFromStatus(organization.status)" />
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
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
  text-align: left;
}

.organization-directory__item--active {
  border-color: rgba(133, 98, 67, 0.34);
  box-shadow: 0 12px 28px rgba(58, 43, 28, 0.1);
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
