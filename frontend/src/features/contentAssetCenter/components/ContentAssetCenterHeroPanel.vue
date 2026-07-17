<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  ContentAsset,
  GenerateContentAssetPayload,
  UploadContentAssetPayload
} from '@/services/apiTypes';
import { formatCount } from '@/utils/formatters';

interface StoreOption {
  value: string;
  label: string;
}

interface Summary {
  totalAssets: number;
  generatedAssets: number;
  videoAssets: number;
  publishedVideoAssets: number;
  archivedAssets: number;
}

interface Props {
  selectedAsset: ContentAsset | null;
  selectedStoreName: string;
  defaultStoreId: string | null;
  storeOptions: StoreOption[];
  summary: Summary;
  isSubmitting: boolean;
  actionFeedback?: string | null;
  actionError?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionFeedback: null,
  actionError: null
});

const emit = defineEmits<{
  upload: [payload: UploadContentAssetPayload];
  generate: [payload: GenerateContentAssetPayload];
}>();

const uploadForm = reactive({
  storeId: '',
  assetCategory: 'product',
  assetType: 'image',
  assetName: '',
  sourceUrl: '',
  previewText: '',
  changeSummary: '',
  referenceHint: ''
});

const generateForm = reactive({
  storeId: '',
  assetCategory: 'product',
  assetType: 'video',
  assetName: '',
  brief: '',
  tone: 'conversion-first',
  referenceHint: '',
  clipTemplate: 'highlight-carousel',
  durationSeconds: 20,
  publishPlatform: 'douyin'
});

const selectedAssetIsPublished = computed(
  () => props.selectedAsset?.assetType === 'video' && (props.selectedAsset.previewText ?? '').includes('published to')
);

const selectedStatusTone = computed(() => {
  if (!props.selectedAsset) {
    return 'warn';
  }

  if (props.selectedAsset.assetStatus === 'archived') {
    return 'neutral';
  }

  if (selectedAssetIsPublished.value) {
    return 'success';
  }

  return props.selectedAsset.generated ? 'success' : 'warn';
});

const selectedStatusLabel = computed(() => {
  if (!props.selectedAsset) {
    return '未选择素材';
  }

  if (props.selectedAsset.assetStatus === 'archived') {
    return '已归档';
  }

  if (selectedAssetIsPublished.value) {
    return '已发布';
  }

  return props.selectedAsset.generated ? 'AI 生成' : '人工上传';
});

const canUseSelectedAsVideoSource = computed(
  () =>
    props.selectedAsset?.assetCategory === 'product' &&
    ['image', 'cover'].includes(props.selectedAsset.assetType)
);

const generateButtonLabel = computed(() =>
  generateForm.assetType === 'video' ? '生成视频成片' : '生成素材'
);

watch(
  () => props.defaultStoreId,
  (value) => {
    if (!value) {
      return;
    }

    if (!uploadForm.storeId) {
      uploadForm.storeId = value;
    }

    if (!generateForm.storeId) {
      generateForm.storeId = value;
    }
  },
  { immediate: true }
);

watch(
  () => props.selectedAsset?.assetId,
  () => {
    if (!canUseSelectedAsVideoSource.value || generateForm.assetType !== 'video') {
      return;
    }

    if (!generateForm.assetName.trim() && props.selectedAsset?.assetName) {
      generateForm.assetName = `${props.selectedAsset.assetName}-short-video`;
    }
  },
  { immediate: true }
);

function submitUpload() {
  if (!uploadForm.storeId || !uploadForm.assetName.trim()) {
    return;
  }

  emit('upload', {
    storeId: uploadForm.storeId,
    assetCategory: uploadForm.assetCategory,
    assetType: uploadForm.assetType,
    assetName: uploadForm.assetName.trim(),
    sourceUrl: uploadForm.sourceUrl.trim() || undefined,
    previewText: uploadForm.previewText.trim() || undefined,
    changeSummary: uploadForm.changeSummary.trim() || undefined,
    referenceHint: uploadForm.referenceHint.trim() || undefined
  });
}

