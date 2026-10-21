package backend.automation.controller;

import backend.automation.application.AutomationActionService;
import backend.automation.application.AutomationActionService.AutomationActionView;
import backend.automation.application.AutomationActionService.AutomationRuleView;
import backend.automation.application.AutomationActionService.SaveAutomationActionRequest;
import backend.automation.application.AutomationActionService.UpdateAutomationActionRequest;
import backend.automation.application.AutomationExecutionService;
import backend.automation.application.AutomationQueryService;
import backend.automation.application.AutomationQueryService.AutomationApprovalView;
import backend.automation.application.AutomationQueryService.AutomationExecutionView;
import backend.automation.application.AutomationQueryService.AutomationPage;
import backend.automation.application.AutomationQueryService.AutomationStatsView;
import backend.automation.application.AutomationScheduler;
import backend.automation.application.AutomationTriggerService;
import backend.automation.application.AutomationTriggerService.OrderSyncTriggerView;
import backend.automation.model.AutomationApproval;
import backend.auth.security.AuthPermissionCodes;
import backend.auth.security.RequireTenantPermission;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.tenant.context.TenantAccessSupport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.AUTOMATION_MANAGE)
public class AutomationController {

    private final AutomationActionService actionService;
    private final AutomationQueryService queryService;
    private final AutomationTriggerService triggerService;
    private final AutomationExecutionService executionService;
    private final ObjectProvider<AutomationScheduler> schedulerProvider;

    public AutomationController(AutomationActionService actionService,
                                AutomationQueryService queryService,
                                AutomationTriggerService triggerService,
                                AutomationExecutionService executionService,
                                ObjectProvider<AutomationScheduler> schedulerProvider) {
        this.actionService = actionService;
        this.queryService = queryService;
        this.triggerService = triggerService;
        this.executionService = executionService;
        this.schedulerProvider = schedulerProvider;
    }

    @GetMapping("/api/automation/actions")
    public ApiResponse<AutomationPage<AutomationActionView>> listActions(@RequestParam(required = false) String storeId,
                                                                        @RequestParam(required = false) String ruleId,
                                                                        @RequestParam(required = false) Boolean enabled,
                                                                        @RequestParam(required = false) Integer page,
                                                                        @RequestParam(required = false) Integer size) {
        List<AutomationActionView> filtered = actionService.listActions(
                TenantAccessSupport.requiredTenantId(), storeId, ruleId, enabled);
        return ApiResponse.success(paginate(filtered, page, size), TraceIdHolder.get());
    }

    @PostMapping("/api/automation/actions")
    public ApiResponse<AutomationActionView> createAction(@RequestBody SaveAutomationActionRequest request) {
        return ApiResponse.success(
                actionService.createAction(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get());
    }

    @PutMapping("/api/automation/actions/{actionId}")
    public ApiResponse<AutomationActionView> updateAction(@PathVariable String actionId,
                                                          @RequestBody UpdateAutomationActionRequest request) {
        return ApiResponse.success(
                actionService.updateAction(TenantAccessSupport.requiredTenantId(), actionId, request),
                TraceIdHolder.get());
    }

    @DeleteMapping("/api/automation/actions/{actionId}")
    public ApiResponse<Void> deleteAction(@PathVariable String actionId) {
        actionService.deleteAction(TenantAccessSupport.requiredTenantId(), actionId);
        return ApiResponse.success(null, TraceIdHolder.get());
    }

    @GetMapping("/api/automation/rules")
    public ApiResponse<List<AutomationRuleView>> listRules(@RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(
                actionService.listRules(TenantAccessSupport.requiredTenantId(), enabled),
                TraceIdHolder.get());
    }

    @GetMapping("/api/automation/executions")
    public ApiResponse<AutomationPage<AutomationExecutionView>> listExecutions(@RequestParam String storeId,
                                                                              @RequestParam(required = false) String status,
                                                                              @RequestParam(required = false) String actionType,
                                                                              @RequestParam(required = false) String triggerSource,
                                                                              @RequestParam(required = false) Integer page,
                                                                              @RequestParam(required = false) Integer size) {
        return ApiResponse.success(
                queryService.listExecutions(TenantAccessSupport.requiredTenantId(),
                        storeId, status, actionType, triggerSource, page, size),
                TraceIdHolder.get());
    }

    @GetMapping("/api/automation/executions/{executionId}")
    public ApiResponse<AutomationExecutionView> getExecution(@PathVariable String executionId) {
        return ApiResponse.success(
                queryService.getExecution(TenantAccessSupport.requiredTenantId(), executionId),
                TraceIdHolder.get());
    }

    @GetMapping("/api/automation/stats")
    public ApiResponse<AutomationStatsView> getStats(@RequestParam String storeId) {
        return ApiResponse.success(
                queryService.getStats(TenantAccessSupport.requiredTenantId(), storeId),
                TraceIdHolder.get());
    }

    @GetMapping("/api/automation/approvals")
    public ApiResponse<AutomationPage<AutomationApprovalView>> listApprovals(@RequestParam(required = false) String status,
                                                                            @RequestParam(required = false) String storeId,
                                                                            @RequestParam(required = false) Integer page,
                                                                            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(
                queryService.listApprovals(TenantAccessSupport.requiredTenantId(), status, storeId, page, size),
                TraceIdHolder.get());
    }

    @PostMapping("/api/automation/approvals/{taskId}/approve")
    public ApiResponse<AutomationApprovalView> approve(@PathVariable String taskId,
                                                      @RequestBody(required = false) ReviewDecisionRequest request) {
        String tenantId = TenantAccessSupport.requiredTenantId();
        AutomationApproval approval = executionService.approve(tenantId, taskId,
                request == null ? null : request.comment());
        return ApiResponse.success(toApprovalView(approval), TraceIdHolder.get());
    }

    @PostMapping("/api/automation/approvals/{taskId}/reject")
    public ApiResponse<AutomationApprovalView> reject(@PathVariable String taskId,
                                                     @RequestBody(required = false) ReviewDecisionRequest request) {
        String tenantId = TenantAccessSupport.requiredTenantId();
        AutomationApproval approval = executionService.reject(tenantId, taskId,
                request == null ? null : request.comment());
        return ApiResponse.success(toApprovalView(approval), TraceIdHolder.get());
    }

    @PostMapping("/api/automation/trigger/order-sync")
    public ApiResponse<OrderSyncTriggerView> triggerOrderSync(@RequestParam String storeId) {
        String tenantId = TenantAccessSupport.requiredTenantId();
        OrderSyncTriggerView view = triggerService.triggerOrderSync(tenantId, storeId);
        schedulerProvider.ifAvailable(scheduler -> scheduler.registerStore(tenantId, storeId));
        return ApiResponse.success(view, TraceIdHolder.get());
    }

    private AutomationApprovalView toApprovalView(AutomationApproval approval) {
        return new AutomationApprovalView(
                approval.taskId(), approval.executionId(), approval.storeId(),
                approval.targetType(), approval.targetId(), approval.reason(), approval.status(),
                approval.reviewedBy(), approval.reviewedAt(), approval.createdAt());
    }

    private static <T> AutomationPage<T> paginate(List<T> filtered, Integer page, Integer size) {
        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int fromIndex = Math.min((normalizedPage - 1) * normalizedSize, filtered.size());
        int toIndex = Math.min(fromIndex + normalizedSize, filtered.size());
        return new AutomationPage<>(new ArrayList<>(filtered.subList(fromIndex, toIndex)),
                normalizedPage, normalizedSize, filtered.size());
    }

    public record ReviewDecisionRequest(String comment) {
    }
}
