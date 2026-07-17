<script setup lang="ts">
import { computed } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { CampaignActivity, CouponTemplate, Store } from '@/services/apiTypes';
import { formatCount, formatDate, formatDateTime } from '@/utils/formatters';

interface Props {
  stores: Store[];
  campaigns: CampaignActivity[];
  activeStoreId: string;
  selectedCampaignId: string | null;
  selectedCampaign: CampaignActivity | null;
  selectedCouponTemplate: CouponTemplate | null;
  isRunningAction: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  selectStore: [storeId: string];
  selectCampaign: [campaignId: string];
  openCreate: [];
  openEdit: [];
  submitApproval: [];
  publish: [];
}>();

const storeOptions = computed(() => [
  { storeId: 'all', shopName: '全部店铺' },
  ...props.stores
]);

function resolveTone(value: string | null | undefined) {
  const normalized = (value ?? '').trim().toLowerCase();

  if (normalized === 'approved' || normalized === 'published') {
    return 'success';
  }

  if (normalized.includes('pending') || normalized.includes('rejected')) {
    return 'warn';
  }

  return 'neutral';
}

function canSubmitApproval(campaign: CampaignActivity | null) {
  if (!campaign) {
    return false;
  }

  return ['draft', 'rejected'].includes((campaign.status ?? '').toLowerCase());
}

function canPublish(campaign: CampaignActivity | null) {
  return (campaign?.status ?? '').toLowerCase() === 'approved';
}

function formatRule(campaign: CampaignActivity | null) {
  if (!campaign) {
    return '--';
  }

  const rule = campaign.rule ?? {};

  if (campaign.activityType === 'full_reduction') {
    return `满 ${rule.thresholdAmount ?? '--'} 减 ${rule.discountAmount ?? '--'}`;
  }

  if (campaign.activityType === 'discount') {
    const rate = Number(rule.discountRate ?? 0);
    return rate > 0 ? `${(rate * 10).toFixed(1)} 折` : '折扣规则未填写';
  }

  return String(rule.note ?? '已配置基础规则');
}
</script>

<template>
  <PanelCard
    eyebrow="Campaign Board"
    title="活动看板与审批轨道"
    description="左侧先按店铺和活动状态锁定对象，右侧直接处理活动详情、审批提交与发布动作。"
  >
    <div class="campaign-board">
      <div class="campaign-board__rail">
        <div class="campaign-board__rail-actions">
          <button type="button" class="campaign-board__primary" @click="$emit('openCreate')">
            新建活动
          </button>
        </div>

        <div class="campaign-board__store-switcher">
          <button
            v-for="store in storeOptions"
            :key="store.storeId"
            type="button"
            :class="[
              'campaign-board__store-chip',
              { 'campaign-board__store-chip--active': store.storeId === activeStoreId }
            ]"
            @click="emit('selectStore', store.storeId)"
          >
            {{ store.shopName }}
          </button>
        </div>

        <button
          v-for="campaign in campaigns"
          :key="campaign.campaignId"
          type="button"
          :class="[
            'campaign-board__item',
            { 'campaign-board__item--active': campaign.campaignId === selectedCampaignId }
          ]"
          @click="$emit('selectCampaign', campaign.campaignId)"
        >
          <div class="campaign-board__item-copy">
            <strong class="campaign-board__item-title">{{ campaign.activityName }}</strong>
            <span class="campaign-board__item-meta">
              {{ campaign.activityType }} / {{ formatDate(campaign.startAt) }}
            </span>
          </div>
          <StatusPill :label="campaign.status" :tone="resolveTone(campaign.status)" />
        </button>

        <p v-if="!campaigns.length" class="campaign-board__empty">
          当前筛选下还没有活动。先新建一个活动，再把商品 ID 和规则补完整。
        </p>
      </div>

      <div class="campaign-board__detail">
        <template v-if="selectedCampaign">
          <div class="campaign-board__detail-head">
            <div>
              <p class="campaign-board__label">Selected Campaign</p>
              <h4 class="campaign-board__detail-title">{{ selectedCampaign.activityName }}</h4>
            </div>
            <div class="campaign-board__detail-actions">
              <button type="button" class="campaign-board__ghost" @click="$emit('openEdit')">
                编辑活动
              </button>
              <button
                type="button"
                class="campaign-board__ghost"
                :disabled="!canSubmitApproval(selectedCampaign) || isRunningAction"
                @click="$emit('submitApproval')"
              >
                提交审批
              </button>
              <button
                type="button"
                class="campaign-board__primary"
                :disabled="!canPublish(selectedCampaign) || isRunningAction"
                @click="$emit('publish')"
              >
                发布活动
              </button>
            </div>
          </div>

          <dl class="campaign-board__facts">
            <div class="campaign-board__fact">
              <dt>活动类型</dt>
              <dd>{{ selectedCampaign.activityType }}</dd>
            </div>
            <div class="campaign-board__fact">
              <dt>活动窗口</dt>
              <dd>{{ formatDate(selectedCampaign.startAt) }} - {{ formatDate(selectedCampaign.endAt) }}</dd>
            </div>
            <div class="campaign-board__fact">
              <dt>挂载商品</dt>
              <dd>{{ formatCount(selectedCampaign.productIds.length) }}</dd>
            </div>
            <div class="campaign-board__fact">
              <dt>券模板</dt>
              <dd>{{ selectedCouponTemplate?.templateName || '未绑定' }}</dd>
            </div>
          </dl>

          <div class="campaign-board__copy-block">
            <p class="campaign-board__label">规则摘要</p>
            <p class="campaign-board__body">{{ formatRule(selectedCampaign) }}</p>
          </div>

          <div class="campaign-board__copy-block">
            <p class="campaign-board__label">执行提示</p>
            <p class="campaign-board__body">
              创建时间：{{ formatDateTime(selectedCampaign.createdAt) }}。审批前至少需要挂载 1 个商品 ID，发布前必须先进入 approved 状态。
            </p>
          </div>
        </template>

        <p v-else class="campaign-board__empty">
          先从左侧选择一个活动，再决定是否提交审批或进入发布。
        </p>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.campaign-board {
  display: grid;
  grid-template-columns: 0.96fr 1.04fr;
  gap: 0.95rem;
}

