<script setup lang="ts">
import { reactive } from 'vue';

import type { DigitalHumanScript } from '@/features/digitalHuman/types';

const props = defineProps<{
  scripts: DigitalHumanScript[];
  organizationId: string;
  isLoading: boolean;
  isRunningAction: boolean;
}>();

const emit = defineEmits<{
  create: [payload: {
    organizationId: string;
    scriptName: string;
    sceneType: string;
    contentText: string;
  }];
  publish: [script: DigitalHumanScript];
}>();

const form = reactive({
  scriptName: '',
  sceneType: 'product-intro',
  contentText: '',
});

function handleCreate() {
  if (!form.scriptName.trim() || !form.contentText.trim()) {
    return;
  }
  emit('create', {
    organizationId: props.organizationId,
    scriptName: form.scriptName.trim(),
    sceneType: form.sceneType,
    contentText: form.contentText.trim(),
  });
  form.scriptName = '';
  form.contentText = '';
}
</script>

<template>
  <section class="dh-panel dh-library">
    <div class="dh-panel__head">
      <div>
        <h3 class="dh-panel__title">话术与剧本库</h3>
        <p class="dh-panel__subline">数字人自动讲解的话术内容，发布后供无人排班调用。</p>
      </div>
    </div>

    <div class="dh-library__form dh-library__form--stack">
      <div class="dh-library__row">
        <input
          v-model="form.scriptName"
          class="dh-input"
          placeholder="话术名称，如 夜间清仓话术"
          maxlength="64"
        />
        <select v-model="form.sceneType" class="dh-select">
          <option value="product-intro">商品讲解</option>
          <option value="live-guide">直播引导</option>
          <option value="interaction">互动应答</option>
        </select>
      </div>
      <textarea
        v-model="form.contentText"
        class="dh-textarea"
        placeholder="话术正文，例如：欢迎来到直播间，今天这款商品限时清仓……"
        rows="3"
      ></textarea>
      <button
        class="dh-btn dh-btn--primary"
        :disabled="isRunningAction || !form.scriptName.trim() || !form.contentText.trim()"
        @click="handleCreate"
      >
        新增话术
      </button>
    </div>

    <div v-if="!scripts.length" class="dh-empty">
      {{ isLoading ? '加载中…' : '还没有话术剧本，先在上面新建一条。' }}
    </div>

    <ul v-else class="dh-list">
      <li v-for="script in scripts" :key="script.scriptId" class="dh-list__item dh-list__item--stack">
        <div class="dh-list__main">
          <span class="dh-list__name">{{ script.scriptName }}</span>
          <span class="dh-tag">{{ script.sceneType === 'product-intro' ? '商品讲解' : script.sceneType === 'live-guide' ? '直播引导' : '互动应答' }}</span>
          <span class="dh-status" :class="script.status === 'published' ? 'dh-status--on' : 'dh-status--off'">
            {{ script.status === 'published' ? '已发布' : '草稿' }}
          </span>
        </div>
        <p class="dh-list__desc">{{ script.contentText }}</p>
        <div class="dh-list__actions">
          <span class="dh-list__sub">版本 {{ script.version }}</span>
          <button
            v-if="script.status !== 'published'"
            class="dh-btn dh-btn--ghost"
            :disabled="isRunningAction"
            @click="emit('publish', script)"
          >
            发布
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>
