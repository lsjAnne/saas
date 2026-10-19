export interface DigitalHumanAvatar {
  avatarId: string;
  organizationId?: string;
  avatarName: string;
  avatarType: string;
  voiceName?: string;
  previewUrl?: string;
  enabled: boolean;
  createdAt: string;
}

export interface DigitalHumanScript {
  scriptId: string;
  organizationId?: string;
  scriptName: string;
  sceneType: string;
  contentText: string;
  version: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface DigitalHumanSchedule {
  scheduleId: string;
  scheduleName: string;
  avatarId: string;
  scriptId: string;
  platformType: string;
  repeatType: string;
  startAt: string;
  endAt?: string;
  enabled: boolean;
  status: string;
  createdAt: string;
}

export interface DigitalHumanSession {
  sessionId: string;
  scheduleId?: string;
  avatarId?: string;
  platformRoomId?: string;
  liveStatus: string;
  startedAt?: string;
  endedAt?: string;
  viewerPeak: number;
  orderCount: number;
  gmvAmount: number;
  heartbeatAt?: string;
  mockFlag: boolean;
  createdAt: string;
}

export interface DigitalHumanOverview {
  avatarCount: number;
  scriptCount: number;
  scheduleCount: number;
  liveSessionCount: number;
  totalSessionCount: number;
  todayGmv: number;
  todayOrders: number;
}

export interface CreateDigitalHumanAvatarPayload {
  organizationId: string;
  avatarName: string;
  avatarType: string;
  voiceName?: string;
  previewUrl?: string;
  enabled?: boolean;
}

export interface CreateDigitalHumanScriptPayload {
  organizationId: string;
  scriptName: string;
  sceneType: string;
  contentText: string;
  version?: string;
  status?: string;
}

export interface CreateDigitalHumanSchedulePayload {
  organizationId: string;
  scheduleName: string;
  avatarId: string;
  scriptId: string;
  platformType: string;
  repeatType: string;
  startAt: string;
  endAt?: string;
  enabled?: boolean;
}

export interface DigitalHumanFaq {
  faqId: string;
  organizationId?: string;
  productId?: string;
  productName?: string;
  questionType: string;
  keywords?: string;
  question: string;
  answer: string;
  enabled: boolean;
  hitCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface DigitalHumanBarrage {
  barrageId: string;
  sessionId?: string;
  platformType: string;
  userNick?: string;
  content: string;
  replyStatus: string;
  createdAt: string;
}

export interface DigitalHumanInteraction {
  interactionId: string;
  sessionId?: string;
  barrageId?: string;
  hitType: string;
  faqId?: string;
  question: string;
  answer: string;
  createdAt: string;
}

export interface InteractionReplyView {
  barrage: DigitalHumanBarrage;
  interaction: DigitalHumanInteraction;
}

export interface HotQuestionView {
  productName?: string;
  question: string;
  hitCount: number;
}

export interface InteractionOverview {
  faqCount: number;
  barrageCount: number;
  repliedCount: number;
  fallbackCount: number;
  hitRate: number;
  hotQuestions: HotQuestionView[];
}

export interface CreateDigitalHumanFaqPayload {
  organizationId: string;
  productId?: string;
  productName?: string;
  questionType: string;
  keywords?: string;
  question: string;
  answer: string;
  enabled?: boolean;
}

export interface SubmitDigitalHumanBarragePayload {
  organizationId: string;
  sessionId?: string;
  platformType?: string;
  userNick?: string;
  content: string;
}

export interface SimulateDigitalHumanBarragePayload {
  organizationId: string;
  sessionId?: string;
  platformType?: string;
  count?: number;
}

export interface DigitalHumanLearningItem {
  itemId: string;
  tenantId: string;
  organizationId?: string;
  sessionId?: string;
  question: string;
  questionType: string;
  productName?: string;
  occurrenceCount: number;
  status: string;
  faqId?: string;
  lastSeenAt: string;
  createdAt: string;
}

export interface HotUnansweredQuestion {
  itemId: string;
  question: string;
  questionType: string;
  productName?: string;
  occurrenceCount: number;
  status: string;
}

export interface LearningGrowth {
  faqCount: number;
  manualFaqCount: number;
  autoLearnedCount: number;
  memoryTotal: number;
  statusNewCount: number;
  statusLearningCount: number;
  statusConfirmedCount: number;
  statusIgnoredCount: number;
  conversionRate: number;
  hotUnanswered: HotUnansweredQuestion[];
}

/** 直播平台弹幕连接器状态（弹幕源接入面板） */
export interface BarrageConnectionStatus {
  platformType: string;
  running: boolean;
  mode: 'mock' | 'real';
  connectedAt?: string | null;
  lastMessageAt?: string | null;
  messageCount: number;
  errorMessage?: string | null;
}
