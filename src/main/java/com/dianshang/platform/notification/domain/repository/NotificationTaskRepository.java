package com.dianshang.platform.notification.domain.repository;

import com.dianshang.platform.notification.model.NotificationTask;

import java.util.List;
import java.util.Optional;

public interface NotificationTaskRepository {

    List<NotificationTask> findByTenantId(String tenantId);

    Optional<NotificationTask> findByNotificationTaskId(String notificationTaskId);

    NotificationTask save(NotificationTask notificationTask);

    void deleteAll();
}
