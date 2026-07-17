<script setup lang="ts">
import { reactive, watch } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import type {
  CreateLogisticsRecordPayload,
  CreateOrderMergePayload,
  CreateOrderReverseStatusPayload,
  CreateOrderRoutePlanPayload,
  CreateOrderSplitPayload,
  CreateOrderSyncPayload,
  FulfillmentTask,
  OrderDetailView,
  OrderMain,
  Store
} from '@/services/apiTypes';

interface Props {
  stores: Store[];
  selectedOrder: OrderMain | null;
  selectedOrderDetail: OrderDetailView | null;
  selectedFulfillmentTask: FulfillmentTask | null;
  isSubmitting: boolean;
  errorMessage: string | null;
  canManageFulfillment: boolean;
}

interface Emits {
  syncOrder: [payload: CreateOrderSyncPayload];
  auditOrder: [];
  splitOrder: [payload: CreateOrderSplitPayload];
  mergeOrders: [payload: CreateOrderMergePayload];
  planRoute: [payload: CreateOrderRoutePlanPayload];
  updateReverseStatus: [payload: CreateOrderReverseStatusPayload];
  confirmTask: [];
  retryTask: [];
  replayTask: [];
  createLogisticsRecord: [payload: CreateLogisticsRecordPayload];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const syncForm = reactive<CreateOrderSyncPayload>({
  storeId: '',
  platformOrderId: '',
  productId: '',
  skuId: '',
  quantity: 1,
  unitPrice: 99,
  buyerName: '',
  buyerPhoneMask: '',
  shippingAddress: ''
});

const splitForm = reactive<CreateOrderSplitPayload>({
  remark: '',
  children: [
    {
      childLabel: 'main-package',
      quantity: 1,
      orderType: 'normal'
    },
    {
      childLabel: 'gift-package',
      quantity: 1,
      orderType: 'gift'
    }
  ]
});

const mergeForm = reactive<CreateOrderMergePayload>({
  storeId: '',
  orderIds: [],
  mergedOrderType: 'combined',
  remark: ''
});

const mergeOrderIdsInput = reactive({
  value: ''
});

const routePlanForm = reactive<CreateOrderRoutePlanPayload>({
  warehouseCandidates: ['WH-SH-A', 'WH-SH-B'],
  shippingStrategy: 'priority_delivery'
});

const routeWarehouseInput = reactive({
  value: 'WH-SH-A,WH-SH-B'
});

const reverseForm = reactive<CreateOrderReverseStatusPayload>({
  reverseStatus: 'refunding',
  remark: ''
});

const logisticsForm = reactive<CreateLogisticsRecordPayload>({
  trackingNumber: '',
  logisticsCompany: '',
  logisticsStatus: 'in_transit'
});

watch(
  () => [props.selectedOrder?.storeId, props.stores[0]?.storeId],
  ([selectedStoreId, firstStoreId]) => {
    const nextStoreId = selectedStoreId || firstStoreId || '';

    if (!syncForm.storeId) {
      syncForm.storeId = nextStoreId;
    }

    mergeForm.storeId = nextStoreId;
  },
  { immediate: true }
);

watch(
  () => props.selectedOrderDetail?.items[0],
  (firstItem) => {
    if (!firstItem) {
      return;
    }

    if (!syncForm.productId) {
      syncForm.productId = firstItem.productId;
    }

    if (!syncForm.skuId) {
      syncForm.skuId = firstItem.skuId;
    }
  },
  { immediate: true }
);

watch(
  () => props.selectedOrder?.orderId,
  (orderId) => {
    if (!orderId) {
      return;
    }

    mergeOrderIdsInput.value = orderId;
    mergeForm.orderIds = [orderId];
  },
  { immediate: true }
);

function addSplitChild() {
  splitForm.children.push({
    childLabel: `child-${splitForm.children.length + 1}`,
    quantity: 1,
    orderType: 'normal'
  });
}

function removeSplitChild(index: number) {
  if (splitForm.children.length <= 1) {
    return;
  }

  splitForm.children.splice(index, 1);
}

function submitSyncOrder() {
  emit('syncOrder', {
    ...syncForm,
    quantity: Number(syncForm.quantity),
    unitPrice: Number(syncForm.unitPrice)
  });
}

function submitSplitOrder() {
  emit('splitOrder', {
    remark: splitForm.remark,
    children: splitForm.children.map((item) => ({
      childLabel: item.childLabel,
      quantity: Number(item.quantity),
      orderType: item.orderType
    }))
  });
}

function submitMergeOrders() {
  mergeForm.orderIds = mergeOrderIdsInput.value
    .split(/[,\n]/)
    .map((item) => item.trim())
    .filter(Boolean);

  emit('mergeOrders', {
    storeId: mergeForm.storeId,
    orderIds: mergeForm.orderIds,
    mergedOrderType: mergeForm.mergedOrderType,
    remark: mergeForm.remark
  });
}

function submitRoutePlan() {
  routePlanForm.warehouseCandidates = routeWarehouseInput.value
    .split(/[,\n]/)
    .map((item) => item.trim())
    .filter(Boolean);

  emit('planRoute', {
    warehouseCandidates: routePlanForm.warehouseCandidates,
    shippingStrategy: routePlanForm.shippingStrategy
  });
}

function submitReverseStatus() {
  emit('updateReverseStatus', {
    reverseStatus: reverseForm.reverseStatus,
    remark: reverseForm.remark
  });
}

function submitLogisticsRecord() {
  emit('createLogisticsRecord', {
    trackingNumber: logisticsForm.trackingNumber,
    logisticsCompany: logisticsForm.logisticsCompany,
    logisticsStatus: logisticsForm.logisticsStatus
  });
}
</script>

<template>
  <div class="order-actions">
    <InlineErrorCard
      v-if="errorMessage"
      title="动作没有完成"
      :message="errorMessage"
      retry-label="我知道了"
      @retry="() => undefined"
    />

