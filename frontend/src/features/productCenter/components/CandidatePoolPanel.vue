<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { CandidateProduct } from '@/services/apiTypes';
import { formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  candidates: CandidateProduct[];
  selectedCandidate: CandidateProduct | null;
  selectedCandidateId: string | null;
  isRunningAction: boolean;
}

defineProps<Props>();

defineEmits<{
  select: [candidateProductId: string];
  openCreate: [];
  openEdit: [];
  openGenerate: [];
  changeStatus: [status: string];
}>();

function resolveTone(value: string | null | undefined) {
  const normalized = (value ?? '').toLowerCase();

  if (normalized.includes('pool') || normalized.includes('testable')) {
    return 'success';
  }

  if (normalized.includes('risk') || normalized.includes('not')) {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Candidate Pool"
    title="候选商品池"
    description="先判断该不该做，再决定怎么写。这里聚焦候选来源、利润预估、风险等级和推荐理由。"
  >
    <div class="candidate-pool">
      <div class="candidate-pool__rail">
        <div class="candidate-pool__rail-actions">
          <button type="button" class="candidate-pool__primary" @click="$emit('openCreate')">
            新建候选
          </button>
        </div>

        <button
          v-for="candidate in candidates"
          :key="candidate.candidateProductId"
          type="button"
          :class="[
            'candidate-pool__item',
            { 'candidate-pool__item--active': candidate.candidateProductId === selectedCandidateId }
          ]"
          @click="$emit('select', candidate.candidateProductId)"
        >
          <div class="candidate-pool__item-copy">
            <strong class="candidate-pool__item-title">{{ candidate.title }}</strong>
            <span class="candidate-pool__item-meta">
              {{ candidate.sourceType }} / {{ candidate.category || '未分类' }}
            </span>
          </div>
          <StatusPill :label="candidate.status" :tone="resolveTone(candidate.status)" />
        </button>

        <p v-if="!candidates.length" class="candidate-pool__empty">
          还没有候选商品。先新建一个来源条目，后面草稿区才有可推进对象。
        </p>
      </div>

      <div class="candidate-pool__detail">
        <template v-if="selectedCandidate">
          <div class="candidate-pool__detail-head">
            <div>
              <p class="candidate-pool__label">Selected Candidate</p>
              <h4 class="candidate-pool__detail-title">{{ selectedCandidate.title }}</h4>
            </div>
            <div class="candidate-pool__detail-actions">
              <button type="button" class="candidate-pool__ghost" @click="$emit('openEdit')">
                编辑候选
              </button>
              <button type="button" class="candidate-pool__primary" @click="$emit('openGenerate')">
                生成草稿
              </button>
            </div>
          </div>

          <dl class="candidate-pool__facts">
            <div class="candidate-pool__fact">
              <dt>利润预估</dt>
              <dd>{{ formatCurrency(selectedCandidate.estimatedProfit) }}</dd>
            </div>
            <div class="candidate-pool__fact">
              <dt>风险等级</dt>
              <dd>{{ selectedCandidate.riskLevel || '--' }}</dd>
            </div>
            <div class="candidate-pool__fact">
              <dt>来源</dt>
              <dd>{{ selectedCandidate.sourceType }}</dd>
            </div>
            <div class="candidate-pool__fact">
              <dt>进入时间</dt>
              <dd>{{ formatDateTime(selectedCandidate.createdAt) }}</dd>
            </div>
          </dl>

          <div class="candidate-pool__copy-block">
            <p class="candidate-pool__label">推荐理由</p>
            <p class="candidate-pool__body">{{ selectedCandidate.recommendationReason || '暂无推荐理由' }}</p>
          </div>

          <div class="candidate-pool__copy-block">
            <p class="candidate-pool__label">AI 摘要</p>
            <p class="candidate-pool__body">{{ selectedCandidate.aiSummary || '暂无 AI 摘要' }}</p>
          </div>

          <div class="candidate-pool__status-bar">
            <button
              type="button"
              class="candidate-pool__status-btn"
              :disabled="isRunningAction"
              @click="$emit('changeStatus', 'testable')"
            >
              标记可测
            </button>
            <button
              type="button"
              class="candidate-pool__status-btn"
              :disabled="isRunningAction"
              @click="$emit('changeStatus', 'in_pool')"
            >
              进入池中
            </button>
            <button
              type="button"
              class="candidate-pool__status-btn candidate-pool__status-btn--warn"
              :disabled="isRunningAction"
              @click="$emit('changeStatus', 'not_recommended')"
            >
              标记不推荐
            </button>
          </div>
        </template>

        <p v-else class="candidate-pool__empty">
          先从左侧选择一个候选商品，再决定是否推进到草稿。
        </p>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.candidate-pool {
  display: grid;
  grid-template-columns: 0.94fr 1.06fr;
  gap: 0.95rem;
}

.candidate-pool__rail,
.candidate-pool__detail,
.candidate-pool__copy-block {
  display: grid;
  gap: 0.8rem;
}

.candidate-pool__rail-actions,
.candidate-pool__detail-head,
.candidate-pool__detail-actions,
.candidate-pool__status-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  justify-content: space-between;
}

.candidate-pool__primary,
.candidate-pool__ghost,
.candidate-pool__status-btn {
  min-height: 2.9rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.candidate-pool__primary {
  border: 0;
  background: linear-gradient(135deg, #2e251d, #856243);
  color: #fff8f0;
}

.candidate-pool__ghost,
.candidate-pool__status-btn {
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-ink);
}

.candidate-pool__status-btn--warn {
  color: var(--color-warning);
}

.candidate-pool__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.74);
  text-align: left;
}

.candidate-pool__item--active {
  border-color: rgba(133, 98, 67, 0.34);
  background: linear-gradient(135deg, rgba(250, 242, 233, 0.94), rgba(245, 232, 213, 0.88));
}

.candidate-pool__item-copy,
.candidate-pool__detail {
  display: grid;
  gap: 0.35rem;
}

.candidate-pool__item-title,
.candidate-pool__item-meta,
.candidate-pool__label,
.candidate-pool__detail-title,
.candidate-pool__body,
.candidate-pool__empty {
  margin: 0;
}

.candidate-pool__detail {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(248, 244, 238, 0.8);
}

.candidate-pool__item-title,
.candidate-pool__detail-title {
  color: var(--color-ink-strong);
}

.candidate-pool__item-meta,
.candidate-pool__label,
.candidate-pool__body,
.candidate-pool__empty {
  color: var(--color-ink-soft);
}

.candidate-pool__label {
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.candidate-pool__detail-title {
  margin-top: 0.35rem;
  font-family: var(--font-display);
  font-size: 1.55rem;
}

.candidate-pool__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
  margin: 0;
}

.candidate-pool__fact {
  display: grid;
  gap: 0.35rem;
  padding: 0.8rem 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.72);
}

.candidate-pool__fact dt {
  color: var(--color-ink-soft);
  font-size: 0.8rem;
}

.candidate-pool__fact dd {
  margin: 0;
  color: var(--color-ink-strong);
}

.candidate-pool__body,
.candidate-pool__empty {
  line-height: 1.7;
}

@media (max-width: 1120px) {
  .candidate-pool {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .candidate-pool__facts {
    grid-template-columns: 1fr;
  }
}
</style>
