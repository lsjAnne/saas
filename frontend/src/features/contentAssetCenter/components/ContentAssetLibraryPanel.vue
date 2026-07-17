<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { ContentAssetLibraryItem } from '@/features/contentAssetCenter/composables/useContentAssetCenterOverview';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  assets: ContentAssetLibraryItem[];
  selectedAssetId: string | null;
  isLoading: boolean;
}

defineProps<Props>();
defineEmits<{
  select: [assetId: string];
}>();
</script>

<template>
  <PanelCard
    eyebrow="Asset Library"
    title="素材列表与工作池"
    description="优先呈现最近活跃、可继续生成视频或待发布的素材，选中后右侧立刻联动。"
  >
    <div v-if="assets.length" class="asset-library">
      <button
        v-for="asset in assets"
        :key="asset.assetId"
        :class="['asset-library__item', { 'asset-library__item--active': asset.assetId === selectedAssetId }]"
        type="button"
        @click="$emit('select', asset.assetId)"
      >
        <div class="asset-library__head">
          <div>
            <p class="asset-library__name">{{ asset.assetName }}</p>
            <p class="asset-library__meta">
              {{ asset.storeName }} / {{ asset.categoryLabel }} / {{ asset.typeLabel }}
            </p>
          </div>
          <StatusPill :label="asset.statusLabel" :tone="asset.statusTone" />
        </div>

        <p class="asset-library__preview">
          {{ asset.previewText || '当前素材还没有补充预览说明，可在右侧预览区继续查看详情。' }}
        </p>

        <div class="asset-library__footer">
          <span>{{ asset.generated ? 'AI 生成' : '人工上传' }}</span>
          <span>{{ formatDateTime(asset.updatedAt) }}</span>
        </div>
      </button>
    </div>

    <div v-else class="asset-library__empty">
      <p class="asset-library__empty-title">
        {{ isLoading ? '素材池正在加载...' : '当前分类还没有素材' }}
      </p>
      <p class="asset-library__empty-copy">
        先在上方上传素材，或直接选中商品图生成视频成片，再回到这里继续挑选和复用。
      </p>
    </div>
  </PanelCard>
</template>

<style scoped>
.asset-library {
  display: grid;
  gap: 0.8rem;
}

.asset-library__item {
  padding: 1rem;
  border-radius: var(--radius-xl);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.62);
  text-align: left;
}

.asset-library__item--active {
  border-color: rgba(41, 101, 88, 0.28);
  background:
    radial-gradient(circle at top right, rgba(98, 166, 150, 0.14), transparent 35%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(241, 248, 246, 0.94));
  box-shadow: 0 18px 40px rgba(41, 101, 88, 0.12);
}

.asset-library__head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.asset-library__name,
.asset-library__meta,
.asset-library__preview,
.asset-library__empty-title,
.asset-library__empty-copy {
  margin: 0;
}

.asset-library__name {
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: 1.2rem;
}

.asset-library__meta {
  margin-top: 0.4rem;
  color: var(--color-ink-faint);
  font-size: 0.84rem;
}

.asset-library__preview {
  margin-top: 0.75rem;
  color: var(--color-ink-soft);
  line-height: 1.68;
}

.asset-library__footer {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  margin-top: 0.75rem;
  color: var(--color-ink-faint);
  font-size: 0.82rem;
}

.asset-library__empty {
  padding: 1.2rem;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.58);
  border: 1px dashed rgba(91, 72, 54, 0.16);
}

.asset-library__empty-title {
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: 1.2rem;
}

.asset-library__empty-copy {
  margin-top: 0.55rem;
  color: var(--color-ink-soft);
  line-height: 1.68;
}
</style>
