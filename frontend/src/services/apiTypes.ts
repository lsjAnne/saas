export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  traceId: string;
}

export interface LoginUser {
  id: string;
  name: string;
  role: string;
  tenantId: string;
  organizationId: string;
  operatorType: string;
  permissionCodes: string[];
}

export interface LoginPayload {
  token: string;
  user: LoginUser;
}

export interface RegisterTenantRequest {
  tenantName: string;
  ownerName: string;
  mobile: string;
}

export interface RegisterTenantResponse {
  tenantId: string;
  tenantCode: string;
  tenantName: string;
  tenantStatus: string;
  defaultOrganizationId: string;
  trialEndAt: string | null;
}

export interface SubscriptionPlan {
  planCode: string;
  planName: string;
  billingType: string;
  monthlyPrice: number;
  yearlyPrice: number;
  seatLimit: number;
}

export interface TenantSubscription {
  subscriptionId: string;
  tenantId: string;
  planCode: string;
  planName: string;
  subscriptionStatus: string;
  startedAt: string;
  expiredAt: string;
  seatCount: number;
  autoRenew: boolean;
}

export interface UsageQuota {
  tenantId: string;
  quotaCode: string;
  quotaLimit: number;
  usedAmount: number;
  resetAt: string;
}

export interface BillingOrder {
  billingOrderId: string;
  tenantId: string;
  planCode: string;
  planName: string;
  orderType: string;
  payableAmount: number;
  paymentStatus: string;
  externalOrderNo: string | null;
  paidAt: string | null;
  createdAt: string;
}

export interface SubscriptionActionPayload {
  planCode: string;
  seatCount: number;
  autoRenew: boolean;
}

export interface PlanChangePayload {
  planCode: string;
}

export interface SubscriptionRenewPayload {
  months: number;
}

export interface SeatPurchasePayload {
  seatCount: number;
}

export interface InvoiceRequestPayload {
  billingOrderId: string;
  invoiceTitle: string;
  invoiceTaxNo: string;
}

export interface InvoiceRequest {
  invoiceRequestId: string;
  tenantId: string;
  billingOrderId: string;
  invoiceTitle: string;
  invoiceTaxNo: string;
  invoiceStatus: string;
  createdAt: string;
}

export interface ExternalIntegrationOption {
  systemCode: string;
  displayName: string;
  selected: boolean;
}

export interface TenantContextView {
  tenantId: string;
  tenantCode: string;
  tenantName: string;
  tenantStatus: string;
  operatorId: string;
  operatorType: string;
  defaultOrganizationId: string;
}

export interface TenantProfile {
  tenantId: string;
  tenantCode: string;
  tenantName: string;
  tenantStatus: string;
  ownerName: string;
  mobile: string;
  defaultOrganizationId: string;
  featureFlags: Record<string, boolean>;
  trialEndAt: string | null;
  createdAt: string;
}

export interface Store {
  storeId: string;
  tenantId: string;
  organizationId: string;
  ownerUserId: string;
  platformType: string;
  platformShopId: string;
  shopName: string;
  authStatus: string;
  profitThreshold: number;
  riskThreshold: number;
  defaultShipConfig: Record<string, unknown> | null;
  createdAt: string;
}

export interface ChannelAccount {
  channelAccountId: string;
  organizationId: string;
  channelType: string;
  accountName: string;
  authStatus: string;
  expiresAt: string | null;
  extraConfig: Record<string, unknown> | null;
  createdAt: string;
}

export interface Supplier {
  supplierId: string;
  storeId: string;
  supplierPlatformType: string;
  supplierPlatformId: string;
  supplierName: string;
  sourceUrl: string | null;
  priceScore: number | null;
  deliveryScore: number | null;
  stabilityScore: number | null;
  riskLevel: string | null;
  dropshipSupportFlag: boolean;
  primary: boolean;
  backup: boolean;
  blacklistFlag: boolean;
  createdAt: string;
}

export interface CreateSupplierPayload {
  storeId: string;
  supplierPlatformType: string;
  supplierPlatformId: string;
  supplierName: string;
  sourceUrl?: string;
  priceScore?: number | null;
  deliveryScore?: number | null;
  stabilityScore?: number | null;
  riskLevel?: string;
  dropshipSupportFlag?: boolean;
}

export interface UpdateSupplierPayload {
  supplierName: string;
  sourceUrl?: string;
  priceScore?: number | null;
  deliveryScore?: number | null;
  stabilityScore?: number | null;
  riskLevel?: string;
  dropshipSupportFlag?: boolean;
  blacklistFlag?: boolean;
}

export interface SupplierAdmissionReview {
  supplierId: string;
  storeId: string;
  admissionStatus: string;
  whitelistFlag: boolean;
  qualificationComplete: boolean;
  qualificationDocs: string[];
  remark: string | null;
  reviewedAt: string;
}

export interface CreateSupplierAdmissionReviewPayload {
  qualificationDocs: string[];
  decision: string;
  remark?: string;
}

export interface SupplierScorecard {
  supplierId: string;
  storeId: string;
  deliveryScore: number;
  fulfillmentScore: number;
  qualityScore: number;
  compositeScore: number;
  ratingGrade: string;
  scoredAt: string;
}

export interface CreateSupplierScorecardPayload {
  deliveryScore: number;
  fulfillmentScore: number;
  qualityScore: number;
}

export interface SupplierDeliveryAppointment {
  appointmentId: string;
  supplierId: string;
  storeId: string;
  purchaseReference: string;
  appointmentDate: string;
  plannedQty: number;
  appointmentStatus: string;
  remark: string | null;
  createdAt: string;
}

export interface CreateSupplierDeliveryAppointmentPayload {
  storeId: string;
  purchaseReference: string;
  appointmentDate: string;
  plannedQty: number;
  remark?: string;
}

export interface SupplierSettlementStatement {
  statementId: string;
  supplierId: string;
  storeId: string;
  statementPeriod: string;
  accountPeriodDays: number;
  payableAmount: number;
  dueDate: string;
  settlementStatus: string;
  createdAt: string;
}

export interface CreateSupplierSettlementStatementPayload {
  storeId: string;
  supplierId: string;
  statementPeriod: string;
  accountPeriodDays: number;
  payableAmount: number;
  dueDate: string;
}

export interface SupplierRiskEvent {
  riskEventId: string;
  supplierId: string;
  storeId: string;
  warningLevel: string;
  riskType: string;
  fallbackSupplierId: string;
  remark: string | null;
  createdAt: string;
}

export interface CreateSupplierRiskEventPayload {
  riskType: string;
  severity: string;
  remark?: string;
}

export interface SupplierSrmLinkage {
  supplierId: string;
  storeId: string;
  admissionStatus: string;
  ratingGrade: string;
  inquiryCount: number;
  deliveryAppointmentCount: number;
  settlementStatementCount: number;
  riskEventCount: number;
  latestInquiryId: string | null;
  erpSyncStatus: string;
  wmsInboundStatus: string;
}

export interface CampaignActivity {
  campaignId: string;
  storeId: string;
  activityType: string;
  activityName: string;
  status: string;
  startAt: string;
  endAt: string;
  productIds: string[];
  rule: Record<string, unknown>;
  couponTemplateId: string | null;
  needApproval: boolean;
  createdAt: string;
}

export interface CreateCampaignPayload {
  storeId: string;
  activityType: string;
  activityName: string;
  startAt: string;
  endAt: string;
  productIds: string[];
  rule: Record<string, unknown>;
  couponTemplateId?: string;
}

export interface UpdateCampaignPayload {
  storeId: string;
  activityType: string;
  activityName: string;
  startAt: string;
  endAt: string;
  productIds: string[];
  rule: Record<string, unknown>;
  couponTemplateId?: string;
}

export interface CouponTemplate {
  couponTemplateId: string;
  storeId: string;
  templateName: string;
  discountType: string;
  discountValue: number;
  thresholdAmount: number | null;
  status: string;
  createdAt: string;
}

