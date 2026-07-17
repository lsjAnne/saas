import { computed, shallowRef } from 'vue';
import { defineStore } from 'pinia';

import { AUTH_STORAGE_KEY } from '@/services/http/apiClient';
import type { LoginPayload, LoginUser } from '@/services/apiTypes';

interface PersistedAuthState {
  token: string;
  user: LoginUser;
}

export const useAuthStore = defineStore('auth', () => {
  const token = shallowRef<string | null>(null);
  const user = shallowRef<LoginUser | null>(null);
  const hydrated = shallowRef(false);

  const tenantId = computed(() => user.value?.tenantId ?? '');
  const organizationId = computed(() => user.value?.organizationId ?? '');
  const permissionCodes = computed(() => user.value?.permissionCodes ?? []);
  const isAuthenticated = computed(() => Boolean(token.value && user.value));

  function hasPermission(permissionCode: string) {
    return permissionCodes.value.includes(permissionCode);
  }

  function hasAnyPermission(codes: string[] = []) {
    if (!codes.length) {
      return true;
    }

    return codes.some((code) => hasPermission(code));
  }

  function hasAllPermissions(codes: string[] = []) {
    if (!codes.length) {
      return true;
    }

    return codes.every((code) => hasPermission(code));
  }

  function hydrate() {
    if (hydrated.value) {
      return;
    }

    const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);

    if (raw) {
      try {
        const parsed = JSON.parse(raw) as PersistedAuthState;
        token.value = parsed.token;
        user.value = parsed.user;
      } catch {
        window.localStorage.removeItem(AUTH_STORAGE_KEY);
      }
    }

    hydrated.value = true;
  }

  function persistSession(payload: LoginPayload) {
    token.value = payload.token;
    user.value = payload.user;
    window.localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({
        token: payload.token,
        user: payload.user
      })
    );
  }

  function clearSession() {
    token.value = null;
    user.value = null;
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
  }

  return {
    token,
    user,
    tenantId,
    organizationId,
    permissionCodes,
    isAuthenticated,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    hydrate,
    persistSession,
    clearSession
  };
});
