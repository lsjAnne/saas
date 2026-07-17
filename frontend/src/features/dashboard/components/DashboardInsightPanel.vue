<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  DashboardActionItem,
  DashboardQuickEntry
} from '@/features/dashboard/composables/useDashboardOverview';
import { formatCount } from '@/utils/formatters';

interface Props {
  suggestedActions: DashboardActionItem[];
  quickEntries: DashboardQuickEntry[];
}

defineProps<Props>();
</script>

<template>
  <PanelCard
    eyebrow="Action Deck"
    title="今日建议动作区与快捷入口区"
    description="默认展示系统建议动作，同时给订单、异常、草稿和直播保留直达入口。"
  >
    <div class="insight-panel">
      <section class="insight-panel__block">
        <div class="insight-panel__copy">
          <p class="insight-panel__label">今日建议动作</p>
          <h4 class="insight-panel__headline">先做最能改变今天结果的几件事</h4>
        </div>

        <div class="insight-panel__actions">
          <RouterLink
            v-for="action in suggestedActions"
            :key="action.id"
            :to="action.to"
            class="insight-panel__action-card"
          >
            <div class="insight-panel__action-head">
              <h5 class="insight-panel__action-title">{{ action.title }}</h5>
              <StatusPill :label="action.metricText" :tone="action.tone" />
            </div>
            <p class="insight-panel__action-copy">{{ action.description }}</p>
          </RouterLink>

          <p v-if="suggestedActions.length === 0" class="insight-panel__empty">
            当前账号没有可展示的建议动作入口。
          </p>
        </div>
      </section>

      <section class="insight-panel__block">
        <div class="insight-panel__copy">
          <p class="insight-panel__label">快捷入口</p>
          <h4 class="insight-panel__headline">一跳进入高频执行页</h4>
        </div>

        <div class="insight-panel__shortcuts">
          <RouterLink
            v-for="entry in quickEntries"
            :key="entry.id"
            :to="entry.to"
            class="insight-panel__shortcut-card"
          >
            <div class="insight-panel__shortcut-head">
              <h5 class="insight-panel__shortcut-title">{{ entry.title }}</h5>
              <strong class="insight-panel__shortcut-value">
                {{ formatCount(entry.count, ` ${entry.unit}`) }}
              </strong>
            </div>
            <p class="insight-panel__shortcut-copy">{{ entry.description }}</p>
          </RouterLink>

          <p v-if="quickEntries.length === 0" class="insight-panel__empty">
            当前账号没有可展示的快捷入口。
          </p>
        </div>
      </section>
    </div>
  </PanelCard>
</template>

<style scoped>
.insight-panel {
  display: grid;
  gap: 1rem;
}

.insight-panel__block,
.insight-panel__action-card,
.insight-panel__shortcut-card {
  display: grid;
  gap: 0.9rem;
}

.insight-panel__block {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.insight-panel__copy {
  display: grid;
  gap: 0.35rem;
}

.insight-panel__label,
.insight-panel__headline,
.insight-panel__action-title,
.insight-panel__action-copy,
.insight-panel__shortcut-title,
.insight-panel__shortcut-copy,
.insight-panel__empty {
  margin: 0;
}

.insight-panel__label {
  color: var(--color-ink-faint);
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.insight-panel__headline,
.insight-panel__action-title,
.insight-panel__shortcut-title,
.insight-panel__shortcut-value {
  color: var(--color-ink-strong);
}

.insight-panel__actions,
.insight-panel__shortcuts {
  display: grid;
  gap: 0.85rem;
}

.insight-panel__action-card,
.insight-panel__shortcut-card {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(252, 249, 244, 0.92);
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease;
}

.insight-panel__action-card:hover,
.insight-panel__shortcut-card:hover {
  transform: translateY(-1px);
  border-color: rgba(133, 98, 67, 0.24);
  box-shadow: 0 12px 28px rgba(58, 43, 28, 0.08);
}

.insight-panel__action-head,
.insight-panel__shortcut-head {
  display: flex;
  justify-content: space-between;
  gap: 0.9rem;
  align-items: flex-start;
}

.insight-panel__action-copy,
.insight-panel__shortcut-copy,
.insight-panel__empty {
  color: var(--color-ink-soft);
  line-height: 1.65;
}

@media (max-width: 760px) {
  .insight-panel__action-head,
  .insight-panel__shortcut-head {
    flex-direction: column;
  }
}
</style>
