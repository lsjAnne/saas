<script setup lang="ts">
import { reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  CampaignActivity,
  MemberTouchTaskView,
  MemberView,
  SaveMemberTouchTaskPayload
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  member: MemberView | null;
  touchTasks: MemberTouchTaskView[];
  publishedCampaignOptions: CampaignActivity[];
  activeStoreId: string;
  isSubmitting: boolean;
  actionError?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  actionError: null
});

const emit = defineEmits<{
  createTask: [payload: SaveMemberTouchTaskPayload];
}>();

const form = reactive({
  taskType: 'recall',
  triggerType: 'manual',
  campaignId: '',
  channel: 'site_message',
  scheduledAt: '',
  remark: ''
});

function pad(value: number) {
  return String(value).padStart(2, '0');
}

function defaultDateValue() {
  const date = new Date();
  date.setMinutes(0, 0, 0);
  date.setHours(date.getHours() + 2);

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}`;
}

watch(
  () => props.member?.memberId,
  () => {
    form.taskType = 'recall';
    form.triggerType = 'manual';
    form.campaignId = '';
    form.channel = 'site_message';
    form.scheduledAt = defaultDateValue();
    form.remark = '';
  },
  { immediate: true }
);

function toOffsetDateTime(value: string) {
  const date = new Date(value);
  const offsetMinutes = -date.getTimezoneOffset();
  const sign = offsetMinutes >= 0 ? '+' : '-';
  const absoluteMinutes = Math.abs(offsetMinutes);
  const offsetHours = pad(Math.floor(absoluteMinutes / 60));
  const offsetRemainder = pad(absoluteMinutes % 60);

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:00${sign}${offsetHours}:${offsetRemainder}`;
}

function submitTask() {
  if (!props.member) {
    return;
  }

  emit('createTask', {
    storeId: props.member.storeId,
    memberId: props.member.memberId,
    taskType: form.taskType,
    triggerType: form.triggerType,
    campaignId: form.campaignId || undefined,
    channel: form.channel,
    scheduledAt: form.scheduledAt ? toOffsetDateTime(form.scheduledAt) : undefined,
    remark: form.remark.trim()
  });
}

function resolveTone(status: string) {
  const normalized = status.toLowerCase();
  if (normalized === 'planned' || normalized === 'in_progress') {
    return 'success';
  }

  return 'neutral';
}
</script>

<template>
  <PanelCard
    eyebrow="Touch Tasks"
    title="触达任务与召回轨道"
    description="围绕当前会员创建 recall / repurchase / care 任务，并看最近触达队列。"
  >
    <div class="member-touch">
      <form v-if="member" class="member-touch__form" @submit.prevent="submitTask">
        <label class="member-touch__field">
          <span>任务类型</span>
          <select v-model="form.taskType">
            <option value="recall">recall</option>
            <option value="repurchase">repurchase</option>
            <option value="care">care</option>
          </select>
        </label>
        <label class="member-touch__field">
          <span>触发类型</span>
          <select v-model="form.triggerType">
            <option value="manual">manual</option>
            <option value="segment_rule">segment_rule</option>
            <option value="system">system</option>
          </select>
        </label>
        <label class="member-touch__field">
          <span>渠道</span>
          <select v-model="form.channel">
            <option value="site_message">site_message</option>
            <option value="sms">sms</option>
            <option value="wechat">wechat</option>
          </select>
        </label>
        <label class="member-touch__field">
          <span>关联活动</span>
          <select v-model="form.campaignId">
            <option value="">不绑定活动</option>
            <option
              v-for="campaign in publishedCampaignOptions"
              :key="campaign.campaignId"
              :value="campaign.campaignId"
            >
              {{ campaign.activityName }}
            </option>
          </select>
        </label>
        <label class="member-touch__field">
          <span>计划时间</span>
          <input v-model="form.scheduledAt" type="datetime-local" />
        </label>
        <label class="member-touch__field member-touch__field--wide">
          <span>备注</span>
          <textarea v-model="form.remark" rows="3" />
        </label>

        <p v-if="actionError" class="member-touch__error">{{ actionError }}</p>

        <button class="member-touch__primary" type="submit" :disabled="isSubmitting">
          创建触达任务
        </button>
      </form>

      <div class="member-touch__list">
        <article v-for="task in touchTasks" :key="task.taskId" class="member-touch__item">
          <div class="member-touch__item-head">
            <div>
              <h4 class="member-touch__item-title">{{ task.taskType }}</h4>
              <p class="member-touch__item-meta">
                {{ task.channel }} / {{ task.triggerType }}
              </p>
            </div>
            <StatusPill :label="task.status" :tone="resolveTone(task.status)" />
          </div>
          <p class="member-touch__item-body">{{ task.remark || '暂无备注' }}</p>
          <p class="member-touch__item-meta">计划时间 {{ formatDateTime(task.scheduledAt) }}</p>
        </article>

        <div v-if="!touchTasks.length" class="member-touch__empty">
          当前会员还没有触达任务，适合先建一条 recall 或 care 任务。
        </div>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.member-touch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.member-touch__form,
.member-touch__list {
  display: grid;
  gap: 0.85rem;
}

.member-touch__field {
  display: grid;
  gap: 0.45rem;
}

.member-touch__field--wide,
.member-touch__primary,
.member-touch__error {
  grid-column: span 2;
}

.member-touch__field span {
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.member-touch__form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.member-touch__field input,
.member-touch__field select,
.member-touch__field textarea {
  min-height: 2.8rem;
  padding: 0.75rem 0.9rem;
  border-radius: 1rem;
  border: 1px solid rgba(91, 72, 54, 0.12);
  background: rgba(255, 255, 255, 0.72);
  color: var(--color-ink-strong);
  font: inherit;
}

.member-touch__primary {
  min-height: 2.7rem;
  padding: 0 1rem;
  border: 0;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, #30525a, #6b8d8b);
  color: #fff8f0;
}

.member-touch__item {
  display: grid;
  gap: 0.6rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.72);
}

.member-touch__item-head {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
}

.member-touch__item-title,
.member-touch__item-meta,
.member-touch__item-body,
.member-touch__error {
  margin: 0;
}

.member-touch__item-title {
  color: var(--color-ink-strong);
}

.member-touch__item-meta,
.member-touch__item-body,
.member-touch__empty {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.member-touch__error {
  color: var(--color-danger);
}

.member-touch__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.64);
}

@media (max-width: 1080px) {
  .member-touch,
  .member-touch__form {
    grid-template-columns: 1fr;
  }

  .member-touch__field--wide,
  .member-touch__primary,
  .member-touch__error {
    grid-column: span 1;
  }
}
</style>
