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
      <p class="login-page__eyebrow">接入开通</p>
      <h1 class="login-page__title">
        接入工作台
      </h1>
      <p class="login-page__subtitle">
        登录、开通与店铺接入一站式完成
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
    radial-gradient(circle at top left, var(--color-accent-soft), transparent 26%),
    var(--color-surface);
}

.login-page__header {
  display: grid;
  gap: 0.35rem;
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
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.login-page__title {
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 600;
}

.login-page__subtitle,
.login-page__booting {
  color: var(--color-ink-soft);
  line-height: 1.6;
}
</style>
