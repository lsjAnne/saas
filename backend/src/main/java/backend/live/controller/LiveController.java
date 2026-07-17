package backend.live.controller;

import backend.auth.security.AuthPermissionCodes;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.live.application.LiveApplicationService;
import backend.live.application.LiveApplicationService.CommitmentDecisionCommand;
import backend.live.application.LiveApplicationService.ConfigureCommitmentWhitelistCommand;
import backend.live.application.LiveApplicationService.EnableControlModeCommand;
import backend.live.application.LiveApplicationService.InteractionReplyCommand;
import backend.live.application.LiveApplicationService.LiveCommitmentAuditOperationView;
import backend.live.application.LiveApplicationService.LiveCommitmentWhitelistConfigView;
import backend.live.application.LiveApplicationService.LiveControlModeOperationView;
import backend.live.application.LiveApplicationService.LiveInteractionReplyView;
import backend.live.application.LiveApplicationService.LiveInteractionTransferOperationView;
import backend.live.application.LiveApplicationService.InteractionTransferCommand;
import backend.live.application.LiveService.*;
import backend.live.model.LivePlan;
import backend.live.model.LiveScript;
import backend.live.model.LiveSession;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.LIVE_MANAGE)
public class LiveController {

    private final LiveApplicationService liveApplicationService;

    public LiveController(LiveApplicationService liveApplicationService) {
        this.liveApplicationService = liveApplicationService;
    }