function submitGenerate() {
  if (!generateForm.storeId || !generateForm.assetName.trim() || !generateForm.brief.trim()) {
    return;
  }

  if (generateForm.assetType === 'video' && !canUseSelectedAsVideoSource.value) {
    return;
  }

  emit('generate', {
    storeId: generateForm.storeId,
    assetCategory: generateForm.assetType === 'video' ? 'product' : generateForm.assetCategory,
    assetType: generateForm.assetType,
    assetName: generateForm.assetName.trim(),
    brief: generateForm.brief.trim(),
    tone: generateForm.tone.trim() || undefined,
    referenceHint: generateForm.referenceHint.trim() || undefined,
    sourceAssetId: generateForm.assetType === 'video' ? props.selectedAsset?.assetId : undefined,
    clipTemplate: generateForm.assetType === 'video' ? generateForm.clipTemplate.trim() || undefined : undefined,
    durationSeconds: generateForm.assetType === 'video' ? generateForm.durationSeconds : undefined,
    publishPlatform: generateForm.assetType === 'video' ? generateForm.publishPlatform.trim() || undefined : undefined
  });
}
</script>

<template>
  <section class="asset-hero">
    <div class="asset-hero__copy">
      <p class="asset-hero__eyebrow">Content Asset Atelier</p>
      <h3 class="asset-hero__title">素材中心不只管存档，还要把商品图真正推成可发布的视频成片</h3>
      <p class="asset-hero__subtitle">
        当前焦点店铺：{{ selectedStoreName }}。左侧选中商品图后，可以直接在这里生成视频、补齐剪辑参数，再把成片发布动作写回素材链路。
      </p>

      <div class="asset-hero__forms">
        <section class="asset-hero__composer">
          <div class="asset-hero__composer-head">
            <p class="asset-hero__composer-label">上传现有素材</p>
            <span class="asset-hero__composer-note">适合沉淀主图、封面、脚本截图和已剪好的视频资源。</span>
          </div>

          <div class="asset-hero__field-grid">
            <label class="asset-hero__field">
              <span>归属店铺</span>
              <select v-model="uploadForm.storeId">
                <option v-for="store in storeOptions" :key="store.value" :value="store.value">
                  {{ store.label }}
                </option>
              </select>
            </label>
            <label class="asset-hero__field">
              <span>素材分类</span>
              <select v-model="uploadForm.assetCategory">
                <option value="product">商品</option>
                <option value="live">直播</option>
                <option value="service">客服</option>
              </select>
            </label>
            <label class="asset-hero__field">
              <span>素材类型</span>
              <select v-model="uploadForm.assetType">
                <option value="image">图片</option>
                <option value="video">视频</option>
                <option value="copy">文案</option>
                <option value="faq">FAQ</option>
                <option value="cover">封面</option>
              </select>
            </label>
            <label class="asset-hero__field asset-hero__field--wide">
              <span>素材名称</span>
              <input v-model="uploadForm.assetName" type="text" placeholder="例如：新品茶具主图封面" />
            </label>
            <label class="asset-hero__field asset-hero__field--wide">
              <span>来源 URL</span>
              <input v-model="uploadForm.sourceUrl" type="url" placeholder="https://assets.example.com/cover.png" />
            </label>
            <label class="asset-hero__field asset-hero__field--wide">
              <span>预览说明</span>
              <input
                v-model="uploadForm.previewText"
                type="text"
                placeholder="例如：用于商品详情首屏和短视频封面"
              />
            </label>
          </div>

          <div class="asset-hero__composer-actions">
            <button
              class="asset-hero__button asset-hero__button--solid"
              type="button"
              :disabled="isSubmitting"
              @click="submitUpload"
            >
              上传素材
            </button>
          </div>
        </section>

        <section class="asset-hero__composer asset-hero__composer--dark">
          <div class="asset-hero__composer-head">
            <p class="asset-hero__composer-label">生成与编排</p>
            <span class="asset-hero__composer-note">可走通普通素材生成，也可直接把选中的商品图编成视频成片。</span>
          </div>

          <div class="asset-hero__field-grid">
            <label class="asset-hero__field">
              <span>归属店铺</span>
              <select v-model="generateForm.storeId">
                <option v-for="store in storeOptions" :key="store.value" :value="store.value">
                  {{ store.label }}
                </option>
              </select>
            </label>
            <label class="asset-hero__field">
              <span>素材分类</span>
              <select v-model="generateForm.assetCategory" :disabled="generateForm.assetType === 'video'">
                <option value="product">商品</option>
                <option value="live">直播</option>
                <option value="service">客服</option>
              </select>
            </label>
            <label class="asset-hero__field">
              <span>生成类型</span>
              <select v-model="generateForm.assetType">
                <option value="video">视频成片</option>
                <option value="script">脚本</option>
                <option value="copy">文案</option>
                <option value="faq">FAQ</option>
                <option value="image">图片</option>
              </select>
            </label>
            <label class="asset-hero__field asset-hero__field--wide">
              <span>生成名称</span>
              <input v-model="generateForm.assetName" type="text" placeholder="例如：茶具 20 秒短视频成片" />
            </label>
            <label class="asset-hero__field asset-hero__field--wide">
              <span>生成 Brief</span>
              <textarea
                v-model="generateForm.brief"
                rows="3"
                placeholder="例如：把主图编成 20 秒视频，突出材质、赠品和下单理由"
              />
            </label>
            <label class="asset-hero__field">
              <span>语气</span>
              <input v-model="generateForm.tone" type="text" placeholder="例如：conversion-first" />
            </label>
            <label class="asset-hero__field asset-hero__field--wide">
              <span>引用提示</span>
              <input v-model="generateForm.referenceHint" type="text" placeholder="例如：短视频首发批次" />
            </label>

            <template v-if="generateForm.assetType === 'video'">
              <div class="asset-hero__workflow-card asset-hero__field--wide">
                <p class="asset-hero__workflow-label">当前视频源</p>
                <p class="asset-hero__workflow-value">
                  {{
                    canUseSelectedAsVideoSource
                      ? `使用当前选中的商品图：${selectedAsset?.assetName}`
                      : '请先在左侧选择一个商品分类下的图片或封面素材'
                  }}
                </p>
              </div>
              <label class="asset-hero__field">
                <span>剪辑模板</span>
                <input v-model="generateForm.clipTemplate" type="text" placeholder="例如：highlight-carousel" />
              </label>
              <label class="asset-hero__field">
                <span>时长秒数</span>
                <input v-model.number="generateForm.durationSeconds" type="number" min="5" max="60" />
              </label>
              <label class="asset-hero__field">
                <span>目标平台</span>
                <input v-model="generateForm.publishPlatform" type="text" placeholder="例如：douyin" />
              </label>
            </template>
          </div>

          <div class="asset-hero__composer-actions">
            <button
              class="asset-hero__button asset-hero__button--inverted"
              type="button"
              :disabled="isSubmitting || (generateForm.assetType === 'video' && !canUseSelectedAsVideoSource)"
              @click="submitGenerate"
            >
              {{ generateButtonLabel }}
            </button>
          </div>
        </section>
      </div>

      <p v-if="actionFeedback" class="asset-hero__feedback asset-hero__feedback--success">
        {{ actionFeedback }}
      </p>
      <p v-if="actionError" class="asset-hero__feedback asset-hero__feedback--error">
        {{ actionError }}
      </p>
    </div>

    <div class="asset-hero__focus">
      <div class="asset-hero__focus-head">
        <div>
          <p class="asset-hero__focus-label">当前焦点素材</p>
          <h4 class="asset-hero__focus-title">{{ selectedAsset?.assetName || '请先从左侧选中一个素材' }}</h4>
        </div>
        <StatusPill :label="selectedStatusLabel" :tone="selectedStatusTone" />
      </div>

      <div class="asset-hero__focus-meta">
        <span>{{ selectedAsset?.assetCategory || '未分类' }}</span>
        <span>{{ selectedAsset?.assetType || '未定义类型' }}</span>
        <span>{{ selectedStoreName }}</span>
      </div>

      <div class="asset-hero__focus-grid">
        <article class="asset-hero__focus-item">
          <p class="asset-hero__focus-item-label">来源通道</p>
          <p class="asset-hero__focus-item-value">{{ selectedAsset?.sourceChannel || '--' }}</p>
        </article>
        <article class="asset-hero__focus-item">
          <p class="asset-hero__focus-item-label">生成方式</p>
          <p class="asset-hero__focus-item-value">{{ selectedAsset?.generated ? 'AI 生成' : '人工上传' }}</p>
        </article>
        <article class="asset-hero__focus-item">
          <p class="asset-hero__focus-item-label">预览模式</p>
          <p class="asset-hero__focus-item-value">{{ selectedAsset?.previewMode || '--' }}</p>
        </article>
      </div>
    </div>
  </section>

  <section class="asset-hero__metrics">
    <MetricCard label="素材总量" :value="formatCount(summary.totalAssets)" caption="当前租户可继续复用或编排的全部内容资产" />
    <MetricCard label="AI 生成" :value="formatCount(summary.generatedAssets)" caption="已经进入生成链路并沉淀下来的资产数量" />
    <MetricCard label="视频成片" :value="formatCount(summary.videoAssets)" caption="已经产出可继续剪辑或发布的视频素材" />
    <MetricCard label="已发布视频" :value="formatCount(summary.publishedVideoAssets)" caption="已经完成平台发布回写的视频成片" />
    <MetricCard label="已归档" :value="formatCount(summary.archivedAssets)" caption="已经冻结版本但仍可追溯的历史资产" />
  </section>
