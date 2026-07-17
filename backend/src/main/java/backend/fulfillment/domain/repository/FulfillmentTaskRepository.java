package backend.fulfillment.domain.repository;

import backend.fulfillment.model.FulfillmentTask;

import java.util.List;
import java.util.Optional;

public interface FulfillmentTaskRepository {

    FulfillmentTask save(FulfillmentTask fulfillmentTask);

    List<FulfillmentTask> findByStoreIds(List<String> storeIds);

    List<FulfillmentTask> findByOrderId(String orderId);

    Optional<FulfillmentTask> findByFulfillmentTaskId(String fulfillmentTaskId);

    void deleteAll();
}

