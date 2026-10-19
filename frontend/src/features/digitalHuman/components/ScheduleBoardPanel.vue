<script setup lang="ts">
import { reactive } from 'vue';

import type {
  DigitalHumanAvatar,
  DigitalHumanSchedule,
  DigitalHumanScript,
} from '@/features/digitalHuman/types';

const props = defineProps<{
  schedules: DigitalHumanSchedule[];
  avatars: DigitalHumanAvatar[];
  scripts: DigitalHumanScript[];
  organizationId: string;
  isLoading: boolean;
  isRunningAction: boolean;
}>();

const emit = defineEmits<{
  create: [payload: {
    organizationId: string;
    scheduleName: string;
    avatarId: string;
    scriptId: string;
    platformType: string;
    repeatType: string;
    startAt: string;
  }];
  start: [scheduleId: string];
  stop: [scheduleId: string];
}>();

const form = reactive({
  scheduleName: '',
  avatarId: '',
  scriptId: '',
  platformType: 'douyin',
  repeatType: 'daily',
  startAt: '',
});

function handleCreate() {
  if (!form.scheduleName.trim() || !form.avatarId || !form.scriptId || !form.startAt) {
    return;
  }
  emit('create', {
    organizationId: props.organizationId,
    scheduleName: form.scheduleName.trim(),
    avatarId: form.avatarId,
    scriptId: form.scriptId,
    platformType: form.platformType,
    repeatType: form.repeatType,
    startAt: form.startAt,
  });
  form.scheduleName = '';
  form.startAt = '';
}

const platformLabel: Record<string, string> = {
  douyin: '抖音',
  kuaishou: '快手',
  wechat_channels: '视频号',
  pdd: '拼多多',
  taobao: '淘宝',
  jd: '京东',
};

const repeatLabel: Record<string, string> = {
  daily: '每日',
  weekly: '每周',
  once: '单次',
};
</script>

<template>
  <section class="dh-panel dh-library">
    <div class="dh-panel__head">
      <div>
        <h3 class="dh-panel__title">无人直播排班</h3>
        <p class="dh-panel__subline">选择形象与话术，设定平台与开播时间，一键开播或停播。</p>
      </div>
    </div>

    <div class="dh-library__form dh-library__form--stack">
      <div class="dh-library__row">
        <input
          v-model="form.scheduleName"
          class="dh-input"
          placeholder="排班名称，如 每日无人值守夜场"
          maxlength="64"
        />
        <select v-model="form.platformType" class="dh-select">
          <option value="douyin">抖音</option>
          <option value="kuaishou">快手</option>
          <option value="wechat_channels">视频号</option>
          <option value="pdd">拼多多</option>
          <option value="taobao">淘宝</option>
          <option value="jd">京东</option>
        </select>
        <select v-model="form.repeatType" class="dh-select">
          <option value="daily">每日</option>
          <option value="weekly">每周</option>
          <option value="once">单次</option>
        </select>
      </div>
      <div class="dh-library__row">
        <select v-model="form.avatarId" class="dh-select">
          <option value="" disabled>选择数字人形象</option>
          <option v-for="avatar in avatars" :key="avatar.avatarId" :value="avatar.avatarId">
            {{ avatar.avatarName }}
          </option>
        </select>
        <select v-model="form.scriptId" class="dh-select">
          <option value="" disabled>选择话术剧本</option>
          <option v-for="script in scripts" :key="script.scriptId" :value="script.scriptId">
            {{ script.scriptName }}
          </option>
        </select>
        <input
          v-model="form.startAt"
          class="dh-input"
          type="datetime-local"
        />
      </div>
      <button
        class="dh-btn dh-btn--primary"
        :disabled="isRunningAction || !form.scheduleName.trim() || !form.avatarId || !form.scriptId || !form.startAt"
        @click="handleCreate"
      >
        新增排班
      </button>
    </div>

    <div v-if="!schedules.length" class="dh-empty">
      {{ isLoading ? '加载中…' : '还没有无人排班，先在上方新建一个计划。' }}
    </div>

    <ul v-else class="dh-list">
      <li v-for="schedule in schedules" :key="schedule.scheduleId" class="dh-list__item">
        <div class="dh-list__main">
          <span class="dh-list__name">{{ schedule.scheduleName }}</span>
          <span class="dh-tag">{{ platformLabel[schedule.platformType] ?? schedule.platformType }}</span>
          <span class="dh-tag">{{ repeatLabel[schedule.repeatType] ?? schedule.repeatType }}</span>
          <span class="dh-list__sub">
            {{ new Date(schedule.startAt).toLocaleString('zh-CN', { hour12: false }) }}
          </span>
          <span class="dh-status" :class="schedule.status === 'running' ? 'dh-status--live' : schedule.status === 'stopped' ? 'dh-status--off' : 'dh-status--on'">
            {{ schedule.status === 'running' ? '直播中' : schedule.status === 'stopped' ? '已停播' : '待开播' }}
          </span>
        </div>
        <div class="dh-list__actions">
          <button
            v-if="schedule.status !== 'running'"
            class="dh-btn dh-btn--primary"
            :disabled="isRunningAction"
            @click="emit('start', schedule.scheduleId)"
          >
            开播
          </button>
          <button
            v-else
            class="dh-btn dh-btn--danger"
            :disabled="isRunningAction"
            @click="emit('stop', schedule.scheduleId)"
          >
            停播
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>