.campaign-board__rail,
.campaign-board__detail,
.campaign-board__copy-block {
  display: grid;
  gap: 0.8rem;
}

.campaign-board__rail-actions,
.campaign-board__detail-head,
.campaign-board__detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  justify-content: space-between;
}

.campaign-board__store-switcher {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.campaign-board__primary,
.campaign-board__ghost,
.campaign-board__store-chip {
  min-height: 2.85rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.campaign-board__primary {
  border: 0;
  background: linear-gradient(135deg, #5b241d, #a1543d);
  color: #fff8f0;
}

.campaign-board__ghost,
.campaign-board__store-chip {
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.74);
  color: var(--color-ink);
}

.campaign-board__store-chip--active {
  border-color: rgba(161, 84, 61, 0.3);
  background: rgba(250, 238, 231, 0.96);
  color: #6d3428;
}

.campaign-board__primary:disabled,
.campaign-board__ghost:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.campaign-board__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.74);
  text-align: left;
}

.campaign-board__item--active {
  border-color: rgba(161, 84, 61, 0.32);
  background: linear-gradient(135deg, rgba(252, 240, 236, 0.96), rgba(246, 228, 220, 0.88));
}

.campaign-board__item-copy,
.campaign-board__detail {
  display: grid;
  gap: 0.35rem;
}

.campaign-board__item-title,
.campaign-board__item-meta,
.campaign-board__label,
.campaign-board__detail-title,
.campaign-board__body,
.campaign-board__empty {
  margin: 0;
}

.campaign-board__detail {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(250, 245, 241, 0.84);
}

.campaign-board__item-title,
.campaign-board__detail-title {
  color: var(--color-ink-strong);
}

.campaign-board__item-meta,
.campaign-board__label,
.campaign-board__body,
.campaign-board__empty {
  color: var(--color-ink-soft);
}

.campaign-board__label {
  font-size: 0.76rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.campaign-board__detail-title {
  margin-top: 0.35rem;
  font-family: var(--font-display);
  font-size: 1.55rem;
}

.campaign-board__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
  margin: 0;
}

.campaign-board__fact {
  display: grid;
  gap: 0.35rem;
  padding: 0.8rem 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.72);
}

.campaign-board__fact dt {
  color: var(--color-ink-soft);
  font-size: 0.8rem;
}

.campaign-board__fact dd {
  margin: 0;
  color: var(--color-ink-strong);
}

.campaign-board__body,
.campaign-board__empty {
  line-height: 1.7;
}

@media (max-width: 1120px) {
  .campaign-board {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .campaign-board__facts {
    grid-template-columns: 1fr;
  }
}
</style>
