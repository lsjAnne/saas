package backend.notification.domain.repository;

import backend.notification.model.NotificationTask;

import java.util.List;
import java.util.Optional;

public interface NotificationTaskRepository {

    List<NotificationTask> findByTenantId(String tenantId);

    Optional<NotificationTask> findByNotificationTaskId(String notificationTaskId);

    NotificationTask save(NotificationTask notificationTask);

    void deleteAll();
}

