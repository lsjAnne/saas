<script setup lang="ts">
import type { DigitalHumanLearningItem, LearningGrowth } from '@/features/digitalHuman/types';

const props = defineProps<{
  growth: LearningGrowth | null;
  items: DigitalHumanLearningItem[];
  isLoading: boolean;
  isRunningAction: boolean;
}>();

const emit = defineEmits<{
  (e: 'confirm', itemId: string): void;
  (e: 'ignore', itemId: string): void;
}>();

const QUESTION_TYPES: { value: string; label: string }[] = [
  { value: 'price', label: '价格' },
  { value: 'size', label: '尺码' },
  { value: 'material', label: '材质' },
  { value: 'shipping', label: '发货物流' },
  { value: 'after_sale', label: '售后' },
  { value: 'discount', label: '优惠' },
  { value: 'usage', label: '使用' },
  { value: 'generic', label: '通用' },
];

const STATUS_META: Record<string, { label: string; css: string }> = {
  new: { label: '已记住', css: 'dh-status--remember' },
  learning: { label: '正在学习', css: 'dh-status--learning' },
  confirmed: { label: '已入库', css: 'dh-status--on' },
  ignored: { label: '已忽略', css: 'dh-status--off' },
};

function typeLabel(type: string) {
  return QUESTION_TYPES.find((t) => t.value === type)?.label ?? type;
}

function statusMeta(status: string) {
  return STATUS_META[status] ?? { label: status, css: 'dh-status--off' };
}

function canAct(item: DigitalHumanLearningItem) {
  return (item.status === 'new' || item.status === 'learning') && !props.isRunningAction;
}
</script>

<template>
  <section class="dh-panel dh-learning">
    <header class="dh-panel__head">
      <h2 class="dh-panel__title">成长与记忆</h2>
      <p class="dh-panel__subline">
        答不上的问题会被记住，同类问题问得多了自动沉淀进知识库——数字人越用越懂。
      </p>
    </header>

    <div v-if="props.growth" class="dh-learning__stats">
      <div class="dh-learning__stat">
        <b>{{ props.growth.faqCount }}</b>
        <span>知识库条目</span>
      </div>
      <div class="dh-learning__stat dh-learning__stat--accent">
        <b>{{ props.growth.autoLearnedCount }}</b>
        <span>自动学习入库</span>
      </div>
      <div class="dh-learning__stat">
        <b>{{ props.growth.memoryTotal }}</b>
        <span>正在记忆</span>
      </div>
      <div class="dh-learning__stat dh-learning__stat--accent">
        <b>{{ props.growth.conversionRate }}%</b>
        <span>记忆转化率</span>
      </div>
    </div>

    <div v-if="props.growth && props.growth.hotUnanswered.length" class="dh-learning__hot">
      <span class="dh-learning__hot-label">高频待答：</span>
      <span
        v-for="(q, idx) in props.growth.hotUnanswered.slice(0, 3)"
        :key="q.itemId"
        class="dh-tag dh-tag--hot"
      >
        {{ q.productName ? `${q.productName}·` : '' }}{{ q.question }}（{{ q.occurrenceCount }}）
      </span>
    </div>

    <div v-if="props.items.length" class="dh-learning__list">
      <div
        v-for="item in props.items"
        :key="item.itemId"
        class="dh-learning__row"
        :class="{ 'dh-learning__row--muted': item.status === 'ignored' }"
      >
        <div class="dh-learning__row-main">
          <span class="dh-list__name">{{ item.question }}</span>
          <span class="dh-tag">{{ typeLabel(item.questionType) }}</span>
          <span v-if="item.productName" class="dh-tag">{{ item.productName }}</span>
          <span class="dh-learning__count">×{{ item.occurrenceCount }}</span>
          <span class="dh-status" :class="statusMeta(item.status).css">{{ statusMeta(item.status).label }}</span>
        </div>
        <div v-if="canAct(item)" class="dh-list__actions">
          <button class="dh-btn dh-btn--primary" :disabled="props.isRunningAction" @click="emit('confirm', item.itemId)">
            转正入库
          </button>
          <button class="dh-btn dh-btn--danger" :disabled="props.isRunningAction" @click="emit('ignore', item.itemId)">
            忽略
          </button>
        </div>
      </div>
    </div>

    <div v-else-if="!props.isLoading" class="dh-empty">
      还没有未答问题。发一条知识库覆盖不到的弹幕，数字人会把它记下来并自动学习。
    </div>
  </section>
</template>

<style scoped>
.dh-learning__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}
.dh-learning__stat {
  background: var(--color-panel-strong);
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.dh-learning__stat b {
  font-size: 1.35rem;
  font-weight: 600;
  color: var(--color-ink-strong);
}
.dh-learning__stat--accent b {
  color: var(--color-accent);
}
.dh-learning__stat span {
  font-size: 0.75rem;
  color: var(--color-ink-soft);
}
.dh-learning__hot {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.dh-learning__hot-label {
  font-size: 0.8125rem;
  color: var(--color-ink-muted);
}
.dh-tag--hot {
  color: #b45309;
  background: #fef3c7;
  border-color: #fde68a;
}
.dh-learning__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.dh-learning__row {
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  background: var(--color-panel-strong);
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.dh-learning__row--muted {
  opacity: 0.55;
}
.dh-learning__row-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.dh-learning__count {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-ink-strong);
}
.dh-status--remember {
  color: #6b7280;
  background: #f3f4f6;
}
.dh-status--learning {
  color: #b45309;
  background: #fef3c7;
}
@media (max-width: 720px) {
  .dh-learning__stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
