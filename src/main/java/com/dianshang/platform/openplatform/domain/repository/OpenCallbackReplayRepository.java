package com.dianshang.platform.openplatform.domain.repository;

import com.dianshang.platform.openplatform.model.OpenCallbackReplayRecord;

public interface OpenCallbackReplayRepository {

    boolean exists(String subscriptionId, String requestId);

    OpenCallbackReplayRecord save(OpenCallbackReplayRecord replayRecord);

    void deleteAll();
}
