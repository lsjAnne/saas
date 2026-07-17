<script setup lang="ts">
import { computed } from 'vue';

import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { CustomerServiceTicket, TicketSlaOverviewView } from '@/services/apiTypes';
import { formatDateTime, formatPercent } from '@/utils/formatters';

interface Summary {
  totalTicketCount: number;
  riskTicketCount: number;
  processingTicketCount: number;
  overdueTicketCount: number;
  withinSlaRate: number;
  averageFirstResponseMinutes: number;
  satisfactionCount: number;
  averageSatisfactionScore: number;
}

interface Props {
  selectedTicket: CustomerServiceTicket | null;
  selectedStoreName: string;
  ticketSlaOverview: TicketSlaOverviewView | null;
  summary: Summary;
  actionFeedback?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionFeedback: null
});

const focusTone = computed<'neutral' | 'warn' | 'success'>(() => {
  if (props.selectedTicket?.riskFlag) {
    return 'warn';
  }

  if ((props.selectedTicket?.ticketStatus ?? '').toLowerCase() === 'processing') {
    return 'success';
  }

  return 'neutral';
});

function formatMinutes(value: number) {
  if (!Number.isFinite(value) || value <= 0) {
    return '--';
  }

  return `${value.toFixed(1)} 分钟`;
}

function formatScore(value: number) {
  if (!Number.isFinite(value) || value <= 0) {
    return '--';
  }

  return `${value.toFixed(1)} / 5`;
}
</script>

<template>
  <PanelCard
    eyebrow="Customer Service Center"
    title="把工单响应、满意度回收和售后审批收进同一条客服主链"
    description="这页不再承担会话聊天辅助，而是把售后主链最关键的响应面、动作面和审批面并排放进一个控制台。"
    dark
  >
    <div class="customer-service-hero">
      <div class="customer-service-hero__copy">
        <p class="customer-service-hero__eyebrow">当前服务面</p>
        <h4 class="customer-service-hero__title">{{ selectedStoreName }}</h4>
        <p class="customer-service-hero__summary">
          当前筛选范围下共有 {{ summary.totalTicketCount }} 个工单，其中
          {{ summary.overdueTicketCount }} 个已逾期，{{ summary.riskTicketCount }} 个处于风险态。
        </p>
        <p v-if="actionFeedback" class="customer-service-hero__feedback">{{ actionFeedback }}</p>
      </div>

      <div class="customer-service-hero__focus">
        <div class="customer-service-hero__focus-header">
          <div>
            <p class="customer-service-hero__focus-eyebrow">当前焦点工单</p>
            <h5 class="customer-service-hero__focus-title">
              {{ selectedTicket?.ticketId || '等待选择工单' }}
            </h5>
          </div>
          <StatusPill
            :label="selectedTicket?.ticketStatus || 'idle'"
            :tone="focusTone"
          />
        </div>
        <p class="customer-service-hero__focus-meta">
          订单 {{ selectedTicket?.orderId || '--' }} / 客户 {{ selectedTicket?.customerId || '--' }}
        </p>
        <p class="customer-service-hero__focus-meta">
          创建时间 {{ formatDateTime(selectedTicket?.createdAt) }}
        </p>
        <p class="customer-service-hero__focus-note">
          {{
            selectedTicket?.aiReplySuggestion
              ? '当前工单已有 AI 回复建议，可直接在右侧详情面板评估并落满意度。'
              : '当前工单还没有 AI 回复建议，右侧详情面板可直接生成并进入处理态。'
          }}
        </p>
      </div>
    </div>

    <div class="customer-service-hero__metrics">
      <MetricCard
        label="SLA 达标率"
        :value="formatPercent(summary.withinSlaRate, 0)"
        caption="按当前店铺范围实时回读"
      />
      <MetricCard
        label="平均首响"
        :value="formatMinutes(summary.averageFirstResponseMinutes)"
        caption="由工单与首响记录共同计算"
      />
      <MetricCard
        label="满意度均分"
        :value="formatScore(summary.averageSatisfactionScore)"
        :caption="`已收集 ${summary.satisfactionCount} 次评分`"
      />
      <MetricCard
        label="处理中工单"
        :value="`${summary.processingTicketCount}`"
        :caption="`风险工单 ${summary.riskTicketCount} 个`"
      />
    </div>

    <p v-if="ticketSlaOverview" class="customer-service-hero__tail">
      总工单 {{ ticketSlaOverview.totalTicketCount }}，SLA 内 {{ ticketSlaOverview.withinSlaCount }}，
      逾期 {{ ticketSlaOverview.overdueTicketCount }}。这一组指标直接作为客服售后主链的日常看板。
    </p>
  </PanelCard>
</template>

<style scoped>
.customer-service-hero,
.customer-service-hero__copy,
.customer-service-hero__focus {
  display: grid;
  gap: 0.9rem;
}

.customer-service-hero {
  grid-template-columns: 1.1fr 0.9fr;
  align-items: stretch;
}

.customer-service-hero__copy {
  padding: 1.05rem;
  border-radius: var(--radius-lg);
  background: linear-gradient(145deg, rgba(255, 244, 233, 0.12), rgba(255, 255, 255, 0.04));
  border: 1px solid rgba(255, 241, 231, 0.08);
}

.customer-service-hero__focus {
  padding: 1.05rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 248, 240, 0.1);
  border: 1px solid rgba(255, 244, 236, 0.08);
}

.customer-service-hero__eyebrow,
.customer-service-hero__title,
.customer-service-hero__summary,
.customer-service-hero__feedback,
.customer-service-hero__focus-eyebrow,
.customer-service-hero__focus-title,
.customer-service-hero__focus-meta,
.customer-service-hero__focus-note,
.customer-service-hero__tail {
  margin: 0;
}

.customer-service-hero__eyebrow,
.customer-service-hero__focus-eyebrow {
  color: rgba(255, 244, 236, 0.7);
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.customer-service-hero__title,
.customer-service-hero__focus-title {
  color: #fff8f0;
}

.customer-service-hero__title {
  font-family: var(--font-display);
  font-size: clamp(1.9rem, 3vw, 2.7rem);
  line-height: 1.02;
}

.customer-service-hero__summary,
.customer-service-hero__feedback,
.customer-service-hero__focus-meta,
.customer-service-hero__focus-note,
.customer-service-hero__tail {
  color: rgba(255, 244, 236, 0.78);
  line-height: 1.7;
}

.customer-service-hero__feedback {
  color: #f6d8b5;
}

.customer-service-hero__focus-header {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.customer-service-hero__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
  margin-top: 1rem;
}

.customer-service-hero__tail {
  margin-top: 1rem;
}

@media (max-width: 1080px) {
  .customer-service-hero,
  .customer-service-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
