package com.dianshang.platform.notification.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.notification.application.NotificationApplicationService;
import com.dianshang.platform.notification.application.NotificationService.BatchSendNotificationRequest;
import com.dianshang.platform.notification.application.NotificationService.SendNotificationRequest;
import com.dianshang.platform.notification.application.NotificationService.UpdateNotificationTemplateRequest;
import com.dianshang.platform.notification.model.NotificationTask;
import com.dianshang.platform.notification.model.NotificationTemplate;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
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
