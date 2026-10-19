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
</template>

<style scoped>
.tenant-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
}

.tenant-layout__main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background: var(--color-surface);
  min-width: 0;
}

.tenant-layout__content {
  padding: 1.4rem 1.6rem 2rem;
  min-width: 0;
}

@media (max-width: 960px) {
  .tenant-layout {
    grid-template-columns: 1fr;
  }

  .tenant-layout__content {
    padding: 1rem;
  }
}
</style>
