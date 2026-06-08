package com.dianshang.platform.rule.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.rule.application.RuleService;
import com.dianshang.platform.rule.application.RuleService.FallbackSimulationRequest;
import com.dianshang.platform.rule.application.RuleService.FallbackSimulationView;
import com.dianshang.platform.rule.application.RuleService.RuleGovernanceOverviewView;
import com.dianshang.platform.rule.application.RuleService.RuleOrchestrationView;
import com.dianshang.platform.rule.application.RuleService.SaveRuleRequest;
import com.dianshang.platform.rule.model.AutomationRule;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
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