    <PanelCard
      eyebrow="Seed Sync"
      title="同步真实订单"
      description="这里直接生成真实订单与履约任务。第一次同步新平台单时，请明确填写 storeId、productId 和 skuId。"
    >
      <form class="order-actions__form" @submit.prevent="submitSyncOrder">
        <div class="order-actions__grid order-actions__grid--two">
          <label class="order-actions__field">
            <span>店铺</span>
            <select v-model="syncForm.storeId">
              <option v-for="store in stores" :key="store.storeId" :value="store.storeId">
                {{ store.shopName }} / {{ store.storeId }}
              </option>
            </select>
          </label>

          <label class="order-actions__field">
            <span>平台订单号</span>
            <input v-model="syncForm.platformOrderId" type="text" placeholder="dy-order-9301" />
          </label>
        </div>

        <div class="order-actions__grid order-actions__grid--three">
          <label class="order-actions__field">
            <span>商品 ID</span>
            <input v-model="syncForm.productId" type="text" placeholder="product-1" />
          </label>
          <label class="order-actions__field">
            <span>SKU ID</span>
            <input v-model="syncForm.skuId" type="text" placeholder="sku-9301" />
          </label>
          <label class="order-actions__field">
            <span>数量</span>
            <input v-model.number="syncForm.quantity" type="number" min="1" />
          </label>
        </div>

        <div class="order-actions__grid order-actions__grid--three">
          <label class="order-actions__field">
            <span>单价</span>
            <input v-model.number="syncForm.unitPrice" type="number" min="0" step="0.01" />
          </label>
          <label class="order-actions__field">
            <span>买家姓名</span>
            <input v-model="syncForm.buyerName" type="text" placeholder="王五" />
          </label>
          <label class="order-actions__field">
            <span>手机号脱敏</span>
            <input v-model="syncForm.buyerPhoneMask" type="text" placeholder="139****3001" />
          </label>
        </div>

        <label class="order-actions__field">
          <span>收货地址</span>
          <textarea
            v-model="syncForm.shippingAddress"
            rows="2"
            placeholder="上海市浦东新区张江路 188 号"
          />
        </label>

        <button class="order-actions__submit order-actions__submit--primary" :disabled="isSubmitting">
          同步订单
        </button>
      </form>
    </PanelCard>

    <PanelCard
      eyebrow="Order Actions"
      title="订单动作编排"
      description="审单、拆单、合单、路由和逆向动作集中在这里，所有动作完成后都会回读订单详情与 OMS 摘要。"
    >
      <div v-if="selectedOrder" class="order-actions__stack">
        <div class="order-actions__action-head">
          <div>
            <p class="order-actions__selected-title">{{ selectedOrder.platformOrderId }}</p>
            <p class="order-actions__selected-meta">
              当前选中订单 {{ selectedOrder.orderId }} · 店铺 {{ selectedOrder.storeId }}
            </p>
          </div>
          <button class="order-actions__ghost" :disabled="isSubmitting" @click="emit('auditOrder')">
            立即审单
          </button>
        </div>

