<script setup lang="ts">
import { reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import type {
  CandidateProduct,
  CreateCandidateProductPayload,
  GenerateProductDraftPayload,
  ProductDraft,
  Store,
  UpdateCandidateProductPayload,
  UpdateProductDraftPayload
} from '@/services/apiTypes';

interface Props {
  mode:
    | 'create-candidate'
    | 'edit-candidate'
    | 'generate-draft'
    | 'edit-draft'
    | 'publish-draft'
    | null;
  stores: Store[];
  selectedCandidate: CandidateProduct | null;
  selectedDraft: ProductDraft | null;
  isSubmitting: boolean;
  errorMessage?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  errorMessage: null
});

const emit = defineEmits<{
  close: [];
  openCreate: [];
  openEditCandidate: [];
  openGenerate: [];
  openEditDraft: [];
  openPublish: [];
  createCandidate: [payload: CreateCandidateProductPayload];
  updateCandidate: [payload: UpdateCandidateProductPayload];
  generateDraft: [payload: GenerateProductDraftPayload];
  updateDraft: [payload: UpdateProductDraftPayload];
  publishDraft: [platformProductId?: string];
}>();

const candidateForm = reactive({
  storeId: '',
  sourceType: '1688',
  sourceUrl: '',
  title: '',
  category: '',
  estimatedProfit: '0',
  riskLevel: 'low',
  recommendationReason: '',
  aiSummary: ''
});

const draftGenerateForm = reactive({
  aiVersion: 'ai-v1',
  suggestedPrice: '0'
});

const draftEditForm = reactive({
  title: '',
  sellingPoints: '',
  detailContent: '',
  faqContent: '',
  suggestedPrice: '0',
  status: 'pending_publish'
});

const publishForm = reactive({
  platformProductId: ''
});

watch(
  () => [props.mode, props.selectedCandidate?.candidateProductId, props.selectedDraft?.productDraftId],
  () => {
    if (props.mode === 'create-candidate') {
      candidateForm.storeId = props.selectedCandidate?.storeId || props.stores[0]?.storeId || '';
      candidateForm.sourceType = '1688';
      candidateForm.sourceUrl = '';
      candidateForm.title = '';
      candidateForm.category = '';
      candidateForm.estimatedProfit = '0';
      candidateForm.riskLevel = 'low';
      candidateForm.recommendationReason = '';
      candidateForm.aiSummary = '';
    }

    if (props.mode === 'edit-candidate' && props.selectedCandidate) {
      candidateForm.storeId = props.selectedCandidate.storeId;
      candidateForm.sourceType = props.selectedCandidate.sourceType;
      candidateForm.sourceUrl = props.selectedCandidate.sourceUrl;
      candidateForm.title = props.selectedCandidate.title;
      candidateForm.category = props.selectedCandidate.category || '';
      candidateForm.estimatedProfit = String(props.selectedCandidate.estimatedProfit ?? 0);
      candidateForm.riskLevel = props.selectedCandidate.riskLevel || 'low';
      candidateForm.recommendationReason = props.selectedCandidate.recommendationReason || '';
      candidateForm.aiSummary = props.selectedCandidate.aiSummary || '';
    }

    if (props.mode === 'generate-draft') {
      draftGenerateForm.aiVersion = 'ai-v1';
      draftGenerateForm.suggestedPrice = '0';
    }

    if (props.mode === 'edit-draft' && props.selectedDraft) {
      draftEditForm.title = props.selectedDraft.title;
      draftEditForm.sellingPoints = props.selectedDraft.sellingPoints || '';
      draftEditForm.detailContent = props.selectedDraft.detailContent || '';
      draftEditForm.faqContent = props.selectedDraft.faqContent || '';
      draftEditForm.suggestedPrice = String(props.selectedDraft.suggestedPrice ?? 0);
      draftEditForm.status = props.selectedDraft.status || 'pending_publish';
    }

    if (props.mode === 'publish-draft') {
      publishForm.platformProductId = '';
    }
  },
  { immediate: true }
);

function toNumber(value: string, fieldName: string) {
  const parsed = Number(value);

  if (Number.isNaN(parsed) || parsed < 0) {
    throw new Error(`${fieldName} 必须是大于等于 0 的数字`);
  }

  return parsed;
}

function submitCandidate() {
  const payload: CreateCandidateProductPayload = {
    storeId: candidateForm.storeId.trim(),
    sourceType: candidateForm.sourceType.trim(),
    sourceUrl: candidateForm.sourceUrl.trim(),
    title: candidateForm.title.trim(),
    category: candidateForm.category.trim(),
    estimatedProfit: toNumber(candidateForm.estimatedProfit, '利润预估'),
    riskLevel: candidateForm.riskLevel.trim(),
    recommendationReason: candidateForm.recommendationReason.trim(),
    aiSummary: candidateForm.aiSummary.trim()
  };

  emit('createCandidate', payload);
}