export interface CreateCouponTemplatePayload {
  storeId: string;
  templateName: string;
  discountType: string;
  discountValue: number;
  thresholdAmount?: number;
  status?: string;
}

export interface ConnectStorePayload {
  organizationId: string;
  ownerUserId: string;
  platformType: string;
  platformShopId: string;
  shopName: string;
  profitThreshold: number;
  riskThreshold: number;
  defaultShipConfig?: Record<string, unknown>;
}

export interface UpdateStoreSettingsPayload {
  profitThreshold: number;
  riskThreshold: number;
  defaultShipConfig?: Record<string, unknown>;
}

export interface CreateChannelAccountPayload {
  organizationId: string;
  channelType: string;
  accountName: string;
  extraConfig?: Record<string, unknown>;
}

export interface CandidateProduct {
  candidateProductId: string;
  storeId: string;
  sourceType: string;
  sourceUrl: string;
  sourceUrlHash: string;
  title: string;
  category: string | null;
  status: string;
  estimatedProfit: number | null;
  riskLevel: string | null;
  recommendationReason: string | null;
  aiSummary: string | null;
  createdAt: string;
}

export interface ProductDraft {
  productDraftId: string;
  storeId: string;
  candidateProductId: string;
  title: string;
  sellingPoints: string | null;
  detailContent: string | null;
  faqContent: string | null;
  suggestedPrice: number | null;
  status: string;
  aiVersion: string | null;
  createdAt: string;
}

export interface Product {
  productId: string;
  storeId: string;
  platformProductId: string;
  productDraftId: string;
  title: string;
  status: string;
  healthScore: number | null;
  publishedAt: string | null;
  createdAt: string;
}

export interface ProductSourceMapping {
  productMappingId: string;
  storeId: string;
  productId: string;
  supplierId: string;
  mappingType: string;
  active: boolean;
  riskFlag: boolean;
  createdAt: string;
}

export interface ProductMappingCatalogEntry {
  productId: string;
  storeId: string;
  storeName: string;
  platformProductId: string | null;
  title: string;
  status: string;
  healthScore: number | null;
  publishedAt: string | null;
  createdAt: string;
  mappingCount: number;
  activePrimaryCount: number;
  activeBackupCount: number;
  productMissing: boolean;
}

export interface CreateProductSourceMappingPayload {
  storeId: string;
  productId: string;
  supplierId: string;
  mappingType: string;
}

export interface SwitchProductMappingSupplierPayload {
  supplierId: string;
}

export interface ContentAsset {
  assetId: string;
  storeId: string;
  assetCategory: string;
  assetType: string;
  assetName: string;
  assetStatus: string;
  previewMode: string;
  previewUrl: string | null;
  previewText: string | null;
  generated: boolean;
  sourceChannel: string;
  createdAt: string;
  updatedAt: string;
}

export interface ContentAssetVersion {
  versionId: string;
  assetId: string;
  versionNo: number;
  versionLabel: string;
  versionStatus: string;
  changeSummary: string | null;
  contentSnapshot: string | null;
  previewUrl: string | null;
  createdAt: string;
}

export interface ContentAssetReference {
  referenceId: string;
  assetId: string;
  referenceType: string;
  referenceName: string;
  referenceTargetId: string;
  quoteText: string | null;
  createdAt: string;
}

export interface ContentAssetDetailView {
  asset: ContentAsset;
  versions: ContentAssetVersion[];
  references: ContentAssetReference[];
}

export interface ContentAssetReferenceCopyView {
  assetId: string;
  assetName: string;
  referenceText: string;
  referenceCount: number;
  generatedAt: string;
}

export interface UploadContentAssetPayload {
  storeId: string;
  assetCategory: string;
  assetType: string;
  assetName: string;
  sourceUrl?: string;
  previewText?: string;
  changeSummary?: string;
  referenceHint?: string;
}

export interface GenerateContentAssetPayload {
  storeId: string;
  assetCategory: string;
  assetType: string;
  assetName: string;
  brief: string;
  tone?: string;
  referenceHint?: string;
  sourceAssetId?: string;
  clipTemplate?: string;
  durationSeconds?: number;
  publishPlatform?: string;
}

export interface ArchiveContentAssetVersionPayload {
  remark?: string;
}

export interface PublishContentAssetPayload {
  platformCode: string;
  publishTitle?: string;
  publishRemark?: string;
}

export interface CreateCandidateProductPayload {
  storeId: string;
  sourceType: string;
  sourceUrl: string;
  title: string;
  category?: string;
  estimatedProfit?: number;
  riskLevel?: string;
  recommendationReason?: string;
  aiSummary?: string;
}

export interface UpdateCandidateProductPayload {
  sourceUrl: string;
  title: string;
  category?: string;
  estimatedProfit?: number;
  riskLevel?: string;
  recommendationReason?: string;
  aiSummary?: string;
}

export interface UpdateCandidateProductStatusPayload {
  status: string;
}

export interface GenerateProductDraftPayload {
  candidateProductId: string;
  aiVersion?: string;
  suggestedPrice?: number;
}

export interface UpdateProductDraftPayload {
  title: string;
  sellingPoints?: string;
  detailContent?: string;
  faqContent?: string;
  suggestedPrice?: number;
  status: string;
}

export interface PublishProductDraftPayload {
  platformProductId?: string;
}

export interface OrderMain {
  orderId: string;
  storeId: string;
  platformOrderId: string;
  orderStatus: string;
  logisticsStatus: string;
  totalAmount: number | null;
  estimatedProfit: number | null;
  buyerName: string | null;
  buyerPhoneMask: string | null;
  shippingAddress: string | null;
  timeoutAt: string | null;
  createdAt: string;
}

export interface StandardizedOrderView {
  orderId: string;
  storeId: string;
  platformOrderId: string;
  sourcePlatform: string;
  standardOrderStatus: string;
  totalAmount: number | null;
  createdAt: string;
}

export interface OmsWorkbenchView {
  standardizedOrderCount: number;
  riskOrderCount: number;
  splitOrderCount: number;
  mergedOrderGroupCount: number;
  manualReplayCount: number;
  reverseOrderCount: number;
}

export interface OrderAuditReviewView {
  orderId: string;
  storeId: string;
  riskLevel: string;
  addressValid: boolean;
  riskTags: string[];
  reviewedAt: string;
}

export interface OrderSplitChildView {
  childOrderId: string | null;
  childLabel: string;
  quantity: number;
  orderType: string;
}

export interface OrderSplitView {
  parentOrderId: string;
  storeId: string;
  splitStatus: string;
  remark: string | null;
  children: OrderSplitChildView[];
  updatedAt: string;
}

export interface OrderMergeView {
  mergeGroupId: string;
  storeId: string;
  orderIds: string[];
  mergedOrderType: string;
  remark: string | null;
  createdAt: string;
}

export interface OrderRoutePlanView {
  orderId: string;
  storeId: string;
  priorityCode: string;
  routeWarehouseCode: string;
  shippingStrategy: string | null;
  plannedAt: string;
}

export interface OrderReverseStatusView {
  orderId: string;
  storeId: string;
  reverseStatus: string;
  reverseCategory: string;
  remark: string | null;
  updatedAt: string;
}

export interface OrderOrchestrationView {
  orderId: string;
  storeId: string;
  fulfillmentTaskId: string | null;
  splitChildCount: number;
  mergeOrderCount: number;
  routeWarehouseCode: string | null;
  wmsSyncStatus: string | null;
  tmsSyncStatus: string | null;
  financeSyncStatus: string | null;
}

export interface OrderItem {
  orderItemId: string;
  orderId: string;
  productId: string;
  skuId: string;
  quantity: number;
  unitPrice: number | null;
  createdAt: string;
}

