<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';

import { AUTH_UNAUTHORIZED_EVENT } from '@/services/http/apiClient';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const router = useRouter();

function handleUnauthorized() {
  authStore.clearSession();
  router.push('/login');
}

onMounted(() => {
  window.addEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized);
});

onUnmounted(() => {
  window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized);
});
</script>

<template>
  <RouterView />
</template>
