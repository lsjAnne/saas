package backend.openplatform.domain.repository;

import backend.openplatform.model.OpenCallbackReplayRecord;

public interface OpenCallbackReplayRepository {

    boolean exists(String subscriptionId, String requestId);

    OpenCallbackReplayRecord save(OpenCallbackReplayRecord replayRecord);

    void deleteAll();
}

