<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { resolveFirstAccessibleConsolePath } from '@/app/router';
import OnboardingAccessPanel from '@/features/onboarding/components/OnboardingAccessPanel.vue';
import OnboardingWorkspacePanel from '@/features/onboarding/components/OnboardingWorkspacePanel.vue';
import { useOnboardingWorkspace } from '@/features/onboarding/composables/useOnboardingWorkspace';

const route = useRoute();
const router = useRouter();
const onboardingWorkspace = useOnboardingWorkspace();

const redirectPath = computed(() =>
  typeof route.query.redirect === 'string' ? route.query.redirect : ''
);

function resolveConsoleTarget() {
  const redirect = redirectPath.value.trim();

  if (redirect.startsWith('/app')) {
    return redirect;
  }

  return resolveFirstAccessibleConsolePath();
}

async function handleEnterConsole() {
  await router.push(resolveConsoleTarget());
}

function swallowError() {
  return undefined;
}

function handleRefreshWorkspace() {
  onboardingWorkspace.loadWorkspace().catch(swallowError);
}

function handleSignIn(payload: { username: string; password: string }) {
  onboardingWorkspace.signIn(payload).catch(swallowError);
}

function handleRegister(payload: {
  tenantName: string;
  ownerName: string;
  mobile: string;
}) {
  onboardingWorkspace.registerAndStart(payload).catch(swallowError);
}

function handleChangePlan(planCode: string) {
  onboardingWorkspace.changePlan(planCode).catch(swallowError);
}

function handleConnectStore(payload: {
  organizationId: string;
  ownerUserId: string;
  platformType: string;
  platformShopId: string;
  shopName: string;
  profitThreshold: number;
  riskThreshold: number;
  defaultShipConfig?: Record<string, unknown>;
}) {
  onboardingWorkspace.submitStoreConnection(payload).catch(swallowError);
}

function handleSaveSettings(payload: {
  profitThreshold: number;
  riskThreshold: number;
  defaultShipConfig?: Record<string, unknown>;
}) {
  onboardingWorkspace.submitStoreSettings(payload).catch(swallowError);
}

onMounted(() => {
  onboardingWorkspace.bootstrap().catch(swallowError);
});
</script>

<template>
  <section class="login-page">
    <header class="login-page__header">
      <p class="login-page__eyebrow">Tenant Access & Store Onboarding</p>
      <h1 class="login-page__title">
        把登录页改造成租户接入工作台，而不是一次性跳板
      </h1>
      <p class="login-page__subtitle">
        未登录时在这里完成试用开通与套餐选择，已登录后继续补齐默认组织、店铺授权和初始化设置，最后再进入控制台。
      </p>
    </header>

    <p v-if="onboardingWorkspace.isBootstrapping" class="login-page__booting">
      正在加载套餐目录与当前租户上下文...
    </p>

    <OnboardingWorkspacePanel
      v-else-if="onboardingWorkspace.isAuthenticated"
      :registration="onboardingWorkspace.registration"
      :context="onboardingWorkspace.context"
      :subscription="onboardingWorkspace.subscription"
      :quotas="onboardingWorkspace.quotas"
      :plans="onboardingWorkspace.plans"
      :stores="onboardingWorkspace.stores"
      :selected-store-id="onboardingWorkspace.selectedStoreId"
      :selected-store="onboardingWorkspace.selectedStore"
      :default-organization-id="onboardingWorkspace.defaultOrganizationId"
      :current-operator-id="onboardingWorkspace.currentOperatorId"
      :is-refreshing="onboardingWorkspace.isRefreshingWorkspace"
      :is-updating-plan="onboardingWorkspace.isUpdatingPlan"
      :is-connecting-store="onboardingWorkspace.isConnectingStore"
      :is-saving-settings="onboardingWorkspace.isSavingSettings"
      :feedback-message="onboardingWorkspace.workspaceFeedback"
      :error-message="onboardingWorkspace.workspaceError"
      @refresh="handleRefreshWorkspace"
      @enter-console="handleEnterConsole"
      @logout="onboardingWorkspace.signOut"
      @select-plan="handleChangePlan"
      @select-store="onboardingWorkspace.selectStore"
      @connect-store="handleConnectStore"
      @save-settings="handleSaveSettings"
    />

    <OnboardingAccessPanel
      v-else
      :plans="onboardingWorkspace.plans"
      :selected-plan-code="onboardingWorkspace.selectedPlanCode"
      :is-logging-in="onboardingWorkspace.isLoggingIn"
      :is-registering="onboardingWorkspace.isRegistering"
      :feedback-message="onboardingWorkspace.accessFeedback"
      :error-message="onboardingWorkspace.accessError"
      :default-bootstrap-password="onboardingWorkspace.defaultBootstrapPassword"
      @select-plan="onboardingWorkspace.selectPlan"
      @login="handleSignIn"
      @register="handleRegister"
    />
  </section>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  gap: 1.2rem;
  padding: clamp(1.2rem, 3vw, 2rem);
  background:
    radial-gradient(circle at top left, rgba(216, 192, 163, 0.34), transparent 22%),
    radial-gradient(circle at bottom right, rgba(45, 36, 28, 0.08), transparent 28%),
    linear-gradient(145deg, #f7efe6, #f1e6da 52%, #fbf7f2);
}

.login-page__header {
  display: grid;
  gap: 0.8rem;
  width: min(100%, 1320px);
  margin: 0 auto;
}

.login-page__eyebrow,
.login-page__title,
.login-page__subtitle,
.login-page__booting {
  margin: 0;
}

.login-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.78rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.login-page__title {
  max-width: 14ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.4rem, 4vw, 4.6rem);
  line-height: 0.98;
}

.login-page__subtitle,
.login-page__booting {
  max-width: 68rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}
</style>
