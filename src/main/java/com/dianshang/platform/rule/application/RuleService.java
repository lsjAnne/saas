package com.dianshang.platform.rule.application;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.rule.domain.repository.AutomationRuleRepository;
import com.dianshang.platform.rule.model.AutomationRule;
import com.dianshang.platform.store.domain.repository.StoreRepository;
import com.dianshang.platform.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class RuleService {

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final AutomationRuleRepository automationRuleRepository;

    public RuleService(AuditLogService auditLogService,
                       StoreRepository storeRepository,
                       AutomationRuleRepository automationRuleRepository) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.automationRuleRepository = automationRuleRepository;
    }

    public List<AutomationRule> listRules(String tenantId) {
        return automationRuleRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public AutomationRule createRule(String tenantId, SaveRuleRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        AutomationRule saved = automationRuleRepository.save(new AutomationRule(
                null,
                request.storeId(),
                request.ruleType(),
                normalizeCategory(request.ruleCategory()),
                normalizeCategory(request.riskCategory()),
                request.ruleName(),
                request.ruleExpression(),
                request.enabled() == null || request.enabled(),
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_AUTOMATION_RULE", "automation_rule", saved.ruleId());
        return saved;
    }

    public AutomationRule updateRule(String tenantId, String ruleId, SaveRuleRequest request) {
        AutomationRule current = requireOwnedRule(tenantId, ruleId);
        if (!current.storeId().equals(request.storeId())) {
            throw new BusinessException("1004", "storeId does not match current rule", HttpStatus.BAD_REQUEST);
        }
        requireOwnedStore(tenantId, request.storeId());
        AutomationRule updated = automationRuleRepository.save(new AutomationRule(
                current.ruleId(),
                current.storeId(),
                request.ruleType(),
                normalizeCategory(request.ruleCategory()),
                normalizeCategory(request.riskCategory()),
                request.ruleName(),
                request.ruleExpression(),
                request.enabled() == null ? current.enabled() : request.enabled(),
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "UPDATE_AUTOMATION_RULE", "automation_rule", ruleId);
        return updated;
    }

    public AutomationRule enableRule(String tenantId, String ruleId) {
        AutomationRule current = requireOwnedRule(tenantId, ruleId);
        if (current.enabled()) {
            return current;
        }
        AutomationRule updated = automationRuleRepository.save(new AutomationRule(
                current.ruleId(),
                current.storeId(),
                current.ruleType(),
                current.ruleCategory(),
                current.riskCategory(),
                current.ruleName(),
                current.ruleExpression(),
                true,
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "ENABLE_AUTOMATION_RULE", "automation_rule", ruleId);
        return updated;
    }

    public AutomationRule disableRule(String tenantId, String ruleId) {
        AutomationRule current = requireOwnedRule(tenantId, ruleId);
        if (!current.enabled()) {
            return current;
        }
        AutomationRule updated = automationRuleRepository.save(new AutomationRule(
                current.ruleId(),
                current.storeId(),
                current.ruleType(),
                current.ruleCategory(),
                current.riskCategory(),
                current.ruleName(),
                current.ruleExpression(),
                false,
                current.createdAt()
        ));
        auditLogService.recordForTenant(tenantId, "DISABLE_AUTOMATION_RULE", "automation_rule", ruleId);
        return updated;
    }

    public void clear() {
        automationRuleRepository.deleteAll();
    }

    private AutomationRule requireOwnedRule(String tenantId, String ruleId) {
        AutomationRule rule = automationRuleRepository.findByRuleId(ruleId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        Store store = storeRepository.findByStoreId(rule.storeId())
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        return rule;
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "tenant context invalid", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    private String normalizeCategory(String value) {
        return value == null || value.isBlank() ? "general" : value;
    }

    public record SaveRuleRequest(
            String storeId,
            String ruleType,
            String ruleCategory,
            String riskCategory,
            String ruleName,
            String ruleExpression,
            Boolean enabled
    ) {
    }
}
