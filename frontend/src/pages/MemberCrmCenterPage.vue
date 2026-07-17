<script setup lang="ts">
import { onMounted } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import MemberCrmHeroPanel from '@/features/memberCrmCenter/components/MemberCrmHeroPanel.vue';
import MemberPoolPanel from '@/features/memberCrmCenter/components/MemberPoolPanel.vue';
import MemberProfilePanel from '@/features/memberCrmCenter/components/MemberProfilePanel.vue';
import MemberSegmentPanel from '@/features/memberCrmCenter/components/MemberSegmentPanel.vue';
import MemberTouchTaskPanel from '@/features/memberCrmCenter/components/MemberTouchTaskPanel.vue';
import { useMemberCrmCenterOverview } from '@/features/memberCrmCenter/composables/useMemberCrmCenterOverview';
import type {
  SaveMemberCrmProfilePayload,
  SaveMemberSegmentRulePayload,
  SaveMemberTagPayload,
  SaveMemberTouchTaskPayload
} from '@/services/apiTypes';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const memberCrmCenter = useMemberCrmCenterOverview();

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await memberCrmCenter.load(authStore.token);
}

async function withToken<T>(callback: (token: string) => Promise<T>) {
  if (!authStore.token) {
    return;
  }

  await callback(authStore.token);
}

async function handleSelectStore(storeId: string) {
  await withToken((token) => memberCrmCenter.selectStore(storeId, token));
}

async function handleSelectLevel(levelCode: string) {
  await withToken((token) => memberCrmCenter.selectLevel(levelCode, token));
}

async function handleSelectLifecycle(stage: string) {
  await withToken((token) => memberCrmCenter.selectLifecycleStage(stage, token));
}

async function handleSelectTag(tagCode: string) {
  await withToken((token) => memberCrmCenter.selectTag(tagCode, token));
}

async function handleSelectMember(memberId: string) {
  await withToken((token) => memberCrmCenter.selectMember(memberId, token));
}

async function handleSubmitProfile(payload: SaveMemberCrmProfilePayload) {
  await withToken((token) => memberCrmCenter.submitCrmProfile(payload, token));
}

async function handleAddTag(payload: SaveMemberTagPayload) {
  await withToken((token) => memberCrmCenter.submitAddTag(payload, token));
}

async function handleRemoveTag(memberTagId: string) {
  await withToken((token) => memberCrmCenter.submitRemoveTag(memberTagId, token));
}

async function handleCreateRule(payload: SaveMemberSegmentRulePayload) {
  await withToken((token) => memberCrmCenter.submitSegmentRule(payload, token));
}

async function handleRunRule(ruleId: string) {
  await withToken((token) => memberCrmCenter.runSegmentRule(ruleId, token));
}

async function handleExportGroup() {
  await withToken((token) => memberCrmCenter.submitExportGroup(token));
}

async function handleCreateTouchTask(payload: SaveMemberTouchTaskPayload) {
  await withToken((token) => memberCrmCenter.submitTouchTask(payload, token));
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="member-crm-page">
    <header class="member-crm-page__header">
      <p class="member-crm-page__eyebrow">Member & CRM Center</p>
      <h2 class="member-crm-page__title">
        把会员池、CRM 档案、标签分群和召回动作收拢到同一张经营控制台
      </h2>
      <p class="member-crm-page__subtitle">
        这页不再让“组织与成员”代替 CRM 主入口。会员列表、CRM 档案、标签分群、触达任务和导出动作都直接接上真实后端。
      </p>
    </header>

    <InlineErrorCard
      v-if="memberCrmCenter.errorMessage"
      :message="memberCrmCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <MemberCrmHeroPanel
      :selected-member="memberCrmCenter.selectedMember"
      :selected-store-name="memberCrmCenter.selectedStoreName"
      :selected-crm-profile="memberCrmCenter.selectedCrmProfile"
      :selected-crm-linkage="memberCrmCenter.selectedCrmLinkage"
      :summary="memberCrmCenter.summary"
      :action-feedback="memberCrmCenter.actionFeedback"
    />

    <div class="member-crm-page__grid">
      <MemberPoolPanel
        :stores="memberCrmCenter.stores"
        :members="memberCrmCenter.memberList"
        :selected-member-id="memberCrmCenter.selectedMemberId"
        :active-store-id="memberCrmCenter.activeStoreId"
        :level-filter="memberCrmCenter.levelFilter"
        :lifecycle-filter="memberCrmCenter.lifecycleFilter"
        :tag-filter="memberCrmCenter.tagFilter"
        :available-levels="memberCrmCenter.availableLevels"
        :available-lifecycle-stages="memberCrmCenter.availableLifecycleStages"
        :member-tags="memberCrmCenter.memberTags"
        @select-store="handleSelectStore"
        @select-level="handleSelectLevel"
        @select-lifecycle="handleSelectLifecycle"
        @select-tag="handleSelectTag"
        @select-member="handleSelectMember"
      />

      <div class="member-crm-page__stack">
        <MemberProfilePanel
          :member="memberCrmCenter.selectedMember"
          :crm-profile="memberCrmCenter.selectedCrmProfile"
          :crm-linkage="memberCrmCenter.selectedCrmLinkage"
          :is-submitting="memberCrmCenter.isRunningAction"
          :action-error="memberCrmCenter.actionError"
          @submit-profile="handleSubmitProfile"
          @add-tag="handleAddTag"
          @remove-tag="handleRemoveTag"
        />

        <MemberSegmentPanel
          :stores="memberCrmCenter.stores"
          :active-store-id="memberCrmCenter.activeStoreId"
          :member-tags="memberCrmCenter.memberTags"
          :session-segment-rules="memberCrmCenter.sessionSegmentRules"
          :session-segment-executions="memberCrmCenter.sessionSegmentExecutions"
          :latest-export="memberCrmCenter.latestExport"
          :is-submitting="memberCrmCenter.isRunningAction"
          :action-error="memberCrmCenter.actionError"
          @create-rule="handleCreateRule"
          @run-rule="handleRunRule"
          @export-group="handleExportGroup"
        />

        <MemberTouchTaskPanel
          :member="memberCrmCenter.selectedMember"
          :touch-tasks="memberCrmCenter.selectedMemberTouchTasks"
          :published-campaign-options="memberCrmCenter.publishedCampaignOptions"
          :active-store-id="memberCrmCenter.activeStoreId"
          :is-submitting="memberCrmCenter.isRunningAction"
          :action-error="memberCrmCenter.actionError"
          @create-task="handleCreateTouchTask"
        />
      </div>
    </div>

    <p v-if="memberCrmCenter.isLoading" class="member-crm-page__footer-note">
      正在回读会员池、CRM 档案、标签统计和触达任务...
    </p>
  </section>
</template>

<style scoped>
.member-crm-page {
  display: grid;
  gap: 1.2rem;
}

.member-crm-page__header {
  display: grid;
  gap: 0.75rem;
}

.member-crm-page__eyebrow,
.member-crm-page__title,
.member-crm-page__subtitle,
.member-crm-page__footer-note {
  margin: 0;
}

.member-crm-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.member-crm-page__title {
  max-width: 15ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 3.8rem);
  line-height: 1.04;
}

.member-crm-page__subtitle,
.member-crm-page__footer-note {
  max-width: 64rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.member-crm-page__grid {
  display: grid;
  grid-template-columns: 0.96fr 1.04fr;
  gap: 1rem;
  align-items: start;
}

.member-crm-page__stack {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1180px) {
  .member-crm-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
