<script setup lang="ts">
interface Props {
  title?: string;
  message: string;
  retryLabel?: string;
}

withDefaults(defineProps<Props>(), {
  title: '暂时无法完成当前请求',
  retryLabel: '重试'
});

defineEmits<{
  retry: [];
}>();
</script>

<template>
  <div class="inline-error-card">
    <div class="inline-error-card__copy">
      <h4 class="inline-error-card__title">{{ title }}</h4>
      <p class="inline-error-card__message">{{ message }}</p>
    </div>
    <button class="inline-error-card__button" type="button" @click="$emit('retry')">
      {{ retryLabel }}
    </button>
  </div>
</template>

<style scoped>
.inline-error-card {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.125rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(161, 69, 52, 0.16);
  background: rgba(161, 69, 52, 0.06);
}

.inline-error-card__copy {
  display: grid;
  gap: 0.35rem;
}

.inline-error-card__title {
  margin: 0;
  color: var(--color-danger);
  font-size: 0.95rem;
}

.inline-error-card__message {
  margin: 0;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.inline-error-card__button {
  align-self: start;
  padding: 0.85rem 1rem;
  border: 0;
  border-radius: var(--radius-pill);
  background: rgba(161, 69, 52, 0.14);
  color: var(--color-danger);
}
</style>
