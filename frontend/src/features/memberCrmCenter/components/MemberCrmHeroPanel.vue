<script setup lang="ts">
import MetricCard from '@/components/cards/MetricCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { MemberCrmLinkageView, MemberCrmProfileView, MemberView } from '@/services/apiTypes';
import { formatCount, formatCurrency, formatDate } from '@/utils/formatters';

interface Summary {
  totalMembers: number;
  strategicMembers: number;
  highValueMembers: number;
  dormantMembers: number;
  autoTaggedMembers: number;
  activeRecallTaskCount: number;
}

interface Props {
  selectedMember: MemberView | null;
  selectedStoreName: string;
  selectedCrmProfile: MemberCrmProfileView | null;
  selectedCrmLinkage: MemberCrmLinkageView | null;
  summary: Summary;
  actionFeedback?: string | null;
}

withDefaults(defineProps<Props>(), {
  actionFeedback: null
});

function resolveTone(linkage: MemberCrmLinkageView | null) {
  const tier = linkage?.customerTier?.toLowerCase() ?? '';
  const churn = linkage?.churnWarningLevel?.toLowerCase() ?? '';

  if (tier === 'strategic' || tier === 'key_account') {
    return 'success';
  }

  if (churn === 'high') {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <section class="member-crm-hero">
    <div class="member-crm-hero__copy">
      <p class="member-crm-hero__eyebrow">Member Operating Deck</p>
      <h3 class="member-crm-hero__title">
        把会员池、CRM 档案、标签分群和召回任务压进同一张高密度经营主控页
      </h3>
      <p class="member-crm-hero__subtitle">
        当前聚焦店铺：{{ selectedStoreName }}。这页直接承接会员经营，而不是再让“组织与成员”页面替代 CRM 主入口。
      </p>
      <p v-if="actionFeedback" class="member-crm-hero__feedback">{{ actionFeedback }}</p>
    </div>

    <div class="member-crm-hero__focus-card">
      <div class="member-crm-hero__focus-head">
        <div>
          <p class="member-crm-hero__focus-label">当前焦点会员</p>
          <h4 class="member-crm-hero__focus-title">
            {{
              selectedCrmProfile?.customerName ||
              selectedCrmLinkage?.customerName ||
              selectedMember?.nickname ||
              '请选择会员'
            }}
          </h4>
        </div>
        <StatusPill
          v-if="selectedMember"
          :label="selectedCrmLinkage?.customerTier || selectedMember.levelCode"
          :tone="resolveTone(selectedCrmLinkage)"
        />
      </div>

      <div v-if="selectedMember" class="member-crm-hero__focus-meta">
        <span>{{ selectedMember.levelCode }}</span>
        <span>{{ selectedMember.lifecycleStage }}</span>
        <span>{{ formatCurrency(selectedMember.totalPaidAmount) }}</span>
        <span>{{ formatDate(selectedMember.lastOrderAt) }}</span>
      </div>

      <div class="member-crm-hero__focus-stats">
        <article class="member-crm-hero__focus-item">
          <p class="member-crm-hero__focus-item-label">标签数</p>
          <p class="member-crm-hero__focus-item-value">
            {{ formatCount(selectedMember?.tags.length ?? 0) }}
          </p>
        </article>
        <article class="member-crm-hero__focus-item">
          <p class="member-crm-hero__focus-item-label">关联订单</p>
          <p class="member-crm-hero__focus-item-value">
            {{ formatCount(selectedCrmLinkage?.relatedOrderCount ?? 0) }}
          </p>
        </article>
        <article class="member-crm-hero__focus-item">
          <p class="member-crm-hero__focus-item-label">激活触达</p>
          <p class="member-crm-hero__focus-item-value">
            {{ formatCount(selectedCrmLinkage?.activeTouchTaskCount ?? 0) }}
          </p>
        </article>
      </div>
    </div>
  </section>

  <section class="member-crm-hero__metrics">
    <MetricCard
      label="会员总量"
      :value="formatCount(summary.totalMembers)"
      caption="当前筛选口径下的会员总数"
    />
    <MetricCard
      label="战略会员"
      :value="formatCount(summary.strategicMembers)"
      caption="已进入 strategic / key account 经营层的客户数"
    />
    <MetricCard
      label="高价值沉默"
      :value="formatCount(summary.dormantMembers)"
      caption="需要被唤醒的 dormant 会员数"
    />
    <MetricCard
      label="进行中召回"
      :value="formatCount(summary.activeRecallTaskCount)"
      caption="planned / in_progress 的 recall 任务"
    />
  </section>
</template>

<style scoped>
.member-crm-hero,
.member-crm-hero__metrics {
  display: grid;
  gap: 1rem;
}

.member-crm-hero {
  grid-template-columns: 1.05fr 0.95fr;
}

.member-crm-hero__copy,
.member-crm-hero__focus-card {
  padding: 1.4rem;
  border-radius: var(--radius-shell);
  box-shadow: var(--shadow-card);
}

.member-crm-hero__copy {
  background:
    radial-gradient(circle at top right, rgba(86, 118, 120, 0.18), transparent 34%),
    linear-gradient(145deg, rgba(250, 252, 251, 0.96), rgba(229, 238, 236, 0.92));
  border: 1px solid rgba(71, 96, 101, 0.1);
}

.member-crm-hero__focus-card {
  display: grid;
  gap: 1rem;
  background: linear-gradient(150deg, #21353b, #111b1e);
  color: #eef8f6;
}

.member-crm-hero__eyebrow,
.member-crm-hero__title,
.member-crm-hero__subtitle,
.member-crm-hero__feedback,
.member-crm-hero__focus-label,
.member-crm-hero__focus-title,
.member-crm-hero__focus-item-label,
.member-crm-hero__focus-item-value {
  margin: 0;
}

.member-crm-hero__eyebrow,
.member-crm-hero__focus-label {
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.member-crm-hero__eyebrow {
  color: var(--color-ink-faint);
}

.member-crm-hero__title {
  margin-top: 0.55rem;
  max-width: 14ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3.5rem);
  line-height: 1.05;
}

.member-crm-hero__subtitle,
.member-crm-hero__feedback {
  margin-top: 0.8rem;
  max-width: 54rem;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.member-crm-hero__feedback {
  color: var(--color-success);
}

.member-crm-hero__focus-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.member-crm-hero__focus-label {
  color: rgba(234, 248, 244, 0.68);
}

.member-crm-hero__focus-title {
  margin-top: 0.45rem;
  font-family: var(--font-display);
  font-size: 1.55rem;
}

.member-crm-hero__focus-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  color: rgba(234, 248, 244, 0.82);
  font-size: 0.86rem;
}

.member-crm-hero__focus-meta span {
  display: inline-flex;
  min-height: 2rem;
  align-items: center;
  padding: 0 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.08);
}

.member-crm-hero__focus-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.member-crm-hero__focus-item {
  padding: 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.08);
}

.member-crm-hero__focus-item-label {
  color: rgba(234, 248, 244, 0.64);
  font-size: 0.78rem;
}

.member-crm-hero__focus-item-value {
  margin-top: 0.55rem;
  font-size: 1.2rem;
  font-weight: 600;
}

.member-crm-hero__metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

@media (max-width: 1180px) {
  .member-crm-hero,
  .member-crm-hero__metrics,
  .member-crm-hero__focus-stats {
    grid-template-columns: 1fr;
  }
}
</style>
