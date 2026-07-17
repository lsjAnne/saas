<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { OrganizationMember, Store } from '@/services/apiTypes';
import type {
  InviteOrganizationMemberPayload,
  RolePermissionMatrixView
} from '@/services/organizationCenterService';
import { formatDate } from '@/utils/formatters';

interface Props {
  members: OrganizationMember[];
  organizationStores: Store[];
  selectedOrganizationName: string;
  roleMatrix: RolePermissionMatrixView[];
  isSubmittingInvite: boolean;
  updatingMemberId: string | null;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  inviteMember: [payload: InviteOrganizationMemberPayload];
  updateRole: [payload: { memberId: string; roleCode: string }];
}>();

const inviteForm = reactive<InviteOrganizationMemberPayload>({
  userId: '',
  userName: '',
  mobile: '',
  roleCode: 'operator'
});

const roleDrafts = reactive<Record<string, string>>({});

const roleOptions = computed(() => props.roleMatrix.map((item) => item.roleCode));

watch(
  () => props.members,
  (members) => {
    Object.keys(roleDrafts).forEach((key) => {
      delete roleDrafts[key];
    });

    members.forEach((member) => {
      roleDrafts[member.memberId] = member.roleCode;
    });
  },
  { immediate: true }
);

function roleLabel(roleCode: string) {
  const normalized = roleCode === 'tenant_admin' ? 'admin' : roleCode;
  const mapping: Record<string, string> = {
    owner: '租户 Owner',
    admin: '组织 Admin',
    operator: '运营 Operator',
    service: '客服 Service'
  };

  return mapping[normalized] ?? normalized;
}

function roleDescription(roleCode: string) {
  const normalized = roleCode === 'tenant_admin' ? 'admin' : roleCode;
  const mapping: Record<string, string> = {
    owner: '可分配全部角色，并覆盖租户治理、审计与经营全链路。',
    admin: '可管理组织与成员，但不能把其他成员提升为 Owner。',
    operator: '聚焦店铺经营与履约执行，继承当前组织下的业务范围。',
    service: '聚焦客服、会员与通知协同，适合售后与客户运营岗位。'
  };

  return mapping[normalized] ?? '沿当前组织范围继承默认权限。';
}

function toneFromStatus(status: string) {
  return status === 'active' ? 'success' : 'neutral';
}

function handleInviteSubmit() {
  emit('inviteMember', {
    userId: inviteForm.userId.trim(),
    userName: inviteForm.userName.trim(),
    mobile: inviteForm.mobile?.trim() || '',
    roleCode: inviteForm.roleCode
  });

  inviteForm.userId = '';
  inviteForm.userName = '';
  inviteForm.mobile = '';
  inviteForm.roleCode = 'operator';
}

