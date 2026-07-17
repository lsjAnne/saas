<script setup lang="ts">
interface Props {
  eyebrow?: string;
  title: string;
  description?: string;
  dark?: boolean;
}

withDefaults(defineProps<Props>(), {
  eyebrow: '',
  description: '',
  dark: false
});
</script>

<template>
  <section :class="['panel-card', { 'panel-card--dark': dark }]">
    <header class="panel-card__header">
      <p v-if="eyebrow" class="panel-card__eyebrow">{{ eyebrow }}</p>
      <h3 class="panel-card__title">{{ title }}</h3>
      <p v-if="description" class="panel-card__description">{{ description }}</p>
    </header>
    <div class="panel-card__body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.panel-card {
  border-radius: var(--radius-xl);
  padding: 1.35rem;
  border: 1px solid var(--color-line);
  background: var(--color-panel);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(10px);
}

.panel-card--dark {
  background: linear-gradient(165deg, #382d24, #221912);
  border-color: rgba(255, 255, 255, 0.06);
}

.panel-card__header {
  display: grid;
  gap: 0.45rem;
}

.panel-card__eyebrow {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--color-ink-faint);
}

.panel-card__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.6rem;
  color: var(--color-ink-strong);
}

.panel-card--dark .panel-card__title {
  color: #fff8f0;
}

.panel-card__description {
  margin: 0;
  color: var(--color-ink-soft);
  line-height: 1.65;
}

.panel-card--dark .panel-card__description,
.panel-card--dark .panel-card__eyebrow {
  color: rgba(255, 244, 236, 0.72);
}

.panel-card__body {
  margin-top: 1.2rem;
}
</style>
