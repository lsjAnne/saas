<script setup lang="ts">
import { computed, reactive } from 'vue';
import type { DigitalHumanFaq } from '@/features/digitalHuman/types';

const props = defineProps<{
  faqs: DigitalHumanFaq[];
  organizationId: string;
  isLoading: boolean;
  isRunningAction: boolean;
}>();

const emit = defineEmits<{
  (e: 'create', payload: Record<string, unknown>): void;
  (e: 'remove', faqId: string): void;
  (e: 'toggle', faq: DigitalHumanFaq): void;
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

const form = reactive({
  productName: '',
  questionType: 'price',
  keywords: '',
  question: '',
  answer: '',
});

const typeLabel = (type: string) =>
  QUESTION_TYPES.find((t) => t.value === type)?.label ?? type;

const groupedFaqs = computed(() => {
  const groups = new Map<string, DigitalHumanFaq[]>();
  for (const faq of props.faqs) {
    const key = faq.productName && faq.productName.trim() ? faq.productName : '通用问题';
    if (!groups.has(key)) {
      groups.set(key, []);
    }
    groups.get(key)!.push(faq);
  }
  return [...groups.entries()];
});

function canSubmit() {
  return form.question.trim() && form.answer.trim() && !props.isRunningAction;
}

function handleCreate() {
  if (!canSubmit()) {
    return;
  }
  emit('create', {
    organizationId: props.organizationId,
    productId: null,
    productName: form.productName.trim() || null,
    questionType: form.questionType,
    keywords: form.keywords.trim() || null,
    question: form.question.trim(),
    answer: form.answer.trim(),
    enabled: true,
  });
  form.question = '';
  form.answer = '';
  form.keywords = '';
}
</script>

<template>
  <section class="dh-panel">
    <div class="dh-panel__head">
      <h2 class="dh-panel__title">问答知识库</h2>
      <p class="dh-panel__subline">
        按商品与问题类型维护标准答案，数字人"不同产品、不同问题对应不同回答"。
      </p>
    </div>

    <div class="dh-library__form dh-library__form--stack">
      <div class="dh-library__row">
        <input
          v-model="form.productName"
          class="dh-input"
          placeholder="商品名称，如 无线蓝牙耳机（留空为通用问题）"
        />
        <select v-model="form.questionType" class="dh-select">
          <option v-for="t in QUESTION_TYPES" :key="t.value" :value="t.value">
            {{ t.label }}
          </option>
        </select>
      </div>
      <input
        v-model="form.keywords"
        class="dh-input"
        placeholder="触发关键词，逗号分隔，如：多少钱,价格,售价"
      />
      <input
        v-model="form.question"
        class="dh-input"
        placeholder="观众可能的问法，如：这个无线蓝牙耳机多少钱"
      />
      <textarea
        v-model="form.answer"
        class="dh-textarea"
        placeholder="标准回答，如：直播间到手价 ¥169，还送收纳包～"
      ></textarea>
      <button
        class="dh-btn dh-btn--primary"
        :disabled="!canSubmit()"
        @click="handleCreate"
      >
        加入知识库
      </button>
    </div>

    <div v-if="props.isLoading" class="dh-empty">加载中…</div>
    <div v-else-if="props.faqs.length === 0" class="dh-empty">
      还没有问答知识，先在上方添加一条。添加后，观众问同类问题数字人会直接作答。
    </div>

    <div v-else class="dh-list">
      <div
        v-for="[group, items] in groupedFaqs"
        :key="group"
        class="dh-faq-group"
      >
        <div class="dh-faq-group__title">
          {{ group }}
          <span class="dh-tag">{{ items.length }} 条</span>
        </div>
        <ul class="dh-list dh-faq-group__list">
          <li
            v-for="faq in items"
            :key="faq.faqId"
            class="dh-list__item dh-list__item--stack"
            :class="{ 'dh-faq-item--off': !faq.enabled }"
          >
            <div class="dh-list__main">
              <span class="dh-list__name">{{ faq.question }}</span>
              <span class="dh-tag">{{ typeLabel(faq.questionType) }}</span>
              <span class="dh-list__sub">命中 {{ faq.hitCount }} 次</span>
            </div>
            <p class="dh-list__desc">{{ faq.answer }}</p>
            <div class="dh-list__actions">
              <span class="dh-tag">{{ faq.enabled ? '启用中' : '已停用' }}</span>
              <button
                class="dh-btn dh-btn--ghost"
                :disabled="props.isRunningAction"
                @click="emit('toggle', faq)"
              >
                {{ faq.enabled ? '停用' : '启用' }}
              </button>
              <button
                class="dh-btn dh-btn--danger"
                :disabled="props.isRunningAction"
                @click="emit('remove', faq.faqId)"
              >
                删除
              </button>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<style>
.dh-faq-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.dh-faq-group__title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-ink-strong);
  display: flex;
  align-items: center;
  gap: 8px;
}
.dh-faq-group__list {
  gap: 8px;
}
.dh-faq-item--off {
  opacity: 0.6;
}
</style>
