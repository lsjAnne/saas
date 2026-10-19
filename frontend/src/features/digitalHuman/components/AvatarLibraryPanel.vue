<script setup lang="ts">
import { reactive } from 'vue';

import type { DigitalHumanAvatar } from '@/features/digitalHuman/types';

const props = defineProps<{
  avatars: DigitalHumanAvatar[];
  organizationId: string;
  isLoading: boolean;
  isRunningAction: boolean;
}>();

const emit = defineEmits<{
  create: [payload: {
    organizationId: string;
    avatarName: string;
    avatarType: string;
    voiceName: string;
    previewUrl: string;
  }];
  toggle: [avatar: DigitalHumanAvatar];
  remove: [avatarId: string];
}>();

const form = reactive({
  avatarName: '',
  avatarType: '2D',
  voiceName: '',
});

function handleCreate() {
  if (!form.avatarName.trim()) {
    return;
  }
  emit('create', {
    organizationId: props.organizationId,
    avatarName: form.avatarName.trim(),
    avatarType: form.avatarType,
    voiceName: form.voiceName.trim(),
    previewUrl: '',
  });
  form.avatarName = '';
  form.voiceName = '';
}
</script>

<template>
  <section class="dh-panel dh-library">
    <div class="dh-panel__head">
      <div>
        <h3 class="dh-panel__title">数字人形象库</h3>
        <p class="dh-panel__subline">维护可开播的数字人主播形象与声音资产。</p>
      </div>
    </div>

    <div class="dh-library__form">
      <input
        v-model="form.avatarName"
        class="dh-input"
        placeholder="形象名称，如 带货主播小美"
        maxlength="64"
      />
      <select v-model="form.avatarType" class="dh-select">
        <option value="2D">2D 形象</option>
        <option value="3D">3D 形象</option>
      </select>
      <input
        v-model="form.voiceName"
        class="dh-input"
        placeholder="声音资产，如 甜美女声"
        maxlength="64"
      />
      <button
        class="dh-btn dh-btn--primary"
        :disabled="isRunningAction || !form.avatarName.trim()"
        @click="handleCreate"
      >
        新增形象
      </button>
    </div>

    <div v-if="!avatars.length" class="dh-empty">
      {{ isLoading ? '加载中…' : '还没有数字人形象，先在上面新建一个。' }}
    </div>

    <ul v-else class="dh-list">
      <li v-for="avatar in avatars" :key="avatar.avatarId" class="dh-list__item">
        <div class="dh-list__main">
          <span class="dh-list__name">{{ avatar.avatarName }}</span>
          <span class="dh-tag">{{ avatar.avatarType }} 形象</span>
          <span v-if="avatar.voiceName" class="dh-list__sub">声音：{{ avatar.voiceName }}</span>
        </div>
        <div class="dh-list__actions">
          <span class="dh-status" :class="avatar.enabled ? 'dh-status--on' : 'dh-status--off'">
            {{ avatar.enabled ? '启用' : '停用' }}
          </span>
          <button
            class="dh-btn dh-btn--ghost"
            :disabled="isRunningAction"
            @click="emit('toggle', avatar)"
          >
            {{ avatar.enabled ? '停用' : '启用' }}
          </button>
          <button
            class="dh-btn dh-btn--danger"
            :disabled="isRunningAction"
            @click="emit('remove', avatar.avatarId)"
          >
            删除
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>
