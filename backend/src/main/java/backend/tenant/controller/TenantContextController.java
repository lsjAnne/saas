package backend.tenant.controller;

import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.auth.security.AuthPermissionCodes;
import backend.saas.application.SaasTenantService;
import backend.saas.model.TenantProfile;
import backend.tenant.context.TenantContext;
import backend.tenant.context.TenantAccessSupport;
import backend.tenant.dto.TenantContextView;
import backend.auth.security.RequireTenantPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequireTenantPermission(AuthPermissionCodes.TENANT_CONTEXT_READ)
public class TenantContextController {

    private final SaasTenantService saasTenantService;

    public TenantContextController(SaasTenantService saasTenantService) {
        this.saasTenantService = saasTenantService;
    }

    @GetMapping("/api/tenant/context")
    public ApiResponse<TenantContextView> getContext() {
        TenantContext context = TenantAccessSupport.requiredContext();
        TenantProfile profile = saasTenantService.getTenantProfile(context.tenantId());
        return ApiResponse.success(new TenantContextView(
                profile.tenantId(),
                profile.tenantCode(),
                profile.tenantName(),
                profile.tenantStatus(),
                context.operatorId(),
                context.operatorType(),
                profile.defaultOrganizationId()
        ), TraceIdHolder.get());
    }
}