</template>

<style scoped>
.asset-hero,
.asset-hero__metrics {
  display: grid;
  gap: 1rem;
}

.asset-hero {
  grid-template-columns: 1.15fr 0.85fr;
  align-items: start;
}

.asset-hero__copy,
.asset-hero__focus {
  padding: 1.45rem;
  border-radius: var(--radius-shell);
  box-shadow: var(--shadow-card);
}

.asset-hero__copy {
  background:
    radial-gradient(circle at top left, rgba(199, 116, 66, 0.22), transparent 30%),
    radial-gradient(circle at bottom right, rgba(46, 100, 85, 0.14), transparent 28%),
    linear-gradient(145deg, rgba(255, 250, 244, 0.97), rgba(246, 234, 221, 0.96));
  border: 1px solid rgba(121, 74, 40, 0.12);
}

.asset-hero__focus {
  display: grid;
  gap: 1rem;
  min-height: 100%;
  background:
    radial-gradient(circle at top right, rgba(144, 205, 195, 0.2), transparent 32%),
    linear-gradient(160deg, rgba(32, 45, 49, 0.98), rgba(17, 25, 29, 0.98));
  color: #f6f3ed;
}

.asset-hero__eyebrow,
.asset-hero__title,
.asset-hero__subtitle,
.asset-hero__feedback,
.asset-hero__focus-label,
.asset-hero__focus-title,
.asset-hero__focus-item-label,
.asset-hero__focus-item-value,
.asset-hero__composer-label,
.asset-hero__composer-note,
.asset-hero__workflow-label,
.asset-hero__workflow-value {
  margin: 0;
}

