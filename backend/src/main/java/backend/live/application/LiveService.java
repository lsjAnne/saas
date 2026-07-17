package backend.live.application;

import backend.live.model.LivePlan;
import backend.live.model.LiveScript;
import backend.live.model.LiveSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Deprecated(forRemoval = false)
public class LiveService {

    private final LiveApplicationService liveApplicationService;

    public LiveService(LiveApplicationService liveApplicationService) {
        this.liveApplicationService = liveApplicationService;
    }

    public List<LivePlan> listLivePlans(String tenantId) {
        return liveApplicationService.listLivePlans(tenantId);
    }

    public List<LiveSession> listLiveSessions(String tenantId) {
        return liveApplicationService.listLiveSessions(tenantId);
    }

    public LiveSessionStatusView getLiveSessionStatus(String tenantId, String liveSessionId) {
        return liveApplicationService.getLiveSessionStatus(tenantId, liveSessionId);
    }

    public LiveConcurrencyOverviewView getConcurrencyOverview(String tenantId) {
        return liveApplicationService.getConcurrencyOverview(tenantId);
    }

    public List<LiveConcurrencyQueueView> listConcurrencyQueues(String tenantId) {
        return liveApplicationService.listConcurrencyQueues(tenantId);
    }

    public List<LiveRiskEventView> listLiveRiskEvents(String tenantId) {
        return liveApplicationService.listLiveRiskEvents(tenantId);
    }

    public LiveSpecialAnalysisView getLiveSpecialAnalysis(String tenantId) {
        return liveApplicationService.getLiveSpecialAnalysis(tenantId);
    }

    public List<LiveRiskRecoveryPlanView> listRiskRecoveryPlans(String tenantId) {
        return liveApplicationService.listRiskRecoveryPlans(tenantId);
    }

    public LiveSpecialAnalysisDrilldownView getLiveSpecialAnalysisDrilldown(String tenantId) {
        return liveApplicationService.getLiveSpecialAnalysisDrilldown(tenantId);
    }

    public LivePlan createLivePlan(String tenantId, CreateLivePlanRequest request) {
        return liveApplicationService.createLivePlan(tenantId, request);
    }

    public LivePlan updateLivePlan(String tenantId, String livePlanId, UpdateLivePlanRequest request) {
        return liveApplicationService.updateLivePlan(tenantId, livePlanId, request);
    }

    public LivePlan getLivePlan(String tenantId, String livePlanId) {
        return liveApplicationService.getLivePlan(tenantId, livePlanId);
    }

    public List<LiveProductPoolItemView> getLiveProductPool(String tenantId, String livePlanId) {
        return liveApplicationService.getLiveProductPool(tenantId, livePlanId);
    }

    public List<LiveBoundProductItemView> listBoundLiveProducts(String tenantId, String livePlanId) {
        return liveApplicationService.listBoundLiveProducts(tenantId, livePlanId);
    }

    public LiveBoundProductItemView bindLiveProduct(String tenantId, String livePlanId, BindLiveProductRequest request) {
        return liveApplicationService.bindLiveProduct(tenantId, livePlanId, request);
    }

    public LiveBoundProductItemView removeBoundLiveProduct(String tenantId, String livePlanId, String liveProductItemId) {
        return liveApplicationService.removeBoundLiveProduct(tenantId, livePlanId, liveProductItemId);
    }

    public List<LiveAccountView> listLiveAccounts(String tenantId) {
        return liveApplicationService.listLiveAccounts(tenantId);
    }

    public List<LiveAccountGovernanceView> listLiveAccountGovernance(String tenantId) {
        return liveApplicationService.listLiveAccountGovernance(tenantId);
    }

    public LiveScript generateScript(String tenantId, String livePlanId) {
        return liveApplicationService.generateScript(tenantId, livePlanId);
    }

    public LiveScriptReviewView reviewScript(String tenantId, String livePlanId) {
        return liveApplicationService.reviewScript(tenantId, livePlanId);
    }

    public LivePlan publishLivePlan(String tenantId, String livePlanId) {
        return liveApplicationService.publishLivePlan(tenantId, livePlanId);
    }

    public LivePlan scheduleLivePlan(String tenantId, String livePlanId) {
        return liveApplicationService.scheduleLivePlan(tenantId, livePlanId);
    }

    public LivePlan duplicateLivePlan(String tenantId, String livePlanId) {
        return liveApplicationService.duplicateLivePlan(tenantId, livePlanId);
    }

    public LivePlan cancelLivePlan(String tenantId, String livePlanId) {
        return liveApplicationService.cancelLivePlan(tenantId, livePlanId);
    }

    public LiveConcurrencyCheckView validateConcurrency(String tenantId, String livePlanId) {
        return liveApplicationService.validateConcurrency(tenantId, livePlanId);
    }

    public LiveSession startLive(String tenantId, String livePlanId) {
        return liveApplicationService.startLive(tenantId, livePlanId);
    }

