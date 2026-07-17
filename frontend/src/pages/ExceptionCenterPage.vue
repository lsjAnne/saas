<script setup lang="ts">
import { onMounted, reactive } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import ExceptionCategoryPanel from '@/features/exceptionCenter/components/ExceptionCategoryPanel.vue';
import ExceptionCenterHeroPanel from '@/features/exceptionCenter/components/ExceptionCenterHeroPanel.vue';
import ExceptionDetailPanel from '@/features/exceptionCenter/components/ExceptionDetailPanel.vue';
import ExceptionQueuePanel from '@/features/exceptionCenter/components/ExceptionQueuePanel.vue';
import { useExceptionCenterOverview } from '@/features/exceptionCenter/composables/useExceptionCenterOverview';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const exceptionCenter = reactive(useExceptionCenterOverview());

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await exceptionCenter.load(authStore.token);
}

async function handleSelectCategory(categoryCode: string) {
  exceptionCenter.selectCategory(categoryCode);

  if (!authStore.token || !exceptionCenter.selectedExceptionId) {
    return;
  }

  await exceptionCenter.selectException(exceptionCenter.selectedExceptionId, authStore.token);
}

async function handleSelectException(exceptionTaskId: string) {
  if (!authStore.token) {
    return;
  }

  await exceptionCenter.selectException(exceptionTaskId, authStore.token);
}

async function handleProcess() {
  if (!authStore.token) {
    return;
  }

  await exceptionCenter.processSelected(authStore.token, authStore.user?.id);
}

async function handleIgnore() {
  if (!authStore.token) {
    return;
  }

  await exceptionCenter.ignoreSelected(authStore.token, authStore.user?.id);
}

async function handleEscalate() {
  if (!authStore.token) {
    return;
  }

  await exceptionCenter.escalateSelected(authStore.token, authStore.user?.id);
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="exception-center-page">
    <header class="exception-center-page__header">
      <p class="exception-center-page__eyebrow">Exception Center</p>
      <h2 class="exception-center-page__title">
        把异常分类、推荐 SOP 和处理动作收进同一个风险处置中心
      </h2>
      <p class="exception-center-page__subtitle">
        这页不是简单告警墙，而是围绕异常分类导航、详情回读和动作留痕形成一条真正可执行的异常处理链路。
      </p>
    </header>

    <InlineErrorCard
      v-if="exceptionCenter.errorMessage"
      :message="exceptionCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <ExceptionCenterHeroPanel
      :summary="exceptionCenter.summary"
      :selected-exception="exceptionCenter.selectedException"
      :action-feedback="exceptionCenter.actionFeedback"
    />

    <div class="exception-center-page__grid">
      <div class="exception-center-page__stack">
        <ExceptionCategoryPanel
          :categories="exceptionCenter.categories"
          :active-category-code="exceptionCenter.activeCategoryCode"
          @select="handleSelectCategory"
        />
        <ExceptionQueuePanel
          :exceptions="exceptionCenter.exceptions"
          :selected-exception-id="exceptionCenter.selectedExceptionId"
          @select="handleSelectException"
        />
      </div>

      <ExceptionDetailPanel
        :selected-exception="exceptionCenter.selectedException"
        :suggested-sop="exceptionCenter.suggestedSop"
        :action-choice="exceptionCenter.actionChoice"
        :action-remark="exceptionCenter.actionRemark"
        :action-feedback="exceptionCenter.actionFeedback"
        :is-running-action="exceptionCenter.isRunningAction"
        :can-process-selected="exceptionCenter.canProcessSelected"
        :can-ignore-selected="exceptionCenter.canIgnoreSelected"
        :can-escalate-selected="exceptionCenter.canEscalateSelected"
        @update-action-choice="exceptionCenter.actionChoice = $event"
        @update-action-remark="exceptionCenter.actionRemark = $event"
        @process="handleProcess"
        @ignore="handleIgnore"
        @escalate="handleEscalate"
      />
    </div>

    <p v-if="exceptionCenter.isLoading" class="exception-center-page__footer-note">
      正在回读异常主队列、详情与推荐 SOP...
    </p>
  </section>
</template>

<style scoped>
.exception-center-page {
  display: grid;
  gap: 1.2rem;
}

.exception-center-page__header {
  display: grid;
  gap: 0.75rem;
}

.exception-center-page__eyebrow,
.exception-center-page__title,
.exception-center-page__subtitle,
.exception-center-page__footer-note {
  margin: 0;
}

.exception-center-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.exception-center-page__title {
  max-width: 16ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.15rem, 3.8vw, 3.7rem);
  line-height: 1.05;
}

.exception-center-page__subtitle,
.exception-center-page__footer-note {
  max-width: 62rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.exception-center-page__grid {
  display: grid;
  grid-template-columns: 0.94fr 1.06fr;
  gap: 1rem;
  align-items: start;
}

.exception-center-page__stack {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1180px) {
  .exception-center-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