.asset-hero__eyebrow,
.asset-hero__focus-label,
.asset-hero__composer-label,
.asset-hero__workflow-label {
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.asset-hero__eyebrow {
  color: var(--color-ink-faint);
}

.asset-hero__title {
  margin-top: 0.55rem;
  max-width: 14ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3.5rem);
  line-height: 1.02;
}

.asset-hero__subtitle,
.asset-hero__feedback {
  margin-top: 0.8rem;
  color: var(--color-ink-soft);
  line-height: 1.72;
}

.asset-hero__feedback--success {
  color: var(--color-success);
}

.asset-hero__feedback--error {
  color: var(--color-danger);
}

.asset-hero__forms {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1.25rem;
}

.asset-hero__composer {
  padding: 1rem;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(121, 74, 40, 0.1);
}

.asset-hero__composer--dark {
  background: rgba(31, 39, 43, 0.9);
  border-color: rgba(255, 255, 255, 0.08);
}

.asset-hero__composer-head {
  display: grid;
  gap: 0.35rem;
}

.asset-hero__composer-note {
  color: var(--color-ink-soft);
  font-size: 0.88rem;
  line-height: 1.6;
}

.asset-hero__composer--dark .asset-hero__composer-label,
.asset-hero__composer--dark .asset-hero__composer-note,
.asset-hero__composer--dark .asset-hero__field span,
.asset-hero__composer--dark .asset-hero__workflow-label,
.asset-hero__composer--dark .asset-hero__workflow-value {
  color: rgba(245, 239, 233, 0.82);
}