    public LiveSession pauseLive(String tenantId, String livePlanId) {
        return liveApplicationService.pauseLive(tenantId, livePlanId);
    }

    public LiveSession resumeLive(String tenantId, String livePlanId) {
        return liveApplicationService.resumeLive(tenantId, livePlanId);
    }

    public LiveSession stopLive(String tenantId, String livePlanId) {
        return liveApplicationService.stopLive(tenantId, livePlanId);
    }

    public RunDueLivePlansResultView runDueScheduledLives(String tenantId) {
        return liveApplicationService.runDueScheduledLives(tenantId);
    }

    public LiveSession simulateCallback(String tenantId, String liveSessionId, SimulateLiveCallbackRequest request) {
        return liveApplicationService.simulateCallback(tenantId, liveSessionId, request);
    }

    public SkipCurrentProductResultView skipCurrentProduct(String tenantId, String liveSessionId) {
        return liveApplicationService.skipCurrentProduct(tenantId, liveSessionId);
    }

    public LiveSession strongControl(String tenantId, String liveSessionId, StrongControlRequest request) {
        return liveApplicationService.strongControl(tenantId, liveSessionId, request);
    }

    public LiveSession switchScene(String tenantId, String liveSessionId, SwitchSceneRequest request) {
        return liveApplicationService.switchScene(tenantId, liveSessionId, request);
    }

    public LiveSession manualTakeover(String tenantId, String liveSessionId, ManualTakeoverRequest request) {
        return liveApplicationService.manualTakeover(tenantId, liveSessionId, request);
    }

    public LiveSession promiseAudit(String tenantId, String liveSessionId, PromiseAuditRequest request) {
        return liveApplicationService.promiseAudit(tenantId, liveSessionId, request);
    }

    public LiveSession createRunningSessionSeed(String tenantId, String livePlanId, String liveAccountId) {
        return liveApplicationService.createRunningSessionSeed(tenantId, livePlanId, liveAccountId);
    }

    public void clear() {
        liveApplicationService.clear();
    }

    public record CreateLivePlanRequest(
            @NotBlank(message = "storeId娑撳秷鍏樻稉铏光敄")
            String storeId,
            String liveAccountId,
            @NotBlank(message = "planName娑撳秷鍏樻稉铏光敄")
            String planName,
            @NotNull(message = "scheduledStartAt娑撳秷鍏樻稉铏光敄")
            OffsetDateTime scheduledStartAt,
            OffsetDateTime scheduledEndAt,
            String anchorProfileName
    ) {
    }

    public record UpdateLivePlanRequest(
            String liveAccountId,
            @NotBlank(message = "planName娑撳秷鍏樻稉铏光敄")
            String planName,
            @NotNull(message = "scheduledStartAt娑撳秷鍏樻稉铏光敄")
            OffsetDateTime scheduledStartAt,
            OffsetDateTime scheduledEndAt,
            String anchorProfileName
    ) {
    }

    public record LiveAccountView(
            String liveAccountId,
            String organizationId,
            String channelType,
            String accountName,
            String authStatus,
            boolean occupied,
            String occupiedSessionId,
            OffsetDateTime expiresAt
    ) {
    }

    public record LiveAccountGovernanceView(
            String liveAccountId,
            String organizationId,
            String accountName,
            String authStatus,
            boolean occupied,
            String occupiedSessionId,
            int runningSessionCount,
            int queuedPlanCount,
            boolean expiringSoon,
            String governanceRiskLevel,
            OffsetDateTime expiresAt
    ) {
    }

    public record LiveConcurrencyCheckView(
            String livePlanId,
            boolean allowed,
            boolean accountOccupied,
            boolean tenantQuotaExceeded,
            int tenantRunningCount,
            int tenantQuotaLimit,
            boolean runtimeReady,
            boolean callbackReady,
            String runtimeProvider,
            String reason
    ) {
    }

    public record LiveScriptReviewView(
            String livePlanId,
            String scriptVersion,
            String scriptSnippet,
            boolean reviewed,
            boolean publishable
    ) {
    }

    public record LiveProductPoolItemView(
            String storeId,
            String candidateProductId,
            String productDraftId,
            String productId,
            String title,
            String sourceType,
            String category,
            String candidateStatus,
            String draftStatus,
            String productStatus,
            Integer healthScore,
            String riskLevel,
            String recommendationReason
    ) {
    }

    public record BindLiveProductRequest(
            String productId,
            String productDraftId,
            String candidateProductId,
            Integer sortOrder
    ) {
    }

    public record LiveBoundProductItemView(
            String liveProductItemId,
            String livePlanId,
            int sortOrder,
            OffsetDateTime boundAt,
            String storeId,
            String candidateProductId,
            String productDraftId,
            String productId,
            String title,
            String sourceType,
            String category,
            String candidateStatus,
            String draftStatus,
            String productStatus,
            Integer healthScore,
            String riskLevel,
            String recommendationReason
    ) {
    }

