<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  ConfigureCommitmentWhitelistPayload,
  EnableControlModePayload,
  InteractionReplyPayload,
  LiveCommitmentWhitelistConfigView,
  LiveInteractionReplyView,
  LiveRiskEventView,
  LiveSession,
  LiveSessionStatusView,
  ManualTakeoverPayload,
  PromiseAuditPayload,
  SimulateLiveCallbackPayload,
  SkipCurrentProductResultView,
  StrongControlPayload,
  SwitchLiveScenePayload
} from '@/services/apiTypes';
import { formatDateTime } from '@/utils/formatters';

interface Props {
  liveSessions: LiveSession[];
  selectedPlanId: string | null;
  selectedSessionId: string | null;
  selectedSessionStatus: LiveSessionStatusView | null;
  selectedSessionRiskEvents: LiveRiskEventView[];
  latestSkipResult: SkipCurrentProductResultView | null;
  latestInteractionReply: LiveInteractionReplyView | null;
  latestCommitmentWhitelistConfig: LiveCommitmentWhitelistConfigView | null;
  actionFeedback: string | null;
  isRunningAction: boolean;
}

interface Emits {
  select: [liveSessionId: string];
  pause: [];
  resume: [];
  stop: [];
  skipCurrentProduct: [];
  switchScene: [payload: SwitchLiveScenePayload];
  strongControl: [payload: StrongControlPayload];
  enableControlMode: [payload: EnableControlModePayload];
  resumeSystemMode: [];
  manualTakeover: [payload: ManualTakeoverPayload];
  promiseAudit: [payload: PromiseAuditPayload];
  configureCommitmentWhitelist: [payload: ConfigureCommitmentWhitelistPayload];
  replyInteraction: [payload: InteractionReplyPayload];
  simulateCallback: [payload: SimulateLiveCallbackPayload];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const form = reactive({
  sceneTarget: 'flash_sale',
  sceneReason: 'boost conversion',
  strongControlMode: 'strict_control',
  strongControlReason: 'price commitment risk',
  documentedControlMode: 'enhanced',
  documentedOperatorId: '30021',
  documentedControlReason: 'comment spike',
  takeoverOperator: 'risk-operator',
  takeoverReason: 'human intervention required',
  promiseText: 'guaranteed lowest price',
  promiseRiskLevel: 'high',
  promiseDecision: 'rejected',
  promiseRemark: 'need human review',
  whitelistPhrases: '补发赠品\n短信通知',
  manualConfirmPhrases: '赔付优惠券\n补偿红包',
  prohibitedPhrases: '12小时送达\n最低价保证',
  interactionEventId: '95009',
  customerQuestion: '这单可以补发赠品并短信通知吗？',
  draftReplyText: '支持补发赠品并发送短信通知，请留意站内消息。',
  knowledgeSourceSummary: 'FAQ#gift-01',
  commitmentType: 'compensation',
  callbackEventType: 'paused',
  callbackErrorMessage: 'live interrupted by callback'
});

const sessionStatus = computed(() => props.selectedSessionStatus?.sessionStatus?.toLowerCase() ?? '');
const hasSelectedSession = computed(() => Boolean(props.selectedSessionStatus));
const hasSelectedPlan = computed(() => Boolean(props.selectedPlanId));
const canPause = computed(() => sessionStatus.value === 'running');
const canResume = computed(() => sessionStatus.value === 'paused');
const canStop = computed(() => ['running', 'paused'].includes(sessionStatus.value));
const canGovern = computed(() => ['running', 'paused', 'manual_takeover'].includes(sessionStatus.value));
const canSkipCurrentProduct = computed(() => sessionStatus.value === 'running');
const roomIsMockFallback = computed(
  () => props.selectedSessionStatus?.roomId?.startsWith('mock-room-') ?? false
);
const runtimeRoomSummary = computed(() => {
  const session = props.selectedSessionStatus;

  if (!session?.roomId) {
    return {
      label: '未分配房间',
      detail: '当前会话还没有拿到 roomId，不能判断是真实 runtime 还是 mock 托底。'
    };
  }

  if (roomIsMockFallback.value) {
    return {
      label: '模拟托底房间',
      detail: 'roomId 以 mock-room- 开头，说明系统在 runtime 未接通时走了 mock fallback。'
    };
  }

  return {
    label: '真实运行房间',
    detail: '当前会话已拿到真实房间号，可以按正常直播链路继续跟踪。'
  };
});

watch(
  () => props.selectedSessionStatus,
  (value) => {
    form.sceneTarget = value?.currentScene || 'flash_sale';
    form.strongControlMode = value?.controlMode || 'strict_control';
    form.documentedControlMode = ['standard', 'enhanced', 'manual'].includes(value?.controlMode || '')
      ? (value?.controlMode as 'standard' | 'enhanced' | 'manual')
      : 'enhanced';
    form.takeoverOperator = value?.takeoverOperator || 'risk-operator';
    form.promiseDecision = value?.promiseAuditStatus === 'approved' ? 'approved' : 'rejected';
    form.promiseRemark = value?.promiseAuditRemark || 'need human review';
  },
  { immediate: true }
);

function toneFromStatus(status: string | null | undefined) {
  const normalized = (status ?? '').toLowerCase();

  if (normalized.includes('running') || normalized.includes('approved')) {
    return 'success';
  }

  if (normalized.includes('pause') || normalized.includes('pending') || normalized.includes('manual')) {
    return 'warn';
  }

  if (normalized.includes('fail') || normalized.includes('reject') || normalized.includes('blocked')) {
    return 'warn';
  }

  return 'neutral';
}

function formatReadyFlag(value: boolean) {
  return value ? '已就绪' : '未就绪';
}

function formatStrictGate(enabled: boolean, ready: boolean) {
  if (enabled) {
    return ready ? '严格门禁已满足' : '严格门禁阻断中';
  }

  return ready ? '非严格门禁，当前已就绪' : '非严格门禁，允许降级继续';
}

function parsePhraseList(value: string) {
  return value
    .split(/\r?\n|,/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function handleSwitchScene() {
  emit('switchScene', {
    targetScene: form.sceneTarget.trim(),
    reason: form.sceneReason.trim() || null
  });
}

function handleStrongControl() {
  emit('strongControl', {
    controlMode: form.strongControlMode.trim(),
    reason: form.strongControlReason.trim() || null
  });
}

function handleEnableControlMode() {
  emit('enableControlMode', {
    controlMode: form.documentedControlMode.trim(),
    operatorId: form.documentedOperatorId.trim() || null,
    reason: form.documentedControlReason.trim() || null
  });
}

function handleManualTakeover() {
  emit('manualTakeover', {
    takeoverOperator: form.takeoverOperator.trim(),
    reason: form.takeoverReason.trim() || null
  });
}

function handlePromiseAudit() {
  emit('promiseAudit', {
    promiseText: form.promiseText.trim(),
    riskLevel: form.promiseRiskLevel.trim(),
    decision: form.promiseDecision.trim(),
    remark: form.promiseRemark.trim() || null
  });
}

function handleConfigureCommitmentWhitelist() {
  emit('configureCommitmentWhitelist', {
    whitelistPhrases: parsePhraseList(form.whitelistPhrases),
    manualConfirmPhrases: parsePhraseList(form.manualConfirmPhrases),
    prohibitedPhrases: parsePhraseList(form.prohibitedPhrases)
  });
}

function handleReplyInteraction() {
  emit('replyInteraction', {
    interactionEventId: form.interactionEventId.trim(),
    customerQuestion: form.customerQuestion.trim(),
    draftReplyText: form.draftReplyText.trim() || null,
    knowledgeSourceSummary: form.knowledgeSourceSummary.trim() || null,
    commitmentType: form.commitmentType.trim() || null
  });
}

function handleSimulateCallback() {
  emit('simulateCallback', {
    eventType: form.callbackEventType.trim(),
    errorMessage: form.callbackErrorMessage.trim() || null
  });
}
</script>

<template>
  <PanelCard
    eyebrow="Session Command"
    title="直播会话指挥台"
    description="在播会话、风险互动、承诺回复、控场切换和人工接管都在这里完成。"
  >
    <div class="live-session-command">
      <div class="live-session-command__list">
        <button
          v-for="session in liveSessions"
          :key="session.liveSessionId"
          type="button"
          :class="[
            'live-session-command__item',
            { 'live-session-command__item--active': session.liveSessionId === selectedSessionId }
          ]"
          @click="emit('select', session.liveSessionId)"
        >
          <div class="live-session-command__item-head">
            <div>
              <p class="live-session-command__item-title">{{ session.liveSessionId }}</p>
              <p class="live-session-command__item-meta">
                计划 {{ session.livePlanId }} / 账号 {{ session.liveAccountId }}
              </p>
            </div>
            <StatusPill :label="session.sessionStatus" :tone="toneFromStatus(session.sessionStatus)" />
          </div>
          <p class="live-session-command__item-time">
            开始 {{ formatDateTime(session.actualStartAt) }} / 场景 {{ session.currentScene || '--' }}
          </p>
        </button>
      </div>

      <div class="live-session-command__detail-card">
        <div v-if="selectedSessionStatus" class="live-session-command__detail">
          <div class="live-session-command__detail-head">
            <div>
              <p class="live-session-command__detail-title">{{ selectedSessionStatus.liveSessionId }}</p>
              <p class="live-session-command__detail-meta">
                房间 {{ selectedSessionStatus.roomId || '--' }} / 账号
                {{ selectedSessionStatus.liveAccountName || selectedSessionStatus.liveAccountId }}
              </p>
            </div>
            <StatusPill
              :label="selectedSessionStatus.sessionStatus"
              :tone="toneFromStatus(selectedSessionStatus.sessionStatus)"
            />
          </div>

          <dl class="live-session-command__facts">
            <div class="live-session-command__fact">
              <dt>控场模式</dt>
              <dd>{{ selectedSessionStatus.controlMode || '--' }}</dd>
            </div>
            <div class="live-session-command__fact">
              <dt>当前场景</dt>
              <dd>{{ selectedSessionStatus.currentScene || '--' }}</dd>
            </div>
            <div class="live-session-command__fact">
              <dt>接管状态</dt>
              <dd>
                {{ selectedSessionStatus.takeoverStatus || '--' }} /
                {{ selectedSessionStatus.takeoverOperator || '--' }}
              </dd>
            </div>
            <div class="live-session-command__fact">
              <dt>承诺审核</dt>
              <dd>
                {{ selectedSessionStatus.promiseAuditStatus || '--' }} /
                {{ selectedSessionStatus.promiseAuditRemark || '--' }}
              </dd>
            </div>
            <div class="live-session-command__fact">
              <dt>当前商品</dt>
              <dd>
                {{
                  selectedSessionStatus.currentProductTitle ||
                  selectedSessionStatus.currentProductId ||
                  '当前没有商品上下文'
                }}
              </dd>
            </div>
            <div class="live-session-command__fact">
              <dt>持续时长</dt>
              <dd>{{ selectedSessionStatus.durationSeconds }} 秒</dd>
            </div>
          </dl>

          <div class="live-session-command__runtime-card">
            <div class="live-session-command__runtime-head">
              <div>
                <p class="live-session-command__snippet-title">运行态</p>
                <p class="live-session-command__snippet-copy">
                  {{ runtimeRoomSummary.detail }}
                </p>
              </div>
              <StatusPill :label="runtimeRoomSummary.label" :tone="roomIsMockFallback ? 'warn' : 'success'" />
            </div>

            <dl class="live-session-command__runtime-grid">
              <div class="live-session-command__fact">
                <dt>Runtime Provider</dt>
                <dd>{{ selectedSessionStatus.runtimeProvider || '--' }}</dd>
              </div>
              <div class="live-session-command__fact">
                <dt>Runtime 就绪</dt>
                <dd>{{ formatReadyFlag(selectedSessionStatus.runtimeReady) }}</dd>
              </div>
              <div class="live-session-command__fact">
                <dt>Callback 就绪</dt>
                <dd>{{ formatReadyFlag(selectedSessionStatus.callbackReady) }}</dd>
              </div>
              <div class="live-session-command__fact">
                <dt>房间类型</dt>
                <dd>{{ runtimeRoomSummary.label }}</dd>
              </div>
              <div class="live-session-command__fact">
                <dt>Runtime 门禁</dt>
                <dd>
                  {{
                    formatStrictGate(
                      selectedSessionStatus.strictRuntimeGateEnabled,
                      selectedSessionStatus.runtimeReady
                    )
                  }}
                </dd>
              </div>
              <div class="live-session-command__fact">
                <dt>Callback 门禁</dt>
                <dd>
                  {{
                    formatStrictGate(
                      selectedSessionStatus.strictCallbackGateEnabled,
                      selectedSessionStatus.callbackReady
                    )
                  }}
                </dd>
              </div>
            </dl>

            <div class="live-session-command__endpoint-grid">
              <div class="live-session-command__endpoint-card">
                <p class="live-session-command__endpoint-title">Runtime Endpoint</p>
                <p class="live-session-command__endpoint-value">
                  {{ selectedSessionStatus.runtimeProviderEndpoint || '--' }}
                </p>
              </div>
              <div class="live-session-command__endpoint-card">
                <p class="live-session-command__endpoint-title">Callback Endpoint</p>
                <p class="live-session-command__endpoint-value">
                  {{ selectedSessionStatus.callbackEndpoint || '--' }}
                </p>
              </div>
            </div>

            <p class="live-session-command__runtime-message">
              {{ selectedSessionStatus.readinessMessage || '当前没有额外的运行态提示。' }}
            </p>
          </div>

          <div class="live-session-command__snippet-card">
            <p class="live-session-command__snippet-title">
              脚本版本 {{ selectedSessionStatus.activeScriptVersion || '--' }}
            </p>
            <pre class="live-session-command__snippet">{{
              selectedSessionStatus.currentScriptSnippet || '当前没有可展示的脚本片段。'
            }}</pre>
          </div>

          <div v-if="latestSkipResult" class="live-session-command__snippet-card">
            <p class="live-session-command__snippet-title">跳品结果 {{ latestSkipResult.activeScriptVersion }}</p>
            <p class="live-session-command__snippet-copy">
              跳过片段：{{ latestSkipResult.skippedScriptSnippet || '--' }}
            </p>
            <p class="live-session-command__snippet-copy">
              下一片段：{{ latestSkipResult.nextScriptSnippet || '--' }}
            </p>
          </div>

          <div v-if="latestInteractionReply" class="live-session-command__snippet-card">
            <p class="live-session-command__snippet-title">最新自动回复</p>
            <p class="live-session-command__snippet-copy">
              互动 {{ latestInteractionReply.interactionEventId }} / {{ latestInteractionReply.replyStatus }}
            </p>
            <p class="live-session-command__snippet-copy">{{ latestInteractionReply.replyText }}</p>
            <p class="live-session-command__snippet-copy">
              审计状态：{{ latestInteractionReply.commitmentAuditStatus || '无' }} / 来源
              {{ latestInteractionReply.knowledgeSourceSummary }}
            </p>
          </div>

          <div v-if="latestCommitmentWhitelistConfig" class="live-session-command__snippet-card">
            <p class="live-session-command__snippet-title">
              承诺白名单配置 {{ latestCommitmentWhitelistConfig.livePlanId }}
            </p>
            <p class="live-session-command__snippet-copy">
              白名单：{{ latestCommitmentWhitelistConfig.whitelistPhrases.join('、') || '--' }}
            </p>
            <p class="live-session-command__snippet-copy">
              人工确认：{{ latestCommitmentWhitelistConfig.manualConfirmPhrases.join('、') || '--' }}
            </p>
            <p class="live-session-command__snippet-copy">
              禁止承诺：{{ latestCommitmentWhitelistConfig.prohibitedPhrases.join('、') || '--' }}
            </p>
          </div>

          <div class="live-session-command__snippet-card">
            <p class="live-session-command__snippet-title">会话风险事件</p>
            <ul v-if="selectedSessionRiskEvents.length" class="live-session-command__risk-list">
              <li
                v-for="riskEvent in selectedSessionRiskEvents"
                :key="`${riskEvent.eventCode}-${riskEvent.occurredAt}`"
                class="live-session-command__risk-item"
              >
                <div class="live-session-command__risk-head">
                  <StatusPill :label="riskEvent.severity" :tone="toneFromStatus(riskEvent.severity)" />
                  <span>{{ riskEvent.eventCode }}</span>
                </div>
                <p class="live-session-command__risk-detail">{{ riskEvent.detail }}</p>
                <p class="live-session-command__risk-time">{{ formatDateTime(riskEvent.occurredAt) }}</p>
              </li>
            </ul>
            <p v-else class="live-session-command__snippet-copy">当前会话暂无风险事件。</p>
          </div>

          <p v-if="selectedSessionStatus.errorMessage" class="live-session-command__error">
            {{ selectedSessionStatus.errorMessage }}
          </p>
          <p v-else-if="actionFeedback" class="live-session-command__feedback">
            {{ actionFeedback }}
          </p>

          <div class="live-session-command__actions">
            <button
              type="button"
              class="live-session-command__action"
              :disabled="isRunningAction || !canPause"
              @click="emit('pause')"
            >
              暂停
            </button>
            <button
              type="button"
              class="live-session-command__action live-session-command__action--primary"
              :disabled="isRunningAction || !canResume"
              @click="emit('resume')"
            >
              恢复
            </button>
            <button
              type="button"
              class="live-session-command__action"
              :disabled="isRunningAction || !canSkipCurrentProduct"
              @click="emit('skipCurrentProduct')"
            >
              跳过当前商品
            </button>
            <button
              type="button"
              class="live-session-command__action"
              :disabled="isRunningAction || !canGovern"
              @click="emit('resumeSystemMode')"
            >
              恢复系统模式
            </button>
            <button
              type="button"
              class="live-session-command__action live-session-command__action--warn"
              :disabled="isRunningAction || !canStop"
              @click="emit('stop')"
            >
              停止
            </button>
          </div>

          <div class="live-session-command__governance-grid">
            <form class="live-session-command__governance-card" @submit.prevent="handleSwitchScene">
              <p class="live-session-command__governance-title">场景切换</p>
              <label class="live-session-command__field">
                <span>目标场景</span>
                <input v-model="form.sceneTarget" :disabled="isRunningAction || !canGovern" />
              </label>
              <label class="live-session-command__field">
                <span>切换原因</span>
                <input v-model="form.sceneReason" :disabled="isRunningAction || !canGovern" />
              </label>
              <button class="live-session-command__submit" :disabled="isRunningAction || !canGovern">
                切换场景
              </button>
            </form>

            <form class="live-session-command__governance-card" @submit.prevent="handleEnableControlMode">
              <p class="live-session-command__governance-title">文档控场模式</p>
              <label class="live-session-command__field">
                <span>控场模式</span>
                <select v-model="form.documentedControlMode" :disabled="isRunningAction || !canGovern">
                  <option value="standard">standard</option>
                  <option value="enhanced">enhanced</option>
                  <option value="manual">manual</option>
                </select>
              </label>
              <label class="live-session-command__field">
                <span>操作人</span>
                <input v-model="form.documentedOperatorId" :disabled="isRunningAction || !canGovern" />
              </label>
              <label class="live-session-command__field">
                <span>原因</span>
                <input v-model="form.documentedControlReason" :disabled="isRunningAction || !canGovern" />
              </label>
              <button class="live-session-command__submit" :disabled="isRunningAction || !canGovern">
                启用控场模式
              </button>
            </form>

            <form class="live-session-command__governance-card" @submit.prevent="handleStrongControl">
              <p class="live-session-command__governance-title">强控治理</p>
              <label class="live-session-command__field">
                <span>强控模式</span>
                <select v-model="form.strongControlMode" :disabled="isRunningAction || !canGovern">
                  <option value="strict_control">strict_control</option>
                  <option value="risk_control">risk_control</option>
                  <option value="standard">standard</option>
                </select>
              </label>
              <label class="live-session-command__field">
                <span>原因</span>
                <input v-model="form.strongControlReason" :disabled="isRunningAction || !canGovern" />
              </label>
              <button class="live-session-command__submit" :disabled="isRunningAction || !canGovern">
                提交强控
              </button>
            </form>

            <form class="live-session-command__governance-card" @submit.prevent="handleManualTakeover">
              <p class="live-session-command__governance-title">人工接管</p>
              <label class="live-session-command__field">
                <span>接管人</span>
                <input v-model="form.takeoverOperator" :disabled="isRunningAction || !canGovern" />
              </label>
              <label class="live-session-command__field">
                <span>接管原因</span>
                <input v-model="form.takeoverReason" :disabled="isRunningAction || !canGovern" />
              </label>
              <button class="live-session-command__submit" :disabled="isRunningAction || !canGovern">
                切换接管
              </button>
            </form>

            <form class="live-session-command__governance-card" @submit.prevent="handlePromiseAudit">
              <p class="live-session-command__governance-title">承诺审核</p>
              <label class="live-session-command__field">
                <span>承诺文案</span>
                <input v-model="form.promiseText" :disabled="isRunningAction || !canGovern" />
              </label>
              <div class="live-session-command__field-row">
                <label class="live-session-command__field">
                  <span>风险等级</span>
                  <select v-model="form.promiseRiskLevel" :disabled="isRunningAction || !canGovern">
                    <option value="low">low</option>
                    <option value="medium">medium</option>
                    <option value="high">high</option>
                  </select>
                </label>
                <label class="live-session-command__field">
                  <span>审核结论</span>
                  <select v-model="form.promiseDecision" :disabled="isRunningAction || !canGovern">
                    <option value="approved">approved</option>
                    <option value="rejected">rejected</option>
                  </select>
                </label>
              </div>
              <label class="live-session-command__field">
                <span>审核备注</span>
                <textarea v-model="form.promiseRemark" rows="3" :disabled="isRunningAction || !canGovern" />
              </label>
              <button class="live-session-command__submit" :disabled="isRunningAction || !canGovern">
                提交审核
              </button>
            </form>

            <form class="live-session-command__governance-card" @submit.prevent="handleConfigureCommitmentWhitelist">
              <p class="live-session-command__governance-title">承诺白名单</p>
              <p class="live-session-command__governance-hint">
                当前计划：{{ selectedPlanId || '未选择计划' }}
              </p>
              <label class="live-session-command__field">
                <span>白名单短语</span>
                <textarea v-model="form.whitelistPhrases" rows="3" :disabled="isRunningAction || !hasSelectedPlan" />
              </label>
              <label class="live-session-command__field">
                <span>人工确认短语</span>
                <textarea
                  v-model="form.manualConfirmPhrases"
                  rows="3"
                  :disabled="isRunningAction || !hasSelectedPlan"
                />
              </label>
              <label class="live-session-command__field">
                <span>禁止承诺短语</span>
                <textarea
                  v-model="form.prohibitedPhrases"
                  rows="3"
                  :disabled="isRunningAction || !hasSelectedPlan"
                />
              </label>
              <button class="live-session-command__submit" :disabled="isRunningAction || !hasSelectedPlan">
                保存白名单
              </button>
            </form>

            <form class="live-session-command__governance-card" @submit.prevent="handleReplyInteraction">
              <p class="live-session-command__governance-title">互动自动回复</p>
              <label class="live-session-command__field">
                <span>互动事件 ID</span>
                <input v-model="form.interactionEventId" :disabled="isRunningAction || !hasSelectedSession" />
              </label>
              <label class="live-session-command__field">
                <span>用户问题</span>
                <textarea v-model="form.customerQuestion" rows="3" :disabled="isRunningAction || !hasSelectedSession" />
              </label>
              <label class="live-session-command__field">
                <span>草拟回复</span>
                <textarea v-model="form.draftReplyText" rows="3" :disabled="isRunningAction || !hasSelectedSession" />
              </label>
              <div class="live-session-command__field-row">
                <label class="live-session-command__field">
                  <span>承诺类型</span>
                  <input v-model="form.commitmentType" :disabled="isRunningAction || !hasSelectedSession" />
                </label>
                <label class="live-session-command__field">
                  <span>知识来源</span>
                  <input
                    v-model="form.knowledgeSourceSummary"
                    :disabled="isRunningAction || !hasSelectedSession"
                  />
                </label>
              </div>
              <button class="live-session-command__submit" :disabled="isRunningAction || !hasSelectedSession">
                发送自动回复
              </button>
            </form>

            <form class="live-session-command__governance-card" @submit.prevent="handleSimulateCallback">
              <p class="live-session-command__governance-title">互动回调模拟</p>
              <div class="live-session-command__field-row">
                <label class="live-session-command__field">
                  <span>事件类型</span>
                  <select v-model="form.callbackEventType" :disabled="isRunningAction">
                    <option value="started">started</option>
                    <option value="running">running</option>
                    <option value="paused">paused</option>
                    <option value="resumed">resumed</option>
                    <option value="ended">ended</option>
                    <option value="stopped">stopped</option>
                    <option value="failed">failed</option>
                    <option value="interrupted">interrupted</option>
                  </select>
                </label>
                <label class="live-session-command__field">
                  <span>错误信息</span>
                  <input v-model="form.callbackErrorMessage" :disabled="isRunningAction" />
                </label>
              </div>
              <button class="live-session-command__submit" :disabled="isRunningAction">模拟回调</button>
            </form>
          </div>
        </div>

        <p v-else class="live-session-command__empty">当前没有直播会话可展示。</p>
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.live-session-command {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(22rem, 1.08fr);
  gap: 1rem;
}

.live-session-command__list,
.live-session-command__detail,
.live-session-command__facts,
.live-session-command__governance-grid,
.live-session-command__runtime-grid {
  display: grid;
  gap: 0.8rem;
}

.live-session-command__item {
  display: grid;
  gap: 0.5rem;
  width: 100%;
  padding: 0.95rem 1rem;
  text-align: left;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background: rgba(255, 255, 255, 0.74);
}

.live-session-command__item--active {
  border-color: rgba(156, 113, 71, 0.3);
  box-shadow: 0 14px 30px rgba(112, 84, 55, 0.12);
}

.live-session-command__item-head,
.live-session-command__detail-head,
.live-session-command__risk-head,
.live-session-command__runtime-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.live-session-command__detail-card {
  padding: 1rem;
  border-radius: var(--radius-xl);
  border: 1px solid rgba(91, 72, 54, 0.08);
  background:
    radial-gradient(circle at top right, rgba(198, 172, 140, 0.12), transparent 36%),
    rgba(255, 252, 248, 0.92);
}

.live-session-command__item-title,
.live-session-command__item-meta,
.live-session-command__item-time,
.live-session-command__detail-title,
.live-session-command__detail-meta,
.live-session-command__snippet-title,
.live-session-command__snippet-copy,
.live-session-command__feedback,
.live-session-command__error,
.live-session-command__empty,
.live-session-command__governance-title,
.live-session-command__governance-hint,
.live-session-command__risk-detail,
.live-session-command__risk-time,
.live-session-command__endpoint-title,
.live-session-command__endpoint-value,
.live-session-command__runtime-message {
  margin: 0;
}

.live-session-command__item-title,
.live-session-command__detail-title,
.live-session-command__snippet-title,
.live-session-command__governance-title,
.live-session-command__endpoint-title,
.live-session-command__fact dd,
.live-session-command__endpoint-value,
.live-session-command__runtime-message {
  color: var(--color-ink-strong);
}

.live-session-command__item-meta,
.live-session-command__item-time,
.live-session-command__detail-meta,
.live-session-command__feedback,
.live-session-command__empty,
.live-session-command__fact dt,
.live-session-command__snippet-copy,
.live-session-command__governance-hint,
.live-session-command__risk-time {
  color: var(--color-ink-soft);
}

.live-session-command__fact {
  display: grid;
  gap: 0.28rem;
}

.live-session-command__fact dt,
.live-session-command__endpoint-title {
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.live-session-command__fact dd {
  margin: 0;
  line-height: 1.65;
}

.live-session-command__snippet-card,
.live-session-command__governance-card,
.live-session-command__runtime-card,
.live-session-command__endpoint-card {
  display: grid;
  gap: 0.55rem;
  padding: 0.9rem;
  border-radius: var(--radius-lg);
  background: rgba(250, 245, 239, 0.82);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.live-session-command__runtime-card {
  gap: 0.8rem;
}

.live-session-command__runtime-grid,
.live-session-command__endpoint-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.live-session-command__endpoint-grid {
  display: grid;
  gap: 0.8rem;
}

.live-session-command__endpoint-value {
  line-height: 1.65;
  word-break: break-word;
}

.live-session-command__snippet {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--color-ink);
  font-family: 'IBM Plex Mono', monospace;
  font-size: 0.82rem;
  line-height: 1.7;
}

.live-session-command__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.live-session-command__action,
.live-session-command__submit {
  border: 0;
  border-radius: var(--radius-md);
  padding: 0.72rem 0.95rem;
  font: inherit;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    opacity 0.18s ease;
}

.live-session-command__action {
  background: rgba(91, 72, 54, 0.08);
  color: var(--color-ink-strong);
}

.live-session-command__action--primary,
.live-session-command__submit {
  background: linear-gradient(135deg, #8e6744, #c48d58);
  color: white;
  box-shadow: 0 12px 24px rgba(142, 103, 68, 0.18);
}

.live-session-command__action--warn {
  background: rgba(183, 82, 66, 0.14);
  color: #8f3c2f;
}

.live-session-command__action:disabled,
.live-session-command__submit:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.live-session-command__field,
.live-session-command__field-row {
  display: grid;
  gap: 0.55rem;
}

.live-session-command__field-row {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.live-session-command__field span {
  font-size: 0.8rem;
  color: var(--color-ink-soft);
}

.live-session-command__field input,
.live-session-command__field select,
.live-session-command__field textarea {
  width: 100%;
  border: 1px solid rgba(91, 72, 54, 0.12);
  border-radius: 0.85rem;
  background: rgba(255, 255, 255, 0.9);
  color: var(--color-ink-strong);
  padding: 0.72rem 0.8rem;
  font: inherit;
  resize: vertical;
}

.live-session-command__risk-list {
  display: grid;
  gap: 0.6rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.live-session-command__risk-item {
  display: grid;
  gap: 0.35rem;
  padding: 0.75rem 0.8rem;
  border-radius: 0.9rem;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(91, 72, 54, 0.08);
}

.live-session-command__risk-head {
  align-items: center;
}

.live-session-command__error {
  color: #8f3c2f;
}

@media (max-width: 1180px) {
  .live-session-command {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .live-session-command__field-row,
  .live-session-command__runtime-grid,
  .live-session-command__endpoint-grid {
    grid-template-columns: 1fr;
  }

  .live-session-command__runtime-head {
    flex-direction: column;
  }
}
</style>
