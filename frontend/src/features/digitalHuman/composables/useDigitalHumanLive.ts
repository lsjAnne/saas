import { reactive, toRefs } from 'vue';

import { ApiError } from '@/services/http/apiClient';
import type {
  CreateDigitalHumanAvatarPayload,
  CreateDigitalHumanFaqPayload,
  CreateDigitalHumanSchedulePayload,
  CreateDigitalHumanScriptPayload,
  DigitalHumanAvatar,
  DigitalHumanBarrage,
  DigitalHumanFaq,
  DigitalHumanInteraction,
  DigitalHumanLearningItem,
  DigitalHumanOverview,
  DigitalHumanSchedule,
  DigitalHumanScript,
  DigitalHumanSession,
  InteractionOverview,
  InteractionReplyView,
  LearningGrowth,
  SimulateDigitalHumanBarragePayload,
  SubmitDigitalHumanBarragePayload,
  BarrageConnectionStatus,
} from '@/features/digitalHuman/types';
import {
  confirmDigitalHumanLearningItem,
  createDigitalHumanAvatar,
  createDigitalHumanFaq,
  createDigitalHumanSchedule,
  createDigitalHumanScript,
  deleteDigitalHumanAvatar,
  deleteDigitalHumanFaq,
  getDigitalHumanInteractionOverview,
  getDigitalHumanLearningGrowth,
  getDigitalHumanOverview,
  heartbeatDigitalHumanSession,
  ignoreDigitalHumanLearningItem,
  listBarrageConnections,
  listDigitalHumanAvatars,
  listDigitalHumanBarrages,
  listDigitalHumanFaqs,
  listDigitalHumanInteractions,
  listRecentDigitalHumanInteractions,
  listDigitalHumanLearningItems,
  listDigitalHumanSchedules,
  listDigitalHumanScripts,
  listDigitalHumanSessions,
  simulateDigitalHumanBarrages,
  startBarrageConnection,
  startDigitalHumanSchedule,
  stopBarrageConnection,
  stopDigitalHumanSchedule,
  submitDigitalHumanBarrage,
  updateDigitalHumanAvatar,
  updateDigitalHumanFaq,
  updateDigitalHumanScript,
} from '@/features/digitalHuman/services/digitalHumanService';

