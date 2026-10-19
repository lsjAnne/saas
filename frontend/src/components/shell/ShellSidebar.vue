<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

import { canAccessConsoleRoute, consoleRoutes } from '@/app/router';
import { NAV_GROUPS } from '@/app/router/navGroups';
import { useAuthStore } from '@/stores/authStore';

const route = useRoute();
const authStore = useAuthStore();

const groupedItems = computed(() =>
  NAV_GROUPS.map((group) => ({
    ...group,
    items: consoleRoutes
      .filter((item) => item.meta.navGroup === group.key)
      .filter((item) => canAccessConsoleRoute(authStore, item))
      .map((item) => ({
        label: item.meta.navLabel,
        description: item.meta.navDescription,
        to: `/app/${item.path}`
      }))
  })).filter((group) => group.items.length > 0)
);

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
        <h2 class="shell-sidebar__title">电商运营中台</h2>
      </div>
    </section>

    <nav class="shell-sidebar__nav">
      <section
        v-for="group in groupedItems"
        :key="group.key"
        class="shell-sidebar__group"
      >
        <p class="shell-sidebar__group-label">
          <span class="shell-sidebar__group-dot" aria-hidden="true"></span>
          {{ group.label }}
        </p>
        <div class="shell-sidebar__items">
          <RouterLink
            v-for="item in group.items"
            :key="item.to"
            :to="item.to"
            :class="['shell-sidebar__item', { 'shell-sidebar__item--active': isActive(item.to) }]"
          >
            <span class="shell-sidebar__item-label">{{ item.label }}</span>
            <span class="shell-sidebar__item-description">{{ item.description }}</span>
          </RouterLink>
        </div>
      </section>
    </nav>
  </aside>
</template>

<style scoped>
.shell-sidebar {
  display: grid;
  align-content: start;
  gap: 1.1rem;
  padding: 1.1rem 0.9rem 1.4rem;
  min-height: 100%;
  background: var(--color-sidebar-top);
  border-right: 1px solid var(--color-line);
}

.shell-sidebar__brand {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.75rem;
  align-items: center;
  padding: 0.3rem 0.5rem 0.9rem;
  border-bottom: 1px solid var(--color-line);
}

.shell-sidebar__crest {
  width: 2.4rem;
  height: 2.4rem;
  border-radius: var(--radius-md);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  font-weight: 700;
  color: #ffffff;
  background: linear-gradient(140deg, var(--color-accent), var(--color-accent-hover));
}

.shell-sidebar__brand-copy {
  display: grid;
  gap: 0.1rem;
}

.shell-sidebar__eyebrow {
  margin: 0;
  color: var(--color-ink-faint);
  font-size: 0.7rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.shell-sidebar__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--color-ink-strong);
}

.shell-sidebar__nav {
  display: grid;
  gap: 1.15rem;
}

.shell-sidebar__group {
  display: grid;
  gap: 0.45rem;
}

.shell-sidebar__group-label {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin: 0;
  padding: 0 0.55rem;
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  color: var(--color-ink-faint);
}

.shell-sidebar__group-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-accent-soft);
  border: 1px solid var(--color-accent);
}

.shell-sidebar__items {
  display: grid;
  gap: 0.22rem;
}

.shell-sidebar__item {
  position: relative;
  display: grid;
  gap: 0.2rem;
  padding: 0.62rem 0.7rem;
  border-radius: var(--radius-md);
  color: var(--color-ink);
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.shell-sidebar__item:hover {
  background: var(--color-surface-2);
  color: var(--color-ink-strong);
}

.shell-sidebar__item--active {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.shell-sidebar__item--active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12%;
  bottom: 12%;
  width: 3px;
  border-radius: 2px;
  background: var(--color-accent);
}

.shell-sidebar__item-label {
  font-size: 0.92rem;
  font-weight: 600;
}

.shell-sidebar__item-description {
  font-size: 0.75rem;
  line-height: 1.4;
  color: var(--color-ink-soft);
}

.shell-sidebar__item--active .shell-sidebar__item-description {
  color: var(--color-accent);
}

@media (max-width: 960px) {
  .shell-sidebar {
    padding: 0.8rem;
  }

  .shell-sidebar__items {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }
}
</style>
