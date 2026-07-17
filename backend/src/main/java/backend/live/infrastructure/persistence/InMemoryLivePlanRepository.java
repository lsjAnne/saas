package backend.live.infrastructure.persistence;

import backend.live.domain.repository.LivePlanRepository;
import backend.live.model.LivePlan;
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
public class InMemoryLivePlanRepository implements LivePlanRepository {

    private final Map<String, LivePlan> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(11000);

    @Override
    public LivePlan save(LivePlan livePlan) {
        String livePlanId = livePlan.livePlanId();
        if (livePlanId == null || livePlanId.isBlank()) {
            livePlanId = "live-plan-" + sequence.incrementAndGet();
        }
        LivePlan saved = new LivePlan(
                livePlanId,
                livePlan.storeId(),
                livePlan.liveAccountId(),
                livePlan.planName(),
                livePlan.planStatus(),
                livePlan.scheduledStartAt(),
                livePlan.scheduledEndAt(),
                livePlan.anchorProfileName(),
                livePlan.createdAt()
        );
        storage.put(saved.livePlanId(), saved);
        return saved;
    }

    @Override
    public List<LivePlan> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(plan -> storeIds.contains(plan.storeId()))
                .sorted(Comparator.comparing(LivePlan::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<LivePlan> findByLivePlanId(String livePlanId) {
        return Optional.ofNullable(storage.get(livePlanId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(11000);
    }
}