export function useDigitalHumanLive() {
  const state = reactive({
    overview: null as DigitalHumanOverview | null,
    avatars: [] as DigitalHumanAvatar[],
    scripts: [] as DigitalHumanScript[],
    schedules: [] as DigitalHumanSchedule[],
    sessions: [] as DigitalHumanSession[],
    faqs: [] as DigitalHumanFaq[],
    barrages: [] as DigitalHumanBarrage[],
    interactions: [] as DigitalHumanInteraction[],
    interactionOverview: null as InteractionOverview | null,
    learningItems: [] as DigitalHumanLearningItem[],
    learningGrowth: null as LearningGrowth | null,
    barrageConnections: [] as BarrageConnectionStatus[],
    liveChat: [] as DigitalHumanInteraction[],
    isLoading: false,
    isRunningAction: false,
    actionFeedback: null as string | null,
    errorMessage: null as string | null,
  });

  function formatError(error: unknown) {
    if (error instanceof ApiError) {
      return error.message;
    }
    return error instanceof Error ? error.message : '请求失败，请稍后重试。';
  }

  async function load(token: string) {
    state.isLoading = true;
    state.errorMessage = null;
    try {
      const [overview, avatars, scripts, schedules, sessions, faqs, barrages, interactionOverview, learningItems, learningGrowth, barrageConnections] =
        await Promise.all([
          getDigitalHumanOverview(token),
          listDigitalHumanAvatars(token),
          listDigitalHumanScripts(token),
          listDigitalHumanSchedules(token),
          listDigitalHumanSessions(token),
          listDigitalHumanFaqs(token),
          listDigitalHumanBarrages(token),
          getDigitalHumanInteractionOverview(token),
          listDigitalHumanLearningItems(token),
          getDigitalHumanLearningGrowth(token),
          listBarrageConnections(token),
        ]);
      state.overview = overview;
      state.avatars = avatars;
      state.scripts = scripts;
      state.schedules = schedules;
      state.sessions = sessions;
      state.faqs = faqs;
      state.barrages = barrages;
      state.interactions = barrages.length
        ? (await listDigitalHumanInteractionsOfFirstSession(barrages, token))
        : [];
      state.interactionOverview = interactionOverview;
      state.learningItems = learningItems;
      state.learningGrowth = learningGrowth;
      state.barrageConnections = barrageConnections;
    } catch (error) {
      state.errorMessage = formatError(error);
    } finally {
      state.isLoading = false;
    }
  }

  async function listDigitalHumanInteractionsOfFirstSession(
    barrages: DigitalHumanBarrage[],
    token: string,
  ): Promise<DigitalHumanInteraction[]> {
    const sessionIds = [...new Set(barrages.map((b) => b.sessionId).filter(Boolean))];
    if (sessionIds.length === 0) {
      return [];
    }
    const results = await Promise.all(
      sessionIds.slice(0, 3).map((id) => listDigitalHumanInteractions(id as string, token)),
    );
    return results.flat().sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  }

  async function refreshInteractions(token: string) {
    const sessionIds = [...new Set(state.barrages.map((b) => b.sessionId).filter(Boolean))];
    if (sessionIds.length === 0) {
      state.interactions = [];
      return;
    }
    const results = await Promise.all(
      sessionIds.slice(0, 3).map((id) => listDigitalHumanInteractions(id as string, token)),
    );
    state.interactions = results.flat().sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  }

  /** 弹幕源轮询：把最新问答实时上屏到直播间聊天区（不刷新整页，轻量单请求） */
  async function refreshRecentChat(token: string) {
    try {
      const recent = await listRecentDigitalHumanInteractions(20, token);
      state.liveChat = recent;
    } catch {
      // 轮询失败静默，下个周期重试
    }
  }

  async function createAvatar(payload: CreateDigitalHumanAvatarPayload, token: string) {
    state.isRunningAction = true;
    state.actionFeedback = null;
    try {
      await createDigitalHumanAvatar(payload, token);
      state.actionFeedback = '数字人形象已创建。';
      await load(token);
    } catch (error) {
      state.actionFeedback = `创建失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function deleteAvatar(avatarId: string, token: string) {
    state.isRunningAction = true;
    try {
      await deleteDigitalHumanAvatar(avatarId, token);
      state.actionFeedback = '数字人形象已删除。';
      await load(token);
    } catch (error) {
      state.actionFeedback = `删除失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function toggleAvatar(avatar: DigitalHumanAvatar, token: string) {
    state.isRunningAction = true;
    try {
      await updateDigitalHumanAvatar(
        avatar.avatarId,
        {
          organizationId: avatar.organizationId ? avatar.organizationId : '',
          avatarName: avatar.avatarName,
          avatarType: avatar.avatarType,
          voiceName: avatar.voiceName,
          previewUrl: avatar.previewUrl,
          enabled: !avatar.enabled,
        },
        token,
      );
      state.actionFeedback = avatar.enabled ? '形象已停用。' : '形象已启用。';
      await load(token);
    } catch (error) {
      state.actionFeedback = `操作失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function createScript(payload: CreateDigitalHumanScriptPayload, token: string) {
    state.isRunningAction = true;
    state.actionFeedback = null;
    try {
      await createDigitalHumanScript(payload, token);
      state.actionFeedback = '话术剧本已创建。';
      await load(token);
    } catch (error) {
      state.actionFeedback = `创建失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function publishScript(script: DigitalHumanScript, token: string) {
    state.isRunningAction = true;
    try {
      await updateDigitalHumanScript(
        script.scriptId,
        {
          organizationId: script.organizationId ? script.organizationId : '',
          scriptName: script.scriptName,
          sceneType: script.sceneType,
          contentText: script.contentText,
          version: script.version,
          status: 'published',
        },
        token,
      );
      state.actionFeedback = '话术已发布。';
      await load(token);
    } catch (error) {
      state.actionFeedback = `发布失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function createSchedule(payload: CreateDigitalHumanSchedulePayload, token: string) {
    state.isRunningAction = true;
    state.actionFeedback = null;
    try {
      await createDigitalHumanSchedule(payload, token);
      state.actionFeedback = '无人排班已创建。';
      await load(token);
    } catch (error) {
      state.actionFeedback = `创建失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function startSchedule(scheduleId: string, token: string) {
    state.isRunningAction = true;
    try {
      const session = await startDigitalHumanSchedule(scheduleId, token);
      state.actionFeedback = `已开播，房间号 ${session.platformRoomId ?? 'mock'}（mock 模式）。`;
      await load(token);
    } catch (error) {
      state.actionFeedback = `开播失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function stopSchedule(scheduleId: string, token: string) {
    state.isRunningAction = true;
    try {
      await stopDigitalHumanSchedule(scheduleId, token);
      state.actionFeedback = '直播已停播。';
      await load(token);
    } catch (error) {
      state.actionFeedback = `停播失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function heartbeatSession(sessionId: string, token: string) {
    state.isRunningAction = true;
    try {
      const session = await heartbeatDigitalHumanSession(sessionId, token);
      state.actionFeedback = `心跳已回读，当前峰值 ${session.viewerPeak} 人、订单 ${session.orderCount}。`;
      await load(token);
    } catch (error) {
      state.actionFeedback = `心跳失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function createFaq(payload: CreateDigitalHumanFaqPayload, token: string) {
    state.isRunningAction = true;
    state.actionFeedback = null;
    try {
      await createDigitalHumanFaq(payload, token);
      state.actionFeedback = '问答已加入知识库。';
      await load(token);
    } catch (error) {
      state.actionFeedback = `创建失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function updateFaq(faqId: string, payload: CreateDigitalHumanFaqPayload, token: string) {
    state.isRunningAction = true;
    try {
      await updateDigitalHumanFaq(faqId, payload, token);
      state.actionFeedback = '问答已更新。';
      await load(token);
    } catch (error) {
      state.actionFeedback = `更新失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function deleteFaq(faqId: string, token: string) {
    state.isRunningAction = true;
    try {
      await deleteDigitalHumanFaq(faqId, token);
      state.actionFeedback = '问答已删除。';
      await load(token);
    } catch (error) {
      state.actionFeedback = `删除失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function sendBarrage(payload: SubmitDigitalHumanBarragePayload, token: string) {
    state.isRunningAction = true;
    try {
      const reply = await submitDigitalHumanBarrage(payload, token);
      state.liveChat = [reply.interaction, ...state.liveChat].slice(0, 50);
      state.actionFeedback = reply.interaction.hitType === 'faq'
        ? '数字人已回答该弹幕。'
        : '未命中知识库，已用兜底话术回应。';
      await load(token);
      return reply;
    } catch (error) {
      state.actionFeedback = `弹幕发送失败：${formatError(error)}`;
      return null;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function simulateBarrages(payload: SimulateDigitalHumanBarragePayload, token: string) {
    state.isRunningAction = true;
    try {
      const replies = await simulateDigitalHumanBarrages(payload, token);
      state.liveChat = [
        ...replies.map((r) => r.interaction).reverse(),
        ...state.liveChat,
      ].slice(0, 50);
      state.actionFeedback = `已模拟 ${replies.length} 条观众弹幕，数字人逐条作答。`;
      await load(token);
      return replies;
    } catch (error) {
      state.actionFeedback = `模拟弹幕失败：${formatError(error)}`;
      return [];
    } finally {
      state.isRunningAction = false;
    }
  }

  async function confirmLearningItem(itemId: string, token: string) {
    state.isRunningAction = true;
    try {
      await confirmDigitalHumanLearningItem(itemId, token);
      state.actionFeedback = '已确认该问题，转正进知识库。';
      await load(token);
    } catch (error) {
      state.actionFeedback = `确认失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function ignoreLearningItem(itemId: string, token: string) {
    state.isRunningAction = true;
    try {
      await ignoreDigitalHumanLearningItem(itemId, token);
      state.actionFeedback = '已忽略该问题，不再自动学习。';
      await load(token);
    } catch (error) {
      state.actionFeedback = `忽略失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function startBarrage(platformType: string, token: string) {
    state.isRunningAction = true;
    try {
      state.barrageConnections = await startBarrageConnection(platformType, token);
      const running = state.barrageConnections.find((c) => c.platformType === platformType);
      state.actionFeedback = running?.running
        ? `${platformLabel(platformType)}弹幕流已启动（${running.mode === 'mock' ? '模拟模式，实时观看弹幕进入问答' : '真实模式待联调'}）。`
        : `${platformLabel(platformType)}启动失败，请检查配置。`;
      await load(token);
    } catch (error) {
      state.actionFeedback = `启动失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  async function stopBarrage(platformType: string, token: string) {
    state.isRunningAction = true;
    try {
      state.barrageConnections = await stopBarrageConnection(platformType, token);
      state.actionFeedback = `${platformLabel(platformType)}弹幕流已停止。`;
      await load(token);
    } catch (error) {
      state.actionFeedback = `停止失败：${formatError(error)}`;
    } finally {
      state.isRunningAction = false;
    }
  }

  function platformLabel(platformType: string) {
    const labels: Record<string, string> = {
      douyin: '抖音',
      kuaishou: '快手',
      wechat_channels: '视频号',
      mock: '模拟弹幕',
    };
    return labels[platformType] ?? platformType;
  }

  return reactive({
    ...toRefs(state),
    load,
    createAvatar,
    deleteAvatar,
    toggleAvatar,
    createScript,
    publishScript,
    createSchedule,
    startSchedule,
    stopSchedule,
    heartbeatSession,
    createFaq,
    updateFaq,
    deleteFaq,
    sendBarrage,
    simulateBarrages,
    refreshInteractions,
    refreshRecentChat,
    confirmLearningItem,
    ignoreLearningItem,
    startBarrage,
    stopBarrage,
    platformLabel,
  });
}
