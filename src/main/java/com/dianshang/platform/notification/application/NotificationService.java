package com.dianshang.platform.notification.application;

import com.dianshang.platform.notification.model.NotificationTask;
import com.dianshang.platform.notification.model.NotificationTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Deprecated(forRemoval = false)
public class NotificationService {

    private final NotificationApplicationService notificationApplicationService;

    public NotificationService(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    public List<NotificationTask> listNotifications(String tenantId) {
        return notificationApplicationService.listNotifications(tenantId);
    }

    public NotificationTask sendNotification(String tenantId, SendNotificationRequest request) {
        return notificationApplicationService.sendNotification(tenantId, request);
    }

    public List<NotificationTask> batchSendNotifications(String tenantId, BatchSendNotificationRequest request) {
        return notificationApplicationService.batchSendNotifications(tenantId, request);
    }

    public NotificationTask retryNotification(String tenantId, String notificationTaskId) {
        return notificationApplicationService.retryNotification(tenantId, notificationTaskId);
    }

    public List<NotificationTask> runDueNotifications(String tenantId) {
        return notificationApplicationService.runDueNotifications(tenantId);
    }

    public List<NotificationTemplate> listTemplates(String tenantId) {
        return notificationApplicationService.listTemplates(tenantId);
    }

    public NotificationTemplate updateTemplate(String tenantId,
                                               String notificationTemplateId,
                                               UpdateNotificationTemplateRequest request) {
        return notificationApplicationService.updateTemplate(tenantId, notificationTemplateId, request);
    }

    public NotificationTask createSystemNotification(String tenantId,
                                                     String notifyType,
                                                     String templateCode,
                                                     String targetReceiver,
                                                     String payloadJson) {
        return notificationApplicationService.createSystemNotification(
                tenantId,
                notifyType,
                templateCode,
                targetReceiver,
                payloadJson
        );
    }

    public void clear() {
        notificationApplicationService.clear();
    }

    public record SendNotificationRequest(
            String notifyType,
            String templateCode,
            String targetReceiver,
            String payloadJson,
            String priority,
            OffsetDateTime scheduledAt
    ) {
    }

    public record BatchSendNotificationRequest(
            String notifyType,
            String templateCode,
            List<String> targetReceivers,
            String payloadJson,
            String priority,
            OffsetDateTime scheduledAt,
            String batchId
    ) {
    }

    public record UpdateNotificationTemplateRequest(
            String templateName,
            String titleTemplate,
            String contentTemplate,
            Boolean enabled
    ) {
    }

    public record NotificationTemplateSeed(
            String templateCode,
            String templateName,
            String notifyType,
            String titleTemplate,
            String contentTemplate
    ) {
    }
}
