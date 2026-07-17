<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { GovernanceApprovalRequestView } from '@/services/apiTypes';
import { formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  requests: GovernanceApprovalRequestView[];
}

defineProps<Props>();

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
    eyebrow="Governance Requests"
    title="治理审批请求"
    description="把制度化审批请求单独拉出来，方便识别文档号、金额、当前阶段与责任人。"
  >
    <div v-if="requests.length" class="governance-requests">
      <article
        v-for="request in requests"
        :key="request.requestId"
        class="governance-requests__item"
      >
        <div class="governance-requests__head">
          <div>
            <p class="governance-requests__subject">{{ request.subject }}</p>
            <p class="governance-requests__meta">
              {{ request.documentNo }} · {{ request.templateCode }} · {{ request.counterparty }}
            </p>
          </div>
          <StatusPill :label="request.requestStatus" :tone="toneFromStatus(request.requestStatus)" />
        </div>

        <div class="governance-requests__facts">
          <span>金额 {{ formatCurrency(request.amount) }}</span>
          <span>阶段 {{ request.currentStageCode || '--' }} / {{ request.currentStageOrder ?? '--' }}</span>
          <span>处理人 {{ request.currentHandlerId || '--' }}</span>
          <span>发起人 {{ request.requesterId }}</span>
          <span>创建于 {{ formatDateTime(request.createdAt) }}</span>
        </div>

        <p class="governance-requests__remark">
          {{ request.remark || '当前没有补充备注。' }}
        </p>
      </article>
    </div>

    <p v-else class="governance-requests__empty">
      当前没有治理审批请求。
    </p>
  </PanelCard>
</template>

<style scoped>
.governance-requests {
  display: grid;
  gap: 0.85rem;
}

.governance-requests__item {
  display: grid;
  gap: 0.7rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(249, 243, 235, 0.76));
}

.governance-requests__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.governance-requests__subject,
.governance-requests__meta,
.governance-requests__remark,
.governance-requests__empty {
  margin: 0;
}

.governance-requests__subject {
  color: var(--color-ink-strong);
  font-size: 1.04rem;
}

.governance-requests__meta,
.governance-requests__facts,
.governance-requests__remark,
.governance-requests__empty {
  color: var(--color-ink-soft);
}

.governance-requests__meta {
  margin-top: 0.3rem;
}

.governance-requests__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.45rem 0.8rem;
  font-size: 0.92rem;
}

.governance-requests__remark {
  line-height: 1.65;
}

@media (max-width: 720px) {
  .governance-requests__head,
  .governance-requests__facts {
    grid-template-columns: 1fr;
  }

  .governance-requests__head {
    display: grid;
  }
}
</style>
