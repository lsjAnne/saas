<script setup lang="ts">
import { shallowRef } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ContentAssetVersion } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  versions: ContentAssetVersion[];
  assetStatus: string | null;
  isSubmitting: boolean;
  actionError?: string | null;
}

interface ArtifactInfo {
  resourceType: string;
  assetId: string | null;
  params: string[];
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<{
  archive: [remark: string];
}>();

const remark = shallowRef('');

function submitArchive() {
  emit('archive', remark.value.trim());
}

function resolveTone(status: string) {
  return status === 'archived' ? 'neutral' : 'success';
}

function resolveStage(version: ContentAssetVersion) {
  const summary = `${version.changeSummary ?? ''}\n${version.contentSnapshot ?? ''}`.toLowerCase();

  if (summary.includes('published to') || summary.includes('publish package')) {
    return '发布包';
  }
  if (summary.includes('edited-cut') || summary.includes('edited cut ready')) {
    return '剪辑成片';
  }
  if (summary.includes('storyboard')) {
    return '分镜草稿';
  }
  return version.versionStatus === 'archived' ? '归档快照' : '当前快照';
}

function splitSnapshotLines(value: string | null | undefined) {
  return (value ?? '')
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function parseArtifact(url: string | null | undefined): ArtifactInfo | null {
  const value = (url ?? '').trim();
  const matched = value.match(/^generated:\/\/([^/?]+)(?:\/([^?]+))?(?:\?(.*))?$/);

  if (!matched) {
    return null;
  }

  const [, resourceType, assetId, query] = matched;
  return {
    resourceType,
    assetId: assetId ?? null,
    params: (query ?? '')
      .split('&')
      .map((entry) => entry.trim())
      .filter(Boolean)
      .map((entry) => entry.replace('=', ': '))
  };
}
</script>

<template>
  <PanelCard
    eyebrow="Version Ledger"
    title="版本记录与剪辑时间线"
    description="在这里确认 storyboard、剪辑成片和发布版本的顺序，再决定是否归档留痕。"
  >
    <div class="version-panel__actions">
      <input
        v-model="remark"
        class="version-panel__remark"
        type="text"
        placeholder="归档备注，例如：发布验收完成后归档"
      />
      <button
        class="version-panel__button"
        type="button"
        :disabled="!versions.length || assetStatus === 'archived' || isSubmitting"
        @click="submitArchive"
      >
        归档版本
      </button>
    </div>

    <p v-if="actionError" class="version-panel__error">{{ actionError }}</p>

    <div v-if="versions.length" class="version-panel__timeline">
      <article v-for="version in versions" :key="version.versionId" class="version-panel__item">
        <div class="version-panel__item-head">
          <div>
            <p class="version-panel__label">{{ version.versionLabel }}</p>
            <p class="version-panel__time">{{ formatDateTime(version.createdAt) }}</p>
          </div>
          <StatusPill :label="version.versionStatus" :tone="resolveTone(version.versionStatus)" />
        </div>

        <div class="version-panel__chip-row">
          <span class="version-panel__chip">{{ resolveStage(version) }}</span>
          <span v-if="parseArtifact(version.previewUrl)?.resourceType" class="version-panel__chip">
            {{ parseArtifact(version.previewUrl)?.resourceType }}
          </span>
          <span v-if="parseArtifact(version.previewUrl)?.assetId" class="version-panel__chip">
            {{ parseArtifact(version.previewUrl)?.assetId }}
          </span>
        </div>

        <p class="version-panel__summary">
          {{ version.changeSummary || version.contentSnapshot || '当前版本未补充变更说明。' }}
        </p>

        <ul v-if="splitSnapshotLines(version.contentSnapshot).length" class="version-panel__snapshot-list">
          <li v-for="line in splitSnapshotLines(version.contentSnapshot).slice(0, 5)" :key="line">{{ line }}</li>
        </ul>

        <p v-if="version.previewUrl" class="version-panel__artifact">
          产物：{{ version.previewUrl }}
        </p>
      </article>
    </div>

    <div v-else class="version-panel__empty">
      还没有版本记录，先在上方上传素材或生成视频成片。
    </div>
  </PanelCard>
</template>

<style scoped>
.version-panel__actions {
  display: flex;
  gap: 0.8rem;
  align-items: center;
}

.version-panel__remark {
  flex: 1;
  min-height: 2.9rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.76);
  color: var(--color-ink-strong);
}

.version-panel__button {
  min-height: 2.9rem;
  padding: 0 1.1rem;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: var(--color-accent);
  color: #fff7f0;
}

.version-panel__button:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.version-panel__error,
.version-panel__label,
.version-panel__time,
.version-panel__summary,
.version-panel__empty,
.version-panel__artifact {
  margin: 0;
}

.version-panel__error {
  margin-top: 0.8rem;
  color: var(--color-danger);
}

.version-panel__timeline {
  display: grid;
  gap: 0.8rem;
  margin-top: 0.9rem;
}

.version-panel__item {
  padding: 1rem;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.version-panel__item-head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.version-panel__label {
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: 1.18rem;
}

.version-panel__time,
.version-panel__artifact {
  margin-top: 0.42rem;
  color: var(--color-ink-faint);
  font-size: 0.82rem;
}

.version-panel__chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-top: 0.75rem;
}

.version-panel__chip {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(91, 72, 54, 0.08);
  color: var(--color-ink-strong);
  font-size: 0.8rem;
}

.version-panel__summary {
  margin-top: 0.75rem;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.version-panel__snapshot-list {
  margin: 0.7rem 0 0;
  padding-left: 1.1rem;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.version-panel__empty {
  margin-top: 0.9rem;
  padding: 1rem;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.56);
  color: var(--color-ink-soft);
}

@media (max-width: 720px) {
  .version-panel__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
