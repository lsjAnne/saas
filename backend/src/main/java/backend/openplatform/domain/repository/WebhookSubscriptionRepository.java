package backend.openplatform.domain.repository;

import backend.openplatform.model.WebhookSubscription;

import java.util.List;
import java.util.Optional;

public interface WebhookSubscriptionRepository {

    WebhookSubscription save(WebhookSubscription webhookSubscription);

    List<WebhookSubscription> findByOrganizationIds(List<String> organizationIds);

    Optional<WebhookSubscription> findBySubscriptionId(String subscriptionId);

    Optional<WebhookSubscription> findByOrganizationIdAndEventCodeAndCallbackUrl(String organizationId,
                                                                                 String eventCode,
                                                                                 String callbackUrl);

    void deleteAll();
}

