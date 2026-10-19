<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  MemberCrmLinkageView,
  MemberCrmProfileView,
  MemberView,
  SaveMemberCrmProfilePayload,
  SaveMemberTagPayload
} from '@/services/apiTypes';
import { formatCount, formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  member: MemberView | null;
  crmProfile: MemberCrmProfileView | null;
  crmLinkage: MemberCrmLinkageView | null;
  isSubmitting: boolean;
  actionError?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<{
  submitProfile: [payload: SaveMemberCrmProfilePayload];
  addTag: [payload: SaveMemberTagPayload];
  removeTag: [memberTagId: string];
}>();

const profileForm = reactive({
  customerName: '',
  primaryContactName: '',
  primaryContactMobile: '',
  wechatId: '',
  sourceChannel: '',
  sourceDetail: '',
  customerTier: 'standard'
});

const tagForm = reactive({
  tagCode: '',
  tagName: '',
  sourceType: 'manual'
});

const visibleCustomerName = computed(
  () =>
    props.crmProfile?.customerName ||
    props.crmLinkage?.customerName ||
    props.member?.nickname ||
    '--'
);

watch(
  () => [props.member?.memberId, props.crmProfile?.updatedAt, props.crmLinkage?.memberId],
  () => {
    profileForm.customerName =
      props.crmProfile?.customerName ||
      props.crmLinkage?.customerName ||
      props.member?.nickname ||
      '';
    profileForm.primaryContactName =
      props.crmProfile?.primaryContactName || props.crmLinkage?.primaryContactName || '';
    profileForm.primaryContactMobile = props.crmProfile?.primaryContactMobile || '';
    profileForm.wechatId = props.crmProfile?.wechatId || '';
    profileForm.sourceChannel =
      props.crmProfile?.sourceChannel || props.crmLinkage?.sourceChannel || '';
    profileForm.sourceDetail = props.crmProfile?.sourceDetail || '';
    profileForm.customerTier =
      props.crmProfile?.customerTier || props.crmLinkage?.customerTier || 'standard';

    tagForm.tagCode = '';
    tagForm.tagName = '';
    tagForm.sourceType = 'manual';
  },
  { immediate: true }
);

function submitProfile() {
  emit('submitProfile', {
    customerName: profileForm.customerName.trim(),
    primaryContactName: profileForm.primaryContactName.trim(),
    primaryContactMobile: profileForm.primaryContactMobile.trim(),
    wechatId: profileForm.wechatId.trim(),
    sourceChannel: profileForm.sourceChannel.trim(),
    sourceDetail: profileForm.sourceDetail.trim(),
    customerTier: profileForm.customerTier.trim()
  });
}

function submitTag() {
  emit('addTag', {
    tagCode: tagForm.tagCode.trim(),
    tagName: tagForm.tagName.trim(),
    sourceType: tagForm.sourceType.trim()
  });
}

function resolveTone() {
  const churn = props.crmLinkage?.churnWarningLevel?.toLowerCase() ?? '';

  if (churn === 'high') {
    return 'warn';
  }

  if ((props.crmLinkage?.customerTier ?? '').toLowerCase() === 'strategic') {
    return 'success';
  }

  return 'neutral';
}

const labelMap: Record<string, string> = {
  standard: '普通客户',
  vip: 'VIP客户',
  strategic: '战略客户',
  key_account: '重点客户',
  normal: '普通会员',
  gold: '黄金会员',
  svip: '超级VIP',
  new: '新客',
  active: '活跃',
  dormant: '沉默',
  silent: '静默',
  churn_risk: '流失风险',
  low: '低风险',
  medium: '中风险',
  high: '高风险',
  manual: '手工',
  rule: '规则',
  ai: 'AI'
};

function labelOf(value: string) {
  return labelMap[value] ?? value;
}
</script>

<template>
  <PanelCard
    title="会员档案与标签治理"
    description="CRM 档案、手工标签和会员联动摘要放在一个面板里，避免来回切页。"
  >
    <div class="member-profile">
      <div v-if="member" class="member-profile__summary">
        <div class="member-profile__summary-copy">
          <p class="member-profile__summary-eyebrow">当前 CRM 档案</p>
          <h4 class="member-profile__summary-title">{{ visibleCustomerName }}</h4>
          <p class="member-profile__summary-meta">
            {{ labelOf(member.levelCode) }} / {{ labelOf(member.lifecycleStage) }} / {{ formatCurrency(member.totalPaidAmount) }}
          </p>
          <p class="member-profile__summary-time">
            最近联动 {{ formatDateTime(crmProfile?.updatedAt || member.lastOrderAt) }}
          </p>
        </div>
        <div class="member-profile__summary-tags">
          <StatusPill
            :label="labelOf(crmLinkage?.customerTier || member.levelCode)"
            :tone="resolveTone()"
          />
          <StatusPill
            v-if="crmLinkage?.churnWarningLevel && crmLinkage.churnWarningLevel !== 'none'"
            :label="labelOf(crmLinkage.churnWarningLevel)"
            tone="warn"
          />
        </div>
      </div>

      <div v-if="member" class="member-profile__metrics">
        <article class="member-profile__metric">
          <p class="member-profile__metric-label">订单</p>
          <p class="member-profile__metric-value">{{ formatCount(crmLinkage?.relatedOrderCount ?? 0) }}</p>
        </article>
        <article class="member-profile__metric">
          <p class="member-profile__metric-label">售后</p>
          <p class="member-profile__metric-value">{{ formatCount(crmLinkage?.relatedAfterSaleCount ?? 0) }}</p>
        </article>
        <article class="member-profile__metric">
          <p class="member-profile__metric-label">工单</p>
          <p class="member-profile__metric-value">{{ formatCount(crmLinkage?.relatedTicketCount ?? 0) }}</p>
        </article>
        <article class="member-profile__metric">
          <p class="member-profile__metric-label">活动</p>
          <p class="member-profile__metric-value">{{ formatCount(crmLinkage?.relatedCampaignCount ?? 0) }}</p>
        </article>
      </div>

      <form v-if="member" class="member-profile__form" @submit.prevent="submitProfile">
        <label class="member-profile__field">
          <span>客户名称</span>
          <input v-model="profileForm.customerName" :disabled="isSubmitting" />
        </label>
        <label class="member-profile__field">
          <span>客户层级</span>
          <select v-model="profileForm.customerTier" :disabled="isSubmitting">
            <option value="standard">普通客户</option>
            <option value="vip">VIP客户</option>
            <option value="strategic">战略客户</option>
            <option value="key_account">重点客户</option>
          </select>
        </label>
        <label class="member-profile__field">
          <span>联系人</span>
          <input v-model="profileForm.primaryContactName" :disabled="isSubmitting" />
        </label>
        <label class="member-profile__field">
          <span>联系电话</span>
          <input v-model="profileForm.primaryContactMobile" :disabled="isSubmitting" />
        </label>
        <label class="member-profile__field">
          <span>微信号</span>
          <input v-model="profileForm.wechatId" :disabled="isSubmitting" />
        </label>
        <label class="member-profile__field">
          <span>来源渠道</span>
          <input v-model="profileForm.sourceChannel" :disabled="isSubmitting" />
        </label>
        <label class="member-profile__field member-profile__field--wide">
          <span>来源明细</span>
          <textarea v-model="profileForm.sourceDetail" rows="3" :disabled="isSubmitting" />
        </label>

        <p v-if="actionError" class="member-profile__error">{{ actionError }}</p>

        <button class="member-profile__submit" type="submit" :disabled="isSubmitting">
          保存 CRM 档案
        </button>
      </form>

      <div v-if="member" class="member-profile__tags">
        <div class="member-profile__tag-list">
          <button
            v-for="tag in member.tags"
            :key="tag.memberTagId"
            type="button"
            class="member-profile__tag-chip"
            @click="emit('removeTag', tag.memberTagId)"
          >
            {{ tag.tagName }} / {{ labelOf(tag.sourceType) }}
          </button>
        </div>

        <form class="member-profile__tag-form" @submit.prevent="submitTag">
          <label class="member-profile__field">
            <span>标签编码</span>
            <input v-model="tagForm.tagCode" :disabled="isSubmitting" />
          </label>
          <label class="member-profile__field">
            <span>标签名称</span>
            <input v-model="tagForm.tagName" :disabled="isSubmitting" />
          </label>
          <label class="member-profile__field">
            <span>来源类型</span>
            <select v-model="tagForm.sourceType" :disabled="isSubmitting">
              <option value="manual">手工</option>
              <option value="rule">规则</option>
              <option value="ai">AI</option>
            </select>
          </label>
          <button class="member-profile__ghost" type="submit" :disabled="isSubmitting">
            新增标签
          </button>
        </form>
      </div>

      <div v-else class="member-profile__empty">
        先从左侧选择一个会员，再编辑 CRM 档案和标签。
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.member-profile,
.member-profile__summary,
.member-profile__summary-copy,
.member-profile__summary-tags,
.member-profile__tags,
.member-profile__tag-list {
  display: grid;
  gap: 0.9rem;
}

.member-profile__summary {
  grid-template-columns: 1fr auto;
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
}

.member-profile__summary-eyebrow,
.member-profile__summary-title,
.member-profile__summary-meta,
.member-profile__summary-time,
.member-profile__error {
  margin: 0;
}

.member-profile__summary-eyebrow {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.member-profile__summary-title {
  color: var(--color-ink-strong);
  font-size: 1.2rem;
}

.member-profile__summary-meta,
.member-profile__summary-time {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.member-profile__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
}

.member-profile__metric {
  padding: 0.9rem;
  border-radius: var(--radius-lg);
  background: var(--color-panel-strong);
  border: 1px solid var(--color-line);
}

.member-profile__metric-label,
.member-profile__metric-value {
  margin: 0;
}

.member-profile__metric-label {
  color: var(--color-ink-soft);
  font-size: 0.8rem;
}

.member-profile__metric-value {
  margin-top: 0.45rem;
  color: var(--color-ink-strong);
  font-size: 1.1rem;
}

.member-profile__form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

.member-profile__field,
.member-profile__tag-form {
  display: grid;
  gap: 0.45rem;
}

.member-profile__field--wide,
.member-profile__submit,
.member-profile__error {
  grid-column: span 3;
}

.member-profile__field span {
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.member-profile__field input,
.member-profile__field select,
.member-profile__field textarea {
  min-height: 2.8rem;
  padding: 0.75rem 0.9rem;
  border-radius: 1rem;
  border: 1px solid var(--color-line);
  background: #ffffff;
  color: var(--color-ink-strong);
  font: inherit;
}

.member-profile__submit,
.member-profile__ghost {
  min-height: 2.7rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.member-profile__submit {
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #ffffff;
}

.member-profile__submit:hover {
  background: var(--color-accent-hover);
}

.member-profile__ghost,
.member-profile__tag-chip {
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.member-profile__ghost:hover,
.member-profile__tag-chip:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.member-profile__tag-list {
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
}

.member-profile__tag-chip {
  min-height: 2.4rem;
  padding: 0 0.9rem;
  border-radius: var(--radius-pill);
  text-align: left;
}

.member-profile__tag-form {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: end;
}

.member-profile__error {
  color: var(--color-danger);
}

.member-profile__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
  color: var(--color-ink-soft);
  line-height: 1.7;
}

@media (max-width: 1080px) {
  .member-profile__summary,
  .member-profile__metrics,
  .member-profile__form,
  .member-profile__tag-form {
    grid-template-columns: 1fr;
  }

  .member-profile__field--wide,
  .member-profile__submit,
  .member-profile__error {
    grid-column: span 1;
  }
}
</style>
