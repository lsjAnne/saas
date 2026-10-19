<script setup lang="ts">
import { computed, reactive } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import type {
  RegisterTenantRequest,
  SubscriptionPlan
} from '@/services/apiTypes';
import type { LoginRequest } from '@/services/authService';

interface Props {
  plans: SubscriptionPlan[];
  selectedPlanCode: string;
  isLoggingIn: boolean;
  isRegistering: boolean;
  feedbackMessage?: string | null;
  errorMessage?: string | null;
  defaultBootstrapPassword: string;
}

const props = withDefaults(defineProps<Props>(), {
  feedbackMessage: null,
  errorMessage: null
});

const emit = defineEmits<{
  selectPlan: [planCode: string];
  login: [payload: LoginRequest];
  register: [payload: RegisterTenantRequest];
}>();

const loginForm = reactive({
  username: '',
  password: ''
});

const registerForm = reactive({
  tenantName: '',
  ownerName: '',
  mobile: ''
});

const selectedPlan = computed(
  () => props.plans.find((plan) => plan.planCode === props.selectedPlanCode) ?? null
);

function formatPrice(value: number) {
  if (!value) {
    return '免费';
  }

  return `¥${value.toLocaleString('zh-CN')}`;
}

function handleLogin() {
  emit('login', {
    username: loginForm.username.trim(),
    password: loginForm.password
  });
}

function handleRegister() {
  emit('register', {
    tenantName: registerForm.tenantName.trim(),
    ownerName: registerForm.ownerName.trim(),
    mobile: registerForm.mobile.trim()
  });
}
</script>

<template>
  <section class="onboarding-access">
    <PanelCard
      title="登录与开通"
      description="先选定试跑套餐，再登录已有租户或现场开通试用。"
    >
      <div class="onboarding-access__hero">
        <div>
          <p class="onboarding-access__hero-title">当前选择：{{ selectedPlan?.planName || '未选择套餐' }}</p>
          <p class="onboarding-access__hero-copy">
            {{
              selectedPlan?.planCode === 'trial'
                ? '保持试用版会在注册后直接进入 14 天试跑期。'
                : `注册完成后会自动登录，并立即切换到 ${selectedPlan?.planName}。`
            }}
          </p>
        </div>
        <p class="onboarding-access__hero-note">
          试用租户默认初始密码：{{ defaultBootstrapPassword }}
        </p>
      </div>

      <div class="onboarding-access__plans">
        <button
          v-for="plan in plans"
          :key="plan.planCode"
          type="button"
          :class="[
            'onboarding-access__plan',
            { 'onboarding-access__plan--active': plan.planCode === selectedPlanCode }
          ]"
          @click="emit('selectPlan', plan.planCode)"
        >
          <div class="onboarding-access__plan-head">
            <strong class="onboarding-access__plan-title">{{ plan.planName }}</strong>
            <span class="onboarding-access__plan-tag">
              {{ plan.planCode === selectedPlanCode ? '已选中' : '点击选用' }}
            </span>
          </div>
          <p class="onboarding-access__plan-price">
            {{ formatPrice(plan.monthlyPrice) }}
            <span>/ 月</span>
          </p>
          <p class="onboarding-access__plan-meta">
            年付 {{ formatPrice(plan.yearlyPrice) }} · 席位上限 {{ plan.seatLimit }}
          </p>
        </button>
      </div>
    </PanelCard>

    <div
      v-if="feedbackMessage || errorMessage"
      :class="[
        'onboarding-access__banner',
        { 'onboarding-access__banner--error': errorMessage }
      ]"
    >
      {{ errorMessage || feedbackMessage }}
    </div>

    <div class="onboarding-access__grid">
      <PanelCard
        title="登录已有租户"
        description="已有租户可直接登录并留在此页继续补齐套餐、店铺和初始化动作。"
      >
        <form class="onboarding-access__form" @submit.prevent="handleLogin">
          <label class="onboarding-access__field">
            <span>账号</span>
            <input
              v-model="loginForm.username"
              type="text"
              autocomplete="username"
              placeholder="手机号或账号"
              required
            />
          </label>
          <label class="onboarding-access__field">
            <span>密码</span>
            <input
              v-model="loginForm.password"
              type="password"
              autocomplete="current-password"
              placeholder="输入登录密码"
              required
            />
          </label>
          <button class="onboarding-access__primary" type="submit" :disabled="isLoggingIn">
            {{ isLoggingIn ? '登录中...' : '登录后继续初始化' }}
          </button>
        </form>
      </PanelCard>

      <PanelCard
        title="现场开通试用租户"
        description="注册后会自动创建默认组织，并用默认密码直接登录回接入工作台。"
      >
        <form class="onboarding-access__form" @submit.prevent="handleRegister">
          <label class="onboarding-access__field">
            <span>租户名称</span>
            <input
              v-model="registerForm.tenantName"
              type="text"
              placeholder="例如：星澜电商"
              required
            />
          </label>
          <label class="onboarding-access__field">
            <span>负责人</span>
            <input
              v-model="registerForm.ownerName"
              type="text"
              placeholder="输入负责人姓名"
              required
            />
          </label>
          <label class="onboarding-access__field">
            <span>手机号</span>
            <input
              v-model="registerForm.mobile"
              type="text"
              inputmode="tel"
              placeholder="输入租户管理员手机号"
              required
            />
          </label>
          <p class="onboarding-access__tip">
            当前套餐：{{ selectedPlan?.planName || '试用版' }}。注册完成后将用默认密码
            {{ defaultBootstrapPassword }} 自动登录。
          </p>
          <button class="onboarding-access__primary" type="submit" :disabled="isRegistering">
            {{ isRegistering ? '开通中...' : '开通并进入工作台' }}
          </button>
        </form>
      </PanelCard>
    </div>
  </section>
