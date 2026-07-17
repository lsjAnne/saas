package backend.notification.controller;

import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.auth.security.AuthPermissionCodes;
import backend.notification.application.NotificationApplicationService;
import backend.notification.application.NotificationApplicationService.DeliveryReceiptCommand;
import backend.notification.application.NotificationApplicationService.NotificationGatewayConfigView;
import backend.notification.application.NotificationApplicationService.NotificationChannelGatewayStatView;
import backend.notification.application.NotificationApplicationService.NotificationGatewayOverviewView;
import backend.notification.application.NotificationApplicationService.UpdateNotificationGatewayChannelBindingRequest;
import backend.notification.application.NotificationApplicationService.UpdateNotificationGatewayConfigRequest;
import backend.notification.application.NotificationApplicationService.UpdateNotificationGatewayProviderRequest;
import backend.notification.application.NotificationService.BatchSendNotificationRequest;
import backend.notification.application.NotificationService.ReplayDeadLetterNotificationRequest;
import backend.notification.application.NotificationService.SendNotificationRequest;
import backend.notification.application.NotificationService.UpdateNotificationTemplateRequest;
import backend.notification.model.NotificationTask;
import backend.notification.model.NotificationTemplate;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequireTenantPermission(AuthPermissionCodes.NOTIFICATION_MANAGE)
public class NotificationController {

    private final NotificationApplicationService notificationApplicationService;

