<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  FulfillmentTask,
  LogisticsRecord,
  OrderAuditReviewView,
  OrderDetailView,
  OrderOrchestrationView
} from '@/services/apiTypes';
import { formatCurrency, formatDateTime } from '@/utils/formatters';

interface Props {
  orderDetail: OrderDetailView | null;
  orchestration: OrderOrchestrationView | null;
  auditReview: OrderAuditReviewView | null;
  fulfillmentTask: FulfillmentTask | null;
  logisticsRecords: LogisticsRecord[];
  canManageFulfillment: boolean;
}

defineProps<Props>();

function toneFromStatus(status: string | null | undefined) {
  const normalized = (status ?? '').toLowerCase();

  if (
    normalized.includes('success') ||
    normalized.includes('ready') ||
    normalized.includes('delivered')
  ) {
    return 'success';
  }

  if (
    normalized.includes('risk') ||
    normalized.includes('manual') ||
    normalized.includes('failed') ||
    normalized.includes('refund')
  ) {
    return 'warn';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Detail Surface"
    title="订单详情与编排态"
    description="订单主信息、商品行、编排摘要、审单结果和物流记录都在这块统一展示。"
  >
    <div v-if="orderDetail" class="order-detail">
      <div class="order-detail__section">
        <div class="order-detail__section-head">
          <div>
            <p class="order-detail__headline">{{ orderDetail.order.platformOrderId }}</p>
            <p class="order-detail__subheadline">
              订单 {{ orderDetail.order.orderId }} · 店铺 {{ orderDetail.order.storeId }}
            </p>
          </div>
          <div class="order-detail__pills">
            <StatusPill
              :label="orderDetail.order.orderStatus"
              :tone="toneFromStatus(orderDetail.order.orderStatus)"
            />
            <StatusPill
              :label="orderDetail.order.logisticsStatus"
              :tone="toneFromStatus(orderDetail.order.logisticsStatus)"
            />
          </div>
        </div>

        <dl class="order-detail__facts">
          <div class="order-detail__fact">
            <dt>买家信息</dt>
            <dd>{{ orderDetail.order.buyerName || '--' }} / {{ orderDetail.order.buyerPhoneMask || '--' }}</dd>
          </div>
          <div class="order-detail__fact">
            <dt>订单金额</dt>
            <dd>{{ formatCurrency(orderDetail.order.totalAmount) }}</dd>
          </div>
          <div class="order-detail__fact">
            <dt>预计利润</dt>
            <dd>{{ formatCurrency(orderDetail.order.estimatedProfit) }}</dd>
          </div>
          <div class="order-detail__fact">
            <dt>收货地址</dt>
            <dd>{{ orderDetail.order.shippingAddress || '--' }}</dd>
          </div>
          <div class="order-detail__fact">
            <dt>超时点</dt>
            <dd>{{ formatDateTime(orderDetail.order.timeoutAt) }}</dd>
          </div>
          <div class="order-detail__fact">
            <dt>创建时间</dt>
            <dd>{{ formatDateTime(orderDetail.order.createdAt) }}</dd>
          </div>
        </dl>
      </div>

      <div class="order-detail__section">
        <div class="order-detail__section-head">
          <p class="order-detail__section-title">商品行</p>
          <p class="order-detail__section-meta">{{ orderDetail.items.length }} 行</p>
        </div>

        <div class="order-detail__item-list">
          <article
            v-for="item in orderDetail.items"
            :key="item.orderItemId"
            class="order-detail__item-card"
          >
            <p class="order-detail__item-title">{{ item.productId }}</p>
            <p class="order-detail__item-meta">SKU {{ item.skuId }}</p>
            <p class="order-detail__item-caption">
              数量 {{ item.quantity }} · 单价 {{ formatCurrency(item.unitPrice) }}
            </p>
          </article>
        </div>
      </div>

      <div v-if="orchestration" class="order-detail__section">
        <div class="order-detail__section-head">
          <p class="order-detail__section-title">编排摘要</p>
          <p class="order-detail__section-meta">OMS / WMS / TMS / Finance</p>
        </div>

        <dl class="order-detail__facts">
          <div class="order-detail__fact">
            <dt>履约任务</dt>
            <dd>{{ orchestration.fulfillmentTaskId || '--' }}</dd>
          </div>
          <div class="order-detail__fact">
            <dt>拆单子项</dt>
            <dd>{{ orchestration.splitChildCount }}</dd>
          </div>
          <div class="order-detail__fact">
            <dt>合单数量</dt>
            <dd>{{ orchestration.mergeOrderCount }}</dd>
          </div>
          <div class="order-detail__fact">
            <dt>路由仓</dt>
            <dd>{{ orchestration.routeWarehouseCode || '--' }}</dd>
          </div>
        </dl>

        <div class="order-detail__pill-row">
          <StatusPill :label="`WMS ${orchestration.wmsSyncStatus || '--'}`" :tone="toneFromStatus(orchestration.wmsSyncStatus)" />
          <StatusPill :label="`TMS ${orchestration.tmsSyncStatus || '--'}`" :tone="toneFromStatus(orchestration.tmsSyncStatus)" />
          <StatusPill :label="`Finance ${orchestration.financeSyncStatus || '--'}`" :tone="toneFromStatus(orchestration.financeSyncStatus)" />
        </div>
      </div>

      <div v-if="auditReview" class="order-detail__section">
        <div class="order-detail__section-head">
          <p class="order-detail__section-title">审单结果</p>
          <StatusPill :label="auditReview.riskLevel" :tone="toneFromStatus(auditReview.riskLevel)" />
        </div>
        <p class="order-detail__review-copy">
          地址校验 {{ auditReview.addressValid ? '通过' : '未通过' }} · 复核时间
          {{ formatDateTime(auditReview.reviewedAt) }}
        </p>
        <div class="order-detail__pill-row">
          <StatusPill
            v-for="tag in auditReview.riskTags"
            :key="tag"
            :label="tag"
            tone="warn"
          />
        </div>
      </div>

      <div v-if="canManageFulfillment" class="order-detail__section">
        <div class="order-detail__section-head">
          <div>
            <p class="order-detail__section-title">履约与物流</p>
            <p class="order-detail__section-meta">
              {{ fulfillmentTask?.fulfillmentTaskId || '尚未选择履约任务' }}
            </p>
          </div>
          <StatusPill
            v-if="fulfillmentTask"
            :label="fulfillmentTask.status"
            :tone="toneFromStatus(fulfillmentTask.status)"
          />
        </div>

        <dl v-if="fulfillmentTask" class="order-detail__facts">
          <div class="order-detail__fact">
            <dt>重试次数</dt>
            <dd>{{ fulfillmentTask.retryCount ?? 0 }}</dd>
          </div>
          <div class="order-detail__fact">
            <dt>最近错误</dt>
            <dd>{{ fulfillmentTask.lastErrorMessage || '--' }}</dd>
          </div>
          <div class="order-detail__fact">
            <dt>到期时间</dt>
            <dd>{{ formatDateTime(fulfillmentTask.dueAt) }}</dd>
          </div>
          <div class="order-detail__fact">
            <dt>任务创建</dt>
            <dd>{{ formatDateTime(fulfillmentTask.createdAt) }}</dd>
          </div>
        </dl>

        <div v-if="logisticsRecords.length" class="order-detail__timeline">
          <article
            v-for="record in logisticsRecords"
            :key="record.logisticsRecordId"
            class="order-detail__timeline-item"
          >
            <div class="order-detail__timeline-head">
              <p class="order-detail__timeline-title">{{ record.trackingNumber }}</p>
              <StatusPill :label="record.logisticsStatus" :tone="toneFromStatus(record.logisticsStatus)" />
            </div>
            <p class="order-detail__timeline-meta">
              {{ record.logisticsCompany }} · 创建 {{ formatDateTime(record.createdAt) }}
            </p>
            <p class="order-detail__timeline-meta">
              最近同步 {{ formatDateTime(record.syncedAt) }}
            </p>
          </article>
        </div>

        <p v-else class="order-detail__empty">
          当前履约任务还没有物流记录。
        </p>
      </div>
    </div>

    <p v-else class="order-detail__empty">
      先从左侧队列选择一笔订单，详情、编排态和履约记录才会回读到这里。
    </p>
  </PanelCard>
</template>

<style scoped>
.order-detail,
.order-detail__section {
  display: grid;
  gap: 0.85rem;
}

.order-detail__section-head,
.order-detail__timeline-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.order-detail__headline,
.order-detail__subheadline,
.order-detail__section-title,
.order-detail__section-meta,
.order-detail__item-title,
.order-detail__item-meta,
.order-detail__item-caption,
.order-detail__review-copy,
.order-detail__empty,
.order-detail__timeline-title,
.order-detail__timeline-meta {
  margin: 0;
}

.order-detail__headline,
.order-detail__section-title,
.order-detail__item-title,
.order-detail__timeline-title {
  color: var(--color-ink-strong);
}

.order-detail__subheadline,
.order-detail__section-meta,
.order-detail__item-meta,
.order-detail__item-caption,
.order-detail__review-copy,
.order-detail__empty,
.order-detail__timeline-meta,
.order-detail__fact dt {
  color: var(--color-ink-soft);
}

.order-detail__pills,
.order-detail__pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.order-detail__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
  margin: 0;
}

.order-detail__fact {
  display: grid;
  gap: 0.24rem;
}

.order-detail__fact dt {
  font-size: 0.76rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.order-detail__fact dd {
  margin: 0;
  color: var(--color-ink-strong);
  line-height: 1.65;
}

.order-detail__item-list,
.order-detail__timeline {
  display: grid;
  gap: 0.75rem;
}

.order-detail__item-card,
.order-detail__timeline-item {
  display: grid;
  gap: 0.42rem;
  padding: 0.92rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 252, 248, 0.84);
}

.order-detail__empty {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px dashed rgba(91, 72, 54, 0.14);
  background: rgba(255, 252, 248, 0.7);
  line-height: 1.7;
}

@media (max-width: 720px) {
  .order-detail__section-head,
  .order-detail__timeline-head {
    flex-direction: column;
  }

  .order-detail__facts {
    grid-template-columns: 1fr;
  }
}
</style>
