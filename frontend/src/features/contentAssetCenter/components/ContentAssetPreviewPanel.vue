<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ContentAssetDetailView, ContentAssetVersion } from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  detail: ContentAssetDetailView | null;
  storeName: string;
  isLoading: boolean;
}

interface PreviewArtifactInfo {
  resourceType: string;
  assetId: string | null;
  params: Array<{ label: string; value: string }>;
}

const props = defineProps<Props>();

const previewMode = computed(() => props.detail?.asset.previewMode ?? 'text');
const previewUrl = computed(() => props.detail?.asset.previewUrl ?? '');
const hasRemotePreview = computed(() => /^https?:\/\//.test(previewUrl.value));
const latestVersion = computed(() => props.detail?.versions[0] ?? null);
const activeVersion = computed(
  () => props.detail?.versions.find((version) => version.versionStatus === 'active') ?? latestVersion.value
);

const sourceAssetReference = computed(() =>
  props.detail?.references.find((reference) => reference.referenceType === 'source_asset') ?? null
);
const clipTemplateReference = computed(() =>
  props.detail?.references.find((reference) => reference.referenceType === 'clip_template') ?? null
);
const plannedPublishReference = computed(() =>
  props.detail?.references.find((reference) => reference.referenceType === 'publish_target') ?? null
);
const latestPublishReference = computed(() =>
  props.detail?.references.find((reference) => reference.referenceType === 'publish') ?? null
);

function buildLabel(key: string) {
  return (
    {
      source: '源素材',
      template: '剪辑模板',
      target: '目标平台',
      duration: '时长'
    } satisfies Record<string, string>
  )[key] ?? key;
}

function parseGeneratedPreview(url: string | null | undefined): PreviewArtifactInfo | null {
  const value = (url ?? '').trim();
  const matched = value.match(/^generated:\/\/([^/?]+)(?:\/([^?]+))?(?:\?(.*))?$/);

  if (!matched) {
    return null;
  }

  const [, resourceType, assetId, query] = matched;
  const params = (query ?? '')
    .split('&')
    .map((entry) => entry.trim())
    .filter(Boolean)
    .map((entry) => {
      const [rawKey, rawValue] = entry.split('=');
      return {
        label: buildLabel(rawKey ?? ''),
        value: decodeURIComponent(rawValue ?? '')
      };
    })
    .filter((entry) => entry.value);

  return {
    resourceType,
    assetId: assetId ?? null,
    params
  };
}

function summarizeVersionStage(version: ContentAssetVersion | null) {
  if (!version) {
    return '待补齐预览';
  }

  const summary = `${version.changeSummary ?? ''}\n${version.contentSnapshot ?? ''}`.toLowerCase();
  if (summary.includes('published to') || summary.includes('publish package')) {
    return '已进入发布包';
  }
  if (summary.includes('edited-cut') || summary.includes('edited cut ready')) {
    return '已进入剪辑成片';
  }
  if (summary.includes('storyboard')) {
    return '已生成分镜草稿';
  }
  return version.versionStatus === 'archived' ? '历史归档版本' : '当前预览版本';
}

function splitSnapshotLines(value: string | null | undefined) {
  return (value ?? '')
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

const previewArtifactInfo = computed(() => parseGeneratedPreview(previewUrl.value));
const previewArtifact = computed(() => previewUrl.value || '--');
const previewStatusLabel = computed(() => {
  if (!props.detail) {
    return '未选择';
  }

  if (props.detail.asset.assetStatus === 'archived') {
    return '归档预览';
  }

  if (latestPublishReference.value) {
    return '已发布成片';
  }

  if (props.detail.asset.assetType === 'video') {
    return '待发布成片';
  }

  return props.detail.asset.generated ? '生成预览' : '原始预览';
});

const previewNarrative = computed(() => {
  const detail = props.detail;
  if (!detail) {
    return '';
  }

  if (detail.asset.previewText) {
    return detail.asset.previewText;
  }

  if (activeVersion.value?.contentSnapshot) {
    return activeVersion.value.contentSnapshot;
  }

  return '当前素材还没有补齐预览说明。';
});

const activeSnapshotLines = computed(() => splitSnapshotLines(activeVersion.value?.contentSnapshot));
const currentVersionLabel = computed(() => activeVersion.value?.versionLabel ?? '--');
const currentStageLabel = computed(() => summarizeVersionStage(activeVersion.value));
</script>

<template>
  <PanelCard
    eyebrow="Preview Stage"
    title="预览区与素材流转"
    description="在一个视图里同时看素材预览、当前成片阶段、剪辑来源和发布回写，不再来回切页确认。"
    :dark="Boolean(detail)"
  >
    <div v-if="detail" class="preview-panel">
      <div class="preview-panel__frame">
        <div v-if="previewMode === 'image' && hasRemotePreview" class="preview-panel__image-shell">
          <img :src="previewUrl" :alt="detail.asset.assetName" class="preview-panel__image" />
        </div>

        <video
          v-else-if="previewMode === 'video' && hasRemotePreview"
          class="preview-panel__video"
          :src="previewUrl"
          controls
          playsinline
        />

        <div v-else-if="previewMode === 'video'" class="preview-panel__placeholder">
          <span class="preview-panel__badge">Video Workflow</span>
          <h4 class="preview-panel__placeholder-title">{{ detail.asset.assetName }}</h4>
          <p class="preview-panel__placeholder-copy">{{ previewNarrative }}</p>

          <div class="preview-panel__chip-row">
            <span class="preview-panel__chip">{{ currentVersionLabel }}</span>
            <span class="preview-panel__chip">{{ currentStageLabel }}</span>
            <span v-if="previewArtifactInfo?.resourceType" class="preview-panel__chip">
              {{ previewArtifactInfo.resourceType }}
            </span>
          </div>

          <dl v-if="previewArtifactInfo?.params.length" class="preview-panel__detail-grid">
            <div
              v-for="entry in previewArtifactInfo.params"
              :key="`${entry.label}-${entry.value}`"
              class="preview-panel__detail-item"
            >
              <dt>{{ entry.label }}</dt>
              <dd>{{ entry.value }}</dd>
            </div>
          </dl>

          <ul v-if="activeSnapshotLines.length" class="preview-panel__snapshot-list">
            <li v-for="line in activeSnapshotLines.slice(0, 5)" :key="line">{{ line }}</li>
          </ul>
        </div>

        <div v-else class="preview-panel__placeholder preview-panel__placeholder--text">
          <span class="preview-panel__badge">Text Snapshot</span>
          <h4 class="preview-panel__placeholder-title">{{ detail.asset.assetName }}</h4>
          <p class="preview-panel__placeholder-copy">{{ previewNarrative }}</p>

          <pre v-if="activeVersion?.contentSnapshot" class="preview-panel__snippet">{{
            activeVersion.contentSnapshot
          }}</pre>
        </div>
      </div>

      <div class="preview-panel__meta">
        <div class="preview-panel__meta-head">
          <div>
            <p class="preview-panel__meta-label">素材摘要</p>
            <h4 class="preview-panel__meta-title">{{ detail.asset.assetName }}</h4>
          </div>
          <StatusPill
            :label="previewStatusLabel"
            :tone="detail.asset.assetStatus === 'archived' ? 'neutral' : 'success'"
          />
        </div>

        <div class="preview-panel__meta-grid">
          <article class="preview-panel__meta-item">
            <p class="preview-panel__meta-item-label">归属店铺</p>
            <p class="preview-panel__meta-item-value">{{ storeName }}</p>
          </article>
          <article class="preview-panel__meta-item">
            <p class="preview-panel__meta-item-label">当前版本</p>
            <p class="preview-panel__meta-item-value">{{ currentVersionLabel }}</p>
          </article>
          <article class="preview-panel__meta-item">
            <p class="preview-panel__meta-item-label">视频源素材</p>
            <p class="preview-panel__meta-item-value">{{ sourceAssetReference?.referenceTargetId || '--' }}</p>
          </article>
          <article class="preview-panel__meta-item">
            <p class="preview-panel__meta-item-label">剪辑模板</p>
            <p class="preview-panel__meta-item-value">{{ clipTemplateReference?.referenceTargetId || '--' }}</p>
          </article>
          <article class="preview-panel__meta-item">
            <p class="preview-panel__meta-item-label">最新发布</p>
            <p class="preview-panel__meta-item-value">
              {{ latestPublishReference?.referenceTargetId || plannedPublishReference?.referenceTargetId || '--' }}
            </p>
          </article>
          <article class="preview-panel__meta-item">
            <p class="preview-panel__meta-item-label">生成产物</p>
            <p class="preview-panel__meta-item-value">{{ previewArtifact }}</p>
          </article>
        </div>

        <p class="preview-panel__note">
          {{ detail.asset.previewText || '当前素材未补充说明，建议在上传、生成或发布时补齐备注。' }}
        </p>
        <p class="preview-panel__note">最近更新：{{ formatDateTime(detail.asset.updatedAt) }}</p>
      </div>
    </div>

    <div v-else class="preview-panel__empty">
      <p class="preview-panel__empty-title">
        {{ isLoading ? '正在联动预览区...' : '左侧选择一个素材后，这里会展示预览和视频流转状态。' }}
      </p>
      <p class="preview-panel__empty-copy">
        商品图生成视频、剪辑模板和发布回写都会汇总在这里，方便直接判断是否可以进入下一步。
      </p>
    </div>
  </PanelCard>
</template>

<style scoped>
.preview-panel {
  display: grid;
  gap: 1rem;
}

.preview-panel__frame {
  min-height: 22rem;
  padding: 1rem;
  border-radius: var(--radius-xl);
  background:
    radial-gradient(circle at top left, rgba(204, 119, 65, 0.18), transparent 26%),
    linear-gradient(160deg, rgba(19, 25, 28, 0.98), rgba(30, 41, 45, 0.98));
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.preview-panel__image-shell,
.preview-panel__placeholder,
.preview-panel__video {
  display: grid;
  min-height: 100%;
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.04);
}

.preview-panel__image-shell {
  place-items: center;
}

.preview-panel__image {
  display: block;
  width: 100%;
  min-height: 20rem;
  object-fit: cover;
}

.preview-panel__video {
  width: 100%;
  object-fit: cover;
}

.preview-panel__placeholder {
  align-content: center;
  justify-items: start;
  gap: 0.9rem;
  padding: 1.4rem;
}

.preview-panel__placeholder--text {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.08), rgba(214, 170, 126, 0.08));
}