        <form class="order-actions__form" @submit.prevent="submitSplitOrder">
          <div class="order-actions__form-head">
            <p class="order-actions__block-title">拆单</p>
            <button type="button" class="order-actions__ghost" @click="addSplitChild">
              新增子单
            </button>
          </div>

          <label class="order-actions__field">
            <span>备注</span>
            <input v-model="splitForm.remark" type="text" placeholder="拆成主商品与赠品单" />
          </label>

          <div
            v-for="(child, index) in splitForm.children"
            :key="`${child.childLabel}-${index}`"
            class="order-actions__grid order-actions__grid--split"
          >
            <label class="order-actions__field">
              <span>子单标签</span>
              <input v-model="child.childLabel" type="text" />
            </label>
            <label class="order-actions__field">
              <span>数量</span>
              <input v-model.number="child.quantity" type="number" min="1" />
            </label>
            <label class="order-actions__field">
              <span>类型</span>
              <input v-model="child.orderType" type="text" />
            </label>
            <button type="button" class="order-actions__ghost order-actions__ghost--danger" @click="removeSplitChild(index)">
              删除
            </button>
          </div>

          <button class="order-actions__submit" :disabled="isSubmitting">
            提交拆单
          </button>
        </form>

        <form class="order-actions__form" @submit.prevent="submitMergeOrders">
          <div class="order-actions__form-head">
            <p class="order-actions__block-title">合单</p>
            <p class="order-actions__block-copy">按逗号输入需要合并的订单 ID</p>
          </div>

          <div class="order-actions__grid order-actions__grid--two">
            <label class="order-actions__field">
              <span>店铺 ID</span>
              <input v-model="mergeForm.storeId" type="text" />
            </label>
            <label class="order-actions__field">
              <span>合单类型</span>
              <input v-model="mergeForm.mergedOrderType" type="text" />
            </label>
          </div>

          <label class="order-actions__field">
            <span>订单 IDs</span>
            <textarea v-model="mergeOrderIdsInput.value" rows="2" />
          </label>

          <label class="order-actions__field">
            <span>备注</span>
            <input v-model="mergeForm.remark" type="text" placeholder="同客同址合单" />
          </label>

          <button class="order-actions__submit" :disabled="isSubmitting">
            提交合单
          </button>
        </form>

        <form class="order-actions__form" @submit.prevent="submitRoutePlan">
          <div class="order-actions__form-head">
            <p class="order-actions__block-title">路由规划</p>
            <p class="order-actions__block-copy">用仓候选 + 发货策略直接回读路由结果</p>
          </div>

          <label class="order-actions__field">
            <span>候选仓</span>
            <textarea v-model="routeWarehouseInput.value" rows="2" />
          </label>

          <label class="order-actions__field">
            <span>发货策略</span>
            <input v-model="routePlanForm.shippingStrategy" type="text" />
          </label>

          <button class="order-actions__submit" :disabled="isSubmitting">
            规划路由
          </button>
        </form>

        <form class="order-actions__form" @submit.prevent="submitReverseStatus">
          <div class="order-actions__form-head">
            <p class="order-actions__block-title">逆向状态</p>
            <p class="order-actions__block-copy">退款、退货、售后统一走后端逆向状态切换</p>
          </div>

          <div class="order-actions__grid order-actions__grid--two">
            <label class="order-actions__field">
              <span>逆向状态</span>
              <input v-model="reverseForm.reverseStatus" type="text" />
            </label>
            <label class="order-actions__field">
              <span>备注</span>
              <input v-model="reverseForm.remark" type="text" placeholder="高风险订单发起退款" />
            </label>
          </div>

          <button class="order-actions__submit" :disabled="isSubmitting">
            更新逆向状态
          </button>
        </form>
      </div>

      <p v-else class="order-actions__empty">
        先选择一笔订单，再执行审单、拆合单、路由或逆向动作。
      </p>
    </PanelCard>

    <PanelCard
      v-if="canManageFulfillment"
      eyebrow="Fulfillment Actions"
      title="履约推进与物流登记"
      description="这里处理确认、重试、异常回放和物流录入，只对有履约权限的账号开放。"
    >
      <div v-if="selectedFulfillmentTask" class="order-actions__stack">
        <div class="order-actions__action-head">
          <div>
            <p class="order-actions__selected-title">{{ selectedFulfillmentTask.fulfillmentTaskId }}</p>
            <p class="order-actions__selected-meta">
              当前任务 {{ selectedFulfillmentTask.status }} · 重试 {{ selectedFulfillmentTask.retryCount ?? 0 }} 次
            </p>
          </div>
          <div class="order-actions__button-row">
            <button class="order-actions__ghost" :disabled="isSubmitting" @click="emit('confirmTask')">
              确认任务
            </button>
            <button class="order-actions__ghost" :disabled="isSubmitting" @click="emit('retryTask')">
              重试任务
            </button>
            <button class="order-actions__ghost order-actions__ghost--danger" :disabled="isSubmitting" @click="emit('replayTask')">
              异常回放
            </button>
          </div>
        </div>

