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
      title: typeof route.meta.title === 'string' ? route.meta.title : '租户控制台',
      subtitle:
        typeof route.meta.subtitle === 'string'
          ? route.meta.subtitle
          : '保持租户、组织、权限与交付边界的统一视图'
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
      <p class="shell-topbar__eyebrow">Tenant Console</p>
      <h1 class="shell-topbar__title">{{ currentSection.title }}</h1>
      <p class="shell-topbar__subtitle">{{ currentSection.subtitle }}</p>
    </div>

    <div class="shell-topbar__meta">
      <span class="shell-topbar__chip shell-topbar__chip--accent">{{ tenantLabel }}</span>
      <span class="shell-topbar__chip">{{ organizationLabel }}</span>
      <span class="shell-topbar__chip">{{ roleLabel }}</span>
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
  padding: 1.35rem 1.7rem;
  background: rgba(255, 253, 250, 0.68);
  border-bottom: 1px solid var(--color-line);
  backdrop-filter: blur(14px);
}

.shell-topbar__brand {
  display: grid;
  gap: 0.28rem;
}

.shell-topbar__eyebrow {
  margin: 0;
  color: var(--color-ink-faint);
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.shell-topbar__title {
  margin: 0;
  font-family: var(--font-display);
  color: var(--color-ink-strong);
  font-size: clamp(1.35rem, 2.3vw, 1.85rem);
}

.shell-topbar__subtitle {
  margin: 0;
  color: var(--color-ink-soft);
  font-size: 0.92rem;
}

.shell-topbar__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.75rem;
  align-items: center;
}

.shell-topbar__chip,
.shell-topbar__user {
  min-height: 2.75rem;
  border-radius: var(--radius-pill);
  display: inline-flex;
  align-items: center;
}

.shell-topbar__chip {
  padding: 0 1rem;
  border: 1px solid var(--color-line);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-ink);
}

.shell-topbar__chip--accent {
  background: rgba(217, 192, 163, 0.2);
  border-color: rgba(133, 98, 67, 0.18);
}

.shell-topbar__user {
  gap: 0.7rem;
  padding: 0.35rem 0.45rem 0.35rem 0.35rem;
  border: 0;
  background: linear-gradient(135deg, #2d241c, #805f41);
  color: #fff7ef;
  box-shadow: 0 14px 30px rgba(58, 43, 28, 0.2);
}

.shell-topbar__avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.16);
  font-weight: 700;
}

.shell-topbar__user-copy {
  display: grid;
  justify-items: start;
  gap: 0.05rem;
  padding-right: 0.45rem;
}

.shell-topbar__user-name {
  font-size: 0.92rem;
}

.shell-topbar__user-action {
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.74;
}

@media (max-width: 960px) {
  .shell-topbar {
    padding: 1rem 1.1rem;
  }

  .shell-topbar__meta {
    justify-content: flex-start;
  }
}
</style>
