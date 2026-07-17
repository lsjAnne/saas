package backend.notification.infrastructure.persistence;

import backend.notification.domain.repository.NotificationTemplateRepository;
import backend.notification.model.NotificationTemplate;
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
public class InMemoryNotificationTemplateRepository implements NotificationTemplateRepository {

    private final Map<String, NotificationTemplate> storage = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    @Override
    public List<NotificationTemplate> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(template -> tenantId.equals(template.tenantId()))
                .sorted(Comparator.comparing(NotificationTemplate::templateCode)
                        .thenComparing(NotificationTemplate::notificationTemplateId))
                .toList();
    }

    @Override
    public Optional<NotificationTemplate> findByNotificationTemplateId(String notificationTemplateId) {
        return Optional.ofNullable(storage.get(notificationTemplateId));
    }

    @Override
    public Optional<NotificationTemplate> findByTenantIdAndTemplateCode(String tenantId, String templateCode) {
        return storage.values().stream()
                .filter(template -> tenantId.equals(template.tenantId()))
                .filter(template -> templateCode.equals(template.templateCode()))
                .findFirst();
    }

    @Override
    public NotificationTemplate save(NotificationTemplate notificationTemplate) {
        NotificationTemplate stored = notificationTemplate;
        if (notificationTemplate.notificationTemplateId() == null || notificationTemplate.notificationTemplateId().isBlank()) {
            stored = new NotificationTemplate(
                    "notification-template-" + sequence.getAndIncrement(),
                    notificationTemplate.tenantId(),
                    notificationTemplate.templateCode(),
                    notificationTemplate.templateName(),
                    notificationTemplate.notifyType(),
                    notificationTemplate.titleTemplate(),
                    notificationTemplate.contentTemplate(),
                    notificationTemplate.enabled(),
                    notificationTemplate.createdAt()
            );
        }
        storage.put(stored.notificationTemplateId(), stored);
        return stored;
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(1L);
    }
}