export interface OrderDetailView {
  order: OrderMain;
  items: OrderItem[];
}

export interface CreateOrderSyncPayload {
  storeId: string;
  platformOrderId: string;
  productId: string;
  skuId: string;
  quantity: number;
  unitPrice: number;
  buyerName: string;
  buyerPhoneMask: string;
  shippingAddress: string;
}

export interface OrderSyncResult {
  taskId: string;
  storeId: string;
  orderId: string;
  fulfillmentTaskId: string;
  reused: boolean;
}

export interface CreateOrderSplitChildPayload {
  childLabel: string;
  quantity: number;
  orderType: string;
}

export interface CreateOrderSplitPayload {
  remark?: string;
  children: CreateOrderSplitChildPayload[];
}

export interface CreateOrderMergePayload {
  storeId: string;
  orderIds: string[];
  mergedOrderType: string;
  remark?: string;
}

export interface CreateOrderRoutePlanPayload {
  warehouseCandidates: string[];
  shippingStrategy?: string;
}

export interface CreateOrderReverseStatusPayload {
  reverseStatus: string;
  remark?: string;
}

export interface FulfillmentTask {
  fulfillmentTaskId: string;
  storeId: string;
  orderId: string;
  idempotencyKey: string | null;
  status: string;
  retryCount: number | null;
  dueAt: string | null;
  lastErrorMessage: string | null;
  createdAt: string;
}

export interface FulfillmentReplayView {
  fulfillmentTaskId: string;
  status: string;
  replayCount: number;
}

export interface LogisticsRecord {
  logisticsRecordId: string;
  fulfillmentTaskId: string;
  trackingNumber: string;
  logisticsCompany: string;
  logisticsStatus: string;
  syncedAt: string | null;
  createdAt: string;
}

export interface CreateLogisticsRecordPayload {
  trackingNumber: string;
  logisticsCompany: string;
  logisticsStatus: string;
}

export interface ExceptionTask {
  exceptionTaskId: string;
  storeId: string;
  relatedType: string;
  relatedId: string;
  exceptionType: string;
  severity: string;
  status: string;
  suggestionText: string;
  ownerUserId: string | null;
  createdAt: string;
}

export interface UpdateExceptionTaskPayload {
  action?: string;
  operatorId?: string;
  remark?: string;
}

export interface DashboardSummary {
  todaySalesAmount: number;
  todayOrderCount: number;
  grossProfit: number;
  exceptionCount: number;
  pendingConfirmCount: number;
  lowStockCount: number;
  topSuggestion: string;
}

export interface DashboardTrend {
  date: string;
  salesAmount: number;
  orderCount: number;
  grossProfit: number;
}

export interface DashboardRisk {
  riskCode: string;
  riskName: string;
  riskLevel: string;
  riskCount: number;
  suggestion: string;
}

export interface DashboardCampaignAnalysis {
  totalCampaignCount: number;
  publishedCampaignCount: number;
  pendingApprovalCount: number;
  publishSuccessRate: number;
  totalCouponTemplateCount: number;
  recommendedStrategy: string;
}

export interface DashboardMemberAnalysis {
  totalMembers: number;
  vipMembers: number;
  dormantMembers: number;
  averagePaidAmount: number;
  repurchaseRate: number;
}

export interface RecommendationOverviewView {
  headline: string;
  productRecommendationCount: number;
  memberRecommendationCount: number;
  campaignRecommendationCount: number;
  recommendedActions: string[];
}

export interface ProductRecommendationView {
  candidateProductId: string;
  storeId: string;
  title: string;
  category: string | null;
  status: string;
  estimatedProfit: number | null;
  riskLevel: string | null;
  recommendationReason: string;
  aiSummary: string | null;
  score: number;
  suggestedAction: string;
}

export interface MemberRecommendationView {
  memberId: string;
  storeId: string;
  nickname: string;
  levelCode: string;
  totalOrderCount: number;
  totalPaidAmount: number | null;
  segmentCode: string;
  recommendationReason: string;
  score: number;
  suggestedAction: string;
}

export interface CampaignRecommendationView {
  campaignType: string;
  campaignName: string;
  targetStoreId: string;
  recommendedCandidateProductIds: string[];
  targetMemberSegment: string;
  recommendationReason: string;
  suggestedRule: Record<string, unknown>;
  score: number;
  suggestedAction: string;
}

export type BusinessAssistantIntent =
  | 'summary'
  | 'product'
  | 'member'
  | 'order'
  | 'risk'
  | 'fulfillment'
  | 'inventory'
  | 'campaign';

export type BusinessAssistantConfidence = 'high' | 'medium' | 'low';

export interface BusinessAssistantAction {
  label: string;
  route: string;
  description: string;
}

export interface BusinessAssistantResponse {
  id: string;
  intent: BusinessAssistantIntent;
  intentLabel: string;
  question: string;
  storeName: string;
  title: string;
  summary: string;
  confidence: BusinessAssistantConfidence;
  confidenceLabel: string;
  confidenceReason: string;
  coverageLabels: string[];
  matchedSignals: string[];
  evidence: string[];
  insights: string[];
  followUpQuestions: string[];
  actions: BusinessAssistantAction[];
  sourceLabels: string[];
  createdAt: string;
}

export interface InventorySnapshot {
  inventorySnapshotId: string;
  storeId: string;
  productId: string;
  skuId: string;
  availableStock: number;
  reservedStock: number;
  safetyStock: number;
  snapshotAt: string;
  createdAt: string;
}

export interface UpdateInventorySafetyStockPayload {
  safetyStock: number;
}

export interface ReplenishmentTask {
  replenishmentTaskId: string;
  storeId: string;
  productId: string;
  skuId: string;
  suggestedQty: number;
  taskStatus: string;
  approvalStatus: string;
  reasonText: string | null;
  createdAt: string;
}

export interface CreateReplenishmentTaskPayload {
  storeId: string;
  productId: string;
  skuId: string;
  suggestedQty: number;
  reasonText?: string;
}

export interface PurchaseRequestItem {
  productId: string;
  skuId: string;
  requestedQty: number;
  targetUnitPrice: number;
}

export interface PurchaseRequest {
  purchaseRequestId: string;
  requestNo: string;
  storeId: string;
  supplierId: string;
  requestStatus: string;
  approvalStatus: string;
  requestedBy: string;
  expectedDeliveryDate: string;
  totalRequestedQty: number;
  reasonText: string;
  items: PurchaseRequestItem[];
  createdAt: string;
}

export interface CreatePurchaseOrderPayload {
  purchaseRequestId: string;
  remark?: string;
}

export interface PurchaseOrder {
  purchaseOrderId: string;
  orderNo: string;
  purchaseRequestId: string;
  purchaseRequestNo: string;
  storeId: string;
  supplierId: string;
  orderStatus: string;
  approvalStatus: string;
  dispatchStatus: string;
  receivingStatus: string;
  inboundStatus: string;
  discrepancyStatus: string;
  returnStatus: string;
  purchaserId: string;
  expectedDeliveryDate: string;
  totalRequestedQty: number;
  items: PurchaseRequestItem[];
  remark: string | null;
  createdAt: string;
}

export interface PurchaseReceiptItem {
  productId: string;
  skuId: string;
  receivedQty: number;
  batchNo: string;
  productionDate: string | null;
  expiryDate: string | null;
  unitCost: number;
  receivedAt: string;
}

export interface PurchaseReceipt {
  purchaseReceiptId: string;
  receiptNo: string;
  purchaseOrderId: string;
  purchaseOrderNo: string;
  storeId: string;
  supplierId: string;
  inboundStatus: string;
  receiverId: string;
  items: PurchaseReceiptItem[];
  remark: string | null;
  receivedAt: string;
}

