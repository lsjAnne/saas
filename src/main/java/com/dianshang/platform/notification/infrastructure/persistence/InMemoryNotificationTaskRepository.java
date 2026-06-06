package com.dianshang.platform.notification.infrastructure.persistence;

import com.dianshang.platform.notification.domain.repository.NotificationTaskRepository;
import com.dianshang.platform.notification.model.NotificationTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryNotificationTaskRepository implements NotificationTaskRepository {

    private final Map<String, NotificationTask> storage = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    @Override
    public List<NotificationTask> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(task -> tenantId.equals(task.tenantId()))
                .sorted(Comparator.comparing(NotificationTask::createdAt).reversed()
                        .thenComparing(NotificationTask::notificationTaskId, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public Optional<NotificationTask> findByNotificationTaskId(String notificationTaskId) {
        return Optional.ofNullable(storage.get(notificationTaskId));
    }

    @Override
    public NotificationTask save(NotificationTask notificationTask) {
        NotificationTask stored = notificationTask;
        if (notificationTask.notificationTaskId() == null || notificationTask.notificationTaskId().isBlank()) {
            stored = new NotificationTask(
                    "notification-" + sequence.getAndIncrement(),
                    notificationTask.tenantId(),
                    notificationTask.notifyType(),
                    notificationTask.templateCode(),
                    notificationTask.targetReceiver(),
                    notificationTask.sendStatus(),
                    notificationTask.retryCount(),
                    notificationTask.payloadJson(),
                    notificationTask.priority(),
                    notificationTask.scheduledAt(),
                    notificationTask.batchId(),
                    notificationTask.deadLetterReason(),
                    notificationTask.createdAt()
            );
        }
        storage.put(stored.notificationTaskId(), stored);
        return stored;
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(1L);
    }
}
