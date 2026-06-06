package com.dianshang.platform.live.domain.repository;

import com.dianshang.platform.live.model.LiveSession;

import java.util.List;
import java.util.Optional;

public interface LiveSessionRepository {

    LiveSession save(LiveSession liveSession);

    List<LiveSession> findByStoreIds(List<String> storeIds);

    Optional<LiveSession> findLatestByLivePlanId(String livePlanId);

    Optional<LiveSession> findRunningByLiveAccountId(String liveAccountId);

    int countRunningByTenantId(String tenantId);

    void deleteAll();
}
