<script setup lang="ts">
import { reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  MemberGroupExportView,
  MemberSegmentExecutionView,
  MemberSegmentRuleView,
  MemberTagSummaryView,
  SaveMemberSegmentRulePayload,
  Store
} from '@/services/apiTypes';
import { formatCount, formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  stores: Store[];
  activeStoreId: string;
  memberTags: MemberTagSummaryView[];
  sessionSegmentRules: MemberSegmentRuleView[];
  sessionSegmentExecutions: MemberSegmentExecutionView[];
  latestExport: MemberGroupExportView | null;
  isSubmitting: boolean;
  actionError?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<{
  createRule: [payload: SaveMemberSegmentRulePayload];
  runRule: [ruleId: string];
  exportGroup: [];
}>();

const form = reactive({
  storeId: '',
  ruleName: '',
  lifecycleStage: '',
  minTotalPaidAmount: '5000',
  levelCode: '',
  tagCode: '',
  tagName: ''
});

watch(
  () => [props.activeStoreId, props.stores.length],
  () => {
    form.storeId =
      props.activeStoreId !== 'all' ? props.activeStoreId : props.stores[0]?.storeId || '';
  },
  { immediate: true }
);

function toOptionalNumber(value: string) {
  if (!value.trim()) {
    return undefined;
  }

  const parsed = Number(value);
  return Number.isNaN(parsed) ? undefined : parsed;
}

function submitRule() {
  emit('createRule', {
    storeId: form.storeId,
    ruleName: form.ruleName.trim(),
    lifecycleStage: form.lifecycleStage.trim() || undefined,
    minTotalPaidAmount: toOptionalNumber(form.minTotalPaidAmount),
    levelCode: form.levelCode.trim() || undefined,
    tagCode: form.tagCode.trim(),
    tagName: form.tagName.trim()
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Segments & Tags"
    title="标签热度、分群规则与导出"
    description="这里聚合标签统计、规则创建、规则执行和当前口径导出，不把 CRM 域拆散。"
  >
    <div class="member-segment">
      <div class="member-segment__tag-cloud">
        <article v-for="tag in memberTags" :key="tag.tagCode" class="member-segment__tag-card">
          <div>
            <p class="member-segment__tag-code">{{ tag.tagCode }}</p>
            <h4 class="member-segment__tag-name">{{ tag.tagName }}</h4>
          </div>
          <StatusPill :label="tag.sourceType" :tone="tag.sourceType === 'rule' ? 'success' : 'neutral'" />
          <p class="member-segment__tag-count">{{ formatCount(tag.memberCount) }} 人</p>
        </article>
      </div>

      <form class="member-segment__form" @submit.prevent="submitRule">
        <label class="member-segment__field">
          <span>店铺</span>
          <select v-model="form.storeId">
            <option v-for="store in stores" :key="store.storeId" :value="store.storeId">
              {{ store.shopName }}
            </option>
          </select>
        </label>
        <label class="member-segment__field">
          <span>规则名称</span>
          <input v-model="form.ruleName" />
        </label>
        <label class="member-segment__field">
          <span>生命周期</span>
          <input v-model="form.lifecycleStage" placeholder="dormant / silent" />
        </label>
        <label class="member-segment__field">
          <span>最小成交额</span>
          <input v-model="form.minTotalPaidAmount" type="number" min="0" step="0.01" />
        </label>
        <label class="member-segment__field">
          <span>会员层级</span>
          <input v-model="form.levelCode" placeholder="vip / svip" />
        </label>
        <label class="member-segment__field">
          <span>标签编码</span>
          <input v-model="form.tagCode" />
        </label>
        <label class="member-segment__field member-segment__field--wide">
          <span>标签名称</span>
          <input v-model="form.tagName" />
        </label>

        <p v-if="actionError" class="member-segment__error">{{ actionError }}</p>

        <button class="member-segment__primary" type="submit" :disabled="isSubmitting">
          创建分群规则
        </button>
      </form>

      <div class="member-segment__rule-list">
        <article
          v-for="rule in sessionSegmentRules"
          :key="rule.ruleId"
          class="member-segment__rule-item"
        >
          <div>
            <h4 class="member-segment__rule-title">{{ rule.ruleName }}</h4>
            <p class="member-segment__rule-meta">
              {{ rule.tagName }} / {{ rule.lifecycleStage || 'all' }} / {{ formatCurrency(rule.minTotalPaidAmount) }}
            </p>
            <p class="member-segment__rule-meta">{{ formatDateTime(rule.createdAt) }}</p>
          </div>
          <button
            type="button"
            class="member-segment__ghost"
            :disabled="isSubmitting"
            @click="emit('runRule', rule.ruleId)"
          >
            执行规则
          </button>
        </article>

        <article
          v-for="execution in sessionSegmentExecutions"
          :key="execution.ruleId"
          class="member-segment__execution-item"
        >
          <p class="member-segment__execution-title">{{ execution.ruleName }}</p>
          <p class="member-segment__execution-body">
            命中 {{ formatCount(execution.matchedMemberCount) }} 人，成员 ID：
            {{ execution.matchedMemberIds.join(', ') || '--' }}
          </p>
        </article>
      </div>

      <div class="member-segment__export">
        <div>
          <p class="member-segment__export-label">当前群组导出</p>
          <p class="member-segment__export-body">
            {{
              latestExport
                ? `最近导出 ${latestExport.totalMembers} 人，exportId: ${latestExport.exportId}`
                : '还没有执行当前口径导出'
            }}
          </p>
        </div>
        <button
          type="button"
          class="member-segment__primary"
          :disabled="isSubmitting"
          @click="emit('exportGroup')"
        >
          导出当前群组
        </button>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.member-segment,
.member-segment__tag-cloud,
.member-segment__rule-list {
  display: grid;
  gap: 0.9rem;
}

.member-segment__tag-cloud {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.member-segment__tag-card,
.member-segment__rule-item,
.member-segment__execution-item,
.member-segment__export {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.72);
}

.member-segment__tag-card,
.member-segment__rule-item,
.member-segment__export {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.member-segment__tag-code,
.member-segment__tag-name,
.member-segment__tag-count,
.member-segment__rule-title,
.member-segment__rule-meta,
.member-segment__execution-title,
.member-segment__execution-body,
.member-segment__export-label,
.member-segment__export-body,
.member-segment__error {
  margin: 0;
}

.member-segment__tag-code,
.member-segment__export-label {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.member-segment__tag-name,
.member-segment__rule-title,
.member-segment__execution-title {
  margin-top: 0.3rem;
  color: var(--color-ink-strong);
}

.member-segment__tag-count,
.member-segment__rule-meta,
.member-segment__execution-body,
.member-segment__export-body {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.member-segment__form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

.member-segment__field {
  display: grid;
  gap: 0.45rem;
}

.member-segment__field--wide,
.member-segment__primary,
.member-segment__error {
  grid-column: span 3;
}

.member-segment__field span {
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.member-segment__field input,
.member-segment__field select {
  min-height: 2.8rem;
  padding: 0 0.9rem;
  border-radius: 1rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-ink-strong);
}

.member-segment__primary,
.member-segment__ghost {
  min-height: 2.7rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.member-segment__primary {
  border: 0;
  background: linear-gradient(135deg, #30525a, #6b8d8b);
  color: #fff8f0;
}

.member-segment__ghost {
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.66);
  color: var(--color-ink);
}

.member-segment__error {
  color: var(--color-danger);
}

@media (max-width: 1080px) {
  .member-segment__tag-cloud,
  .member-segment__form {
    grid-template-columns: 1fr;
  }

  .member-segment__field--wide,
  .member-segment__primary,
  .member-segment__error {
    grid-column: span 1;
  }
}
</style>
