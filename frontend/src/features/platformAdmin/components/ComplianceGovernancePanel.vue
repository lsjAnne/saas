<script setup lang="ts">
import { reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  AdminComplianceAcceptanceView,
  AdminComplianceDocumentView,
  ComplianceDocumentPublishPayload
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  documents: AdminComplianceDocumentView[];
  acceptances: AdminComplianceAcceptanceView[];
  selectedTenantId: string | null;
  filters: {
    tenantId: string;
    documentCode: string;
    acceptanceStatus: string;
  };
  isSubmitting: boolean;
  actionError?: string | null;
}

interface Emits {
  updateFilters: [payload: { tenantId?: string; documentCode?: string; acceptanceStatus?: string }];
  publishDocument: [documentCode: string, payload: ComplianceDocumentPublishPayload];
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});
const emit = defineEmits<Emits>();

const filterForm = reactive({
  tenantId: '',
  documentCode: '',
  acceptanceStatus: ''
});

const publishForm = reactive({
  documentCode: '',
  version: '',
  title: '',
  content: ''
});

watch(
  () => props.filters,
  (next) => {
    filterForm.tenantId = next.tenantId;
    filterForm.documentCode = next.documentCode;
    filterForm.acceptanceStatus = next.acceptanceStatus;
  },
  { immediate: true, deep: true }
);

watch(
  () => [props.selectedTenantId, props.documents[0]?.documentCode],
  () => {
    filterForm.tenantId = props.selectedTenantId ?? '';
    publishForm.documentCode = props.documents[0]?.documentCode ?? '';
  },
  { immediate: true }
);

function statusTone(status: string) {
  const normalized = status.toLowerCase();
  return normalized === 'accepted' || normalized === 'ready' ? 'success' : 'warn';
}

function submitFilters() {
  emit('updateFilters', {
    tenantId: filterForm.tenantId || undefined,
    documentCode: filterForm.documentCode || undefined,
    acceptanceStatus: filterForm.acceptanceStatus || undefined
  });
}

