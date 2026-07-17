<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ApprovalTemplateView } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  templates: ApprovalTemplateView[];
}

defineProps<Props>();
</script>

<template>
  <PanelCard
    eyebrow="Template Matrix"
    title="审批模板与阶段编排"
    description="模板本身就是治理资产，直接展示启用态、阶段顺序和处理人分配。"
  >
    <div v-if="templates.length" class="template-matrix">
      <article
        v-for="template in templates"
        :key="template.templateId"
        class="template-matrix__card"
      >
        <div class="template-matrix__header">
          <div>
            <p class="template-matrix__name">{{ template.templateName }}</p>
            <p class="template-matrix__meta">
              {{ template.templateCode }} · {{ template.approvalType }}
            </p>
          </div>
          <StatusPill
            :label="template.enabled ? '已启用' : '已停用'"
            :tone="template.enabled ? 'success' : 'neutral'"
          />
        </div>

        <div class="template-matrix__stages">
          <article
            v-for="stage in template.stages"
            :key="`${template.templateId}-${stage.stageCode}-${stage.stageOrder}`"
            class="template-matrix__stage"
          >
            <p class="template-matrix__stage-order">0{{ stage.stageOrder }}</p>
            <div>
              <p class="template-matrix__stage-code">{{ stage.stageCode }}</p>
              <p class="template-matrix__stage-handler">处理人 {{ stage.handlerId }}</p>
            </div>
          </article>
        </div>

        <p class="template-matrix__created-at">
          创建于 {{ formatDateTime(template.createdAt) }}
        </p>
      </article>
    </div>

    <p v-else class="template-matrix__empty">
      当前没有审批模板数据。
    </p>
  </PanelCard>
</template>

<style scoped>
.template-matrix {
  display: grid;
  gap: 0.85rem;
}

.template-matrix__card {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.template-matrix__header,
.template-matrix__stage {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.template-matrix__name,
.template-matrix__meta,
.template-matrix__stage-order,
.template-matrix__stage-code,
.template-matrix__stage-handler,
.template-matrix__created-at,
.template-matrix__empty {
  margin: 0;
}

.template-matrix__name,
.template-matrix__stage-code {
  color: var(--color-ink-strong);
}

.template-matrix__meta,
.template-matrix__stage-handler,
.template-matrix__created-at,
.template-matrix__empty {
  color: var(--color-ink-soft);
}

.template-matrix__meta,
.template-matrix__created-at {
  margin-top: 0.3rem;
}

.template-matrix__stages {
  display: grid;
  gap: 0.65rem;
}

.template-matrix__stage {
  padding: 0.8rem 0.85rem;
  border-radius: calc(var(--radius-lg) - 0.25rem);
  background: rgba(250, 245, 239, 0.82);
}

.template-matrix__stage-order {
  min-width: 2rem;
  color: var(--color-ink-faint);
  font-family: var(--font-display);
  font-size: 1.15rem;
}
</style>
