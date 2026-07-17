<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { DashboardPendingItem } from '@/features/dashboard/composables/useDashboardOverview';
import { formatCount } from '@/utils/formatters';

interface Props {
  pendingItems: DashboardPendingItem[];
}

defineProps<Props>();
</script>

<template>
  <PanelCard
    eyebrow="Pending Queue"
    title="待处理事项区"
    description="先把今天真正需要推进的队列排清楚，再决定去哪个中心页继续处理。"
  >
    <div class="pending-panel">
      <RouterLink
        v-for="item in pendingItems"
        :key="item.id"
        :to="item.to"
        class="pending-panel__item"
      >
        <div class="pending-panel__header">
          <div>
            <h4 class="pending-panel__title">{{ item.title }}</h4>
            <p class="pending-panel__description">{{ item.description }}</p>
          </div>
          <StatusPill :label="formatCount(item.count, ' 项')" :tone="item.tone" />
        </div>
      </RouterLink>

      <p v-if="pendingItems.length === 0" class="pending-panel__empty">
        当前账号没有可进入的待处理队列入口。
      </p>
    </div>
  </PanelCard>
</template>

<style scoped>
.pending-panel {
  display: grid;
  gap: 0.85rem;
}

.pending-panel__item {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease;
}

.pending-panel__item:hover {
  transform: translateY(-1px);
  border-color: rgba(133, 98, 67, 0.24);
  box-shadow: 0 12px 28px rgba(58, 43, 28, 0.08);
}

.pending-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 0.9rem;
  align-items: flex-start;
}

.pending-panel__title,
.pending-panel__description,
.pending-panel__empty {
  margin: 0;
}

.pending-panel__title {
  color: var(--color-ink-strong);
}

.pending-panel__description,
.pending-panel__empty {
  margin-top: 0.35rem;
  color: var(--color-ink-soft);
  line-height: 1.65;
}

@media (max-width: 760px) {
  .pending-panel__header {
    flex-direction: column;
  }
}
</style>