export interface PurchaseReceiptDiscrepancy {
  discrepancyId: string;
  discrepancyNo: string;
  purchaseOrderId: string;
  purchaseOrderNo: string;
  storeId: string;
  productId: string;
  skuId: string;
  orderedQty: number;
  receivedQty: number;
  discrepancyQty: number;
  discrepancyType: string;
  status: string;
  reportedAt: string;
}

export interface PayableLedgerEntry {
  ledgerEntryId: string;
  ledgerNo: string;
  storeId: string;
  supplierId: string;
  purchaseOrderId: string;
  purchaseOrderNo: string;
  bizType: string;
  relatedId: string;
  direction: string;
  entryAmount: number;
  occurredAt: string;
}

export interface SupplierReconciliationView {
  supplierId: string;
  supplierName: string;
  storeId: string;
  receiptAmount: number;
  returnAmount: number;
  netPayableAmount: number;
  entryCount: number;
  pendingDiscrepancyCount: number;
  reconciliationStatus: string;
}

export interface PurchaseCostCollectionView {
  purchaseOrderId: string;
  purchaseOrderNo: string;
  storeId: string;
  supplierId: string;
  basePurchaseCost: number;
  allocatedExpense: number;
  totalCollectedCost: number;
  unitCollectedCost: number;
  netReceiptQty: number;
  costLotCount: number;
}

export interface WmsLinkageView {
  storeId: string;
  warehouseCount: number;
  activeWaveCount: number;
  lockedBatchCount: number;
  cycleCountTaskCount: number;
  reverseInboundCount: number;
  omsSyncStatus: string;
  erpSyncStatus: string;
  tmsHandoverStatus: string;
}

export interface FinanceBill {
  financeBillId: string;
  storeId: string;
  billType: string;
  periodStart: string;
  periodEnd: string;
  incomeAmount: number;
  costAmount: number;
  grossProfit: number;
  billStatus: string;
  createdAt: string;
}

export interface SettlementRecord {
  settlementRecordId: string;
  financeBillId: string;
  settlementType: string;
  settlementAmount: number;
  settlementStatus: string;
  settledAt: string | null;
  createdAt: string;
}

export interface FinanceBillDetailView {
  bill: FinanceBill;
  settlements: SettlementRecord[];
  splitStatus: string;
  invoiceCheckStatus: string;
}

export interface FinanceBillReconcileView {
  financeBillId: string;
  billStatus: string;
  discrepancyCount: number;
  invoiceCheckStatus: string;
}

export interface ReceivableLedgerEntry {
  receivableLedgerId: string;
  receivableNo: string;
  storeId: string;
  orderId: string;
  platformOrderId: string;
  customerName: string;
  receivableAmount: number;
  collectedAmount: number;
  outstandingAmount: number;
  receivableStatus: string;
  occurredAt: string;
}

export interface CustomerPaymentRecord {
  paymentRecordId: string;
  paymentNo: string;
  storeId: string;
  orderId: string;
  platformOrderId: string;
  customerName: string;
  paymentChannel: string;
  paymentAmount: number;
  paymentStatus: string;
  operatorId: string;
  receivedAt: string;
  remark: string | null;
}

export interface FinanceVoucher {
  voucherId: string;
  voucherNo: string;
  storeId: string;
  referenceType: string;
  referenceId: string;
  voucherType: string;
  voucherAmount: number;
  archiveStatus: string;
  operatorId: string;
  archivedAt: string;
  remark: string | null;
}

export interface FinanceInvoice {
  invoiceId: string;
  invoiceNo: string;
  storeId: string;
  referenceType: string;
  referenceId: string;
  sourceInvoiceId: string | null;
  invoiceTitle: string;
  invoiceTaxNo: string;
  invoiceAmount: number;
  invoiceStatus: string;
  archiveStatus: string;
  redFlush: boolean;
  operatorId: string;
  issuedAt: string;
  updatedAt: string;
  remark: string | null;
}

export interface FinancePeriodClosingRecord {
  closingRecordId: string;
  closingNo: string;
  storeId: string;
  periodStart: string;
  periodEnd: string;
  closingStatus: string;
  operatorId: string;
  linkedFinanceBillCount: number;
  archivedVoucherCount: number;
  closedAt: string;
  remark: string | null;
}

export interface ExpenseBreakdownView {
  expenseType: string;
  expenseAmount: number;
  allocationCount: number;
}

export interface ProfitStatementView {
  storeId: string;
  shopName: string;
  periodStart: string;
  periodEnd: string;
  salesOrderCount: number;
  salesIncomeAmount: number;
  estimatedCostAmount: number;
  grossProfitAmount: number;
  allocatedExpenseAmount: number;
  netProfitAmount: number;
  profitMarginRate: number;
  targetProfitThreshold: number;
  thresholdStatus: string;
  outstandingReceivableAmount: number;
  expenseBreakdown: ExpenseBreakdownView[];
}

export interface StoreProfitReportView {
  storeId: string;
  shopName: string;
  periodStart: string;
  periodEnd: string;
  salesOrderCount: number;
  salesIncomeAmount: number;
  grossProfitAmount: number;
  allocatedExpenseAmount: number;
  netProfitAmount: number;
  profitMarginRate: number;
  targetProfitThreshold: number;
  thresholdStatus: string;
  outstandingReceivableAmount: number;
}

export interface FinanceGeneralLedgerView {
  storeId: string;
  shopName: string;
  periodStart: string;
  periodEnd: string;
  financeBillCount: number;
  settledFinanceBillCount: number;
  salesIncomeAmount: number;
  receivableCollectedAmount: number;
  outstandingReceivableAmount: number;
  payableNetAmount: number;
  settlementAmount: number;
  archivedVoucherAmount: number;
  issuedInvoiceAmount: number;
  redFlushInvoiceAmount: number;
  pendingVoucherCount: number;
  pendingInvoiceCount: number;
  closingStatus: string;
}

export interface FinanceClosingCheckItemView {
  checkCode: string;
  checkName: string;
  checkStatus: string;
  pendingCount: number;
  detail: string;
}

export interface FinanceClosingCheckView {
  storeId: string;
  shopName: string;
  periodStart: string;
  periodEnd: string;
  readyToClose: boolean;
  blockingIssueCount: number;
  checkItems: FinanceClosingCheckItemView[];
}

export interface GenerateFinanceBillPayload {
  storeId: string;
  billType: string;
  periodStart: string;
  periodEnd: string;
}

export interface SettleFinanceBillPayload {
  settlementType?: string;
}

export interface RecordCustomerPaymentPayload {
  orderId: string;
  paymentChannel?: string;
  paymentAmount: number;
  remark?: string;
}

export interface ArchiveFinanceVoucherPayload {
  storeId: string;
  referenceType: string;
  referenceId: string;
  voucherType: string;
  voucherAmount: number;
  remark?: string;
}

export interface IssueFinanceInvoicePayload {
  storeId: string;
  referenceType: string;
  referenceId: string;
  invoiceTitle: string;
  invoiceTaxNo: string;
  invoiceAmount: number;
  remark?: string;
}

export interface UpdateFinanceInvoicePayload {
  remark?: string;
}

export interface CloseFinancePeriodPayload {
  storeId: string;
  periodStart: string;
  periodEnd: string;
  remark?: string;
}

export interface ExternalIntegrationPreferences {
  tenantId: string;
  featureFlags: Record<string, boolean>;
  options: ExternalIntegrationOption[];
}

export interface ObservabilityEndpoint {
  configured: boolean;
  host: string;
  maskedEndpoint: string;
  sourceType: string;
  sourceName: string;
  defaultValue: boolean;
  trusted: boolean;
  status: string;
  probeReachable: boolean;
  probeDetail: string;
}

export interface ExternalSystemConnectivitySnapshot {
  systemCode: string;
  provider: string;
  protocol: string;
  configured: boolean;
  reachable: boolean;
  host: string;
  maskedTarget: string;
  sourceType: string;
  sourceName: string;
  defaultValue: boolean;
  trusted: boolean;
  status: string;
  detail: string;
}

