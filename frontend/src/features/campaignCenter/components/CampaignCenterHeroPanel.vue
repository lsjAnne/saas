<script setup lang="ts">
import MetricCard from '@/components/cards/MetricCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { CampaignActivity, CouponTemplate } from '@/services/apiTypes';
import { formatCount, formatDate } from '@/utils/formatters';

interface Summary {
  totalCampaigns: number;
  pendingApprovalCount: number;
  approvedCount: number;
  publishedCount: number;
  couponBoundCount: number;
  enabledTemplateCount: number;
}

interface Props {
  selectedCampaign: CampaignActivity | null;
  selectedStoreName: string;
  selectedCouponTemplate: CouponTemplate | null;
  summary: Summary;
  actionFeedback?: string | null;
}

withDefaults(defineProps<Props>(), {
  actionFeedback: null
});

defineEmits<{
  openCreate: [];
  openEdit: [];
}>();

function resolveTone(value: string | null | undefined) {
  const normalized = (value ?? '').trim().toLowerCase();

  if (normalized === 'published' || normalized === 'approved') {
    return 'success';
  }

  if (normalized.includes('pending') || normalized.includes('rejected')) {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <section class="campaign-hero">
    <div class="campaign-hero__copy">
      <p class="campaign-hero__eyebrow">Campaign Command Deck</p>
      <h3 class="campaign-hero__title">
        把活动、优惠券、审批和发布轨道压进同一张能直接执行的营销主控页
      </h3>
      <p class="campaign-hero__subtitle">
        当前聚焦店铺：{{ selectedStoreName }}。首轮不做花哨编排器，先把活动排期、规则、商品挂载和审批发布链路真正接进来。
      </p>

      <div class="campaign-hero__actions">
        <button
          class="campaign-hero__button campaign-hero__button--solid"
          type="button"
          @click="$emit('openCreate')"
        >
          新建活动
        </button>
        <button class="campaign-hero__button" type="button" @click="$emit('openEdit')">
          编辑当前活动
        </button>
      </div>

      <p v-if="actionFeedback" class="campaign-hero__feedback">{{ actionFeedback }}</p>
    </div>

    <div class="campaign-hero__focus-card">
      <div class="campaign-hero__focus-head">
        <div>
          <p class="campaign-hero__focus-label">当前焦点活动</p>
          <h4 class="campaign-hero__focus-title">
            {{ selectedCampaign?.activityName || '请选择活动' }}
          </h4>
        </div>
        <StatusPill
          v-if="selectedCampaign"
          :label="selectedCampaign.status"
          :tone="resolveTone(selectedCampaign.status)"
        />
      </div>

      <div v-if="selectedCampaign" class="campaign-hero__focus-meta">
        <span>{{ selectedCampaign.activityType }}</span>
        <span>{{ formatDate(selectedCampaign.startAt) }} - {{ formatDate(selectedCampaign.endAt) }}</span>
        <span v-if="selectedCampaign.needApproval">需要审批</span>
        <span v-if="selectedCouponTemplate">{{ selectedCouponTemplate.templateName }}</span>
      </div>

      <div class="campaign-hero__focus-stats">
        <article class="campaign-hero__focus-item">
          <p class="campaign-hero__focus-item-label">挂载商品</p>
          <p class="campaign-hero__focus-item-value">
            {{ formatCount(selectedCampaign?.productIds.length ?? 0) }}
          </p>
        </article>
        <article class="campaign-hero__focus-item">
          <p class="campaign-hero__focus-item-label">券模板</p>
          <p class="campaign-hero__focus-item-value">
            {{ selectedCampaign?.couponTemplateId ? '已绑定' : '未绑定' }}
          </p>
        </article>
        <article class="campaign-hero__focus-item">
          <p class="campaign-hero__focus-item-label">发布轨道</p>
          <p class="campaign-hero__focus-item-value">
            {{ selectedCampaign?.status || '--' }}
          </p>
        </article>
      </div>
    </div>
  </section>

  <section class="campaign-hero__metrics">
    <MetricCard
      label="活动总量"
      :value="formatCount(summary.totalCampaigns)"
      caption="当前租户下已进入营销排期的活动数"
    />
    <MetricCard
      label="待审批"
      :value="formatCount(summary.pendingApprovalCount)"
      caption="已经发起审批但尚未完成的活动"
    />
    <MetricCard
      label="已发布"
      :value="formatCount(summary.publishedCount)"
      caption="已经通过审批并真正发布的活动"
    />
    <MetricCard
      label="可用券模板"
      :value="formatCount(summary.enabledTemplateCount)"
      caption="当前可被活动直接绑定的 enabled 模板数"
    />
  </section>
</template>

<style scoped>
.campaign-hero,
.campaign-hero__metrics {
  display: grid;
  gap: 1rem;
}

.campaign-hero {
  grid-template-columns: 1.08fr 0.92fr;
}

.campaign-hero__copy,
.campaign-hero__focus-card {
  padding: 1.4rem;
  border-radius: var(--radius-shell);
  box-shadow: var(--shadow-card);
}

.campaign-hero__copy {
  background:
    radial-gradient(circle at top right, rgba(181, 115, 82, 0.18), transparent 34%),
    linear-gradient(145deg, rgba(255, 250, 245, 0.96), rgba(244, 230, 224, 0.92));
  border: 1px solid rgba(123, 68, 44, 0.1);
}

.campaign-hero__focus-card {
  display: grid;
  gap: 1rem;
  background: linear-gradient(150deg, #4a221d, #251210);
  color: #fff5ed;
}

.campaign-hero__eyebrow,
.campaign-hero__title,
.campaign-hero__subtitle,
.campaign-hero__feedback,
.campaign-hero__focus-label,
.campaign-hero__focus-title,
.campaign-hero__focus-item-label,
.campaign-hero__focus-item-value {
  margin: 0;
}

.campaign-hero__eyebrow,
.campaign-hero__focus-label {
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.campaign-hero__eyebrow {
  color: var(--color-ink-faint);
}

.campaign-hero__title {
  margin-top: 0.55rem;
  max-width: 13ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3.5rem);
  line-height: 1.05;
}

.campaign-hero__subtitle,
.campaign-hero__feedback {
  margin-top: 0.8rem;
  max-width: 54rem;
  color: var(--color-ink-soft);
  line-height: 1.7;
}

.campaign-hero__feedback {
  color: var(--color-success);
}

.campaign-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  margin-top: 1.1rem;
}

.campaign-hero__button {
  min-height: 2.8rem;
  padding: 0 1.1rem;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(123, 68, 44, 0.14);
  background: rgba(255, 255, 255, 0.6);
  color: var(--color-ink-strong);
}

.campaign-hero__button--solid {
  background: linear-gradient(135deg, #8f4b35, #4b241c);
  border-color: #8f4b35;
  color: #fff8f0;
}

.campaign-hero__focus-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.campaign-hero__focus-label {
  color: rgba(255, 237, 228, 0.68);
}

.campaign-hero__focus-title {
  margin-top: 0.45rem;
  font-family: var(--font-display);
  font-size: 1.55rem;
}

.campaign-hero__focus-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  color: rgba(255, 237, 228, 0.82);
  font-size: 0.86rem;
}

.campaign-hero__focus-meta span {
  display: inline-flex;
  min-height: 2rem;
  align-items: center;
  padding: 0 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.08);
}

.campaign-hero__focus-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.campaign-hero__focus-item {
  padding: 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.08);
}

.campaign-hero__focus-item-label {
  color: rgba(255, 237, 228, 0.64);
  font-size: 0.78rem;
}

.campaign-hero__focus-item-value {
  margin-top: 0.55rem;
  font-size: 1.2rem;
  font-weight: 600;
}

.campaign-hero__metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

@media (max-width: 1180px) {
  .campaign-hero,
  .campaign-hero__metrics,
  .campaign-hero__focus-stats {
    grid-template-columns: 1fr;
  }
}
</style>
