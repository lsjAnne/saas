package backend.rule.domain.repository;

import backend.rule.model.AutomationRule;

import java.util.List;
import java.util.Optional;

public interface AutomationRuleRepository {

    AutomationRule save(AutomationRule automationRule);

    List<AutomationRule> findByStoreIds(List<String> storeIds);

    Optional<AutomationRule> findByRuleId(String ruleId);

    void deleteAll();
}

