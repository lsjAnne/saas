package backend.auth.controller;

import backend.auth.application.AuthService;
import backend.auth.application.AuthService.AuthSecurityStatusView;
import backend.auth.application.AuthService.ChangePasswordRequest;
import backend.auth.application.AuthService.PasswordChangeResult;
import backend.auth.application.AuthService.SensitiveOperationConfirmationRequest;
import backend.auth.application.AuthService.SensitiveOperationConfirmationView;
import backend.auth.application.support.AuthUserAccessService.AccessCheckView;
import backend.auth.application.support.AuthUserAccessService.RolePermissionMatrixView;
import backend.auth.dto.LoginRequest;
import backend.auth.dto.LoginResponse;
import backend.auth.dto.LoginUserView;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), TraceIdHolder.get());
    }

    @PostMapping("/api/auth/logout")
    public ApiResponse<Map<String, Object>> logout() {
        return ApiResponse.success(authService.logout(), TraceIdHolder.get());
    }

    @PostMapping("/api/auth/password/change")
    public ApiResponse<PasswordChangeResult> changePassword(@Valid @RequestBody ChangePasswordHttpRequest request) {
        return ApiResponse.success(
                authService.changePassword(new ChangePasswordRequest(request.currentPassword(), request.newPassword())),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/me")
    public ApiResponse<LoginUserView> currentUser() {
        return ApiResponse.success(authService.currentUser(), TraceIdHolder.get());
    }

    @GetMapping("/api/auth/security-status")
    public ApiResponse<AuthSecurityStatusView> getSecurityStatus() {
        return ApiResponse.success(authService.getSecurityStatus(), TraceIdHolder.get());
    }

    @GetMapping("/api/auth/rbac/roles")
    public ApiResponse<List<RolePermissionMatrixView>> listTenantRolePermissionMatrix() {
        return ApiResponse.success(authService.listTenantRolePermissionMatrix(), TraceIdHolder.get());
    }

    @GetMapping("/api/auth/access-checks")
    public ApiResponse<AccessCheckView> checkAccess(@RequestParam @NotBlank String permissionCode) {
        return ApiResponse.success(authService.checkAccess(permissionCode), TraceIdHolder.get());
    }

    @PostMapping("/api/auth/sensitive-operation-confirmations")
    public ApiResponse<SensitiveOperationConfirmationView> confirmSensitiveOperation(
            @Valid @RequestBody SensitiveOperationConfirmationHttpRequest request
    ) {
        return ApiResponse.success(
                authService.confirmSensitiveOperation(
                        new SensitiveOperationConfirmationRequest(request.currentPassword(), request.permissionCode())
                ),
                TraceIdHolder.get()
        );
    }
}

record ChangePasswordHttpRequest(
        @NotBlank(message = "currentPassword is required")
        String currentPassword,
        @NotBlank(message = "newPassword is required")
        String newPassword
) {
}

record SensitiveOperationConfirmationHttpRequest(
        @NotBlank(message = "currentPassword is required")
        String currentPassword,
        @NotBlank(message = "permissionCode is required")
        String permissionCode
) {
}

