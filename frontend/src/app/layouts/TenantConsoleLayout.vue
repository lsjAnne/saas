<script setup lang="ts">
import { useRouter } from 'vue-router';

import ShellSidebar from '@/components/shell/ShellSidebar.vue';
import ShellTopbar from '@/components/shell/ShellTopbar.vue';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const router = useRouter();

authStore.hydrate();

function handleLogout() {
  authStore.clearSession();
  router.push('/login');
}
</script>

<template>
  <div class="tenant-layout">
    <div class="tenant-layout__shell">
      <ShellSidebar />

      <div class="tenant-layout__main">
        <ShellTopbar
          :tenant-label="authStore.tenantId || 'tenant'"
          :organization-label="authStore.organizationId || 'org-unbound'"
          :user-name="authStore.user?.name || '租户用户'"
          :role-label="authStore.user?.role || 'owner'"
          :operator-type="authStore.user?.operatorType || 'tenant-user'"
          @logout="handleLogout"
        />

        <main class="tenant-layout__content">
          <RouterView />
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tenant-layout {
  min-height: 100vh;
  padding: 1.35rem;
}

.tenant-layout__shell {
  min-height: calc(100vh - 2.7rem);
  display: grid;
  grid-template-columns: 252px minmax(0, 1fr);
  border-radius: var(--radius-shell);
  overflow: hidden;
  border: 1px solid rgba(91, 72, 54, 0.1);
  box-shadow: var(--shadow-soft);
}

.tenant-layout__main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background: linear-gradient(180deg, rgba(255, 250, 244, 0.72), rgba(249, 243, 236, 0.9));
}

.tenant-layout__content {
  padding: 1.6rem;
}

@media (max-width: 960px) {
  .tenant-layout {
    padding: 0.75rem;
  }

  .tenant-layout__shell {
    grid-template-columns: 1fr;
  }
}
</style>