.asset-hero__field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
  margin-top: 0.9rem;
}

.asset-hero__field {
  display: grid;
  gap: 0.42rem;
}

.asset-hero__field--wide {
  grid-column: span 3;
}

.asset-hero__field span {
  color: var(--color-ink);
  font-size: 0.84rem;
}

.asset-hero__field input,
.asset-hero__field select,
.asset-hero__field textarea {
  width: 100%;
  border: 1px solid rgba(121, 74, 40, 0.12);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.88);
  color: var(--color-ink-strong);
  padding: 0.82rem 0.9rem;
  font: inherit;
  box-sizing: border-box;
}

.asset-hero__composer--dark .asset-hero__field input,
.asset-hero__composer--dark .asset-hero__field select,
.asset-hero__composer--dark .asset-hero__field textarea {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.12);
  color: #fff8f0;
}

.asset-hero__workflow-card {
  display: grid;
  gap: 0.4rem;
  padding: 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.08);
}

.asset-hero__workflow-value {
  line-height: 1.6;
}

.asset-hero__composer-actions {
  margin-top: 0.9rem;
}

.asset-hero__button {
  min-height: 2.9rem;
  padding: 0 1.15rem;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(121, 74, 40, 0.14);
  background: rgba(255, 255, 255, 0.62);
  color: var(--color-ink-strong);
}

.asset-hero__button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.asset-hero__button--solid {
  background: #a95434;
  border-color: #a95434;
  color: #fff6ef;
}

.asset-hero__button--inverted {
  background: rgba(144, 205, 195, 0.18);
  border-color: rgba(144, 205, 195, 0.24);
  color: #f7f9fa;
}

.asset-hero__focus-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.asset-hero__focus-label {
  color: rgba(240, 245, 245, 0.62);
}

.asset-hero__focus-title {
  margin-top: 0.45rem;
  font-family: var(--font-display);
  font-size: 1.55rem;
}

.asset-hero__focus-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  color: rgba(240, 245, 245, 0.84);
  font-size: 0.86rem;
}

.asset-hero__focus-meta span {
  display: inline-flex;
  min-height: 2rem;
  align-items: center;
  padding: 0 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.08);
}

.asset-hero__focus-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.asset-hero__focus-item {
  padding: 0.95rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.08);
}

.asset-hero__focus-item-label {
  color: rgba(240, 245, 245, 0.62);
  font-size: 0.78rem;
}

.asset-hero__focus-item-value {
  margin-top: 0.55rem;
  font-size: 1.16rem;
  font-weight: 600;
}

.asset-hero__metrics {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

@media (max-width: 1320px) {
  .asset-hero,
  .asset-hero__metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1080px) {
  .asset-hero__forms,
  .asset-hero__focus-grid,
  .asset-hero__field-grid {
    grid-template-columns: 1fr;
  }

  .asset-hero__field--wide {
    grid-column: span 1;
  }
}
</style>
