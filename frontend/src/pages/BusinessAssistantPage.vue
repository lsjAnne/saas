<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import BusinessAssistantComposerPanel from '@/features/businessAssistant/components/BusinessAssistantComposerPanel.vue';
import BusinessAssistantHistoryPanel from '@/features/businessAssistant/components/BusinessAssistantHistoryPanel.vue';
import BusinessAssistantResponsePanel from '@/features/businessAssistant/components/BusinessAssistantResponsePanel.vue';
import { useBusinessAssistantCopilot } from '@/features/businessAssistant/composables/useBusinessAssistantCopilot';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const router = useRouter();
const businessAssistant = useBusinessAssistantCopilot();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await businessAssistant.bootstrap(authStore.token);
}

async function handleSelectStore(storeId: string) {
  if (!authStore.token) {
    return;
  }

  await businessAssistant.selectStore(storeId, authStore.token);
}

function handleUpdateDraftQuestion(value: string) {
  businessAssistant.updateDraftQuestion(value);
}

async function handleAskQuestion(question: string) {
  if (!authStore.token) {
    return;
  }

  await businessAssistant.askQuestion(question, authStore.token);
}

async function handlePickQuestion(question: string) {
  businessAssistant.updateDraftQuestion(question);
  await handleAskQuestion(question);
}

async function handlePickFollowUp(question: string) {
  businessAssistant.updateDraftQuestion(question);
  await handleAskQuestion(question);
}

async function handleReplayQuestion(question: string) {
  businessAssistant.replayHistory(question);
  await handleAskQuestion(question);
}

function handleOpenRoute(route: string) {
  void router.push(route);
}

onMounted(() => {
  void bootstrapPage();
});
</script>

<template>
  <section class="business-assistant-page">
    <header class="business-assistant-page__header">
      <p class="business-assistant-page__eyebrow">Business Assistant</p>
      <h2 class="business-assistant-page__title">经营助手已升级成通用经营问答工作台</h2>
      <p class="business-assistant-page__subtitle">
        这里不再只是推荐聚合页，而是把商品、订单、风险、库存、活动和会员数据压缩成一句话提问后的经营建议。
      </p>
    </header>

    <InlineErrorCard
      v-if="businessAssistant.errorMessage"
      :message="businessAssistant.errorMessage"
      @retry="bootstrapPage"
    />

    <div class="business-assistant-page__layout">
      <BusinessAssistantComposerPanel
        :stores="businessAssistant.stores"
        :active-store-id="businessAssistant.activeStoreId"
        :selected-store-name="businessAssistant.selectedStoreName"
        :draft-question="businessAssistant.draftQuestion"
        :recommended-questions="businessAssistant.recommendedQuestions"
        :snapshot="businessAssistant.snapshot"
        :is-loading="businessAssistant.isBootstrapping || businessAssistant.isRefreshingCore"
        :is-answering="businessAssistant.isAnswering"
        :overview-headline="businessAssistant.overview?.headline ?? null"
        @select-store="handleSelectStore"
        @update-draft-question="handleUpdateDraftQuestion"
        @ask-question="handleAskQuestion"
        @pick-question="handlePickQuestion"
      />

      <div class="business-assistant-page__stack">
        <BusinessAssistantResponsePanel
          :response="businessAssistant.currentResponse"
          :is-loading="businessAssistant.isBootstrapping || businessAssistant.isRefreshingCore || businessAssistant.isAnswering"
          :selected-store-name="businessAssistant.selectedStoreName"
          @open-route="handleOpenRoute"
          @pick-follow-up="handlePickFollowUp"
        />
        <BusinessAssistantHistoryPanel
          :history="businessAssistant.history"
          @replay-question="handleReplayQuestion"
        />
      </div>
    </div>

    <p
      v-if="businessAssistant.isBootstrapping || businessAssistant.isRefreshingCore"
      class="business-assistant-page__footer-note"
    >
      正在同步推荐概览、店铺、会员、订单、库存与活动等必要经营数据...
    </p>
  </section>
</template>

<style scoped>
.business-assistant-page {
  display: grid;
  gap: 1.2rem;
}

.business-assistant-page__header {
  display: grid;
  gap: 0.75rem;
}

.business-assistant-page__eyebrow,
.business-assistant-page__title,
.business-assistant-page__subtitle,
.business-assistant-page__footer-note {
  margin: 0;
}

.business-assistant-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.business-assistant-page__title {
  max-width: 14ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 3.6rem);
  line-height: 1.04;
}

.business-assistant-page__subtitle,
.business-assistant-page__footer-note {
  max-width: 60rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.business-assistant-page__layout {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(0, 0.98fr);
  gap: 1rem;
  align-items: start;
}

.business-assistant-page__stack {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1180px) {
  .business-assistant-page__layout {
    grid-template-columns: 1fr;
  }
}
</style>