    @GetMapping("/api/live-plans")
    public ApiResponse<List<LivePlan>> listLivePlans() {
        return ApiResponse.success(
                liveApplicationService.listLivePlans(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans")
    public ApiResponse<LivePlan> createLivePlan(@Valid @RequestBody CreateLivePlanRequest request) {
        return ApiResponse.success(
                liveApplicationService.createLivePlan(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-plans/{id}")
    public ApiResponse<LivePlan> getLivePlan(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.getLivePlan(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-plans/{id}/product-pool")
    public ApiResponse<List<LiveProductPoolItemView>> getLiveProductPool(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.getLiveProductPool(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-plans/{id}/products")
    public ApiResponse<List<LiveBoundProductItemView>> listBoundLiveProducts(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.listBoundLiveProducts(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/products/bind")
    public ApiResponse<LiveBoundProductItemView> bindLiveProduct(@PathVariable String id,
                                                                 @Valid @RequestBody BindLiveProductRequest request) {
        return ApiResponse.success(
                liveApplicationService.bindLiveProduct(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @DeleteMapping("/api/live-plans/{id}/products/{itemId}")
    public ApiResponse<LiveBoundProductItemView> removeBoundLiveProduct(@PathVariable String id,
                                                                        @PathVariable String itemId) {
        return ApiResponse.success(
                liveApplicationService.removeBoundLiveProduct(TenantAccessSupport.requiredTenantId(), id, itemId),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/live-plans/{id}")
    public ApiResponse<LivePlan> updateLivePlan(@PathVariable String id,
                                                @Valid @RequestBody UpdateLivePlanRequest request) {
        return ApiResponse.success(
                liveApplicationService.updateLivePlan(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/generate-script")
    public ApiResponse<LiveScript> generateScript(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.generateScript(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/review-script")
    public ApiResponse<LiveScriptReviewView> reviewScript(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.reviewScript(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/configure-commitment-whitelist")
    public ApiResponse<LiveCommitmentWhitelistConfigView> configureCommitmentWhitelist(@PathVariable String id,
                                                                                       @RequestBody ConfigureCommitmentWhitelistRequest request) {
        return ApiResponse.success(
                liveApplicationService.configureCommitmentWhitelist(
                        TenantAccessSupport.requiredTenantId(),
                        id,
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/publish")
    public ApiResponse<LivePlan> publishLivePlan(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.publishLivePlan(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/schedule")
    public ApiResponse<LivePlan> scheduleLivePlan(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.scheduleLivePlan(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/duplicate")
    public ApiResponse<LivePlan> duplicateLivePlan(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.duplicateLivePlan(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/cancel")
    public ApiResponse<LivePlan> cancelLivePlan(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.cancelLivePlan(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-accounts")
    public ApiResponse<List<LiveAccountView>> listLiveAccounts() {
        return ApiResponse.success(
                liveApplicationService.listLiveAccounts(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-accounts/governance")
    public ApiResponse<List<LiveAccountGovernanceView>> listLiveAccountGovernance() {
        return ApiResponse.success(
                liveApplicationService.listLiveAccountGovernance(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/validate-concurrency")
    public ApiResponse<LiveConcurrencyCheckView> validateConcurrency(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.validateConcurrency(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/start")
    public ApiResponse<LiveSession> startLive(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.startLive(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/pause")
    public ApiResponse<LiveSession> pauseLive(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.pauseLive(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/resume")
    public ApiResponse<LiveSession> resumeLive(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.resumeLive(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/{id}/stop")
    public ApiResponse<LiveSession> stopLive(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.stopLive(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-sessions")
    public ApiResponse<List<LiveSession>> listLiveSessions() {
        return ApiResponse.success(
                liveApplicationService.listLiveSessions(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-sessions/{id}/status")
    public ApiResponse<LiveSessionStatusView> getLiveSessionStatus(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.getLiveSessionStatus(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-sessions/concurrency-overview")
    public ApiResponse<LiveConcurrencyOverviewView> getConcurrencyOverview() {
        return ApiResponse.success(
                liveApplicationService.getConcurrencyOverview(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-concurrency-queues")
    public ApiResponse<List<LiveConcurrencyQueueView>> listConcurrencyQueues() {
        return ApiResponse.success(
                liveApplicationService.listConcurrencyQueues(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-risk-events")
    public ApiResponse<List<LiveRiskEventView>> listLiveRiskEvents() {
        return ApiResponse.success(
                liveApplicationService.listLiveRiskEvents(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-sessions/{id}/risk-events")
    public ApiResponse<List<LiveRiskEventView>> listLiveSessionRiskEvents(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.listLiveSessionRiskEvents(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-special-analysis")
    public ApiResponse<LiveSpecialAnalysisView> getLiveSpecialAnalysis() {
        return ApiResponse.success(
                liveApplicationService.getLiveSpecialAnalysis(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-risk-recovery-plans")
    public ApiResponse<List<LiveRiskRecoveryPlanView>> listRiskRecoveryPlans() {
        return ApiResponse.success(
                liveApplicationService.listRiskRecoveryPlans(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/live-special-analysis/drilldown")
    public ApiResponse<LiveSpecialAnalysisDrilldownView> getLiveSpecialAnalysisDrilldown() {
        return ApiResponse.success(
                liveApplicationService.getLiveSpecialAnalysisDrilldown(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-plans/run-due")
    public ApiResponse<RunDueLivePlansResultView> runDueLivePlans() {
        return ApiResponse.success(
                liveApplicationService.runDueScheduledLives(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-sessions/{id}/simulate-callback")
    public ApiResponse<LiveSession> simulateCallback(@PathVariable String id,
                                                     @Valid @RequestBody SimulateLiveCallbackRequest request) {
        return ApiResponse.success(
                liveApplicationService.simulateCallback(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-sessions/{id}/skip-current-product")
    public ApiResponse<SkipCurrentProductResultView> skipCurrentProduct(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.skipCurrentProduct(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-sessions/{id}/strong-control")
    public ApiResponse<LiveSession> strongControl(@PathVariable String id,
                                                  @Valid @RequestBody StrongControlRequest request) {
        return ApiResponse.success(
                liveApplicationService.strongControl(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-sessions/{id}/enable-control-mode")
    public ApiResponse<LiveControlModeOperationView> enableControlMode(@PathVariable String id,
                                                                       @Valid @RequestBody EnableControlModeRequest request) {
        return ApiResponse.success(
                liveApplicationService.enableControlMode(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-sessions/{id}/switch-scene")
    public ApiResponse<LiveSession> switchScene(@PathVariable String id,
                                                @Valid @RequestBody SwitchSceneRequest request) {
        return ApiResponse.success(
                liveApplicationService.switchScene(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-sessions/{id}/manual-takeover")
    public ApiResponse<LiveSession> manualTakeover(@PathVariable String id,
                                                   @Valid @RequestBody ManualTakeoverRequest request) {
        return ApiResponse.success(
                liveApplicationService.manualTakeover(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-sessions/{id}/resume-system-mode")
    public ApiResponse<LiveControlModeOperationView> resumeSystemMode(@PathVariable String id) {
        return ApiResponse.success(
                liveApplicationService.resumeSystemMode(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-sessions/{id}/interaction/transfer")
    public ApiResponse<LiveInteractionTransferOperationView> transferInteraction(@PathVariable String id,
                                                                                 @Valid @RequestBody InteractionTransferRequest request) {
        return ApiResponse.success(
                liveApplicationService.transferInteraction(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-sessions/{id}/interaction/reply")
    public ApiResponse<LiveInteractionReplyView> replyInteraction(@PathVariable String id,
                                                                  @Valid @RequestBody InteractionReplyRequest request) {
        return ApiResponse.success(
                liveApplicationService.replyInteraction(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-sessions/{id}/promise-audit")
    public ApiResponse<LiveSession> promiseAudit(@PathVariable String id,
                                                 @Valid @RequestBody PromiseAuditRequest request) {
        return ApiResponse.success(
                liveApplicationService.promiseAudit(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-sessions/{id}/commitments/confirm")
    public ApiResponse<LiveCommitmentAuditOperationView> confirmCommitment(@PathVariable String id,
                                                                           @Valid @RequestBody CommitmentDecisionRequest request) {
        return ApiResponse.success(
                liveApplicationService.confirmCommitment(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/live-sessions/{id}/commitments/reject")
    public ApiResponse<LiveCommitmentAuditOperationView> rejectCommitment(@PathVariable String id,
                                                                          @Valid @RequestBody CommitmentDecisionRequest request) {
        return ApiResponse.success(
                liveApplicationService.rejectCommitment(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }
}

record EnableControlModeRequest(
        @NotBlank(message = "controlMode is required")
        String controlMode,
        String operatorId,
        String reason
) {
    EnableControlModeCommand toCommand() {
        return new EnableControlModeCommand(controlMode, operatorId, reason);
    }
}

record ConfigureCommitmentWhitelistRequest(
        List<String> whitelistPhrases,
        List<String> manualConfirmPhrases,
        List<String> prohibitedPhrases
) {
    ConfigureCommitmentWhitelistCommand toCommand() {
        return new ConfigureCommitmentWhitelistCommand(
                whitelistPhrases,
                manualConfirmPhrases,
                prohibitedPhrases
        );
    }
}

record InteractionTransferRequest(
        @NotBlank(message = "interactionEventId is required")
        String interactionEventId,
        @NotBlank(message = "transferType is required")
        String transferType,
        @NotBlank(message = "targetUserId is required")
        String targetUserId,
        String transferReason
) {
    InteractionTransferCommand toCommand() {
        return new InteractionTransferCommand(interactionEventId, transferType, targetUserId, transferReason);
    }
}

record InteractionReplyRequest(
        @NotBlank(message = "interactionEventId is required")
        String interactionEventId,
        @NotBlank(message = "customerQuestion is required")
        String customerQuestion,
        String draftReplyText,
        String knowledgeSourceSummary,
        String commitmentType
) {
    InteractionReplyCommand toCommand() {
        return new InteractionReplyCommand(
                interactionEventId,
                customerQuestion,
                draftReplyText,
                knowledgeSourceSummary,
                commitmentType
        );
    }
}

record CommitmentDecisionRequest(
        @NotBlank(message = "interactionEventId is required")
        String interactionEventId,
        @NotBlank(message = "commitmentType is required")
        String commitmentType,
        @NotBlank(message = "commitmentLevel is required")
        String commitmentLevel,
        @NotBlank(message = "commitmentText is required")
        String commitmentText,
        String approvedBy,
        String approvalId,
        String remark
) {
    CommitmentDecisionCommand toCommand() {
        return new CommitmentDecisionCommand(
                interactionEventId,
                commitmentType,
                commitmentLevel,
                commitmentText,
                approvedBy,
                approvalId,
                remark
        );
    }
}
