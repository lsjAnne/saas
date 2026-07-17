<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { Product, ProductDraft } from '@/services/apiTypes';
import { formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  drafts: ProductDraft[];
  relatedDrafts: ProductDraft[];
  selectedDraft: ProductDraft | null;
  selectedDraftId: string | null;
  sessionPublishedProducts: Product[];
}

defineProps<Props>();

defineEmits<{
  select: [productDraftId: string];
  openEdit: [];
  openPublish: [];
}>();

function resolveTone(status: string | null | undefined) {
  const normalized = (status ?? '').toLowerCase();

  if (normalized.includes('published')) {
    return 'success';
  }

  if (normalized.includes('reject')) {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Draft Workbench"
    title="草稿工位"
    description="草稿区只承接可发布前的最后一轮整理：标题、卖点、详情、FAQ 和建议价格。"
  >
    <div class="draft-workbench">
      <div class="draft-workbench__list">
        <article
          v-for="draft in relatedDrafts"
          :key="draft.productDraftId"
          :class="[
            'draft-workbench__item',
            { 'draft-workbench__item--active': draft.productDraftId === selectedDraftId }
          ]"
          @click="$emit('select', draft.productDraftId)"
        >
          <div class="draft-workbench__item-copy">
            <strong class="draft-workbench__item-title">{{ draft.title }}</strong>
            <span class="draft-workbench__item-meta">
              {{ draft.aiVersion || '--' }} / {{ formatCurrency(draft.suggestedPrice) }}
            </span>
          </div>
          <StatusPill :label="draft.status" :tone="resolveTone(draft.status)" />
        </article>

        <p v-if="!drafts.length" class="draft-workbench__empty">
          还没有任何草稿。先从候选池里挑一个条目推进生成。
        </p>
      </div>

      <div class="draft-workbench__detail">
        <template v-if="selectedDraft">
          <div class="draft-workbench__detail-head">
            <div>
              <p class="draft-workbench__label">Selected Draft</p>
              <h4 class="draft-workbench__detail-title">{{ selectedDraft.title }}</h4>
            </div>
            <div class="draft-workbench__detail-actions">
              <button type="button" class="draft-workbench__ghost" @click="$emit('openEdit')">
                编辑草稿
              </button>
              <button type="button" class="draft-workbench__primary" @click="$emit('openPublish')">
                发布草稿
              </button>
            </div>
          </div>

          <dl class="draft-workbench__facts">
            <div class="draft-workbench__fact">
              <dt>建议售价</dt>
              <dd>{{ formatCurrency(selectedDraft.suggestedPrice) }}</dd>
            </div>
            <div class="draft-workbench__fact">
              <dt>AI 版本</dt>
              <dd>{{ selectedDraft.aiVersion || '--' }}</dd>
            </div>
            <div class="draft-workbench__fact">
              <dt>状态</dt>
              <dd>{{ selectedDraft.status }}</dd>
            </div>
            <div class="draft-workbench__fact">
              <dt>生成时间</dt>
              <dd>{{ formatDateTime(selectedDraft.createdAt) }}</dd>
            </div>
          </dl>

          <div class="draft-workbench__copy-block">
            <p class="draft-workbench__label">卖点</p>
            <p class="draft-workbench__body">{{ selectedDraft.sellingPoints || '暂无卖点文案' }}</p>
          </div>

          <div class="draft-workbench__copy-block">
            <p class="draft-workbench__label">详情内容</p>
            <p class="draft-workbench__body">{{ selectedDraft.detailContent || '暂无详情内容' }}</p>
          </div>

          <div class="draft-workbench__copy-block">
            <p class="draft-workbench__label">FAQ</p>
            <p class="draft-workbench__body">{{ selectedDraft.faqContent || '暂无 FAQ 内容' }}</p>
          </div>

          <div v-if="sessionPublishedProducts.length" class="draft-workbench__session-note">
            最近一次发布商品号：
            {{ sessionPublishedProducts[0].platformProductId }}
          </div>
        </template>

        <p v-else class="draft-workbench__empty">
          先选择一个草稿，这里才会显示可发布前的内容全貌。
        </p>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.draft-workbench {
  display: grid;
  grid-template-columns: 0.94fr 1.06fr;
  gap: 0.95rem;
}

.draft-workbench__list,
.draft-workbench__detail,
.draft-workbench__copy-block {
  display: grid;
  gap: 0.8rem;
}

.draft-workbench__detail-head,
.draft-workbench__detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  justify-content: space-between;
}

.draft-workbench__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.74);
}

.draft-workbench__item--active {
  border-color: rgba(133, 98, 67, 0.34);
  background: linear-gradient(135deg, rgba(250, 242, 233, 0.94), rgba(245, 232, 213, 0.88));
}

.draft-workbench__item-copy,
.draft-workbench__detail {
  display: grid;
  gap: 0.35rem;
}

.draft-workbench__item-title,
.draft-workbench__item-meta,
.draft-workbench__label,
.draft-workbench__detail-title,
.draft-workbench__body,
.draft-workbench__empty,
.draft-workbench__session-note {
  margin: 0;
}

.draft-workbench__detail {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(248, 244, 238, 0.8);
}

.draft-workbench__item-title,
.draft-workbench__detail-title {
  color: var(--color-ink-strong);
}

.draft-workbench__item-meta,
.draft-workbench__label,
.draft-workbench__body,
.draft-workbench__empty,
.draft-workbench__session-note {
  color: var(--color-ink-soft);
}

.draft-workbench__label {
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.draft-workbench__detail-title {
  margin-top: 0.35rem;
  font-family: var(--font-display);
  font-size: 1.55rem;
}

.draft-workbench__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
  margin: 0;
}

.draft-workbench__fact {
  display: grid;
  gap: 0.35rem;
  padding: 0.8rem 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.72);
}

.draft-workbench__fact dt {
  color: var(--color-ink-soft);
  font-size: 0.8rem;
}

.draft-workbench__fact dd {
  margin: 0;
  color: var(--color-ink-strong);
}

.draft-workbench__primary,
.draft-workbench__ghost {
  min-height: 2.9rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.draft-workbench__primary {
  border: 0;
  background: linear-gradient(135deg, #2e251d, #856243);
  color: #fff8f0;
}

.draft-workbench__ghost {
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-ink);
}

.draft-workbench__body,
.draft-workbench__empty {
  line-height: 1.7;
}

.draft-workbench__session-note {
  padding: 0.85rem 0.95rem;
  border-radius: var(--radius-lg);
  background: rgba(183, 117, 47, 0.08);
  color: var(--color-warning);
}

@media (max-width: 1120px) {
  .draft-workbench {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .draft-workbench__facts {
    grid-template-columns: 1fr;
  }
}
</style>
