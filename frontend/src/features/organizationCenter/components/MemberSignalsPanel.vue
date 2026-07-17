<script setup lang="ts">
import { computed } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import type { Store } from '@/services/apiTypes';
import type { RolePermissionMatrixView } from '@/services/organizationCenterService';
import { formatCount } from '@/utils/formatters';

interface Props {
  roleMatrix: RolePermissionMatrixView[];
  permissionCodes: string[];
  currentUserRole: string | null;
  selectedOrganizationName: string;
  selectedOrganizationStores: Store[];
  optionalNotices: string[];
}

const props = defineProps<Props>();

const currentPermissionCodes = computed(() => [...new Set(props.permissionCodes)].sort());

function normalizeRole(roleCode: string | null | undefined) {
  if (roleCode === 'tenant_admin') {
    return 'admin';
  }

  if (roleCode === 'customer_service') {
    return 'service';
  }

  return roleCode ?? 'operator';
}

function roleLabel(roleCode: string | null | undefined) {
  const mapping: Record<string, string> = {
    owner: '租户 Owner',
    admin: '组织 Admin',
    operator: '运营 Operator',
    service: '客服 Service'
  };

  const normalized = normalizeRole(roleCode);
  return mapping[normalized] ?? normalized;
}

function roleDescription(roleCode: string | null | undefined) {
  const mapping: Record<string, string> = {
    owner: '覆盖租户治理、组织调整、审计查看与核心配置管理。',
    admin: '负责组织成员治理、角色分配与日常权限维护。',
    operator: '聚焦店铺经营、订单履约与组织内业务执行。',
    service: '聚焦客服协同、会员触达与售后支持场景。'
  };

  const normalized = normalizeRole(roleCode);
  return mapping[normalized] ?? '沿当前组织范围继承默认权限。';
}
</script>

<template>
  <PanelCard
    eyebrow="Roles & Scope"
    title="角色权限与数据范围"
    :description="`围绕 ${selectedOrganizationName} 展示角色矩阵、当前登录权限以及组织级店铺范围。`"
  >
    <div class="member-governance">
      <div class="member-governance__metrics">
        <MetricCard
          label="角色层级"
          :value="formatCount(roleMatrix.length, ' 个')"
          caption="当前租户可分配的角色层级数量"
        />
        <MetricCard
          label="当前权限"
          :value="formatCount(currentPermissionCodes.length, ' 项')"
          caption="当前登录账号实际拿到的权限编码数"
        />
        <MetricCard
          label="组织店铺"
          :value="formatCount(selectedOrganizationStores.length, ' 家')"
          caption="当前组织下成员默认继承的店铺范围"
        />
      </div>

      <div class="member-governance__layout">
        <section class="member-governance__block">
          <div class="member-governance__section-head">
            <div>
              <h4 class="member-governance__section-title">角色权限区</h4>
              <p class="member-governance__section-copy">
                角色矩阵直接来自租户 RBAC 接口，用于邀请成员和调整角色时对齐真实权限边界。
              </p>
            </div>
          </div>

          <div v-if="roleMatrix.length > 0" class="member-governance__role-grid">
            <article
              v-for="role in roleMatrix"
              :key="role.roleCode"
              class="member-governance__role-card"
            >
              <div class="member-governance__role-head">
                <div>
                  <h5 class="member-governance__role-title">{{ roleLabel(role.roleCode) }}</h5>
                  <p class="member-governance__role-copy">{{ roleDescription(role.roleCode) }}</p>
                </div>
                <strong class="member-governance__role-count">
                  {{ formatCount(role.permissionCodes.length, ' 项') }}
                </strong>
              </div>

              <div class="member-governance__chips">
                <span
                  v-for="permissionCode in role.permissionCodes"
                  :key="permissionCode"
                  class="member-governance__chip"
                >
                  {{ permissionCode }}
                </span>
              </div>
            </article>
          </div>

          <p v-else class="member-governance__empty">当前还没有可展示的角色矩阵。</p>
        </section>

        <section class="member-governance__block">
          <div class="member-governance__section-head">
            <div>
              <h4 class="member-governance__section-title">数据范围区</h4>
              <p class="member-governance__section-copy">
                当前版本按组织生效店铺范围，成员默认继承当前组织下的店铺权限。
              </p>
            </div>
          </div>

          <div class="member-governance__scope-card">
            <p class="member-governance__scope-label">当前登录角色</p>
            <p class="member-governance__scope-value">{{ roleLabel(currentUserRole) }}</p>
          </div>

          <div class="member-governance__stack">
            <div>
              <p class="member-governance__label">当前账号权限编码</p>
              <div class="member-governance__chips">
                <span
                  v-for="permissionCode in currentPermissionCodes"
                  :key="permissionCode"
                  class="member-governance__chip member-governance__chip--accent"
                >
                  {{ permissionCode }}
                </span>
                <p v-if="currentPermissionCodes.length === 0" class="member-governance__empty">
                  当前账号还没有加载到权限编码。
                </p>
              </div>
            </div>

            <div>
              <p class="member-governance__label">当前组织店铺范围</p>
              <div class="member-governance__chips">
                <span
                  v-for="store in selectedOrganizationStores"
                  :key="store.storeId"
                  class="member-governance__chip"
                >
                  {{ store.shopName }}
                </span>
                <p v-if="selectedOrganizationStores.length === 0" class="member-governance__empty">
                  当前组织下还没有店铺记录，成员范围将随组织后续绑定店铺同步扩展。
                </p>
              </div>
            </div>

            <div v-if="optionalNotices.length > 0">
              <p class="member-governance__label">降级提示</p>
              <ul class="member-governance__notice-list">
                <li
                  v-for="notice in optionalNotices"
                  :key="notice"
                  class="member-governance__notice-item"
                >
                  {{ notice }}
                </li>
              </ul>
            </div>
          </div>
        </section>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.member-governance {
  display: grid;
  gap: 1rem;
}

