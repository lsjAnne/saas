package com.dianshang.platform.inventory.infrastructure.persistence;

import com.dianshang.platform.inventory.domain.repository.ReplenishmentTaskRepository;
import com.dianshang.platform.inventory.model.ReplenishmentTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryReplenishmentTaskRepository implements ReplenishmentTaskRepository {

    private final Map<String, ReplenishmentTask> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(11000);

    @Override
    public ReplenishmentTask save(ReplenishmentTask replenishmentTask) {
        String replenishmentTaskId = replenishmentTask.replenishmentTaskId();
        if (replenishmentTaskId == null || replenishmentTaskId.isBlank()) {
            replenishmentTaskId = "replenishment-" + sequence.incrementAndGet();
        }
        ReplenishmentTask saved = new ReplenishmentTask(
                replenishmentTaskId,
                replenishmentTask.storeId(),
                replenishmentTask.productId(),
                replenishmentTask.skuId(),
                replenishmentTask.suggestedQty(),
                replenishmentTask.taskStatus(),
                replenishmentTask.approvalStatus(),
                replenishmentTask.reasonText(),
                replenishmentTask.createdAt()
        );
        storage.put(saved.replenishmentTaskId(), saved);
        return saved;
    }

    @Override
    public List<ReplenishmentTask> findByStoreIds(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        Set<String> storeIdSet = Set.copyOf(storeIds);
        return storage.values().stream()
                .filter(task -> storeIdSet.contains(task.storeId()))
                .sorted(Comparator.comparing(ReplenishmentTask::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<ReplenishmentTask> findByReplenishmentTaskId(String replenishmentTaskId) {
        return Optional.ofNullable(storage.get(replenishmentTaskId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(11000);
    }
}