.preview-panel__badge {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(242, 245, 245, 0.82);
  font-size: 0.76rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.preview-panel__placeholder-title,
.preview-panel__placeholder-copy,
.preview-panel__meta-label,
.preview-panel__meta-title,
.preview-panel__meta-item-label,
.preview-panel__meta-item-value,
.preview-panel__note,
.preview-panel__empty-title,
.preview-panel__empty-copy,
.preview-panel__snippet,
.preview-panel__detail-item dt,
.preview-panel__detail-item dd {
  margin: 0;
}

.preview-panel__placeholder-title,
.preview-panel__meta-title,
.preview-panel__empty-title,
.preview-panel__detail-item dd {
  color: #fff6ef;
  font-family: var(--font-display);
}

.preview-panel__placeholder-title {
  font-size: clamp(1.6rem, 3vw, 2.4rem);
}

.preview-panel__placeholder-copy,
.preview-panel__snapshot-list,
.preview-panel__detail-item dt {
  color: rgba(242, 245, 245, 0.76);
  line-height: 1.75;
}

.preview-panel__chip-row,
.preview-panel__detail-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.preview-panel__chip {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.08);
  color: #fff6ef;
  font-size: 0.8rem;
}

.preview-panel__detail-grid {
  width: 100%;
}

.preview-panel__detail-item {
  min-width: 10rem;
  display: grid;
  gap: 0.22rem;
  padding: 0.8rem 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.08);
}