.member-governance__metrics,
.member-governance__layout,
.member-governance__role-grid,
.member-governance__stack {
  display: grid;
  gap: 0.85rem;
}

.member-governance__metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.member-governance__layout {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.member-governance__block,
.member-governance__role-card,
.member-governance__scope-card {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.member-governance__block {
  display: grid;
  gap: 0.9rem;
}

.member-governance__section-title,
.member-governance__section-copy,
.member-governance__role-title,
.member-governance__role-copy,
.member-governance__scope-label,
.member-governance__scope-value,
.member-governance__label,
.member-governance__empty {
  margin: 0;
}

.member-governance__section-title,
.member-governance__role-title,
.member-governance__scope-value,
.member-governance__label {
  color: var(--color-ink-strong);
}

.member-governance__section-copy,
.member-governance__role-copy,
.member-governance__scope-label,
.member-governance__empty {
  color: var(--color-ink-soft);
  line-height: 1.65;
}

.member-governance__role-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.member-governance__role-count {
  color: var(--color-accent);
  white-space: nowrap;
}

.member-governance__scope-value {
  margin-top: 0.35rem;
  font-size: 1.1rem;
}

.member-governance__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.member-governance__chip {
  min-height: 2rem;
  padding: 0.35rem 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(132, 108, 85, 0.12);
  color: var(--color-ink);
  display: inline-flex;
  align-items: center;
  word-break: break-all;
}

.member-governance__chip--accent {
  background: rgba(117, 82, 59, 0.16);
}

.member-governance__notice-list {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--color-ink-soft);
  display: grid;
  gap: 0.45rem;
}

.member-governance__notice-item {
  line-height: 1.6;
}

@media (max-width: 1080px) {
  .member-governance__layout,
  .member-governance__metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .member-governance__role-head {
    flex-direction: column;
  }
}
</style>
