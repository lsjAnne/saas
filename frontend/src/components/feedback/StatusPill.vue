<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  label: string;
  tone?: 'neutral' | 'warn' | 'success';
}

const props = withDefaults(defineProps<Props>(), {
  tone: 'neutral'
});

const classes = computed(() => [
  'status-pill',
  `status-pill--${props.tone}`
]);
</script>

<template>
  <span :class="classes">{{ label }}</span>
</template>

<style scoped>
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 2rem;
  padding: 0 0.9rem;
  border-radius: var(--radius-pill);
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.status-pill--neutral {
  background: rgba(132, 108, 85, 0.12);
  color: var(--color-ink);
}

.status-pill--warn {
  background: rgba(183, 117, 47, 0.14);
  color: var(--color-warning);
}

.status-pill--success {
  background: rgba(47, 109, 83, 0.13);
  color: var(--color-success);
}
</style>
