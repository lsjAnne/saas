package backend.openplatform.infrastructure.persistence;

import backend.openplatform.domain.repository.OpenCallbackReplayRepository;
import backend.openplatform.model.OpenCallbackReplayRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryOpenCallbackReplayRepository implements OpenCallbackReplayRepository {

    private final Set<String> storage = ConcurrentHashMap.newKeySet();

    @Override
    public boolean exists(String subscriptionId, String requestId) {
        return storage.contains(key(subscriptionId, requestId));
    }

    @Override
    public OpenCallbackReplayRecord save(OpenCallbackReplayRecord replayRecord) {
        storage.add(key(replayRecord.subscriptionId(), replayRecord.requestId()));
        return replayRecord;
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }

    private String key(String subscriptionId, String requestId) {
        return subscriptionId + "::" + requestId;
    }
}

