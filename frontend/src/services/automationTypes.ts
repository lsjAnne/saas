// 运营自动化中心 M1 契约类型定义。
// 与后端 /api/automation 冻结契约对齐，路径与字段名禁止更改。
// 分页结构沿用前端既有约定（参考 MemberPageResult）：{ list, page, pageSize, total }。

export type AutomationActionType =
  | 'AUTO_SHIP'
  | 'PAUSE_REVIEW'
  | 'AUTO_RECONCILE'
  | 'GENERATE_BILL'
  | 'NOTIFY';

export type AutomationExecutionStatus =
  | 'SUCCESS'
  | 'FAILED'
  | 'PENDING_REVIEW'
  | 'SKIPPED';

export type AutomationTriggerSource = 'SCHEDULE' | 'MANUAL' | 'EVENT';

export type AutomationApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AutomationRule {
  ruleId: string;
  ruleName: string;
  ruleType: string;
  ruleExpression: string;
  enabled: boolean;
}

export interface AutomationAction {
  actionId: string;
  ruleId: string;
  ruleName: string;
  ruleType: string;
  ruleExpression: string;
  actionType: AutomationActionType;
  actionConfig: string;
  priority: number;
  enabled: boolean;
  createdAt: string;
}

export interface AutomationPage<T> {
  list: T[];
  page: number;
  pageSize: number;
  total: number;
}

export interface AutomationExecution {
  executionId: string;
  storeId: string;
  ruleId: string;
  ruleName: string;
  actionType: AutomationActionType;
  triggerSource: AutomationTriggerSource;
  targetType: string;
  targetId: string;
  result: string;
  status: AutomationExecutionStatus;
  retryCount: number;
  costMs: number;
  errorMessage: string | null;
  executedAt: string;
}

export interface AutomationStatsByActionType {
  actionType: AutomationActionType;
  count: number;
  success: number;
  failed: number;
}

export interface AutomationStats {
  total: number;
  success: number;
  failed: number;
  pendingReview: number;
  skipped: number;
  todayTotal: number;
  todaySuccess: number;
  todayFailed: number;
  pendingApprovalCount: number;
  avgCostMs: number;
  byActionType: AutomationStatsByActionType[];
}

export interface AutomationApprovalTask {
  taskId: string;
  executionId: string;
  storeId: string;
  targetType: string;
  targetId: string;
  reason: string;
  status: AutomationApprovalStatus;
  reviewedBy: string | null;
  reviewedAt: string | null;
  createdAt: string;
}

export interface AutomationOrderSyncTriggerResult {
  triggerSource: AutomationTriggerSource;
  pulledOrderCount: number;
  matchedRuleCount: number;
  executionCount: number;
  createdExecutionIds: string[];
}

export interface CreateAutomationActionPayload {
  storeId: string;
  ruleId: string;
  actionType: AutomationActionType;
  actionConfig: string;
  priority: number;
  enabled: boolean;
}

export interface UpdateAutomationActionPayload {
  actionType?: AutomationActionType;
  actionConfig?: string;
  priority?: number;
  enabled?: boolean;
}

export interface AutomationActionQuery {
  storeId?: string;
  ruleId?: string;
  enabled?: boolean;
  page?: number;
  size?: number;
}

export interface AutomationExecutionQuery {
  storeId?: string;
  status?: AutomationExecutionStatus | '';
  actionType?: AutomationActionType | '';
  triggerSource?: AutomationTriggerSource | '';
  page?: number;
  size?: number;
}

export interface AutomationApprovalQuery {
  status?: AutomationApprovalStatus;
  storeId?: string;
  page?: number;
  size?: number;
}

export const AUTOMATION_ACTION_TYPE_OPTIONS: AutomationActionType[] = [
  'AUTO_SHIP',
  'PAUSE_REVIEW',
  'AUTO_RECONCILE',
  'GENERATE_BILL',
  'NOTIFY'
];

export const AUTOMATION_ACTION_TYPE_LABELS: Record<AutomationActionType, string> = {
  AUTO_SHIP: '自动发货',
  PAUSE_REVIEW: '挂起人工',
  AUTO_RECONCILE: '自动对账',
  GENERATE_BILL: '生成单据',
  NOTIFY: '通知'
};
