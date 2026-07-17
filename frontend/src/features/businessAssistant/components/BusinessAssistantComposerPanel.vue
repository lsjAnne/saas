<script setup lang="ts">
import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { Store } from '@/services/apiTypes';

interface SnapshotView {
  recommendationCount: number;
  orderRiskCount: number;
  blockedFulfillmentCount: number;
  lowStockCount: number;
  activeCampaignCount: number;
  dormantMemberCount: number;
}

interface Props {
  stores: Store[];
  activeStoreId: string;
  selectedStoreName: string;
  draftQuestion: string;
  recommendedQuestions: string[];
  snapshot: SnapshotView;
  isLoading: boolean;
  isAnswering: boolean;
  overviewHeadline: string | null;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  selectStore: [storeId: string];
  updateDraftQuestion: [value: string];
  askQuestion: [question: string];
  pickQuestion: [question: string];
}>();

function handleSubmit() {
  emit('askQuestion', props.draftQuestion);
}

function handleInput(event: Event) {
  const target = event.target as HTMLTextAreaElement;
  emit('updateDraftQuestion', target.value);
}
</script>

<template>
  <PanelCard
    eyebrow="Natural Language Assistant"
    title="把复杂经营动作压缩成一句话提问"
    :description="
      overviewHeadline ||
      `当前范围是 ${selectedStoreName}，可以直接提商品、订单、库存、活动、会员或风险问题，助手会回读真实业务数据再给建议。`
    "
    dark
  >
    <div class="assistant-composer">
      <div class="assistant-composer__toolbar">
        <div class="assistant-composer__scope">
          <p class="assistant-composer__scope-label">当前分析范围</p>
          <h4 class="assistant-composer__scope-value">{{ selectedStoreName }}</h4>
        </div>
        <StatusPill :label="isLoading ? 'syncing' : 'ready'" :tone="isLoading ? 'warn' : 'success'" />
      </div>

      <div class="assistant-composer__stores">
        <button
          type="button"
          :class="[
            'assistant-composer__store-button',
            { 'assistant-composer__store-button--active': activeStoreId === 'all' }
          ]"
          @click="emit('selectStore', 'all')"
        >
          全部店铺
        </button>
        <button
          v-for="store in stores"
          :key="store.storeId"
          type="button"
          :class="[
            'assistant-composer__store-button',
            { 'assistant-composer__store-button--active': activeStoreId === store.storeId }
          ]"
          @click="emit('selectStore', store.storeId)"
        >
          {{ store.shopName }}
        </button>
      </div>

      <label class="assistant-composer__input-card">
        <span class="assistant-composer__input-label">输入问题</span>
        <textarea
          class="assistant-composer__textarea"
          :value="draftQuestion"
          :disabled="isLoading || isAnswering"
          rows="5"
          placeholder="例如：今天卖什么最稳妥？哪些订单今天必须人工处理？这周要不要做活动？"
          @input="handleInput"
        />
        <div class="assistant-composer__input-actions">
          <p class="assistant-composer__hint">
            当前会基于推荐、订单、异常、库存、活动和会员数据生成回答，并给出下一步动作与追问方向。
          </p>
          <button
            type="button"
            class="assistant-composer__submit"
            :disabled="isLoading || isAnswering || !draftQuestion.trim()"
            @click="handleSubmit"
          >
            {{ isAnswering ? '分析中...' : '生成建议' }}
          </button>
        </div>
      </label>

      <div class="assistant-composer__question-bank">
        <p class="assistant-composer__section-title">推荐问题</p>
        <div class="assistant-composer__question-list">
          <button
            v-for="question in recommendedQuestions"
            :key="question"
            type="button"
            class="assistant-composer__question-chip"
            :disabled="isLoading || isAnswering"
            @click="emit('pickQuestion', question)"
          >
            {{ question }}
          </button>
        </div>
      </div>

      <div class="assistant-composer__metrics">
        <MetricCard
          label="总建议量"
          :value="`${snapshot.recommendationCount}`"
          caption="商品、会员、活动建议合并后的总量"
        />
        <MetricCard
          label="订单风险"
          :value="`${snapshot.orderRiskCount}`"
          caption="异常中心里与当前范围相关的风险任务"
        />
        <MetricCard
          label="履约阻塞"
          :value="`${snapshot.blockedFulfillmentCount}`"
          caption="可能影响自动发货的阻塞任务"
        />
        <MetricCard
          label="低库存"
          :value="`${snapshot.lowStockCount}`"
          caption="已经压到安全库存线的 SKU 数量"
        />
        <MetricCard
          label="进行中活动"
          :value="`${snapshot.activeCampaignCount}`"
          caption="已经进入执行或待审批状态的活动"
        />
        <MetricCard
          label="沉默会员"
          :value="`${snapshot.dormantMemberCount}`"
          caption="仍可承接召回或活动触达的人群"
        />
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.assistant-composer {
  display: grid;
  gap: 1rem;
}

.assistant-composer__toolbar,
.assistant-composer__input-actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.assistant-composer__scope,
.assistant-composer__input-card,
.assistant-composer__question-chip {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 244, 236, 0.1);
  background: rgba(255, 248, 240, 0.08);
}

.assistant-composer__scope,
.assistant-composer__input-card {
  padding: 1rem;
}

.assistant-composer__scope-label,
.assistant-composer__scope-value,
.assistant-composer__hint,
.assistant-composer__section-title,
.assistant-composer__input-label {
  margin: 0;
}

.assistant-composer__scope-label,
.assistant-composer__section-title,
.assistant-composer__input-label {
  color: rgba(255, 244, 236, 0.72);
  font-size: 0.76rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.assistant-composer__scope-value {
  color: #fff8f0;
  font-family: var(--font-display);
  font-size: 1.8rem;
}

.assistant-composer__stores,
.assistant-composer__question-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.assistant-composer__store-button,
.assistant-composer__question-chip,
.assistant-composer__submit {
  min-height: 2.75rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
  font: inherit;
}

.assistant-composer__store-button,
.assistant-composer__submit {
  border: 1px solid rgba(255, 244, 236, 0.12);
}

.assistant-composer__store-button {
  background: rgba(255, 255, 255, 0.08);
  color: #fff8f0;
}

.assistant-composer__store-button--active {
  background: linear-gradient(135deg, rgba(117, 82, 59, 0.96), rgba(36, 25, 18, 0.96));
  border-color: transparent;
}

.assistant-composer__input-card {
  display: grid;
  gap: 0.85rem;
}

.assistant-composer__textarea {
  width: 100%;
  min-height: 8rem;
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 244, 236, 0.12);
  background: rgba(28, 20, 15, 0.65);
  color: #fff8f0;
  font: inherit;
  line-height: 1.7;
  resize: vertical;
}

.assistant-composer__textarea::placeholder,
.assistant-composer__hint {
  color: rgba(255, 244, 236, 0.68);
}

.assistant-composer__submit {
  border: 0;
  background: linear-gradient(135deg, rgba(255, 248, 240, 0.96), rgba(229, 206, 181, 0.96));
  color: var(--color-ink-strong);
  font-weight: 600;
}

.assistant-composer__submit:disabled,
.assistant-composer__question-chip:disabled,
.assistant-composer__store-button:disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

.assistant-composer__question-chip {
  border: 0;
  text-align: left;
  color: #fff8f0;
}

.assistant-composer__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

@media (max-width: 1180px) {
  .assistant-composer__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .assistant-composer__toolbar,
  .assistant-composer__input-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .assistant-composer__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .assistant-composer__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