function submitCandidateUpdate() {
  const payload: UpdateCandidateProductPayload = {
    sourceUrl: candidateForm.sourceUrl.trim(),
    title: candidateForm.title.trim(),
    category: candidateForm.category.trim(),
    estimatedProfit: toNumber(candidateForm.estimatedProfit, '利润预估'),
    riskLevel: candidateForm.riskLevel.trim(),
    recommendationReason: candidateForm.recommendationReason.trim(),
    aiSummary: candidateForm.aiSummary.trim()
  };

  emit('updateCandidate', payload);
}

function submitDraftGeneration() {
  if (!props.selectedCandidate) {
    return;
  }

  emit('generateDraft', {
    candidateProductId: props.selectedCandidate.candidateProductId,
    aiVersion: draftGenerateForm.aiVersion.trim(),
    suggestedPrice: toNumber(draftGenerateForm.suggestedPrice, '建议售价')
  });
}

function submitDraftUpdate() {
  const payload: UpdateProductDraftPayload = {
    title: draftEditForm.title.trim(),
    sellingPoints: draftEditForm.sellingPoints.trim(),
    detailContent: draftEditForm.detailContent.trim(),
    faqContent: draftEditForm.faqContent.trim(),
    suggestedPrice: toNumber(draftEditForm.suggestedPrice, '建议售价'),
    status: draftEditForm.status.trim()
  };

  emit('updateDraft', payload);
}

function submitDraftPublish() {
  emit('publishDraft', publishForm.platformProductId.trim() || undefined);
}
</script>

<template>
  <PanelCard
    eyebrow="Action Station"
    title="商品操作台"
    description="候选与草稿都在这里进入编辑态。路由页只编排，表单和动作入口都被集中收纳在这块操作台里。"
  >
    <div class="product-composer">
      <div v-if="!mode" class="product-composer__idle">
        <p class="product-composer__copy">
          先选择动作：新建候选、编辑当前候选、生成草稿、编辑草稿，或把当前草稿发布出去。
        </p>
        <div class="product-composer__action-row">
          <button type="button" class="product-composer__primary" @click="emit('openCreate')">
            新建候选
          </button>
          <button type="button" class="product-composer__ghost" @click="emit('openEditCandidate')">
            编辑候选
          </button>
          <button type="button" class="product-composer__ghost" @click="emit('openGenerate')">
            生成草稿
          </button>
          <button type="button" class="product-composer__ghost" @click="emit('openEditDraft')">
            编辑草稿
          </button>
          <button type="button" class="product-composer__ghost" @click="emit('openPublish')">
            发布草稿
          </button>
        </div>
      </div>

      <template v-else>
        <div class="product-composer__bar">
          <strong class="product-composer__bar-title">
            {{
              mode === 'create-candidate'
                ? '新建候选商品'
                : mode === 'edit-candidate'
                  ? '编辑候选商品'
                  : mode === 'generate-draft'
                    ? '生成商品草稿'
                    : mode === 'edit-draft'
                      ? '编辑商品草稿'
                      : '发布商品草稿'
            }}
          </strong>
          <button type="button" class="product-composer__ghost" @click="$emit('close')">
            收起
          </button>
        </div>

        <p v-if="errorMessage" class="product-composer__error">
          {{ errorMessage }}
        </p>

        <form
          v-if="mode === 'create-candidate' || mode === 'edit-candidate'"
          class="product-composer__form"
          @submit.prevent="mode === 'create-candidate' ? submitCandidate() : submitCandidateUpdate()"
        >
          <label class="product-composer__field">
            <span>店铺</span>
            <select v-model="candidateForm.storeId" :disabled="mode === 'edit-candidate'">
              <option v-for="store in stores" :key="store.storeId" :value="store.storeId">
                {{ store.shopName }}
              </option>
            </select>
          </label>
          <label class="product-composer__field">
            <span>来源类型</span>
            <input v-model="candidateForm.sourceType" type="text" :disabled="mode === 'edit-candidate'" />
          </label>
          <label class="product-composer__field product-composer__field--full">
            <span>来源 URL</span>
            <input v-model="candidateForm.sourceUrl" type="text" required />
          </label>
          <label class="product-composer__field product-composer__field--full">
            <span>标题</span>
            <input v-model="candidateForm.title" type="text" required />
          </label>
          <label class="product-composer__field">
            <span>分类</span>
            <input v-model="candidateForm.category" type="text" />
          </label>
          <label class="product-composer__field">
            <span>利润预估</span>
            <input v-model="candidateForm.estimatedProfit" type="number" min="0" step="0.01" />
          </label>
          <label class="product-composer__field">
            <span>风险等级</span>
            <input v-model="candidateForm.riskLevel" type="text" />
          </label>
          <label class="product-composer__field product-composer__field--full">
            <span>推荐理由</span>
            <textarea v-model="candidateForm.recommendationReason" rows="4" />
          </label>
          <label class="product-composer__field product-composer__field--full">
            <span>AI 摘要</span>
            <textarea v-model="candidateForm.aiSummary" rows="5" />
          </label>

          <div class="product-composer__submit-row">
            <button type="submit" class="product-composer__primary" :disabled="isSubmitting">
              {{ isSubmitting ? '提交中...' : mode === 'create-candidate' ? '创建候选' : '保存候选' }}
            </button>
          </div>
        </form>

        <form
          v-else-if="mode === 'generate-draft'"
          class="product-composer__form"
          @submit.prevent="submitDraftGeneration"
        >
          <label class="product-composer__field">
            <span>AI 版本</span>
            <input v-model="draftGenerateForm.aiVersion" type="text" />
          </label>
          <label class="product-composer__field">
            <span>建议售价</span>
            <input v-model="draftGenerateForm.suggestedPrice" type="number" min="0" step="0.01" />
          </label>

          <div class="product-composer__submit-row">
            <button type="submit" class="product-composer__primary" :disabled="isSubmitting">
              {{ isSubmitting ? '生成中...' : '生成草稿' }}
            </button>
          </div>
        </form>

        <form
          v-else-if="mode === 'edit-draft'"
          class="product-composer__form"
          @submit.prevent="submitDraftUpdate"
        >
          <label class="product-composer__field product-composer__field--full">
            <span>标题</span>
            <input v-model="draftEditForm.title" type="text" required />
          </label>
          <label class="product-composer__field">
            <span>建议售价</span>
            <input v-model="draftEditForm.suggestedPrice" type="number" min="0" step="0.01" />
          </label>
          <label class="product-composer__field">
            <span>草稿状态</span>
            <input v-model="draftEditForm.status" type="text" />
          </label>
          <label class="product-composer__field product-composer__field--full">
            <span>卖点</span>
            <textarea v-model="draftEditForm.sellingPoints" rows="4" />
          </label>
          <label class="product-composer__field product-composer__field--full">
            <span>详情内容</span>
            <textarea v-model="draftEditForm.detailContent" rows="6" />
          </label>
          <label class="product-composer__field product-composer__field--full">
            <span>FAQ</span>
            <textarea v-model="draftEditForm.faqContent" rows="5" />
          </label>

          <div class="product-composer__submit-row">
            <button type="submit" class="product-composer__primary" :disabled="isSubmitting">
              {{ isSubmitting ? '保存中...' : '保存草稿' }}
            </button>
          </div>
        </form>

        <form v-else class="product-composer__form" @submit.prevent="submitDraftPublish">
          <label class="product-composer__field product-composer__field--full">
            <span>平台商品号（可选）</span>
            <input v-model="publishForm.platformProductId" type="text" placeholder="留空则由后端生成" />
          </label>

          <div class="product-composer__submit-row">
            <button type="submit" class="product-composer__primary" :disabled="isSubmitting">
              {{ isSubmitting ? '发布中...' : '确认发布草稿' }}
            </button>
          </div>
        </form>
      </template>
    </div>
  </PanelCard>