.preview-panel__detail-item dt {
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.preview-panel__snapshot-list {
  margin: 0;
  padding-left: 1.1rem;
}

.preview-panel__snippet {
  width: 100%;
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(242, 245, 245, 0.82);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'IBM Plex Mono', monospace;
  line-height: 1.68;
}

.preview-panel__meta {
  display: grid;
  gap: 0.9rem;
}

.preview-panel__meta-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.preview-panel__meta-label {
  color: rgba(239, 244, 244, 0.66);
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.preview-panel__meta-title {
  margin-top: 0.45rem;
  font-size: 1.5rem;
}

.preview-panel__meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.preview-panel__meta-item {
  padding: 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.07);
}

.preview-panel__meta-item-label {
  color: rgba(239, 244, 244, 0.6);
  font-size: 0.76rem;
}

.preview-panel__meta-item-value {
  margin-top: 0.55rem;
  color: #fff6ef;
  line-height: 1.55;
  word-break: break-word;
}

.preview-panel__note {
  color: rgba(239, 244, 244, 0.8);
  line-height: 1.75;
}

.preview-panel__empty {
  padding: 1.1rem;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.08);
  border: 1px dashed rgba(255, 255, 255, 0.16);
}

.preview-panel__empty-copy {
  margin-top: 0.55rem;
  color: rgba(239, 244, 244, 0.8);
  line-height: 1.7;
}

@media (max-width: 1180px) {
  .preview-panel__meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .preview-panel__meta-grid {
    grid-template-columns: 1fr;
  }

  .preview-panel__meta-head {
    flex-direction: column;
  }
}
</style>
