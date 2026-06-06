package com.dianshang.platform.support.controller;

import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.support.application.SupportSessionService;
import com.dianshang.platform.support.dto.CreateSupportSessionRequest;
import com.dianshang.platform.support.model.SupportSession;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequirePlatformRoles;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/support-sessions")
@RequirePlatformRoles({"platform-support", "platform-admin", "platform-ops"})
public class SupportSessionAdminController {

    private final SupportSessionService supportSessionService;

    public SupportSessionAdminController(SupportSessionService supportSessionService) {
        this.supportSessionService = supportSessionService;
    }

    @PostMapping
    public ApiResponse<SupportSession> create(@Valid @RequestBody CreateSupportSessionRequest request) {
        return ApiResponse.success(supportSessionService.create(request), TraceIdHolder.get());
    }

    @GetMapping
    public ApiResponse<List<SupportSession>> list() {
        return ApiResponse.success(supportSessionService.findAll(), TraceIdHolder.get());
    }

    @PostMapping("/{id}/close")
    public ApiResponse<SupportSession> close(@PathVariable String id) {
        return ApiResponse.success(supportSessionService.close(id), TraceIdHolder.get());
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<SupportSession> approve(@PathVariable String id,
                                               @Valid @RequestBody ApproveSupportSessionRequest request) {
        return ApiResponse.success(
                supportSessionService.approve(id, request.approver(), request.expiresAt(), request.approvalRemark()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<SupportSession> reject(@PathVariable String id,
                                              @Valid @RequestBody RejectSupportSessionRequest request) {
        return ApiResponse.success(
                supportSessionService.reject(id, request.approver(), request.approvalRemark()),
                TraceIdHolder.get()
        );
    }
}

@RestController
@RequestMapping("/api/support-sessions")
@RequireTenantPermission(AuthPermissionCodes.APPROVAL_MANAGE)
class SupportSessionController {

    private final SupportSessionService supportSessionService;

    SupportSessionController(SupportSessionService supportSessionService) {
        this.supportSessionService = supportSessionService;
    }

    @PostMapping("/apply")
    ApiResponse<SupportSession> apply(@Valid @RequestBody ApplySupportSessionRequest request) {
        return ApiResponse.success(
                supportSessionService.apply(
                        TenantAccessSupport.requiredTenantId(),
                        TenantAccessSupport.requiredContext().operatorId(),
                        request.reason(),
                        request.requestedExpiresAt()
                ),
                TraceIdHolder.get()
        );
    }

    @GetMapping
    ApiResponse<List<SupportSession>> list() {
        return ApiResponse.success(
                supportSessionService.findByTenantId(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }
}

record ApplySupportSessionRequest(
        @NotBlank(message = "reason不能为空")
        String reason,
        @NotBlank(message = "requestedExpiresAt不能为空")
        String requestedExpiresAt
) {
}

record ApproveSupportSessionRequest(
        @NotBlank(message = "approver不能为空")
        String approver,
        @NotBlank(message = "expiresAt不能为空")
        String expiresAt,
        String approvalRemark
) {
}

record RejectSupportSessionRequest(
        @NotBlank(message = "approver不能为空")
        String approver,
        String approvalRemark
) {
}
