package backend.rule.controller;

import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.auth.security.AuthPermissionCodes;
import backend.rule.application.RuleService;
import backend.rule.application.RuleService.FallbackSimulationRequest;
import backend.rule.application.RuleService.FallbackSimulationView;
import backend.rule.application.RuleService.RuleGovernanceOverviewView;
import backend.rule.application.RuleService.RuleOrchestrationView;
import backend.rule.application.RuleService.SaveRuleRequest;
import backend.rule.model.AutomationRule;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.RULE_MANAGE)
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping("/api/rules")
    public ApiResponse<List<AutomationRule>> listRules() {
        return ApiResponse.success(
                ruleService.listRules(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/rules/governance-overview")
    public ApiResponse<RuleGovernanceOverviewView> getGovernanceOverview() {
        return ApiResponse.success(
                ruleService.getGovernanceOverview(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/rules/orchestrations")
    public ApiResponse<List<RuleOrchestrationView>> listOrchestrations() {
        return ApiResponse.success(
                ruleService.listOrchestrations(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/rules")
    public ApiResponse<AutomationRule> createRule(@Valid @RequestBody SaveRuleRequest request) {
        return ApiResponse.success(
                ruleService.createRule(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/rules/{id}")
    public ApiResponse<AutomationRule> updateRule(@PathVariable String id,
                                                  @Valid @RequestBody SaveRuleRequest request) {
        return ApiResponse.success(
                ruleService.updateRule(TenantAccessSupport.requiredTenantId(), id, request),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/rules/{id}/enable")
    public ApiResponse<AutomationRule> enableRule(@PathVariable String id) {
        return ApiResponse.success(
                ruleService.enableRule(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/rules/{id}/disable")
    public ApiResponse<AutomationRule> disableRule(@PathVariable String id) {
        return ApiResponse.success(
                ruleService.disableRule(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/rules/fallback-simulations")
    public ApiResponse<FallbackSimulationView> simulateFallback(@Valid @RequestBody FallbackSimulationRequest request) {
        return ApiResponse.success(
                ruleService.simulateFallback(TenantAccessSupport.requiredTenantId(), request),
                TraceIdHolder.get()
        );
    }
}

