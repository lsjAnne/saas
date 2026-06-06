package com.dianshang.platform.live.domain.repository;

import com.dianshang.platform.live.model.LivePlan;

import java.util.List;
import java.util.Optional;

public interface LivePlanRepository {

    LivePlan save(LivePlan livePlan);

    List<LivePlan> findByStoreIds(List<String> storeIds);

    Optional<LivePlan> findByLivePlanId(String livePlanId);

    void deleteAll();
}
