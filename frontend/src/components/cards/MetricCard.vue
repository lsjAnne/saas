<script setup lang="ts">
interface Props {
  label: string;
  value: string;
  caption?: string;
}

withDefaults(defineProps<Props>(), {
  caption: ''
});
</script>

<template>
  <article class="metric-card">
    <p class="metric-card__label">{{ label }}</p>
    <h4 class="metric-card__value">{{ value }}</h4>
    <p v-if="caption" class="metric-card__caption">{{ caption }}</p>
  </article>
</template>

<style scoped>
.metric-card {
  border-radius: var(--radius-lg);
  padding: 1rem 1.05rem;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.metric-card__label {
  margin: 0 0 0.55rem;
  font-size: 0.74rem;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--color-ink-faint);
}

.metric-card__value {
  margin: 0;
  font-size: 1.7rem;
  color: var(--color-ink-strong);
}

.metric-card__caption {
  margin: 0.55rem 0 0;
  color: var(--color-ink-soft);
  line-height: 1.55;
}
</style>
