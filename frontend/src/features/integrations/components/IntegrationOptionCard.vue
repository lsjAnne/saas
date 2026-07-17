<script setup lang="ts">
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  ExternalIntegrationOption,
  IntegrationReadinessDescriptor
} from '@/services/apiTypes';

interface Props {
  option: ExternalIntegrationOption;
  selected: boolean;
  readiness: IntegrationReadinessDescriptor;
}

const props = defineProps<Props>();

defineEmits<{
  toggle: [systemCode: string];
}>();

function buildGlyph(systemCode: string) {
  return systemCode.slice(0, 3).toUpperCase();
}
</script>

<template>
  <article class="integration-card">
    <div class="integration-card__glyph">{{ buildGlyph(option.systemCode) }}</div>

    <div class="integration-card__copy">
      <div class="integration-card__title-row">
        <h4 class="integration-card__title">{{ option.displayName }}</h4>
        <StatusPill :label="readiness.label" :tone="readiness.tone" />
      </div>
      <p class="integration-card__description">
        {{ readiness.detail }}
      </p>
    </div>

    <button
      type="button"
      :class="['integration-card__toggle', { 'integration-card__toggle--on': selected }]"
      @click="$emit('toggle', option.systemCode)"
    >
      <span class="integration-card__knob" />
    </button>
  </article>
</template>

<style scoped>
.integration-card {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr) auto;
  gap: 1rem;
  align-items: center;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.84);
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.integration-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 26px rgba(58, 43, 28, 0.08);
}

.integration-card__glyph {
  width: 60px;
  height: 60px;
  display: grid;
  place-items: center;
  border-radius: 1rem;
  background: linear-gradient(135deg, #efe2d1, #d9c0a3);
  color: #513f2e;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.integration-card__copy {
  display: grid;
  gap: 0.45rem;
}

.integration-card__title-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.75rem;
}

.integration-card__title {
  margin: 0;
  font-size: 1rem;
  color: var(--color-ink-strong);
}

.integration-card__description {
  margin: 0;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.integration-card__toggle {
  position: relative;
  width: 58px;
  height: 32px;
  border: 0;
  border-radius: var(--radius-pill);
  background: rgba(121, 101, 83, 0.18);
  transition: background var(--transition-fast);
}

.integration-card__toggle--on {
  background: linear-gradient(135deg, #2e251d, #866444);
}

.integration-card__knob {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #fffdf8;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.18);
  transition: transform var(--transition-fast);
}

.integration-card__toggle--on .integration-card__knob {
  transform: translateX(26px);
}
</style>
