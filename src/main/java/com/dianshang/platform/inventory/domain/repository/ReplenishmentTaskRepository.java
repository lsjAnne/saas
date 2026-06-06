package com.dianshang.platform.inventory.domain.repository;

import com.dianshang.platform.inventory.model.ReplenishmentTask;

import java.util.List;
import java.util.Optional;

public interface ReplenishmentTaskRepository {

    ReplenishmentTask save(ReplenishmentTask replenishmentTask);

    List<ReplenishmentTask> findByStoreIds(List<String> storeIds);

    Optional<ReplenishmentTask> findByReplenishmentTaskId(String replenishmentTaskId);

    void deleteAll();
}
