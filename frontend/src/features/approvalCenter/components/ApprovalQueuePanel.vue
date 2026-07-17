<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ApprovalInstance } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  approvals: ApprovalInstance[];
  selectedApprovalId: string | null;
  selectedApproval: ApprovalInstance | null;
  actionRemark: string;
  transferHandlerId: string;
  actionFeedback: string | null;
  isSubmittingAction: boolean;
}

interface Emits {
  select: [approvalId: string];
  updateActionRemark: [value: string];
  updateTransferHandlerId: [value: string];
  approve: [];
  reject: [];
  transfer: [];
}

defineProps<Props>();
const emit = defineEmits<Emits>();

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toneFromStatus(status: string) {
  const normalized = normalizeText(status);

  if (normalized.includes('approved') || normalized.includes('done')) {
    return 'success';
  }

  if (normalized.includes('reject') || normalized.includes('cancel')) {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Approval Queue"
    title="待处理审批队列"
    description="左侧看队列，右侧直接执行通过、驳回或转交，不用跳到别的页面。"
  >
    <div class="approval-queue">
      <div class="approval-queue__list">
        <button
          v-for="approval in approvals"
          :key="approval.approvalId"
          type="button"
          :class="[
            'approval-queue__item',
            { 'approval-queue__item--active': approval.approvalId === selectedApprovalId }
          ]"
          @click="emit('select', approval.approvalId)"
        >
          <div class="approval-queue__item-head">
            <div>
              <p class="approval-queue__item-title">{{ approval.approvalType }}</p>
              <p class="approval-queue__item-meta">
                {{ approval.relatedType }} · {{ approval.relatedId }}
              </p>
            </div>
            <StatusPill :label="approval.status" :tone="toneFromStatus(approval.status)" />
          </div>
          <p class="approval-queue__item-detail">
            当前处理人 {{ approval.currentHandlerId || '--' }} · {{ formatDateTime(approval.createdAt) }}
          </p>
        </button>
      </div>

      <div class="approval-queue__action-card">
        <div v-if="selectedApproval" class="approval-queue__action-body">
          <div class="approval-queue__action-header">
            <div>
              <p class="approval-queue__action-title">{{ selectedApproval.approvalType }}</p>
              <p class="approval-queue__action-meta">
                {{ selectedApproval.relatedType }} · {{ selectedApproval.relatedId }}
              </p>
            </div>
            <StatusPill :label="selectedApproval.status" :tone="toneFromStatus(selectedApproval.status)" />
          </div>

          <dl class="approval-queue__facts">
            <div class="approval-queue__fact">
              <dt>当前处理人</dt>
              <dd>{{ selectedApproval.currentHandlerId || '--' }}</dd>
            </div>
            <div class="approval-queue__fact">
              <dt>创建时间</dt>
              <dd>{{ formatDateTime(selectedApproval.createdAt) }}</dd>
            </div>
            <div class="approval-queue__fact">
              <dt>发起备注</dt>
              <dd>{{ selectedApproval.remark || '当前没有发起备注。' }}</dd>
            </div>
            <div class="approval-queue__fact">
              <dt>处理结果</dt>
              <dd>{{ selectedApproval.resultRemark || '尚未产生处理结果备注。' }}</dd>
            </div>
          </dl>

          <label class="approval-queue__field">
            <span class="approval-queue__field-label">处理备注</span>
            <textarea
              class="approval-queue__textarea"
              rows="4"
              :value="actionRemark"
              placeholder="例如：补充风险说明、审批理由或驳回意见"
              @input="emit('updateActionRemark', ($event.target as HTMLTextAreaElement).value)"
            />
          </label>

          <label class="approval-queue__field">
            <span class="approval-queue__field-label">转交处理人 ID</span>
            <input
              class="approval-queue__input"
              type="text"
              :value="transferHandlerId"
              placeholder="例如：ops.director"
              @input="emit('updateTransferHandlerId', ($event.target as HTMLInputElement).value)"
            />
          </label>

          <p v-if="actionFeedback" class="approval-queue__feedback">
            {{ actionFeedback }}
          </p>

          <div class="approval-queue__actions">
            <button
              type="button"
              class="approval-queue__action approval-queue__action--primary"
              :disabled="isSubmittingAction"
              @click="emit('approve')"
            >
              通过
            </button>
            <button
              type="button"
              class="approval-queue__action"
              :disabled="isSubmittingAction"
              @click="emit('transfer')"
            >
              转交
            </button>
            <button
              type="button"
              class="approval-queue__action approval-queue__action--warn"
              :disabled="isSubmittingAction"
              @click="emit('reject')"
            >
              驳回
            </button>
          </div>
        </div>

        <p v-else class="approval-queue__empty">
          当前没有可操作的审批项。
        </p>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.approval-queue {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(18rem, 1.08fr);
  gap: 1rem;
}

.approval-queue__list,
.approval-queue__action-body {
  display: grid;
  gap: 0.8rem;
}

.approval-queue__item {
  display: grid;
  gap: 0.55rem;
  width: 100%;
  padding: 0.95rem 1rem;
  text-align: left;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.74);
  transition:
    transform var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.approval-queue__item:hover {
  transform: translateY(-1px);
  border-color: rgba(156, 113, 71, 0.24);
}

.approval-queue__item--active {
  border-color: rgba(156, 113, 71, 0.32);
  box-shadow: 0 14px 30px rgba(112, 84, 55, 0.12);
}

.approval-queue__item-head,
.approval-queue__action-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.approval-queue__item-title,
.approval-queue__item-meta,
.approval-queue__item-detail,
.approval-queue__action-title,
.approval-queue__action-meta,
.approval-queue__feedback,
.approval-queue__empty,
.approval-queue__field-label {
  margin: 0;
}

.approval-queue__item-title,
.approval-queue__action-title {
  color: var(--color-ink-strong);
  font-size: 1.02rem;
}

.approval-queue__item-meta,
.approval-queue__item-detail,
.approval-queue__action-meta,
.approval-queue__feedback,
.approval-queue__empty,
.approval-queue__fact dt {
  color: var(--color-ink-soft);
}

.approval-queue__action-card {
  padding: 1rem;
  border-radius: var(--radius-xl);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background:
    radial-gradient(circle at top right, rgba(198, 172, 140, 0.12), transparent 36%),
    rgba(255, 252, 248, 0.92);
}

.approval-queue__facts {
  display: grid;
  gap: 0.7rem;
  margin: 0;
}

.approval-queue__fact {
  display: grid;
  gap: 0.28rem;
}

.approval-queue__fact dt {
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.approval-queue__fact dd {
  margin: 0;
  color: var(--color-ink-strong);
  line-height: 1.65;
}

.approval-queue__field {
  display: grid;
  gap: 0.5rem;
}

.approval-queue__field-label {
  font-size: 0.8rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-ink-faint);
}

.approval-queue__textarea,
.approval-queue__input {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.88);
  color: var(--color-ink-strong);
  padding: 0.85rem 1rem;
  font: inherit;
}

.approval-queue__textarea {
  resize: vertical;
  min-height: 7.2rem;
}

.approval-queue__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.approval-queue__action {
  min-height: 2.9rem;
  padding: 0 1.15rem;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.86);
  color: var(--color-ink-strong);
  font: inherit;
  font-weight: 600;
}

.approval-queue__action--primary {
  border: 0;
  background: linear-gradient(135deg, rgba(59, 117, 86, 0.98), rgba(89, 148, 115, 0.92));
  color: #fffaf4;
}

.approval-queue__action--warn {
  border-color: rgba(183, 117, 47, 0.18);
  color: var(--color-warning);
}

@media (max-width: 1080px) {
  .approval-queue {
    grid-template-columns: 1fr;
  }
}
</style>
