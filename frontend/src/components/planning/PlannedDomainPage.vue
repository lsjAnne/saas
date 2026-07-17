<script setup lang="ts">
import { computed } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';

interface MetricItem {
  label: string;
  value: string;
  caption?: string;
}

interface ModuleItem {
  title: string;
  description: string;
  phase?: string;
}

interface Props {
  eyebrow: string;
  title: string;
  subtitle: string;
  phaseLabel: string;
  phaseSummary: string;
  accentFrom: string;
  accentTo: string;
  heroTags: string[];
  metrics: MetricItem[];
  primaryModules: ModuleItem[];
  secondaryModules: ModuleItem[];
  interfaces: string[];
  deliveryNotes: string[];
}

const props = defineProps<Props>();

const heroStyle = computed(() => ({
  background: `linear-gradient(135deg, ${props.accentFrom}, ${props.accentTo})`
}));
</script>

<template>
  <section class="planned-domain-page">
    <header class="planned-domain-page__header">
      <p class="planned-domain-page__eyebrow">{{ eyebrow }}</p>
      <h2 class="planned-domain-page__title">{{ title }}</h2>
      <p class="planned-domain-page__subtitle">{{ subtitle }}</p>
    </header>

    <section class="planned-domain-page__hero" :style="heroStyle">
      <div class="planned-domain-page__hero-copy">
        <p class="planned-domain-page__phase-label">{{ phaseLabel }}</p>
        <p class="planned-domain-page__phase-summary">{{ phaseSummary }}</p>
      </div>

      <div class="planned-domain-page__tag-list">
        <span
          v-for="tag in heroTags"
          :key="tag"
          class="planned-domain-page__tag"
        >
          {{ tag }}
        </span>
      </div>
    </section>

    <section class="planned-domain-page__metrics">
      <MetricCard
        v-for="metric in metrics"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :caption="metric.caption"
      />
    </section>

    <div class="planned-domain-page__grid">
      <PanelCard
        eyebrow="Primary Modules"
        title="首屏模块"
        description="这些模块建议优先进入页面首屏，先保证主链路可看、可筛、可跳转。"
      >
        <div class="planned-domain-page__module-list">
          <article
            v-for="module in primaryModules"
            :key="module.title"
            class="planned-domain-page__module-card"
          >
            <div class="planned-domain-page__module-head">
              <h4 class="planned-domain-page__module-title">{{ module.title }}</h4>
              <span
                v-if="module.phase"
                class="planned-domain-page__module-phase"
              >
                {{ module.phase }}
              </span>
            </div>
            <p class="planned-domain-page__module-description">{{ module.description }}</p>
          </article>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Key Interfaces"
        title="关键接口"
        description="骨架页先把接口边界说清楚，后续接真数据时不需要再返工页面结构。"
      >
        <div class="planned-domain-page__interface-list">
          <span
            v-for="entry in interfaces"
            :key="entry"
            class="planned-domain-page__interface-chip"
          >
            {{ entry }}
          </span>
        </div>
      </PanelCard>
    </div>

    <div class="planned-domain-page__grid planned-domain-page__grid--secondary">
      <PanelCard
        eyebrow="Phase Two"
        title="二期扩展"
        description="这些能力建议在主链稳定后补齐，不阻塞当前导航和页面骨架先落地。"
      >
        <div class="planned-domain-page__module-list">
          <article
            v-for="module in secondaryModules"
            :key="module.title"
            class="planned-domain-page__module-card"
          >
            <div class="planned-domain-page__module-head">
              <h4 class="planned-domain-page__module-title">{{ module.title }}</h4>
              <span
                v-if="module.phase"
                class="planned-domain-page__module-phase"
              >
                {{ module.phase }}
              </span>
            </div>
            <p class="planned-domain-page__module-description">{{ module.description }}</p>
          </article>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Delivery Notes"
        title="开发提示"
        description="这些提示是给后续真实开发用的，避免骨架搭好后再整体返工。"
      >
        <ul class="planned-domain-page__note-list">
          <li
            v-for="note in deliveryNotes"
            :key="note"
            class="planned-domain-page__note-item"
          >
            {{ note }}
          </li>
        </ul>
      </PanelCard>
    </div>
  </section>
</template>

<style scoped>
.planned-domain-page {
  display: grid;
  gap: 1.2rem;
}

.planned-domain-page__header {
  display: grid;
  gap: 0.75rem;
}

.planned-domain-page__eyebrow,
.planned-domain-page__title,
.planned-domain-page__subtitle,
.planned-domain-page__phase-label,
.planned-domain-page__phase-summary {
  margin: 0;
}

.planned-domain-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.planned-domain-page__title {
  max-width: 14ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 3.7rem);
  line-height: 1.03;
}

.planned-domain-page__subtitle {
  max-width: 64rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.planned-domain-page__hero {
  display: grid;
  gap: 1.2rem;
  padding: 1.45rem 1.5rem;
  border-radius: var(--radius-shell);
  box-shadow: var(--shadow-card);
}

.planned-domain-page__hero-copy {
  display: grid;
  gap: 0.65rem;
}

.planned-domain-page__phase-label {
  color: rgba(255, 244, 236, 0.72);
  font-size: 0.78rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.planned-domain-page__phase-summary {
  max-width: 54rem;
  color: #fff7ef;
  font-family: var(--font-display);
  font-size: clamp(1.2rem, 2vw, 1.8rem);
  line-height: 1.5;
}

.planned-domain-page__tag-list,
.planned-domain-page__interface-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
}

.planned-domain-page__tag,
.planned-domain-page__interface-chip,
.planned-domain-page__module-phase {
  display: inline-flex;
  align-items: center;
  min-height: 2.15rem;
  padding: 0 0.82rem;
  border-radius: var(--radius-pill);
  font-size: 0.82rem;
}

.planned-domain-page__tag {
  color: #fff9f4;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.planned-domain-page__interface-chip {
  color: var(--color-ink);
  background: rgba(217, 192, 163, 0.22);
  border: 1px solid rgba(133, 98, 67, 0.14);
}

.planned-domain-page__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.9rem;
}

.planned-domain-page__grid {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  gap: 1rem;
}

.planned-domain-page__grid--secondary {
  grid-template-columns: 0.96fr 1.04fr;
}

.planned-domain-page__module-list,
.planned-domain-page__note-list {
  display: grid;
  gap: 0.8rem;
}

.planned-domain-page__module-card {
  display: grid;
  gap: 0.55rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.planned-domain-page__module-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.7rem;
}

.planned-domain-page__module-title,
.planned-domain-page__module-description {
  margin: 0;
}

.planned-domain-page__module-title {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.planned-domain-page__module-phase {
  color: var(--color-accent);
  background: rgba(217, 192, 163, 0.24);
}

.planned-domain-page__module-description,
.planned-domain-page__note-item {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.planned-domain-page__note-list {
  margin: 0;
  padding-left: 1rem;
}

@media (max-width: 1180px) {
  .planned-domain-page__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .planned-domain-page__grid,
  .planned-domain-page__grid--secondary {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .planned-domain-page__metrics {
    grid-template-columns: 1fr;
  }

  .planned-domain-page__hero {
    padding: 1.15rem;
  }
}
</style>
