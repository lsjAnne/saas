package backend.openplatform.controller;

import backend.auth.security.AuthPermissionCodes;
import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.openplatform.application.OpenPlatformApplicationService;
import backend.openplatform.application.OpenPlatformApplicationService.CreatePluginAppCommand;
import backend.openplatform.application.OpenPlatformApplicationService.CreateWebhookSubscriptionCommand;
import backend.openplatform.application.OpenPlatformApplicationService.IntegrationCredentialView;
import backend.openplatform.application.OpenPlatformApplicationService.IntegrationAuditOverviewView;
import backend.openplatform.application.OpenPlatformApplicationService.IssuedIntegrationCredential;
import backend.openplatform.application.OpenPlatformApplicationService.OpenPlatformOverviewView;
import backend.openplatform.application.OpenPlatformApplicationService.PluginGovernanceOverviewView;
import backend.openplatform.application.OpenPlatformApplicationService.WebhookOrchestrationView;
import backend.openplatform.model.OpenPlatformCallLog;
import backend.openplatform.model.PluginApp;
import backend.openplatform.model.WebhookSubscription;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.OPENPLATFORM_MANAGE)
public class OpenPlatformController {

    private final OpenPlatformApplicationService openPlatformApplicationService;

    public OpenPlatformController(OpenPlatformApplicationService openPlatformApplicationService) {
        this.openPlatformApplicationService = openPlatformApplicationService;
    }

    @GetMapping("/api/open/apps")
    public ApiResponse<List<PluginApp>> listApps() {
        return ApiResponse.success(
                openPlatformApplicationService.listApps(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/open/apps")
    public ApiResponse<PluginApp> createApp(@Valid @RequestBody SavePluginAppRequest request) {
        return ApiResponse.success(
                openPlatformApplicationService.createApp(TenantAccessSupport.requiredTenantId(), request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/open/apps/{id}/disable")
    public ApiResponse<PluginApp> disableApp(@PathVariable String id) {
        return ApiResponse.success(
                openPlatformApplicationService.disableApp(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/open/apps/{id}/enable")
    public ApiResponse<PluginApp> enableApp(@PathVariable String id) {
        return ApiResponse.success(
                openPlatformApplicationService.enableApp(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/open/apps/{id}/credentials/refresh")
    @RequireTenantPermission(value = AuthPermissionCodes.OPENPLATFORM_MANAGE, requireSecondaryConfirmation = true)
    public ApiResponse<IssuedIntegrationCredential> refreshCredential(@PathVariable String id) {
        return ApiResponse.success(
                openPlatformApplicationService.refreshCredential(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/apps/{id}/credentials")
    public ApiResponse<List<IntegrationCredentialView>> listCredentials(@PathVariable String id) {
        return ApiResponse.success(
                openPlatformApplicationService.listCredentials(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/open/apps/{id}/credentials/revoke")
    @RequireTenantPermission(value = AuthPermissionCodes.OPENPLATFORM_MANAGE, requireSecondaryConfirmation = true)
    public ApiResponse<IntegrationCredentialView> revokeCredential(@PathVariable String id) {
        return ApiResponse.success(
                openPlatformApplicationService.revokeCredential(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/webhooks")
    public ApiResponse<List<WebhookSubscription>> listWebhooks() {
        return ApiResponse.success(
                openPlatformApplicationService.listWebhooks(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/open/webhooks")
    public ApiResponse<WebhookSubscription> createWebhook(@Valid @RequestBody SaveWebhookSubscriptionRequest request) {
        return ApiResponse.success(
                openPlatformApplicationService.createWebhook(TenantAccessSupport.requiredTenantId(), request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/open/webhooks/{id}/disable")
    public ApiResponse<WebhookSubscription> disableWebhook(@PathVariable String id) {
        return ApiResponse.success(
                openPlatformApplicationService.disableWebhook(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/open/webhooks/{id}/enable")
    public ApiResponse<WebhookSubscription> enableWebhook(@PathVariable String id) {
        return ApiResponse.success(
                openPlatformApplicationService.enableWebhook(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/open/webhooks/{id}/secret/rotate")
    @RequireTenantPermission(value = AuthPermissionCodes.OPENPLATFORM_MANAGE, requireSecondaryConfirmation = true)
    public ApiResponse<WebhookSubscription> rotateWebhookSecret(@PathVariable String id) {
        return ApiResponse.success(
                openPlatformApplicationService.rotateWebhookSecret(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/logs")
    public ApiResponse<List<OpenPlatformCallLog>> listCallLogs() {
        return ApiResponse.success(
                openPlatformApplicationService.listCallLogs(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/webhook-orchestrations")
    public ApiResponse<List<WebhookOrchestrationView>> listWebhookOrchestrations() {
        return ApiResponse.success(
                openPlatformApplicationService.listWebhookOrchestrations(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/integration-audit")
    public ApiResponse<IntegrationAuditOverviewView> getIntegrationAudit() {
        return ApiResponse.success(
                openPlatformApplicationService.getIntegrationAudit(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/overview")
    public ApiResponse<OpenPlatformOverviewView> getOverview() {
        return ApiResponse.success(
                openPlatformApplicationService.getOverview(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/open/plugin-governance")
    public ApiResponse<PluginGovernanceOverviewView> getPluginGovernance() {
        return ApiResponse.success(
                openPlatformApplicationService.getPluginGovernance(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }
}

record SavePluginAppRequest(
        @NotBlank(message = "organizationId is required")
        String organizationId,
        @NotBlank(message = "appName is required")
        String appName,
        @NotBlank(message = "appType is required")
        String appType,
        @NotEmpty(message = "permissionScope is required")
        List<String> permissionScope
) {
    CreatePluginAppCommand toCommand() {
        return new CreatePluginAppCommand(organizationId, appName, appType, permissionScope);
    }
}

record SaveWebhookSubscriptionRequest(
        @NotBlank(message = "organizationId is required")
        String organizationId,
        @NotBlank(message = "eventCode is required")
        String eventCode,
        @NotBlank(message = "callbackUrl is required")
        String callbackUrl
) {
    CreateWebhookSubscriptionCommand toCommand() {
        return new CreateWebhookSubscriptionCommand(organizationId, eventCode, callbackUrl);
    }
}
