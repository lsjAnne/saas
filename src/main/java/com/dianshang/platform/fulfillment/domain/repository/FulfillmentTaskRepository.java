package com.dianshang.platform.fulfillment.domain.repository;

import com.dianshang.platform.fulfillment.model.FulfillmentTask;

import java.util.List;
import java.util.Optional;

public interface FulfillmentTaskRepository {

    FulfillmentTask save(FulfillmentTask fulfillmentTask);

    List<FulfillmentTask> findByStoreIds(List<String> storeIds);

    List<FulfillmentTask> findByOrderId(String orderId);

    Optional<FulfillmentTask> findByFulfillmentTaskId(String fulfillmentTaskId);

    void deleteAll();
}