export interface ExternalIntegrationConnectivity {
  ready: boolean;
  configuredCount: number;
  reachableCount: number;
  erp?: ExternalSystemConnectivitySnapshot;
  wms?: ExternalSystemConnectivitySnapshot;
  messaging?: ExternalSystemConnectivitySnapshot;
  bi?: ExternalSystemConnectivitySnapshot;
  routing?: ExternalSystemConnectivitySnapshot;
}

export interface DeliveryControlDiagnosticSnapshot {
  controlCode: string;
  value: string;
  configured: boolean;
  sourceType: string;
  sourceName: string;
  defaultValue: boolean;
  trusted: boolean;
  status: string;
  detail: string;
}

export interface DeliveryAssetSnapshot {
  assetCode: string;
  path: string;
  present: boolean;
  ready: boolean;
  detail: string;
}

export interface DeliveryPipelineObservabilityView {
  ready: boolean;
  repository: string;
  registry: string;
  releaseKeyControl: DeliveryControlDiagnosticSnapshot;
  registryAuthControl: DeliveryControlDiagnosticSnapshot;
  githubPublishingControl: DeliveryControlDiagnosticSnapshot;
  canaryControl: DeliveryControlDiagnosticSnapshot;
  githubProbe: ObservabilityEndpoint;
  registryProbe: ObservabilityEndpoint;
}

export interface DeliveryPipelineSnapshot {
  ready: boolean;
  repository: string;
  registry: string;
  imageRepository: string;
  releaseKeyConfigured: boolean;
  canaryEnabled: boolean;
  releaseKeyControl: DeliveryControlDiagnosticSnapshot;
  registryAuthControl: DeliveryControlDiagnosticSnapshot;
  githubPublishingControl: DeliveryControlDiagnosticSnapshot;
  canaryControl: DeliveryControlDiagnosticSnapshot;
  workflowAsset: DeliveryAssetSnapshot;
  standardSaasComposeAsset: DeliveryAssetSnapshot;
  privateComposeAsset: DeliveryAssetSnapshot;
  githubProbe: ExternalSystemConnectivitySnapshot;
  registryProbe: ExternalSystemConnectivitySnapshot;
}

export interface ObservabilityStackView {
  logAggregation: ObservabilityEndpoint;
  trace: ObservabilityEndpoint;
  alertRouter: ObservabilityEndpoint;
  dashboard: ObservabilityEndpoint;
}

export interface ObservabilityStackReadinessSnapshot {
  ready: boolean;
  configuredCount: number;
  reachableCount: number;
  logAggregation: ExternalSystemConnectivitySnapshot;
  trace: ExternalSystemConnectivitySnapshot;
  alertRouter: ExternalSystemConnectivitySnapshot;
  dashboard: ExternalSystemConnectivitySnapshot;
  releaseGateStatus: string;
  releaseGateDetail: string;
}

export interface AuditTraceabilitySummary {
  auditLogCount: number;
  traceableAuditLogCount: number;
  latestTraceIds: string[];
}

export interface ExternalErpPlatformView {
  provider: string;
  configured: boolean;
  host: string;
  maskedEndpoint: string;
  partySyncEnabled: boolean;
  orderSyncMode: string;
  ledgerMappingCount: number;
  catalogExportEnabled: boolean;
}

export interface ExternalWmsPlatformView {
  provider: string;
  configured: boolean;
  host: string;
  maskedEndpoint: string;
  facilityCount: number;
  stockSyncMode: string;
  outboundFlow: string;
  batchTrackingEnabled: boolean;
}

export interface ExternalMessagingPlatformView {
  provider: string;
  configured: boolean;
  host: string;
  maskedEndpoint: string;
  virtualHost: string;
  exchange: string;
  queueCount: number;
  callbackBridgeEnabled: boolean;
  deadLetterEnabled: boolean;
  callbackWorkerEnabled: boolean;
  callbackWorkerProvider: string;
  callbackWorkerMaskedEndpoint: string;
  callbackWorkerConsumerGroup: string;
  callbackWorkerReady: boolean;
  callbackWorkerMissingParts: string[];
  callbackWorkerProbeReachable: boolean;
  callbackWorkerProbeDetail: string;
}

export interface ExternalBiPlatformView {
  provider: string;
  configured: boolean;
  host: string;
  maskedEndpoint: string;
  dashboardCount: number;
  datasetCount: number;
  embedEnabled: boolean;
}

export interface ExternalRoutingView {
  provider: string;
  configured: boolean;
  fallbackEnabled: boolean;
  profile: string;
  host: string;
  maskedEndpoint: string;
}

export interface DeliveryEndpointSnapshot {
  mode: string;
  configured: boolean;
  protocol: string;
  reachable: boolean;
  host: string;
  maskedBaseUrl: string;
  verifiedAt: string;
  verificationFresh: boolean;
  verificationStatus: string;
  verificationAgeDays: number;
  detail: string;
}

export interface DualDeliveryAcceptanceSnapshot {
  ready: boolean;
  standardSaas: DeliveryEndpointSnapshot;
  privateDeployment: DeliveryEndpointSnapshot;
}

export interface ObservabilityOverview {
  tenantId: string;
  auditLogCount: number;
  failedNotificationCount: number;
  openPlatformCallLogCount: number;
  rejectedOpenPlatformCallCount: number;
  liveRiskEventCount: number;
  activeAlertCount: number;
  latestTraceIds: string[];
  configuredGatewayCount: number;
  enabledGatewayCount: number;
  mockGatewayCount: number;
  realGatewayCount: number;
  prometheusEndpointEnabled: boolean;
  supportedDeploymentModes: string[];
  requiredExternalSystemCount: number;
  readyExternalSystemCount: number;
  observabilityStackReady: boolean;
  deliveryPipelineReady: boolean;
  dualDeliveryAcceptanceReady: boolean;
  deliveryRepository: string;
  deliveryPipeline: DeliveryPipelineObservabilityView;
  observabilityStack: ObservabilityStackView;
  externalIntegrationConnectivity: ExternalIntegrationConnectivity;
  externalErpPlatform: ExternalErpPlatformView;
  externalWmsPlatform: ExternalWmsPlatformView;
  externalMessagingPlatform: ExternalMessagingPlatformView;
  externalBiPlatform: ExternalBiPlatformView;
  externalRouting: ExternalRoutingView;
}

export interface AdminTenantOverview {
  tenantId: string;
  tenantCode: string;
  tenantName: string;
  tenantStatus: string;
  ownerName: string;
  mobile: string;
  subscriptionId: string | null;
  planCode: string | null;
  planName: string | null;
  subscriptionStatus: string | null;
  seatCount: number;
  quotaCount: number;
  totalQuotaLimit: number;
  totalUsedAmount: number;
  trialEndAt: string | null;
  createdAt: string | null;
}

export interface FeatureToggleUpdatePayload {
  featureFlags: Record<string, boolean>;
}

export interface SubscriptionAutomationSummary {
  autoRenewedCount: number;
  autoSuspendedCount: number;
  autoChargeFailedCount: number;
  autoRenewedTenantIds: string[];
  autoSuspendedTenantIds: string[];
  autoChargeFailedTenantIds: string[];
  executedAt: string;
}

export interface ReleaseEvidenceSummary {
  exportTaskCount: number;
  cleanupTaskCount: number;
  complianceAcceptanceCount: number;
  billingOrderCount: number;
}

export interface ReleaseChecklistItemView {
  itemCode: string;
  status: string;
  evidenceCount: number;
  detail: string;
}

export interface ReleaseReadinessView {
  tenantId: string;
  conclusion: string;
  blockingReasons: string[];
  evidenceSummary: ReleaseEvidenceSummary;
  checklistItems: ReleaseChecklistItemView[];
}

