<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

import { canAccessConsoleRoute, consoleRoutes } from '@/app/router';
import { useAuthStore } from '@/stores/authStore';

const route = useRoute();
const authStore = useAuthStore();

const visibleItems = computed(() =>
  consoleRoutes
    .filter((item) => canAccessConsoleRoute(authStore, item))
    .map((item) => ({
      label: item.meta.navLabel,
      description: item.meta.navDescription,
      to: `/app/${item.path}`
    }))
);

const activeItem = computed(
  () =>
    visibleItems.value.find((item) => route.path.startsWith(item.to)) ??
    visibleItems.value[0]
);

const focusItems = computed(() => {
  const matched = consoleRoutes.find((item) => route.path.startsWith(`/app/${item.path}`));
  return matched?.meta.focusItems ?? [];
});

function isActive(path: string) {
  return route.path.startsWith(path);
}
</script>

<template>
  <aside class="shell-sidebar">
    <section class="shell-sidebar__brand">
      <span class="shell-sidebar__crest">A</span>
      <div class="shell-sidebar__brand-copy">
        <p class="shell-sidebar__eyebrow">Ark Commerce</p>
        <h2 class="shell-sidebar__title">Tenant Suite</h2>
      </div>
    </section>

    <section class="shell-sidebar__group">
      <p class="shell-sidebar__label">Navigation</p>
      <nav class="shell-sidebar__nav">
        <RouterLink
          v-for="item in visibleItems"
          :key="item.to"
          :to="item.to"
          :class="['shell-sidebar__item', { 'shell-sidebar__item--active': isActive(item.to) }]"
        >
          <span class="shell-sidebar__item-label">{{ item.label }}</span>
          <span class="shell-sidebar__item-description">{{ item.description }}</span>
        </RouterLink>
      </nav>
    </section>

    <section class="shell-sidebar__group shell-sidebar__group--focus">
      <p class="shell-sidebar__label">Focus</p>
      <div v-if="activeItem" class="shell-sidebar__focus-card">
        <p class="shell-sidebar__focus-title">{{ activeItem.label }}</p>
        <div class="shell-sidebar__focus-list">
          <span
            v-for="item in focusItems"
            :key="item"
            class="shell-sidebar__focus-pill"
          >
            {{ item }}
          </span>
        </div>
      </div>
      <div v-else class="shell-sidebar__focus-card">
        <p class="shell-sidebar__focus-title">当前账号无可用模块</p>
      </div>
    </section>
  </aside>
</template>

<style scoped>
.shell-sidebar {
  display: grid;
  align-content: start;
  gap: 1.7rem;
  padding: 1.4rem 1rem 1.2rem;
  min-height: 100%;
  background:
    radial-gradient(circle at top, rgba(219, 186, 145, 0.18), transparent 28%),
    linear-gradient(180deg, var(--color-sidebar-top), var(--color-sidebar-bottom));
}

.shell-sidebar__brand {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.9rem;
  align-items: center;
  padding: 0.35rem 0.5rem 0.8rem;
}

.shell-sidebar__crest {
  width: 2.95rem;
  height: 2.95rem;
  border-radius: 1rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 1.35rem;
  color: #fff8f0;
  background: linear-gradient(140deg, rgba(225, 202, 173, 0.42), rgba(255, 255, 255, 0.08));
  border: 1px solid rgba(255, 244, 236, 0.18);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.shell-sidebar__brand-copy,
.shell-sidebar__group {
  display: grid;
  gap: 0.85rem;
}

.shell-sidebar__eyebrow,
.shell-sidebar__label {
  margin: 0;
  color: rgba(255, 244, 236, 0.46);
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.shell-sidebar__title {
  margin: 0.1rem 0 0;
  font-family: var(--font-display);
  font-size: 1.32rem;
  color: #fff5ea;
}

.shell-sidebar__nav,
.shell-sidebar__focus-list {
  display: grid;
  gap: 0.5rem;
}

.shell-sidebar__item {
  display: grid;
  gap: 0.32rem;
  padding: 0.92rem 1rem;
  border-radius: 1.2rem;
  color: rgba(255, 244, 236, 0.9);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid transparent;
  transition:
    transform var(--transition-fast),
    background var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.shell-sidebar__item:hover {
  transform: translateX(2px);
  border-color: rgba(255, 244, 236, 0.12);
  background: rgba(255, 255, 255, 0.06);
}

.shell-sidebar__item--active {
  background: linear-gradient(140deg, rgba(255, 249, 242, 0.98), rgba(225, 207, 186, 0.86));
  color: var(--color-ink-strong);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72), 0 14px 34px rgba(0, 0, 0, 0.18);
}

.shell-sidebar__item-label {
  font-weight: 600;
  letter-spacing: 0.02em;
}

.shell-sidebar__item-description {
  font-size: 0.78rem;
  line-height: 1.4;
  color: inherit;
  opacity: 0.72;
}

.shell-sidebar__group--focus {
  margin-top: 0.15rem;
}

.shell-sidebar__focus-card {
  display: grid;
  gap: 0.9rem;
  padding: 1rem;
  border-radius: 1.35rem;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.03));
  border: 1px solid rgba(255, 244, 236, 0.1);
}

.shell-sidebar__focus-title {
  margin: 0;
  color: #fff6ed;
  font-weight: 600;
}

.shell-sidebar__focus-pill {
  display: inline-flex;
  align-items: center;
  min-height: 2.25rem;
  padding: 0 0.85rem;
  border-radius: var(--radius-pill);
  color: rgba(255, 244, 236, 0.78);
  background: rgba(255, 255, 255, 0.05);
  font-size: 0.82rem;
}

@media (max-width: 960px) {
  .shell-sidebar {
    gap: 1rem;
    padding: 1rem;
  }

  .shell-sidebar__nav {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }
}
</style>
