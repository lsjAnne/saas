<script setup lang="ts">
import { reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  CreateReplenishmentTaskPayload,
  InventorySnapshot,
  Store,
  UpdateInventorySafetyStockPayload
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  stores: Store[];
  snapshots: InventorySnapshot[];
  selectedSnapshotId: string | null;
  selectedSnapshot: InventorySnapshot | null;
  activeStoreId: string;
  isSubmitting: boolean;
  actionError?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<{
  selectStore: [storeId: string];
  selectSnapshot: [snapshotId: string];
  updateSafetyStock: [payload: UpdateInventorySafetyStockPayload];
  createTask: [payload: CreateReplenishmentTaskPayload];
}>();

const safetyStockForm = reactive({
  safetyStock: '0'
});

const taskForm = reactive({
  suggestedQty: '0',
  reasonText: ''
});

watch(
  () => props.selectedSnapshot?.inventorySnapshotId,
  () => {
    safetyStockForm.safetyStock = String(props.selectedSnapshot?.safetyStock ?? 0);
    taskForm.suggestedQty = String(
      Math.max(
        (props.selectedSnapshot?.safetyStock ?? 0) -
          (props.selectedSnapshot?.availableStock ?? 0),
        0
      )
    );
    taskForm.reasonText = '';
  },
  { immediate: true }
);

function resolveTone(snapshot: InventorySnapshot): 'neutral' | 'warn' | 'success' {
  if (snapshot.availableStock <= snapshot.safetyStock) {
    return 'warn';
  }

  if (snapshot.reservedStock > 0) {
    return 'success';
  }

  return 'neutral';
}

function submitSafetyStock() {
  emit('updateSafetyStock', {
    safetyStock: Number(safetyStockForm.safetyStock)
  });
}

function submitTask() {
  if (!props.selectedSnapshot) {
    return;
  }

  emit('createTask', {
    storeId: props.selectedSnapshot.storeId,
    productId: props.selectedSnapshot.productId,
    skuId: props.selectedSnapshot.skuId,
    suggestedQty: Number(taskForm.suggestedQty),
    reasonText: taskForm.reasonText.trim() || undefined
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Inventory Snapshot"
    title="库存快照与补货起点"
    description="先看低库存与冻结状态，再从当前快照直接调安全库存或发起补货任务。"
  >
    <div class="inventory-snapshot">
      <div class="inventory-snapshot__filters">
        <button
          type="button"
          :class="['inventory-snapshot__filter', { 'inventory-snapshot__filter--active': activeStoreId === 'all' }]"
          @click="emit('selectStore', 'all')"
        >
          全部店铺
        </button>
        <button
          v-for="store in stores"
          :key="store.storeId"
          type="button"
          :class="[
            'inventory-snapshot__filter',
            { 'inventory-snapshot__filter--active': activeStoreId === store.storeId }
          ]"
          @click="emit('selectStore', store.storeId)"
        >
          {{ store.shopName }}
        </button>
      </div>

      <div v-if="snapshots.length" class="inventory-snapshot__list">
        <button
          v-for="snapshot in snapshots"
          :key="snapshot.inventorySnapshotId"
          type="button"
          :class="[
            'inventory-snapshot__item',
            { 'inventory-snapshot__item--active': selectedSnapshotId === snapshot.inventorySnapshotId }
          ]"
          @click="emit('selectSnapshot', snapshot.inventorySnapshotId)"
        >
          <div class="inventory-snapshot__item-top">
            <div>
              <p class="inventory-snapshot__item-title">{{ snapshot.skuId }}</p>
              <p class="inventory-snapshot__item-meta">
                {{ snapshot.productId }} / {{ snapshot.storeId }}
              </p>
            </div>
            <StatusPill
              :label="snapshot.availableStock <= snapshot.safetyStock ? 'low stock' : 'healthy'"
              :tone="resolveTone(snapshot)"
            />
          </div>
          <div class="inventory-snapshot__item-metrics">
            <span>可用 {{ snapshot.availableStock }}</span>
            <span>预留 {{ snapshot.reservedStock }}</span>
            <span>安全 {{ snapshot.safetyStock }}</span>
          </div>
          <p class="inventory-snapshot__item-time">{{ formatDateTime(snapshot.snapshotAt) }}</p>
        </button>
      </div>

      <div v-if="selectedSnapshot" class="inventory-snapshot__forms">
        <form class="inventory-snapshot__form" @submit.prevent="submitSafetyStock">
          <label class="inventory-snapshot__field">
            <span>安全库存</span>
            <input v-model="safetyStockForm.safetyStock" type="number" min="0" :disabled="isSubmitting" />
          </label>
          <button class="inventory-snapshot__ghost" type="submit" :disabled="isSubmitting">
            调整安全库存
          </button>
        </form>

        <form class="inventory-snapshot__form inventory-snapshot__form--task" @submit.prevent="submitTask">
          <label class="inventory-snapshot__field">
            <span>建议补货量</span>
            <input v-model="taskForm.suggestedQty" type="number" min="1" :disabled="isSubmitting" />
          </label>
          <label class="inventory-snapshot__field inventory-snapshot__field--wide">
            <span>补货原因</span>
            <textarea
              v-model="taskForm.reasonText"
              rows="3"
              :disabled="isSubmitting"
              placeholder="说明为什么要从当前快照发起补货"
            />
          </label>
          <button class="inventory-snapshot__primary" type="submit" :disabled="isSubmitting">
            发起补货任务
          </button>
        </form>
      </div>

      <p v-if="actionError" class="inventory-snapshot__error">{{ actionError }}</p>
    </div>
  </PanelCard>
</template>

<style scoped>
.inventory-snapshot,
.inventory-snapshot__list,
.inventory-snapshot__forms {
  display: grid;
  gap: 0.9rem;
}

.inventory-snapshot__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.inventory-snapshot__filter,
.inventory-snapshot__item,
.inventory-snapshot__ghost {
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.72);
}

.inventory-snapshot__filter {
  min-height: 2.5rem;
  padding: 0 0.9rem;
  border-radius: var(--radius-pill);
  color: var(--color-ink);
  font: inherit;
}

.inventory-snapshot__filter--active {
  background: linear-gradient(135deg, rgba(106, 67, 39, 0.94), rgba(43, 31, 23, 0.94));
  border-color: transparent;
  color: #fff8f0;
}

.inventory-snapshot__list {
  max-height: 30rem;
  overflow: auto;
}

.inventory-snapshot__item {
  padding: 1rem;
  border-radius: var(--radius-lg);
  text-align: left;
  display: grid;
  gap: 0.7rem;
}

.inventory-snapshot__item--active {
  border-color: rgba(106, 67, 39, 0.38);
  box-shadow: 0 18px 36px rgba(81, 52, 44, 0.12);
}

.inventory-snapshot__item-top {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: start;
}

.inventory-snapshot__item-title,
.inventory-snapshot__item-meta,
.inventory-snapshot__item-time,
.inventory-snapshot__error {
  margin: 0;
}

.inventory-snapshot__item-title {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.inventory-snapshot__item-meta,
.inventory-snapshot__item-time {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.inventory-snapshot__item-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.inventory-snapshot__form {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0.85rem;
  align-items: end;
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.68);
}

.inventory-snapshot__form--task {
  grid-template-columns: 1fr 1fr;
}

.inventory-snapshot__field {
  display: grid;
  gap: 0.45rem;
}

.inventory-snapshot__field--wide,
.inventory-snapshot__primary {
  grid-column: span 2;
}

.inventory-snapshot__field span {
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.inventory-snapshot__field input,
.inventory-snapshot__field textarea {
  min-height: 2.8rem;
  padding: 0.75rem 0.9rem;
  border-radius: 1rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-ink-strong);
  font: inherit;
}

.inventory-snapshot__ghost,
.inventory-snapshot__primary {
  min-height: 2.7rem;
  padding: 0 1rem;
  border-radius: var(--radius-pill);
}

.inventory-snapshot__primary {
  border: 0;
  background: linear-gradient(135deg, #30525a, #6b8d8b);
  color: #fff8f0;
}

.inventory-snapshot__error {
  color: var(--color-danger);
}

@media (max-width: 960px) {
  .inventory-snapshot__form,
  .inventory-snapshot__form--task {
    grid-template-columns: 1fr;
  }

  .inventory-snapshot__field--wide,
  .inventory-snapshot__primary {
    grid-column: span 1;
  }
}
</style>
