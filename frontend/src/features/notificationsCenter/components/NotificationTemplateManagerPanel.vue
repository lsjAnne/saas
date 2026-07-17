<script setup lang="ts">
import { computed, reactive, shallowRef, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  NotificationTemplateView,
  UpdateNotificationTemplatePayload
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  templates: NotificationTemplateView[];
  savingTemplateId: string | null;
}

interface Emits {
  saveTemplate: [
    payload: {
      notificationTemplateId: string;
      template: UpdateNotificationTemplatePayload;
    }
  ];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const selectedTemplateId = shallowRef<string | null>(null);
const draft = reactive<UpdateNotificationTemplatePayload>({
  templateName: '',
  titleTemplate: '',
  contentTemplate: '',
  enabled: true
});

const selectedTemplate = computed(
  () =>
    props.templates.find(
      (template) => template.notificationTemplateId === selectedTemplateId.value
    ) ?? props.templates[0] ?? null
);

watch(
  () => props.templates,
  (templates) => {
    if (!templates.length) {
      selectedTemplateId.value = null;
      return;
    }

    if (!selectedTemplateId.value) {
      selectedTemplateId.value = templates[0].notificationTemplateId;
      return;
    }

    if (!templates.some((template) => template.notificationTemplateId === selectedTemplateId.value)) {
      selectedTemplateId.value = templates[0].notificationTemplateId;
    }
  },
  { immediate: true, deep: true }
);

watch(
  selectedTemplate,
  (template) => {
    if (!template) {
      draft.templateName = '';
      draft.titleTemplate = '';
      draft.contentTemplate = '';
      draft.enabled = true;
      return;
    }

    draft.templateName = template.templateName;
    draft.titleTemplate = template.titleTemplate;
    draft.contentTemplate = template.contentTemplate;
    draft.enabled = template.enabled;
  },
  { immediate: true }
);

function handleSave() {
  if (!selectedTemplate.value) {
    return;
  }

  emit('saveTemplate', {
    notificationTemplateId: selectedTemplate.value.notificationTemplateId,
    template: {
      templateName: draft.templateName.trim(),
      titleTemplate: draft.titleTemplate.trim(),
      contentTemplate: draft.contentTemplate.trim(),
      enabled: draft.enabled
    }
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Template Manager"
    title="通知模板管理"
    description="统一维护模板启停、标题模板和内容模板。这里的修改直接影响后续通知任务的渲染结果。"
  >
    <div v-if="templates.length" class="template-manager">
      <aside class="template-manager__list">
        <button
          v-for="template in templates"
          :key="template.notificationTemplateId"
          type="button"
          :class="[
            'template-manager__item',
            {
              'template-manager__item--active':
                template.notificationTemplateId === selectedTemplateId
            }
          ]"
          @click="selectedTemplateId = template.notificationTemplateId"
        >
          <div class="template-manager__item-head">
            <p class="template-manager__item-name">{{ template.templateName }}</p>
            <StatusPill
              :label="template.enabled ? '已启用' : '已停用'"
              :tone="template.enabled ? 'success' : 'neutral'"
            />
          </div>
          <p class="template-manager__item-code">{{ template.templateCode }} · {{ template.notifyType }}</p>
          <p class="template-manager__item-time">创建于 {{ formatDateTime(template.createdAt) }}</p>
        </button>
      </aside>

      <div v-if="selectedTemplate" class="template-manager__editor">
        <div class="template-manager__editor-head">
          <div>
            <p class="template-manager__editor-eyebrow">Editing</p>
            <h4 class="template-manager__editor-title">{{ selectedTemplate.templateCode }}</h4>
          </div>
          <label class="template-manager__toggle">
            <input v-model="draft.enabled" type="checkbox" />
            <span>启用模板</span>
          </label>
        </div>

        <label class="template-manager__field">
          <span class="template-manager__field-label">模板名称</span>
          <input v-model="draft.templateName" class="template-manager__input" type="text" />
        </label>

        <label class="template-manager__field">
          <span class="template-manager__field-label">标题模板</span>
          <textarea
            v-model="draft.titleTemplate"
            class="template-manager__textarea"
            rows="3"
          />
        </label>

        <label class="template-manager__field">
          <span class="template-manager__field-label">内容模板</span>
          <textarea
            v-model="draft.contentTemplate"
            class="template-manager__textarea"
            rows="7"
          />
        </label>

        <div class="template-manager__actions">
          <button
            class="template-manager__save"
            type="button"
            :disabled="savingTemplateId === selectedTemplate.notificationTemplateId"
            @click="handleSave"
          >
            {{
              savingTemplateId === selectedTemplate.notificationTemplateId
                ? '保存中...'
                : '保存模板'
            }}
          </button>
        </div>
      </div>
    </div>

    <p v-else class="template-manager__empty">当前没有可维护的通知模板。</p>
  </PanelCard>
</template>

<style scoped>
.template-manager {
  display: grid;
  grid-template-columns: minmax(16rem, 0.86fr) minmax(0, 1.14fr);
  gap: 1rem;
}

.template-manager__list,
.template-manager__editor {
  display: grid;
  gap: 0.75rem;
}

.template-manager__item {
  display: grid;
  gap: 0.45rem;
  width: 100%;
  padding: 0.95rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.72);
  text-align: left;
  cursor: pointer;
}

.template-manager__item--active {
  border-color: rgba(143, 95, 58, 0.4);
  background: rgba(252, 246, 239, 0.96);
  box-shadow: inset 0 0 0 1px rgba(143, 95, 58, 0.15);
}

.template-manager__item-head,
.template-manager__editor-head,
.template-manager__actions {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: flex-start;
}

.template-manager__item-name,
.template-manager__item-code,
.template-manager__item-time,
.template-manager__editor-eyebrow,
.template-manager__editor-title,
.template-manager__field-label,
.template-manager__empty {
  margin: 0;
}

.template-manager__item-name,
.template-manager__editor-title {
  color: var(--color-ink-strong);
}

.template-manager__item-code,
.template-manager__item-time,
.template-manager__editor-eyebrow,
.template-manager__field-label,
.template-manager__toggle,
.template-manager__empty {
  color: var(--color-ink-soft);
}

.template-manager__item-code,
.template-manager__editor-eyebrow,
.template-manager__field-label {
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.template-manager__field {
  display: grid;
  gap: 0.4rem;
}

.template-manager__input,
.template-manager__textarea {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: calc(var(--radius-lg) - 0.25rem);
  padding: 0.7rem 0.8rem;
  background: rgba(255, 251, 247, 0.92);
  color: var(--color-ink-strong);
  font: inherit;
  box-sizing: border-box;
}

.template-manager__textarea {
  resize: vertical;
}

.template-manager__toggle {
  display: flex;
  gap: 0.55rem;
  align-items: center;
}

.template-manager__save {
  min-width: 9rem;
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.72rem 1.15rem;
  background: #8f5f3a;
  color: #fff8f0;
  font: inherit;
  cursor: pointer;
}

.template-manager__save:disabled {
  cursor: wait;
  opacity: 0.7;
}

@media (max-width: 960px) {
  .template-manager {
    grid-template-columns: 1fr;
  }
}
</style>
