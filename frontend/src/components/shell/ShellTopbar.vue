<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

interface Props {
  tenantLabel: string;
  organizationLabel: string;
  userName: string;
  roleLabel: string;
  operatorType: string;
}

const props = defineProps<Props>();

defineEmits<{
  logout: [];
}>();

const route = useRoute();

const currentSection = computed(
  () =>
    ({
      title: typeof route.meta.title === 'string' ? route.meta.title : '工作台',
      subtitle:
        typeof route.meta.subtitle === 'string'
          ? route.meta.subtitle
          : '经营、履约、渠道与会员的统一工作台'
    })
);

const userInitial = computed(() => props.userName.trim().charAt(0).toUpperCase() || 'A');
const operatorLabel = computed(() => {
  const normalized = props.operatorType.trim();

  switch (normalized) {
    case 'tenant-user':
      return '租户负责人';
    case 'tenant-admin':
      return '租户管理员';
    case 'customer-service':
      return '客服坐席';
    case 'platform-support':
      return '平台支持';
    default:
      return normalized || props.roleLabel;
  }
});
</script>

<template>
  <header class="shell-topbar">
    <div class="shell-topbar__brand">
      <h1 class="shell-topbar__title">{{ currentSection.title }}</h1>
      <p class="shell-topbar__subtitle">{{ currentSection.subtitle }}</p>
    </div>

    <div class="shell-topbar__meta">
      <span class="shell-topbar__chip">{{ tenantLabel }}</span>
      <span class="shell-topbar__chip">{{ organizationLabel }}</span>
      <span class="shell-topbar__chip">{{ operatorLabel }}</span>

      <button class="shell-topbar__user" type="button" @click="$emit('logout')">
        <span class="shell-topbar__avatar">{{ userInitial }}</span>
        <span class="shell-topbar__user-copy">
          <span class="shell-topbar__user-name">{{ userName }}</span>
          <span class="shell-topbar__user-action">退出登录</span>
        </span>
      </button>
    </div>
  </header>
</template>

<style scoped>
.shell-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.6rem;
  background: var(--color-panel-strong);
  border-bottom: 1px solid var(--color-line);
}

.shell-topbar__brand {
  display: grid;
  gap: 0.18rem;
  min-width: 0;
}

.shell-topbar__title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
  color: var(--color-ink-strong);
}

.shell-topbar__subtitle {
  margin: 0;
  color: var(--color-ink-soft);
  font-size: 0.85rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.shell-topbar__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.55rem;
  align-items: center;
}

.shell-topbar__chip,
.shell-topbar__user {
  min-height: 2.2rem;
  border-radius: var(--radius-md);
  display: inline-flex;
  align-items: center;
}

.shell-topbar__chip {
  padding: 0 0.75rem;
  border: 1px solid var(--color-line);
  background: var(--color-surface);
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.shell-topbar__user {
  gap: 0.6rem;
  padding: 0.25rem 0.35rem 0.25rem 0.3rem;
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
}

.shell-topbar__user:hover {
  border-color: var(--color-line-strong);
  background: var(--color-surface-2);
}

.shell-topbar__avatar {
  width: 1.7rem;
  height: 1.7rem;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-accent);
  color: #ffffff;
  font-size: 0.8rem;
  font-weight: 700;
}

.shell-topbar__user-copy {
  display: grid;
  justify-items: start;
  gap: 0;
  padding-right: 0.3rem;
}

.shell-topbar__user-name {
  font-size: 0.85rem;
  font-weight: 600;
}

.shell-topbar__user-action {
  font-size: 0.7rem;
  color: var(--color-ink-faint);
}

@media (max-width: 960px) {
  .shell-topbar {
    padding: 0.85rem 1rem;
    flex-wrap: wrap;
  }

  .shell-topbar__meta {
    justify-content: flex-start;
  }
}
</style>
