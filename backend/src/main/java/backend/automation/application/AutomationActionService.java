package backend.automation.application;

import backend.automation.domain.repository.AutomationActionRepository;
import backend.automation.model.AutomationAction;
import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.rule.domain.repository.AutomationRuleRepository;
import backend.rule.model.AutomationRule;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 规则动作绑定 CRUD：动作与规则关联，VO 中回填 ruleName/ruleType/ruleExpression。
 */
@Service
public class AutomationActionService {

    private static final List<String> SUPPORTED_ACTION_TYPES = List.of(
            "AUTO_SHIP", "PAUSE_REVIEW", "GENERATE_BILL", "AUTO_RECONCILE", "NOTIFY");

    private final AutomationActionRepository actionRepository;
    private final AutomationRuleRepository ruleRepository;
    private final StoreRepository storeRepository;
    private final AuditLogService auditLogService;

    public AutomationActionService(AutomationActionRepository actionRepository,
                                   AutomationRuleRepository ruleRepository,
                                   StoreRepository storeRepository,
                                   AuditLogService auditLogService) {
        this.actionRepository = actionRepository;
        this.ruleRepository = ruleRepository;
        this.storeRepository = storeRepository;
        this.auditLogService = auditLogService;
    }

    public List<AutomationActionView> listActions(String tenantId, String storeId, String ruleId, Boolean enabled) {
        requireOwnedStore(tenantId, storeId);
        List<AutomationRule> rules = ruleRepository.findByStoreIds(List.of(storeId));
        Map<String, AutomationRule> ruleMap = rules.stream()
                .collect(Collectors.toMap(AutomationRule::ruleId, Function.identity(), (left, right) -> left));
        return actionRepository.findByStoreId(storeId).stream()
                .filter(action -> ruleId == null || ruleId.isBlank() || ruleId.equals(action.ruleId()))
                .filter(action -> enabled == null || enabled == action.enabled())
                .map(action -> toView(action, ruleMap.get(action.ruleId())))
                .toList();
    }

    public AutomationActionView createAction(String tenantId, SaveAutomationActionRequest request) {
        Store store = requireOwnedStore(tenantId, request.storeId());
        AutomationRule rule = requireRuleOfStore(store.storeId(), request.ruleId());
        validateActionType(request.actionType());
        OffsetDateTime now = OffsetDateTime.now();
        AutomationAction saved = actionRepository.save(new AutomationAction(
                UUID.randomUUID().toString(),
                tenantId,
                store.storeId(),
                rule.ruleId(),
                request.actionType(),
                request.actionConfig(),
                request.priority() == null ? 0 : request.priority(),
                request.enabled() == null || request.enabled(),
                now,
                now
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_AUTOMATION_ACTION", "automation_action", saved.actionId());
        return toView(saved, rule);
    }

    public AutomationActionView updateAction(String tenantId, String actionId, UpdateAutomationActionRequest request) {
        AutomationAction current = requireOwnedAction(tenantId, actionId);
        if (request.actionType() != null && !request.actionType().isBlank()) {
            validateActionType(request.actionType());
        }
        AutomationAction updated = new AutomationAction(
                current.actionId(),
                current.tenantId(),
                current.storeId(),
                current.ruleId(),
                request.actionType() == null || request.actionType().isBlank() ? current.actionType() : request.actionType(),
                request.actionConfig() == null ? current.actionConfig() : request.actionConfig(),
                request.priority() == null ? current.priority() : request.priority(),
                request.enabled() == null ? current.enabled() : request.enabled(),
                current.createdAt(),
                OffsetDateTime.now()
        );
        actionRepository.save(updated);
        auditLogService.recordForTenant(tenantId, "UPDATE_AUTOMATION_ACTION", "automation_action", actionId);
        AutomationRule rule = ruleRepository.findByRuleId(current.ruleId()).orElse(null);
        return toView(updated, rule);
    }

    public void deleteAction(String tenantId, String actionId) {
        requireOwnedAction(tenantId, actionId);
        actionRepository.deleteByActionId(actionId);
        auditLogService.recordForTenant(tenantId, "DELETE_AUTOMATION_ACTION", "automation_action", actionId);
    }

    public List<AutomationRuleView> listRules(String tenantId, Boolean enabled) {
        return ruleRepository.findByStoreIds(ownedStoreIds(tenantId)).stream()
                .filter(rule -> enabled == null || enabled == rule.enabled())
                .sorted(Comparator.comparing(AutomationRule::createdAt).thenComparing(AutomationRule::ruleId))
                .map(rule -> new AutomationRuleView(
                        rule.ruleId(), rule.storeId(), rule.ruleType(), rule.ruleName(),
                        rule.ruleExpression(), rule.enabled()))
                .toList();
    }

    private AutomationAction requireOwnedAction(String tenantId, String actionId) {
        AutomationAction action = actionRepository.findByActionId(actionId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, action.storeId());
        return action;
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND);
        }
        return store;
    }

    private AutomationRule requireRuleOfStore(String storeId, String ruleId) {
        AutomationRule rule = ruleRepository.findByRuleId(ruleId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!storeId.equals(rule.storeId())) {
            throw new BusinessException("1004", "rule does not belong to the store", HttpStatus.BAD_REQUEST);
        }
        return rule;
    }

    private void validateActionType(String actionType) {
        if (!SUPPORTED_ACTION_TYPES.contains(actionType)) {
            throw new BusinessException("8601", "unsupported actionType: " + actionType, HttpStatus.BAD_REQUEST);
        }
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream().map(Store::storeId).toList();
    }

    private AutomationActionView toView(AutomationAction action, AutomationRule rule) {
        return new AutomationActionView(
                action.actionId(),
                action.ruleId(),
                rule == null ? null : rule.ruleName(),
                rule == null ? null : rule.ruleType(),
                rule == null ? null : rule.ruleExpression(),
                action.actionType(),
                action.actionConfig(),
                action.priority(),
                action.enabled(),
                action.createdAt()
        );
    }

    public record SaveAutomationActionRequest(
            String storeId,
            String ruleId,
            String actionType,
            String actionConfig,
            Integer priority,
            Boolean enabled
    ) {
    }

    public record UpdateAutomationActionRequest(
            String actionType,
            String actionConfig,
            Integer priority,
            Boolean enabled
    ) {
    }

    public record AutomationActionView(
            String actionId,
            String ruleId,
            String ruleName,
            String ruleType,
            String ruleExpression,
            String actionType,
            String actionConfig,
            int priority,
            boolean enabled,
            OffsetDateTime createdAt
    ) {
    }

    public record AutomationRuleView(
            String ruleId,
            String storeId,
            String ruleType,
            String ruleName,
            String ruleExpression,
            boolean enabled
    ) {
    }
}
