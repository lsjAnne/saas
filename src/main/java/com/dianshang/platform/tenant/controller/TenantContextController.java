package com.dianshang.platform.tenant.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.saas.application.SaasTenantService;
import com.dianshang.platform.saas.model.TenantProfile;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.TenantContext;
import com.dianshang.platform.tenant.dto.TenantContextView;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
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