function submitPublish() {
  if (!publishForm.documentCode || !publishForm.version || !publishForm.title || !publishForm.content) {
    return;
  }

  emit('publishDocument', publishForm.documentCode, {
    version: publishForm.version,
    title: publishForm.title,
    content: publishForm.content
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Compliance Governance"
    title="文档发布与租户签收治理"
    description="平台端要在同一块面板里同时看到文档版本、租户签收状态和重签压力。"
  >
    <div class="compliance-panel">
      <div class="compliance-panel__card">
        <div>
          <p class="compliance-panel__eyebrow">文档发布</p>
          <h4>更新平台侧合规文档</h4>
        </div>
        <div class="compliance-panel__form-grid">
          <label class="compliance-panel__field">
            <span>文档编码</span>
            <select v-model="publishForm.documentCode" class="compliance-panel__control">
              <option v-for="doc in documents" :key="doc.documentCode" :value="doc.documentCode">
                {{ doc.documentCode }}
              </option>
            </select>
          </label>
          <label class="compliance-panel__field">
            <span>版本</span>
            <input v-model="publishForm.version" class="compliance-panel__control" type="text" />
          </label>
          <label class="compliance-panel__field compliance-panel__field--wide">
            <span>标题</span>
            <input v-model="publishForm.title" class="compliance-panel__control" type="text" />
          </label>
          <label class="compliance-panel__field compliance-panel__field--wide">
            <span>内容</span>
            <textarea v-model="publishForm.content" class="compliance-panel__textarea" rows="5" />
          </label>
        </div>
        <button class="compliance-panel__primary" type="button" :disabled="isSubmitting" @click="submitPublish">
          发布文档版本
        </button>
      </div>

      <div class="compliance-panel__card">
        <div>
          <p class="compliance-panel__eyebrow">签收筛选</p>
          <h4>按租户 / 文档 / 状态回读</h4>
        </div>
        <div class="compliance-panel__form-grid">
          <label class="compliance-panel__field">
            <span>租户 ID</span>
            <input v-model="filterForm.tenantId" class="compliance-panel__control" type="text" />
          </label>
          <label class="compliance-panel__field">
            <span>文档编码</span>
            <input v-model="filterForm.documentCode" class="compliance-panel__control" type="text" />
          </label>
          <label class="compliance-panel__field">
            <span>签收状态</span>
            <select v-model="filterForm.acceptanceStatus" class="compliance-panel__control">
              <option value="">全部</option>
              <option value="accepted">accepted</option>
              <option value="pending">pending</option>
              <option value="reaccept_required">reaccept_required</option>
            </select>
          </label>
        </div>
        <button class="compliance-panel__secondary" type="button" :disabled="isSubmitting" @click="submitFilters">
          刷新签收列表
        </button>
      </div>

      <div class="compliance-panel__grid">
        <div class="compliance-panel__card">
          <div>
            <p class="compliance-panel__eyebrow">文档概览</p>
            <h4>{{ documents.length }} 份</h4>
          </div>
          <div class="compliance-panel__list">
            <article v-for="doc in documents" :key="`${doc.documentCode}-${doc.version}`" class="compliance-panel__item">
              <div class="compliance-panel__row">
                <strong>{{ doc.documentCode }} v{{ doc.version }}</strong>
                <span>{{ formatDateTime(doc.updatedAt) }}</span>
              </div>
              <p>{{ doc.title }}</p>
              <p>已签 {{ doc.acceptedCurrentVersionTenantCount }} / 待重签 {{ doc.pendingReacceptanceTenantCount }} / 未签 {{ doc.neverAcceptedTenantCount }}</p>
            </article>
          </div>
        </div>

        <div class="compliance-panel__card">
          <div>
            <p class="compliance-panel__eyebrow">签收情况</p>
            <h4>{{ acceptances.length }} 条</h4>
          </div>
          <div class="compliance-panel__list">
            <article v-for="item in acceptances.slice(0, 8)" :key="`${item.tenantId}-${item.documentCode}`" class="compliance-panel__item">
              <div class="compliance-panel__row">
                <strong>{{ item.tenantName }}</strong>
                <StatusPill :label="item.acceptanceStatus" :tone="statusTone(item.acceptanceStatus)" />
              </div>
              <p>{{ item.documentCode }} / 当前 {{ item.currentVersion }} / 最新签收 {{ item.latestAcceptedVersion || '--' }}</p>
              <p>{{ item.reacceptRequired ? '需要重签' : '当前版本已覆盖' }} / {{ formatDateTime(item.latestAcceptedAt) }}</p>
            </article>
          </div>
          <div v-if="actionError" class="compliance-panel__error">{{ actionError }}</div>
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.compliance-panel,
.compliance-panel__card,
.compliance-panel__grid,
.compliance-panel__list {
  display: grid;
  gap: 0.9rem;
}

.compliance-panel__card {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 251, 247, 0.78);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.compliance-panel__grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.compliance-panel__form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.compliance-panel__field {
  display: grid;
  gap: 0.45rem;
}

.compliance-panel__field--wide {
  grid-column: span 2;
}

.compliance-panel__eyebrow,
.compliance-panel__field span,
.compliance-panel__card h4,
.compliance-panel__item p,
.compliance-panel__error {
  margin: 0;
}

.compliance-panel__eyebrow,
.compliance-panel__field span {
  color: var(--color-ink-faint);
  font-size: 0.75rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.compliance-panel__control,
.compliance-panel__textarea,
.compliance-panel__primary,
.compliance-panel__secondary {
  border-radius: var(--radius-md);
}

.compliance-panel__control,
.compliance-panel__textarea {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.16);
  background: rgba(255, 255, 255, 0.92);
  padding: 0.75rem 0.85rem;
}

.compliance-panel__primary,
.compliance-panel__secondary {
  min-height: 2.8rem;
  border: 0;
  padding: 0 1rem;
  font-weight: 600;
  cursor: pointer;
}

.compliance-panel__primary {
  background: linear-gradient(135deg, #3f5f55, #1f2c28);
  color: #fffaf4;
}

.compliance-panel__secondary {
  background: rgba(61, 46, 36, 0.08);
  color: var(--color-ink-strong);
}

.compliance-panel__item {
  display: grid;
  gap: 0.35rem;
  padding: 0.8rem;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.76);
}

.compliance-panel__row {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.compliance-panel__error {
  color: #b55d2f;
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .compliance-panel__grid,
  .compliance-panel__form-grid {
    grid-template-columns: 1fr;
  }

  .compliance-panel__field--wide {
    grid-column: span 1;
  }
}
</style>
