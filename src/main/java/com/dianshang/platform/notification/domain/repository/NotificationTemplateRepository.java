package com.dianshang.platform.notification.domain.repository;

import com.dianshang.platform.notification.model.NotificationTemplate;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository {

    List<NotificationTemplate> findByTenantId(String tenantId);

    Optional<NotificationTemplate> findByNotificationTemplateId(String notificationTemplateId);

    Optional<NotificationTemplate> findByTenantIdAndTemplateCode(String tenantId, String templateCode);

    NotificationTemplate save(NotificationTemplate notificationTemplate);

    void deleteAll();
}
