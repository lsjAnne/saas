package backend.saas.infrastructure.persistence;

import backend.saas.domain.repository.SubscriptionPlanRepository;
import backend.saas.model.SubscriptionPlan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemorySubscriptionPlanRepository implements SubscriptionPlanRepository {

    private final Map<String, SubscriptionPlan> storage = new LinkedHashMap<>();

    @Override
    public List<SubscriptionPlan> findAll() {
        return List.copyOf(storage.values());
    }

    @Override
    public Optional<SubscriptionPlan> findByPlanCode(String planCode) {
        return Optional.ofNullable(storage.get(planCode));
    }

    @Override
    public void saveAll(List<SubscriptionPlan> plans) {
        for (SubscriptionPlan plan : plans) {
            storage.put(plan.planCode(), plan);
        }
    }
}