export interface DeliveryReadinessView {
  tenantId: string;
  pipeline: DeliveryPipelineSnapshot;
  externalIntegrations: ExternalIntegrationConnectivity;
  acceptance: DualDeliveryAcceptanceSnapshot;
}

export interface ObservabilityReadinessView {
  tenantId: string;
  stack: ObservabilityStackReadinessSnapshot;
  auditTraceability: AuditTraceabilitySummary;
  blockingReasons: string[];
}

export interface ComplianceDocument {
  documentCode: string;
  version: string;
  title: string;
  content: string;
}

export interface ComplianceDocumentPublishPayload {
  version: string;
  title: string;
  content: string;
}

export interface ComplianceAcceptance {
  documentCode: string;
  version: string;
  operatorId: string;
  acceptedAt: string;
}

export interface AdminComplianceDocumentView {
  documentCode: string;
  version: string;
  title: string;
  updatedAt: string;
  acceptedCurrentVersionTenantCount: number;
  pendingReacceptanceTenantCount: number;
  neverAcceptedTenantCount: number;
}

export interface AdminComplianceAcceptanceView {
  tenantId: string;
  tenantName: string;
  tenantStatus: string;
  documentCode: string;
  currentVersion: string;
  latestAcceptedVersion: string | null;
  latestAcceptedBy: string | null;
  latestAcceptedAt: string | null;
  acceptanceStatus: string;
  reacceptRequired: boolean;
}

