package backend.notification.model;

import java.time.OffsetDateTime;

public record NotificationTemplate(
        String notificationTemplateId,
        String tenantId,
        String templateCode,
        String templateName,
        String notifyType,
        String titleTemplate,
        String contentTemplate,
        boolean enabled,
        OffsetDateTime createdAt
) {
    public NotificationTemplate withEditableFields(String templateName,
                                                   String titleTemplate,
                                                   String contentTemplate,
                                                   Boolean enabled) {
        return new NotificationTemplate(
                notificationTemplateId,
                tenantId,
                templateCode,
                templateName,
                notifyType,
                titleTemplate,
                contentTemplate,
                enabled == null ? this.enabled : enabled,
                createdAt
        );
    }
}

