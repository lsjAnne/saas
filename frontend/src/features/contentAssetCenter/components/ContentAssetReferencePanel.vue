<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import type {
  ContentAsset,
  ContentAssetReference,
  ContentAssetReferenceCopyView,
  PublishContentAssetPayload
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  references: ContentAssetReference[];
  copyResult: ContentAssetReferenceCopyView | null;
  selectedAsset: ContentAsset | null;
  isSubmitting: boolean;
  actionError?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<{
  copy: [];
  publish: [payload: PublishContentAssetPayload];
}>();

const publishForm = reactive({
  platformCode: 'douyin',
  publishTitle: '',
  publishRemark: '通过剪辑复核后发布'
});

const canPublishVideo = computed(
  () => props.selectedAsset?.assetType === 'video' && props.selectedAsset.assetStatus !== 'archived'
);

watch(
  () => props.selectedAsset?.assetId,
  () => {
    if (props.selectedAsset?.assetType === 'video') {
      publishForm.publishTitle = props.selectedAsset.assetName;
    }
  },
  { immediate: true }
);

function submitPublish() {
  if (!publishForm.platformCode.trim()) {
    return;
  }

  emit('publish', {
    platformCode: publishForm.platformCode.trim(),
    publishTitle: publishForm.publishTitle.trim() || undefined,
    publishRemark: publishForm.publishRemark.trim() || undefined
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Reference Links"
    title="引用来源与发布回写"
    description="既追踪当前素材被哪些链路复用，也把视频发布动作直接写回同一条素材记录。"
  >
    <div class="reference-panel__head">
      <p class="reference-panel__hint">复制引用会生成复用文本，便于下游编排、审核或跨模块继续接力。</p>
      <button class="reference-panel__button" type="button" :disabled="isSubmitting" @click="$emit('copy')">
        复制引用
      </button>
    </div>

    <div v-if="canPublishVideo" class="reference-panel__publish-card">
      <p class="reference-panel__publish-title">发布当前视频成片</p>
      <div class="reference-panel__publish-grid">
        <label class="reference-panel__field">
          <span>目标平台</span>
          <input v-model="publishForm.platformCode" type="text" placeholder="例如：douyin" />
        </label>
        <label class="reference-panel__field">
          <span>发布标题</span>
          <input v-model="publishForm.publishTitle" type="text" placeholder="填写视频发布标题" />
        </label>
        <label class="reference-panel__field reference-panel__field--wide">
          <span>发布备注</span>
          <input v-model="publishForm.publishRemark" type="text" placeholder="例如：通过剪辑复核后发布" />
        </label>
      </div>
      <button class="reference-panel__publish-button" type="button" :disabled="isSubmitting" @click="submitPublish">
        发布视频
      </button>
    </div>

    <p v-if="actionError" class="reference-panel__error">{{ actionError }}</p>

    <div v-if="copyResult" class="reference-panel__copybox">
      <p class="reference-panel__copy-label">最近一次生成的引用文本</p>
      <pre class="reference-panel__copy-content">{{ copyResult.referenceText }}</pre>
    </div>

    <div v-if="references.length" class="reference-panel__list">
      <article v-for="reference in references" :key="reference.referenceId" class="reference-panel__item">
        <div class="reference-panel__item-head">
          <div>
            <p class="reference-panel__name">{{ reference.referenceName }}</p>
            <p class="reference-panel__meta">
              {{ reference.referenceType }} / {{ reference.referenceTargetId }}
            </p>
          </div>
          <span class="reference-panel__time">{{ formatDateTime(reference.createdAt) }}</span>
        </div>
        <p class="reference-panel__quote">
          {{ reference.quoteText || '当前引用未补充备注。' }}
        </p>
      </article>
    </div>

    <div v-else class="reference-panel__empty">
      当前素材还没有引用记录，复制引用或发布视频后，这里会同步展示来源和回写结果。
    </div>
  </PanelCard>
</template>

<style scoped>
.reference-panel__head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: center;
}

.reference-panel__hint,
.reference-panel__copy-label,
.reference-panel__copy-content,
.reference-panel__name,
.reference-panel__meta,
.reference-panel__quote,
.reference-panel__empty,
.reference-panel__publish-title,
.reference-panel__error {
  margin: 0;
}

.reference-panel__hint,
.reference-panel__meta,
.reference-panel__quote,
.reference-panel__empty {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.reference-panel__button,
.reference-panel__publish-button {
  min-height: 2.9rem;
  padding: 0 1.15rem;
  border-radius: var(--radius-pill);
}

.reference-panel__button {
  border: 1px solid rgba(41, 101, 88, 0.16);
  background: rgba(41, 101, 88, 0.12);
  color: var(--color-success);
}

.reference-panel__publish-button {
  border: 1px solid rgba(121, 74, 40, 0.14);
  background: var(--color-accent);
  color: #fff7f0;
}

.reference-panel__button:disabled,
.reference-panel__publish-button:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.reference-panel__publish-card {
  display: grid;
  gap: 0.8rem;
  margin-top: 0.9rem;
  padding: 1rem;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.reference-panel__publish-title {
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: 1.2rem;
}

.reference-panel__publish-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.reference-panel__field {
  display: grid;
  gap: 0.42rem;
}

.reference-panel__field--wide {
  grid-column: span 2;
}

.reference-panel__field span {
  color: var(--color-ink-faint);
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.reference-panel__field input {
  width: 100%;
  min-height: 2.9rem;
  padding: 0 0.95rem;
  border-radius: 1rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.88);
  color: var(--color-ink-strong);
  font: inherit;
  box-sizing: border-box;
}

.reference-panel__error {
  margin-top: 0.8rem;
  color: var(--color-danger);
}

.reference-panel__copybox {
  margin-top: 0.9rem;
  padding: 1rem;
  border-radius: var(--radius-xl);
  background: linear-gradient(145deg, rgba(31, 39, 43, 0.98), rgba(18, 27, 31, 0.98));
  color: #fff7f1;
}

.reference-panel__copy-label {
  color: rgba(244, 247, 247, 0.74);
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.reference-panel__copy-content {
  margin-top: 0.75rem;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.72;
}

.reference-panel__list {
  display: grid;
  gap: 0.8rem;
  margin-top: 0.9rem;
}

.reference-panel__item {
  padding: 1rem;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.reference-panel__item-head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.reference-panel__name {
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: 1.15rem;
}

.reference-panel__meta,
.reference-panel__time {
  margin-top: 0.42rem;
  color: var(--color-ink-faint);
  font-size: 0.82rem;
}

.reference-panel__quote {
  margin-top: 0.75rem;
}

.reference-panel__empty {
  margin-top: 0.9rem;
  padding: 1rem;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.56);
}

@media (max-width: 720px) {
  .reference-panel__head,
  .reference-panel__item-head,
  .reference-panel__publish-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }

  .reference-panel__field--wide {
    grid-column: span 1;
  }
}
</style>