</template>

<style scoped>
.onboarding-access {
  display: grid;
  gap: 1rem;
}

.onboarding-access__hero,
.onboarding-access__plan-head,
.onboarding-access__grid,
.onboarding-access__form,
.onboarding-access__field {
  display: grid;
  gap: 0.8rem;
}

.onboarding-access__hero {
  grid-template-columns: 1.1fr 0.9fr;
  align-items: start;
}

.onboarding-access__hero-title,
.onboarding-access__hero-copy,
.onboarding-access__hero-note,
.onboarding-access__plan-price,
.onboarding-access__plan-meta,
.onboarding-access__tip,
.onboarding-access__banner {
  margin: 0;
}

.onboarding-access__hero-title {
  color: var(--color-ink-strong);
  font-size: 1.25rem;
  font-weight: 700;
}

.onboarding-access__hero-copy,
.onboarding-access__hero-note {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.onboarding-access__hero-note {
  padding: 1rem 1.1rem;
  border-radius: var(--radius-lg);
  background: var(--color-accent-soft);
}

.onboarding-access__plans {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.9rem;
  margin-top: 1rem;
}

.onboarding-access__plan {
  display: grid;
  gap: 0.7rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
  color: var(--color-ink-strong);
  text-align: left;
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    background var(--transition-fast),
    transform var(--transition-fast);
}

.onboarding-access__plan--active {
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
  transform: translateY(-2px);
}

.onboarding-access__plan-title {
  font-size: 1rem;
}

.onboarding-access__plan-tag,
.onboarding-access__plan-meta,
.onboarding-access__tip {
  color: var(--color-ink-soft);
  font-size: 0.86rem;
  line-height: 1.65;
}

.onboarding-access__plan-price {
  color: var(--color-ink-strong);
  font-size: 1.6rem;
  font-weight: 700;
}

.onboarding-access__plan-price span {
  font-size: 0.88rem;
  font-weight: 500;
}

.onboarding-access__banner {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(47, 109, 83, 0.14);
  background: rgba(47, 109, 83, 0.1);
  color: var(--color-success);
}

.onboarding-access__banner--error {
  border-color: rgba(183, 117, 47, 0.2);
  background: rgba(183, 117, 47, 0.12);
  color: var(--color-warning);
}

.onboarding-access__grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
}

.onboarding-access__field span {
  color: var(--color-ink-soft);
  font-size: 0.84rem;
}

.onboarding-access__field input {
  min-height: 2.75rem;
  padding: 0 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-line);
  background: #fff;
  color: var(--color-ink-strong);
  font: inherit;
}

.onboarding-access__primary {
  min-height: 2.5rem;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--color-accent);
  color: #ffffff;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.onboarding-access__primary:hover {
  background: var(--color-accent-hover);
  color: #ffffff;
}

.onboarding-access__primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 1080px) {
  .onboarding-access__plans,
  .onboarding-access__grid,
  .onboarding-access__hero {
    grid-template-columns: 1fr;
  }
}
</style>
