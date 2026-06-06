package com.dianshang.platform.rule.infrastructure.persistence;

import com.dianshang.platform.rule.domain.repository.AutomationRuleRepository;
import com.dianshang.platform.rule.model.AutomationRule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryAutomationRuleRepository implements AutomationRuleRepository {

    private final ConcurrentHashMap<String, AutomationRule> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public AutomationRule save(AutomationRule automationRule) {
        String ruleId = automationRule.ruleId();
        if (ruleId == null || ruleId.isBlank()) {
            ruleId = "rule-" + sequence.incrementAndGet();
        }
        AutomationRule saved = new AutomationRule(
                ruleId,
                automationRule.storeId(),
                automationRule.ruleType(),
                automationRule.ruleCategory(),
                automationRule.riskCategory(),
                automationRule.ruleName(),
                automationRule.ruleExpression(),
                automationRule.enabled(),
                automationRule.createdAt()
        );
        storage.put(ruleId, saved);
        return saved;
    }

    @Override
    public List<AutomationRule> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(rule -> storeIds.contains(rule.storeId()))
                .sorted(Comparator.comparing(AutomationRule::createdAt).thenComparing(AutomationRule::ruleId))
                .toList();
    }

    @Override
    public Optional<AutomationRule> findByRuleId(String ruleId) {
        return Optional.ofNullable(storage.get(ruleId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}