    public record LiveConcurrencyOverviewView(
            String tenantId,
            int runningSessionCount,
            int tenantQuotaLimit,
            int remainingQuota,
            List<LiveRunningSessionView> runningSessions,
            List<LiveAccountOccupancyView> occupiedAccounts
    ) {
    }

    public record LiveConcurrencyQueueView(
            String livePlanId,
            String planName,
            String liveAccountId,
            String blockedType,
            String blockedReason,
            OffsetDateTime scheduledStartAt
    ) {
    }

    public record LiveRiskEventView(
            String eventCode,
            String severity,
            String liveSessionId,
            String livePlanId,
            String liveAccountId,
            String detail,
            OffsetDateTime occurredAt
    ) {
    }

    public record LiveSpecialAnalysisView(
            int liveAccountCount,
            int occupiedAccountCount,
            int queuedPlanCount,
            int openRiskEventCount,
            int manualTakeoverSessionCount,
            int promiseRejectedSessionCount,
            int strongControlSessionCount,
            int failedSessionCount,
            int runtimeGateBlockedQueueCount,
            boolean runtimeReady,
            boolean callbackReady,
            String runtimeProvider
    ) {
    }

    public record LiveRiskRecoveryPlanView(
            String entityType,
            String entityId,
            String liveSessionId,
            String livePlanId,
            String riskCode,
            String riskLevel,
            String recoveryAction,
            boolean requiresManualReview,
            String blockingReason
    ) {
    }

    public record LiveSpecialAnalysisDrilldownView(
            List<String> highRiskSessionIds,
            List<String> failedSessionIds,
            List<String> manualTakeoverSessionIds,
            List<String> promiseRejectedSessionIds,
            List<String> queuedPlanIds,
            List<String> blockedTypes,
            List<String> riskEventCodes
    ) {
    }

    public record LiveSessionStatusView(
            String liveSessionId,
            String livePlanId,
            String planStatus,
            String sessionStatus,
            String storeId,
            String liveAccountId,
            String liveAccountName,
            String roomId,
            OffsetDateTime actualStartAt,
            OffsetDateTime actualEndAt,
            long durationSeconds,
            boolean accountOccupied,
            int tenantQuotaLimit,
            int tenantRemainingQuota,
            String currentProductId,
            String currentProductTitle,
            String currentProductSourceType,
            String activeScriptVersion,
            String currentScriptSnippet,
            String controlMode,
            String currentScene,
            String takeoverStatus,
            String takeoverOperator,
            String promiseAuditStatus,
            String promiseAuditRemark,
            boolean runtimeReady,
            boolean callbackReady,
            boolean strictRuntimeGateEnabled,
            boolean strictCallbackGateEnabled,
            String runtimeProvider,
            String runtimeProviderEndpoint,
            String callbackEndpoint,
            String readinessMessage,
            String errorMessage
    ) {
    }

    public record RunDueLivePlansResultView(
            int scannedCount,
            int startedCount,
            int failedCount,
            List<LiveSession> startedSessions,
            List<DueLivePlanFailureView> failures
    ) {
    }

    public record DueLivePlanFailureView(
            String livePlanId,
            String planName,
            String reason
    ) {
    }

    public record SkipCurrentProductResultView(
            String liveSessionId,
            String livePlanId,
            String skippedScriptSnippet,
            String nextScriptSnippet,
            String activeScriptVersion
    ) {
    }

    public record LiveRunningSessionView(
            String liveSessionId,
            String livePlanId,
            String storeId,
            String liveAccountId,
            String sessionStatus,
            String roomId,
            OffsetDateTime actualStartAt,
            String errorMessage
    ) {
    }

    public record LiveAccountOccupancyView(
            String liveAccountId,
            String accountName,
            String occupiedSessionId,
            String livePlanId,
            String storeId,
            OffsetDateTime actualStartAt
    ) {
    }

    public record SimulateLiveCallbackRequest(
            @NotBlank(message = "eventType娑撳秷鍏樻稉铏光敄")
            String eventType,
            String errorMessage
    ) {
    }

    public record StrongControlRequest(
            @NotBlank(message = "controlMode涓嶈兘涓虹┖")
            String controlMode,
            String reason
    ) {
    }

    public record SwitchSceneRequest(
            @NotBlank(message = "targetScene涓嶈兘涓虹┖")
            String targetScene,
            String reason
    ) {
    }

    public record ManualTakeoverRequest(
            @NotBlank(message = "takeoverOperator涓嶈兘涓虹┖")
            String takeoverOperator,
            String reason
    ) {
    }

    public record PromiseAuditRequest(
            @NotBlank(message = "promiseText涓嶈兘涓虹┖")
            String promiseText,
            @NotBlank(message = "riskLevel涓嶈兘涓虹┖")
            String riskLevel,
            @NotBlank(message = "decision涓嶈兘涓虹┖")
            String decision,
            String remark
    ) {
    }
}