function handleRoleUpdate(memberId: string) {
  emit('updateRole', {
    memberId,
    roleCode: roleDrafts[memberId] || 'operator'
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Members & Roles"
    title="成员列表与角色分配"
    :description="`当前正在治理 ${selectedOrganizationName} 的成员、角色和店铺范围。`"
  >
    <div class="organization-members">
      <form class="organization-members__invite" @submit.prevent="handleInviteSubmit">
        <div class="organization-members__invite-head">
          <div>
            <h4 class="organization-members__section-title">邀请成员</h4>
            <p class="organization-members__section-copy">
              直接在当前组织内创建成员并指定初始角色，后续可再调整角色。
            </p>
          </div>
          <button
            type="submit"
            class="organization-members__primary"
            :disabled="
              isSubmittingInvite ||
              !inviteForm.userId.trim() ||
              !inviteForm.userName.trim() ||
              !inviteForm.roleCode
            "
          >
            {{ isSubmittingInvite ? '邀请中...' : '邀请成员' }}
          </button>
        </div>

        <div class="organization-members__invite-grid">
          <label class="organization-members__field">
            <span>成员账号</span>
            <input v-model="inviteForm.userId" type="text" placeholder="例如：ops-lin" />
          </label>
          <label class="organization-members__field">
            <span>成员姓名</span>
            <input v-model="inviteForm.userName" type="text" placeholder="例如：林运营" />
          </label>
          <label class="organization-members__field">
            <span>手机号</span>
            <input v-model="inviteForm.mobile" type="text" placeholder="可选" />
          </label>
          <label class="organization-members__field">
            <span>初始角色</span>
            <select v-model="inviteForm.roleCode">
              <option v-for="role in roleOptions" :key="role" :value="role">
                {{ roleLabel(role) }}
              </option>
            </select>
          </label>
        </div>
      </form>

      <div class="organization-members__scope">
        <div>
          <h4 class="organization-members__section-title">店铺范围</h4>
          <p class="organization-members__section-copy">
            当前版本的店铺权限按组织生效，成员默认继承当前组织下的店铺范围。
          </p>
        </div>
        <div class="organization-members__scope-chips">
          <span
            v-for="store in organizationStores"
            :key="store.storeId"
            class="organization-members__scope-chip"
          >
            {{ store.shopName }}
          </span>
          <p v-if="organizationStores.length === 0" class="organization-members__empty">
            当前组织下还没有店铺记录。
          </p>
        </div>
      </div>

      <div class="organization-members__list">
        <article
          v-for="member in members"
          :key="member.memberId"
          class="organization-members__item"
        >
          <div class="organization-members__head">
            <div>
              <h4 class="organization-members__title">{{ member.userName }}</h4>
              <p class="organization-members__meta">
                {{ member.userId }} · {{ member.mobile || '未留手机号' }}
              </p>
            </div>
            <StatusPill :label="member.status" :tone="toneFromStatus(member.status)" />
          </div>

          <div class="organization-members__body">
            <div class="organization-members__role-copy">
              <p class="organization-members__role-label">{{ roleLabel(member.roleCode) }}</p>
              <p class="organization-members__role-description">
                {{ roleDescription(roleDrafts[member.memberId] || member.roleCode) }}
              </p>
              <p class="organization-members__joined">加入时间 {{ formatDate(member.joinedAt) }}</p>
            </div>

            <div class="organization-members__role-editor">
              <label class="organization-members__field">
                <span>角色</span>
                <select v-model="roleDrafts[member.memberId]">
                  <option v-for="role in roleOptions" :key="role" :value="role">
                    {{ roleLabel(role) }}
                  </option>
                </select>
              </label>
              <button
                type="button"
                class="organization-members__secondary"
                :disabled="
                  updatingMemberId === member.memberId ||
                  roleDrafts[member.memberId] === member.roleCode
                "
                @click="handleRoleUpdate(member.memberId)"
              >
                {{ updatingMemberId === member.memberId ? '更新中...' : '更新角色' }}
              </button>
            </div>
          </div>
        </article>

        <p v-if="members.length === 0" class="organization-members__empty">
          当前组织暂无成员记录。
        </p>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.organization-members {
  display: grid;
  gap: 1rem;
}

.organization-members__invite,
.organization-members__scope,
.organization-members__item {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.organization-members__invite,
.organization-members__scope,
.organization-members__list {
  display: grid;
  gap: 0.9rem;
}

.organization-members__invite-head,
.organization-members__head,
.organization-members__body {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.organization-members__invite-head,
.organization-members__head {
  align-items: flex-start;
}

.organization-members__body {
  align-items: end;
}

.organization-members__invite-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.organization-members__field {
  display: grid;
  gap: 0.4rem;
}

.organization-members__field span,
.organization-members__role-label,
.organization-members__section-title {
  color: var(--color-ink-strong);
  font-weight: 600;
}

.organization-members__field input,
.organization-members__field select {
  min-height: 2.75rem;
  padding: 0 0.9rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: #fff;
  font: inherit;
}

.organization-members__section-title,
.organization-members__section-copy,
.organization-members__title,
.organization-members__meta,
.organization-members__role-label,
.organization-members__role-description,
.organization-members__joined,
.organization-members__empty {
  margin: 0;
}

.organization-members__section-copy,
.organization-members__meta,
.organization-members__role-description,
.organization-members__joined,
.organization-members__empty {
  color: var(--color-ink-soft);
  line-height: 1.65;
}

.organization-members__scope-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.organization-members__scope-chip {
  min-height: 2rem;
  padding: 0.35rem 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(132, 108, 85, 0.12);
  color: var(--color-ink);
  display: inline-flex;
  align-items: center;
}

.organization-members__primary,
.organization-members__secondary {
  min-height: 2.75rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
  font: inherit;
}

.organization-members__primary {
  border: 0;
  background: linear-gradient(135deg, rgba(117, 82, 59, 0.96), rgba(36, 25, 18, 0.96));
  color: #fff8f0;
}

.organization-members__secondary {
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: transparent;
  color: var(--color-ink);
}

.organization-members__role-editor {
  min-width: 14rem;
  display: grid;
  gap: 0.65rem;
}

@media (max-width: 1080px) {
  .organization-members__invite-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .organization-members__invite-head,
  .organization-members__head,
  .organization-members__body {
    flex-direction: column;
  }

  .organization-members__role-editor {
    min-width: 100%;
  }
}
</style>
