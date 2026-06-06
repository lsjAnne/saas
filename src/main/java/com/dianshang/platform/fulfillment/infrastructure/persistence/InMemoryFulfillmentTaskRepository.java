package com.dianshang.platform.fulfillment.infrastructure.persistence;

import com.dianshang.platform.fulfillment.domain.repository.FulfillmentTaskRepository;
import com.dianshang.platform.fulfillment.model.FulfillmentTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryFulfillmentTaskRepository implements FulfillmentTaskRepository {

    private final Map<String, FulfillmentTask> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(9200);

    @Override
    public FulfillmentTask save(FulfillmentTask fulfillmentTask) {
        String fulfillmentTaskId = fulfillmentTask.fulfillmentTaskId();
        if (fulfillmentTaskId == null || fulfillmentTaskId.isBlank()) {
            fulfillmentTaskId = "fulfillment-" + sequence.incrementAndGet();
        }
        FulfillmentTask saved = new FulfillmentTask(
                fulfillmentTaskId,
                fulfillmentTask.storeId(),
                fulfillmentTask.orderId(),
                fulfillmentTask.idempotencyKey(),
                fulfillmentTask.status(),
                fulfillmentTask.retryCount(),
                fulfillmentTask.dueAt(),
                fulfillmentTask.lastErrorMessage(),
                fulfillmentTask.createdAt()
        );
        storage.put(saved.fulfillmentTaskId(), saved);
        return saved;
    }

    @Override
    public List<FulfillmentTask> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(task -> storeIds.contains(task.storeId()))
                .sorted(Comparator.comparing(FulfillmentTask::createdAt).reversed())
                .toList();
    }

    @Override
    public List<FulfillmentTask> findByOrderId(String orderId) {
        return storage.values().stream()
                .filter(task -> orderId.equals(task.orderId()))
                .sorted(Comparator.comparing(FulfillmentTask::createdAt))
                .toList();
    }

    @Override
    public Optional<FulfillmentTask> findByFulfillmentTaskId(String fulfillmentTaskId) {
        return Optional.ofNullable(storage.get(fulfillmentTaskId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(9200);
    }
}