</template>

<style scoped>
.product-composer {
  display: grid;
  gap: 0.9rem;
}

.product-composer__idle,
.product-composer__form,
.product-composer__field {
  display: grid;
  gap: 0.8rem;
}

.product-composer__copy,
.product-composer__bar-title,
.product-composer__error {
  margin: 0;
}

.product-composer__copy {
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.product-composer__action-row,
.product-composer__bar,
.product-composer__submit-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  justify-content: space-between;
}

.product-composer__bar-title {
  color: var(--color-ink-strong);
  font-size: 1.05rem;
}

.product-composer__primary,
.product-composer__ghost {
  min-height: 2.9rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.product-composer__primary {
  border: 0;
  background: linear-gradient(135deg, #2e251d, #856243);
  color: #fff8f0;
}

.product-composer__ghost {
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.74);
  color: var(--color-ink);
}

.product-composer__primary:disabled,
.product-composer__ghost:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.product-composer__error {
  padding: 0.85rem 0.95rem;
  border-radius: var(--radius-lg);
  background: rgba(183, 117, 47, 0.1);
  color: var(--color-warning);
}

.product-composer__form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.product-composer__field span {
  color: var(--color-ink-soft);
  font-size: 0.83rem;
}

.product-composer__field input,
.product-composer__field textarea,
.product-composer__field select {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.14);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.78);
  padding: 0.9rem 1rem;
  color: var(--color-ink-strong);
  font: inherit;
}

.product-composer__field--full,
.product-composer__submit-row {
  grid-column: 1 / -1;
}

@media (max-width: 720px) {
  .product-composer__form {
    grid-template-columns: 1fr;
  }
}
</style>
