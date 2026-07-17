<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  CreateSupplierAdmissionReviewPayload,
  CreateSupplierDeliveryAppointmentPayload,
  CreateSupplierRiskEventPayload,
  CreateSupplierScorecardPayload,
  Supplier,
  SupplierAdmissionReview,
  SupplierDeliveryAppointment,
  SupplierRiskEvent,
  SupplierScorecard,
  SupplierSrmLinkage
} from '@/services/apiTypes';
import { formatDate, formatDateTime } from '@/utils/formatters';

interface Props {
  selectedSupplier: Supplier | null;
  srmLinkage: SupplierSrmLinkage | null;
  latestAdmissionReview: SupplierAdmissionReview | null;
  latestScorecard: SupplierScorecard | null;
  latestDeliveryAppointment: SupplierDeliveryAppointment | null;
  latestRiskEvent: SupplierRiskEvent | null;
  isSubmitting: boolean;
  actionError?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<{
  submitAdmissionReview: [payload: CreateSupplierAdmissionReviewPayload];
  submitScorecard: [payload: CreateSupplierScorecardPayload];
  submitDeliveryAppointment: [payload: CreateSupplierDeliveryAppointmentPayload];
  submitRiskEvent: [payload: CreateSupplierRiskEventPayload];
}>();

const admissionForm = reactive({
  decision: 'approved',
  qualificationDocs: '营业执照\n对公账户\n质检证明',
  remark: ''
});

const scorecardForm = reactive({
  deliveryScore: '88',
  fulfillmentScore: '86',
  qualityScore: '90'
});

const appointmentForm = reactive({
  purchaseReference: '',
  appointmentDate: '',
  plannedQty: '100',
  remark: ''
});

const riskEventForm = reactive({
  riskType: 'delay',
  severity: 'medium',
  remark: ''
});

watch(
  () => props.selectedSupplier?.supplierId,
  () => {
    appointmentForm.purchaseReference = props.selectedSupplier
      ? `PO-${props.selectedSupplier.supplierPlatformId}`
      : '';
    appointmentForm.appointmentDate = '';
    appointmentForm.remark = '';
    riskEventForm.remark = '';
  },
  { immediate: true }
);

const canSubmit = computed(() => Boolean(props.selectedSupplier));

function parseQualificationDocs(raw: string) {
  return raw
    .split(/\r?\n|,/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function handleAdmissionReview() {
  emit('submitAdmissionReview', {
    decision: admissionForm.decision,
    qualificationDocs: parseQualificationDocs(admissionForm.qualificationDocs),
    remark: admissionForm.remark
  });
}

function handleScorecard() {
  emit('submitScorecard', {
    deliveryScore: Number(scorecardForm.deliveryScore),
    fulfillmentScore: Number(scorecardForm.fulfillmentScore),
    qualityScore: Number(scorecardForm.qualityScore)
  });
}

function handleAppointment() {
  if (!props.selectedSupplier) {
    return;
  }

  emit('submitDeliveryAppointment', {
    storeId: props.selectedSupplier.storeId,
    purchaseReference: appointmentForm.purchaseReference,
    appointmentDate: appointmentForm.appointmentDate,
    plannedQty: Number(appointmentForm.plannedQty),
    remark: appointmentForm.remark
  });
}

function handleRiskEvent() {
  emit('submitRiskEvent', {
    riskType: riskEventForm.riskType,
    severity: riskEventForm.severity,
    remark: riskEventForm.remark
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Execution Rail"
    title="准入、评分与风险动作"
    description="先把最常用的准入评审、评分卡、到货预约和风险记录收成同一执行面板。"
  >
    <div class="supplier-execution">
      <div class="supplier-execution__linkage">
        <article class="supplier-execution__linkage-card">
          <p class="supplier-execution__label">SRM 联动摘要</p>
          <h4 class="supplier-execution__value">
            {{ srmLinkage?.ratingGrade || '等待供应商上下文' }}
          </h4>
          <p class="supplier-execution__meta">
            准入状态 {{ srmLinkage?.admissionStatus || '--' }} · ERP {{ srmLinkage?.erpSyncStatus || '--' }} · WMS {{ srmLinkage?.wmsInboundStatus || '--' }}
          </p>
        </article>

        <article class="supplier-execution__linkage-card">
          <p class="supplier-execution__label">会话内最新动作</p>
          <div class="supplier-execution__chips">
            <StatusPill v-if="latestAdmissionReview" :label="latestAdmissionReview.admissionStatus" tone="success" />
            <StatusPill v-if="latestScorecard" :label="latestScorecard.ratingGrade" tone="neutral" />
            <StatusPill v-if="latestRiskEvent" :label="latestRiskEvent.warningLevel" tone="warn" />
          </div>
          <p class="supplier-execution__meta">
            这块只展示当前会话刚执行的结果，跨会话长期结果以 SRM 联动摘要为准。
          </p>
        </article>
      </div>

      <div class="supplier-execution__forms">
        <form class="supplier-execution__card" @submit.prevent="handleAdmissionReview">
          <h4 class="supplier-execution__card-title">准入评审</h4>
          <label class="supplier-execution__field">
            <span>评审结论</span>
            <select v-model="admissionForm.decision" :disabled="!canSubmit || isSubmitting">
              <option value="approved">approved</option>
              <option value="conditional">conditional</option>
              <option value="rejected">rejected</option>
            </select>
          </label>
          <label class="supplier-execution__field">
            <span>资质清单</span>
            <textarea v-model="admissionForm.qualificationDocs" :disabled="!canSubmit || isSubmitting" rows="4" />
          </label>
          <label class="supplier-execution__field">
            <span>备注</span>
            <input v-model="admissionForm.remark" :disabled="!canSubmit || isSubmitting" />
          </label>
          <button class="supplier-execution__submit" type="submit" :disabled="!canSubmit || isSubmitting">提交评审</button>
          <p v-if="latestAdmissionReview" class="supplier-execution__footnote">
            最新评审：{{ latestAdmissionReview.admissionStatus }} · {{ formatDateTime(latestAdmissionReview.reviewedAt) }}
          </p>
        </form>

        <form class="supplier-execution__card" @submit.prevent="handleScorecard">
          <h4 class="supplier-execution__card-title">评分卡</h4>
          <label class="supplier-execution__field">
            <span>交付分</span>
            <input v-model="scorecardForm.deliveryScore" :disabled="!canSubmit || isSubmitting" type="number" min="0" max="100" />
          </label>
          <label class="supplier-execution__field">
            <span>履约分</span>
            <input v-model="scorecardForm.fulfillmentScore" :disabled="!canSubmit || isSubmitting" type="number" min="0" max="100" />
          </label>
          <label class="supplier-execution__field">
            <span>质量分</span>
            <input v-model="scorecardForm.qualityScore" :disabled="!canSubmit || isSubmitting" type="number" min="0" max="100" />
          </label>
          <button class="supplier-execution__submit" type="submit" :disabled="!canSubmit || isSubmitting">保存评分</button>
          <p v-if="latestScorecard" class="supplier-execution__footnote">
            综合 {{ latestScorecard.compositeScore.toFixed(1) }} · {{ latestScorecard.ratingGrade }}
          </p>
        </form>

        <form class="supplier-execution__card" @submit.prevent="handleAppointment">
          <h4 class="supplier-execution__card-title">到货预约</h4>
          <label class="supplier-execution__field">
            <span>采购参考号</span>
            <input v-model="appointmentForm.purchaseReference" :disabled="!canSubmit || isSubmitting" />
          </label>
          <label class="supplier-execution__field">
            <span>预约日期</span>
            <input v-model="appointmentForm.appointmentDate" :disabled="!canSubmit || isSubmitting" type="date" />
          </label>
          <label class="supplier-execution__field">
            <span>计划数量</span>
            <input v-model="appointmentForm.plannedQty" :disabled="!canSubmit || isSubmitting" type="number" min="1" />
          </label>
          <button class="supplier-execution__submit" type="submit" :disabled="!canSubmit || isSubmitting">创建预约</button>
          <p v-if="latestDeliveryAppointment" class="supplier-execution__footnote">
            最新预约：{{ latestDeliveryAppointment.purchaseReference }} · {{ formatDate(latestDeliveryAppointment.appointmentDate) }}
          </p>
        </form>

        <form class="supplier-execution__card" @submit.prevent="handleRiskEvent">
          <h4 class="supplier-execution__card-title">风险事件</h4>
          <label class="supplier-execution__field">
            <span>风险类型</span>
            <select v-model="riskEventForm.riskType" :disabled="!canSubmit || isSubmitting">
              <option value="delay">delay</option>
              <option value="quality">quality</option>
              <option value="capacity">capacity</option>
            </select>
          </label>
          <label class="supplier-execution__field">
            <span>严重等级</span>
            <select v-model="riskEventForm.severity" :disabled="!canSubmit || isSubmitting">
              <option value="low">low</option>
              <option value="medium">medium</option>
              <option value="high">high</option>
            </select>
          </label>
          <label class="supplier-execution__field">
            <span>说明</span>
            <input v-model="riskEventForm.remark" :disabled="!canSubmit || isSubmitting" />
          </label>
          <button class="supplier-execution__submit supplier-execution__submit--warn" type="submit" :disabled="!canSubmit || isSubmitting">记录风险</button>
          <p v-if="latestRiskEvent" class="supplier-execution__footnote">
            最新风险：{{ latestRiskEvent.riskType }} · 兜底 {{ latestRiskEvent.fallbackSupplierId }}
          </p>
        </form>
      </div>

      <p v-if="actionError" class="supplier-execution__error">{{ actionError }}</p>
    </div>
  </PanelCard>
</template>

<style scoped>
.supplier-execution,
.supplier-execution__forms,
.supplier-execution__field {
  display: grid;
  gap: 0.9rem;
}

.supplier-execution__linkage {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.supplier-execution__linkage-card,
.supplier-execution__card {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.supplier-execution__forms {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.supplier-execution__label,
.supplier-execution__value,
.supplier-execution__meta,
.supplier-execution__card-title,
.supplier-execution__footnote,
.supplier-execution__error {
  margin: 0;
}

.supplier-execution__label {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.supplier-execution__value,
.supplier-execution__card-title {
  margin-top: 0.5rem;
  color: var(--color-ink-strong);
}

.supplier-execution__meta,
.supplier-execution__footnote {
  margin-top: 0.65rem;
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.supplier-execution__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  margin-top: 0.7rem;
}

.supplier-execution__field span {
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.supplier-execution__field input,
.supplier-execution__field select,
.supplier-execution__field textarea {
  min-height: 2.75rem;
  padding: 0.75rem 0.9rem;
  border-radius: 1rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.8);
  color: var(--color-ink-strong);
}

.supplier-execution__submit {
  min-height: 2.7rem;
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-accent);
  color: #fff8f0;
}

.supplier-execution__submit--warn {
  background: var(--color-warning);
}

.supplier-execution__error {
  color: var(--color-danger);
}

@media (max-width: 1180px) {
  .supplier-execution__linkage,
  .supplier-execution__forms {
    grid-template-columns: 1fr;
  }
}
</style>