export interface TenantDataExportTaskSummary {
  exportTaskId: string;
  scopeCode: string;
  scopeName: string;
  requestedBy: string;
  status: string;
  fileName: string;
  contentType: string;
  maskingStrategy: string;
  timeRangeStart: string | null;
  timeRangeEnd: string | null;
  downloadExpiresAt: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface TenantCleanupTaskView {
  cleanupTaskId: string;
  status: string;
  reason: string;
  cleanupScopes: string[];
  requestedBy: string;
  reviewedBy: string | null;
  executedBy: string | null;
  impactSummary: Record<string, number>;
  resultSummary: Record<string, number>;
  createdAt: string;
  executedAt: string | null;
  completedAt: string | null;
}

export interface Organization {
  id: string;
  tenantId: string;
  organizationName: string;
  status: string;
  createdAt: string;
}

export interface OrganizationMember {
  memberId: string;
  organizationId: string;
  userId: string;
  userName: string;
  mobile: string;
  roleCode: string;
  status: string;
  joinedAt: string;
}

export interface AuditLogRecord {
  tenantId: string;
  operatorId: string;
  operatorType: string;
  actionType: string;
  targetType: string;
  targetId: string;
  traceId: string;
  createdAt: string;
}

export interface MemberTagItemView {
  memberTagId: string;
  tagCode: string;
  tagName: string;
  sourceType: string;
}

export interface MemberTag {
  memberTagId: string;
  storeId: string;
  memberId: string;
  tagCode: string;
  tagName: string;
  sourceType: string;
  createdAt: string;
}

export interface MemberView {
  memberId: string;
  storeId: string;
  customerId: string;
  nickname: string;
  levelCode: string;
  totalOrderCount: number;
  totalPaidAmount: number;
  lastOrderAt: string | null;
  points: number;
  growthValue: number;
  lifecycleStage: string;
  tags: MemberTagItemView[];
}

export interface MemberPageResult {
  list: MemberView[];
  page: number;
  pageSize: number;
  total: number;
}

export interface MemberCrmAnalysisView {
  totalMembers: number;
  strategicMembers: number;
  highValueMembers: number;
  dormantMembers: number;
  silentMembers: number;
  churnWarningMembers: number;
  autoTaggedMembers: number;
  activeRecallTaskCount: number;
}

export interface MemberDetailView {
  member: MemberView;
}

export interface SaveMemberCrmProfilePayload {
  customerName?: string;
  primaryContactName?: string;
  primaryContactMobile?: string;
  wechatId?: string;
  sourceChannel?: string;
  sourceDetail?: string;
  customerTier?: string;
}

export interface MemberCrmProfileView {
  memberId: string;
  storeId: string;
  customerId: string;
  customerName: string;
  primaryContactName: string | null;
  primaryContactMobile: string | null;
  wechatId: string | null;
  sourceChannel: string | null;
  sourceDetail: string | null;
  customerTier: string;
  updatedAt: string;
}

export interface SaveMemberTagPayload {
  tagCode: string;
  tagName: string;
  sourceType?: string;
}

export interface MemberTagSummaryView {
  tagCode: string;
  tagName: string;
  sourceType: string;
  memberCount: number;
}

export interface SaveMemberSegmentRulePayload {
  storeId: string;
  ruleName: string;
  lifecycleStage?: string;
  minTotalPaidAmount?: number;
  levelCode?: string;
  tagCode: string;
  tagName: string;
}

export interface MemberSegmentRuleView {
  ruleId: string;
  storeId: string;
  ruleName: string;
  lifecycleStage: string | null;
  minTotalPaidAmount: number | null;
  levelCode: string | null;
  tagCode: string;
  tagName: string;
  createdAt: string;
}

export interface MemberSegmentExecutionView {
  ruleId: string;
  ruleName: string;
  matchedMemberCount: number;
  matchedMemberIds: string[];
}

export interface MemberCrmLinkageView {
  memberId: string;
  storeId: string;
  customerId: string;
  customerName: string;
  primaryContactName: string | null;
  sourceChannel: string | null;
  customerTier: string;
  lifecycleStage: string;
  totalPaidAmount: number;
  lastOrderAt: string | null;
  relatedOrderCount: number;
  relatedAfterSaleCount: number;
  relatedTicketCount: number;
  relatedCampaignCount: number;
  activeTouchTaskCount: number;
  churnWarningLevel: string;
  tagCodes: string[];
}

export interface SaveMemberTouchTaskPayload {
  storeId: string;
  memberId: string;
  taskType: string;
  triggerType: string;
  campaignId?: string;
  channel: string;
  scheduledAt?: string;
  remark?: string;
}

export interface MemberTouchTaskView {
  taskId: string;
  storeId: string;
  memberId: string;
  taskType: string;
  triggerType: string;
  campaignId: string | null;
  channel: string;
  status: string;
  scheduledAt: string;
  remark: string | null;
  createdAt: string;
}

export interface ExportMemberGroupPayload {
  storeId?: string;
  levelCode?: string;
  tagCode?: string;
  lifecycleStage?: string;
}

export interface MemberGroupExportView {
  exportId: string;
  storeId: string | null;
  levelCode: string | null;
  tagCode: string | null;
  lifecycleStage: string | null;
  totalMembers: number;
  memberIds: string[];
}

export interface PluginApp {
  appId: string;
  organizationId: string;
  appName: string;
  appType: string;
  permissionScope: string[];
  accessKey: string;
  secretMasked: string;
  status: string;
  createdAt: string;
}

export interface IntegrationCredentialView {
  credentialId: string;
  pluginAppId: string;
  credentialType: string;
  accessKey: string;
  secretMasked: string;
  issuedAt: string;
  expiresAt: string | null;
  active: boolean;
}

export interface IssuedIntegrationCredential {
  appId: string;
  credentialType: string;
  accessKey: string;
  secret: string;
  secretMasked: string;
  expiresAt: string | null;
}

export interface WebhookSubscription {
  subscriptionId: string;
  organizationId: string;
  eventCode: string;
  callbackUrl: string;
  secretToken: string;
  status: string;
  createdAt: string;
}

export interface OpenPlatformCallLog {
  logId: string;
  tenantId: string;
  organizationId: string;
  appId: string | null;
  subscriptionId: string | null;
  requestId: string;
  endpoint: string;
  direction: string;
  sourceModule: string;
  resultStatus: string;
  signatureVerified: boolean;
  replayed: boolean;
  traceId: string;
  message: string;
  createdAt: string;
}

export interface OpenPlatformOverviewView {
  appCount: number;
  activeAppCount: number;
  disabledAppCount: number;
  webhookCount: number;
  enabledWebhookCount: number;
  disabledWebhookCount: number;
  activeCredentialCount: number;
  revokedCredentialCount: number;
  expiringCredentialCount: number;
  totalCallLogCount: number;
}

export interface PluginGovernanceEntryView {
  appId: string;
  organizationId: string;
  appName: string;
  appType: string;
  appStatus: string;
  permissionScope: string[];
  credentialStatus: string;
  webhookStatus: string;
  organizationWebhookCount: number;
  enabledWebhookCount: number;
  recentCallCount: number;
  rejectedCallCount: number;
  lastResultStatus: string | null;
  lastActivityAt: string | null;
  governanceRiskLevel: string;
  issues: string[];
  recommendedAction: string;
}

export interface PluginGovernanceOverviewView {
  totalApps: number;
  activeApps: number;
  riskyApps: number;
  appsMissingCredentials: number;
  appsWithRejectedTraffic: number;
  organizationsWithoutWebhooks: number;
  recommendedActions: string[];
  entries: PluginGovernanceEntryView[];
}

export interface WebhookOrchestrationView {
  subscriptionId: string;
  organizationId: string;
  eventCode: string;
  callbackUrl: string;
  status: string;
  callbackAttemptCount: number;
  acceptedCallbackCount: number;
  rejectedCallbackCount: number;
  replayRejectedCount: number;
  signatureRejectedCount: number;
  lastResultStatus: string;
  lastTraceId: string;
  createdAt: string;
}

export interface IntegrationAuditOverviewView {
  totalCallLogCount: number;
  externalAuthorizedCount: number;
  callbackAcceptedCount: number;
  callbackRejectedCount: number;
  scopeRejectedCount: number;
  replayRejectedCount: number;
  signatureRejectedCount: number;
  disabledAppCount: number;
  revokedCredentialCount: number;
  expiringCredentialCount: number;
}

export interface CustomerConversation {
  conversationId: string;
  storeId: string;
  platformType: string;
  platformConversationId: string;
  customerId: string;
  conversationStatus: string;
  riskFlag: boolean;
  lastMessageAt: string;
  createdAt: string;
}

export interface ConversationMessage {
  messageId: string;
  conversationId: string;
  senderType: string;
  messageType: string;
  contentText: string;
  aiGeneratedFlag: boolean;
  riskFlag: boolean;
  createdAt: string;
}

export interface ReplySuggestionView {
  conversationId: string;
  messageId: string;
  suggestedReply: string;
  sourceType: string;
  autoSendEligible: boolean;
  matchedQuestion: string | null;
  confidenceLevel: string;
  handoffReason: string | null;
  riskLabels: string[];
  recommendedAction: string;
  knowledgeSourceSummary: string;
}

export interface CustomerServiceTicket {
  ticketId: string;
  storeId: string;
  orderId: string;
  customerId: string;
  ticketStatus: string;
  riskFlag: boolean;
  aiReplySuggestion: string;
  createdAt: string;
}

export interface TicketSatisfactionView {
  ticketId: string;
  storeId: string;
  score: number;
  comment: string | null;
  ratedAt: string;
}

export interface SaveTicketSatisfactionPayload {
  score: number;
  comment?: string;
}

export interface TicketSlaOverviewView {
  totalTicketCount: number;
  withinSlaCount: number;
  overdueTicketCount: number;
  averageFirstResponseMinutes: number;
  satisfactionCount: number;
  averageSatisfactionScore: number;
}

export interface AfterSaleRecord {
  afterSaleId: string;
  orderId: string;
  afterSaleType: string;
  reasonText: string | null;
  status: string;
  evidenceBlob: string | null;
  createdAt: string;
}

export interface CreateAfterSalePayload {
  orderId: string;
  afterSaleType: string;
  reasonText?: string;
  evidenceBlob?: string;
}

export interface SupportSession {
  id: string;
  tenantId: string;
  requesterId: string;
  reason: string;
  approver: string | null;
  expiresAt: string | null;
  status: string;
  approvalRemark: string | null;
  active: boolean;
  createdAt: string;
}

export interface NotificationTask {
  notificationTaskId: string;
  tenantId: string;
  notifyType: string;
  templateCode: string;
  targetReceiver: string;
  sendStatus: string;
  retryCount: number;
  payloadJson: string;
  priority: string;
  scheduledAt: string | null;
  batchId: string | null;
  deadLetterReason: string | null;
  createdAt: string;
}

export interface NotificationGatewayProviderView {
  gatewayCode: string;
  notifyType: string;
  enabled: boolean;
  mockMode: boolean;
  receiptSupported: boolean;
  endpoint: string;
  routedTaskCount: number;
  deliveredTaskCount: number;
  failedTaskCount: number;
  description: string;
}

export interface NotificationChannelGatewayStatView {
  notifyType: string;
  totalTaskCount: number;
  deliveredTaskCount: number;
  sentTaskCount: number;
  scheduledTaskCount: number;
  failedTaskCount: number;
  deadLetterTaskCount: number;
}

export interface NotificationGatewayOverviewView {
  totalTaskCount: number;
  sentTaskCount: number;
  deliveredTaskCount: number;
  scheduledTaskCount: number;
  failedTaskCount: number;
  deadLetterTaskCount: number;
  urgentTaskCount: number;
  receiptPendingTaskCount: number;
  configuredGatewayCount: number;
  enabledGatewayCount: number;
  mockGatewayCount: number;
  providerStats: NotificationGatewayProviderView[];
  channelStats: NotificationChannelGatewayStatView[];
}

export interface NotificationGatewayChannelBindingView {
  notifyType: string;
  gatewayCode: string;
  enabled: boolean;
  mockMode: boolean;
  receiptSupported: boolean;
  endpoint: string;
  description: string;
}

export interface NotificationGatewayProviderConfigView {
  gatewayCode: string;
  notifyType: string;
  enabled: boolean;
  mockMode: boolean;
  receiptSupported: boolean;
  endpoint: string;
  receiptCallbackPath: string;
  description: string;
}

export interface NotificationGatewayConfigView {
  requireConfiguredGateway: boolean;
  channelBindings: NotificationGatewayChannelBindingView[];
  providers: NotificationGatewayProviderConfigView[];
}

export interface UpdateNotificationGatewayChannelBindingPayload {
  notifyType: string;
  gatewayCode: string;
}

export interface UpdateNotificationGatewayProviderPayload {
  gatewayCode: string;
  notifyType: string;
  enabled: boolean;
  mockMode: boolean;
  receiptSupported: boolean;
  endpoint: string;
  receiptCallbackPath: string;
  description: string;
}

export interface UpdateNotificationGatewayConfigPayload {
  requireConfiguredGateway: boolean;
  channelBindings: UpdateNotificationGatewayChannelBindingPayload[];
  providers: UpdateNotificationGatewayProviderPayload[];
}

export interface NotificationTemplateView {
  notificationTemplateId: string;
  tenantId: string;
  templateCode: string;
  templateName: string;
  notifyType: string;
  titleTemplate: string;
  contentTemplate: string;
  enabled: boolean;
  createdAt: string;
}

export interface UpdateNotificationTemplatePayload {
  templateName: string;
  titleTemplate: string;
  contentTemplate: string;
  enabled: boolean;
}

export interface ReplayNotificationTaskPayload {
  targetReceiver: string | null;
  payloadJson: string | null;
  scheduledAt: string | null;
}

export interface ApprovalInstance {
  approvalId: string;
  tenantId: string;
  approvalType: string;
  relatedType: string;
  relatedId: string;
  status: string;
  currentHandlerId: string | null;
  remark: string | null;
  resultRemark: string | null;
  createdAt: string;
}

export interface ApprovalStageDefinition {
  stageCode: string;
  handlerId: string;
  stageOrder: number;
}

export interface ApprovalTemplateView {
  templateId: string;
  tenantId: string;
  templateCode: string;
  templateName: string;
  approvalType: string;
  enabled: boolean;
  stages: ApprovalStageDefinition[];
  createdAt: string;
}

export interface GovernanceApprovalRequestView {
  requestId: string;
  tenantId: string;
  approvalType: string;
  templateId: string;
  templateCode: string;
  documentNo: string;
  subject: string;
  amount: number;
  counterparty: string;
  requesterId: string;
  requestStatus: string;
  currentStageCode: string | null;
  currentStageOrder: number | null;
  currentHandlerId: string | null;
  remark: string | null;
  createdAt: string;
}

export interface LivePlan {
  livePlanId: string;
  storeId: string;
  liveAccountId: string | null;
  planName: string;
  planStatus: string;
  scheduledStartAt: string;
  scheduledEndAt: string | null;
  anchorProfileName: string | null;
  createdAt: string;
}

export interface LiveSession {
  liveSessionId: string;
  livePlanId: string;
  tenantId: string;
  storeId: string;
  liveAccountId: string;
  sessionStatus: string;
  roomId: string | null;
  actualStartAt: string | null;
  actualEndAt: string | null;
  errorMessage: string | null;
  controlMode: string | null;
  currentScene: string | null;
  takeoverStatus: string | null;
  takeoverOperator: string | null;
  promiseAuditStatus: string | null;
  promiseAuditRemark: string | null;
  createdAt: string;
}

export interface LiveAccountGovernanceView {
  liveAccountId: string;
  organizationId: string;
  accountName: string;
  authStatus: string;
  occupied: boolean;
  occupiedSessionId: string | null;
  runningSessionCount: number;
  queuedPlanCount: number;
  expiringSoon: boolean;
  governanceRiskLevel: string;
  expiresAt: string | null;
}

export interface LiveConcurrencyCheckView {
  livePlanId: string;
  allowed: boolean;
  accountOccupied: boolean;
  tenantQuotaExceeded: boolean;
  tenantRunningCount: number;
  tenantQuotaLimit: number;
  runtimeReady: boolean;
  callbackReady: boolean;
  runtimeProvider: string;
  reason: string | null;
}

export interface LiveRunningSessionView {
  liveSessionId: string;
  livePlanId: string;
  storeId: string;
  liveAccountId: string;
  sessionStatus: string;
  roomId: string | null;
  actualStartAt: string | null;
  errorMessage: string | null;
}

export interface LiveAccountOccupancyView {
  liveAccountId: string;
  accountName: string;
  occupiedSessionId: string | null;
  livePlanId: string | null;
  storeId: string | null;
  actualStartAt: string | null;
}

export interface LiveConcurrencyOverviewView {
  tenantId: string;
  runningSessionCount: number;
  tenantQuotaLimit: number;
  remainingQuota: number;
  runningSessions: LiveRunningSessionView[];
  occupiedAccounts: LiveAccountOccupancyView[];
}

export interface LiveConcurrencyQueueView {
  livePlanId: string;
  planName: string;
  liveAccountId: string | null;
  blockedType: string;
  blockedReason: string;
  scheduledStartAt: string | null;
}

export interface LiveRiskEventView {
  eventCode: string;
  severity: string;
  liveSessionId: string | null;
  livePlanId: string | null;
  liveAccountId: string | null;
  detail: string;
  occurredAt: string;
}

export interface LiveSpecialAnalysisView {
  liveAccountCount: number;
  occupiedAccountCount: number;
  queuedPlanCount: number;
  openRiskEventCount: number;
  manualTakeoverSessionCount: number;
  promiseRejectedSessionCount: number;
  strongControlSessionCount: number;
  failedSessionCount: number;
  runtimeGateBlockedQueueCount: number;
  runtimeReady: boolean;
  callbackReady: boolean;
  runtimeProvider: string;
}

export interface LiveSpecialAnalysisDrilldownView {
  highRiskSessionIds: string[];
  failedSessionIds: string[];
  manualTakeoverSessionIds: string[];
  promiseRejectedSessionIds: string[];
  queuedPlanIds: string[];
  blockedTypes: string[];
  riskEventCodes: string[];
}

export interface LiveRiskRecoveryPlanView {
  entityType: string;
  entityId: string;
  liveSessionId: string | null;
  livePlanId: string | null;
  riskCode: string;
  riskLevel: string;
  recoveryAction: string;
  requiresManualReview: boolean;
  blockingReason: string | null;
}

export interface LiveSessionStatusView {
  liveSessionId: string;
  livePlanId: string;
  planStatus: string;
  sessionStatus: string;
  storeId: string;
  liveAccountId: string;
  liveAccountName: string | null;
  roomId: string | null;
  actualStartAt: string | null;
  actualEndAt: string | null;
  durationSeconds: number;
  accountOccupied: boolean;
  tenantQuotaLimit: number;
  tenantRemainingQuota: number;
  currentProductId: string | null;
  currentProductTitle: string | null;
  currentProductSourceType: string | null;
  activeScriptVersion: string | null;
  currentScriptSnippet: string | null;
  controlMode: string | null;
  currentScene: string | null;
  takeoverStatus: string | null;
  takeoverOperator: string | null;
  promiseAuditStatus: string | null;
  promiseAuditRemark: string | null;
  runtimeReady: boolean;
  callbackReady: boolean;
  strictRuntimeGateEnabled: boolean;
  strictCallbackGateEnabled: boolean;
  runtimeProvider: string;
  runtimeProviderEndpoint: string | null;
  callbackEndpoint: string | null;
  readinessMessage: string | null;
  errorMessage: string | null;
}

export interface LiveControlModeOperationView {
  liveSessionId: string;
  controlMode: string;
  effectiveAt: string;
}

export interface LiveCommitmentWhitelistConfigView {
  livePlanId: string;
  whitelistPhrases: string[];
  manualConfirmPhrases: string[];
  prohibitedPhrases: string[];
  updatedAt: string | null;
}

export interface LiveInteractionReplyView {
  interactionEventId: string;
  replyStatus: string;
  replyText: string;
  commitmentAuditStatus: string | null;
  knowledgeSourceSummary: string;
}

export interface DueLivePlanFailureView {
  livePlanId: string;
  planName: string;
  reason: string;
}

export interface RunDueLivePlansResultView {
  scannedCount: number;
  startedCount: number;
  failedCount: number;
  startedSessions: LiveSession[];
  failures: DueLivePlanFailureView[];
}

export interface SkipCurrentProductResultView {
  liveSessionId: string;
  livePlanId: string;
  skippedScriptSnippet: string | null;
  nextScriptSnippet: string | null;
  activeScriptVersion: string;
}

export interface SimulateLiveCallbackPayload {
  eventType: string;
  errorMessage?: string | null;
}

export interface StrongControlPayload {
  controlMode: string;
  reason?: string | null;
}

export interface EnableControlModePayload {
  controlMode: string;
  operatorId?: string | null;
  reason?: string | null;
}

export interface SwitchLiveScenePayload {
  targetScene: string;
  reason?: string | null;
}

export interface ManualTakeoverPayload {
  takeoverOperator: string;
  reason?: string | null;
}

export interface PromiseAuditPayload {
  promiseText: string;
  riskLevel: string;
  decision: string;
  remark?: string | null;
}

export interface ConfigureCommitmentWhitelistPayload {
  whitelistPhrases: string[];
  manualConfirmPhrases: string[];
  prohibitedPhrases: string[];
}

export interface InteractionReplyPayload {
  interactionEventId: string;
  customerQuestion: string;
  draftReplyText?: string | null;
  knowledgeSourceSummary?: string | null;
  commitmentType?: string | null;
}

export interface IntegrationReadinessDescriptor {
  label: string;
  tone: 'neutral' | 'warn' | 'success';
  detail: string;
}