        <form class="order-actions__form" @submit.prevent="submitLogisticsRecord">
          <div class="order-actions__form-head">
            <p class="order-actions__block-title">登记物流记录</p>
            <p class="order-actions__block-copy">物流单号、承运商与状态回写到履约任务下</p>
          </div>

          <div class="order-actions__grid order-actions__grid--three">
            <label class="order-actions__field">
              <span>物流单号</span>
              <input v-model="logisticsForm.trackingNumber" type="text" />
            </label>
            <label class="order-actions__field">
              <span>物流公司</span>
              <input v-model="logisticsForm.logisticsCompany" type="text" />
            </label>
            <label class="order-actions__field">
              <span>物流状态</span>
              <input v-model="logisticsForm.logisticsStatus" type="text" />
            </label>
          </div>

          <button class="order-actions__submit order-actions__submit--primary" :disabled="isSubmitting">
            登记物流
          </button>
        </form>
      </div>

      <p v-else class="order-actions__empty">
        先选择一条履约任务，再做确认、重试、回放或物流登记。
      </p>
    </PanelCard>
  </div>
</template>

<style scoped>
.order-actions {
  display: grid;
  gap: 1rem;
}

.order-actions__stack,
.order-actions__form {
  display: grid;
  gap: 0.85rem;
}

.order-actions__form {
  padding: 1rem;
  border-radius: var(--radius-xl);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background:
    radial-gradient(circle at top right, rgba(198, 172, 140, 0.12), transparent 36%),
    rgba(255, 252, 248, 0.92);
}

.order-actions__form-head,
.order-actions__action-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.order-actions__grid {
  display: grid;
  gap: 0.75rem;
}

.order-actions__grid--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.order-actions__grid--three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.order-actions__grid--split {
  grid-template-columns: 1.2fr 0.7fr 0.9fr auto;
  align-items: end;
}

.order-actions__field {
  display: grid;
  gap: 0.4rem;
}

.order-actions__field span,
.order-actions__block-copy,
.order-actions__selected-meta,
.order-actions__empty {
  color: var(--color-ink-soft);
}

.order-actions__selected-title,
.order-actions__block-title {
  margin: 0;
  color: var(--color-ink-strong);
  font-weight: 700;
}

.order-actions__selected-meta,
.order-actions__block-copy,
.order-actions__empty {
  margin: 0;
  line-height: 1.6;
}

.order-actions__field input,
.order-actions__field select,
.order-actions__field textarea {
  width: 100%;
  min-height: 2.9rem;
  padding: 0.75rem 0.9rem;
  border-radius: 1rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.88);
  color: var(--color-ink-strong);
  font: inherit;
  resize: vertical;
  box-sizing: border-box;
}

.order-actions__field textarea {
  min-height: 5.4rem;
}

.order-actions__submit,
.order-actions__ghost {
  min-height: 2.9rem;
  padding: 0 1.1rem;
  border-radius: var(--radius-pill);
  font: inherit;
  font-weight: 600;
}

.order-actions__submit {
  width: fit-content;
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.9);
  color: var(--color-ink-strong);
}

.order-actions__submit--primary {
  border: 0;
  background: linear-gradient(135deg, rgba(131, 79, 36, 0.98), rgba(186, 125, 74, 0.92));
  color: #fffaf4;
}

.order-actions__ghost {
  border: 1px solid rgba(91, 72, 54, 0.14);
  background: rgba(255, 255, 255, 0.86);
  color: var(--color-ink-strong);
}

.order-actions__ghost--danger {
  color: var(--color-warning);
}

.order-actions__button-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.order-actions__empty {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px dashed rgba(91, 72, 54, 0.14);
  background: rgba(255, 252, 248, 0.7);
}

@media (max-width: 1080px) {
  .order-actions__grid--three,
  .order-actions__grid--split {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .order-actions__grid--two {
    grid-template-columns: 1fr;
  }

  .order-actions__form-head,
  .order-actions__action-head {
    flex-direction: column;
  }
}
</style>
