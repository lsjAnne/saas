<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';

import PanelCard from '@/components/cards/PanelCard.vue';
import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import AuditTrailPanel from '@/features/auditCenter/components/AuditTrailPanel.vue';
import MemberSignalsPanel from '@/features/organizationCenter/components/MemberSignalsPanel.vue';
import OrganizationDirectoryPanel from '@/features/organizationCenter/components/OrganizationDirectoryPanel.vue';
import OrganizationMembersPanel from '@/features/organizationCenter/components/OrganizationMembersPanel.vue';
import { useOrganizationCenterOverview } from '@/features/organizationCenter/composables/useOrganizationCenterOverview';
import type { InviteOrganizationMemberPayload } from '@/services/organizationCenterService';
import { useAuthStore } from '@/stores/authStore';

const router = useRouter();
const authStore = useAuthStore();
const organizationCenter = useOrganizationCenterOverview();

const selectedOrganizationName = computed(
  () => organizationCenter.selectedOrganization.value?.organizationName ?? '当前组织'
);

authStore.hydrate();

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  try {
    await organizationCenter.load(authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

async function handleSelectOrganization(organizationId: string) {
  if (!authStore.token) {
    return;
  }

  try {
    await organizationCenter.selectOrganization(organizationId, authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

async function handleInviteMember(payload: InviteOrganizationMemberPayload) {
  if (!authStore.token) {
    return;
  }

  try {
    await organizationCenter.submitInvite(payload, authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

async function handleUpdateRole(payload: { memberId: string; roleCode: string }) {
  if (!authStore.token) {
    return;
  }

  try {
    await organizationCenter.updateMemberRole(payload.memberId, payload.roleCode, authStore.token);
  } catch {
    // errorMessage is already set inside the composable
  }
}

function goToAuditCenter() {
  router.push({ name: 'audit-exports' });
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="organization-page">
    <header class="organization-page__header">
      <p class="organization-page__eyebrow">Organization Governance</p>
      <h2 class="organization-page__title">把组织结构、成员治理、权限边界和审计入口收敛到一个治理中枢</h2>
      <p class="organization-page__subtitle">
        当前页围绕组织与权限中心的真实接口编排组织结构区、成员治理区、角色权限区、数据范围区和审计入口区。
      </p>
    </header>

    <InlineErrorCard
      v-if="organizationCenter.errorMessage"
      :message="organizationCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <div class="organization-page__primary">
      <OrganizationDirectoryPanel
        :organizations="organizationCenter.organizations"
        :selected-organization-id="organizationCenter.selectedOrganizationId"
        @select="handleSelectOrganization"
      />
      <OrganizationMembersPanel
        :members="organizationCenter.organizationMembers"
        :organization-stores="organizationCenter.selectedOrganizationStores"
        :selected-organization-name="selectedOrganizationName"
        :role-matrix="organizationCenter.roleMatrix"
        :is-submitting-invite="organizationCenter.isSubmittingInvite"
        :updating-member-id="organizationCenter.updatingMemberId"
        @invite-member="handleInviteMember"
        @update-role="handleUpdateRole"
      />
    </div>

    <MemberSignalsPanel
      :role-matrix="organizationCenter.roleMatrix"
      :permission-codes="authStore.permissionCodes"
      :current-user-role="authStore.user?.role ?? null"
      :selected-organization-name="selectedOrganizationName"
      :selected-organization-stores="organizationCenter.selectedOrganizationStores"
      :optional-notices="organizationCenter.optionalNotices"
    />

    <div class="organization-page__audit">
      <AuditTrailPanel :audit-logs="organizationCenter.governanceAuditLogs" />

      <PanelCard
        eyebrow="Audit Entry"
        title="查看完整审计中心"
        description="组织权限治理页只保留治理相关的近端日志，完整导出治理、全量审计轨迹和合规留痕在独立审计中心查看。"
      >
        <div class="organization-page__audit-entry">
          <p class="organization-page__audit-copy">
            当前已加载 {{ organizationCenter.governanceAuditLogs.length }} 条治理日志，覆盖组织与成员角色变更相关轨迹。
          </p>
          <p class="organization-page__audit-copy">
            如需继续查看导出任务、全量租户审计或合规留痕，请进入完整审计中心。
          </p>
          <button type="button" class="organization-page__audit-button" @click="goToAuditCenter">
            查看完整审计中心
          </button>
        </div>
      </PanelCard>
    </div>
  </section>
</template>

<style scoped>
.organization-page {
  display: grid;
  gap: 1.2rem;
}

.organization-page__header {
  display: grid;
  gap: 0.75rem;
}

.organization-page__eyebrow,
.organization-page__title,
.organization-page__subtitle,
.organization-page__audit-copy {
  margin: 0;
}

.organization-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.organization-page__title {
  max-width: 20ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.1rem, 3.8vw, 3.6rem);
  line-height: 1.05;
}

.organization-page__subtitle {
  max-width: 56rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.organization-page__primary,
.organization-page__audit {
  display: grid;
  gap: 1rem;
}

.organization-page__primary {
  grid-template-columns: 0.95fr 1.05fr;
}

.organization-page__audit {
  grid-template-columns: 1.1fr 0.9fr;
}

.organization-page__audit-entry {
  display: grid;
  gap: 0.9rem;
}

.organization-page__audit-copy {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.organization-page__audit-button {
  min-height: 2.9rem;
  width: fit-content;
  padding: 0 1rem;
  border: 0;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, rgba(117, 82, 59, 0.96), rgba(36, 25, 18, 0.96));
  color: #fff8f0;
  font: inherit;
}

@media (max-width: 1080px) {
  .organization-page__primary,
  .organization-page__audit {
    grid-template-columns: 1fr;
  }
}
</style>
