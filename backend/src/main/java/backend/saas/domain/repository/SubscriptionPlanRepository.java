package backend.saas.domain.repository;

import backend.saas.model.SubscriptionPlan;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository {

    List<SubscriptionPlan> findAll();

    Optional<SubscriptionPlan> findByPlanCode(String planCode);

    void saveAll(List<SubscriptionPlan> plans);
}