    public NotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @GetMapping("/api/notifications")
    public ApiResponse<List<NotificationTask>> listNotifications() {
        return ApiResponse.success(
                notificationApplicationService.listNotifications(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/notifications/gateway-overview")
    public ApiResponse<NotificationGatewayOverviewView> getGatewayOverview() {
        return ApiResponse.success(
                notificationApplicationService.getGatewayOverview(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/notification-gateway-config")
    public ApiResponse<NotificationGatewayConfigView> getGatewayConfig() {
        return ApiResponse.success(
                notificationApplicationService.getGatewayConfig(),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/notification-gateway-config")
    public ApiResponse<NotificationGatewayConfigView> updateGatewayConfig(@RequestBody SaveNotificationGatewayConfigRequest request) {
        return ApiResponse.success(
                notificationApplicationService.updateGatewayConfig(
                        TenantAccessSupport.requiredTenantId(),
                        request == null ? null : request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/notifications/send")
    public ApiResponse<NotificationTask> sendNotification(@Valid @RequestBody SaveNotificationRequest request) {
        return ApiResponse.success(
                notificationApplicationService.sendNotification(TenantAccessSupport.requiredTenantId(), request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/notifications/batch-send")
    public ApiResponse<List<NotificationTask>> batchSendNotifications(@Valid @RequestBody BatchSaveNotificationRequest request) {
        return ApiResponse.success(
                notificationApplicationService.batchSendNotifications(TenantAccessSupport.requiredTenantId(), request.toCommand()),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/notifications/{id}/retry")
    public ApiResponse<NotificationTask> retryNotification(@PathVariable String id) {
        return ApiResponse.success(
                notificationApplicationService.retryNotification(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/notifications/{id}/delivery-receipts")
    public ApiResponse<NotificationTask> recordDeliveryReceipt(@PathVariable String id,
                                                               @Valid @RequestBody SaveDeliveryReceiptRequest request) {
        return ApiResponse.success(
                notificationApplicationService.recordDeliveryReceipt(
                        TenantAccessSupport.requiredTenantId(),
                        id,
                        request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/notifications/{id}/dead-letter-replay")
    public ApiResponse<NotificationTask> replayDeadLetterNotification(@PathVariable String id,
                                                                      @RequestBody ReplayNotificationTaskRequest request) {
        return ApiResponse.success(
                notificationApplicationService.replayDeadLetterNotification(
                        TenantAccessSupport.requiredTenantId(),
                        id,
                        request == null ? null : request.toCommand()
                ),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/api/notifications/run-due")
    public ApiResponse<List<NotificationTask>> runDueNotifications() {
        return ApiResponse.success(
                notificationApplicationService.runDueNotifications(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/api/notification-templates")
    public ApiResponse<List<NotificationTemplate>> listTemplates() {
        return ApiResponse.success(
                notificationApplicationService.listTemplates(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/api/notification-templates/{id}")
    public ApiResponse<NotificationTemplate> updateTemplate(@PathVariable String id,
                                                            @Valid @RequestBody SaveNotificationTemplateRequest request) {
        return ApiResponse.success(
                notificationApplicationService.updateTemplate(TenantAccessSupport.requiredTenantId(), id, request.toCommand()),
                TraceIdHolder.get()
        );
    }
}

record ReplayNotificationTaskRequest(
        String targetReceiver,
        String payloadJson,
        OffsetDateTime scheduledAt
) {
    ReplayDeadLetterNotificationRequest toCommand() {
        return new ReplayDeadLetterNotificationRequest(targetReceiver, payloadJson, scheduledAt);
    }
}

record SaveDeliveryReceiptRequest(
        @NotBlank(message = "gatewayCode is required")
        String gatewayCode,
        @NotBlank(message = "deliveryStatus is required")
        String deliveryStatus,
        String providerMessageId,
        String receiptTraceId,
        String failureReason
) {
    DeliveryReceiptCommand toCommand() {
        return new DeliveryReceiptCommand(gatewayCode, deliveryStatus, providerMessageId, receiptTraceId, failureReason);
    }
}

record SaveNotificationRequest(
        @NotBlank(message = "notifyType is required")
        String notifyType,
        @NotBlank(message = "templateCode is required")
        String templateCode,
        @NotBlank(message = "targetReceiver is required")
        String targetReceiver,
        String payloadJson,
        String priority,
        OffsetDateTime scheduledAt
) {
    SendNotificationRequest toCommand() {
        return new SendNotificationRequest(notifyType, templateCode, targetReceiver, payloadJson, priority, scheduledAt);
    }
}

record BatchSaveNotificationRequest(
        @NotBlank(message = "notifyType is required")
        String notifyType,
        @NotBlank(message = "templateCode is required")
        String templateCode,
        @NotEmpty(message = "targetReceivers is required")
        List<String> targetReceivers,
        String payloadJson,
        String priority,
        OffsetDateTime scheduledAt,
        String batchId
) {
    BatchSendNotificationRequest toCommand() {
        return new BatchSendNotificationRequest(notifyType, templateCode, targetReceivers, payloadJson, priority, scheduledAt, batchId);
    }
}

record SaveNotificationTemplateRequest(
        @NotBlank(message = "templateName is required")
        String templateName,
        @NotBlank(message = "titleTemplate is required")
        String titleTemplate,
        @NotBlank(message = "contentTemplate is required")
        String contentTemplate,
        Boolean enabled
) {
    UpdateNotificationTemplateRequest toCommand() {
        return new UpdateNotificationTemplateRequest(templateName, titleTemplate, contentTemplate, enabled);
    }
}

record SaveNotificationGatewayConfigRequest(
        Boolean requireConfiguredGateway,
        List<SaveNotificationGatewayChannelBindingRequest> channelBindings,
        List<SaveNotificationGatewayProviderRequest> providers
) {
    UpdateNotificationGatewayConfigRequest toCommand() {
        return new UpdateNotificationGatewayConfigRequest(
                requireConfiguredGateway,
                channelBindings == null ? List.of() : channelBindings.stream().map(SaveNotificationGatewayChannelBindingRequest::toCommand).toList(),
                providers == null ? List.of() : providers.stream().map(SaveNotificationGatewayProviderRequest::toCommand).toList()
        );
    }
}

record SaveNotificationGatewayChannelBindingRequest(
        @NotBlank(message = "notifyType is required")
        String notifyType,
        @NotBlank(message = "gatewayCode is required")
        String gatewayCode
) {
    UpdateNotificationGatewayChannelBindingRequest toCommand() {
        return new UpdateNotificationGatewayChannelBindingRequest(notifyType, gatewayCode);
    }
}

record SaveNotificationGatewayProviderRequest(
        @NotBlank(message = "gatewayCode is required")
        String gatewayCode,
        @NotBlank(message = "notifyType is required")
        String notifyType,
        boolean enabled,
        boolean mockMode,
        boolean receiptSupported,
        String endpoint,
        String receiptCallbackPath,
        String description
) {
    UpdateNotificationGatewayProviderRequest toCommand() {
        return new UpdateNotificationGatewayProviderRequest(
                gatewayCode,
                notifyType,
                enabled,
                mockMode,
                receiptSupported,
                endpoint,
                receiptCallbackPath,
                description
        );
    }
}

