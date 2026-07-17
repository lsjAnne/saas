package backend.support.controller;

import backend.auth.security.AuthPermissionCodes;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.support.application.SupportSessionService;
import backend.support.dto.CreateSupportSessionRequest;
import backend.support.model.SupportSession;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequirePlatformRoles;
import backend.auth.security.RequireTenantPermission;
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
        @NotBlank(message = "reason涓嶈兘涓虹┖")
        String reason,
        @NotBlank(message = "requestedExpiresAt涓嶈兘涓虹┖")
        String requestedExpiresAt
) {
}

record ApproveSupportSessionRequest(
        @NotBlank(message = "approver涓嶈兘涓虹┖")
        String approver,
        @NotBlank(message = "expiresAt涓嶈兘涓虹┖")
        String expiresAt,
        String approvalRemark
) {
}

record RejectSupportSessionRequest(
        @NotBlank(message = "approver涓嶈兘涓虹┖")
        String approver,
        String approvalRemark
) {
}

