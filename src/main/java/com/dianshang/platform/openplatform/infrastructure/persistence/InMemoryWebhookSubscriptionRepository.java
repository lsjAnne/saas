package com.dianshang.platform.openplatform.infrastructure.persistence;

import com.dianshang.platform.openplatform.domain.repository.WebhookSubscriptionRepository;
import com.dianshang.platform.openplatform.model.WebhookSubscription;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryWebhookSubscriptionRepository implements WebhookSubscriptionRepository {

    private final Map<String, WebhookSubscription> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(22000);

    @Override
    public WebhookSubscription save(WebhookSubscription webhookSubscription) {
        String subscriptionId = webhookSubscription.subscriptionId();
        if (subscriptionId == null || subscriptionId.isBlank()) {
            subscriptionId = "webhook-" + sequence.incrementAndGet();
        }
        WebhookSubscription saved = new WebhookSubscription(
                subscriptionId,
                webhookSubscription.organizationId(),
                webhookSubscription.eventCode(),
                webhookSubscription.callbackUrl(),
                webhookSubscription.secretToken(),
                webhookSubscription.status(),
                webhookSubscription.createdAt()
        );
        storage.put(saved.subscriptionId(), saved);
        return saved;
    }

    @Override
    public List<WebhookSubscription> findByOrganizationIds(List<String> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return List.of();
        }
        Set<String> organizationIdSet = Set.copyOf(organizationIds);
        return storage.values().stream()
                .filter(subscription -> organizationIdSet.contains(subscription.organizationId()))
                .sorted(Comparator.comparing(WebhookSubscription::createdAt).thenComparing(WebhookSubscription::subscriptionId))
                .toList();
    }

    @Override
    public Optional<WebhookSubscription> findBySubscriptionId(String subscriptionId) {
        return Optional.ofNullable(storage.get(subscriptionId));
    }

    @Override
    public Optional<WebhookSubscription> findByOrganizationIdAndEventCodeAndCallbackUrl(String organizationId,
                                                                                         String eventCode,
                                                                                         String callbackUrl) {
        return storage.values().stream()
                .filter(subscription -> organizationId.equals(subscription.organizationId()))
                .filter(subscription -> eventCode.equals(subscription.eventCode()))
                .filter(subscription -> callbackUrl.equals(subscription.callbackUrl()))
                .findFirst();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(22000);
    }
}
