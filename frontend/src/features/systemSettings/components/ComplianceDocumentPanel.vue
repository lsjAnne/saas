<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  ComplianceAcceptance,
  ComplianceDocument
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  privacyPolicy: ComplianceDocument | null;
  userAgreement: ComplianceDocument | null;
  acceptances: ComplianceAcceptance[];
}

const props = defineProps<Props>();

const documents = computed(() =>
  [props.privacyPolicy, props.userAgreement]
    .filter((document): document is ComplianceDocument => Boolean(document))
    .map((document) => {
      const acceptance = props.acceptances.find((item) => item.documentCode === document.documentCode);

      return {
        ...document,
        acceptance
      };
    })
);
</script>

<template>
  <PanelCard
    eyebrow="Compliance Docs"
    title="合规文档与确认版本"
    description="高频状态放租户中心，这里补文档版本与确认版本是否对齐，帮助管理员判断是否需要再次确认。"
  >
    <div class="document-panel">
      <article v-for="document in documents" :key="document.documentCode" class="document-panel__item">
        <div class="document-panel__header">
          <div>
            <h4 class="document-panel__title">{{ document.title }}</h4>
            <p class="document-panel__meta">当前版本 {{ document.version }}</p>
          </div>
          <StatusPill
            :label="document.acceptance?.version === document.version ? '版本一致' : '需确认'"
            :tone="document.acceptance?.version === document.version ? 'success' : 'warn'"
          />
        </div>

        <p class="document-panel__excerpt">
          {{ document.content.slice(0, 120) || '暂无文档摘要' }}
        </p>

        <p class="document-panel__acceptance">
          {{
            document.acceptance
              ? `最近确认版本 ${document.acceptance.version} · ${formatDateTime(document.acceptance.acceptedAt)}`
              : '当前尚未存在确认记录'
          }}
        </p>
      </article>
    </div>
  </PanelCard>
</template>

<style scoped>
.document-panel {
  display: grid;
  gap: 0.85rem;
}

.document-panel__item {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.document-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.document-panel__title,
.document-panel__meta,
.document-panel__excerpt,
.document-panel__acceptance {
  margin: 0;
}

.document-panel__title {
  color: var(--color-ink-strong);
}

.document-panel__meta,
.document-panel__excerpt,
.document-panel__acceptance {
  margin-top: 0.35rem;
  color: var(--color-ink-soft);
  line-height: 1.65;
}
</style>
